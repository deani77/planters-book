import Foundation
let raw = """
{"fieldCode":"EXAMPLE","annual":{"2021":9,"2022":null,"2023":12},"productionYear":2026,"productionTonnes":120,"plantedAreaHa":10,"areaSource":"Fictional test"}
"""
let field = try JSONDecoder().decode(EstateCropHistoryField.self, from: Data(raw.utf8))
precondition(field.yield(year: 2026) == 12)
precondition(field.yield(year: 2022) == nil)
precondition(field.yield(year: 2021) == 9)
precondition(field.years(currentYear: 2026) == [2022,2023,2024,2025,2026])
precondition(field.years(currentYear: 2027) == [2023,2024,2025,2026,2027])
for area in ["null", "0", "-1"] {
    let missing = raw.replacingOccurrences(of: "\"plantedAreaHa\":10", with: "\"plantedAreaHa\":" + area)
    let value = try JSONDecoder().decode(EstateCropHistoryField.self, from: Data(missing.utf8))
    precondition(value.yield(year: 2026) == nil)
}
print("Crop history checks passed: N/A, area guards, YTD and rolling window")
