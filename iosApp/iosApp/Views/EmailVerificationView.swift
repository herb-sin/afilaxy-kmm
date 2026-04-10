import SwiftUI
import FirebaseAuth

/// Tela de verificação de email — paridade com Android EmailVerificationScreen.
/// Polling automático 3s × 20, reenvio com cooldown 60s.
struct EmailVerificationView: View {
    let userEmail: String
    let onVerified: () -> Void
    let onLogout: () -> Void

    @State private var cooldown = 0
    @State private var showResendSuccess = false
    @State private var isChecking = false
    @State private var isResending = false
    @State private var pollingTask: Task<Void, Never>? = nil

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "envelope.badge.fill")
                .font(.system(size: 72))
                .foregroundColor(.accentColor)

            Text("Verifique seu email")
                .font(.title.bold())

            VStack(spacing: 6) {
                Text("Enviamos um link de verificação para:")
                    .foregroundColor(.secondary)
                Text(userEmail)
                    .font(.headline)
                    .foregroundColor(.accentColor)
            }
            .multilineTextAlignment(.center)

            Text("Clique no link do email para verificar sua conta.")
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Text("💡 Não encontrou? Verifique sua caixa de spam.")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Spacer()

            // Verificar agora
            Button(action: checkVerification) {
                HStack {
                    if isChecking {
                        ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Image(systemName: "arrow.clockwise")
                    }
                    Text("Verificar Agora")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.accentColor)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .disabled(isChecking)

            // Reenviar com cooldown
            Button(action: resendEmail) {
                if isResending {
                    ProgressView()
                } else if cooldown > 0 {
                    Text("Aguarde \(cooldown)s")
                } else {
                    Text("Reenviar Email")
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.accentColor, lineWidth: 1))
            .foregroundColor(.accentColor)
            .disabled(isResending || cooldown > 0)

            if showResendSuccess {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Email reenviado!")
                        .font(.headline)
                    Text("Use apenas o link do email mais recente — links anteriores foram invalidados.")
                        .font(.caption)
                }
                .padding()
                .background(Color.accentColor.opacity(0.1))
                .cornerRadius(10)
            }

            Button("Sair", action: onLogout)
                .foregroundColor(.red)
                .padding(.bottom)
        }
        .padding(.horizontal, 28)
        .navigationBarHidden(true)
        .onAppear(perform: startPolling)
        .onDisappear { pollingTask?.cancel() }
    }

    // MARK: - Polling automático
    private func startPolling() {
        pollingTask = Task {
            var attempts = 0
            while !Task.isCancelled && attempts < 20 {
                try? await Task.sleep(nanoseconds: 3_000_000_000)
                await reload()
                if isVerified() {
                    await MainActor.run { onVerified() }
                    return
                }
                attempts += 1
            }
        }
    }

    // MARK: - Verificação manual
    private func checkVerification() {
        isChecking = true
        Task {
            await reload()
            await MainActor.run {
                isChecking = false
                if isVerified() { onVerified() }
            }
        }
    }

    // MARK: - Reenvio com cooldown 60s
    private func resendEmail() {
        isResending = true
        Task {
            try? await Auth.auth().currentUser?.sendEmailVerification()
            await MainActor.run {
                isResending = false
                showResendSuccess = true
                cooldown = 60
            }
            // Countdown
            for remaining in stride(from: 59, through: 0, by: -1) {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                await MainActor.run { cooldown = remaining }
            }
            await MainActor.run { showResendSuccess = false }
        }
    }

    private func reload() async {
        try? await Auth.auth().currentUser?.reload()
    }

    private func isVerified() -> Bool {
        Auth.auth().currentUser?.isEmailVerified == true
    }
}
