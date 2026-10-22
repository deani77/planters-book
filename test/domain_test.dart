import 'dart:convert';

import 'package:excel/excel.dart';

import 'package:flutter_test/flutter_test.dart';
import 'package:planters_book/domain.dart';
import 'package:planters_book/importer.dart';

import 'dart:typed_data';

void main() {
  test('XLSX single sheet imports typed cell values', () {
    final book = Excel.createExcel();
    final sheet = book.tables.values.single;
    sheet.appendRow(
      [
        'estate_id',
        'name',
        'perimeter_ha',
        'planted_ha',
      ].map((v) => TextCellValue(v)).toList(),
    );
    sheet.appendRow([
      TextCellValue('X'),
      TextCellValue('Sample XLSX Estate'),
      IntCellValue(20),
      IntCellValue(18),
    ]);
    final preview = parseImport(
      'sample.xlsx',
      Uint8List.fromList(book.encode()!),
      'Estate profile',
    );
    expect(preview.errors, isEmpty);
    expect(preview.entries.single.data['planted_ha'], '18');
  });

  test('reject impossible dates, negative production and invalid periods', () {
    final row = {
      'estate_id': 'E',
      'field_id': 'F',
      'date': '2026-02-30',
      'period': 'daily',
      'ffb_tonnes': '10',
    };
    expect(validate('Crop production', row), isNotNull);
    row['date'] = '2026-02-28';
    expect(validate('Crop production', row), isNull);
    row['ffb_tonnes'] = '-1';
    expect(validate('Crop production', row), isNotNull);
    row['ffb_tonnes'] = '10';
    row['period'] = 'yearly';
    expect(validate('Crop production', row), isNotNull);
  });
  test('area and quality constraints', () {
    expect(
      validate('Estate profile', {
        'estate_id': 'E',
        'name': 'Estate',
        'perimeter_ha': '5',
        'planted_ha': '6',
      }),
      isNotNull,
    );
    expect(
      validate('FFB quality / OER', {
        'estate_id': 'E',
        'date': '2026-10-22',
        'ripe_pct': '90',
        'unripe_pct': '20',
        'overripe_pct': '0',
        'oer_pct': '20',
      }),
      isNotNull,
    );
  });
  test('CSV and Markdown preview validate without saving', () {
    for (final name in ['sample.csv', 'sample.md']) {
      final text = name.endsWith('.csv')
          ? 'estate_id,name,perimeter_ha,planted_ha\nE,"Estate, One",10,9'
          : '| estate_id | name | perimeter_ha | planted_ha |\n| --- | --- | --- | --- |\n| E | Estate | 10 | 9 |';
      final result = parseImport(
        name,
        Uint8List.fromList(utf8.encode(text)),
        'Estate profile',
      );
      expect(result.errors, isEmpty);
      expect(result.entries.length, 1);
    }
  });
  test('duplicate rows and unexpected headers block import', () {
    final result = parseImport(
      'a.csv',
      Uint8List.fromList(
        utf8.encode(
          'estate_id,name,perimeter_ha,planted_ha\nE,One,10,9\nE,Two,10,9',
        ),
      ),
      'Estate profile',
    );
    expect(result.errors, isNotEmpty);
    expect(
      () => parseImport(
        'a.csv',
        Uint8List.fromList(utf8.encode('wrong\nvalue')),
        'Estate profile',
      ),
      throwsFormatException,
    );
  });
}
