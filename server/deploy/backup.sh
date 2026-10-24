#!/bin/bash
set -euo pipefail
umask 077
install -d -o root -g root -m 0700 /var/backups/s3-estate
archive="/var/backups/s3-estate/estate-$(date -u +%Y%m%dT%H%M%SZ).tar.gz"
tar -czf "$archive" -C / var/lib/s3-estate/snapshot.json var/lib/s3-estate/archive etc/s3-estate/tokens.json
