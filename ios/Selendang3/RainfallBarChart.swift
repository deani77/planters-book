import SwiftUI
import Charts

struct RainfallBarChart: View {
    let section: EstateDashboardPanel
    @Environment(\.colorScheme) private var scheme
    private let order = ["Highest full year", "Lowest full year", "Full year", "Current YTD"]
    private func color(_ series: String) -> Color {
        switch series {
        case "Highest full year": return scheme == .dark ? Color(red:0.50,green:0.80,blue:0.52) : Color(red:0.18,green:0.49,blue:0.20)
        case "Lowest full year": return scheme == .dark ? Color(red:1,green:0.72,blue:0.30) : Color(red:0.86,green:0.39,blue:0.02)
        case "Current YTD": return scheme == .dark ? Color(red:0.72,green:0.75,blue:0.78) : Color(red:0.39,green:0.43,blue:0.47)
        default: return scheme == .dark ? Color(red:0.56,green:0.79,blue:0.98) : Color(red:0.08,green:0.40,blue:0.75)
        }
    }
    private func roleLabel(_ role: String) -> String? {
        switch role { case "Highest full year": return "High"; case "Lowest full year": return "Low"; case "Current YTD": return "YTD"; default: return nil }
    }
    var body: some View {
        if section.bars.isEmpty {
            Text("Rainfall chart pending complete monthly records.").font(.footnote).padding(12)
        } else {
            let series = order.filter { name in section.bars.contains { $0.series == name } }
            let years = section.rows.map { $0[0] }.reversed()
            let peak = section.bars.map(\.value).max() ?? 0
            let upper = peak <= Double.greatestFiniteMagnitude / 1.18 ? peak * 1.18 : peak
            Chart(section.bars) { bar in
                BarMark(x: .value("Year", bar.category), y: .value("Rainfall (mm)", bar.value))
                    .foregroundStyle(by: .value("Period", bar.series))
                    .cornerRadius(4)
                    .annotation(position: .top) {
                        Text(EstateDashboardData.number(bar.value)).font(.caption2).monospacedDigit()
                            .lineLimit(1).minimumScaleFactor(0.7).frame(width:54).foregroundStyle(color(bar.series))
                    }
                    .accessibilityLabel(bar.category + ", " + bar.series)
                    .accessibilityValue(EstateDashboardData.number(bar.value) + " millimetres")
            }
            .chartForegroundStyleScale(domain: series, range: series.map(color))
            .chartXScale(domain: Array(years))
            .chartYScale(domain: 0...max(upper, 1))
            .chartYAxisLabel("Rainfall (mm)")
            .chartXAxis {
                AxisMarks(values: Array(years)) { axis in
                    AxisValueLabel {
                        if let year = axis.as(String.self) {
                            VStack(spacing:2) {
                                Text(year)
                                if let role = section.bars.first(where: { $0.category == year })?.series,
                                   let label = roleLabel(role) {
                                    Text(label).font(.caption2).foregroundStyle(color(role))
                                }
                            }
                        }
                    }
                }
            }
            .chartLegend(position: .bottom, alignment: .leading, spacing: 8)
            .frame(height: 260).padding(12)
        }
    }
}

