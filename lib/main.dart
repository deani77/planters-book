import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';

import 'domain.dart';
import 'store.dart';
import 'importer.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const PlantersBook());
}

class PlantersBook extends StatelessWidget {
  final BookStore? store;
  const PlantersBook({super.key, this.store});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'Planters Book',
    debugShowCheckedModeBanner: false,
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff246344)),
      useMaterial3: true,
      scaffoldBackgroundColor: const Color(0xfff5f7f2),
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(),
      ),
    ),
    home: BookPage(store: store),
  );
}

class BookPage extends StatefulWidget {
  final BookStore? store;
  const BookPage({super.key, this.store});
  @override
  State<BookPage> createState() => _BookPageState();
}

class _BookPageState extends State<BookPage> {
  BookStore? store;
  List<Entry> entries = [];
  int tab = 0;
  String kind = schemas.keys.first;
  String? estate, field;
  String? startupError;
  final controllers = <String, TextEditingController>{};
  bool busy = false;
  static const tabs = [
    'Home',
    'Estate',
    'Field',
    'Data Input',
    'Tools',
    'Settings',
  ];
  static const icons = [
    Icons.home_outlined,
    Icons.landscape_outlined,
    Icons.grid_view,
    Icons.edit_note,
    Icons.build_outlined,
    Icons.settings_outlined,
  ];
  @override
  void initState() {
    super.initState();
    init();
  }

  Future<void> init() async {
    try {
      store = widget.store ?? await BookStore.open();
      await reload();
    } catch (e) {
      if (mounted) setState(() => startupError = '$e');
    }
  }

  Future<void> reload() async {
    final data = await store!.all();
    if (mounted) setState(() => entries = data);
  }

  @override
  void dispose() {
    for (final c in controllers.values) {
      c.dispose();
    }
    store?.db.close();
    super.dispose();
  }

