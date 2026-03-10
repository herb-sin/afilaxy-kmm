import SwiftUI

struct TermsView: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                    Text("Termos de Uso")
                        .font(.title)
                        .fontWeight(.bold)
                    
                    Text("Última atualização: Janeiro de 2024")
                        .font(.caption)
                        .foregroundColor(.gray)
                    
                    TermSection(
                        title: "1. Aceitação dos Termos",
                        content: "Ao utilizar o Afilaxy, você concorda com estes termos de uso."
                    )
                    
                    TermSection(
                        title: "2. Descrição do Serviço",
                        content: "O Afilaxy conecta pessoas em emergências com helpers voluntários. Não substitui atendimento médico profissional."
                    )
                    
                    TermSection(
                        title: "3. Responsabilidades",
                        content: "• Fornecer informações verdadeiras\n• Usar apenas para emergências reais\n• Respeitar outros usuários"
                    )
                    
                    TermSection(
                        title: "4. Limitação de Responsabilidade",
                        content: "O Afilaxy não se responsabiliza por ações de usuários ou qualidade do atendimento."
                    )
                    
                    TermSection(
                        title: "5. Privacidade",
                        content: "Seus dados são tratados conforme nossa Política de Privacidade."
                    )
                }
                .padding()
        }
        .navigationTitle("Termos de Uso")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct TermSection: View {
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
