import SwiftUI

private struct FaqItem: Identifiable {
    let id = UUID()
    let pergunta: String
    let resposta: String
}

private let faqItems: [FaqItem] = [
    FaqItem(pergunta: "O que é asma?",
            resposta: "Asma é uma doença crônica que afeta as vias respiratórias, causando inflamação e estreitamento dos brônquios."),
    FaqItem(pergunta: "Quais são os sintomas?",
            resposta: "Falta de ar, chiado no peito, tosse e aperto no peito são os principais sintomas."),
    FaqItem(pergunta: "Como usar a bombinha?",
            resposta: "Agite a bombinha, expire completamente, coloque o bocal na boca, pressione e inspire profundamente."),
    FaqItem(pergunta: "O que fazer em uma crise?",
            resposta: "Use a bombinha de alívio imediato, sente-se em posição confortável e respire calmamente. Se não melhorar, procure ajuda médica."),
    FaqItem(pergunta: "Como prevenir crises?",
            resposta: "Evite gatilhos (poeira, fumaça, pólen), use medicação preventiva conforme prescrito e mantenha acompanhamento médico.")
]

struct AutocuidadoView: View {
    var body: some View {
        List {
            Section {
                Text("Perguntas Frequentes sobre Asma")
                    .font(.headline)
                    .padding(.vertical, 4)
            }
            ForEach(faqItems) { item in
                FaqRow(item: item)
            }
        }
        .navigationTitle("Autocuidado")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct FaqRow: View {
    let item: FaqItem
    @State private var expanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                withAnimation { expanded.toggle() }
            } label: {
                HStack {
                    Text(item.pergunta)
                        .font(.subheadline)
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.leading)
                    Spacer()
                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                        .font(.caption)
                }
            }
            if expanded {
                Text(item.resposta)
                    .font(.body)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
