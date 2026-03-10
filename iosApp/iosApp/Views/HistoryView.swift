import SwiftUI
import shared

struct HistoryView: View {
    @EnvironmentObject var container: AppContainer
    @State private var vmState: HistoryState? = nil

    var body: some View {
        Group {
            if vmState?.isLoading == true {
                ProgressView()
            } else if let error = vmState?.error {
                ContentUnavailableView(error, systemImage: "exclamationmark.triangle")
            } else if vmState?.filteredHistory.isEmpty != false {
                ContentUnavailableView("Nenhuma emergência", systemImage: "clock")
            } else {
                List(vmState?.filteredHistory ?? [], id: \.id) { item in
                    HistoryRowView(item: item)
                }
            }
        }
        .navigationTitle("Histórico")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button("Todas") { container.historyViewModel.applyFilter(filter: .all) }
                    Button("Resolvidas") { container.historyViewModel.applyFilter(filter: .resolved) }
                    Button("Canceladas") { container.historyViewModel.applyFilter(filter: .cancelled) }
                    Button("Como Solicitante") { container.historyViewModel.applyFilter(filter: .asRequester) }
                    Button("Como Helper") { container.historyViewModel.applyFilter(filter: .asHelper) }
                } label: {
                    Image(systemName: "line.3.horizontal.decrease.circle")
                }
            }
        }
        .task {
            for await state in container.historyViewModel.state {
                vmState = state
            }
        }
    }
}

private struct HistoryRowView: View {
    let item: EmergencyHistory

    var statusLabel: (String, Color) {
        switch item.status {
        case "resolved":  return ("✅ Resolvida", .green)
        case "cancelled": return ("❌ Cancelada", .red)
        case "matched":   return ("🤝 Em Atendimento", .orange)
        default:          return ("⏳ \(item.status)", .secondary)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(statusLabel.0)
                .font(.headline)
                .foregroundColor(statusLabel.1)
            Text("Solicitante: \(item.requesterName)")
                .font(.subheadline)
            if let helper = item.helperName {
                Text("Helper: \(helper)")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
