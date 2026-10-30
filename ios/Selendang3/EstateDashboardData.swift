import Foundation

struct EstateYphHistory: Decodable { let year: Int; let actualYph: Double? }
struct EstateYph: Decodable {
    let reportingYear: Int
    let throughMonth: Int
    let matureAreaHa: Double
    let actualYtdTonnes: Double?
    let budgetYtdTonnes: Double?
    let fullYearBudgetTonnes: Double?
    let history: [EstateYphHistory]
}
struct DashboardBar: Identifiable {
    let category: String
    let series: String
    let value: Double
    var id: String { category + "/" + series }
}
struct EstateDashboardPanel: Identifiable {
    let title: String
    let headers: [String]
    let rows: [[String]]
    var note: String = ""
    var bars: [DashboardBar] = []
    var unit: String = ""
    var id: String { title }
}
private struct DashboardField { let code: String; let area: Double; let palms: Double? }

struct EstateDashboardData {
    let reference: EstateReference?
    let today: Date
    static func number(_ value: Double?) -> String {
        guard let value = value, value.isFinite else { return "Pending" }
        return String(format:"%0.2f",locale:Locale(identifier:"en_US_POSIX"),value)
    }
    private func table(_ id: String) -> EstateReferenceTable? { reference?.sections.first { $0.id == id } }
    private func cell(_ row: [EstateReferenceCell?], _ table: EstateReferenceTable, _ column: String) -> String? {
        guard let index = table.columns.firstIndex(of:column), index < row.count else { return nil }
        return row[index]?.text
    }
    private func numeric(_ text: String?) -> Double? {
        guard let value = text?.replacingOccurrences(of:",",with:"").trimmingCharacters(in:.whitespacesAndNewlines),
              let result = Double(value), result.isFinite, result >= 0 else { return nil }; return result
    }
    private func count(_ text: String?) -> Double? {
        guard let value = numeric(text), value.rounded() == value, value <= 1e9 else { return nil }; return value
    }
    private func overview(_ label: String) -> String? {
        guard let t = table("overview") else { return nil }
        let rows = t.rows.filter { cell($0,t,"Item") == label }
        return rows.count == 1 ? cell(rows[0],t,"Details") : nil
    }
    private func period(_ key: String) -> String? {
        guard let value = overview(key), value.range(of:"^\\d{4}-(0[1-9]|1[0-2])$", options:.regularExpression) != nil else { return nil }
        return value
    }
    private var fields: [DashboardField]? {
        guard let t = table("fields"), !t.rows.isEmpty else { return nil }
        var result: [DashboardField] = [], codes: Set<String> = []
        for row in t.rows {
            guard let code = cell(row,t,"Field code"), !code.isEmpty, codes.insert(code.uppercased()).inserted,
                  let area = numeric(cell(row,t,"Area (ha)")), area > 0 else { return nil }
            result.append(DashboardField(code:code,area:area,palms:count(cell(row,t,"Total palms"))))
        }
        return result
    }
    private var area: Double? { fields.map { $0.reduce(0) { $0 + $1.area } }.flatMap { $0.isFinite ? $0 : nil } }
    private func share(_ area: Double?, _ total: Double?) -> String {
        guard let area = area, let total = total, total > 0 else { return "Pending" }
        return Self.number(area/total*100)+"%"
    }
    private var ages: EstateDashboardPanel {
        let names = ["Young · 1–5 years","Prime · 6–23 years","Old · >23 years"]
        var sums = [0.0,0.0,0.0], complete = true
        if let fields = fields, let t = table("ages") {
            for field in fields {
                let rows = t.rows.filter { cell($0,t,"Field / category") == field.code }
                guard rows.count == 1, let text = cell(rows[0],t,"Age / years"),
                      let age = Int(text.replacingOccurrences(of:" years",with:"").trimmingCharacters(in:.whitespaces)), age > 0 else { complete = false; break }
                sums[age <= 5 ? 0 : age <= 23 ? 1 : 2] += field.area
            }
        } else { complete = false }
        return EstateDashboardPanel(title:"Age Profile",headers:["Age category","Area (ha)","Share"],
            rows:names.indices.map { [names[$0],complete ? Self.number(sums[$0]) : "Pending",complete ? share(sums[$0],area) : "Pending"] },
            note:"From saved field ages. Total area: " + Self.number(area) + " ha.")
    }
    private func land(_ category: String, title: String) -> EstateDashboardPanel {
        let soilSeries = category == "Soil series"
        let headers = soilSeries ? ["Soil series","Area (ha)","Share","Reference class"] : ["Terrain","Area (ha)","Share"]
        guard let t = table("soil"), let total = area else { return EstateDashboardPanel(title:title,headers:headers,rows:[],note:"Pending complete estate records") }
        let source = t.rows.filter { cell($0,t,"Category") == category }
        var rows: [[String]] = [], sum = 0.0, shares = 0.0, names: Set<String> = []
        for row in source {
            guard let name = cell(row,t,"Type"), names.insert(name).inserted,
                  let hectares = numeric(cell(row,t,"Area (ha)")), let percent = numeric(cell(row,t,"Share (%)")), percent <= 100,
                  abs(hectares-total*percent/100) <= 0.011 else {
                return EstateDashboardPanel(title:title,headers:headers,rows:[],note:"Pending complete area and percentage records")
            }
            sum += hectares; shares += percent
            var values = [name,Self.number(hectares),Self.number(percent)+"%"]
            if soilSeries {
                let rating = cell(row,t,"Suitability class") ?? ""
                let citation = cell(row,t,"Class reference") ?? ""
                values.append(["I","II","III","IV","V"].contains(rating) && !citation.isEmpty ? "Class "+rating+"*" : "Pending")
            }
            rows.append(values)
        }
        guard !rows.isEmpty, abs(sum-total) <= 0.011, abs(shares-100) <= 0.001,
              soilSeries || names == Set(["Flat","Hilly","Undulating"]) else {
            return EstateDashboardPanel(title:title,headers:headers,rows:[],note:"Pending reconciled area records")
        }
        return EstateDashboardPanel(title:title,headers:headers,rows:rows,
            note:soilSeries ? "* Historical agricultural suitability reference; estate verification pending." : "Total area: "+Self.number(total)+" ha")
    }
    private var sph: EstateDashboardPanel {
        var total: Double?
        if let fields = fields, fields.allSatisfy({ $0.palms != nil }) { total = fields.reduce(0) { $0 + $1.palms! } }
        let density: Double? = { guard let total = total, let area = area, area > 0 else { return nil }; return total/area }()
        return EstateDashboardPanel(title:"Average SPH",headers:["Measure","Value"],rows:[["Palms/ha",Self.number(density)]],note:"Total palms across all fields ÷ total estate hectares")
    }
    private var yph: EstateDashboardPanel {
        guard let data = reference?.yph, (1...12).contains(data.throughMonth), data.reportingYear >= 2000,
              data.matureAreaHa.isFinite, data.matureAreaHa > 0 else {
            return EstateDashboardPanel(title:"YPH",headers:["Year / period","Actual","Budget"],rows:[],note:"Pending YPH records")
        }
        var rows: [[String]] = [], bars: [DashboardBar] = []
        for year in (data.reportingYear-4)..<data.reportingYear {
            let matches = data.history.filter { $0.year == year }
            let value = matches.count == 1 ? matches[0].actualYph.flatMap { $0.isFinite && $0 >= 0 ? $0 : nil } : nil
            rows.append([String(year),Self.number(value),"—"])
            if let value = value { bars.append(DashboardBar(category:String(year),series:"Actual",value:value)) }
        }
        func yield(_ tonnes: Double?) -> Double? { tonnes.flatMap { $0.isFinite && $0 >= 0 ? $0/data.matureAreaHa : nil } }
        let actual = yield(data.actualYtdTonnes), budget = yield(data.budgetYtdTonnes)
        let year = String(data.reportingYear)+" YTD"
        rows.append([year,Self.number(actual),Self.number(budget)])
        if let actual = actual { bars.append(DashboardBar(category:year,series:"Actual",value:actual)) }
        if let budget = budget { bars.append(DashboardBar(category:year,series:"Budget",value:budget)) }
        let month = DateFormatter().monthSymbols[data.throughMonth-1]
        return EstateDashboardPanel(title:"YPH",headers:["Year / period","Actual","Budget"],rows:rows,
            note:"YTD January–\(month) \(data.reportingYear). Full-year budget: \(Self.number(yield(data.fullYearBudgetTonnes))) t/ha.",bars:bars,unit:"t/ha")
    }
    private var rainfall: EstateDashboardPanel {
        var calendar = Calendar(identifier:.gregorian); calendar.timeZone = TimeZone(identifier:"Asia/Kuala_Lumpur")!
        let year = calendar.component(.year,from:today), cutoff = calendar.component(.month,from:today)-1
        let months = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"]
        var entries: [(year: Int, end: Int, total: Double?)] = []
        for y in stride(from:year,through:year-4,by:-1) {
            let end = y == year ? cutoff : 12
            var values: [Int:Double] = [:], duplicate = false
            var seen: Set<Int> = []
            if let t = table("rainfall") {
                for row in t.rows where cell(row,t,"Year") == String(y) {
                    if let name = cell(row,t,"Month / total"), let index = months.firstIndex(where:{ $0.caseInsensitiveCompare(name) == .orderedSame }), index < end {
                        if !seen.insert(index).inserted { duplicate = true }
                        if let value = numeric(cell(row,t,"Rainfall (mm)")) { values[index] = value }
                    }
                }
            }
            let sum = values.values.reduce(0,+)
            let total: Double? = end > 0 && values.count == end && !duplicate && sum.isFinite ? sum : nil
            entries.append((y,end,total))
        }
        let full = entries.filter { $0.end == 12 }.compactMap { $0.total }
        let low = full.min(), high = full.max()
        let distinct = full.count >= 2 && high! - low! > 1e-7
        let rows = entries.map { entry -> [String] in
            let period = entry.year != year ? "Full year" : entry.end == 0 ? "No completed month" : "YTD Jan–"+months[entry.end-1]
            return [String(entry.year),period,Self.number(entry.total)]
        }
        let bars = entries.compactMap { entry -> DashboardBar? in
            guard let total = entry.total else { return nil }
            let series = entry.end < 12 ? "Current YTD" : distinct && abs(total - high!) < 1e-7 ? "Highest full year" : distinct && abs(total - low!) < 1e-7 ? "Lowest full year" : "Full year"
            return DashboardBar(category:String(entry.year),series:series,value:total)
        }
        return EstateDashboardPanel(title:"Rainfall",headers:["Year","Period","Total (mm)"],rows:rows,
            note:"Five calendar years. Current month excluded; missing months remain Pending. High/low colours compare completed full years; the current-year YTD is separate. Tied extremes share a colour; equal totals have no distinct extreme. Table newest first.",bars:bars,unit:"mm")
    }

