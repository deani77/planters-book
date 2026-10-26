import SwiftUI

struct HomeView: View {
    @AppStorage("estate_name") private var estateName = ""
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var estate = EstateReferenceStore.shared
    private var isPad: Bool { UIDevice.current.userInterfaceIdiom == .pad }
    @ScaledMetric(relativeTo: .largeTitle) private var iPadHeadingSize: CGFloat = 44
    @ScaledMetric(relativeTo: .body) private var minimumPhoneHomeHeight: CGFloat = 520
    private let sections = [
        "Estate Details", "Field Details", "Upkeep and Cultivation",
        "Crop Production", "Settings", "Planter Tools"
    ]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if isPad {
                    homeHeading.padding(.horizontal, 20)
                    IPadHomeDashboardCard()
                        .padding(.horizontal, 20).padding(.bottom, 16)
                    ScrollView { navigationGrid.padding(.horizontal, 20).padding(.vertical, 12) }
                } else {
                    GeometryReader { geometry in
                        ScrollView {
                            VStack(spacing: 8) {
                                homeHeading.fixedSize(horizontal: false, vertical: true).padding(.bottom, 12)
                                NavigationLink { HomeDashboardScreen() } label: {
                                    VStack(spacing: 10) {
                                        Image(systemName: "chart.bar.xaxis").font(.title2)
                                        Text("Estate Dashboard").font(.title2.bold())
                                    }
                                    .padding(20)
                                    .frame(maxWidth: .infinity, minHeight: 88, maxHeight: .infinity)
                                    .background(EstateTheme.hero)
                                    .foregroundStyle(.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 24))
                                    .overlay(RoundedRectangle(cornerRadius: 24).stroke(.white.opacity(0.12), lineWidth: 1))

                                }
                                .frame(maxHeight: .infinity).padding(.bottom, 12)
                                ForEach(0..<3, id: \.self) { row in
                                    HStack(spacing: 8) {
                                        navigationButton(sections[row * 2], stretches: true)
                                        navigationButton(sections[row * 2 + 1], stretches: true)
                                    }.frame(maxHeight: .infinity)
                                }
                            }
                            .padding(20)
                            .frame(height: max(geometry.size.height, minimumPhoneHomeHeight))
                        }
                    }
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) { homeFooter }
            .background { EstateBackground(prominent: true) }
            .toolbar(.hidden, for: .navigationBar)
        }
        .tint(EstateTheme.accent)
        .groupBoxStyle(EstateGroupBoxStyle())
    }

    private var homeHeading: some View {
        VStack(spacing: 6) {
            Text(estateDisplayName(estateName, estate.reference?.estateName)).font(.caption.weight(.semibold)).tracking(2)
                .foregroundStyle(.secondary)
            Text("Planters Book")
                .font(isPad ? .system(size: iPadHeadingSize, weight: .bold, design: .rounded) : .largeTitle.weight(.bold))
                .foregroundStyle(EstateTheme.accent)
        }
        .multilineTextAlignment(.center)
        .padding(.vertical, isPad ? 16 : 20)
    }

    private var navigationGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 14) {
            ForEach(sections, id: \.self) { title in navigationButton(title) }
        }
    }

    private func navigationButton(_ title: String, stretches: Bool = false) -> some View {
        NavigationLink {
            if title == "Settings" {
                UserSettingsView()
            } else if title == "Estate Details" {
                EstateInformationView()
            } else if title == "Field Details" {
                FieldDetailsView()
            } else if title == "Upkeep and Cultivation" {
                UpkeepCultivationView()
            } else if title == "Planter Tools" {
                PlanterToolsView()
            } else if title == "Crop Production" {
                ProductionView()
            } else {
                ModuleView(title: title)
            }
        } label: {
            VStack(spacing: isPad ? 10 : 7) {
                Image(systemName: sectionSymbol(title))
                    .font(isPad ? .title2 : .title3)
                    .foregroundStyle(EstateTheme.accent)
                Text(title)
                    .font(isPad ? .title3.weight(.semibold) : .subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity, minHeight: isPad ? 64 : 48,
                maxHeight: stretches ? .infinity : nil)
            .padding(isPad ? 12 : 8)
            .background(EstateTheme.surface, in: RoundedRectangle(cornerRadius: 20))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(EstateTheme.border, lineWidth: 1))

        }
        .frame(maxWidth: .infinity, maxHeight: stretches ? .infinity : nil)
    }

    private func sectionSymbol(_ title: String) -> String {
        switch title {
        case "Estate Details": return "leaf.fill"
        case "Field Details": return "square.grid.2x2.fill"
        case "Upkeep and Cultivation": return "sprinkler.and.droplets.fill"
        case "Crop Production": return "chart.bar.fill"
        case "Settings": return "gearshape.fill"
        default: return "function"
        }
    }

    private var homeFooter: some View {
        VStack(spacing: 4) {
            Text("Planters Book").foregroundStyle(.secondary)
            if let data = estate.reference { Text("Estate revision \(data.revision) saved") }
            else { Text("No estate data saved") }
            Text("v0.4 Alpha · iPhone and iPad\nEstate data and production drafts are saved offline")
                .foregroundStyle(EstateTheme.accent)
        }
        .font(isPad ? .body : .footnote)
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
        .background(EstateTheme.background)
        .overlay(alignment: .top) { EstateTheme.border.frame(height: 1) }
    }


}

struct ModuleView: View {
    let title: String
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(title).font(.title.bold())
                Text(title == "About"
                     ? "Planters Book\niPhone and iPad · v0.4 Alpha"
                     : "This section is awaiting implementation and verified estate records.")
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
        }
        .toolbar(.visible, for: .navigationBar)
        .estateScreen()
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}


