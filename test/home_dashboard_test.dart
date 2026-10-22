import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:planters_book/domain.dart';
import 'package:planters_book/home_dashboard.dart';

void main() {
  testWidgets(
    'reference sample has all three operational groups at narrow width',
    (tester) async {
      tester.view.physicalSize = const Size(320, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(child: HomeDashboard(entries: [])),
          ),
        ),
      );
      expect(
        find.text('SAMPLE PREVIEW • Illustrative figures'),
        findsOneWidget,
      );
      expect(find.text('1. Crop Sent to Mill'), findsOneWidget);
      expect(find.text('2. Harvesting Coverage'), findsOneWidget);
      expect(find.text('3. Harvester Productivity'), findsOneWidget);
      expect(find.text('Circle Weeding'), findsOneWidget);
      expect(find.text('Selective Weeding'), findsOneWidget);
      await tester.ensureVisible(find.text('Manuring'));
      await tester.pumpAndSettle();
      expect(find.text('12,500'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'actual estate excludes samples, future dates and summary periods',
    (tester) async {
      final date = DateTime.now().toIso8601String().substring(0, 10);
      final tomorrow = DateTime.now()
          .add(const Duration(days: 1))
          .toIso8601String()
          .substring(0, 10);
      final entries = [
        Entry('Estate profile', {
          'estate_id': 'E',
          'name': 'Actual Estate',
          'perimeter_ha': '110',
          'planted_ha': '100',
        }),
        Entry('Crop production', {
          'estate_id': 'E',
          'field_id': 'F',
          'date': date,
          'period': 'daily',
          'ffb_tonnes': '20',
        }),
        Entry('Crop production', {
          'estate_id': 'E',
          'field_id': 'F',
          'date': date,
          'period': 'monthly',
          'ffb_tonnes': '999',
        }),
        Entry('Crop production', {
          'estate_id': 'E',
          'field_id': 'F',
          'date': tomorrow,
          'period': 'daily',
          'ffb_tonnes': '999',
        }),
        Entry('Crop production', {
          'estate_id': 'E',
          'field_id': 'F',
          'date': date,
          'period': 'daily',
          'ffb_tonnes': '999',
        }, sample: true),
      ];
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(child: HomeDashboard(entries: entries)),
          ),
        ),
      );
      expect(find.text('SAMPLE PREVIEW • Illustrative figures'), findsNothing);
      expect(find.text('0.20'), findsNWidgets(3));
      expect(find.text('93.3'), findsNothing);
      expect(find.text('—'), findsWidgets);
      expect(tester.takeException(), isNull);
    },
  );
}
