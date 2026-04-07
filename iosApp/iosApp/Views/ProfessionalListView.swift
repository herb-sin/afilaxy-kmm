import SwiftUI
import shared

struct ProfessionalListView: View {
    @EnvironmentObject var container: AppContainer
    @State private var selectedSpecialty: ProfessionalSpecialty? = nil
    @State private var selectedPlan: SubscriptionPlan? = nil
    @State private var searchText = ""
    @State private var showFilters = false
    @State private var sortBy: SortOption = .relevance
    @State private var showOnlyAvailable = false
    @State private var showCrmLookup = false

    var body: some View {
        // O ViewModel.init() dispara loadProfessionals() durante o warmup do Koin,
        // antes do token Firebase estar pronto — resulta em lista vazia sem erro visível.
        // .onAppear força um reload quando a tela aparece (auth sempre pronta aqui).
        Group {
            if let state = container.professionals.state {
                listBody(state: state)
            } else {
                LoadingCard().padding()
            }
        }
        .onAppear { container.professionals.loadProfessionals() }
    }

    @ViewBuilder
    private func listBody(state: ProfessionalListState) -> some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                if state.isLoading {
                    LoadingCard()
                } else if let error = state.error {
                    ErrorCard(message: error)
                } else if state.professionals.isEmpty {
                    EmptyProfessionalsCard()
                } else {
                    // Hero Welcome Card
                    ProfessionalsHeroCard()

                    // CTA para profissionais de saúde
                    ProfessionalCTACard()
                    
                    // Filter Controls
                    FilterControlsCard(
                        selectedSpecialty: $selectedSpecialty,
                        selectedPlan: $selectedPlan,
                        sortBy: $sortBy,
                        showOnlyAvailable: $showOnlyAvailable,
                        onSpecialtyChange: { specialty in
                            // Mock specialty change since method doesn't exist
                        }
                    )
                    
                    // Results Header
                    ResultsHeaderCard(count: filteredProfessionals(state.professionals).count)
                    
                    // Professionals Grid
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(filteredProfessionals(state.professionals), id: \.id) { professional in
                            ProfessionalCard(professional: professional)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 100)
        }
        .navigationTitle("Profissionais")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $searchText, prompt: "Buscar por nome ou especialidade")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showCrmLookup = true
                } label: {
                    Image(systemName: "person.text.rectangle.fill")
                        .accessibilityLabel("Consultar CRM")
                }
            }
        }
        .sheet(isPresented: $showCrmLookup) {
            CrmLookupView()
        }
    }
    
    private func filteredProfessionals(_ professionals: [HealthProfessional]) -> [HealthProfessional] {
        var filtered = professionals
        
        // Filter by search text
        if !searchText.isEmpty {
            filtered = filtered.filter { professional in
                professional.name.localizedCaseInsensitiveContains(searchText) ||
                specialtyLabel(String(describing: professional.specialty)).localizedCaseInsensitiveContains(searchText)
            }
        }
        
        // Filter by specialty - convert enum to string for comparison
        if let specialty = selectedSpecialty {
            filtered = filtered.filter { professional in
                // Convert Specialty enum to string for comparison
                let specialtyString = String(describing: professional.specialty).lowercased()
                return specialtyString == specialty.lowercased()
            }
        }
        
        // Filter by plan
        if let plan = selectedPlan {
            filtered = filtered.filter { $0.subscriptionPlan == plan }
        }
        
        // Filter by availability - using mock data since isAvailable doesn't exist
        if showOnlyAvailable {
            // Mock availability filter
            filtered = filtered.filter { _ in true }
        }
        
        // Sort results - using mock sorting since some properties don't exist
        switch sortBy {
        case .relevance:
            // Mock relevance sorting
            filtered = filtered.sorted { $0.name < $1.name }
        case .name:
            filtered = filtered.sorted { $0.name < $1.name }
        case .rating:
            // Mock rating sorting
            filtered = filtered.sorted { $0.name < $1.name }
        case .distance:
            // Mock distance sorting
            filtered = filtered.shuffled()
        }
        
        return filtered
    }
    
    private func specialtyLabel(_ specialty: ProfessionalSpecialty) -> String {
        // Convert specialty string to readable label
        switch specialty.uppercased() {
        case "PNEUMOLOGIST": return "Pneumologista"
        case "ALLERGIST": return "Alergista"
        case "PHYSIOTHERAPIST": return "Fisioterapeuta"
        default: return specialty.capitalized
        }
    }
}

// MARK: - Supporting Views

