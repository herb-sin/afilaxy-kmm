import SwiftUI

/// Card hero da HomeView com status semanal de saúde baseado em pedidos de emergência.
/// Lê `user_stats/{uid}.weeklyCount[weekKey]` via Firestore (HomeView) e recebe
/// o valor já resolvido via `weeklyCount`.
struct WeeklyStatusCard: View {
    let weeklyCount: Int  // -1 = carregando, 0 = nenhum, 1+

    private var config: StatusConfig { StatusConfig.from(count: weeklyCount) }

    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                colors: config.gradientColors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .clipShape(RoundedRectangle(cornerRadius: 20))

            // Decorative circles
            GeometryReader { geo in
                Circle()
                    .fill(Color.white.opacity(0.06))
                    .frame(width: 140, height: 140)
                    .offset(x: geo.size.width - 60, y: -40)
                Circle()
                    .fill(Color.white.opacity(0.04))
                    .frame(width: 80, height: 80)
                    .offset(x: geo.size.width - 30, y: 60)
            }
            .clipped()

            // Content
            VStack(alignment: .leading, spacing: 16) {
                // Top row
                HStack(alignment: .top) {
                    Image(systemName: config.icon)
                        .font(.title)
                        .foregroundColor(.white)
                        .symbolRenderingMode(.hierarchical)

                    Spacer()

                    // Semana badge
                    Text("Esta semana")
                        .font(.caption2.bold())
                        .foregroundColor(.white.opacity(0.7))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color.white.opacity(0.15))
                        .clipShape(Capsule())
                }

                // Status message
                if weeklyCount == -1 {
                    // Skeleton loader
                    VStack(alignment: .leading, spacing: 8) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.white.opacity(0.3))
                            .frame(height: 18)
                            .frame(maxWidth: .infinity)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.white.opacity(0.2))
                            .frame(height: 14)
                            .frame(maxWidth: 200)
                    }
                    .redacted(reason: .placeholder)
                } else {
                    Text(config.headline)
                        .font(.title3.bold())
                        .foregroundColor(.white)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)

                    Text(config.body)
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.88))
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }

                // Count pill (só se carregou)
                if weeklyCount >= 0 {
                    HStack(spacing: 6) {
                        Image(systemName: "waveform.path.ecg")
                            .font(.caption2)
                        Text(config.pillText)
                            .font(.caption.bold())
                    }
                    .foregroundColor(.white.opacity(0.9))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.18))
                    .clipShape(Capsule())
                }
            }
            .padding(20)
        }
        .frame(maxWidth: .infinity)
        .shadow(color: config.shadowColor, radius: 12, x: 0, y: 6)
    }
}

// MARK: - Status Config

private struct StatusConfig {
    let icon: String
    let gradientColors: [Color]
    let headline: String
    let body: String
    let pillText: String
    let shadowColor: Color

    static func from(count: Int) -> StatusConfig {
        switch count {
        case -1:
            return StatusConfig(
                icon: "lungs.fill",
                gradientColors: [Color(hex: "#1A73E8"), Color(hex: "#0D47A1")],
                headline: "Carregando...",
                body: "",
                pillText: "",
                shadowColor: Color(hex: "#1A73E8").opacity(0.35)
            )
        case 0:
            return StatusConfig(
                icon: "checkmark.seal.fill",
                gradientColors: [Color(hex: "#00897B"), Color(hex: "#004D40")],
                headline: "Essa semana você não fez nenhum pedido de socorro.",
                body: "Sua asma parece controlada! Continue assim. 💚",
                pillText: "0 pedidos esta semana",
                shadowColor: Color(hex: "#00897B").opacity(0.35)
            )
        case 1:
            return StatusConfig(
                icon: "exclamationmark.triangle.fill",
                gradientColors: [Color(hex: "#F4A825"), Color(hex: "#E65100")],
                headline: "Você já fez 1 pedido de emergência esta semana.",
                body: "Se precisar da bombinha novamente, considere marcar uma consulta com seu médico. 🩺",
                pillText: "1 pedido esta semana",
                shadowColor: Color(hex: "#F4A825").opacity(0.35)
            )
        default:
            return StatusConfig(
                icon: "cross.circle.fill",
                gradientColors: [Color(hex: "#C62828"), Color(hex: "#7B1A1A")],
                headline: "Você já fez \(count) pedidos de emergência esta semana.",
                body: "Sua asma pode estar fora de controle. Agendar uma consulta com urgência é extremamente importante. ❗",
                pillText: "\(count) pedidos esta semana",
                shadowColor: Color(hex: "#C62828").opacity(0.4)
            )
        }
    }
}

