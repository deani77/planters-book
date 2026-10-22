import 'dart:convert';
import 'dart:typed_data';

import 'package:csv/csv.dart';
import 'package:excel/excel.dart';

import 'domain.dart';

class ImportPreview {
  final List<Entry> entries;
  final List<String> errors;
  ImportPreview(this.entries, this.errors);
}

ImportPreview parseImport(String name, Uint8List bytes, String kind) {
  List<List<dynamic>> rows;
  if (name.toLowerCase().endsWith('.xlsx')) {
    final book = Excel.decodeBytes(bytes);
    if (book.tables.length != 1) {
      throw const FormatException('Use one worksheet per import');
    }
    rows = book.tables.values.single.rows
        .map((r) => r.map((c) => c?.value?.toString() ?? '').toList())
        .toList();
  } else {
    final text = utf8.decode(bytes).replaceFirst('\uFEFF', '');
    if (name.toLowerCase().endsWith('.md')) {
      rows = text
          .split('\n')
          .where((l) => l.trim().startsWith('|'))
          .map(
            (l) => l
                .trim()
                .replaceFirst(RegExp(r'^\|'), '')
                .replaceFirst(RegExp(r'\|$'), '')
                .split('|')
                .map((v) => v.trim())
                .toList(),
          )
          .where((r) => !r.every((v) => RegExp(r'^:?-+:?$').hasMatch(v)))
          .toList();
    } else if (name.toLowerCase().endsWith('.csv')) {
      rows = const CsvToListConverter(
        shouldParseNumbers: false,
        eol: '\n',
      ).convert(text.replaceAll('\r\n', '\n'));
    } else {
      throw const FormatException('Choose CSV, XLSX or Markdown');
    }
  }
  rows = rows
      .where((r) => r.any((v) => v.toString().trim().isNotEmpty))
      .toList();
  if (rows.length < 2) {
    throw const FormatException(
      'A header and at least one data row are required',
    );
  }
  if (rows.length > 10001) {
    throw const FormatException('Maximum 10,000 rows per import');
  }
  final headers = rows.first.map((v) => v.toString().trim()).toList();
  final expected = schemas[kind]!;
  if (headers.toSet().length != headers.length ||
      expected.any((k) => !headers.contains(k)) ||
      headers.any((k) => !expected.contains(k))) {
    throw FormatException('Headers must match: ${expected.join(', ')}');
  }
  final entries = <Entry>[];
  final errors = <String>[];
  final identities = <String>{};
  for (var i = 1; i < rows.length; i++) {
    final r = rows[i];
    if (r.length != headers.length) {
      errors.add('Row ${i + 1}: wrong number of columns');
      continue;
    }
    final data = {
      for (var c = 0; c < headers.length; c++)
        headers[c]: r[c].toString().trim(),
    };
    final error = validate(kind, data);
    final e = Entry(kind, data);
    if (error != null) {
      errors.add('Row ${i + 1}: $error');
    } else if (!identities.add(e.identity)) {
      errors.add('Row ${i + 1}: duplicate record');
    } else {
      entries.add(e);
    }
  }
  return ImportPreview(entries, errors);
}
