import {readFile, writeFile, rename, mkdir, copyFile, open, unlink} from 'node:fs/promises';
import {validateSnapshot} from './validate.mjs';

const directory = '/var/lib/s3-estate';
const destination = `${directory}/snapshot.json`;
const lock = await open(`${directory}/publish.lock`, 'wx', 0o600);
let temp;
try {
  const bytes = await readFile(process.argv[2]);
  const update = validateSnapshot(bytes);
  let previous;
  try {previous = validateSnapshot(await readFile(destination));}
  catch (e) {if (e.code !== 'ENOENT') throw e;}
  if (previous && update.revision <= previous.revision) throw new Error('Revision must increase');
  await mkdir(`${directory}/archive`, {recursive: true, mode: 0o750});
  if (previous) await copyFile(destination, `${directory}/archive/revision-${previous.revision}.json`);
  temp = `${directory}/snapshot-${process.pid}.tmp`;
  await writeFile(temp, bytes, {mode: 0o640, flag: 'wx'});
  // Parent directory has setgid s3estate, so newly published files are readable by the service.
  await rename(temp, destination);
  temp = undefined;
  console.log(`Published estate revision ${update.revision}`);
} finally {
  if (temp) await unlink(temp).catch(() => {});
  await lock.close();
  await unlink(`${directory}/publish.lock`);
}
