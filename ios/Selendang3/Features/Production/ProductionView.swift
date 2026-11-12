import SwiftUI

struct ProductionView: View {
    @ObservedObject private var estate = EstateReferenceStore.shared
    private let columns = [GridItem(.adaptive(minimum: 92, maximum: 200))]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("Local data · Read-only · Available offline")
                    .font(.footnote).foregroundStyle(.secondary)
                GroupBox("Crop Production Dashboard") {
                    TimelineView(.periodic(from: Date(), by: 60)) { context in
                        CropProductionTableView(data: CropProductionTableData(reference: estate.reference, today: context.date))
                    }.padding(.top, 8)
                }
                CropHistoryView(history: estate.reference?.cropHistory, codes: estateFieldCodes(estate.reference))
                Text("Field Crop Records").font(.headline)
                Text("Select a field to view its report. Import records in Settings → Estate Data Management.")
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
                    Text("No field reports saved. Import estate data in Settings → Estate Data Management.")
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
        TimelineView(.periodic(from: Date(), by: 60)) { context in
            let summary = CropProductionTableData(reference: estate.reference, today: context.date).fieldSummary(code: code)
            List {
                Section {
                    Text("Local report · Read-only")
                    if let summary = summary { Text(summary.note).font(.footnote).foregroundStyle(.secondary) }
                }
                if let summary = summary {
                    ForEach(summary.groups) { group in
                        Section(group.title) {
                            ForEach(group.rows) { row in LabeledContent(row.label, value: row.value) }
                            if !group.note.isEmpty { Text(group.note).font(.footnote).foregroundStyle(.secondary) }
                            if group.title == "Harvesting" {
                                Text(summary.crewNote).font(.footnote).foregroundStyle(.secondary)
                            }
                        }
                    }
                } else { Text("Crop records pending for this field.") }
            }
        }.estateScreen().navigationTitle(code + " · Crop Records")
    }
}


