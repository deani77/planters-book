import SwiftUI

struct HomeDashboardScreen: View {
    private var isPad: Bool { UIDevice.current.userInterfaceIdiom == .pad }
    var body: some View {
        Group {
            if isPad {
                ScrollView { IPadHomeDashboardCard().padding(20) }
            } else {
                OperationalDashboardGrid()
            }
        }
        .estateScreen()
        .navigationTitle("Estate Dashboard")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
    }
}

// The phone hierarchy groups crop operations, cultivation and estate costs.
// Larger accessibility text retains scroll access.
private struct OperationalDashboardGrid: View {
    @ObservedObject private var estate = EstateReferenceStore.shared
    private let green = EstateTheme.accent

    var body: some View {
        ScrollView {
            TimelineView(.periodic(from: Date(), by: 60)) { context in
                let cards = EstateDashboardData(reference: estate.reference, today: context.date).operationalCards
                VStack(spacing: 10) {
                    dashboardCard(cards[0], symbol: "leaf.fill", wide: true)
                    HStack(alignment: .top, spacing: 10) {
                        dashboardCard(cards[1], symbol: "square.grid.3x3.fill", height: 144)
                        dashboardCard(cards[2], symbol: "person.fill", height: 144)
                    }
                    HStack(alignment: .top, spacing: 10) {
                        dashboardCard(cards[3], symbol: "leaf.circle.fill", height: 126)
                        dashboardCard(cards[4], symbol: "sprinkler.and.droplets.fill", height: 126)
                    }
                    dashboardCard(cards[5], symbol: "ant.fill", wide: true)
                    dashboardCard(cards[6], symbol: "banknote.fill", wide: true)
                }
                .padding(12)
            }
        }
        .background { EstateBackground() }
    }

    private func dashboardCard(_ card: EstateDashboardPanel, symbol: String, height: CGFloat = 0, wide: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Image(systemName: symbol).font(.caption).foregroundStyle(green)
                Text(card.title).font(.subheadline.weight(.semibold))
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, minHeight: wide ? 18 : 34, alignment: wide ? .center : .topLeading)
            Rectangle().fill(green.opacity(0.15)).frame(height: 1)
            if card.title == "Crop Production" {
                HStack(alignment: .top, spacing: 8) {
                    ForEach([0, 1, 2], id: \.self) { row in
                        metric(card.rows[row], prominent: true, centered: true)
                            .frame(maxWidth: .infinity, alignment: .top)
                    }
                }
                HStack {
                    Text(card.rows[3][0]).foregroundStyle(.secondary)
                    Spacer(minLength: 4)
                    Text(card.rows[3][1] + " " + card.rows[3][2]).fontWeight(.semibold).monospacedDigit()
                }.font(.caption)
            } else if wide {
                HStack(alignment: .top, spacing: 12) {
                    ForEach(card.rows.indices, id: \.self) { row in
                        metric(card.rows[row], prominent: card.title == "Estate Cost", centered: true)
                            .frame(maxWidth: .infinity, alignment: .top)
                    }
                }
            } else {
                ForEach(card.rows.indices, id: \.self) { row in
                    metric(card.rows[row])
                }
                Spacer(minLength: 0)
            }
            Text(card.note).font(.caption2).foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: wide ? .center : .leading)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(10)
        .frame(maxWidth: .infinity, minHeight: height, alignment: .topLeading)
        .background(EstateTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(EstateTheme.border, lineWidth: 1))
    }

    private func metric(_ row: [String], prominent: Bool = false, centered: Bool = false) -> some View {
        VStack(alignment: centered ? .center : .leading, spacing: 2) {
            Text(row[0]).font(.caption2).foregroundStyle(.secondary)
            (Text(row[1]).font(prominent ? .headline : .caption.weight(.semibold)) + Text(" " + row[2]).font(.caption2))
                .foregroundStyle(prominent ? green : Color.primary)
                .monospacedDigit().fixedSize(horizontal: false, vertical: true)
        }
        .multilineTextAlignment(centered ? .center : .leading)
        .accessibilityElement(children: .combine)
    }
}

// All seven measures stay visible inside one home-page dashboard on iPad.
struct IPadHomeDashboardCard: View {
    @ObservedObject private var estate = EstateReferenceStore.shared

    var body: some View {
        TimelineView(.periodic(from: Date(), by: 60)) { context in
            let cards = EstateDashboardData(reference: estate.reference, today: context.date).operationalCards
            VStack(spacing: 8) {
                VStack(spacing: 4) {
                    Image(systemName: "chart.bar.xaxis").font(.title3)
                    Text("Estate Dashboard").font(.title3.weight(.semibold))
                }
                .foregroundStyle(EstateTheme.accent)
                summaryCard(cards[0], indices: [0, 1, 2, 3], emphasized: true)
                HStack(alignment: .top, spacing: 10) {
                    summaryCard(cards[1], indices: [0, 1, 2])
                    summaryCard(cards[2], indices: [0, 1])
                }.fixedSize(horizontal: false, vertical: true)
                HStack(alignment: .top, spacing: 10) {
                    summaryCard(cards[3], indices: [0])
                    summaryCard(cards[4], indices: [0, 1])
                    summaryCard(cards[5], indices: [0, 1])
                }.fixedSize(horizontal: false, vertical: true)
                summaryCard(cards[6], indices: [0, 1], emphasized: true)
                Text("—  Awaiting data").font(.caption2).foregroundStyle(Color.primary.opacity(0.65))
            }
            .padding(12)
            .fixedSize(horizontal: false, vertical: true)
        }
        .background(EstateTheme.surface, in: RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(EstateTheme.border, lineWidth: 1))
    }

    private func summaryCard(_ card: EstateDashboardPanel, indices: [Int], emphasized: Bool = false) -> some View {
        VStack(spacing: 6) {
            Text(card.title)
                .font(.subheadline.weight(.semibold)).foregroundStyle(.primary)
                .fixedSize(horizontal: false, vertical: true)
            HStack(alignment: .top, spacing: 8) {
                ForEach(indices, id: \.self) { index in
                    let metric = card.rows[index]
                    VStack(spacing: 2) {
                        Text(metric[0]).font(.caption).foregroundStyle(Color.primary.opacity(0.65))
                            .frame(minHeight: ["Harvesting Coverage", "Harvester Productivity"].contains(card.title) ? 28 : 14, alignment: .bottom)
                        ViewThatFits(in: .horizontal) {
                            HStack(alignment: .firstTextBaseline, spacing: 4) {
                                metricValue(metric[1])
                                Text(metric[2]).font(.caption2).foregroundStyle(Color.primary.opacity(0.65))
                            }.fixedSize()
                            VStack(spacing: 2) {
                                metricValue(metric[1])
                                Text(metric[2]).font(.caption2).foregroundStyle(Color.primary.opacity(0.65))
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .top)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(metric[0] + ": " + metric[1] + " " + metric[2])
                }
            }
            Spacer(minLength: 0)
            if card.note != "Reporting period pending" {
                Text(card.note).font(.caption2).foregroundStyle(Color.primary.opacity(0.65))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .multilineTextAlignment(.center).padding(6)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(emphasized ? EstateTheme.softAccent.opacity(0.55) : EstateTheme.background,
            in: RoundedRectangle(cornerRadius: 13))
    }

    private func metricValue(_ value: String) -> some View {
        Text(value == "Pending" ? "—" : value)
            .font(.title3.weight(.semibold)).monospacedDigit()
            .foregroundStyle(value == "Pending" ? Color.secondary : Color.primary)
    }

}
