import SwiftUI
import shared

struct ProfileView: View {
    @EnvironmentObject var container: AppContainer
    @State private var showEditSheet = false
    @State private var name = ""
    @State private var phone = ""
    @State private var bloodType = ""
    @State private var allergies = ""
    @State private var medications = ""
    @State private var conditions = ""
    @State private var healthNotes = ""
    @State private var emergencyName = ""
    @State private var emergencyPhone = ""
    @State private var emergencyRelationship = ""
    @State private var fieldsLoaded = false

    var body: some View {
        let state = container.profile.state
        
        ScrollView {
            LazyVStack(spacing: 16) {
                if state == nil || state!.isLoading {
                    LoadingCard()
                } else if let profile = state?.profile {
                    // Hero Section
                    HeroGradientCard {
                        VStack(spacing: 16) {
                            Circle()
                                .fill(Color.white)
                                .frame(width: 80, height: 80)
                                .overlay {
                                    Image(systemName: "person.fill")
                                        .font(.system(size: 32))
                                        .foregroundColor(.afiPrimary)
                                }
                            
                            VStack(spacing: 8) {
                                Text(profile.name) // Using name instead of displayName
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                                
                                HStack(spacing: 8) {
                                    if profile.isHealthProfessional {
                                        StatusBadge(text: "Profissional de Saúde", status: .success)
                                    } else {
                                        StatusBadge(text: "Paciente Verificado", status: .info)
                                    }
                                }
                                
                                Text(profile.email)
                                    .font(.subheadline)
                                    .foregroundColor(.white.opacity(0.8))
                            }
                        }
                    }

                    
                    agendaDeSaudeSection(profile: profile)
                }
                
                if let error = state?.error {
                    ErrorCard(message: error)
                }
                
                if let success = state?.successMessage {
                    AfilaxyCard {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text(success)
                                .foregroundColor(.green)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 100)
        }
        .navigationTitle("Meu Perfil")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showEditSheet) {
            EditProfileSheet(
                name: $name, phone: $phone, bloodType: $bloodType,
                allergies: $allergies, medications: $medications,
                conditions: $conditions, healthNotes: $healthNotes,
                emergencyName: $emergencyName, emergencyPhone: $emergencyPhone,
                emergencyRelationship: $emergencyRelationship,
                onSave: saveProfile
            )
        }
        .onReceive(container.profile.objectWillChange) { _ in
            guard !fieldsLoaded, let profile = container.profile.state?.profile else { return }
            fieldsLoaded = true
            name = profile.name; phone = profile.phone
            bloodType = profile.healthData?.bloodType ?? ""
            allergies = profile.healthData?.allergies.joined(separator: ", ") ?? ""
            medications = profile.healthData?.medications.joined(separator: ", ") ?? ""
            conditions = profile.healthData?.conditions.joined(separator: ", ") ?? ""
            healthNotes = profile.healthData?.notes ?? ""
            emergencyName = profile.emergencyContact?.name ?? ""
            emergencyPhone = profile.emergencyContact?.phone ?? ""
            emergencyRelationship = profile.emergencyContact?.relationship ?? ""
        }
    }

    @ViewBuilder
    private func agendaDeSaudeSection(profile: UserProfile) -> some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 14) {
                // Cabeçalho
                HStack {
                    Image(systemName: "calendar.badge.clock").foregroundColor(.afiPrimary)
                    Text("Agenda de Saúde").font(.headline).fontWeight(.semibold)
                    Spacer()
                    Button { showEditSheet = true } label: {
                        Image(systemName: "pencil.circle").font(.title3).foregroundColor(.afiPrimary)
                    }
                }
                Divider()
                // Dados clínicos
                VStack(spacing: 12) {
                    InfoGridItem(title: "Tipo de Asma", value: profile.healthData?.conditions.first ?? "Não informado", icon: "lungs.fill", accentColor: .afiPrimary)
                    // Medicação
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Medicação Atual").font(.subheadline).fontWeight(.semibold).foregroundColor(.afiPrimary)
                            Spacer()
                            Image(systemName: "pills.fill").foregroundColor(.afiPrimary)
                        }
                        HStack(spacing: 8) {
                            MedicationTypeCard(type: "Controle", count: profile.healthData?.medications.filter { $0.contains("controle") || $0.contains("manutencao") }.count ?? 0)
                            MedicationTypeCard(type: "Resgate",  count: profile.healthData?.medications.filter { $0.contains("resgate")  || $0.contains("bronco")     }.count ?? 0)
                            MedicationTypeCard(type: "Outros",   count: max(0, (profile.healthData?.medications.count ?? 0) - 2))
                        }
                    }
                    .padding(8).background(Color(UIColor.secondarySystemBackground)).cornerRadius(10)
                    // Contato + Protocolo lado a lado
                    HStack(spacing: 12) {
                        InfoGridItem(title: "Contato de Emergência", value: profile.emergencyContact?.name ?? "Não informado", icon: "phone.fill", accentColor: .afiError)
                        NavigationLink(destination: HelpView()) {
                            InfoGridItem(title: "Protocolo de Crise", value: "Ver passos", icon: "list.clipboard.fill", accentColor: .afiWarning)
                        }.buttonStyle(.plain)
                    }
                }

                Divider()
                // Ocorrências recentes
                Text("Ocorrências Recentes").font(.subheadline).fontWeight(.semibold).foregroundColor(.secondary)
                let recent = Array((container.history.state?.filteredHistory ?? []).prefix(3))
                if recent.isEmpty {
                    Text("Nenhuma ocorrência registrada").font(.caption).foregroundColor(.secondary).padding(.vertical, 4)
                } else {
                    ForEach(recent, id: \.id) { item in AgendaHistoryRow(item: item) }
                }
                NavigationLink(value: AppRoute.history) {
                    HStack {
                        Image(systemName: "clock.arrow.circlepath").font(.caption)
                        Text("Ver histórico completo").font(.subheadline)
                        Spacer()
                        Image(systemName: "chevron.right").font(.caption)
                    }.foregroundColor(.afiPrimary).padding(.vertical, 2)
                }
            }
        }
    }

    private func saveProfile() {
        guard let profile = container.profile.state?.profile else { return }
        let updated = profile.doCopy(
            uid: profile.uid, name: name, email: profile.email, phone: phone,
            photoUrl: profile.photoUrl,
            healthData: UserHealthData(
                bloodType: bloodType,
                allergies: split(allergies), medications: split(medications),
                conditions: split(conditions), notes: healthNotes
            ),
            emergencyContact: EmergencyContact(
                name: emergencyName, phone: emergencyPhone, relationship: emergencyRelationship
            ),
            isHealthProfessional: profile.isHealthProfessional
        )
        container.profile.vm?.updateProfile(profile: updated)
        showEditSheet = false
    }

    private func split(_ s: String) -> [String] {
        s.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }
}

