import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:planters_book/store.dart';
import 'package:planters_book/domain.dart';
import 'package:sqflite/sqflite.dart' as sqlite;

void main() {
  sqfliteFfiInit();
  test(
    'SQLite retains data after reopening and rolls back a rejected batch',
    () async {
      final folder = await Directory.systemTemp.createTemp('planters_test_');
      final path = '${folder.path}/book.db';
      sqlite.databaseFactory = databaseFactoryFfi;
      var book = await BookStore.open(path: path);
      final estate = Entry('Estate profile', {
        'estate_id': 'E',
        'name': 'Test Estate',
        'perimeter_ha': '20',
        'planted_ha': '18',
      });
      await book.save([estate]);
      await book.db.close();
      book = await BookStore.open(path: path);
      expect((await book.all()).single.label, 'Test Estate');
      final rain = Entry('Rainfall', {
        'estate_id': 'E',
        'date': '2026-10-22',
        'rainfall_mm': '4',
      });
      final bad = Entry('Rainfall', {
        'estate_id': 'MISSING',
        'date': '2026-10-22',
        'rainfall_mm': '4',
      });
      await expectLater(book.save([rain, bad]), throwsFormatException);
      expect((await book.all()).length, 1);
      await book.save([rain]);
      await expectLater(book.save([rain]), throwsFormatException);
      await book.db.close();
      await folder.delete(recursive: true);
    },
  );
}
