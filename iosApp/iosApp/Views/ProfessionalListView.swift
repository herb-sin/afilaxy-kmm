import SwiftUI
import shared

struct ProfessionalListView: View {
    @EnvironmentObject var container: AppContainer

    var body: some View {
        guard let state = container.professionals.state else {
            return AnyView(ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity))
        }
        return AnyView(listBody(state: state))
    }

    @ViewBuilder
    private func listBody(state: ProfessionalListState) -> some View {
        Group {
            if state.isLoading {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = state.error {
                VStack { Image(systemName: "exclamationmark.triangle").font(.largeTitle); Text(error) }
                    .foregroundColor(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if state.professionals.isEmpty {
                VStack { Image(systemName: "person.slash").font(.largeTitle); Text("Nenhum profissional encontrado") }
                    .foregroundColor(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(state.professionals, id: \.id) { p in
                    NavigationLink(value: AppRoute.professionalDetail(p.id)) {
                        ProfessionalRowView(professional: p)
                    }
                }
            }
        }
        .navigationTitle("Profissionais")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button("Todos")           { container.professionals.vm.filterBySpecialty(specialty: nil) }
                    Button("Pneumologistas")  { container.professionals.vm.filterBySpecialty(specialty: .pneumologist) }
                    Button("Alergistas")      { container.professionals.vm.filterBySpecialty(specialty: .allergist) }
                    Button("Fisioterapeutas") { container.professionals.vm.filterBySpecialty(specialty: .physiotherapist) }
                } label: { Image(systemName: "line.3.horizontal.decrease.circle") }
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
                Text(professional.name).font(.headline)
                if professional.subscriptionPlan == .premium {
                    Image(systemName: "checkmark.seal.fill").foregroundColor(.blue).font(.caption)
                }
            }
            Text(specialtyLabel).font(.subheadline).foregroundColor(.secondary)
            Text("CRM: \(professional.crm)").font(.caption).foregroundColor(.secondary)
            if !professional.bio.isEmpty {
                Text(professional.bio).font(.caption).lineLimit(2)
            }
            if let whatsapp = professional.whatsapp {
                Link(destination: URL(string: "https://wa.me/55\(whatsapp)")!) {
                    Label("Entrar em Contato", systemImage: "phone.fill").font(.subheadline)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
