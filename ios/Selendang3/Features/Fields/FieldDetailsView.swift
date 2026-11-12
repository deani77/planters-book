import SwiftUI

struct FieldDetailsView: View {
    @ObservedObject private var estate = EstateReferenceStore.shared
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                GroupBox("Field Dashboard") {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Fields available: " + (estate.reference == nil ? "Pending" : String(estateFieldCodes(estate.reference).count)))
                        Text("Basic field profiles · area, palms, planting and age records")
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
                                Text("Basic records for this field only. Crop, harvesting and productivity records are under Crop Production.")
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
