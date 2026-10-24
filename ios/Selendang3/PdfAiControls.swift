import SwiftUI
import UniformTypeIdentifiers

private struct SharedEstatePDF: Identifiable {
    let id = UUID()
    let url: URL
}

struct PdfAiControls: View {
    let prompt: String
    let onReview: (String) -> Void
    @State private var selectPDF = false
    @State private var sharedPDF: SharedEstatePDF?
    @State private var message: String?
    @State private var showPaste = false
    @State private var result = ""
    @State private var busy = false
    @State private var reviewAfterDismiss: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Read PDF with AI").font(.headline)
            Text("Choose a PDF, send it to a compatible AI app, then return to review the extracted data. The AI app may upload the PDF and require internet. Nothing is saved automatically.").font(.footnote)
            Button(busy ? "Preparing PDF…" : "Read PDF with AI") { selectPDF = true }.disabled(busy)
            Button("Copy AI Instructions") {
                UIPasteboard.general.string = prompt
                message = "Instructions copied. Paste them into your AI app with the PDF."
            }
            Button("Paste AI Result") { showPaste = true }
            if let message { Text(message).font(.footnote) }
        }
        .fileImporter(isPresented: $selectPDF, allowedContentTypes: [.pdf]) { selection in
            switch selection {
            case .failure(let error): message = error.localizedDescription
            case .success(let selected):
                busy = true
                Task {
                    do {
                        let copy = try await Task.detached(priority: .userInitiated) {
                            let access = selected.startAccessingSecurityScopedResource()
                            defer { if access { selected.stopAccessingSecurityScopedResource() } }
                            let input = try FileHandle(forReadingFrom: selected)
                            defer { try? input.close() }
                            let data = try input.read(upToCount: 25 * 1024 * 1024 + 1) ?? Data()
                            guard data.count <= 25 * 1024 * 1024 else { throw ProductionError.invalid("Choose a PDF smaller than 25 MB.") }
                            guard data.prefix(5) == Data("%PDF-".utf8) else { throw ProductionError.invalid("The selected file is not a PDF.") }
                            let directory = FileManager.default.temporaryDirectory.appendingPathComponent("pdf-ai", isDirectory: true)
                            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
                            for old in (try? FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: [.contentModificationDateKey])) ?? [] {
                                if let date = try? old.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate,
                                   date < Date().addingTimeInterval(-86400) { try? FileManager.default.removeItem(at: old) }
                            }
                            let copy = directory.appendingPathComponent("estate-\(UUID().uuidString).pdf")
                            try data.write(to: copy, options: .atomic)
                            return copy
                        }.value
                        sharedPDF = SharedEstatePDF(url: copy)
                        message = "Choose an app that accepts PDFs. If instructions are missing, use Copy AI Instructions. Return here to paste its JSON result or import its JSON file."
                    } catch { message = error.localizedDescription }
                    busy = false
                }
            }
        }
        .sheet(item: $sharedPDF) { pdf in
            EstatePDFShareSheet(url: pdf.url, prompt: prompt)
        }
        .sheet(isPresented: $showPaste, onDismiss: {
            if let text = reviewAfterDismiss {
                reviewAfterDismiss = nil
                onReview(text)
            }
        }) {
            NavigationStack {
                VStack(alignment: .leading) {
                    Text("Paste the JSON returned by your AI app, then review before saving.")
                    TextEditor(text: $result)
                        .onChange(of: result) { value in
                            if value.utf8.count > 2 * 1024 * 1024 {
                                result = ""
                                message = "AI result exceeds 2 MB. Use a smaller result."
                            }
                        }
                }.padding().navigationTitle("Paste AI Result")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Cancel") { result = ""; showPaste = false } }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Review Data") {
                            reviewAfterDismiss = result
                            result = ""; showPaste = false
                        }.disabled(result.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
        }
    }
}

private struct EstatePDFShareSheet: UIViewControllerRepresentable {
    let url: URL
    let prompt: String
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url, prompt], applicationActivities: nil)
    }
    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
