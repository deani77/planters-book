import SwiftUI

struct EstateRecordsView: View {
    @ObservedObject private var estate = EstateReferenceStore.shared
    var body: some View {
        List {
            if let reference = estate.reference {
                ForEach(reference.sections) { table in
                    NavigationLink(table.title) { EstateRecordTableView(table: table) }
                }
            } else { Text("No estate data imported. Open Settings → Estate Data Management.") }
        }
        .estateScreen().navigationTitle("All Estate Records")
    }
}

struct EstateRecordTableView: View {
    let table: EstateReferenceTable
    var body: some View {
        List {
            if table.rows.isEmpty { Text("Pending verified records") }
            ForEach(table.rows.indices, id: \.self) { index in
                Section {
                    ForEach(table.columns.indices, id: \.self) { column in
                        LabeledContent(table.columns[column],
                            value: column < table.rows[index].count ? (table.rows[index][column]?.text ?? "Pending") : "Pending")
                    }
                }
            }
        }
        .estateScreen().navigationTitle(table.title)
    }
}

struct FieldDetailsView: View {
    @ObservedObject private var estate = EstateReferenceStore.shared
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                GroupBox("Field Overview") {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Fields available: " + (estate.reference == nil ? "Pending" : String(estateFieldCodes(estate.reference).count)))
                        Text("Planting, maturity, area, palm population, terrain, soil and average bunch weight")
                        Text("Choose a field for its basic information. Crop and harvesting records are under Crop Production.")
                            .font(.footnote).foregroundStyle(.secondary)
                    }.frame(maxWidth: .infinity, alignment: .leading)
                }
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(estateFieldCodes(estate.reference), id: \.self) { code in
                        NavigationLink {
                            List {
                                if let profile = estate.reference?.sourceFieldProfile(code) {
                                    Section("Source profile · \(String(profile.reportYear))") {
                                        LabeledContent("Planted area (ha)", value: EstateDashboardData.number(profile.plantedAreaHa))
                                        LabeledContent("Unplanted area (ha)", value: EstateDashboardData.number(profile.unplantedAreaHa))
                                        LabeledContent("Division stands/ha", value: EstateDashboardData.number(profile.divisionStandsPerHa))
                                        LabeledContent("Division palm height (m)", value: EstateDashboardData.number(profile.divisionPalmHeightM))
                                        LabeledContent("Maturity", value: profile.maturity)
                                        Text("Imported profile: report year is shown above. Density and height apply to the division; source date is unconfirmed.").font(.footnote).foregroundStyle(.secondary)
                                    }
                                    ForEach(profile.blocks, id: \.blockId) { block in
                                        Section("Block " + block.blockName) {
                                            LabeledContent("Block identifier", value: block.blockId)
                                            LabeledContent("Planted area (ha)", value: EstateDashboardData.number(block.plantedAreaHa))
                                            LabeledContent("Unplanted area (ha)", value: EstateDashboardData.number(block.unplantedAreaHa))
                                            LabeledContent("Soil code", value: block.soilCode ?? "Pending")
                                            LabeledContent("Harvesting year", value: String(block.harvestingYear))
                                            if let note = block.sourceNote { Text(note).font(.footnote).foregroundStyle(.secondary) }
                                        }
                                    }
                                }
                                let tables = estateFieldTables(estate.reference, code: code)
                                ForEach(tables) { table in
                                    Section(table.title) {
                                        ForEach(table.rows.indices, id: \.self) { index in
                                            ForEach(table.columns.indices, id: \.self) { column in
                                                LabeledContent(table.columns[column],
                                                    value: table.rows[index][column]?.text ?? "Pending")
                                            }
                                        }
                                    }
                                }
                                Text("Field information and imported source records. Production and upkeep are maintained in their respective tabs.")
                                    .font(.footnote)
                            }.estateScreen().navigationTitle(code)
                        } label: {
                            Text(code).font(.headline).frame(maxWidth: .infinity, minHeight: 60)
                        }.buttonStyle(.borderedProminent)
                    }
                }
                if estateFieldCodes(estate.reference).isEmpty {
                    Text("Import estate records through Settings → Estate Data Management to see field buttons.")
                }
            }.padding()
        }
        .estateScreen().navigationTitle("Field Details").navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
    }
}

struct UpkeepCultivationView: View {
    @ObservedObject private var estate = EstateReferenceStore.shared
    private let categories = ["Weeding", "Manuring", "Pest & Disease", "Water Management", "Road Management", "Others"]
    var body: some View {
        let manuring = EstateDashboardData(reference: estate.reference, today: Date()).operationalCards[3]
        List {
            Section("Operations Overview") {
                LabeledContent("Weeding", value: "Field-based progress: Pending")
                LabeledContent("Manuring", value: "Existing estate programme progress: " + manuring.rows[0][1] + " %")
                Text(manuring.note).font(.footnote).foregroundStyle(.secondary)
                Text("Field-based manuring totals: Pending").font(.footnote)
                LabeledContent("Pest & Disease", value: "Active cases and treated area: Pending")
            }
            Section("Field Maintenance Records") {
                ForEach(estateFieldCodes(estate.reference), id: \.self) { code in
                    NavigationLink(code) {
                        List {
                            ForEach(categories, id: \.self) { category in
                                Section(category) { Text("Field activity records: Pending") }
                            }
                            Text("Field-level upkeep records are not yet available in the local data model. Estate-level manuring figures are not assigned to individual fields.")
                        }.estateScreen().navigationTitle(code + " · Upkeep & Cultivation")
                    }
                }
                if estateFieldCodes(estate.reference).isEmpty {
                    Text("Import field information in Settings → Estate Data Management to see fields.")
                }
            }
        }.estateScreen().navigationTitle("Upkeep and Cultivation").navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
    }
}
