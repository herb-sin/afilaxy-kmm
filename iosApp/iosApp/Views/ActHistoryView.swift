import SwiftUI
import shared

struct ActHistoryView: View {
    @StateObject private var wrapper: ActViewModelWrapper = {
        if let vm = ViewModelProvider.shared.getActViewModel() {
            return ActViewModelWrapper(vm)
        }
        return ActViewModelWrapper.empty()
    }()

    var body: some View {
        Group {
            let history: [AsthmaControlTestResult] = Array(wrapper.state?.history ?? [])
            if wrapper.state?.isLoading == true && history.isEmpty {
                ProgressView()
            } else if history.isEmpty {
                Text("Nenhum teste realizado ainda.")
                    .foregroundColor(.secondary)
            } else {
                List(history, id: \.id) { result in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(formatDate(result.timestamp))
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text(result.level.label).fontWeight(.semibold)
                        }
                        Spacer()
                        Text("\(result.score)")
                            .font(.title2)
                            .fontWeight(.bold)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .navigationTitle("Histórico do ACT")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { wrapper.loadHistory() }
    }
}

private func formatDate(_ timestamp: Int64) -> String {
    let date = Date(timeIntervalSince1970: Double(timestamp) / 1000)
    let formatter = DateFormatter()
    formatter.dateFormat = "dd/MM/yyyy"
    formatter.locale = Locale(identifier: "pt_BR")
    return formatter.string(from: date)
}
