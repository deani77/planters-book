import SwiftUI

struct CropProductionTableView: View {
    let data: CropProductionTableData
    @State private var measure = 0
    private var isPad: Bool { UIDevice.current.userInterfaceIdiom == .pad }
    private let measures = ["Crop (mt)", "Yield (mt/ha)", "Achievement (%)"]
    private var identityWidth: CGFloat { isPad ? 130 : 80 }
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(data.note).font(.footnote).foregroundStyle(.secondary)
            Picker("Measure", selection: $measure) {
                ForEach(measures.indices, id: \.self) { Text(measures[$0]).tag($0) }
            }.pickerStyle(.segmented)
            if data.rows.isEmpty {
                Text("Field records pending. Import estate data to populate this table.")
            } else {
                VStack(spacing: 0) {
                    HStack(spacing: 0) {
                        heading("Field\nArea (ha)").frame(width: identityWidth)
                        ForEach(["This Month", "Year to Date", "Full Year"], id: \.self) { title in
                            heading(title).frame(maxWidth: .infinity)
                        }
                    }.frame(height: 48).background(EstateTheme.softAccent)
                    ForEach(data.rows) { row in tableRow(row) }
                    if let total = data.total { tableRow(total, total: true) }
                }.clipShape(RoundedRectangle(cornerRadius: 8))
                Text("T = Target · A = Actual. Choose Crop, Yield or Achievement above. Actuals are sent-to-mill crop. Full Year compares actual YTD with the annual target; no full-year actual is assumed.")
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
    }
    private func heading(_ title: String) -> some View {
        Text(title).font(isPad ? .subheadline.weight(.semibold) : .caption.weight(.semibold))
            .multilineTextAlignment(.center).padding(.horizontal, 3).frame(maxHeight: .infinity)
    }
    private func tableRow(_ row: CropProductionTableRow, total: Bool = false) -> some View {
        HStack(spacing: 0) {
            VStack(spacing: 3) {
                Text(row.field).fontWeight(.semibold)
                Text(row.area).foregroundStyle(.secondary)
            }.frame(width: identityWidth)
            ForEach(0..<3) { period in
                VStack(spacing: 3) {
                    if measure == 2 {
                        Text(row.values[10 + period] + (row.values[10 + period] == "Pending" ? "" : " %"))
                            .fontWeight(.semibold)
                    } else {
                        let target = row.values[(measure == 0 ? 0 : 3) + period]
                        Text("T " + target).fontWeight(.medium)
                            .accessibilityLabel("Target " + target)
                        if period < 2 {
                            let actual = row.values[(measure == 0 ? 6 : 8) + period]
                            Text("A " + actual).fontWeight(.semibold).foregroundStyle(EstateTheme.accent)
                                .accessibilityLabel("Actual " + actual)
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .overlay(alignment: .leading) { Rectangle().fill(EstateTheme.border).frame(width: 0.5) }
            }
        }
        .font(isPad ? .subheadline : .caption2).monospacedDigit().multilineTextAlignment(.center)
        .minimumScaleFactor(0.7)
        .frame(height: isPad ? 56 : 48)
        .background(total ? EstateTheme.accent.opacity(0.15) : EstateTheme.surface)
        .overlay(alignment: .bottom) { Rectangle().fill(EstateTheme.border).frame(height: 0.5) }
    }
}
