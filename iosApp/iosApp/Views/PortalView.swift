import SwiftUI
import shared

struct PortalView: View {
    @EnvironmentObject var container: AppContainer
    
    var body: some View {
        let isHealthProfessional = container.profile.state?.profile?.isHealthProfessional == true
        
        NavigationView {
            if isHealthProfessional {
                ProfessionalDashboardView()
            } else {
                ProfessionalListView()
            }
        }
        .navigationTitle("Portal")
        .navigationBarTitleDisplayMode(.large)
    }
}

struct ProfessionalDashboardView: View {
    @EnvironmentObject var container: AppContainer
    
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 20) {
                // Hero Section
                HeroGradientCard {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image(systemName: "stethoscope")
                                .font(.title2)
                                .foregroundColor(.white)
                            
                            Text("Dashboard Profissional")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                            
                            Spacer()
                        }
                        
                        Text("Gerencie seus pacientes e acompanhe métricas importantes")
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.9))
                    }
                }
                
                // Metrics Grid
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
                    MetricCard(
                        title: "Total Pacientes",
                        value: "24",
                        subtitle: "+3 este mês",
                        icon: "person.2.fill",
                        accentColor: .afiPrimary,
                        borderPosition: .leading
                    )
                    
                    MetricCard(
                        title: "Alertas Críticos",
                        value: "2",
                        subtitle: "Requer atenção",
                        icon: "exclamationmark.triangle.fill",
                        accentColor: .afiError,
                        borderPosition: .leading
                    )
                    
                    MetricCard(
                        title: "Taxa de Adesão",
                        value: "87%",
                        subtitle: "↑ 5% vs mês anterior",
                        icon: "chart.line.uptrend.xyaxis",
                        accentColor: .afiSuccess,
                        borderPosition: .leading
                    )
                    
                    MetricCard(
                        title: "Teleconsultas",
                        value: "12",
                        subtitle: "Esta semana",
                        icon: "video.fill",
                        accentColor: .afiTertiary,
                        borderPosition: .leading
                    )
                }
                
                // Recent Patients
                AfilaxyCard {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            Text("Pacientes Recentes")
                                .font(.headline)
                                .foregroundColor(.afiTextPrimary)
                            
                            Spacer()
                            
                            Button("Ver Todos") {
                                // TODO: Navigate to full patient list
                            }
                            .font(.subheadline)
                            .foregroundColor(.afiPrimary)
                        }
                        
                        VStack(spacing: 12) {
                            PatientRow(
                                name: "Maria Silva",
                                condition: "Asma Controlada",
                                adherence: 0.9,
                                status: .success
                            )
                            
                            PatientRow(
                                name: "João Santos",
                                condition: "Asma Parcialmente Controlada",
                                adherence: 0.7,
                                status: .warning
                            )
                            
                            PatientRow(
                                name: "Ana Costa",
                                condition: "Asma Não Controlada",
                                adherence: 0.4,
                                status: .error
                            )
                        }
                    }
                }
                
                // Critical Alerts
                AfilaxyCard(backgroundColor: .afiErrorContainer) {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.afiError)
                            
                            Text("Alertas Críticos")
                                .font(.headline)
                                .foregroundColor(.afiOnErrorContainer)
                            
                            Spacer()
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            AlertRow(
                                patient: "Ana Costa",
                                message: "Uso excessivo de broncodilatador",
                                time: "2h atrás"
                            )
                            
                            AlertRow(
                                patient: "Pedro Lima",
                                message: "Não usa medicação há 3 dias",
                                time: "5h atrás"
                            )
                        }
                    }
                }
            }
            .padding()
        }
        .background(Color.afiBackground)
        .navigationTitle("Dashboard")
        .navigationBarTitleDisplayMode(.large)
    }
}

struct PatientRow: View {
    let name: String
    let condition: String
    let adherence: Double
    let status: StatusBadge.BadgeStatus
    
    var body: some View {
        HStack(spacing: 12) {
            // Avatar placeholder
            Circle()
                .fill(Color.afiPrimary.opacity(0.2))
                .frame(width: 40, height: 40)
                .overlay(
                    Text(String(name.prefix(1)))
                        .font(.headline)
                        .foregroundColor(.afiPrimary)
                )
            
            VStack(alignment: .leading, spacing: 2) {
                Text(name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.afiTextPrimary)
                
                Text(condition)
                    .font(.caption)
                    .foregroundColor(.afiTextSecondary)
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 4) {
                StatusBadge(
                    text: "\(Int(adherence * 100))%",
                    status: status
                )
                
                // Adherence bar
                ProgressView(value: adherence)
                    .progressViewStyle(LinearProgressViewStyle(tint: status.color))
                    .frame(width: 60)
            }
        }
        .padding(.vertical, 4)
    }
}

struct AlertRow: View {
    let patient: String
    let message: String
    let time: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Rectangle()
                .fill(Color.afiError)
                .frame(width: 3, height: 40)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(patient)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.afiOnErrorContainer)
                
                Text(message)
                    .font(.caption)
                    .foregroundColor(.afiOnErrorContainer.opacity(0.8))
                
                Text(time)
                    .font(.caption2)
                    .foregroundColor(.afiOnErrorContainer.opacity(0.6))
            }
            
            Spacer()
        }
    }
}

#if DEBUG
struct PortalView_Previews: PreviewProvider {
    static var previews: some View {
        PortalView()
            .environmentObject(AppContainer())
    }
}
#endif