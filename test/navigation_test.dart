import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:sqflite/sqflite.dart' as sqlite;
import 'package:planters_book/main.dart';
import 'package:planters_book/store.dart';

void main() {
  testWidgets(
    'six tabs render at phone width and forms expose all record types',
    (tester) async {
      sqfliteFfiInit();
      sqlite.databaseFactory = databaseFactoryFfi;
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(() => tester.view.resetPhysicalSize());
      final book = await tester.runAsync(
        () => BookStore.open(path: inMemoryDatabasePath),
      );
      await tester.runAsync(() => book!.sample());
      await tester.pumpWidget(PlantersBook(store: book));
      await tester.runAsync(
        () => Future<void>.delayed(const Duration(milliseconds: 500)),
      );
      await tester.pumpAndSettle();
      expect(find.text('Estate Crop Production'), findsOneWidget);
      for (final label in ['Estate', 'Field', 'Input', 'Tools', 'Settings']) {
        await tester.tap(
          find.descendant(
            of: find.byType(NavigationBar),
            matching: find.text(label),
          ),
        );
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
      }
      await tester.pumpWidget(const SizedBox());
    },
  );
}
