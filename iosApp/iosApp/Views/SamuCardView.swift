import SwiftUI

/// Cartão SOS — exibido em tela cheia para ser mostrado a alguém próximo.
/// O usuário em crise de asma não precisa falar: a tela instrui um bystander
/// a ligar para o SAMU 192 em seu lugar.
struct SamuCardView: View {
    let userName: String
    var onDismiss: () -> Void = {}

    @State private var pulse = false

    var body: some View {
        ZStack {
            // Fundo vermelho de alto contraste
            Color(red: 0.72, green: 0.05, blue: 0.05)
                .ignoresSafeArea()

            // Círculo pulsante decorativo
            Circle()
                .fill(Color.white.opacity(0.05))
                .frame(width: 340, height: 340)
                .scaleEffect(pulse ? 1.08 : 1.0)
                .animation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true), value: pulse)
                .offset(y: -120)

            VStack(spacing: 0) {
                Spacer()

                // Ícone de alerta
                Text("🆘")
                    .font(.system(size: 80))
                    .scaleEffect(pulse ? 1.06 : 1.0)
                    .animation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true), value: pulse)

                Spacer().frame(height: 24)

                // Mensagem principal — máximo contraste, letra grande
                Text("PRECISO DE AJUDA\nIMEDIATA")
                    .font(.system(size: 34, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)

                Spacer().frame(height: 20)

                // Contexto
                Text("Estou com uma crise de asma.\nNão consigo respirar direito\ne preciso de socorro.")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white.opacity(0.92))
                    .multilineTextAlignment(.center)
                    .lineSpacing(5)
                    .padding(.horizontal, 24)

                Spacer().frame(height: 32)

                // Divisor
                Rectangle()
                    .fill(Color.white.opacity(0.3))
                    .frame(height: 1)
                    .padding(.horizontal, 40)

                Spacer().frame(height: 28)

                // Instrução para o bystander
                VStack(spacing: 10) {
                    Text("Por favor:")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(.white.opacity(0.88))

                    Text("192")
                        .font(.system(size: 72, weight: .black, design: .monospaced))
                        .foregroundColor(.white)
                        .shadow(color: .black.opacity(0.3), radius: 4, x: 0, y: 2)

                    Text("SAMU — ligue do SEU celular")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white.opacity(0.88))
                        .multilineTextAlignment(.center)

                    // Aviso para o bystander não usar o celular da vítima
                    HStack(spacing: 6) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.caption)
                        Text("⚠️ Deixe este celular com a vítima — ela está trocando mensagens com quem vem ajudá-la.")
                            .font(.system(size: 12))
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .foregroundColor(.white.opacity(0.88))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.white.opacity(0.15))
                    .cornerRadius(10)
                    .padding(.horizontal, 8)
                }

                Spacer().frame(height: 32)

                // O que dizer ao SAMU
                VStack(alignment: .leading, spacing: 8) {
                    Text("Quando atender, diga:")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white.opacity(0.9))

                    ForEach([
                        "\"Uma pessoa está com crise grave de asma\"",
                        "\"Ela usa o app Afilaxy e precisa de socorro\"",
                        "Informe o endereço ou local onde estão"
                    ], id: \.self) { line in
                        HStack(alignment: .top, spacing: 10) {
                            Text("•")
                                .foregroundColor(.white.opacity(0.7))
                            Text(line)
                                .font(.system(size: 14))
                                .foregroundColor(.white.opacity(0.85))
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
                .padding(.horizontal, 32)
                .padding(.vertical, 16)
                .background(Color.white.opacity(0.1))
                .cornerRadius(14)
                .padding(.horizontal, 20)

                Spacer().frame(height: 32)

                // Botão de discagem removido: o bystander deve ligar do SEU CELULAR,
                // mantendo o celular da vítima livre para o chat com o helper.

                // Fechar — discreto, para o próprio usuário dispensar o cartão
                Button {
                    onDismiss()
                } label: {
                    Text("Fechar cartão")
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.5))
                        .padding(.vertical, 12)
                }

                Spacer().frame(height: 20)
            }
        }
        .onAppear { pulse = true }
    }
}

#Preview {
    SamuCardView(userName: "João Silva")
}
