import 'package:flutter/material.dart';

import 'domain.dart';

const _estateInk = Color(0xff294d3d);
const _estateMuted = Color(0xff78867b);
const _estatePanel = Color(0xfff1f6f2);

/// Estate overview proposal. Sample values are never persisted.
class EstateDashboard extends StatefulWidget {
  final List<Entry> entries;
  final String? selectedEstate;
  final ValueChanged<String?> onEstateChanged;
  const EstateDashboard({
    super.key,
    required this.entries,
    this.selectedEstate,
    required this.onEstateChanged,
  });
  @override
  State<EstateDashboard> createState() => _EstateDashboardState();
}

class _EstateDashboardState extends State<EstateDashboard> {
  bool samplePreview = false;
  Widget section(IconData icon, String title) => Padding(
    padding: const EdgeInsets.only(top: 22, bottom: 12),
    child: Row(
      children: [
        Icon(icon, size: 18, color: const Color(0xff388653)),
        const SizedBox(width: 9),
        Expanded(
          child: Text(
            title,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: _estateInk,
            ),
          ),
        ),
      ],
    ),
  );
  Widget panel(List<Widget> children) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: _estatePanel,
      borderRadius: BorderRadius.circular(13),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: children,
    ),
  );
  Widget rule() => const Padding(
    padding: EdgeInsets.symmetric(vertical: 11),
    child: Divider(height: 1, color: Color(0xffdce4dd)),
  );
  Widget row(String title, String value, {String? note}) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Text(
                title,
                style: const TextStyle(fontSize: 12, color: _estateMuted),
              ),
            ),
            const SizedBox(width: 10),
            Flexible(
              child: Text(
                value,
                textAlign: TextAlign.right,
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: _estateInk,
                ),
              ),
            ),
          ],
        ),
        if (note != null)
          Padding(
            padding: const EdgeInsets.only(top: 5),
            child: Text(
              note,
              style: const TextStyle(fontSize: 11, color: _estateMuted),
            ),
          ),
      ],
    ),
  );
  Widget tile(String title, String value, String unit) => Expanded(
    child: Column(
      children: [
        Text(title, style: const TextStyle(fontSize: 10, color: _estateMuted)),
        const SizedBox(height: 7),
        Text(
          value,
          style: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: _estateInk,
          ),
        ),
        const SizedBox(height: 3),
        Text(unit, style: const TextStyle(fontSize: 10, color: _estateMuted)),
      ],
    ),
  );
  @override
  Widget build(BuildContext context) {
    final estates = widget.entries
        .where((e) => e.kind == 'Estate profile' && !e.sample)
        .toList();
    final sample = estates.isEmpty || samplePreview;
    final estate =
        estates
            .where((e) => e.data['estate_id'] == widget.selectedEstate)
            .firstOrNull ??
        estates.firstOrNull;
    final fields = sample
        ? [
            Entry('Field profile', {
              'field_id': 'F01',
              'planting_year': '2012',
              'perimeter_ha': '495',
              'planted_ha': '450',
            }),
            Entry('Field profile', {
              'field_id': 'F02',
              'planting_year': '2016',
              'perimeter_ha': '660',
              'planted_ha': '600',
            }),
            Entry('Field profile', {
              'field_id': 'F03',
              'planting_year': '2020',
              'perimeter_ha': '495',
              'planted_ha': '450',
            }),
          ]
        : widget.entries
              .where(
                (e) =>
                    e.kind == 'Field profile' &&
                    !e.sample &&
                    e.data['estate_id'] == estate?.data['estate_id'],
              )
              .toList();
    final perimeter = sample
        ? 1650.0
        : double.parse(estate!.data['perimeter_ha']!);
    final planted = sample ? 1500.0 : double.parse(estate!.data['planted_ha']!);
    final fieldArea = fields.fold<double>(
      0,
      (sum, e) => sum + double.parse(e.data['planted_ha']!),
    );
    final year = DateTime.now().year;
    String fmt(double value) => value
        .toStringAsFixed(2)
        .replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => ',');
    final ages = <int, double>{};
    for (final field in fields) {
      final plantingYear = int.parse(field.data['planting_year']!);
      ages.update(
        plantingYear,
        (area) => area + double.parse(field.data['planted_ha']!),
        ifAbsent: () => double.parse(field.data['planted_ha']!),
      );
    }
    final years = ages.keys.toList()..sort();
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(21),
        border: Border.all(color: const Color(0xffe0e9df), width: 5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'PLANTERS BOOK',
            style: TextStyle(
              fontSize: 10,
              letterSpacing: .7,
              color: _estateMuted,
            ),
          ),
          const SizedBox(height: 5),
          const Row(
            children: [
              Expanded(
                child: Text(
                  'Estate Overview',
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                    color: _estateInk,
                  ),
                ),
              ),
              Icon(Icons.landscape_outlined, color: Color(0xff388653)),
            ],
          ),
          const SizedBox(height: 7),
          Text(
            sample ? 'Selendang 03' : estate!.label,
            style: const TextStyle(fontSize: 13, color: _estateMuted),
          ),
          if (sample)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(9),
                decoration: BoxDecoration(
                  color: const Color(0xfffaf2df),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: const Text(
                  'SAMPLE PREVIEW • Illustrative estate and budgets',
                  style: TextStyle(fontSize: 11, color: Color(0xff8d6824)),
                ),
              ),
            ),
          if (estates.isNotEmpty)
            Row(
              children: [
                Expanded(
                  child: DropdownButton<String>(
                    isExpanded: true,
                    value: estate!.data['estate_id'],
                    items: estates
                        .map(
                          (e) => DropdownMenuItem(
                            value: e.data['estate_id'],
                            child: Text(
                              e.label,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        )
                        .toList(),
                    onChanged: (v) {
                      setState(() => samplePreview = false);
                      widget.onEstateChanged(v);
                    },
                  ),
                ),
                TextButton(
                  onPressed: () =>
                      setState(() => samplePreview = !samplePreview),
                  child: Text(sample ? 'View actual' : 'Sample preview'),
                ),
              ],
            ),
          section(Icons.badge_outlined, 'Estate Profile'),
          panel([
            row('Estate name', sample ? 'Selendang 03' : estate!.label),
            rule(),
            row('Estate code', sample ? 'SEL03' : estate!.data['estate_id']!),
            rule(),
            row('Registered fields', '${fields.length} fields'),
            rule(),
            row(
              'Profile entries',
              'Input → Estate profile',
              note: 'Estate and field profiles are entered through Input. This page is a read-only overview.',
            ),
          ]),
          section(Icons.map_outlined, 'Land Area'),
          panel([
            Row(
              children: [
                tile('Perimeter', fmt(perimeter), 'ha'),
                tile('Planted', fmt(planted), 'ha'),
              ],
            ),
            rule(),
            row(
              'Balance within perimeter',
              '${fmt(perimeter - planted)} ha',
              note: 'Perimeter less planted area; land use is not yet classified.',
            ),
            rule(),
            row('Planted area registered in fields', '${fmt(fieldArea)} ha'),
            if ((fieldArea - planted).abs() > .01)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  'Field areas differ from the estate planted area by ${fmt((fieldArea - planted).abs())} ha. Check the profiles in Input.',
                  style: const TextStyle(
                    fontSize: 11,
                    color: Color(0xff8d6824),
                  ),
                ),
              ),
          ]),
          section(Icons.park_outlined, 'Planting Information'),
          if (fields.isEmpty)
            panel([
              const Text(
                'No fields registered. Add field profiles in Input to see planting years and age distribution.',
                style: TextStyle(fontSize: 12, color: _estateMuted),
              ),
            ])
          else ...[
            panel([
              const Text(
                'Age profile by planting year',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: _estateInk,
                ),
              ),
              const SizedBox(height: 12),
              ...years.map(
                (y) => Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    children: [
                      row('$y · ${year - y} years', '${fmt(ages[y]!)} ha'),
                      const SizedBox(height: 5),
                      LinearProgressIndicator(
                        value: fieldArea > 0 ? ages[y]! / fieldArea : 0,
                        minHeight: 5,
                        borderRadius: BorderRadius.circular(5),
                        color: const Color(0xff388653),
                        backgroundColor: const Color(0xffdfe8dc),
                      ),
                    ],
                  ),
                ),
              ),
              const Text(
                'Calendar age; based on registered field planted hectares.',
                style: TextStyle(fontSize: 10, color: _estateMuted),
              ),
            ]),
            const SizedBox(height: 12),
            const Text(
              'Field planting summary',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _estateInk,
              ),
            ),
            const SizedBox(height: 5),
            const Text(
              'Swipe across for hectares →',
              style: TextStyle(fontSize: 10, color: _estateMuted),
            ),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: DataTable(
                horizontalMargin: 8,
                columnSpacing: 18,
                headingTextStyle: const TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                  color: _estateInk,
                ),
                dataTextStyle: const TextStyle(fontSize: 12, color: _estateInk),
                columns: const [
                  'Field',
                  'Planting year',
                  'Age',
                  'Perimeter ha',
                  'Planted ha',
                ].map((t) => DataColumn(label: Text(t))).toList(),
                rows: fields
                    .map(
                      (e) => DataRow(
                        cells: [
                          DataCell(Text(e.data['field_id']!)),
                          DataCell(Text(e.data['planting_year']!)),
                          DataCell(
                            Text(
                              '${year - int.parse(e.data['planting_year']!)}',
                            ),
                          ),
                          DataCell(
                            Text(fmt(double.parse(e.data['perimeter_ha']!))),
                          ),
                          DataCell(
                            Text(fmt(double.parse(e.data['planted_ha']!))),
                          ),
                        ],
                      ),
                    )
                    .toList(),
              ),
            ),
          ],
          section(Icons.track_changes_outlined, 'Annual Budgets · $year'),
          panel([
            row(
              'FFB production',
              sample ? '27,000 tonnes' : '—',
              note: sample
                  ? 'Equivalent yield: 18.00 t/ha using 1,500 planted ha.'
                  : null,
            ),
            rule(),
            row('Circle weeding programme', sample ? '4,500 ha' : '—'),
            rule(),
            row('Selective weeding programme', sample ? '3,000 ha' : '—'),
            rule(),
            row('Fertilizer programme', sample ? '16,000 bags' : '—'),
            rule(),
            row('Operating cost budget', sample ? 'RM 2,400,000' : '—'),
            const SizedBox(height: 10),
            Text(
              sample
                  ? 'Proposed budget categories for review. Sample values are not saved and do not alter Home actuals.'
                  : 'Annual budget entry is not available yet. These categories are proposed for the next Input update.',
              style: const TextStyle(fontSize: 11, color: _estateMuted),
            ),
          ]),
          section(Icons.tune_outlined, 'Estate-level Settings'),
          panel([
            row('Budget reporting period', 'Calendar year'),
            rule(),
            row('Area basis for crop yield', 'Estate planted hectares'),
            rule(),
            row(
              'Preferences',
              'Design pending',
              note: 'Estate-specific defaults will be defined after this screen is reviewed. Language, units, backups and device sharing belong in Settings.',
            ),
          ]),
          const SizedBox(height: 16),
          const Text(
            'Estate overview proposal • Sample data is for design review only.',
            style: TextStyle(fontSize: 11, color: _estateMuted),
          ),
        ],
      ),
    );
  }
}
