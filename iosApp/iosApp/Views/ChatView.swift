import SwiftUI
import shared

struct ChatView: View {
    @Environment(\.dismiss) var dismiss
    let emergencyId: String
    @StateObject private var viewModel: ObservableChatViewModel
    @State private var showResolveDialog = false
    
    init(emergencyId: String) {
        self.emergencyId = emergencyId
        _viewModel = StateObject(wrappedValue: ObservableChatViewModel(emergencyId: emergencyId))
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Messages List
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.messages, id: \.timestamp) { message in
                            MessageBubble(
                                message: message,
                                currentUserId: viewModel.currentUserId
                            )
                        }
                    }
                    .padding()
                }
                
                Divider()
                
                // Input Bar
                HStack(spacing: 12) {
                    TextField("Mensagem", text: $viewModel.message)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                    
                    Button(action: {
                        viewModel.sendMessage()
                    }) {
                        Image(systemName: "paperplane.fill")
                            .foregroundColor(.white)
                            .padding(10)
                            .background(Color.red)
                            .clipShape(Circle())
                    }
                    .disabled(viewModel.message.isEmpty)
                }
                .padding()
            }
            .navigationTitle("Chat")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Fechar") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Resolver") {
                        showResolveDialog = true
                    }
                    .foregroundColor(.green)
                }
            }
            .alert("Resolver Emergência", isPresented: $showResolveDialog) {
                Button("Cancelar", role: .cancel) { }
                Button("Sim", role: .destructive) {
                    viewModel.resolveEmergency()
                    dismiss()
                }
            } message: {
                Text("A emergência foi resolvida?")
            }
        }
    }
}

struct MessageBubble: View {
    let message: ChatMessage
    let currentUserId: String?
    
    var isFromCurrentUser: Bool {
        message.senderId == currentUserId
    }
    
    var body: some View {
        HStack {
            if isFromCurrentUser {
                Spacer()
            }
            
            VStack(alignment: isFromCurrentUser ? .trailing : .leading, spacing: 4) {
                Text(message.senderName)
                    .font(.caption)
                    .foregroundColor(.gray)
                
                Text(message.message)
                    .padding(12)
                    .background(isFromCurrentUser ? Color.red : Color(.systemGray5))
                    .foregroundColor(isFromCurrentUser ? .white : .primary)
                    .cornerRadius(16)
            }
            
            if !isFromCurrentUser {
                Spacer()
            }
        }
    }
}

class ObservableChatViewModel: ObservableObject {
    private let viewModel: ChatViewModel
    @Published var messages: [ChatMessage] = []
    @Published var message = ""
    @Published var isLoading = false
    @Published var currentUserId: String?
    
    init(emergencyId: String) {
        viewModel = ViewModelProvider.shared.getChatViewModel(emergencyId: emergencyId)
        observeState()
    }
    
    private func observeState() {
        viewModel.state.watch { [weak self] state in
            guard let state = state as? ChatState else { return }
            DispatchQueue.main.async {
                self?.messages = state.messages
                self?.message = state.message
                self?.isLoading = state.isLoading
                self?.currentUserId = state.currentUserId
            }
        }
    }
    
    func sendMessage() {
        viewModel.sendMessage(message: message)
    }
    
    func resolveEmergency() {
        viewModel.resolveEmergency()
    }
}

