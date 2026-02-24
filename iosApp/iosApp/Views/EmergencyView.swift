import SwiftUI
import shared

struct EmergencyView: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var viewModel = ObservableEmergencyViewModel()
    @State private var showRequest = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.red)
                
                Text("Descreva a Emergência")
                    .font(.title2)
                    .fontWeight(.bold)
                
                TextEditor(text: $viewModel.description)
                    .frame(height: 150)
                    .padding(8)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                    )
                    .onChange(of: viewModel.description) { newValue in
                        viewModel.onDescriptionChange(newValue)
                    }
                
                if let error = viewModel.error {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }
                
                Button(action: {
                    viewModel.createEmergency()
                }) {
                    if viewModel.isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Text("Solicitar Ajuda")
                            .fontWeight(.semibold)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.red)
                .foregroundColor(.white)
                .cornerRadius(10)
                .disabled(viewModel.isLoading || viewModel.description.isEmpty)
                
                Spacer()
            }
            .padding(32)
            .navigationTitle("Emergência")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancelar") {
                        dismiss()
                    }
                }
            }
            .onChange(of: viewModel.emergencyId) { emergencyId in
                if emergencyId != nil {
                    showRequest = true
                }
            }
            .fullScreenCover(isPresented: $showRequest) {
                if let emergencyId = viewModel.emergencyId {
                    EmergencyRequestView(emergencyId: emergencyId, viewModel: viewModel)
                }
            }
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
