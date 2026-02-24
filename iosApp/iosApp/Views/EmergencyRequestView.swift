import SwiftUI
import shared

struct EmergencyRequestView: View {
    @Environment(\.dismiss) var dismiss
    let emergencyId: String
    @StateObject private var viewModel: ObservableEmergencyViewModel
    @State private var showChat = false
    
    init(emergencyId: String, viewModel: ObservableEmergencyViewModel) {
        self.emergencyId = emergencyId
        _viewModel = StateObject(wrappedValue: viewModel)
    }
    
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