    private var workforce: EstateDashboardPanel {
        let staff = count(overview("Workforce staff")), local = count(overview("Workforce local workers")), foreign = count(overview("Workforce foreign workers"))
        let direct = count(overview("Workforce total workers")), contractors = count(overview("Workforce contract workers"))
        let complete = period("Workforce reporting period") != nil && staff != nil && local != nil && foreign != nil && direct != nil && local!+foreign! == direct!
        func label(_ value: Double?) -> String { guard complete, let value = value else { return "Pending" }; return String(format:"%.0f",value) }
        let total: Double? = complete && contractors != nil ? staff!+direct!+contractors! : nil
        return EstateDashboardPanel(title:"Workforce",headers:["Category","People"],rows:[
            ["Staff",label(staff)],["Local Workers",label(local)],["Foreign Workers",label(foreign)],
            ["Contract Workers",label(contractors)],["Total Workforce",label(total)]],
            note:"Source month: "+(period("Workforce reporting period") ?? "Pending")+". Contractors are separate from local/foreign worker counts.")
    }
    private var ratios: EstateDashboardPanel {
        let immature = numeric(overview("Ratios immature area (ha)")), mature = numeric(overview("Ratios mature area (ha)")), workers = count(overview("Ratios mature operations workers"))
        let complete = period("Ratios reporting period") != nil && immature != nil && mature != nil && workers != nil && area != nil && abs(immature!+mature!-area!) <= 0.011
        let value: String = complete && mature! > 0 && workers! > 0 ? "1:"+Self.number(mature!/workers!) : "Pending"
        return EstateDashboardPanel(title:"Ratios",headers:["Maturity","Area (ha)","Workers:ha"],rows:[
            ["Immature",complete ? Self.number(immature) : "Pending",complete && immature == 0 ? "—" : "Pending"],
            ["Mature",complete ? Self.number(mature) : "Pending",complete && mature == 0 ? "—" : value]],
            note:"Source month: "+(period("Ratios reporting period") ?? "Pending")+". Uses mature-area operations workers only.")
    }
    private var costs: EstateDashboardPanel {
        let actualPeriod = period("FFB cost YTD reporting period"), budgetYear = overview("FFB cost budget year")
        let valid = actualPeriod != nil && budgetYear == String(actualPeriod!.prefix(4))
        let pairs = [("Operation Cost","operation"),("General Charges","general charges")]
        var rows: [[String]] = [], bars: [DashboardBar] = []
        for (label,key) in pairs {
            let actual = valid ? numeric(overview("FFB cost \(key) actual YTD (RM/tonne)")) : nil
            let budget = valid ? numeric(overview("FFB cost \(key) budget year (RM/tonne)")) : nil
            rows.append([label,Self.number(actual),Self.number(budget)])
            if let budget = budget { bars.append(DashboardBar(category:label,series:"Budget Year",value:budget)) }
            if let actual = actual { bars.append(DashboardBar(category:label,series:"Actual YTD",value:actual)) }
        }
        return EstateDashboardPanel(title:"Kos FFB/TAN",headers:["Measure","Actual YTD","Budget Year"],rows:rows,
            note:"Actual YTD through \(actualPeriod ?? "Pending") vs Budget Year \(budgetYear ?? "Pending"). Operation Cost = Penjagaan + Membaja + Pungutan; General Charges = Perbelanjaan Am.",bars:bars,unit:"RM/tonne")
    }

