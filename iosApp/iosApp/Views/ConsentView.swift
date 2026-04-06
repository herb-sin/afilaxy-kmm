import SwiftUI

struct ConsentView: View {
    var onConsentGiven: () -> Void
    @AppStorage("analytics_consent") private var analyticsConsent = true

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {

                // ── Cabeçalho ─────────────────────────────────────────────────
                VStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 28)
                            .fill(Color.accentColor.opacity(0.12))
                            .frame(width: 104, height: 104)
                        Image(systemName: "heart.fill")
                            .font(.system(size: 52))
                            .foregroundColor(.accentColor)
                    }

                    Text("Antes de começar")
                        .font(.title2).bold()

                    Text("Sua privacidade é importante para nós.\nVeja o que coletamos e por quê.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 40)

                // ── Dados Essenciais ──────────────────────────────────────────
                ConsentBlock(
                    icon: "lock.fill",
                    iconColor: .green,
                    title: "Dados Essenciais",
                    description: "Localização durante emergências, matching com helpers e mensagens de chat. Necessários para o funcionamento do app.",
                    isEnabled: .constant(true),
                    isToggleable: false,
                    badge: "Sempre ativo"
                )

                // ── Analytics & Melhoria ──────────────────────────────────────
                ConsentBlock(
                    icon: "chart.bar.fill",
                    iconColor: Color(.systemBlue),
                    title: "Analytics & Melhoria",
                    description: "Gravidade de crises, avaliações pós-atendimento e NPS. Nos ajuda a melhorar o serviço e realizar pesquisas de saúde pública.",
                    isEnabled: $analyticsConsent,
                    isToggleable: true,
                    badge: nil
                )

                // ── Ação ──────────────────────────────────────────────────────
                VStack(spacing: 12) {
                    Button {
                        UserDefaults.standard.set(true, forKey: "consent_shown")
                        onConsentGiven()
                    } label: {
                        Text("Continuar")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }

                    Text("Você pode alterar suas preferências a qualquer momento em Configurações > Privacidade.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.bottom, 40)
            }
            .padding(.horizontal, 24)
        }
    }
}

// MARK: - ConsentBlock

private struct ConsentBlock: View {
    let icon: String
    let iconColor: Color
    let title: String
    let description: String
    @Binding var isEnabled: Bool
    let isToggleable: Bool
    let badge: String?

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: icon)
                .foregroundColor(iconColor)
                .font(.title3)
                .frame(width: 30)
                .padding(.top, 2)

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(title)
                        .font(.headline)
                    Spacer()
                    if isToggleable {
                        Toggle("", isOn: $isEnabled).labelsHidden()
                    } else if let badge = badge {
                        Text(badge)
                            .font(.caption).bold()
                            .foregroundColor(.green)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color.green.opacity(0.12))
                            .clipShape(Capsule())
                    }
                }
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(isEnabled ? Color.accentColor.opacity(0.08) : Color(.systemGray6))
        )
    }
}
