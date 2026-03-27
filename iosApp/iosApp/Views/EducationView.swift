import SwiftUI

struct EducationView: View {
    @State private var selectedCategory: EducationCategory = .basics
    
    var body: some View {
        NavigationView {
            ScrollView {
                LazyVStack(spacing: 20) {
                    // Welcome Card
                    HeroGradientCard {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Image(systemName: "graduationcap.fill")
                                    .font(.title2)
                                    .foregroundColor(.white)
                                
                                Text("Educação sobre Asma")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                            }
                            
                            Text("Conhecimento é poder! Entenda melhor sua condição e aprenda a viver bem com asma.")
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.9))
                        }
                    }
                    
                    // Category Selector
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(EducationCategory.allCases, id: \.self) { category in
                                CategoryChip(
                                    category: category,
                                    isSelected: selectedCategory == category
                                ) {
                                    selectedCategory = category
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                    
                    // Content based on selected category
                    contentForCategory(selectedCategory)
                }
                .padding()
            }
            .background(Color.afiBackground)
            .navigationTitle("Educação")
            .navigationBarTitleDisplayMode(.large)
        }
    }
    
    @ViewBuilder
    private func contentForCategory(_ category: EducationCategory) -> some View {
        switch category {
        case .basics:
            BasicsContent()
        case .medications:
            MedicationsContent()
        case .triggers:
            TriggersContent()
        case .emergency:
            EmergencyContent()
        case .lifestyle:
            LifestyleContent()
        }
    }
}

enum EducationCategory: String, CaseIterable {
    case basics = "Básico"
    case medications = "Medicamentos"
    case triggers = "Gatilhos"
    case emergency = "Emergência"
    case lifestyle = "Estilo de Vida"
    
    var icon: String {
        switch self {
        case .basics: return "info.circle.fill"
        case .medications: return "pills.fill"
        case .triggers: return "exclamationmark.triangle.fill"
        case .emergency: return "cross.circle.fill"
        case .lifestyle: return "heart.fill"
        }
    }
}

struct CategoryChip: View {
    let category: EducationCategory
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: category.icon)
                    .font(.caption)
                
                Text(category.rawValue)
                    .font(.subheadline)
                    .fontWeight(.medium)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(isSelected ? Color.afiPrimary : Color.afiSurfaceContainer)
            .foregroundColor(isSelected ? .white : .afiTextPrimary)
            .cornerRadius(20)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Content Views

struct BasicsContent: View {
    var body: some View {
        LazyVStack(spacing: 16) {
            EducationCard(
                title: "O que é Asma?",
                icon: "info.circle.fill",
                iconColor: .afiPrimary,
                content: "A asma é uma doença crônica que afeta as vias respiratórias, causando inflamação e estreitamento dos brônquios. Isso dificulta a passagem do ar, causando sintomas como falta de ar, chiado no peito e tosse."
            )
            
            EducationCard(
                title: "Sintomas Principais",
                icon: "exclamationmark.triangle.fill",
                iconColor: .afiWarning,
                content: """
                • Falta de ar ou dificuldade para respirar
                • Chiado no peito (sibilância)
                • Tosse, especialmente à noite
                • Sensação de aperto no peito
                • Cansaço durante atividades físicas
                """
            )
            
            EducationCard(
                title: "Tipos de Asma",
                icon: "list.bullet.circle.fill",
                iconColor: .afiTertiary,
                content: """
                • Asma alérgica: Causada por alérgenos
                • Asma não-alérgica: Causada por irritantes
                • Asma ocupacional: Relacionada ao trabalho
                • Asma induzida por exercício
                • Asma noturna: Sintomas piores à noite
                """
            )
        }
    }
}

struct MedicationsContent: View {
    var body: some View {
        LazyVStack(spacing: 16) {
            EducationCard(
                title: "Medicamentos de Resgate",
                icon: "cross.circle.fill",
                iconColor: .afiError,
                content: """
                São broncodilatadores de ação rápida (beta-agonistas) usados durante crises:
                
                • Salbutamol (Aerolin®)
                • Fenoterol (Berotec®)
                
                USO: Apenas durante crises ou antes de exercícios. NÃO use diariamente!
                """
            )
            
            EducationCard(
                title: "Medicamentos de Manutenção",
                icon: "calendar.circle.fill",
                iconColor: .afiSuccess,
                content: """
                São anti-inflamatórios usados diariamente para prevenir crises:
                
                • Corticoides inalatórios
                • Broncodilatadores de longa duração
                • Antileucotrienos
                
                USO: Todos os dias, mesmo sem sintomas!
                """
            )
            
            EducationCard(
                title: "Como Usar o Inalador",
                icon: "play.circle.fill",
                iconColor: .afiPrimary,
                content: """
                1. Retire a tampa e agite o inalador
                2. Expire completamente
                3. Coloque os lábios ao redor do bocal
                4. Inspire profundamente e pressione
                5. Segure a respiração por 10 segundos
                6. Expire lentamente
                """
            )
            
            ImportantNote(
                text: "⚠️ IMPORTANTE: Medicamentos de resgate são para emergências. Se você usa mais de 2x por semana, procure seu médico!"
            )
        }
    }
}