  void message(Object text) {
    if (mounted) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('$text')));
    }
  }

  Future<void> action(Future<void> Function() fn) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      await fn();
      await reload();
    } catch (e) {
      message(e);
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  TextEditingController control(String key) => controllers.putIfAbsent(
    key,
    () => TextEditingController(
      text: key == 'date'
          ? DateTime.now().toIso8601String().substring(0, 10)
          : key == 'period'
          ? 'daily'
          : '',
    ),
  );
  Widget heading(String title, String subtitle) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 16),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.headlineMedium),
        const SizedBox(height: 6),
        Text(subtitle),
      ],
    ),
  );
  Widget card(String title, String subtitle, {VoidCallback? onTap}) => Card(
    child: ListTile(
      contentPadding: const EdgeInsets.all(20),
      title: Text(
        title,
        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 10),
        child: Text(subtitle),
      ),
      trailing: onTap == null ? null : const Icon(Icons.chevron_right),
      onTap: onTap,
    ),
  );
  String detail(Entry e) => [
    if (e.sample) 'SAMPLE DATA',
    ...e.data.entries.map((p) => '${p.key.replaceAll('_', ' ')}: ${p.value}'),
    if (e.data.containsKey('planting_year'))
      'Age profile: ${DateTime.now().year - int.parse(e.data['planting_year']!)} years (calendar age)',
  ].join('\n');
  List<Entry> get estateProfiles =>
      entries.where((e) => e.kind == 'Estate profile').toList();
  Widget estatePicker() => DropdownButtonFormField<String>(
    initialValue: estate,
    decoration: const InputDecoration(labelText: 'Select estate'),
    items: estateProfiles
        .map(
          (e) => DropdownMenuItem(
            value: e.data['estate_id'],
            child: Text(e.label),
          ),
        )
        .toList(),
    onChanged: (v) => setState(() {
      estate = v;
      field = null;
    }),
  );
  Widget records(Iterable<Entry> data) =>
      Column(children: data.map((e) => card(e.kind, detail(e))).toList());
  Widget dashboard() {
    final real = entries.where((e) => !e.sample).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        heading(
          'Your plantation, at a glance',
          'Offline • read-only dashboard',
        ),
        card(
          'Estate profiles',
          '${real.where((e) => e.kind == 'Estate profile').length} real estates',
        ),
        card(
          'Field profiles',
          '${real.where((e) => e.kind == 'Field profile').length} real fields',
        ),
        card(
          'Daily FFB recorded',
          '${real.where((e) => e.kind == 'Crop production' && e.data['period'] == 'daily').fold<double>(0, (s, e) => s + double.parse(e.data['ffb_tonnes']!)).toStringAsFixed(2)} tonnes across recorded daily entries\nWeekly/monthly summaries are excluded to avoid double counting.',
        ),
        if (entries.any((e) => e.sample))
          card(
            'Sample data loaded',
            'Sample records are labeled and excluded from the totals above.',
          ),
        if (entries.isEmpty)
          card(
            'Start your estate book',
            'Create an estate in Data Input, then add its fields and operational records.',
          ),
      ],
    );
  }

  Widget estateView() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      heading('Estate', 'Select an estate to view its records.'),
      estatePicker(),
      if (estate != null)
        records(entries.where((e) => e.data['estate_id'] == estate)),
    ],
  );
  Widget fieldView() {
    final fields = entries.where(
      (e) => e.kind == 'Field profile' && e.data['estate_id'] == estate,
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        heading('Fields', 'Large field cards • select to view details'),
        estatePicker(),
        const SizedBox(height: 16),
        ...fields.map(
          (e) => SizedBox(
            width: double.infinity,
            child: card(
              e.label,
              '${e.sample ? 'SAMPLE • ' : ''}${e.data['field_id']}\nPlanted ${e.data['planted_ha']} ha / Perimeter ${e.data['perimeter_ha']} ha',
              onTap: () => setState(() => field = e.data['field_id']),
            ),
          ),
        ),
        if (fields.isNotEmpty)
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: DataTable(
              columns: const [
                'Field',
                'Planting year',
                'Age (years)',
                'Perimeter (ha)',
                'Planted (ha)',
              ].map((title) => DataColumn(label: Text(title))).toList(),
              rows: fields
                  .map(
                    (e) => DataRow(
                      cells: [
                        DataCell(Text(e.data['field_id']!)),
                        DataCell(Text(e.data['planting_year']!)),
                        DataCell(
                          Text(
                            '${DateTime.now().year - int.parse(e.data['planting_year']!)}',
                          ),
                        ),
                        DataCell(Text(e.data['perimeter_ha']!)),
                        DataCell(Text(e.data['planted_ha']!)),
                      ],
                    ),
                  )
                  .toList(),
            ),
          ),
        if (field != null)
          records(
            entries.where(
              (e) =>
                  e.data['estate_id'] == estate && e.data['field_id'] == field,
            ),
          ),
      ],
    );
  }

  Widget input() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      heading(
        'Data Input',
        'All new records are entered here. Estate/field IDs link your data.',
      ),
      DropdownButtonFormField<String>(
        initialValue: kind,
        decoration: const InputDecoration(labelText: 'Record type'),
        items: schemas.keys
            .map((k) => DropdownMenuItem(value: k, child: Text(k)))
            .toList(),
        onChanged: (v) => setState(() => kind = v!),
      ),
      const SizedBox(height: 16),
      if (['Weeding', 'Manuring'].contains(kind))
        const Text('Estate-level activity; no field allocation.'),
      ...schemas[kind]!.map(
        (k) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 7),
          child: k == 'period'
              ? DropdownButtonFormField<String>(
                  initialValue: control(k).text,
                  decoration: const InputDecoration(labelText: 'Period'),
                  items: ['daily', 'weekly', 'monthly']
                      .map((p) => DropdownMenuItem(value: p, child: Text(p)))
                      .toList(),
                  onChanged: (v) => control(k).text = v!,
                )
              : TextField(
                  controller: control(k),
                  keyboardType: numeric(k)
                      ? const TextInputType.numberWithOptions(decimal: true)
                      : TextInputType.text,
                  decoration: InputDecoration(
                    labelText: k.replaceAll('_', ' '),
                    helperText: k == 'date'
                        ? 'YYYY-MM-DD • period start for weekly/monthly'
                        : k == 'notes'
                        ? 'Optional'
                        : null,
                  ),
                ),
        ),
      ),
      const SizedBox(height: 16),
      FilledButton.icon(
        onPressed: busy
            ? null
            : () => action(() async {
                final data = {
                  for (final k in schemas[kind]!) k: control(k).text.trim(),
                };
                final error = validate(kind, data);
                if (error != null) throw FormatException(error);
                await store!.save([Entry(kind, data)]);
                message('Saved on this device');
              }),
        icon: const Icon(Icons.save_outlined),
        label: const Text('Save record'),
      ),
      OutlinedButton.icon(
        onPressed: busy ? null : importFile,
        icon: const Icon(Icons.upload_file),
        label: const Text('Import CSV / XLSX / Markdown'),
      ),
    ],
  );
  Future<void> importFile() async {
    try {
      final f = await FilePicker.pickFile(
        type: FileType.custom,
        allowedExtensions: ['csv', 'xlsx', 'md'],
      );
      if (f == null) return;
      if ((await f.length() ?? 0) > 5 * 1024 * 1024) {
        throw const FormatException('Maximum file size is 5 MB');
      }
      final bytes = await f.readAsBytes();
      if (bytes.length > 5 * 1024 * 1024) {
        throw const FormatException('Maximum file size is 5 MB');
      }
      final preview = parseImport(f.name, bytes, kind);
      if (!mounted) return;
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text('Preview: $kind'),
          content: SizedBox(
            width: 600,
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${preview.entries.length} valid rows • ${preview.errors.length} errors',
                  ),
                  const Text(
                    'All rows must pass. Existing IDs and duplicates are checked when saving. No rows have been saved.',
                  ),
                  ...preview.errors
                      .take(30)
                      .map(
                        (e) =>
                            Text(e, style: const TextStyle(color: Colors.red)),
                      ),
                  ...preview.entries
                      .take(20)
                      .map(
                        (e) => Padding(
                          padding: const EdgeInsets.symmetric(vertical: 8),
                          child: Text(detail(e)),
                        ),
                      ),
                  if (preview.entries.length > 20)
                    const Text(
                      'Showing first 20 rows. All rows were validated.',
                    ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: preview.errors.isEmpty && preview.entries.isNotEmpty
                  ? () => Navigator.pop(ctx, true)
                  : null,
              child: const Text('Confirm import'),
            ),
          ],
        ),
      );
      if (confirmed == true) {
        await action(() async {
          await store!.save(preview.entries);
          message('Import saved');
        });
      }
    } catch (e) {
      message(e);
    }
  }

  Widget tools() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      heading('Tools', 'Import guidance'),
      card(
        'Import a table',
        'Choose a record type in Data Input, then import a file. CSV headers, the single XLSX worksheet, or a Markdown pipe table must match the fields below. Dates: YYYY-MM-DD. Percentages: 0–100. Import estate profiles before field profiles, then production.',
      ),
      ...schemas.entries.map((s) => card(s.key, s.value.join(', '))),
      card(
        'Period guidance',
        'Daily, weekly and monthly crop entries are separate summaries. Enter the period start date. Home totals count daily records only.',
      ),
    ],
  );
  Widget settings() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      heading('Settings', 'Local device storage'),
      card(
        'Offline first',
        'SQLite on this device. No account or network required for recording. Cloud sync and backup are not implemented yet.',
      ),
      card(
        'Data status',
        '${entries.length} records • ${entries.where((e) => e.sample).length} sample records',
      ),
      OutlinedButton(
        onPressed: busy || entries.any((e) => e.data['estate_id'] == 'SAMPLE')
            ? null
            : () => action(() async {
                await store!.sample();
                message('Clearly labeled sample data added');
              }),
        child: const Text('Load sample estate'),
      ),
      const SizedBox(height: 12),
      const Text(
        'MVP • New entries and import only. Editing, deletion, exports and automatic backups are planned. Removing the app can remove its local data.',
      ),
    ],
  );
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Planters Book'),
      actions: [
        const Padding(
          padding: EdgeInsets.all(16),
          child: Icon(Icons.offline_pin_outlined),
        ),
      ],
    ),
    body: startupError != null
        ? Center(child: Text('Unable to open local storage: $startupError'))
        : store == null
        ? const Center(child: CircularProgressIndicator())
        : SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Align(
                alignment: Alignment.topCenter,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 850),
                  child: [
                    dashboard,
                    estateView,
                    fieldView,
                    input,
                    tools,
                    settings,
                  ][tab](),
                ),
              ),
            ),
          ),
    bottomNavigationBar: NavigationBar(
      selectedIndex: tab,
      onDestinationSelected: (i) => setState(() => tab = i),
      destinations: List.generate(
        tabs.length,
        (i) => NavigationDestination(icon: Icon(icons[i]), label: tabs[i]),
      ),
    ),
  );
}
