import SwiftUI
import shared

struct HistoryView: View {
    @EnvironmentObject var container: AppContainer
    @State private var selectedFilter: HistoryFilter = .all
    @State private var showFilterSheet = false
    @State private var searchText = ""

    var body: some View {
        guard let state = container.history.state else {
            return AnyView(LoadingCard().padding())
        }
        return AnyView(historyBody(state: state))
    }

    @ViewBuilder
    private func historyBody(state: HistoryState) -> some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                if state.isLoading {
                    LoadingCard()
                } else if let error = state.error {
                    ErrorCard(message: error)
                } else if state.filteredHistory.isEmpty {
                    EmptyHistoryCard()
                } else {
                    // Statistics Hero Card
                    StatisticsHeroCard(history: state.filteredHistory)
                    
                    // Filter Controls
                    FilterControlsCard(selectedFilter: $selectedFilter) { filter in
                        // Apply filter logic here
                    }
                    
                    // Timeline Section
                    TimelineHeaderCard(count: state.filteredHistory.count)
                    
                    // History Timeline
                    ForEach(groupedHistory(state.filteredHistory), id: \.key) { group in
                        TimelineGroupCard(date: group.key, items: group.value)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 100)
        }
        .navigationTitle("Histórico")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $searchText, prompt: "Buscar por helper ou localização")
    }
    
    private func groupedHistory(_ history: [EmergencyHistory]) -> [(key: String, value: [EmergencyHistory])] {
        let grouped = Dictionary(grouping: history) { item in
            formatDateGroup(item.timestamp)
        }
        return grouped.sorted { $0.key > $1.key }
    }
    
    private func formatDateGroup(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp / 1000))
        let formatter = DateFormatter()
        formatter.dateFormat = "dd/MM/yyyy"
        return formatter.string(from: date)
    }
}

// MARK: - Supporting Views

struct StatisticsHeroCard: View {
    let history: [EmergencyHistory]
    
    private var stats: (total: Int, resolved: Int, asHelper: Int, avgResponse: String) {
        let total = history.count
        let resolved = history.filter { $0.status == "resolved" }.count
        let asHelper = history.filter { $0.helperName != nil }.count
        return (total, resolved, asHelper, "2.5 min") // Mock avg response time
    }
    
    var body: some View {
        HeroGradientCard {
            VStack(spacing: 16) {
                HStack {
                    Image(systemName: "chart.line.uptrend.xyaxis")
                        .font(.title2)
                        .foregroundColor(.white)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Suas Estatísticas")
                            .font(.headline)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                        Text("Resumo das suas atividades")
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.8))
                    }
                    
                    Spacer()
                }
                
                HStack(spacing: 20) {
                    StatMetric(value: "\(stats.total)", label: "Total", icon: "list.bullet")
                    StatMetric(value: "\(stats.resolved)", label: "Resolvidas", icon: "checkmark.circle")
                    StatMetric(value: "\(stats.asHelper)", label: "Ajudou", icon: "heart")
                    StatMetric(value: stats.avgResponse, label: "Tempo Médio", icon: "clock")
                }
            }
        }
    }
}

struct StatMetric: View {
    let value: String
    let label: String
    let icon: String
    
    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(.white.opacity(0.8))
            
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.white)
            
            Text(label)
                .font(.caption2)
                .foregroundColor(.white.opacity(0.7))
        }
        .frame(maxWidth: .infinity)
    }
}

struct FilterControlsCard: View {
    @Binding var selectedFilter: HistoryFilter
    let onFilterChange: (HistoryFilter) -> Void
    
    init(selectedFilter: Binding<HistoryFilter>, onFilterChange: @escaping (HistoryFilter) -> Void) {
        self._selectedFilter = selectedFilter
        self.onFilterChange = onFilterChange
    }
    
    private let filters: [(HistoryFilter, String, String)] = [
        (.all, "Todas", "list.bullet"),
        (.resolved, "Resolvidas", "checkmark.circle"),
        (.cancelled, "Canceladas", "xmark.circle"),
        (.asHelper, "Como Helper", "heart")
    ]
    
    var body: some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Filtros")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(filters, id: \.0) { filter in
                            FilterChip(
                                title: filter.1,
                                icon: filter.2,
                                isSelected: selectedFilter == filter.0
                            ) {
                                selectedFilter = filter.0
                                onFilterChange(filter.0)
                            }
                        }
                    }
                    .padding(.horizontal, 4)
                }
            }
        }
    }
}

