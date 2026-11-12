import SwiftUI

struct UpkeepCultivationView: View {
    @ObservedObject private var estate = EstateReferenceStore.shared
    var body: some View {
        let manuring = EstateDashboardData(reference: estate.reference, today: Date()).operationalCards[3]
        List {
            Section("Upkeep and Cultivation Dashboard") {
                LabeledContent("Weeding progress", value: "Pending")
                LabeledContent("Manuring progress", value: manuring.rows[0][1] + " %")
                Text(manuring.note).font(.footnote).foregroundStyle(.secondary)
                LabeledContent("Pest and Disease", value: "Pending")
            }
            Section("Major Upkeep Work") {
                LabeledContent("Weeding", value: "Pending")
                LabeledContent("Manuring", value: manuring.rows[0][1] + " %")
                LabeledContent("Pest and Disease", value: "Pending")
                LabeledContent("Water Management", value: "Pending")
                LabeledContent("Road Management", value: "Pending")
                LabeledContent("Others", value: "Pending")
            }
            Section {
                Text("Upkeep records are shown at estate level. Imported field profiles are not presented as field-level upkeep activity.")
                Text("Crop production activities are recorded under Crop Production.")
            }
        }
        .estateScreen().navigationTitle("Upkeep and Cultivation").navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
    }
}