    var areaSummary: EstateDashboardPanel {
        let sourcePeriod = period("Estate area reporting period")
        let current = String(format: "%04d-%02d", reportCalendar.component(.year, from: today), reportCalendar.component(.month, from: today))
        let perimeter = numeric(overview("Perimeter area (ha)"))
        let mature = numeric(overview("Mature area (ha)"))
        let complete = sourcePeriod != nil && sourcePeriod! <= current && perimeter != nil && perimeter! > 0 && mature != nil && mature! <= perimeter!
        let format = NumberFormatter(); format.locale = Locale(identifier: "en_US_POSIX")
        format.numberStyle = .decimal; format.usesGroupingSeparator = true; format.groupingSize = 3
        format.groupingSeparator = ","; format.decimalSeparator = "."
        format.minimumFractionDigits = 2; format.maximumFractionDigits = 2
        func label(_ value: Double?) -> String {
            guard complete, let value = value else { return "Pending" }
            return format.string(from: NSNumber(value: value)) ?? "Pending"
        }
        return EstateDashboardPanel(title: "Estate Area", headers: ["Measure", "Area (ha)"], rows: [
            ["Perimeter Hectare", label(perimeter)], ["Mature Hectare", label(mature)]
        ], note: complete ? "Reporting period: " + sourcePeriod! : "Awaiting verified estate area records.")
    }

