import SwiftUI
import PDFKit
import UniformTypeIdentifiers

/// An optional user-owned PDF stored separately from estate reports and production drafts.
struct EstateMapCard: View {
    @State private var showsMap = false

    var body: some View {
        Button { showsMap = true } label: {
            HStack(spacing: 12) {
                Image(systemName: "map.fill").font(.title2)
                Text("Estate Map").font(.headline)
                Spacer()
                Image(systemName: "chevron.right")
            }
            .foregroundStyle(EstateTheme.accent)
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 56)
            .background(EstateTheme.surface, in: RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(EstateTheme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityHint("Open or import your estate PDF map")
        .fullScreenCover(isPresented: $showsMap) { EstateMapView() }
    }
}

private enum EstateMapStorage {
    static let maximumBytes = 50 * 1024 * 1024

    static func savedURL() throws -> URL {
        try FileManager.default.url(for: .applicationSupportDirectory,
                                    in: .userDomainMask, appropriateFor: nil, create: true)
            .appendingPathComponent("UserEstateMap.pdf")
    }

    static func load() throws -> PDFDocument? {
        let url = try savedURL()
        guard FileManager.default.fileExists(atPath: url.path) else { return nil }
        guard let document = PDFDocument(url: url), !document.isLocked,
              document.pageCount > 0 else {
            throw MapError.invalidPDF
        }
        return document
    }

    static func importPDF(from url: URL) throws -> PDFDocument {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        let values = try url.resourceValues(forKeys: [.fileSizeKey, .isRegularFileKey])
        guard values.isRegularFile == true, let size = values.fileSize,
              size > 0, size <= maximumBytes else { throw MapError.sizeLimit }
        let data = try Data(contentsOf: url)
        guard !data.isEmpty, data.count <= maximumBytes else { throw MapError.sizeLimit }
        guard let document = PDFDocument(data: data), !document.isLocked,
              document.pageCount > 0 else { throw MapError.invalidPDF }
        // Atomic replacement preserves the previous map if validation or saving fails.
        try data.write(to: savedURL(), options: .atomic)
        return document
    }

    enum MapError: LocalizedError {
        case sizeLimit, invalidPDF
        var errorDescription: String? {
            switch self {
            case .sizeLimit: return "Choose a PDF file no larger than 50 MB."
            case .invalidPDF: return "Choose a readable PDF with at least one page and no password lock."
            }
        }
    }
}

private struct EstateMapView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var document: PDFDocument?
    @State private var showsImporter = false
    @State private var showsReplaceConfirmation = false
    @State private var errorMessage: String?
    @State private var resetID = UUID()

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { dismiss() } label: {
                    Label("Back", systemImage: "chevron.left").frame(minHeight: 44)
                }
                Spacer()
                Text("Estate Map").font(.headline)
                Spacer()
                Menu {
                    Button(document == nil ? "Import PDF Map" : "Replace PDF Map") {
                        if document == nil { showsImporter = true }
                        else { showsReplaceConfirmation = true }
                    }
                    Button("Reset View") { resetID = UUID() }
                        .disabled(document == nil)
                } label: {
                    Label("Map Options", systemImage: "ellipsis.circle").frame(minHeight: 44)
                }
            }
            .padding(.horizontal, 16)
            .background(EstateTheme.surface)
            Divider()
            if let document {
                EstateMapPDF(document: document)
                    .id(resetID)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                Text("Pinch to zoom · Drag to explore")
                    .font(.footnote).foregroundStyle(.secondary).padding(8)
            } else {
                Spacer()
                Text("Add your estate map").font(.headline)
                Text("Import a PDF from Files to view it offline on this iPad.")
                    .multilineTextAlignment(.center).padding()
                Button("Import PDF Map") { showsImporter = true }
                    .buttonStyle(.borderedProminent)
                Spacer()
            }
        }
        .background(EstateTheme.background)
        .tint(EstateTheme.accent)
        .onAppear {
            do { document = try EstateMapStorage.load() }
            catch { errorMessage = error.localizedDescription }
        }
        .confirmationDialog("Replace the saved estate map?", isPresented: $showsReplaceConfirmation,
                            titleVisibility: .visible) {
            Button("Choose Replacement PDF") { showsImporter = true }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("The current map is replaced only after the new PDF is validated and saved.")
        }
        .fileImporter(isPresented: $showsImporter, allowedContentTypes: [.pdf]) { result in
            do {
                let url = try result.get()
                document = try EstateMapStorage.importPDF(from: url)
                resetID = UUID()
            } catch { errorMessage = error.localizedDescription }
        }
        .alert("Estate Map", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }
}

private struct EstateMapPDF: UIViewRepresentable {
    let document: PDFDocument

    func makeUIView(context: Context) -> PDFView {
        let view = PDFView()
        view.displayMode = .singlePageContinuous
        view.document = document
        view.autoScales = true
        view.backgroundColor = .secondarySystemBackground
        return view
    }

    func updateUIView(_ uiView: PDFView, context: Context) {}
}
