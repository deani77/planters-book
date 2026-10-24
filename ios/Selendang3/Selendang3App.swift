import SwiftUI

@main
struct Selendang3App: App {
    @AppStorage("theme") private var theme = "system"

    var body: some Scene {
        WindowGroup {
            HomeView()
                .preferredColorScheme(theme == "dark" ? .dark : theme == "light" ? .light : nil)
        }
    }
}
