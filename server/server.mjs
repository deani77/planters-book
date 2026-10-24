import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {createHash, timingSafeEqual} from 'node:crypto';
import {pathToFileURL} from 'node:url';
import {join, dirname} from 'node:path';
import {productionStore} from './production.mjs';
import {MAX_BYTES, validateSnapshot} from './validate.mjs';

const digest = token => createHash('sha256').update(token).digest('hex');

export function createEstateServer({dataPath, tokenPath, productionPath = join(dirname(dataPath), "production", "records.json")}) {
  const production = productionStore(productionPath);
  const buckets = new Map();
  function allow(key, maximum) {
    const now = Date.now();
    for (const [k, b] of buckets) if (b.until <= now) buckets.delete(k);
    const bucket = buckets.get(key) ?? {count: 0, until: now + 60000};
    if (!buckets.has(key) && buckets.size >= 2000) return false;
    buckets.set(key, bucket);
    return ++bucket.count <= maximum;
  }
  const server = http.createServer({maxHeaderSize: 8192}, async (req, res) => {
    res.setHeader('Cache-Control', 'no-store');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    const reply = (code, body) => {res.writeHead(code, {'Content-Type': 'application/json'}); res.end(JSON.stringify(body));};
    const productionRequest = req.url === '/v1/estates/selendang-3/production';
    if (req.method !== 'GET' && !(productionRequest && req.method === 'POST')) return reply(405, {error: 'Method not allowed'});
    if (req.url === '/health') return reply(200, {status: 'ok'});
    if (!productionRequest && req.url !== '/v1/estates/selendang-3/snapshot') return reply(404, {error: 'Not found'});
    // Bound total traffic even if authorization headers are continually changed.
    if (!allow('global', 300)) return reply(429, {error: 'Try again later'});
    const header = req.headers.authorization ?? '';
    let tokens;
    try {
      tokens = JSON.parse(await readFile(tokenPath, 'utf8'));
      if (!Array.isArray(tokens)) throw new Error('Invalid token registry');
    }
    catch {return reply(503, {error: 'Service not configured'});}
    const hash = header.startsWith('Bearer ') ? digest(header.slice(7)) : '';
    const entry = tokens.find(t => t && !t.revoked && typeof t.sha256 === 'string' &&
      /^[a-f0-9]{64}$/.test(t.sha256) && hash.length === 64 &&
      timingSafeEqual(Buffer.from(t.sha256, 'hex'), Buffer.from(hash, 'hex')));
    if (!entry) return reply(401, {error: 'Access token required'});
    if (!allow(hash, 30)) return reply(429, {error: 'Try again later'});
    if (productionRequest) {
      try {
        if (req.method === 'GET') return reply(200, await production.get());
        if (!entry.scopes?.includes('production:write')) return reply(403, {error:'Production write permission required'});
        if (!req.headers['content-type']?.startsWith('application/json')) return reply(415, {error:'Use JSON'});
        let bytes = 0, chunks = [];
        for await (const chunk of req) {
          bytes += chunk.length;
          if (bytes > MAX_BYTES) return reply(413,{error:'Request too large'});
          chunks.push(chunk);
        }
        let body;
        try { body = JSON.parse(Buffer.concat(chunks).toString('utf8')); }
        catch { return reply(400,{error:'Invalid JSON'}); }
        if (body.schemaVersion !== 1 || body.estateId !== 'selendang-3') return reply(400,{error:'Wrong estate or schema'});
        return reply(200,await production.sync(body.changes, entry.id));
      } catch (e) { return reply(e.status ?? 503,{error:e.status ? e.message : 'Production unavailable; saved data retained'}); }
    }
    try {
      const bytes = await readFile(dataPath);
      if (bytes.length > MAX_BYTES) throw new Error('Oversize');
      validateSnapshot(bytes);
      res.writeHead(200, {'Content-Type': 'application/json; charset=utf-8'});
      res.end(bytes);
    } catch {reply(503, {error: 'Estate data not available'});}
  });
  server.requestTimeout = 15000;
  server.headersTimeout = 10000;
  server.keepAliveTimeout = 5000;
  server.maxConnections = 50;
  return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const server = createEstateServer({
    dataPath: process.env.DATA_PATH ?? '/var/lib/s3-estate/snapshot.json',
    tokenPath: process.env.TOKEN_PATH ?? '/etc/s3-estate/tokens.json',
  });
  server.listen(8787, '127.0.0.1', () => console.log('Estate data service ready on loopback'));
  process.on('SIGTERM', () => server.close(() => process.exit(0)));
}
