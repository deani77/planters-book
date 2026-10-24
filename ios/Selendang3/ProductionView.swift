import SwiftUI

struct ProductionView: View {
    @ObservedObject private var estate = EstateReferenceStore.shared
    private let columns = [GridItem(.adaptive(minimum: 92, maximum: 200))]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("VPS data · Read-only · Available offline")
                    .font(.footnote).foregroundStyle(.secondary)
                GroupBox("Crop Production Dashboard") {
                    TimelineView(.periodic(from: Date(), by: 60)) { context in
                        CropProductionTableView(data: CropProductionTableData(reference: estate.reference, today: context.date))
                    }.padding(.top, 8)
                }
                Text("Field Crop Records").font(.headline)
                Text("Select a field to view its report. Refresh records in Settings → Estate Data Sync.")
                    .font(.footnote).foregroundStyle(.secondary)
                LazyVGrid(columns: columns, spacing: 10) {
                    ForEach(estateFieldCodes(estate.reference), id: \.self) { code in
                        NavigationLink {
                            FieldCropReportView(code: code)
                        } label: {
                            Text(code).font(.subheadline.weight(.semibold))
                                .frame(maxWidth: .infinity, minHeight: 44)
                                .background(EstateTheme.softAccent, in: Capsule())
                                .overlay(Capsule().stroke(EstateTheme.border, lineWidth: 1))
                        }.buttonStyle(.plain)
                    }
                }
                if estateFieldCodes(estate.reference).isEmpty {
                    Text("No field reports saved. Download estate data from the VPS in Settings.")
                        .font(.footnote)
                }
            }.padding()
        }
        .estateScreen().navigationTitle("Crop Production")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
    }
}

private struct FieldCropReportView: View {
    let code: String
    @ObservedObject private var estate = EstateReferenceStore.shared
    var body: some View {
        List {
            Section {
                Text("VPS report · Read-only")
                Text(estate.reference?.reportingPeriod ?? "No saved estate report")
                    .font(.footnote).foregroundStyle(.secondary)
            }
            let tables = estateFieldCropTables(estate.reference, code: code)
            if tables.isEmpty { Text("Crop records pending for this field.") }
            ForEach(tables) { table in
                Section(table.id == "fields" ? "Crop, harvesting and productivity" : table.title) {
                    ForEach(table.rows.indices, id: \.self) { index in
                        ForEach(table.columns.indices, id: \.self) { column in
                            LabeledContent(table.columns[column], value: table.rows[index][column]?.text ?? "Pending")
                        }
                    }
                }
            }
        }.estateScreen().navigationTitle(code + " · Crop Records")
    }
}
