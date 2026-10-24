import {readFile, writeFile, rename, open} from 'node:fs/promises';
import {randomUUID} from 'node:crypto';
import {dirname} from 'node:path';

export const productionFields = ['PM04D','PM05E','PM06F','PM08G','PM10K','PM11M','PM15P'];
export function validateRecord(row) {
  if (!row || typeof row !== 'object' || Array.isArray(row) || !productionFields.includes(row.field) ||
      !/^\d{4}-\d{2}-\d{2}$/.test(row.date ?? '') || row.date < '2000-01-01' || row.date > '2100-12-31' ||
      !Number.isFinite(Date.parse(row.date)) || new Date(row.date).toISOString().slice(0,10) !== row.date) throw Error('Invalid field/date');
  const output = {date:row.date,field:row.field};
  for (const key of ['harvested','sentToMill','backlog','hectares','harvesters']) {
    const value = row[key] ?? null;
    if (value !== null && (typeof value !== 'number' || !Number.isFinite(value) || value < 0 || value > 1e9 ||
        (key === 'harvesters' && !Number.isInteger(value)))) throw Error('Invalid production value');
    output[key] = value;
  }
  if (Object.values(output).slice(2).every(v=>v===null)) throw Error('Empty record');
  return output;
}
export const recordKey = r => `${r.date}/${r.field}`;
export function productionStore(path) {
  let queue = Promise.resolve();
  async function load() {
    let state;
    try { state = JSON.parse(await readFile(path,'utf8')); }
    catch (e) { if (e.code === 'ENOENT') return {schemaVersion:1,revision:0,records:[],audit:[]}; throw e; }
    if (state.schemaVersion !== 1 || !Number.isSafeInteger(state.revision) || state.revision < 0 || !Array.isArray(state.records) || !Array.isArray(state.audit)) throw Error('Invalid production storage');
    const keys = new Set();
    for (const entry of state.records) {
      validateRecord(entry.record);
      if (!Number.isSafeInteger(entry.version) || entry.version < 1 || entry.version > state.revision || keys.has(recordKey(entry.record))) throw Error('Invalid stored version');
      keys.add(recordKey(entry.record));
    }
    return state;
  }
  const publicState = state => ({schemaVersion:1,estateId:'selendang-3',revision:state.revision,records:state.records});
  return {
    async get() { await queue; return publicState(await load()); },
    sync(changes, actor) {
      const run = queue.then(async () => {
        if (!Array.isArray(changes) || changes.length > 100) throw Object.assign(Error('At most 100 changes per request'),{status:400});
        const keys = new Set();
        const validated = changes.map(c => {
          let record;
          try { record = validateRecord(c?.record); } catch { throw Object.assign(Error("Invalid production record"),{status:400}); }
          if (!Number.isSafeInteger(c.baseVersion) || c.baseVersion < 0 || !/^[a-zA-Z0-9-]{16,64}$/.test(c.mutationId ?? '') || keys.has(recordKey(record))) throw Object.assign(Error('Invalid change'),{status:400});
          keys.add(recordKey(record)); return {...c,record};
        });
        const state = await load(), conflicts = [], accepted = [];
        for (const change of validated) {
          const key = recordKey(change.record), index = state.records.findIndex(e=>recordKey(e.record)===key);
          const previous = state.records[index];
          if (previous?.mutationId === change.mutationId) {
            if (JSON.stringify(previous.record) !== JSON.stringify(change.record)) throw Object.assign(Error('Mutation ID reused with different data'),{status:400});
            accepted.push(key); continue;
          }
          if ((previous?.version ?? 0) !== change.baseVersion) { conflicts.push({key,current:previous}); continue; }
          if (state.records.length >= 10000 && !previous || state.audit.length >= 100000) throw Object.assign(Error('Production storage limit reached'),{status:507});
          const entry = {record:change.record,version:++state.revision,mutationId:change.mutationId,updatedAt:new Date().toISOString(),updatedBy:actor};
          if (index < 0) state.records.push(entry); else state.records[index] = entry;
          state.audit.push({key,previous:previous ?? null,current:entry});
          accepted.push(key);
        }
        if (state.records.length && accepted.length) {
          const temporary = `${path}.${randomUUID()}.tmp`;
          const content = JSON.stringify(state);
          if (Buffer.byteLength(content) > 32*1024*1024) throw Object.assign(Error('Production storage limit reached'),{status:507});
          const file = await open(temporary,'wx',0o600);
          try { await file.writeFile(content); await file.sync(); } finally { await file.close(); }
          await rename(temporary,path);
          const directory = await open(dirname(path),'r');
          try { await directory.sync(); } finally { await directory.close(); }
        }
        return {...publicState(state),accepted,conflicts};
      });
      queue = run.catch(()=>{}); return run;
    }
  };
}
