import 'package:flutter/material.dart';

import 'domain.dart';

const _ink = Color(0xff294d3d);
const _muted = Color(0xff78867b);
const _panel = Color(0xfff1f6f2);
const _green = Color(0xff388653);

/// Read-only operational Home, following the shared 22 October mockup.
class HomeDashboard extends StatefulWidget {
  final List<Entry> entries;
  const HomeDashboard({super.key, required this.entries});
  @override
  State<HomeDashboard> createState() => _HomeDashboardState();
}

class _HomeDashboardState extends State<HomeDashboard> {
  String? selected;
  bool preview = false;
  String number(double? value, [int digits = 2]) {
    if (value == null) return '—';
    final parts = value.toStringAsFixed(digits).split('.');
    parts[0] = parts[0].replaceAllMapped(
      RegExp(r'\B(?=(\d{3})+(?!\d))'),
      (_) => ',',
    );
    return parts.join('.');
  }

  Widget label(String text) =>
      Text(text, style: const TextStyle(fontSize: 10, color: _muted));
  Widget metric(
    String title,
    String value,
    String unit, {
    bool amber = false,
  }) => Expanded(
    child: Column(
      children: [
        label(title),
        const SizedBox(height: 5),
        Text(
          value,
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: amber ? const Color(0xffa77a27) : _ink,
          ),
        ),
        const SizedBox(height: 3),
        label(unit),
      ],
    ),
  );
  Widget line() => const Padding(
    padding: EdgeInsets.symmetric(vertical: 16),
    child: Divider(height: 1, color: Color(0xffdce4dd)),
  );
  Widget panel(List<Widget> children) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(13),
    decoration: BoxDecoration(
      color: _panel,
      borderRadius: BorderRadius.circular(13),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: children,
    ),
  );
  Widget section(IconData icon, String title, {bool major = false}) => Padding(
    padding: const EdgeInsets.only(top: 18, bottom: 12),
    child: Row(
      children: [
        Icon(icon, size: 17, color: _green),
        const SizedBox(width: 9),
        Expanded(
          child: Text(
            title,
            style: TextStyle(
              fontSize: 16,
              fontWeight: major ? FontWeight.w700 : FontWeight.w500,
              color: _ink,
            ),
          ),
        ),
      ],
    ),
  );
  Widget pair(
    String title,
    String value, {
    String unit = '',
    bool bold = false,
  }) => Row(
    mainAxisAlignment: MainAxisAlignment.spaceBetween,
    children: [
      Expanded(
        child: Text(title, style: const TextStyle(fontSize: 13, color: _ink)),
      ),
      Text(
        value,
        style: TextStyle(
          fontSize: 17,
          color: _green,
          fontWeight: FontWeight.w700,
        ),
      ),
      if (unit.isNotEmpty)
        Text(' $unit', style: const TextStyle(fontSize: 11, color: _muted)),
    ],
  );
  Widget productionPeriod(
    String title,
    String actual,
    String budget,
    String achievement,
  ) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(title, style: const TextStyle(fontSize: 13, color: _ink)),
      const SizedBox(height: 12),
      Row(
        children: [
          metric('Actual', actual, 't/ha'),
          metric('Budget', budget, 't/ha'),
          metric('Achievement', achievement, '%', amber: true),
        ],
      ),
    ],
  );
  Widget productivityRow(
    String name,
    String today,
    String mtd, {
    bool header = false,
  }) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 5),
    child: Row(
      children: [
        Expanded(
          flex: 2,
          child: Text(
            name,
            style: TextStyle(
              fontSize: header ? 10 : 12,
              color: header ? _muted : _ink,
            ),
          ),
        ),
        Expanded(
          child: Text(
            today,
            textAlign: TextAlign.right,
            style: TextStyle(
              fontSize: header ? 10 : 13,
              color: header ? _muted : _ink,
            ),
          ),
        ),
        Expanded(
          child: Text(
            mtd,
            textAlign: TextAlign.right,
            style: TextStyle(
              fontSize: header ? 10 : 13,
              color: header ? _muted : _ink,
            ),
          ),
        ),
      ],
    ),
  );
  Widget weeding(String title, List<String> values) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(title, style: const TextStyle(fontSize: 14, color: _ink)),
      const SizedBox(height: 12),
      Row(
        children: [
          metric('This Month', values[0], 'ha'),
          metric('YTD', values[1], 'ha'),
          metric('YTD Rounds', values[2], 'rounds'),
        ],
      ),
    ],
  );
  @override
  Widget build(BuildContext context) {
    final estates = widget.entries
        .where((e) => e.kind == 'Estate profile' && !e.sample)
        .toList();
    final sample = preview || estates.isEmpty;
    final estate =
        estates.where((e) => e.data['estate_id'] == selected).firstOrNull ??
        estates.firstOrNull;
    final now = DateTime.now();
    final day = DateTime(now.year, now.month, now.day);
    final month = DateTime(now.year, now.month);
    final year = DateTime(now.year);
    final data = widget.entries.where(
      (e) => !e.sample && e.data['estate_id'] == estate?.data['estate_id'],
    );
    final area = double.tryParse(estate?.data['planted_ha'] ?? '');
    double? sum(String kind, String key, DateTime since, {String? method}) {
      final rows = data.where((e) {
        final date = DateTime.tryParse(e.data['date'] ?? '');
        return e.kind == kind &&
            date != null &&
            !date.isBefore(since) &&
            !date.isAfter(day) &&
            (kind != 'Crop production' || e.data['period'] == 'daily') &&
            (method == null ||
                (e.data['method'] ?? '').trim().toLowerCase() == method);
      }).toList();
      if (rows.isEmpty) return null;
      return rows.fold<double>(
        0,
        (total, e) => total + (double.tryParse(e.data[key] ?? '') ?? 0),
      );
    }

    String yieldSince(DateTime since) {
      final tonnes = sum('Crop production', 'ffb_tonnes', since);
      return number(
        tonnes == null || area == null || area <= 0 ? null : tonnes / area,
      );
    }

    List<String> weedValues(String method) {
      final m = sum('Weeding', 'area_ha', month, method: method);
      final y = sum('Weeding', 'area_ha', year, method: method);
      return [
        number(m),
        number(y),
        number(y == null || area == null || area <= 0 ? null : y / area),
      ];
    }

    final months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];
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
            style: TextStyle(fontSize: 10, letterSpacing: .7, color: _muted),
          ),
          const SizedBox(height: 5),
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Estate Crop Production',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                    color: _ink,
                  ),
                ),
              ),
              Icon(Icons.eco_outlined, color: _green, size: 22),
            ],
          ),
          const SizedBox(height: 7),
          Text(
            '${sample ? 'Selendang 03' : estate!.label} · ${now.day} ${months[now.month - 1]} ${now.year}',
            style: const TextStyle(fontSize: 12, color: _muted),
          ),
          const SizedBox(height: 12),
          if (sample)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 7),
              decoration: BoxDecoration(
                color: const Color(0xfffaf2df),
                borderRadius: BorderRadius.circular(6),
              ),
              child: const Text(
                'SAMPLE PREVIEW • Illustrative figures',
                style: TextStyle(fontSize: 11, color: Color(0xff8d6824)),
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
                    onChanged: (value) => setState(() {
                      selected = value;
                      preview = false;
                    }),
                  ),
                ),
                TextButton(
                  onPressed: () => setState(() => preview = !preview),
                  child: Text(sample ? 'View actual' : 'Sample preview'),
                ),
              ],
            ),
          line(),
          section(Icons.local_shipping_outlined, '1. Crop Sent to Mill'),
          panel([
            pair('Today', sample ? '0.08' : yieldSince(day), unit: 't/ha'),
            line(),
            productionPeriod(
              'Month to Date (MTD)',
              sample ? '1.54' : yieldSince(month),
              sample ? '1.65' : '—',
              sample ? '93.3' : '—',
            ),
            line(),
            productionPeriod(
              'Year to Date (YTD)',
              sample ? '15.20' : yieldSince(year),
              sample ? '15.80' : '—',
              sample ? '96.2' : '—',
            ),
            line(),
            pair('YTD vs Annual Budget', sample ? '84.4%' : '—'),
          ]),
          section(Icons.park_outlined, '2. Harvesting Coverage'),
          Row(
            children: [
              Expanded(
                child: panel([
                  Center(child: label('Today')),
                  const SizedBox(height: 6),
                  Center(
                    child: Text(
                      sample ? '185.50' : '—',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: _ink,
                      ),
                    ),
                  ),
                  const SizedBox(height: 3),
                  Center(child: label('ha')),
                ]),
              ),
              const SizedBox(width: 7),
              Expanded(
                child: panel([
                  Center(child: label('MTD')),
                  const SizedBox(height: 6),
                  Center(
                    child: Text(
                      sample ? '3,750' : '—',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: _ink,
                      ),
                    ),
                  ),
                  const SizedBox(height: 3),
                  Center(child: label('ha')),
                ]),
              ),
              const SizedBox(width: 7),
              Expanded(
                child: panel([
                  Center(child: label('Rounds')),
                  const SizedBox(height: 6),
                  Center(
                    child: Text(
                      sample ? '2.50' : '—',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: _ink,
                      ),
                    ),
                  ),
                  const SizedBox(height: 3),
                  Center(child: label('MTD')),
                ]),
              ),
            ],
          ),
          section(Icons.groups_outlined, '3. Harvester Productivity'),
          panel([
            productivityRow('Indicator', 'Today', 'MTD', header: true),
            productivityRow(
              'Attendance',
              sample ? '83' : '—',
              sample ? '1,660' : '—',
            ),
            productivityRow(
              'Ha / Harvester',
              sample ? '2.23' : '—',
              sample ? '2.26' : '—',
            ),
            productivityRow(
              'Tonnes / Harvester',
              sample ? '1.42' : '—',
              sample ? '1.36' : '—',
            ),
            const SizedBox(height: 4),
            const Text(
              'Attendance: workers today; worker-days MTD',
              style: TextStyle(fontSize: 10, color: _muted),
            ),
          ]),
          line(),
          section(Icons.grass_outlined, 'Weeding', major: true),
          panel([
            weeding(
              'Circle Weeding',
              sample ? ['125.50', '1,450.00', '3.20'] : weedValues('circle'),
            ),
            line(),
            weeding(
              'Selective Weeding',
              sample ? ['85.20', '920.00', '2.10'] : weedValues('selective'),
            ),
          ]),
          line(),
          section(Icons.inventory_2_outlined, 'Manuring', major: true),
          panel([
            const Text(
              'Fertilizer Applied',
              style: TextStyle(fontSize: 14, color: _ink),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                metric('This Month', sample ? '1,200' : '—', 'bags'),
                metric('MTD', sample ? '850' : '—', 'bags'),
                metric('YTD', sample ? '12,500' : '—', 'bags'),
              ],
            ),
            line(),
            pair('YTD vs Annual Programme', sample ? '78.1%' : '—'),
            const SizedBox(height: 10),
            if (sample)
              const LinearProgressIndicator(
                value: .781,
                minHeight: 7,
                borderRadius: BorderRadius.all(Radius.circular(8)),
                color: _green,
                backgroundColor: Color(0xffdfe8dc),
              ),
          ]),
          const SizedBox(height: 16),
          Text(
            sample
                ? 'Design preview only. These figures are illustrative and are not saved as estate records. “This Month” in Manuring follows the reference’s provisional monthly planned bags; MTD means applied bags.'
                : '— means data is not available. Crop actuals use recorded daily FFB tonnes ÷ estate planted hectares. Weeding methods: circle / selective. Budgets, harvesting coverage, harvester-specific attendance and fertilizer bag programmes need additional input fields.',
            style: const TextStyle(fontSize: 11, color: _muted),
          ),
        ],
      ),
    );
  }
}
