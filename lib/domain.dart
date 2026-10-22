import 'dart:convert';

const schemas = <String, List<String>>{
  'Estate profile': ['estate_id', 'name', 'perimeter_ha', 'planted_ha'],
  'Field profile': [
    'estate_id',
    'field_id',
    'name',
    'perimeter_ha',
    'planted_ha',
    'planting_year',
  ],
  'Crop production': ['estate_id', 'field_id', 'date', 'period', 'ffb_tonnes'],
  'Weeding': ['estate_id', 'date', 'area_ha', 'method', 'cost_rm'],
  'Manuring': [
    'estate_id',
    'date',
    'area_ha',
    'fertilizer',
    'quantity_kg',
    'cost_rm',
  ],
  'Rainfall': ['estate_id', 'date', 'rainfall_mm'],
  'Workforce': ['estate_id', 'date', 'role', 'workers', 'days_worked'],
  'Costs': ['estate_id', 'date', 'category', 'amount_rm', 'notes'],
  'FFB quality / OER': [
    'estate_id',
    'date',
    'ripe_pct',
    'unripe_pct',
    'overripe_pct',
    'oer_pct',
  ],
};
const optionalFields = {'notes'};
bool numeric(String key) =>
    RegExp(r'(_ha|_tonnes|_rm|_kg|_mm|_pct)$').hasMatch(key) ||
    ['planting_year', 'workers', 'days_worked'].contains(key);
String? validate(String kind, Map<String, String> row) {
  for (final key in schemas[kind]!) {
    final value = row[key]?.trim() ?? '';
    if (value.isEmpty && !optionalFields.contains(key)) {
      return '$key is required';
    }
    if (numeric(key)) {
      final n = double.tryParse(value);
      if (n == null || !n.isFinite || n < 0) {
        return '$key must be a non-negative number';
      }
      if (key.endsWith('_pct') && n > 100) return '$key must be 0–100';
      if (['workers', 'planting_year'].contains(key) &&
          n != n.roundToDouble()) {
        return '$key must be a whole number';
      }
      if (key == 'planting_year' && (n < 1900 || n > DateTime.now().year)) {
        return 'Invalid planting year';
      }
    }
    if (key == 'date') {
      final d = DateTime.tryParse(value);
      if (!RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(value) ||
          d == null ||
          d.toIso8601String().substring(0, 10) != value) {
        return 'Use a valid YYYY-MM-DD date';
      }
    }
    if (key == 'period' && !['daily', 'weekly', 'monthly'].contains(value)) {
      return 'period must be daily, weekly or monthly';
    }
  }
  if (row.containsKey('planted_ha') &&
      double.parse(row['planted_ha']!) > double.parse(row['perimeter_ha']!)) {
    return 'Planted area exceeds perimeter area';
  }
  if (kind == 'FFB quality / OER' &&
      [
            'ripe_pct',
            'unripe_pct',
            'overripe_pct',
          ].fold<double>(0, (a, k) => a + double.parse(row[k]!)) >
          100.001) {
    return 'Quality percentages exceed 100%';
  }
  return null;
}

class Entry {
  final String kind;
  final Map<String, String> data;
  final bool sample;
  Entry(this.kind, this.data, {this.sample = false});
  String get identity => jsonEncode([
    kind,
    ...([
      'estate_id',
      'field_id',
      'date',
      'period',
      'role',
      'category',
    ].where(data.containsKey).map((k) => data[k])),
  ]);
  String get label => data['name'] ?? data['date'] ?? kind;
}
