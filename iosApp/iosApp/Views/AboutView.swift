import SwiftUI

struct AboutView: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                    Image(systemName: "heart.circle.fill")
                        .font(.system(size: 80))
                        .foregroundColor(.red)
                    
                    Text("Afilaxy")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    
                    Text("Versão 2.1.0-kmm")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                    
                    Divider()
                        .padding(.vertical)
                    
                    Text("Sistema de Emergência Médica")
                        .font(.title3)
                        .fontWeight(.semibold)
                    
                    Text("Conectamos pessoas em situações críticas com helpers próximos, oferecendo suporte rápido e eficiente.")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.gray)
                        .padding(.horizontal)
                    
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Recursos")
                            .font(.headline)
                        
                        FeatureRow(icon: "exclamationmark.triangle.fill", text: "Emergências em tempo real")
                        FeatureRow(icon: "message.fill", text: "Chat direto com helpers")
                        FeatureRow(icon: "location.fill", text: "Busca de helpers próximos")
                        FeatureRow(icon: "lock.fill", text: "Autenticação segura")
                        FeatureRow(icon: "globe", text: "Multiplataforma")
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    .padding(.horizontal)
                    
                    Text("Desenvolvido com ❤️")
                        .font(.caption)
                        .foregroundColor(.gray)
                    
                    Text("© 2024 Afilaxy")
                        .font(.caption2)
                        .foregroundColor(.gray)
                }
                .padding(.vertical, 32)
            }
            .navigationTitle("Sobre")
            .navigationBarTitleDisplayMode(.inline)
        }
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
