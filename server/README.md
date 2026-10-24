# S3 Estate Info VPS service

This read-only Node.js service serves the same complete JSON snapshot used by Android file import. Caddy provides public HTTPS. The API binds only to `127.0.0.1:8787`; it does not expose a database port. No third-party Node dependencies are required.

## API

- `GET /health`: public service health, no estate records.
- `GET /v1/estates/selendang-3/snapshot`: requires `Authorization: Bearer <read-only token>`, returns JSON only after validation.
- No public uploads, writes, account registration or administration endpoints.
- Unknown/revoked tokens receive 401; malformed/unavailable snapshots receive 503. HTTP is redirected to HTTPS by the proxy. Secrets are not logged. A bounded in-memory request limiter applies globally and per authorized token.

Snapshot data, token registry, raw access tokens, TLS keys and backup archives belong outside the repository. `/etc/s3-estate/tokens.json` contains only SHA-256 hashes of randomly generated tokens. The publisher's private files and phone exports must never be committed.

## Server layout

- `/opt/s3-estate/server/`: the `.mjs` source files.
- `/etc/s3-estate/tokens.json`: root:s3estate, mode 0640; array of `{id, sha256, revoked}` records.
- `/var/lib/s3-estate/`: root:s3estate, mode 2750, current snapshot and archived revisions.
- `/etc/s3-estate/Caddyfile`: copy of the proxy configuration.
- `/etc/s3-estate/domain.env`: `ESTATE_DOMAIN=your-public-hostname`.
- `/var/lib/s3-estate-tls/`: root-only persisted certificate state.
- `/var/backups/s3-estate/`: root-only daily local archives.

The systemd API runs as `s3estate`, with a read-only filesystem view and no shell login. The publisher is run separately by the server administrator. Existing unrelated services must not be stopped or reconfigured. The proxy requires free ports 80/443 and a hostname whose public A/AAAA records point to this VPS. Verify IPv6 too if an AAAA record exists.

## Administrator actions

Validate and atomically publish a prepared complete snapshot, archiving the previous revision:

```sh
sudo node /opt/s3-estate/server/publish.mjs /path/to/private-update.json
```

The revision must increase. Structural checks do not certify the source figures. Publishing a snapshot does not automatically push it to phones; users select Sync Now, review it and confirm the import.

Issue a separate staff token, saved to a private file rather than stdout:

```sh
sudo node /opt/s3-estate/server/tokens.mjs issue staff-01 /root/staff-01-access.txt
sudo node /opt/s3-estate/server/tokens.mjs revoke staff-01
```

Distribute staff tokens privately. Revocation takes effect on subsequent requests and does not erase previously downloaded records from phones. This is read-only token access, not an account/password administration portal.

## Operations and backups

```sh
systemctl status s3-estate
systemctl status s3-estate-backup.timer
systemctl start s3-estate-backup.service
node --test /opt/s3-estate/server/server.test.mjs
```

`backup.sh` creates daily archives of the current snapshot, previous revisions and hashed access registry. Archives are retained; monitor disk usage. Keep an additional copy outside this VPS. Local scheduled backups do not protect against termination of the VPS service. The initial deployment includes a one-time local-machine copy; automatic off-server backup needs a separate destination.

Before termination, export the current snapshot and copy the server backups to a private external location. Phones can continue using and importing JSON files without this server. For migration, retain the estate ID, schema and revision sequence, and update the server address in Settings if the hostname changes. Provider-assigned hostnames should be treated as tied to the VPS; use your own domain for longer-term portability.

## Tests and limitations

Run `node --test server/server.test.mjs` from this folder. Tests cover authorized downloads, anonymous/wrong/revoked-token rejection, write rejection and invalid data handling. Real certificates and external routing must be checked after deployment. The initial server snapshot has seven empty sections marked awaiting verified records. The web administration/upload page and automated source conversion are not yet implemented.

### Production writes

Create /var/lib/s3-estate/production owned by s3estate:s3estate, mode 0700. Deploy production.mjs with server.mjs and the updated systemd unit allowing writes only in that directory. Add this directory to the existing private backup script and back it up before upgrades. Grant production:write only to authorized token IDs using tokens.mjs grant-production-write ID. All existing tokens otherwise remain read-only. The HTTPS production endpoint is derived from the snapshot endpoint's origin and uses the same certificate and bearer token. Do not reverse-proxy it without authentication or expose the production JSON files as static downloads.