    private var reportCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Kuala_Lumpur")!
        return calendar
    }
    private func reportDate(_ text: String) -> Date? {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = reportCalendar; formatter.timeZone = reportCalendar.timeZone
        formatter.dateFormat = "yyyy-MM-dd"; formatter.isLenient = false
        guard let date = formatter.date(from: text), formatter.string(from: date) == text,
              date <= reportCalendar.startOfDay(for: today) else { return nil }
        return date
    }
    private func nonnegative(_ value: Double?) -> Double? {
        guard let value = value, value.isFinite, value >= 0 else { return nil }
        return value
    }
    private var currentCrop: EstateCropReport? {
        guard let report = reference?.cropProductionReport, let date = reportDate(report.reportDate),
              reportCalendar.isDate(date, equalTo: today, toGranularity: .month) else { return nil }
        return report
    }
    private var previousCrop: EstateCropReport? {
        let start = reportCalendar.date(from: reportCalendar.dateComponents([.year, .month], from: today))!
        let monthEnd = reportCalendar.date(byAdding: .day, value: -1, to: start)!
        let reports = ([reference?.cropProductionReport].compactMap { $0 } + (reference?.cropProductionReportHistory ?? []))
            .filter { report in
                guard report.isCompleteMonth == true, let date = reportDate(report.reportDate) else { return false }
                return reportCalendar.isDate(date, inSameDayAs: monthEnd)
            }
        return reports.count == 1 ? reports[0] : nil
    }
    private var manuringProgress: Double? {
        guard let report = reference?.manuringReport, let date = reportDate(report.reportDate),
              reportCalendar.component(.year, from: date) == reportCalendar.component(.year, from: today),
              let applied = nonnegative(report.appliedYtdMt), let programme = nonnegative(report.annualProgrammeMt),
              programme > 0 else { return nil }
        return nonnegative(applied / programme * 100)
    }
    private var manuringNote: String {
        guard manuringProgress != nil, let report = reference?.manuringReport else { return "Reporting period pending" }
        return "YTD through " + report.reportDate + " · Applied mt / annual programme mt"
    }

    // Sparse daily drafts are not substituted for complete estate monthly totals.
    var homeRows: [EstateDashboardPanel] {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Kuala_Lumpur")!
        let year = calendar.component(.year, from: today)
        let month = calendar.component(.month, from: today)
        let current = String(format: "%04d-%02d", year, month)
        let formatter = DateFormatter()
        formatter.calendar = calendar; formatter.timeZone = calendar.timeZone
        formatter.locale = Locale(identifier: "en_US_POSIX"); formatter.dateFormat = "MMM yyyy"
        let previous = calendar.date(byAdding: .month, value: -1, to: calendar.date(from: calendar.dateComponents([.year, .month], from: today))!)!
        var ytd: Double?, annualBudgetProgress: Double?, yieldPeriod = "YTD " + String(year)
        if let source = reference?.yph, source.reportingYear == year,
           (1...month).contains(source.throughMonth), source.matureAreaHa.isFinite, source.matureAreaHa > 0,
           let tonnes = source.actualYtdTonnes, tonnes.isFinite, tonnes >= 0 {
            ytd = tonnes / source.matureAreaHa
            if let budget = source.fullYearBudgetTonnes, budget.isFinite, budget > 0 {
                let percent = tonnes / budget * 100
                if percent.isFinite { annualBudgetProgress = percent }
            }
            yieldPeriod = "YTD through " + formatter.string(from: calendar.date(from: DateComponents(year: year, month: source.throughMonth, day: 1))!)
        }
        let costPeriod = period("FFB cost YTD reporting period")
        let costValid = costPeriod != nil && costPeriod!.hasPrefix(String(year) + "-") && costPeriod! <= current &&
            overview("FFB cost budget year") == String(year)
        let operation = costValid ? numeric(overview("FFB cost operation actual YTD (RM/tonne)")) : nil
        let gc = costValid ? numeric(overview("FFB cost general charges actual YTD (RM/tonne)")) : nil
        var costLabel = "YTD " + String(year)
        if costValid, let date = ISO8601DateFormatter().date(from: costPeriod! + "-01T00:00:00Z") {
            costLabel = "YTD through " + formatter.string(from: date)
        }
        return [
            EstateDashboardPanel(title: "Crop data · t/ha", headers: [], rows: [],
                note: formatter.string(from: previous) + ": " + Self.number(nonnegative(previousCrop?.totals.sentToMillMtdMtPerHa)) + " · " + formatter.string(from: today) + " MTD: " + Self.number(nonnegative(currentCrop?.totals.sentToMillMtdMtPerHa)) + "\n" +
                    yieldPeriod + ": " + Self.number(ytd) + "\nYTD vs Year Budget: " +
                    (annualBudgetProgress.map { Self.number($0) + "%" } ?? "Pending")),
            EstateDashboardPanel(title: "Upkeep and Manuring · %", headers: [], rows: [],
                note: "Weeding: Pending · Manuring: " + (manuringProgress.map { Self.number($0) + "%" } ?? "Pending")),
            EstateDashboardPanel(title: "Estate cost · Actual YTD · RM/tonne", headers: [], rows: [],
                note: "Operation: " + Self.number(operation) + " · GC: " + Self.number(gc) + "\n" + costLabel)
        ]
    }

    // Uses the same source periods and validation as the home summaries.
    var operationalCards: [EstateDashboardPanel] {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Kuala_Lumpur")!
        let year = calendar.component(.year, from: today)
        let month = calendar.component(.month, from: today)
        let current = String(format: "%04d-%02d", year, month)
        let formatter = DateFormatter()
        formatter.calendar = calendar; formatter.timeZone = calendar.timeZone
        formatter.locale = Locale(identifier: "en_US_POSIX"); formatter.dateFormat = "MMM yyyy"
        var ytd: Double?, annualBudgetProgress: Double?, yieldPeriod = "YTD " + String(year)
        if let source = reference?.yph, source.reportingYear == year,
           (1...month).contains(source.throughMonth), source.matureAreaHa.isFinite, source.matureAreaHa > 0,
           let tonnes = source.actualYtdTonnes, tonnes.isFinite, tonnes >= 0 {
            ytd = tonnes / source.matureAreaHa
            if let budget = source.fullYearBudgetTonnes, budget.isFinite, budget > 0 {
                let percent = tonnes / budget * 100
                if percent.isFinite { annualBudgetProgress = percent }
            }
            yieldPeriod = "YTD through " + formatter.string(from: calendar.date(from: DateComponents(year: year, month: source.throughMonth, day: 1))!)
        }
        let costPeriod = period("FFB cost YTD reporting period")
        let costValid = costPeriod != nil && costPeriod!.hasPrefix(String(year) + "-") && costPeriod! <= current &&
            overview("FFB cost budget year") == String(year)
        let operation = costValid ? numeric(overview("FFB cost operation actual YTD (RM/tonne)")) : nil
        let gc = costValid ? numeric(overview("FFB cost general charges actual YTD (RM/tonne)")) : nil
        var costLabel = "YTD " + String(year)
        if costValid, let date = ISO8601DateFormatter().date(from: costPeriod! + "-01T00:00:00Z") {
            costLabel = "YTD through " + formatter.string(from: date)
        }
        return [
            EstateDashboardPanel(title: "Crop Production", headers: [], rows: [
                [formatter.string(from: calendar.date(byAdding: .month, value: -1, to: today)!), Self.number(nonnegative(previousCrop?.totals.sentToMillMtdMtPerHa)), "mt/ha"],
                [formatter.string(from: today) + " MTD", Self.number(nonnegative(currentCrop?.totals.sentToMillMtdMtPerHa)), "mt/ha"],
                ["Actual YTD", Self.number(ytd), "mt/ha"],
                ["YTD / Year Budget", Self.number(annualBudgetProgress), "%"]
            ], note: yieldPeriod + (currentCrop.map { " · MTD through " + $0.reportDate } ?? "")),
            EstateDashboardPanel(title: "Harvesting Coverage", headers: [], rows: [
                ["Hectare Completed", Self.number(nonnegative(currentCrop?.totals.hectaresMtd)), "ha"],
                ["Completed rounds", Self.number(nonnegative(currentCrop?.totals.roundsMtd)), "rounds"]
            ], note: currentCrop.map { "Report: " + $0.reportDate } ?? "Reporting period pending"),
            EstateDashboardPanel(title: "Harvester Productivity", headers: [], rows: [
                ["Tonnes per mandays", Self.number(nonnegative(currentCrop?.totals.mtPerHarvesterDayMtd)), "mt/manday"],
                ["Hectare per mandays", Self.number(nonnegative(currentCrop?.totals.haPerHarvesterDayMtd)), "ha/manday"]
            ], note: currentCrop.map { "MTD through " + $0.reportDate } ?? "Reporting period pending"),
            EstateDashboardPanel(title: "Manuring", headers: [], rows: [
                ["Programme progress", Self.number(manuringProgress), "%"]
            ], note: manuringNote),
            EstateDashboardPanel(title: "Weeding", headers: [], rows: [
                ["Circle", "Pending", "%"], ["Selective", "Pending", "%"]
            ], note: "Reporting period pending"),
            EstateDashboardPanel(title: "Pest & Disease", headers: [], rows: [
                ["Affected area", "Pending", "ha"], ["Incidence", "Pending", "%"]
            ], note: "Reporting period pending"),
            EstateDashboardPanel(title: "Estate Cost", headers: [], rows: [
                ["Operation Cost", Self.number(operation), "RM/mt"],
                ["General Charges", Self.number(gc), "RM/mt"]
            ], note: costLabel)

        ]
    }

    var ffbQuality: EstateDashboardPanel {
        var rows = ["Ripe (%)","Underipe (%)","Unripe (%)","Reject (mt)"].map { [$0,"Pending","Pending"] }
        var note = "Awaiting approved FFB quality records."
        if let report = reference?.ffbRejectReport, let date = reportDate(report.reportDate) {
            let sameMonth = reportCalendar.isDate(date, equalTo: today, toGranularity: .month)
            let sameYear = reportCalendar.isDate(date, equalTo: today, toGranularity: .year)
            rows[3][1] = Self.number(sameMonth ? nonnegative(report.totals.rejectMtdMt) : nil)
            rows[3][2] = Self.number(sameYear ? nonnegative(report.totals.rejectYtdMt) : nil)
            note = "Reject report: " + report.reportDate + ". Rejected FFB quantities are shown in mt. Other quality categories remain Pending."
        }
        return EstateDashboardPanel(title:"FFB Quality", headers:["Category","This Month","YTD"], rows:rows, note:note)
    }
    var panels: [EstateDashboardPanel] {
        [ages,land("Terrain",title:"Topography"),yph,sph,land("Soil series",title:"Soil Type"),rainfall,workforce,ratios,costs,
         ffbQuality,
         EstateDashboardPanel(title:"OER (%)",headers:["Period","KPG (%)","KPA (%)"],rows:[["YTD","Pending","Pending"]],
             note:"KPG = actual OER; KPA = budget OER. Awaiting approved OER records.",unit:"%"),
         EstateDashboardPanel(title:"Oil Yield (t/ha)",headers:["Period","Actual (t/ha)","Budget (t/ha)"],rows:[["YTD","Pending","Pending"]],
             note:"Oil tonnes per hectare. Awaiting approved oil yield records.",unit:"t/ha")]

    }
}


