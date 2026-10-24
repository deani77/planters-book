import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtemp,writeFile,readFile,rm,mkdir} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {createHash,randomUUID} from 'node:crypto';
import {productionStore} from './production.mjs';
import {createEstateServer} from './server.mjs';
const row = (sentToMill = 2) => ({date:'2026-01-01',field:'PM04D',harvested:null,sentToMill,backlog:null,hectares:null,harvesters:null});
const change = (record = row(),baseVersion = 0) => ({record,baseVersion,mutationId:randomUUID()});

test('atomic persistence, idempotency, stale conflicts, explicit rebase and audit',async()=>{
 const dir=await mkdtemp(`${tmpdir()}/production-`), path=`${dir}/records.json`;
 try {
  const store=productionStore(path), first=change();
  let result=await store.sync([first],'phone-one'); assert.equal(result.revision,1);
  result=await store.sync([first],'phone-one'); assert.equal(result.revision,1); assert.equal(result.records.length,1);
  result=await productionStore(path).get(); assert.equal(result.records[0].record.sentToMill,2);
  const [a,b]=await Promise.all([store.sync([change(row(3),1)],'a'),store.sync([change(row(4),1)],'b')]);
  assert.equal(a.conflicts.length+b.conflicts.length,1);
  result=await store.get(); assert.equal(result.revision,2);
  result=await store.sync([change(row(0),2)],'resolved'); assert.equal(result.records[0].record.sentToMill,0);
  assert.equal(result.records[0].record.harvested,null);
  const disk=JSON.parse(await readFile(path)); assert.equal(disk.audit.length,3); assert.equal(disk.audit[2].previous.version,2);
  assert.equal(disk.records[0].updatedBy,'resolved');
  assert.equal((await store.sync([{...first,record:row(999)}],'a')).conflicts.length,1);
 } finally {await rm(dir,{recursive:true,force:true});}
});

test('validation failure is all-or-nothing and corrupt storage is never replaced',async()=>{
 const dir=await mkdtemp(`${tmpdir()}/production-`),path=`${dir}/records.json`, store=productionStore(path);
 try {
  for(const invalid of [row(-1),{...row(),date:'2026-02-30'},{...row(),harvesters:1.5},{...row(),field:'wrong'}])
   await assert.rejects(store.sync([change(),change(invalid)],'test'));
  assert.equal((await store.get()).revision,0);
  await writeFile(path,'broken'); await assert.rejects(store.sync([change()],'test'));
  assert.equal(await readFile(path,'utf8'),'broken');
 } finally {await rm(dir,{recursive:true,force:true});}
});

test('HTTPS proxy API auth, write scopes, cross-phone download, limits and restart',async()=>{
 const dir=await mkdtemp(`${tmpdir()}/production-api-`); await mkdir(`${dir}/production`);
 const tokenPath=`${dir}/tokens.json`, dataPath=`${dir}/snapshot.json`;
 const hash=x=>createHash('sha256').update(x).digest('hex');
 await writeFile(tokenPath,JSON.stringify([{id:'reader',sha256:hash('reader')},{id:'writer',sha256:hash('writer'),scopes:['production:write']} ]));
 const server=createEstateServer({tokenPath,dataPath}); await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const url=`http://127.0.0.1:${server.address().port}/v1/estates/selendang-3/production`;
 const post=(changes,token='writer')=>fetch(url,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({schemaVersion:1,estateId:'selendang-3',changes})});
 try {
  assert.equal((await fetch(url)).status,401); assert.equal((await post([change()],'reader')).status,403);
  const first=change(); assert.equal((await post([first])).status,200);
  const downloaded=await (await fetch(url,{headers:{Authorization:'Bearer reader'}})).json(); assert.equal(downloaded.records.length,1);
  assert.equal((await post([change(row(6))])).status,200);
  assert.equal((await post([change({...row(),field:'wrong'})])).status,400);
  assert.equal((await post(Array.from({length:101},()=>change()))).status,400);
  assert.equal((await fetch(url,{method:'DELETE',headers:{Authorization:'Bearer writer'}})).status,405);
  await writeFile(tokenPath,JSON.stringify([{id:'writer',sha256:hash('writer'),scopes:['production:write'],revoked:true}]));
  assert.equal((await post([change()])).status,401);
 } finally {await new Promise(r=>server.close(r));await rm(dir,{recursive:true,force:true});}
});