struct TriggersContent: View {
    var body: some View {
        LazyVStack(spacing: 16) {
            EducationCard(
                title: "Alérgenos Comuns",
                icon: "allergens",
                iconColor: .afiWarning,
                content: """
                • Ácaros da poeira
                • Pelos de animais
                • Pólen de plantas
                • Fungos e mofo
                • Baratas e seus dejetos
                """
            )
            
            EducationCard(
                title: "Irritantes Ambientais",
                icon: "wind",
                iconColor: .afiError,
                content: """
                • Fumaça de cigarro
                • Poluição do ar
                • Produtos de limpeza
                • Perfumes e sprays
                • Tinta fresca
                • Ar frio ou seco
                """
            )
            
            EducationCard(
                title: "Outros Gatilhos",
                icon: "ellipsis.circle.fill",
                iconColor: .afiTertiary,
                content: """
                • Exercícios intensos
                • Estresse emocional
                • Infecções respiratórias
                • Refluxo gastroesofágico
                • Alguns medicamentos
                • Mudanças climáticas
                """
            )
            
            EducationCard(
                title: "Como Evitar Gatilhos",
                icon: "shield.fill",
                iconColor: .afiSuccess,
                content: """
                • Mantenha a casa limpa e arejada
                • Use capas antialérgicas
                • Evite tapetes e cortinas pesadas
                • Não fume e evite fumantes
                • Use máscara em locais poluídos
                • Controle a umidade (40-60%)
                """
            )
        }
    }
}

struct EmergencyContent: View {
    var body: some View {
        LazyVStack(spacing: 16) {
            EducationCard(
                title: "Sinais de Crise Grave",
                icon: "exclamationmark.triangle.fill",
                iconColor: .afiError,
                content: """
                🚨 PROCURE AJUDA IMEDIATAMENTE:
                
                • Dificuldade extrema para respirar
                • Não consegue falar frases completas
                • Lábios ou unhas azulados
                • Medicamento de resgate não faz efeito
                • Confusão mental ou sonolência
                """
            )
            
            EducationCard(
                title: "O que Fazer na Crise",
                icon: "cross.fill",
                iconColor: .afiPrimary,
                content: """
                1. Mantenha a calma
                2. Use o medicamento de resgate
                3. Sente-se ereto, não se deite
                4. Respire lentamente
                5. Se não melhorar em 15 min, procure ajuda
                6. Ligue 192 (SAMU) se necessário
                """
            )
            
            EducationCard(
                title: "Kit de Emergência",
                icon: "medical.thermometer.fill",
                iconColor: .afiTertiary,
                content: """
                Sempre tenha com você:
                
                • Medicamento de resgate
                • Lista de medicamentos
                • Contatos de emergência
                • Plano de ação da asma
                • Documento de identidade
                """
            )
            
            ImportantNote(
                text: "📱 Use o Afilaxy para encontrar ajuda rápida durante uma crise!"
            )
        }
    }
}

struct LifestyleContent: View {
    var body: some View {
        LazyVStack(spacing: 16) {
            EducationCard(
                title: "Exercícios e Asma",
                icon: "figure.run",
                iconColor: .afiSuccess,
                content: """
                • Exercite-se regularmente, mas com cuidado
                • Faça aquecimento antes
                • Use medicamento preventivo se prescrito
                • Prefira atividades como natação
                • Evite exercícios em dias frios e secos
                • Pare se sentir sintomas
                """
            )
            
            EducationCard(
                title: "Alimentação Saudável",
                icon: "leaf.fill",
                iconColor: .afiPrimary,
                content: """
                • Mantenha peso adequado
                • Coma frutas e vegetais ricos em antioxidantes
                • Evite alimentos que causam refluxo
                • Beba bastante água
                • Evite conservantes se for alérgico
                • Considere suplementos (com orientação médica)
                """
            )
            
            EducationCard(
                title: "Sono e Descanso",
                icon: "bed.double.fill",
                iconColor: .afiTertiary,
                content: """
                • Durma 7-8 horas por noite
                • Mantenha o quarto limpo e arejado
                • Use travesseiros antialérgicos
                • Evite animais no quarto
                • Eleve a cabeceira se tem refluxo
                • Mantenha horários regulares
                """
            )
            
            EducationCard(
                title: "Controle do Estresse",
                icon: "heart.fill",
                iconColor: .afiWarning,
                content: """
                • Pratique técnicas de relaxamento
                • Faça meditação ou yoga
                • Mantenha hobbies prazerosos
                • Converse com amigos e família
                • Procure ajuda psicológica se necessário
                • Aprenda técnicas de respiração
                """
            )
        }
    }
}

struct EducationCard: View {
    let title: String
    let icon: String
    let iconColor: Color
    let content: String
    
    var body: some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: icon)
                        .font(.title2)
                        .foregroundColor(iconColor)
                    
                    Text(title)
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.afiTextPrimary)
                    
                    Spacer()
                }
                
                Text(content)
                    .font(.subheadline)
                    .foregroundColor(.afiTextSecondary)
                    .lineSpacing(2)
            }
        }
    }
}

struct ImportantNote: View {
    let text: String
    
    var body: some View {
        AfilaxyCard(backgroundColor: .afiErrorContainer) {
            Text(text)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.afiOnErrorContainer)
        }
    }
}

#if DEBUG
struct EducationView_Previews: PreviewProvider {
    static var previews: some View {
        EducationView()
    }
}
#endif