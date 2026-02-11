import SwiftUI
import shared

struct EmergencyRequestView: View {
    @Environment(\.dismiss) var dismiss
    let emergencyId: String
    @StateObject private var viewModel = ObservableEmergencyViewModel()
    @State private var showChat = false
    
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            ProgressView()
                .scaleEffect(2)
            
            Text("Procurando Helper...")
                .font(.title2)
                .fontWeight(.semibold)
            
            Text("Aguarde enquanto notificamos helpers próximos")
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            Spacer()
            
            Button(action: {
                viewModel.cancelEmergency()
                dismiss()
            }) {
                Text("Cancelar Emergência")
                    .fontWeight(.semibold)
                    .foregroundColor(.red)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(10)
            .padding(.horizontal, 32)
            .disabled(viewModel.isLoading)
        }
        .padding(.vertical, 32)
        .onChange(of: viewModel.emergencyStatus) { status in
            if status == "matched" {
                showChat = true
            }
        }
        .fullScreenCover(isPresented: $showChat) {
            ChatView(emergencyId: emergencyId)
        }
        .onAppear {
            viewModel.observeEmergencyStatus(emergencyId)
        }
    }
}

class ObservableEmergencyViewModel: ObservableObject {
    private let viewModel: EmergencyViewModel
    @Published var description = ""
    @Published var isLoading = false
    @Published var error: String?
    @Published var emergencyId: String?
    @Published var emergencyStatus: String?
    
    init() {
        viewModel = ViewModelProvider.shared.getEmergencyViewModel()
        observeState()
    }
    
    private func observeState() {
        viewModel.state.watch { [weak self] state in
            guard let state = state as? EmergencyState else { return }
            DispatchQueue.main.async {
                self?.description = state.description_
                self?.isLoading = state.isLoading
                self?.error = state.error
                self?.emergencyId = state.activeEmergencyId
                self?.emergencyStatus = state.emergencyStatus
            }
        }
    }
    
    func onDescriptionChange(_ description: String) {
        viewModel.onDescriptionChange(description: description)
    }
    
    func createEmergency() {
        viewModel.onCreateEmergency()
    }
    
    func cancelEmergency() {
        viewModel.onCancelEmergency()
    }
    
    func observeEmergencyStatus(_ emergencyId: String) {
        viewModel.observeEmergencyStatus(emergencyId: emergencyId)
    }
}
