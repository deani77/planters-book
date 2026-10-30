import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

SCRIPT = Path(__file__).resolve().parents[1] / 'estate_zip.py'
spec = importlib.util.spec_from_file_location('estate_zip', SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class EstateZipTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.source = Path(self.directory.name) / 'snapshot.json'
        self.output = Path(self.directory.name) / 'update.zip'
        sample = SCRIPT.parents[1] / 'sample-data/estate-update.example.json'
        self.root = json.loads(sample.read_bytes())
        self.root['revision'] = 2
        self.root['privateExtension'] = {'preserve': 'fictional'}
        self.root['manuringReport'] = None
        self.payload = json.dumps(self.root, indent=2).encode()
        self.source.write_bytes(self.payload)

    def test_preserves_selendang_snapshot_bytes_and_unknown_extensions(self):
        module.package(self.source, self.output)
        with zipfile.ZipFile(self.output) as archive:
            self.assertEqual(set(archive.namelist()), {'manifest.json', 'estate.json'})
            self.assertEqual(archive.read('estate.json'), self.payload)
            manifest = json.loads(archive.read('manifest.json'))
            self.assertEqual(manifest['sha256'], hashlib.sha256(self.payload).hexdigest())
            self.assertEqual(manifest['mode'], 'replace')
            self.assertEqual(manifest['estateId'], self.root['estateId'])

    def test_does_not_overwrite_private_export(self):
        self.output.write_bytes(b'existing private backup')
        with self.assertRaises(FileExistsError):
            module.package(self.source, self.output)
        self.assertEqual(self.output.read_bytes(), b'existing private backup')

    def test_merge_selection_and_explicit_clear(self):
        module.package(self.source, self.output, base_revision=1, sections=['fields'], reports=['manuringReport'])
        with zipfile.ZipFile(self.output) as archive:
            manifest = json.loads(archive.read('manifest.json'))
            self.assertEqual(manifest['baseRevision'], 1)
            self.assertEqual(manifest['mode'], 'merge')
            self.assertEqual(manifest['reports'], ['manuringReport'])

    def test_rejects_invalid_selections_before_creating_file(self):
        for options in [dict(sections=['fields']), dict(base_revision=1),
                        dict(base_revision=2, sections=['fields']),
                        dict(base_revision=1, sections=['fields', 'fields']),
                        dict(base_revision=1, sections=['unknown']),
                        dict(base_revision=1, reports=['cropBudget'])]:
            with self.assertRaises(ValueError):
                module.package(self.source, self.output, **options)
            self.assertFalse(self.output.exists())

    def test_rejects_bad_schema_and_oversized_payload(self):
        for root in [[], {'schemaVersion': 2}, dict(self.root, schemaVersion=True),
                     dict(self.root, revision=1.5), dict(self.root, sections=[])]:
            self.source.write_text(json.dumps(root))
            with self.assertRaises(ValueError):
                module.package(self.source, self.output)
        self.source.write_bytes(b' ' * (2 * 1024 * 1024 + 1))
        with self.assertRaises(ValueError):
            module.package(self.source, self.output)
        self.assertFalse(self.output.exists())


if __name__ == '__main__':
    unittest.main()
