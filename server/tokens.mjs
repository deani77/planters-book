import {readFile, writeFile, rename, chown, open, unlink} from 'node:fs/promises';
import {randomBytes, createHash} from 'node:crypto';

if (process.getuid() !== 0) throw new Error('Run as server administrator');
const [action, id, output] = process.argv.slice(2);
if (!['issue','revoke','grant-production-write','revoke-production-write'].includes(action) || !/^[a-z0-9-]{1,64}$/.test(id ?? '')) throw new Error('Usage: tokens.mjs issue ID PRIVATE_OUTPUT_FILE | revoke ID | grant-production-write ID | revoke-production-write ID');
const path = '/etc/s3-estate/tokens.json';
const lock = await open('/etc/s3-estate/tokens.lock', 'wx', 0o600);
try {
  const entries = JSON.parse(await readFile(path,'utf8'));
  const entry = entries.find(e => e.id === id);
  if (action === 'issue') {
    if (!output || entry) throw new Error('Provide a new unique user ID and private output file');
    const token = randomBytes(32).toString('base64url');
    await writeFile(output, `S3 Estate Info read-only access token for ${id}\n${token}\n`, {mode:0o600,flag:'wx'});
    entries.push({id,sha256:createHash('sha256').update(token).digest('hex'),revoked:false});
  } else {
    if (!entry) throw new Error('Unknown user ID');
    if (action === 'revoke') entry.revoked = true;
    else {
      if (entry.revoked) throw new Error('Cannot change permissions on revoked token');
      entry.scopes = (entry.scopes ?? []).filter(scope => scope !== 'production:write');
      if (action === 'grant-production-write') entry.scopes.push('production:write');
    }
  }
  const {stat} = await import('node:fs/promises');
  const original = await stat(path);
  const temporary = `/etc/s3-estate/tokens-${process.pid}.tmp`;
  await writeFile(temporary, JSON.stringify(entries), {mode:0o640,flag:'wx'});
  await chown(temporary, original.uid, original.gid);
  await rename(temporary, path);
  console.log('Token registry updated; no service restart required');
} finally {await lock.close(); await unlink('/etc/s3-estate/tokens.lock');}
