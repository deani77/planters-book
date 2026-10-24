import SwiftUI

struct UserSettingsView: View {
    @AppStorage("theme") private var theme = "system"
    @AppStorage("server_address") private var address = ""
    @State private var accessToken = ""
    @State private var syncMessage: String?
    @StateObject private var estate = EstateReferenceStore.shared
    @State private var syncBusy = false
    @State private var pendingEstateData: Data?
    @State private var pendingEstate: EstateReference?
    @State private var showEstatePreview = false

    var body: some View {
        Form {
            Section {
                NavigationLink("Inbox") {
                    SettingsPlaceholder(title: "Inbox", heading: "No messages yet",
                        message: "Announcements and replies will appear here once messaging is connected.")
                }
                NavigationLink("User Profile") { UserProfileView() }
                NavigationLink("Appearance") {
                    Form {
                        Picker("Theme", selection: $theme) {
                            Text("Light").tag("light")
                            Text("Dark").tag("dark")
                            Text("Follow System").tag("system")
                        }.pickerStyle(.inline)
                    }.estateScreen().navigationTitle("Appearance")
                }
                NavigationLink("About") { ModuleView(title: "About") }
            } footer: {
                Text("Profile and appearance are saved on this device. The access token is kept only for this Settings session.")
            }
            Section("Estate Data Sync") {
                TextField("Full HTTPS snapshot address", text: $address).textInputAutocapitalization(.never).autocorrectionDisabled()
                SecureField("Access token", text: $accessToken).textInputAutocapitalization(.never).autocorrectionDisabled()
                Text("Downloads estate data from the VPS only. Confirm new updates to replace the offline copy. Data entry and uploads are unavailable.").font(.footnote)
                if let reference = estate.reference { Text("Saved estate revision \(reference.revision) · available offline") }
                if let checked = estate.lastChecked { Text("Estate last checked: " + checked.formatted()).font(.footnote) }
                if let error = estate.storageError { Text(error).foregroundStyle(.red) }
                Button(syncBusy ? "Syncing…" : "Sync Now") {
                    syncBusy = true
                    syncMessage = "Connecting to the estate server…"
                    Task {
                        var results: [String] = []
                        var endpoint: EstateSyncEndpoint
                        do { endpoint = try EstateSyncEndpoint(address: address, token: accessToken) }
                        catch { syncMessage = error.localizedDescription; syncBusy = false; return }
                        do {
                            let (update, data) = try await estate.download(address: endpoint.snapshot.absoluteString, token: endpoint.token)
                            if update.revision > (estate.reference?.revision ?? 0) {
                                pendingEstate = update; pendingEstateData = data; showEstatePreview = true
                                results.append("Estate revision \(update.revision) downloaded. Review and confirm the import.")
                            } else { results.append("Estate is up to date at revision \(update.revision).") }
                        } catch { results.append("Estate sync failed: " + error.localizedDescription) }
                        syncMessage = results.joined(separator: "\n\n")
                        syncBusy = false
                    }
                }.disabled(syncBusy)
                if syncBusy { ProgressView("Synchronizing…") }
                Text("Your token stays in this Settings session and is never stored in the app.").font(.footnote)
                if let message = syncMessage { Text(message) }
            }

        }
        .alert("Import Estate Update?", isPresented: $showEstatePreview) {
            Button("Import Estate Update") {
                do {
                    guard let data = pendingEstateData else { return }
                    try estate.apply(data)
                    syncMessage = (syncMessage ?? "") + "\nEstate revision \(estate.reference?.revision ?? 0) saved. These records are available offline."
                } catch { syncMessage = error.localizedDescription }
                pendingEstateData = nil; pendingEstate = nil
            }
            Button("Cancel", role: .cancel) { pendingEstateData = nil; pendingEstate = nil }
        } message: {
            if let update = pendingEstate {
                Text("Revision \(update.revision)\n\(update.reportingPeriod)\n\(update.sections.reduce(0) { $0 + $1.rows.count }) rows across seven sections. This replaces the saved estate reference data. These server records remain available offline.")
            }
        }
        .estateScreen().navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
    }

}

private struct UserProfileView: View {
    @AppStorage("profile_name") private var savedName = ""
    @AppStorage("profile_position") private var savedPosition = ""
    @AppStorage("profile_phone") private var savedPhone = ""
    @State private var name = ""
    @State private var position = ""
    @State private var phone = ""
    @State private var saved = false

    var body: some View {
        Form {
            Section {
                TextField("Display name", text: $name)
                TextField("Position (optional)", text: $position)
                TextField("Phone (optional)", text: $phone)
                    .keyboardType(.phonePad)
                Button("Save Profile") {
                    savedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
                    savedPosition = position.trimmingCharacters(in: .whitespacesAndNewlines)
                    savedPhone = phone.trimmingCharacters(in: .whitespacesAndNewlines)
                    saved = true
                }
                .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                if saved { Text("Profile saved on this device.") }
            } footer: {
                Text("Your profile on this device. This is not a signed-in account.")
            }
        }
        .estateScreen().navigationTitle("User Profile")
        .onAppear {
            name = savedName
            position = savedPosition
            phone = savedPhone
        }
        .onChange(of: name) { _ in saved = false }
        .onChange(of: position) { _ in saved = false }
        .onChange(of: phone) { _ in saved = false }
    }
}

private struct SettingsPlaceholder: View {
    let title: String
    let heading: String
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            Text(heading).font(.title2.bold())
            Text(message).multilineTextAlignment(.center)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .estateScreen().navigationTitle(title)
    }
}

