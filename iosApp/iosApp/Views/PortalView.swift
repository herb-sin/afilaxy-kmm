import SwiftUI

struct PortalView: View {
    @Environment(\.openURL) private var openURL
    @State private var showCrmLookup = false

    var body: some View {
        // ContentView already wraps this in a NavigationStack — no NavigationView needed here.
        ScrollView {
            VStack(spacing: 16) {
                // Logo da Cellula Mater oculto a pedido — código mantido para reativação
                // rápida quando fizer sentido voltar a exibir (contrato/logo ainda não
                // finalizados). Ver também PortalScreen.kt (Android).
                // Image("logo-cellula-mater")
                //     .resizable()
                //     .scaledToFit()
                //     .frame(width: 96, height: 96)
                //     .onTapGesture {
                //         AnalyticsManager.logEvent("partner_link_clicked", params: ["partner": "cellula_mater"])
                //         if let url = URL(string: "https://cellulamater.com.br/") {
                //             openURL(url)
                //         }
                //     }

                PortalActionCard(
                    icon: "person.badge.plus",
                    title: "Quero me tornar Parceiro",
                    description: "Parcerias CRM, CREFITO, CRP e Clínicas",
                    buttonLabel: "Conhecer o Portal"
                ) {
                    if let url = URL(string: "https://afilaxy.com/profissionais") {
                        openURL(url)
                    }
                }

                PortalActionCard(
                    icon: "person.text.rectangle.fill",
                    title: "Consultar CRM",
                    description: "Confirme o registro de um médico no Conselho Federal de Medicina.",
                    buttonLabel: "Consultar"
                ) {
                    showCrmLookup = true
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 100)
        }
        .navigationTitle("Apoiadores")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            // Cláusula 2.3 do contrato de parceria — mesma métrica registrada no Android
            // (ver PortalScreen.kt), sujeita ao mesmo opt-in de consentimento.
            AnalyticsManager.logScreenView("Apoio")
        }
        .sheet(isPresented: $showCrmLookup) {
            CrmLookupView()
        }
    }
}

private struct PortalActionCard: View {
    let icon: String
    let title: String
    let description: String
    let buttonLabel: String
    let action: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundColor(AfilaxyColors.primary)

                Text(title)
                    .font(.headline)
                    .fontWeight(.bold)

                Spacer()
            }

            Text(description)
                .font(.subheadline)
                .foregroundColor(.secondary)

            Button(action: action) {
                HStack {
                    Spacer()
                    Text(buttonLabel)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Spacer()
                }
                .padding(.vertical, 8)
                .background(AfilaxyColors.primary)
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .strokeBorder(AfilaxyColors.primary.opacity(0.25), lineWidth: 1)
        )
    }
}

#if DEBUG
struct PortalView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            PortalView()
        }
    }
}
#endif
