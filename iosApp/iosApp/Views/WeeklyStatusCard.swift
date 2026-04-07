import SwiftUI

/// Card hero da HomeView com status semanal de saúde baseado em pedidos de emergência.
/// Lê `user_stats/{uid}.weeklyCount[weekKey]` via Firestore (HomeView) e recebe
/// o valor já resolvido via `weeklyCount`.
struct WeeklyStatusCard: View {
    let weeklyCount: Int  // -1 = carregando, 0 = nenhum, 1+
    // Total acumulado de todas as semanas — nunca zera na virada ISO.
    // -1 enquanto carrega (exibe skeleton junto com weeklyCount=-1).
    var totalEmergencies: Int = -1

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
                    let total = totalEmergencies >= 0 ? totalEmergencies : weeklyCount
                    let pillText: String = {
                        if total == 0 { return "nenhum pedido ainda" }
                        if total == 1 && weeklyCount == 1 { return "1 pedido \u00b7 1 esta semana" }
                        if total == 1 { return "1 pedido no total" }
                        if weeklyCount > 0 { return "\(total) no total \u00b7 \(weeklyCount) esta semana" }
                        return "\(total) no total"
                    }()
                    HStack(spacing: 6) {
                        Image(systemName: "waveform.path.ecg")
                            .font(.caption2)
                        Text(pillText)
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
                gradientColors: [Color(hex: "#1976D2"), Color(hex: "#1565C0")],
                headline: "Essa semana você não fez nenhum pedido de socorro.",
                body: "Lembre-se: Duas crises ou mais por semana indicam Asma não controlada!",
                pillText: "0 pedidos esta semana",
                shadowColor: Color(hex: "#1976D2").opacity(0.35)
            )
        case 1:
            return StatusConfig(
                icon: "exclamationmark.triangle.fill",
                gradientColors: [Color(hex: "#F4A825"), Color(hex: "#E65100")],
                headline: "Você fez 1 pedido de socorro esta semana.",
                body: "Alguém parou o que estava fazendo para te ajudar, de graça. Agendar uma consulta é o passo mais responsável, com a sua Saúde e com a pessoa que te ajudou. Ela pode precisar de você, assim como você precisou dela!",
                pillText: "1 pedido esta semana",
                shadowColor: Color(hex: "#F4A825").opacity(0.35)
            )
        case 2:
            return StatusConfig(
                icon: "cross.circle.fill",
                gradientColors: [Color(hex: "#C62828"), Color(hex: "#7B1A1A")],
                headline: "2º pedido de socorro esta semana.",
                body: "ALERTA CLÍNICO: Duas crises ou mais por semana indicam Asma não controlada! É URGENTE agendar uma consulta!",
                pillText: "2 pedidos esta semana",
                shadowColor: Color(hex: "#C62828").opacity(0.4)
            )
        case 3:
            return StatusConfig(
                icon: "cross.circle.fill",
                gradientColors: [Color(hex: "#4A0000"), Color(hex: "#1A0000")],
                headline: "Você já pediu socorro \(count) vezes esta semana.",
                body: "Você acumula \(count) pedidos de ajuda. Esse quadro vai além da urgência. Por favor, busque assistência médica.",
                pillText: "\(count) pedidos esta semana",
                shadowColor: Color(hex: "#4A0000").opacity(0.5)
            )
        default:
            return StatusConfig(
                icon: "cross.circle.fill",
                gradientColors: [Color(hex: "#6A0DAD"), Color(hex: "#3D0066")],
                headline: "Você já pediu socorro \(count) vezes esta semana.",
                body: "Você acumula \(count) pedidos de ajuda. Esse quadro vai além da urgência. Por favor, busque assistência médica.",
                pillText: "\(count) pedidos esta semana",
                shadowColor: Color(hex: "#6A0DAD").opacity(0.5)
            )
        }
    }
}

