import SwiftUI
import shared

struct EmergencyView: View {
    @Environment(\.dismiss) var dismiss
    @State private var description = ""
    @State private var isLoading = false
    @State private var error: String?
    
    @State private var viewModel = ViewModelProvider.shared.getEmergencyViewModel()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.red)
                
                Text("Descreva a Emergência")
                    .font(.title2)
                    .fontWeight(.bold)
                
                TextEditor(text: $description)
                    .frame(height: 150)
                    .padding(8)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                    )
                
                if let error = error {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }
                
                Button(action: createEmergency) {
                    if isLoading {
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
                .disabled(isLoading || description.isEmpty)
                
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
        }
    }
    
    private func createEmergency() {
        isLoading = true
        error = nil
        viewModel.onCreateEmergency()
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isLoading = false
            dismiss()
        }
    }
}
