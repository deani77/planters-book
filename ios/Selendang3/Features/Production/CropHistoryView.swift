import SwiftUI
import Charts

struct CropHistoryView: View {
    let history: EstateCropHistory?
    let codes: [String]
    @State private var selection: String?
    private var fieldCodes: [String] { Array(Set(codes + (history?.fields.map(\.fieldCode) ?? []))).sorted() }
    private var selected: String? { selection.flatMap { fieldCodes.contains($0) ? $0 : nil } ?? fieldCodes.first }
    private var currentYear: Int { Calendar.current.component(.year, from: Date()) }
    var body: some View {
        GroupBox("Crop History · t/ha") {
            VStack(alignment: .leading, spacing: 12) {
                ScrollView(.horizontal) {
                    HStack { ForEach(fieldCodes, id: \.self) { code in
                        Button(code) { selection = code }.buttonStyle(.bordered)
                            .tint(selected == code ? .accentColor : .secondary)
                            .accessibilityAddTraits(selected == code ? .isSelected : [])
                    } }
                }
                if let history, let field = history.fields.filter({ $0.fieldCode == selected }).onlyElement {
                    Text("Production cutoff: " + history.cutoff + " · Completed years and partial YTD are not directly comparable.")
                        .font(.footnote)
                    Chart {
                        ForEach(field.years(currentYear: currentYear), id: \.self) { year in
                            if let value = field.yield(year: year) {
                                BarMark(x: .value("Year", String(year)), y: .value("t/ha", value))
                                    .foregroundStyle(year == field.productionYear ? Color.orange : Color.accentColor)
                            }
                        }
                    }.frame(height: 180).chartYAxisLabel("t/ha")
                    ForEach(field.years(currentYear: currentYear), id: \.self) { year in
                        HStack {
                            Text(String(year) + (year == field.productionYear ? " YTD" : ""))
                            Spacer()
                            Text(field.yield(year: year).map { String(format: "%.2f", $0) } ?? "N/A")
                        }
                    }
                    Text("Actual cumulative production: \(String(format: "%.2f", field.productionTonnes)) tonnes · Planted area: \(field.plantedAreaHa.map { String(format: "%.2f ha", $0) } ?? "N/A")")
                        .font(.footnote)
                    Text("YTD = actual cumulative tonnes ÷ planted ha. " + field.areaSource)
                        .font(.footnote).foregroundStyle(.secondary)
                    if field.plantedAreaHa == nil { Text("Planted hectares unavailable; YTD yield is N/A.") }
                } else {
                    Text("Crop history unavailable. Import records in Settings → Estate Data Management.")
                }
            }.padding(.top, 8)
        }
    }
}

private extension Array {
    var onlyElement: Element? { count == 1 ? first : nil }
}