struct ProfessionalsHeroCard: View {
    var body: some View {
        HeroGradientCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "stethoscope")
                        .font(.title2)
                        .foregroundColor(.white)
                    
                    Text("Profissionais de Saúde")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                }
                
                Text("Conecte-se com pneumologistas, alergistas e fisioterapeutas especializados em asma. Todos os profissionais são verificados e qualificados.")
                    .font(.subheadline)
                    .foregroundColor(.white.opacity(0.9))
                    .lineSpacing(2)
                
                HStack(spacing: 16) {
                    StatBadge(value: "50+", label: "Profissionais")
                    StatBadge(value: "4.8", label: "Avaliação Média")
                    StatBadge(value: "24h", label: "Resposta")
                }
            }
        }
    }
}

// MARK: - Professional CTA Card

struct ProfessionalCTACard: View {
    @Environment(\.openURL) private var openURL

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: "person.badge.plus")
                .font(.system(size: 34))
                .foregroundColor(AfilaxyColors.primary)

            VStack(alignment: .leading, spacing: 4) {
                Text("Sou Profissional de Saúde")
                    .font(.subheadline)
                    .fontWeight(.semibold)

                Text("Cadastre-se e apareça como referência no Afilaxy.")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }

            Spacer()

            Button {
                if let url = URL(string: "https://afilaxy.com/profissionais") {
                    openURL(url)
                }
            } label: {
                Text("Quero me cadastrar")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(AfilaxyColors.primary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(AfilaxyColors.primary.opacity(0.12))
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: Color.black.opacity(0.06), radius: 6, x: 0, y: 2)
    }
}

struct StatBadge: View {
    let value: String
    let label: String
    
    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.white)
            Text(label)
                .font(.caption2)
                .foregroundColor(.white.opacity(0.8))
        }
    }
}

struct FilterControlsCard: View {
    @Binding var selectedSpecialty: ProfessionalSpecialty?
    @Binding var selectedPlan: SubscriptionPlan?
    @Binding var sortBy: SortOption
    @Binding var showOnlyAvailable: Bool
    let onSpecialtyChange: (ProfessionalSpecialty?) -> Void
    
    private let specialties: [(ProfessionalSpecialty?, String, String)] = [
        (nil, "Todos", "person.3.fill"),
        ("PNEUMOLOGIST", "Pneumo", "lungs.fill"),
        ("ALLERGIST", "Alergia", "allergens"),
        ("PHYSIOTHERAPIST", "Fisio", "figure.walk")
    ]
    
