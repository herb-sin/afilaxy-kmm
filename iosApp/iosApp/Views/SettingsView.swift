import SwiftUI
import FirebaseFirestore
import FirebaseAuth
import shared

struct SettingsView: View {
    @EnvironmentObject var container: AppContainer
    @State private var showLogoutAlert = false
    @State private var showShareSheet = false
    @State private var logFileURLs: [URL] = []
    @State private var showClearLogsAlert = false
    @State private var showNoLogsAlert = false

    var body: some View {
        List {
            Section("Desenvolvedor") {
                Button {
                    exportLogs()
                } label: {
                    Label("Exportar Logs", systemImage: "square.and.arrow.up")
                }
                
                Button(role: .destructive) {
                    showClearLogsAlert = true
                } label: {
                    Label("Limpar Logs", systemImage: "trash")
                }
                
                HStack {
                    Text("Tamanho dos Logs")
                    Spacer()
                    Text(formatBytes(FileLogger.shared.getTotalLogSize()))
                        .foregroundColor(.secondary)
                        .font(.caption)
                }
            }
            
            Section("Informações") {
                NavigationLink(value: AppRoute.help) {
                    Label("Ajuda", systemImage: "questionmark.circle")
                }
                NavigationLink(value: AppRoute.about) {
                    Label("Sobre", systemImage: "info.circle")
                }
                NavigationLink(value: AppRoute.terms) {
                    Label("Termos de Uso", systemImage: "doc.text")
                }
                NavigationLink(value: AppRoute.privacy) {
                    Label("Política de Privacidade", systemImage: "lock.shield")
                }
            }

            Section {
                Button(role: .destructive) {
                    showLogoutAlert = true
                } label: {
                    Label("Sair", systemImage: "rectangle.portrait.and.arrow.right")
                }
            }
        }
        .navigationTitle("Configurações")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Sair", isPresented: $showLogoutAlert) {
            Button("Cancelar", role: .cancel) {}
            Button("Sair", role: .destructive) {
                let state = container.emergency.state
                if state?.hasActiveEmergency == true, let eid = state?.emergencyId as? String {
                    Firestore.firestore().collection("emergency_requests").document(eid)
                        .updateData(["active": false, "status": "cancelled"])
                }
                if state?.isHelperMode == true, let uid = Auth.auth().currentUser?.uid {
                    Firestore.firestore().collection("helpers").document(uid).delete()
                    LocationManagerBridge.shared.disableHelperMode()
                }
                container.emergency.clearEmergencyStateSwift()
                container.freezeAll()
                try? Auth.auth().signOut()
            }
        } message: {
            Text("Deseja realmente sair?")
        }
        .alert("Limpar Logs", isPresented: $showClearLogsAlert) {
            Button("Cancelar", role: .cancel) {}
            Button("Limpar", role: .destructive) {
                FileLogger.shared.clearAllLogs()
            }
        } message: {
            Text("Todos os logs serão removidos permanentemente.")
        }
        .alert("Sem Logs", isPresented: $showNoLogsAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Nenhum arquivo de log encontrado. Use o app para gerar logs.")
        }
        .sheet(isPresented: $showShareSheet) {
            if !logFileURLs.isEmpty {
                ShareSheet(items: logFileURLs)
            }
        }
    }
    
    private func exportLogs() {
        let urls = FileLogger.shared.getAllLogFileURLs()
        FileLogger.shared.write(level: "DEBUG", tag: "SettingsView", message: "Found \(urls.count) log files")

        // Lê conteúdo de todos os arquivos e compartilha como texto
        let content = urls.compactMap { url in
            try? String(contentsOf: url, encoding: .utf8)
        }.joined(separator: "\n\n--- next file ---\n\n")

        if content.isEmpty {
            showNoLogsAlert = true
        } else {
            logFileURLs = urls
            // Compartilha o texto diretamente em vez das URLs
            let av = UIActivityViewController(activityItems: [content], applicationActivities: nil)
            if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let root = scene.windows.first?.rootViewController {
                root.present(av, animated: true)
            }
        }
    }
    
    private func formatBytes(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }
}

// MARK: - Share Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