func estateFieldCodes(_ reference: EstateReference?) -> [String] {
    guard let table = reference?.sections.first(where: { $0.id == "fields" }),
          let column = table.columns.firstIndex(of: "Field code") else { return [] }
    return Array(Set(table.rows.compactMap { row -> String? in
        guard column < row.count, let code = row[column]?.text.trimmingCharacters(in: .whitespacesAndNewlines),
              !code.isEmpty else { return nil }
        return code
    })).sorted()
}

// Keep the source snapshot intact; route its field columns to their owning modules.
private let basicFieldColumns: Set<String> = ["Field code", "Alternate code", "Area (ha)",
    "Source palms/ha", "Total palms", "Review status", "Planting year", "Planting date",
    "Age / years", "Maturity", "Soil series", "Terrain"]

func estateFieldTables(_ reference: EstateReference?, code: String) -> [EstateReferenceTable] {
    fieldRecordTables(reference, code: code, crop: false)
}
func estateFieldCropTables(_ reference: EstateReference?, code: String) -> [EstateReferenceTable] {
    fieldRecordTables(reference, code: code, crop: true)
}
private func fieldRecordTables(_ reference: EstateReference?, code: String, crop: Bool) -> [EstateReferenceTable] {
    guard estateFieldCodes(reference).contains(code) else { return [] }
    return (reference?.sections ?? []).compactMap { table in
        guard let key = table.columns.firstIndex(where: { ["Field code", "Field / category", "Field"].contains($0) }) else { return nil }
        let rows = table.rows.filter { key < $0.count && $0[key]?.text.trimmingCharacters(in: .whitespacesAndNewlines) == code }
        guard !rows.isEmpty else { return nil }
        let indices: [Int]
        if table.id == "fields" {
            indices = table.columns.indices.filter { crop ? ($0 == key || !basicFieldColumns.contains(table.columns[$0])) : basicFieldColumns.contains(table.columns[$0]) }
            if crop && indices.count <= 1 { return nil }
        } else if crop ? table.id == "abw" : ["ages", "soil", "roads"].contains(table.id) {
            indices = Array(table.columns.indices)
        } else { return nil }
        guard !indices.isEmpty else { return nil }
        return EstateReferenceTable(id: table.id, columns: indices.map { table.columns[$0] }, rows: rows.map { row in indices.map { row[$0] } })
    }
}