struct FilterChip: View {
    let title: String
    let icon: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.caption)
                Text(title)
                    .font(.caption)
                    .fontWeight(.medium)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                isSelected ? Color.afiPrimary : Color.afiSurface
            )
            .foregroundColor(
                isSelected ? .white : Color.afiOnSurface
            )
            .clipShape(Capsule())
        }
    }
}

struct TimelineHeaderCard: View {
    let count: Int
    
    var body: some View {
        AfilaxyCard {
            HStack {
                Image(systemName: "timeline.selection")
                    .foregroundColor(Color.afiprimary)
                    .font(.title3)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text("Timeline")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text("\(count) emergência(s) encontrada(s)")
                        .font(.caption)
                        .foregroundColor(Color.afionSurface.opacity(0.6))
                }
                
                Spacer()
                
                Text("Mais Recentes")
                    .font(.caption)
                    .foregroundColor(Color.afiprimary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.afiprimary.opacity(0.1))
                    .clipShape(Capsule())
            }
        }
    }
}

struct TimelineGroupCard: View {
    let date: String
    let items: [EmergencyHistory]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Date Header
            HStack {
                Text(date)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(Color.afiprimary)
                
                Rectangle()
                    .fill(Color.afiprimary.opacity(0.3))
                    .frame(height: 1)
                
                Text("\(items.count)")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(Color.afiprimary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.afiprimary.opacity(0.1))
                    .clipShape(Circle())
            }
            
            // Timeline Items
            VStack(spacing: 8) {
                ForEach(items, id: \.id) { item in
                    TimelineItemCard(item: item)
                }
            }
        }
    }
}

struct TimelineItemCard: View {
    let item: EmergencyHistory
    
    private var statusInfo: (String, Color, String) {
        switch item.status {
        case "resolved":  return ("Resolvida", .green, "checkmark.circle.fill")
        case "cancelled": return ("Cancelada", .red, "xmark.circle.fill")
        case "matched":   return ("Em Atendimento", .orange, "person.2.fill")
        default:          return (item.status, Color.afionSurface.opacity(0.6), "clock.fill")
        }
    }
    
    private var timeString: String {
        let date = Date(timeIntervalSince1970: TimeInterval(item.timestamp / 1000))
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
    
    var body: some View {
        AfilaxyCard {
            VStack(spacing: 12) {
                // Header
                HStack {
                    HStack(spacing: 8) {
                        Image(systemName: statusInfo.2)
                            .foregroundColor(statusInfo.1)
                            .font(.subheadline)
                        
                        Text(statusInfo.0)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(statusInfo.1)
                    }
                    
                    Spacer()
                    
                    Text(timeString)
                        .font(.caption)
                        .foregroundColor(Color.afionSurface.opacity(0.6))
                }
                
                // Content
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "person.fill")
                            .foregroundColor(Color.afionSurface.opacity(0.6))
                            .font(.caption)
                            .frame(width: 16)
                        
                        Text("Solicitante: \(item.requesterName)")
                            .font(.subheadline)
                    }
                    
                    if let helper = item.helperName {
                        HStack {
                            Image(systemName: "heart.fill")
                                .foregroundColor(Color.afiprimary)
                                .font(.caption)
                                .frame(width: 16)
                            
                            Text("Helper: \(helper)")
                                .font(.subheadline)
                                .foregroundColor(Color.afiprimary)
                        }
                    }
                    
                    if let locationString = "São Paulo, SP" {
                        HStack {
                            Image(systemName: "location.fill")
                                .foregroundColor(Color.afionSurface.opacity(0.6))
                                .font(.caption)
                                .frame(width: 16)
                            
                            Text(locationString)
                                .font(.caption)
                                .foregroundColor(Color.afionSurface.opacity(0.6))
                        }
                    }
                }
            }
        }
    }
}

struct EmptyHistoryCard: View {
    var body: some View {
        AfilaxyCard {
            VStack(spacing: 16) {
                Image(systemName: "clock.badge.questionmark")
                    .font(.system(size: 48))
                    .foregroundColor(Color.afionSurface.opacity(0.4))
                
                VStack(spacing: 8) {
                    Text("Nenhuma Emergência")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text("Seu histórico aparecerá aqui quando você solicitar ou ajudar em emergências")
                        .font(.subheadline)
                        .foregroundColor(Color.afionSurface.opacity(0.6))
                        .multilineTextAlignment(.center)
                }
                
                Button("Ativar Modo Helper") {
                    // Navigate to helper activation
                }
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(Color.afiprimary)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(Color.afiprimary.opacity(0.1))
                .clipShape(Capsule())
            }
            .padding(.vertical, 20)
        }
    }
}