struct MedicationTypeCard: View {
    let type: String
    let count: Int
    
    private var color: Color {
        switch type {
        case "Controle": return .afiSuccess
        case "Resgate": return .afiError
        default: return .afiSecondary
        }
    }
    
    var body: some View {
        VStack(spacing: 4) {
            Text("\(count)")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(color)
            
            Text(type)
                .font(.caption2)
                .foregroundColor(.afiTextSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(color.opacity(0.1))
        .cornerRadius(8)
    }
}

struct InfoCard: View {
    let title: String
    let items: [(String, String)]
    
    var body: some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 12) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(AfilaxyColors.primary)
                
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(items, id: \.0) { item in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.0)
                                .font(.caption)
                                .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                            Text(item.1)
                                .font(.subheadline)
                                .fontWeight(.medium)
                        }
                    }
                }
            }
        }
    }
}

struct QuickActionRow: View {
    let icon: String
    let title: String
    let subtitle: String
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(AfilaxyColors.primary)
                .frame(width: 24)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(AfilaxyColors.onSurface.opacity(0.4))
        }
        .padding(.vertical, 4)
    }
}

struct EditProfileSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var name: String
    @Binding var phone: String
    @Binding var bloodType: String
    @Binding var allergies: String
    @Binding var medications: String
    @Binding var conditions: String
    @Binding var healthNotes: String
    @Binding var emergencyName: String
    @Binding var emergencyPhone: String
    @Binding var emergencyRelationship: String
    let onSave: () -> Void
    
    var body: some View {
        NavigationView {
            Form {
                Section("Informações Pessoais") {
                    TextField("Nome completo", text: $name)
                    TextField("Telefone", text: $phone).keyboardType(.phonePad)
                }
                Section("Dados de Saúde") {
                    TextField("Alergias (separadas por vírgula)", text: $allergies)
                    VStack(alignment: .leading, spacing: 4) {
                        TextField("Medicação Atual", text: $medications)
                        Text("Separe por vírgula. Ex: fluticasona controle, salbutamol resgate")
                            .font(.caption2).foregroundColor(.secondary)
                    }
                    TextField("Tipo de Asma / Condições médicas", text: $conditions)
                    TextField("Observações adicionais", text: $healthNotes)
                }

                Section("Contato de Emergência") {
                    TextField("Nome", text: $emergencyName)
                    TextField("Telefone", text: $emergencyPhone).keyboardType(.phonePad)
                    TextField("Parentesco (ex: Mãe)", text: $emergencyRelationship)
                }
            }
            .navigationTitle("Editar Perfil")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancelar") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Salvar") { onSave() }
                        .fontWeight(.semibold)
                }
            }
        }
    }
}

struct AgendaHistoryRow: View {

    let item: EmergencyHistory

    private var statusInfo: (String, Color) {
        switch item.status {
        case "resolved":  return ("Resolvida", .green)
        case "cancelled": return ("Cancelada", .red)
        case "matched":   return ("Em Atendimento", .orange)
        default:          return (item.status, .secondary)
        }
    }

    private var timeString: String {
        let date = Date(timeIntervalSince1970: TimeInterval(item.timestamp / 1000))
        let f = DateFormatter()
        f.dateFormat = "dd/MM HH:mm"
        return f.string(from: date)
    }

    var body: some View {
        HStack(spacing: 10) {
            Circle().fill(statusInfo.1).frame(width: 8, height: 8)
            VStack(alignment: .leading, spacing: 1) {
                Text(statusInfo.0).font(.caption).fontWeight(.medium).foregroundColor(statusInfo.1)
                Text(timeString).font(.caption2).foregroundColor(.secondary)
            }
            Spacer()
        }
    }
}
