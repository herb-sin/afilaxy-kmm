import SwiftUI
import shared

struct HistoryView: View {
    @EnvironmentObject var container: AppContainer

    var body: some View {
        guard let state = container.history.state else {
            return AnyView(ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity))
        }
        return AnyView(historyBody(state: state))
    }

    @ViewBuilder
    private func historyBody(state: HistoryState) -> some View {
        Group {
            if state.isLoading {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = state.error {
                VStack { Image(systemName: "exclamationmark.triangle").font(.largeTitle); Text(error) }
                    .foregroundColor(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if state.filteredHistory.isEmpty {
                VStack { Image(systemName: "clock").font(.largeTitle); Text("Nenhuma emergência") }
                    .foregroundColor(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(state.filteredHistory, id: \.id) { item in
                    HistoryRowView(item: item)
                }
            }
        }
        .navigationTitle("Histórico")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button("Todas")           { container.history.vm.applyFilter(filter: .all) }
                    Button("Resolvidas")      { container.history.vm.applyFilter(filter: .resolved) }
                    Button("Canceladas")      { container.history.vm.applyFilter(filter: .cancelled) }
                    Button("Como Solicitante"){ container.history.vm.applyFilter(filter: .asRequester) }
                    Button("Como Helper")     { container.history.vm.applyFilter(filter: .asHelper) }
                } label: { Image(systemName: "line.3.horizontal.decrease.circle") }
            }
        }
    }
}

private struct HistoryRowView: View {
    let item: EmergencyHistory

    var statusInfo: (String, Color) {
        switch item.status {
        case "resolved":  return ("✅ Resolvida", .green)
        case "cancelled": return ("❌ Cancelada", .red)
        case "matched":   return ("🤝 Em Atendimento", .orange)
        default:          return ("⏳ \(item.status)", .secondary)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(statusInfo.0).font(.headline).foregroundColor(statusInfo.1)
            Text("Solicitante: \(item.requesterName)").font(.subheadline)
            if let helper = item.helperName {
                Text("Helper: \(helper)").font(.subheadline).foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
