import 'dart:convert';

import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

import 'domain.dart';

class BookStore {
  final Database db;
  BookStore(this.db);
  static Future<BookStore> open({String? path}) async => BookStore(
    await openDatabase(
      path ?? p.join(await getDatabasesPath(), 'planters_book.db'),
      version: 1,
      onCreate: (db, v) async {
        await db.execute(
          'CREATE TABLE entries (identity TEXT PRIMARY KEY, kind TEXT NOT NULL, data TEXT NOT NULL, sample INTEGER NOT NULL DEFAULT 0)',
        );
      },
    ),
  );
  Future<List<Entry>> all() async =>
      (await db.query('entries', orderBy: 'kind, identity'))
          .map(
            (r) => Entry(
              r['kind'] as String,
              Map<String, String>.from(jsonDecode(r['data'] as String)),
              sample: r['sample'] == 1,
            ),
          )
          .toList();
  Future<void> save(List<Entry> entries) async {
    await db.transaction((tx) async {
      final existing = (await tx.query('entries'))
          .map(
            (r) => Entry(
              r['kind'] as String,
              Map<String, String>.from(jsonDecode(r['data'] as String)),
            ),
          )
          .toList();
      final combined = [...existing, ...entries];
      for (final e in entries) {
        final error = validate(e.kind, e.data);
        if (error != null) throw FormatException(error);
        if (e.kind != 'Estate profile' &&
            !combined.any(
              (x) =>
                  x.kind == 'Estate profile' &&
                  x.data['estate_id'] == e.data['estate_id'],
            )) {
          throw const FormatException('Estate ID does not exist');
        }
        if (e.kind == 'Crop production' &&
            !combined.any(
              (x) =>
                  x.kind == 'Field profile' &&
                  x.data['estate_id'] == e.data['estate_id'] &&
                  x.data['field_id'] == e.data['field_id'],
            )) {
          throw const FormatException(
            'Field ID does not belong to this estate',
          );
        }
        if (combined.where((x) => x.identity == e.identity).length > 1) {
          throw const FormatException('Duplicate record; no data was saved');
        }
        await tx.insert('entries', {
          'identity': e.identity,
          'kind': e.kind,
          'data': jsonEncode(e.data),
          'sample': e.sample ? 1 : 0,
        });
      }
    });
  }

  Future<void> sample() => save([
    Entry('Estate profile', {
      'estate_id': 'SAMPLE',
      'name': 'Sample Estate',
      'perimeter_ha': '600',
      'planted_ha': '520',
    }, sample: true),
    Entry('Field profile', {
      'estate_id': 'SAMPLE',
      'field_id': 'F01',
      'name': 'Sample Field 01',
      'perimeter_ha': '100',
      'planted_ha': '85',
      'planting_year': '2015',
    }, sample: true),
    Entry('Crop production', {
      'estate_id': 'SAMPLE',
      'field_id': 'F01',
      'date': DateTime.now().toIso8601String().substring(0, 10),
      'period': 'daily',
      'ffb_tonnes': '12.5',
    }, sample: true),
  ]);
}
