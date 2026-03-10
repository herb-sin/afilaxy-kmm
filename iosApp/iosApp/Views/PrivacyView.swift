import SwiftUI

struct PrivacyView: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                    Text("Política de Privacidade")
                        .font(.title)
                        .fontWeight(.bold)
                    
                    Text("Última atualização: Janeiro de 2024")
                        .font(.caption)
                        .foregroundColor(.gray)
                    
                    PrivacySection(
                        title: "1. Informações Coletadas",
                        content: "Coletamos nome, email, localização GPS, mensagens de chat e token FCM."
                    )
                    
                    PrivacySection(
                        title: "2. Uso das Informações",
                        content: "Usamos seus dados para conectar você com helpers, enviar notificações e melhorar o serviço."
                    )
                    
                    PrivacySection(
                        title: "3. Localização",
                        content: "Coletada apenas durante criação de emergências ou modo helper ativo."
                    )
                    
                    PrivacySection(
                        title: "4. Compartilhamento",
                        content: "Compartilhamos dados apenas com helpers durante emergências. Nunca vendemos seus dados."
                    )
                    
                    PrivacySection(
                        title: "5. Armazenamento",
                        content: "Dados armazenados no Firebase com criptografia e em conformidade com LGPD."
                    )
                    
                    PrivacySection(
                        title: "6. Seus Direitos",
                        content: "Você pode acessar, corrigir, excluir ou exportar seus dados a qualquer momento."
                    )
                    
                    PrivacySection(
                        title: "7. Segurança",
                        content: "Implementamos autenticação Firebase, HTTPS e monitoramento de segurança."
                    )
                    
                    Text("Em conformidade com a LGPD")
                        .font(.caption)
                        .foregroundColor(.gray)
                        .padding(.top)
                }
                .padding()
        }
        .navigationTitle("Privacidade")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct PrivacySection: View {
    let title: String
    let content: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .foregroundColor(.red)
            
            Text(content)
                .font(.body)
                .foregroundColor(.primary)
        }
    }
}
