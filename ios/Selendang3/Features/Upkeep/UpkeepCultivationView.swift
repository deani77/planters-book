import SwiftUI

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
