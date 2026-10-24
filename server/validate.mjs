export const MAX_BYTES = 2 * 1024 * 1024;
export const SECTION_IDS = ['overview', 'fields', 'ages', 'soil', 'rainfall', 'abw', 'roads'];

export function validateSnapshot(bytes) {
  if (bytes.length > MAX_BYTES) throw new Error('Snapshot exceeds 2 MB');
  const text = new TextDecoder('utf-8', {fatal: true}).decode(bytes);
  const value = JSON.parse(text);
  if (value.schemaVersion !== 1 || value.estateId !== 'selendang-3') throw new Error('Invalid schema or estate');
  if (!Number.isSafeInteger(value.revision) || value.revision <= 0) throw new Error('Invalid revision');
  if (typeof value.reportingPeriod !== 'string' || !value.reportingPeriod.trim() || value.reportingPeriod.length > 100) throw new Error('Invalid reporting period');
  if (typeof value.exportedAt !== 'string' || value.exportedAt.length > 50 || !/(Z|[+-]\d{2}:\d{2})$/.test(value.exportedAt) || !Number.isFinite(Date.parse(value.exportedAt))) throw new Error('Invalid export timestamp');
  if (!Array.isArray(value.sections) || value.sections.length !== 7) throw new Error('Seven sections required');
  const seen = new Set();
  let rows = 0;
  for (const section of value.sections) {
    if (!SECTION_IDS.includes(section.id) || seen.has(section.id)) throw new Error('Missing or duplicate section');
    seen.add(section.id);
    const {columns, rows: records} = section;
    if (!Array.isArray(columns) || columns.length < 1 || columns.length > 20 || new Set(columns).size !== columns.length || columns.some(c => typeof c !== 'string' || !c.trim() || c.length > 80)) throw new Error('Invalid columns');
    if (!Array.isArray(records) || (rows += records.length) > 10000) throw new Error('Too many rows');
    for (const row of records) {
      if (!Array.isArray(row) || row.length !== columns.length) throw new Error('Invalid row width');
      for (const cell of row) {
        if (!(cell === null || (typeof cell === 'string' && cell.length <= 1000) || (typeof cell === 'number' && Number.isFinite(cell)))) throw new Error('Invalid cell');
      }
    }
  }
  if (Object.hasOwn(value, 'yph') && value.yph !== null) validateYph(value.yph);
  return value;
}

export function validateYph(value) {
  if (!value || typeof value !== 'object' || !Number.isInteger(value.reportingYear) || value.reportingYear < 1900 || value.reportingYear > 2200 || !Number.isInteger(value.throughMonth) || value.throughMonth < 1 || value.throughMonth > 12) throw new Error('Invalid YPH period');
  function nullableNumber(object, key) {
    if (!Object.hasOwn(object, key)) throw new Error('Missing YPH value');
    const number = object[key];
    if (number !== null && (typeof number !== 'number' || !Number.isFinite(number) || number < 0)) throw new Error('Invalid YPH value');
    return number;
  }
  if (!(nullableNumber(value, 'matureAreaHa') > 0)) throw new Error('Invalid YPH area');
  nullableNumber(value, 'actualYtdTonnes');
  const budget = nullableNumber(value, 'budgetYtdTonnes'), annual = nullableNumber(value, 'fullYearBudgetTonnes');
  if (budget !== null && annual !== null && budget > annual) throw new Error('YTD budget exceeds annual budget');
  if (!Array.isArray(value.history) || value.history.length > 50) throw new Error('Invalid YPH history');
  const seen = new Set();
  for (const row of value.history) {
    if (!Number.isInteger(row.year) || row.year < 1900 || row.year >= value.reportingYear || seen.has(row.year)) throw new Error('Invalid historical YPH year');
    seen.add(row.year);
    nullableNumber(row, 'actualYph');
  }
  return value;
}