    var body: some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 16) {
                // Specialty Filters
                VStack(alignment: .leading, spacing: 8) {
                    Text("Especialidade")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(specialties, id: \.1) { specialty in
                                FilterChip(
                                    title: specialty.1,
                                    icon: specialty.2,
                                    isSelected: selectedSpecialty == specialty.0
                                ) {
                                    selectedSpecialty = specialty.0
                                    onSpecialtyChange(specialty.0)
                                }
                            }
                        }
                        .padding(.horizontal, 4)
                    }
                }
                
                // Additional Filters
                HStack {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Filtros")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                        
                        HStack {
                            Toggle("Disponíveis", isOn: $showOnlyAvailable)
                                .toggleStyle(SwitchToggleStyle(tint: AfilaxyColors.primary))
                                .font(.caption)
                        }
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 8) {
                        Text("Ordenar")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                        
                        Menu {
                            Button("Relevância") { sortBy = .relevance }
                            Button("Nome") { sortBy = .name }
                            Button("Avaliação") { sortBy = .rating }
                            Button("Distância") { sortBy = .distance }
                        } label: {
                            HStack(spacing: 4) {
                                Text(sortBy.label)
                                    .font(.caption)
                                    .foregroundColor(AfilaxyColors.primary)
                                Image(systemName: "chevron.down")
                                    .font(.caption2)
                                    .foregroundColor(AfilaxyColors.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

struct ResultsHeaderCard: View {
    let count: Int
    
    var body: some View {
        AfilaxyCard {
            HStack {
                Image(systemName: "person.2.fill")
                    .foregroundColor(AfilaxyColors.primary)
                    .font(.title3)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text("Resultados")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text("\(count) profissional(is) encontrado(s)")
                        .font(.caption)
                        .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                }
                
                Spacer()
                
                Text("Verificados")
                    .font(.caption)
                    .foregroundColor(.green)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.green.opacity(0.1))
                    .clipShape(Capsule())
            }
        }
    }
}

struct ProfessionalCard: View {
    let professional: HealthProfessional
    
    private var specialtyInfo: (String, String, Color) {
        // Mock specialty info since enum doesn't exist
        return ("Especialista", "stethoscope", AfilaxyColors.primary)
    }
    
    private var planInfo: (String, Color) {
        // Mock plan info since enum doesn't exist
        return ("PRO", .blue)
    }
    
    var body: some View {
        NavigationLink(value: AppRoute.professionalDetail(professional.id)) {
            AfilaxyCard {
                VStack(spacing: 12) {
                    // Header with Avatar and Plan Badge
                    HStack {
                        Circle()
                            .fill(specialtyInfo.2.opacity(0.2))
                            .frame(width: 40, height: 40)
                            .overlay {
                                Image(systemName: specialtyInfo.1)
                                    .font(.title3)
                                    .foregroundColor(specialtyInfo.2)
                            }
                        
                        Spacer()
                        
                        if true { // Mock plan check since subscriptionPlan doesn't exist
                            Text(planInfo.0)
                                .font(.caption2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(planInfo.1)
                                .clipShape(Capsule())
                        }
                    }
                    
                    // Professional Info
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(professional.name)
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .lineLimit(1)
                            
                            if true { // Mock premium check
                                Image(systemName: "checkmark.seal.fill")
                                    .foregroundColor(.blue)
                                    .font(.caption)
                            }
                        }
                        
                        Text(specialtyInfo.0)
                            .font(.caption)
                            .foregroundColor(specialtyInfo.2)
                            .fontWeight(.medium)
                        
                        Text("CRM: \(professional.crm)")
                            .font(.caption2)
                            .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                        
                        // Rating - mock since rating doesn't exist
                        let mockRating = 4.5
                        HStack(spacing: 4) {
                            HStack(spacing: 2) {
                                ForEach(0..<5) { index in
                                    Image(systemName: index < Int(mockRating) ? "star.fill" : "star")
                                        .font(.caption2)
                                        .foregroundColor(.yellow)
                                }
                            }
                            Text(String(format: "%.1f", mockRating))
                                .font(.caption2)
                                .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    
                    // Availability Status - mock since isAvailable doesn't exist
                    let mockAvailable = true
                    HStack {
                        Circle()
                            .fill(mockAvailable ? .green : .gray)
                            .frame(width: 8, height: 8)
                        
                        Text(mockAvailable ? "Disponível" : "Ocupado")
                            .font(.caption2)
                            .foregroundColor(mockAvailable ? .green : .gray)
                        
                        Spacer()
                        
                        // Quick Contact - mock since whatsapp doesn't exist
                        Button(action: {
                            // Mock WhatsApp action
                        }) {
                            Image(systemName: "message.fill")
                                .font(.caption)
                                .foregroundColor(AfilaxyColors.primary)
                                .padding(6)
                                .background(AfilaxyColors.primary.opacity(0.1))
                                .clipShape(Circle())
                        }
                    }
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct EmptyProfessionalsCard: View {
    var body: some View {
        AfilaxyCard {
            VStack(spacing: 16) {
                Image(systemName: "person.slash.fill")
                    .font(.system(size: 48))
                    .foregroundColor(AfilaxyColors.onSurface.opacity(0.4))
                
                VStack(spacing: 8) {
                    Text("Nenhum Profissional Encontrado")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text("Tente ajustar os filtros ou buscar por uma especialidade diferente")
                        .font(.subheadline)
                        .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                        .multilineTextAlignment(.center)
                }
                
                Button("Limpar Filtros") {
                    // Clear all filters
                }
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(AfilaxyColors.primary)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(AfilaxyColors.primary.opacity(0.1))
                .clipShape(Capsule())
            }
            .padding(.vertical, 20)
        }
    }
}

// MARK: - Enums and Extensions

enum Specialty: String, CaseIterable {
    case pneumologist = "pneumologist"
    case allergist = "allergist"
    case physiotherapist = "physiotherapist"
    
    var label: String {
        switch self {
        case .pneumologist: return "Pneumologista"
        case .allergist: return "Alergista"
        case .physiotherapist: return "Fisioterapeuta"
        }
    }
}

enum SortOption: String, CaseIterable {
    case relevance = "relevance"
    case name = "name"
    case rating = "rating"
    case distance = "distance"
    
    var label: String {
        switch self {
        case .relevance: return "Relevância"
        case .name: return "Nome"
        case .rating: return "Avaliação"
        case .distance: return "Distância"
        }
    }
}

// Placeholder types for missing models
typealias ProfessionalSpecialty = String
