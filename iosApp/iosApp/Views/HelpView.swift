import SwiftUI

struct HelpView: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                    Text("Como usar o Afilaxy")
                        .font(.title)
                        .fontWeight(.bold)
                    
                    HelpCard(
                        icon: "exclamationmark.triangle.fill",
                        title: "Criar Emergência",
                        description: "Toque no botão vermelho, descreva a situação e aguarde um helper aceitar."
                    )
                    
                    HelpCard(
                        icon: "heart.fill",
                        title: "Ser Helper",
                        description: "Ative o modo helper no menu. Você receberá notificações de emergências próximas."
                    )
                    
                    HelpCard(
                        icon: "location.fill",
                        title: "Permissões",
                        description: "Permita acesso à localização 'sempre' para receber emergências próximas."
                    )
                    
                    HelpCard(
                        icon: "bell.fill",
                        title: "Notificações",
                        description: "Mantenha notificações ativadas para ser alertado sobre emergências."
                    )
                    
                    Divider()
                        .padding(.vertical)
                    
                    Text("Perguntas Frequentes")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    FAQItem(
                        question: "O que fazer em uma emergência?",
                        answer: "Toque no botão de emergência, descreva a situação e aguarde um helper próximo."
                    )
                    
                    FAQItem(
                        question: "Como cancelar uma emergência?",
                        answer: "Na tela de aguardo, toque em 'Cancelar Emergência'."
                    )
                    
                    FAQItem(
                        question: "Posso ser helper e solicitar emergência?",
                        answer: "Sim! Você pode alternar entre os modos a qualquer momento."
                    )
                }
                .padding()
            }
            .navigationTitle("Ajuda")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

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

struct FAQItem: View {
    let question: String
    let answer: String
    @State private var isExpanded = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button(action: {
                withAnimation {
                    isExpanded.toggle()
                }
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
