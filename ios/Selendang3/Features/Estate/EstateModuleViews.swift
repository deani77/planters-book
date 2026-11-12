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
