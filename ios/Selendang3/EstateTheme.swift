import SwiftUI

// Semantic colors keep content legible in both themes without changing saved data.
enum EstateTheme {
    private static func adaptive(_ light: UInt32, _ dark: UInt32) -> Color {
        func ui(_ hex: UInt32) -> UIColor {
            UIColor(red: CGFloat((hex >> 16) & 255) / 255,
                    green: CGFloat((hex >> 8) & 255) / 255,
                    blue: CGFloat(hex & 255) / 255, alpha: 1)
        }
        return Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? ui(dark) : ui(light) })
    }
    static let accent = adaptive(0x20543E, 0x9EDDB9)
    static let background = adaptive(0xF3F6F1, 0x101C17)
    static let surface = adaptive(0xFFFFFF, 0x1A2B23)
    static let softAccent = adaptive(0xE5EFE7, 0x243F31)
    static let border = adaptive(0xD5E2D8, 0x365243)
    static let hero = LinearGradient(colors: [Color(red: 0.13, green: 0.32, blue: 0.24),
        Color(red: 0.06, green: 0.21, blue: 0.16)], startPoint: .topLeading, endPoint: .bottomTrailing)
}

struct EstateBackground: View {
    var prominent = false
    @Environment(\.colorScheme) private var scheme
    var body: some View {
        ZStack {
            EstateTheme.background
            LinearGradient(colors: [EstateTheme.softAccent.opacity(prominent ? 0.9 : 0.3), .clear],
                startPoint: .topLeading, endPoint: .bottomTrailing)
            Canvas { context, size in
                // Original vector palm fronds: decorative, local and independent of network access.
                for (x, y, rotation) in [(0.95, 0.10, 2.3), (0.03, 0.65, -0.8), (0.9, 0.94, 3.5)] {
                    let origin = CGPoint(x: size.width * x, y: size.height * y)
                    let length = min(size.width * 0.7, 310)
                    func point(_ along: Double, _ across: Double) -> CGPoint {
                        CGPoint(x: origin.x + along * cos(rotation) - across * sin(rotation),
                                y: origin.y + along * sin(rotation) + across * cos(rotation))
                    }
                    var stem = Path(); stem.move(to: origin); stem.addLine(to: point(length, 0))
                    context.stroke(stem, with: .color(EstateTheme.accent), lineWidth: 1.5)
                    for leaf in 1...11 {
                        let along = length * Double(leaf) / 12
                        let reach = length * 0.24 * sin(Double(leaf) / 12 * .pi)
                        for side in [-1.0, 1.0] {
                            var blade = Path(); blade.move(to: point(along - 15, 0))
                            blade.addQuadCurve(to: point(along + 22, reach * side), control: point(along - 8, reach * side))
                            blade.addQuadCurve(to: point(along + 5, 0), control: point(along + 17, reach * side * 0.3))
                            context.fill(blade, with: .color(EstateTheme.accent))
                        }
                    }
                }
            }
            .opacity(prominent ? (scheme == .dark ? 0.12 : 0.08) : 0.025)
            .accessibilityHidden(true)
            .allowsHitTesting(false)
        }.ignoresSafeArea()
    }
}

private struct EstateScreenStyle: ViewModifier {
    func body(content: Content) -> some View {
        content.scrollContentBackground(.hidden)
            .background { EstateBackground() }
            .tint(EstateTheme.accent)
            .toolbarBackground(EstateTheme.surface, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
    }
}

struct EstateGroupBoxStyle: GroupBoxStyle {
    func makeBody(configuration: Configuration) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            configuration.label.font(.headline).foregroundStyle(EstateTheme.accent)
            configuration.content
        }
        .frame(maxWidth: .infinity, alignment: .leading).padding(16)
        .background(EstateTheme.surface, in: RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(EstateTheme.border, lineWidth: 1))
    }
}

extension View {
    func estateScreen() -> some View { modifier(EstateScreenStyle()) }
}
