import SwiftUI
import shared

struct ProfileView: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var viewModel = ObservableProfileViewModel()
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Informações Pessoais")) {
                    HStack {
                        Text("Nome")
                        Spacer()
                        Text(viewModel.name)
                            .foregroundColor(.gray)
                    }
                    
                    HStack {
                        Text("Email")
                        Spacer()
                        Text(viewModel.email)
                            .foregroundColor(.gray)
                    }
                }
                
                Section(header: Text("Contato")) {
                    TextField("Telefone", text: $viewModel.phone)
                        .keyboardType(.phonePad)
                    
                    TextField("Contato de Emergência", text: $viewModel.emergencyContact)
                        .keyboardType(.phonePad)
                }
                
                Section(header: Text("Estatísticas")) {
                    HStack {
                        Text("Emergências Criadas")
                        Spacer()
                        Text("\(viewModel.emergenciesCreated)")
                            .foregroundColor(.gray)
                    }
                    
                    HStack {
                        Text("Ajudas Prestadas")
                        Spacer()
                        Text("\(viewModel.helpsProvided)")
                            .foregroundColor(.gray)
                    }
                    
                    if viewModel.averageRating > 0 {
                        HStack {
                            Text("Avaliação")
                            Spacer()
                            HStack(spacing: 2) {
                                ForEach(0..<5) { index in
                                    Image(systemName: index < Int(viewModel.averageRating) ? "star.fill" : "star")
                                        .font(.caption)
                                        .foregroundColor(.yellow)
                                }
                                Text(String(format: "%.1f", viewModel.averageRating))
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                        }
                    }
                }
                
                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                    }
                }
                
                Section {
                    Button(action: {
                        viewModel.saveProfile()
                    }) {
                        if viewModel.isLoading {
                            HStack {
                                Spacer()
                                ProgressView()
                                Spacer()
                            }
                        } else {
                            Text("Salvar Alterações")
                                .frame(maxWidth: .infinity)
                                .foregroundColor(.red)
                        }
                    }
                    .disabled(viewModel.isLoading)
                }
            }
            .navigationTitle("Perfil")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Fechar") {
                        dismiss()
                    }
                }
            }
        }
    }
}

class ObservableProfileViewModel: ObservableObject {
    private let viewModel: ProfileViewModel
    @Published var name = ""
    @Published var email = ""
    @Published var phone = ""
    @Published var emergencyContact = ""
    @Published var emergenciesCreated = 0
    @Published var helpsProvided = 0
    @Published var averageRating = 0.0
    @Published var isLoading = false
    @Published var error: String?
    
    init() {
        viewModel = ViewModelProvider.shared.getProfileViewModel()
        observeState()
        loadProfile()
    }
    
    private func observeState() {
        viewModel.state.watch { [weak self] state in
            guard let state = state as? ProfileState else { return }
            DispatchQueue.main.async {
                if let profile = state.profile {
                    self?.name = profile.name
                    self?.email = profile.email
                    self?.phone = profile.phone
                }
                self?.isLoading = state.isLoading || state.isSaving
                self?.error = state.error
            }
        }
    }
    
    func loadProfile() {
        viewModel.loadProfile()
    }
    
    func saveProfile() {
        // TODO: Update profile with new values
    }
}
