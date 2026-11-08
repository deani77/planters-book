import SwiftUI
import Charts

struct EstateDashboardView: View {
    @AppStorage("estate_name") private var estateName = ""
    @ObservedObject private var estate = EstateReferenceStore.shared
    var body: some View {
        TimelineView(.periodic(from:Date(),by:60)) { context in
            let data = EstateDashboardData(reference:estate.reference,today:context.date)
            ScrollView {
                VStack(alignment:.leading,spacing:16) {
                    Text("ESTATE DETAILS · " + (estateName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Your Estate" : estateName)).font(.title2.bold())
                    panel(data.areaSummary)
                    if let reference = estate.reference {
                        Text("Saved estate revision \(reference.revision)").font(.headline)
                        Text(reference.reportingPeriod).font(.footnote)
                    } else { Text("No estate data imported. Open Settings → Estate Data Management.") }
                    Text("Uses saved offline records. Each measure retains its reporting period. Pending means data is unavailable.")
                        .font(.footnote).foregroundStyle(.secondary)
                    NavigationLink("All Estate Records") { EstateRecordsView() }
                    LazyVGrid(columns:[GridItem(.adaptive(minimum:320),alignment:.top)],alignment:.leading,spacing:16) {
                        ForEach(data.panels) { section in panel(section) }
                    }
                }.padding()
            }
        }
        .toolbar(.visible,for:.navigationBar)
        .estateScreen()
        .navigationTitle("Estate Details").navigationBarTitleDisplayMode(.inline)
    }
    private func panel(_ section: EstateDashboardPanel) -> some View {
        VStack(alignment:.leading,spacing:0) {
            Text(section.title).font(.headline).foregroundStyle(EstateTheme.accent).frame(maxWidth:.infinity,alignment:.leading)
                .padding(12).background(EstateTheme.softAccent)
            if section.title == "Rainfall" {
                RainfallBarChart(section: section)
            } else if !section.bars.isEmpty {
                Chart(section.bars) { bar in
                    BarMark(x:.value("Measure",bar.category),y:.value(section.unit,bar.value))
                        .foregroundStyle(by:.value("Series",bar.series)).position(by:.value("Series",bar.series))
                }
                .chartForegroundStyleScale(domain:section.title == "YPH" ? ["Actual","Budget"] : ["Budget Year","Actual YTD"],range:section.title == "YPH" ? [Color.blue,Color.orange] : [Color.orange,Color.blue])
                .chartYScale(domain:0...(max(section.bars.map(\.value).max() ?? 1,1)*1.12))
                .chartYAxisLabel(section.unit)
                .frame(height:230).padding(12)
                .accessibilityLabel(section.title+" comparison. Exact values are listed below.")
            }
            if section.rows.isEmpty { Text("Pending complete source records").padding(12) }
            ScrollView(.horizontal) {
                VStack(alignment:.leading,spacing:0) {
                    tableRow(section.headers,bold:true)
                    ForEach(section.rows.indices,id:\.self) { index in
                        Divider(); tableRow(section.rows[index],bold:false)
                    }
                }.padding(8)
            }
            if !section.note.isEmpty { Text(section.note).font(.footnote).foregroundStyle(.secondary).padding(12) }
        }
        .background(EstateTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(EstateTheme.border, lineWidth: 1))
    }
    private func tableRow(_ cells: [String], bold: Bool) -> some View {
        HStack(alignment:.top,spacing:12) {
            ForEach(cells.indices,id:\.self) { index in
                Text(cells[index]).font(.subheadline).fontWeight(bold ? .semibold : .regular)
                    .frame(width:index == 0 ? 155 : 100,alignment:.leading)
            }
        }.padding(.vertical,8)
    }
}


