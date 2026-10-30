#!/usr/bin/env python3
"""Wrap an offline Selendang-3/Planters Book schema-v1 snapshot, byte-for-byte."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

SECTIONS = {'overview', 'fields', 'ages', 'soil', 'rainfall', 'abw', 'roads'}
REPORTS = {'yph', 'cropProductionReport', 'cropProductionReportHistory', 'manuringReport', 'cropBudget', 'ffbRejectReport', 'fieldProfiles', 'cropHistory'}


def package(source, destination, *, sections=None, reports=None, base_revision=None):
    payload = Path(source).read_bytes()
    if len(payload) > 2 * 1024 * 1024:
        raise ValueError('Snapshot exceeds 2 MB')
    root = json.loads(payload)
    if (not isinstance(root, dict) or type(root.get('schemaVersion')) is not int or root['schemaVersion'] != 1
            or not isinstance(root.get('estateId'), str) or not root['estateId'].strip()
            or type(root.get('revision')) is not int or root['revision'] <= 0):
        raise ValueError('Expected a schema-v1 estate snapshot')
    tables = root.get('sections', [])
    if (not isinstance(tables, list) or len(tables) != 7
            or not all(isinstance(table, dict) for table in tables)
            or {table.get('id') for table in tables} != SECTIONS):
        raise ValueError('Snapshot must include all seven estate sections')
    manifest = dict(format='planters-book-estate', version=1, payload='estate.json',
                    sha256=hashlib.sha256(payload).hexdigest(), estateId=root['estateId'],
                    revision=root['revision'], mode='replace')
    if base_revision is not None:
        sections, reports = sections or [], reports or []
        if (base_revision <= 0 or base_revision >= root['revision'] or not (sections or reports)
                or len(set(sections)) != len(sections) or not set(sections) <= SECTIONS
                or len(set(reports)) != len(reports) or not set(reports) <= REPORTS
                or not set(reports) <= root.keys()):
            raise ValueError('Invalid merge base or selection; selected reports must be present (null clears)')
        manifest.update(mode='merge', baseRevision=base_revision, sections=sections, reports=reports)
    elif sections or reports:
        raise ValueError('Merge selections need --base-revision')
    # Exclusive creation avoids overwriting an existing private export.
    with zipfile.ZipFile(destination, 'x', compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('manifest.json', json.dumps(manifest, sort_keys=True))
        archive.writestr('estate.json', payload)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('snapshot', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--base-revision', type=int)
    parser.add_argument('--sections', nargs='*', choices=sorted(SECTIONS))
    parser.add_argument('--reports', nargs='*', choices=sorted(REPORTS))
    args = parser.parse_args()
    try:
        package(args.snapshot, args.output, sections=args.sections, reports=args.reports,
                base_revision=args.base_revision)
    except (ValueError, OSError, KeyError, TypeError) as error:
        parser.exit(1, f'Could not create estate ZIP: {error}\n')
    print('Estate ZIP created. Review it in Planters Book before importing. Keep it private.')


if __name__ == '__main__':
    main()
