import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:planters_book/domain.dart';
import 'package:planters_book/estate_dashboard.dart';

void main() {
  testWidgets('Estate preview shows proposed sections without phone overflow', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: EstateDashboard(entries: const [], onEstateChanged: (_) {}),
          ),
        ),
      ),
    );
    expect(
      find.text('SAMPLE PREVIEW • Illustrative estate and budgets'),
      findsOneWidget,
    );
    for (final title in [
      'Estate Profile',
      'Land Area',
      'Planting Information',
      'Estate-level Settings',
    ]) {
      expect(find.text(title), findsOneWidget);
    }
    await tester.ensureVisible(find.text('Fertilizer programme'));
    await tester.pumpAndSettle();
    expect(find.text('16,000 bags'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'Estate actuals isolate the selected estate and reveal area discrepancy',
    (tester) async {
      final entries = [
        Entry('Estate profile', {
          'estate_id': 'E',
          'name': 'Real Estate',
          'perimeter_ha': '110',
          'planted_ha': '100',
        }),
        Entry('Field profile', {
          'estate_id': 'E',
          'field_id': 'F1',
          'name': 'Field 1',
          'planting_year': '2015',
          'perimeter_ha': '90',
          'planted_ha': '80',
        }),
        Entry('Field profile', {
          'estate_id': 'OTHER',
          'field_id': 'X',
          'name': 'Unrelated',
          'planting_year': '2018',
          'perimeter_ha': '40',
          'planted_ha': '30',
        }),
      ];
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(
              child: EstateDashboard(
                entries: entries,
                selectedEstate: 'E',
                onEstateChanged: (_) {},
              ),
            ),
          ),
        ),
      );
      expect(
        find.text('SAMPLE PREVIEW • Illustrative estate and budgets'),
        findsNothing,
      );
      expect(find.text('1 fields'), findsOneWidget);
      expect(find.text('F1'), findsOneWidget);
      expect(find.text('X'), findsNothing);
      expect(
        find.textContaining('differ from the estate planted area by 20.00 ha'),
        findsOneWidget,
      );
      expect(find.text('16,000 bags'), findsNothing);
      expect(find.text('—'), findsNWidgets(5));
      expect(tester.takeException(), isNull);
    },
  );
}
