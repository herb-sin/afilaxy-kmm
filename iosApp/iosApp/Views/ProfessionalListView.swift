import SwiftUI
import shared

struct ProfessionalListView: View {
    @EnvironmentObject var container: AppContainer
    @State private var vmState: ProfessionalListState? = nil

    var body: some View {
        Group {
            if vmState?.isLoading == true {
                ProgressView()
            } else if let error = vmState?.error {
                ContentUnavailableView(error, systemImage: "exclamationmark.triangle")
            } else if vmState?.professionals.isEmpty != false {
                ContentUnavailableView("Nenhum profissional encontrado", systemImage: "person.slash")
            } else {
                List(vmState?.professionals ?? [], id: \.id) { professional in
                    ProfessionalRowView(professional: professional)
                }
            }
        }
        .navigationTitle("Profissionais")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button("Todos") { container.professionalListViewModel.filterBySpecialty(specialty: nil) }
                    Button("Pneumologistas") { container.professionalListViewModel.filterBySpecialty(specialty: .pneumologist) }
                    Button("Alergistas") { container.professionalListViewModel.filterBySpecialty(specialty: .allergist) }
                    Button("Fisioterapeutas") { container.professionalListViewModel.filterBySpecialty(specialty: .physiotherapist) }
                } label: {
                    Image(systemName: "line.3.horizontal.decrease.circle")
                }
            }
        }
        .task {
            for await state in container.professionalListViewModel.state {
                vmState = state
            }
        }
    }
}

private struct ProfessionalRowView: View {
    let professional: HealthProfessional

    var specialtyLabel: String {
        switch professional.specialty {
        case .pneumologist:    return "Pneumologista"
        case .allergist:       return "Alergista"
        case .physiotherapist: return "Fisioterapeuta"
        default:               return ""
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(professional.name)
                    .font(.headline)
                if professional.subscriptionPlan == .premium {
                    Image(systemName: "checkmark.seal.fill")
                        .foregroundColor(.blue)
                        .font(.caption)
                }
            }
            Text(specialtyLabel)
                .font(.subheadline)
                .foregroundColor(.secondary)
            Text("CRM: \(professional.crm)")
                .font(.caption)
                .foregroundColor(.secondary)
            if !professional.bio.isEmpty {
                Text(professional.bio)
                    .font(.caption)
                    .lineLimit(2)
            }
            if let whatsapp = professional.whatsapp {
                Link(destination: URL(string: "https://wa.me/55\(whatsapp)")!) {
                    Label("Entrar em Contato", systemImage: "phone.fill")
                        .font(.subheadline)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
