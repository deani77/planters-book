import Foundation
import Combine

private final class EstateNoRedirect: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }
}

@MainActor
final class EstateReferenceStore: ObservableObject {
    static let shared = EstateReferenceStore()
    @Published private(set) var reference: EstateReference?
    @Published private(set) var storageError: String?
    @Published private(set) var lastChecked: Date?
    private let url: URL
    init() {
        url = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("estate-reference-v1.json")
        lastChecked = UserDefaults.standard.object(forKey: "estate_last_checked") as? Date
        do {
            if FileManager.default.fileExists(atPath: url.path) { reference = try EstateReference.parse(Data(contentsOf: url)) }
        } catch { storageError = "Saved estate data could not be read. Download a new update to recover; the file is retained." }
    }
    func download(address: String, token: String) async throws -> (EstateReference, Data) {
        let endpoint = try EstateSyncEndpoint(address: address, token: token)
        let session = URLSession(configuration: .ephemeral, delegate: EstateNoRedirect(), delegateQueue: nil)
        defer { session.invalidateAndCancel() }
        var request = URLRequest(url: endpoint.snapshot, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 30)
        request.setValue("Bearer " + endpoint.token, forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (bytes, response) = try await session.bytes(for: request)
        guard let http = response as? HTTPURLResponse else { throw ProductionError.invalid("No server response.") }
        guard http.statusCode == 200 else {
            if http.statusCode == 401 || http.statusCode == 403 { throw ProductionError.invalid("Server rejected the access token (HTTP \(http.statusCode)).") }
            throw ProductionError.invalid("Estate download failed (HTTP \(http.statusCode)); saved data is unchanged.")
        }
        var data = Data()
        for try await byte in bytes {
            guard data.count < 2*1024*1024 else { throw ProductionError.invalid("Estate update exceeds 2 MB.") }
            data.append(byte)
        }
        let update = try EstateReference.parse(data)
        guard update.revision >= (reference?.revision ?? 0) else { throw ProductionError.invalid("Server estate data is older than the saved revision.") }
        lastChecked = Date(); UserDefaults.standard.set(lastChecked, forKey: "estate_last_checked")
        return (update, data)
    }
    func apply(_ data: Data) throws {
        let update = try EstateReference.parse(data)
        guard update.revision > (reference?.revision ?? 0) else { throw ProductionError.invalid("This revision is already saved or older.") }
        let directory = url.deletingLastPathComponent()
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        if let previous = try? Data(contentsOf: url) {
            try previous.write(to: directory.appendingPathComponent("estate-reference-previous.json"), options: .atomic)
        }
        try data.write(to: url, options: .atomic)
        reference = update; storageError = nil
    }
}
