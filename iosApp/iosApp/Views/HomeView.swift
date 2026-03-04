import SwiftUI
import shared

struct HomeView: View {
    let onLogout: () -> Void
    
    @State private var showEmergency = false
    @State private var showSettings = false
    @State private var isHelperMode = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                // Emergency Button
                Button(action: {
                    showEmergency = true
                }) {
                    VStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 64))
                        Text("EMERGÊNCIA")
                            .font(.title2)
                            .fontWeight(.bold)
                    }
                    .frame(width: 200, height: 200)
                    .foregroundColor(.white)
                    .background(Color.red)
                    .cornerRadius(100)
                }
                
                // Helper Mode Toggle
                Toggle(isOn: $isHelperMode) {
                    HStack {
                        Image(systemName: "heart.fill")
                            .foregroundColor(.red)
                        Text("Modo Helper")
                            .fontWeight(.semibold)
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding(.horizontal, 32)
                
                // Menu Grid
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                    MenuCard(icon: "clock.fill", title: "Histórico") {
                        // TODO: Navigate to history
                    }
                    
                    MenuCard(icon: "bell.fill", title: "Notificações") {
                        // TODO: Navigate to notifications
                    }
                    
                    MenuCard(icon: "gearshape.fill", title: "Configurações") {
                        showSettings = true
                    }
                    
                    MenuCard(icon: "info.circle", title: "Sobre") {
                        // TODO: Navigate to about
                    }
                }
                .padding(.horizontal, 32)
                
                Spacer()
            }
            .padding(.top, 32)
            .navigationTitle("Afilaxy")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onLogout) {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
            .sheet(isPresented: $showEmergency) {
                EmergencyView()
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
            }
        }
    }
}

struct MenuCard: View {
    let icon: String
    let title: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 32))
                Text(title)
                    .font(.subheadline)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 100)
            .background(Color(.systemGray6))
            .foregroundColor(.primary)
            .cornerRadius(10)
        }
    }
}
