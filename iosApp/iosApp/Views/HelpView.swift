import SwiftUI

struct HelpView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {

                // ── Protocolo de Crise de Asma ────────────────────────────────
                Text("Em caso de crise de asma")
                    .font(.title)
                    .fontWeight(.bold)

                CrisisStepCard(
                    step: 1,
                    title: "Sente-se em posição vertical",
                    description: "Mantenha a calma. Sente-se ereto ou incline-se levemente para frente, apoiando as mãos nos joelhos. Evite deitar.",
                    color: .blue
                )

                CrisisStepCard(
                    step: 2,
                    title: "Use o inalador de resgate",
                    description: "Salbutamol (Aerolin): 2 a 4 jatos. Aguarde 30 segundos entre cada jato. Repita a cada 20 min se necessário, até 3 vezes.",
                    color: .green
                )

                CrisisStepCard(
                    step: 3,
                    title: "Peça ajuda se não melhorar",
                    description: "Se não houver melhora após 10 minutos: acione o SAMU pelo Afilaxy ou ligue diretamente.",
                    color: .red
                )

                // Botão SAMU
                Button {
                    if let url = URL(string: "tel://192") {
                        UIApplication.shared.open(url)
                    }
                } label: {
                    HStack {
                        Image(systemName: "phone.fill")
                        Text("Ligar para o SAMU — 192")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.red)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }

                Divider().padding(.vertical)

                // ── Como usar o Afilaxy ───────────────────────────────────────
                Text("Como usar o Afilaxy")
                    .font(.title2)
                    .fontWeight(.bold)

                HelpCard(
                    icon: "exclamationmark.triangle.fill",
                    title: "Criar Emergência",
                    description: "Toque no botão vermelho de emergência na tela inicial. O app notificará helpers próximos com inaladores disponíveis."
                )

                HelpCard(
                    icon: "heart.fill",
                    title: "Ser Helper",
                    description: "Ative o Modo Ajudante no card da tela inicial. Quando alguém precisar de ajuda próximo a você, você receberá uma notificação."
                )

                HelpCard(
                    icon: "location.fill",
                    title: "Permissões",
                    description: "Para receber alertas de emergências próximas, permita acesso à localização 'sempre' nas configurações do app."
                )

                HelpCard(
                    icon: "bell.fill",
                    title: "Notificações",
                    description: "Mantenha notificações ativadas para ser alertado imediatamente sobre emergências."
                )

                Divider().padding(.vertical)

                // ── FAQ ───────────────────────────────────────────────────────
                Text("Perguntas Frequentes")
                    .font(.title2)
                    .fontWeight(.bold)

                FAQItem(
                    question: "O que fazer em uma emergência?",
                    answer: "Toque no botão de emergência, aguarde um helper aceitar, e siga o protocolo de crise enquanto aguarda."
                )

                FAQItem(
                    question: "Como cancelar uma emergência?",
                    answer: "Na tela de aguardo, toque em 'Cancelar Emergência'."
                )

                FAQItem(
                    question: "Posso ser helper e solicitar emergência?",
                    answer: "Sim! Ao tocar em 'Emergência', o Modo Ajudante é desativado automaticamente."
                )

                FAQItem(
                    question: "O app substitui o SAMU?",
                    answer: "Não. O Afilaxy conecta pessoas para compartilhar medicação de resgate enquanto o socorro profissional não chega. Em crises graves, ligue 192."
                )
            }
            .padding()
        }
        .navigationTitle("Protocolo & Ajuda")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - CrisisStepCard

private struct CrisisStepCard: View {
    let step: Int
    let title: String
    let description: String
    let color: Color

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            ZStack {
                Circle()
                    .fill(color)
                    .frame(width: 36, height: 36)
                Text("\(step)")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                    .fontWeight(.semibold)
                Text(description)
                    .font(.body)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.07), radius: 4, x: 0, y: 2)
    }
}

// MARK: - HelpCard

struct HelpCard: View {
    let icon: String
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            Image(systemName: icon)
                .font(.title)
                .foregroundColor(.red)
                .frame(width: 40)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                Text(description)
                    .font(.body)
                    .foregroundColor(.gray)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}

// MARK: - FAQItem

struct FAQItem: View {
    let question: String
    let answer: String
    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button(action: {
                withAnimation { isExpanded.toggle() }
            }) {
                HStack {
                    Text(question)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                    Spacer()
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.gray)
                }
            }

            if isExpanded {
                Text(answer)
                    .font(.body)
                    .foregroundColor(.gray)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}
