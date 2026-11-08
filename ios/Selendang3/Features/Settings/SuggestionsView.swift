import SwiftUI

// Feedback address supplied by the app owner.
private let suggestionsEmail = "deani77@gmail.com"

struct SuggestionsView: View {
    @AppStorage("suggestion_draft") private var suggestion = ""
    @Environment(\.openURL) private var openURL
    @State private var status: String?

    var body: some View {
        Form {
            Section {
                Text("Share an idea or report a problem. Your suggestion stays as a draft on this device until you send it through your email app.")
                TextEditor(text: $suggestion).frame(minHeight: 180)
                    .accessibilityLabel("Your suggestion")
                    .onChange(of: suggestion) { value in
                        if value.count > 2000 { suggestion = String(value.prefix(2000)) }
                        status = nil
                    }
                Text("\(suggestion.count)/2000").font(.footnote)
                if suggestionsEmail.isEmpty {
                    Text("The receiving email address has not been configured yet. Your draft is saved.")
                } else { Text("To: " + suggestionsEmail) }
                Button("Email Suggestion") {
                    var components = URLComponents()
                    components.scheme = "mailto"
                    components.path = suggestionsEmail
                    components.queryItems = [
                        URLQueryItem(name: "subject", value: "Planters Book — Suggestion"),
                        URLQueryItem(name: "body", value: "Suggestion:\n" + suggestion.trimmingCharacters(in: .whitespacesAndNewlines) + "\n\nPlanters Book · iOS · " + (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""))
                    ]
                    components.percentEncodedQuery = components.percentEncodedQuery?.replacingOccurrences(of: "+", with: "%2B")
                    guard let url = components.url else { status = "Could not prepare email. Your draft is saved."; return }
                    openURL(url) { accepted in
                        status = accepted ? "Email app opened. Tap Send there to submit. Your draft remains saved here." : "No email app is available. Set up an email app, then try again. Your draft is saved."
                    }
                }.disabled(suggestionsEmail.isEmpty || suggestion.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                Button("Clear Draft", role: .destructive) { suggestion = ""; status = "Draft cleared." }
                    .disabled(suggestion.isEmpty)
                if let status { Text(status) }
            }
        }
        .estateScreen().navigationTitle("Suggestions")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
    }
}
