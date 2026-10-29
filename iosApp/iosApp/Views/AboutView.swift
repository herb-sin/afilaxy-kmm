import SwiftUI

struct AboutView: View {
    @Environment(\.dismiss) var dismiss

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
        return "\(version) (\(build))"
    }

    private var currentYear: String {
        String(Calendar.current.component(.year, from: Date()))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Image(systemName: "heart.circle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.red)

                Text("Afilaxy")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Versão \(appVersion)")
                    .font(.subheadline)
                    .foregroundColor(.gray)

                Divider()
                    .padding(.vertical)

                Text("A rede de quem tem asma")
                    .font(.title3)
                    .fontWeight(.semibold)

                Text("Conectamos quem está em crise de asma, sem a bombinha por perto, a pessoas próximas "
                     + "que podem ajudar. No dia a dia, ajudamos você a acompanhar sua asma e a manter o "
                     + "tratamento com um profissional de saúde.")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.gray)
                    .padding(.horizontal)

                VStack(alignment: .leading, spacing: 12) {
                    Text("Recursos")
                        .font(.headline)

                    FeatureRow(icon: "person.2.fill", text: "Pedido de ajuda a pessoas próximas (Modo Ajudante)")
                    FeatureRow(icon: "message.fill", text: "Chat com quem vai te ajudar")
                    FeatureRow(icon: "sun.max.fill", text: "Check-ins diários de bem-estar")
                    FeatureRow(icon: "checklist", text: "Teste de Controle da Asma (ACT)")
                    FeatureRow(icon: "cloud.sun.fill", text: "Índice de risco com clima e qualidade do ar")
                    FeatureRow(icon: "doc.text.fill", text: "Relatório de saúde para compartilhar com seu médico")
                    FeatureRow(icon: "cross.case.fill", text: "Mapa de farmácias 24h")
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding(.horizontal)

                Text("O Afilaxy não substitui atendimento médico. Em uma crise grave, ligue 192 (SAMU).")
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(10)
                    .padding(.horizontal)

                Text("Feito com ❤️ por quem também tem asma")
                    .font(.caption)
                    .foregroundColor(.gray)

                Text("© 2024–\(currentYear) Afilaxy")
                    .font(.caption2)
                    .foregroundColor(.gray)
            }
            .padding(.vertical, 32)
        }
        .navigationTitle("Sobre")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct FeatureRow: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.red)
            Text(text)
        }
    }
}
