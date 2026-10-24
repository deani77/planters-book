import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtemp, writeFile, rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {createHash, randomBytes} from 'node:crypto';
import {createEstateServer} from './server.mjs';
import {validateSnapshot, SECTION_IDS} from './validate.mjs';

const fixture = () => ({schemaVersion: 1, estateId: 'selendang-3', revision: 1,
  reportingPeriod: 'Fictional test', exportedAt: '2026-10-21T00:00:00Z',
  sections: SECTION_IDS.map(id => ({id, columns: ['Value'], rows: [[null], [0]]}))});

test('pending zero distinction and invalid snapshots', () => {
  const value = validateSnapshot(Buffer.from(JSON.stringify(fixture())));
  assert.equal(value.sections[0].rows[0][0], null);
  assert.equal(value.sections[0].rows[1][0], 0);
  const invalid = fixture(); invalid.sections[1].id = invalid.sections[0].id;
  assert.throws(() => validateSnapshot(Buffer.from(JSON.stringify(invalid))));
  assert.throws(() => validateSnapshot(Buffer.from(JSON.stringify({...fixture(), estateId: 'other'}))));
});

test('API denies anonymous and revoked access; serves only a valid snapshot', async () => {
  const dir = await mkdtemp(`${tmpdir()}/s3-test-`);
  const token = randomBytes(32).toString('base64url');
  const tokenPath = `${dir}/tokens.json`, dataPath = `${dir}/snapshot.json`;
  const tokenEntry = {id: 'test', sha256: createHash('sha256').update(token).digest('hex')};
  await writeFile(tokenPath, JSON.stringify([tokenEntry]));
  await writeFile(dataPath, JSON.stringify(fixture()));
  const server = createEstateServer({tokenPath, dataPath});
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${server.address().port}/v1/estates/selendang-3/snapshot`;
  try {
    assert.equal((await fetch(url)).status, 401);
    assert.equal((await fetch(url, {headers: {Authorization: 'Bearer incorrect'}})).status, 401);
    const headers = {Authorization: `Bearer ${token}`};
    const response = await fetch(url, {headers});
    assert.equal(response.status, 200); assert.equal((await response.json()).revision, 1);
    assert.equal((await fetch(url, {method: 'POST', headers})).status, 405);
    await writeFile(dataPath, '{}');
    assert.equal((await fetch(url, {headers})).status, 503);
    await writeFile(tokenPath, JSON.stringify([{...tokenEntry, revoked: true}]));
    assert.equal((await fetch(url, {headers})).status, 401);
  } finally {
    await new Promise(resolve => server.close(resolve));
    await rm(dir, {recursive: true});
  }
});

test('optional YPH metadata validates periods, pending values and area', () => {
  const data = fixture();
  data.yph = {reportingYear:2026,throughMonth:9,matureAreaHa:100,actualYtdTonnes:0,
    budgetYtdTonnes:1000,fullYearBudgetTonnes:1800,history:[{year:2025,actualYph:null}]};
  assert.equal(validateSnapshot(Buffer.from(JSON.stringify(data))).yph.actualYtdTonnes, 0);
  data.yph.matureAreaHa = 0;
  assert.throws(() => validateSnapshot(Buffer.from(JSON.stringify(data))));
  data.yph.matureAreaHa = 100;
  data.yph.history.push({year:2025,actualYph:10});
  assert.throws(() => validateSnapshot(Buffer.from(JSON.stringify(data))));
});
