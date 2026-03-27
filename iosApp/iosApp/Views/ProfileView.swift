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
                                Text(profile.displayName ?? profile.name)
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
                            
                            Button(action: { showEditSheet = true }) {
                                HStack(spacing: 8) {
                                    Image(systemName: "pencil")
                                    Text("Editar Perfil")
                                }
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(.afiPrimary)
                                .padding(.horizontal, 20)
                                .padding(.vertical, 10)
                                .background(Color.white)
                                .clipShape(Capsule())
                            }
                        }
                    }
                    
                    // Bento Grid Layout
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                        // Tipo de Asma Card
                        InfoGridItem(
                            title: "Tipo de Asma",
                            value: profile.healthData?.conditions.first ?? "Não informado",
                            icon: "lungs.fill",
                            accentColor: .afiPrimary
                        )
                        
                        // Último Exame Card
                        InfoGridItem(
                            title: "Último Exame",
                            value: profile.healthData?.notes ?? "Nenhum registro",
                            icon: "stethoscope",
                            accentColor: .afiTertiary
                        )
                        
                        // Medicamentos Card (span 2 columns)
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Medicação Atual")
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.afiPrimary)
                                
                                Spacer()
                                
                                Image(systemName: "pills.fill")
                                    .foregroundColor(.afiPrimary)
                            }
                            
                            HStack(spacing: 8) {
                                MedicationTypeCard(type: "Controle", count: profile.healthData?.medications.filter { $0.contains("controle") || $0.contains("manutencao") }.count ?? 0)
                                MedicationTypeCard(type: "Resgate", count: profile.healthData?.medications.filter { $0.contains("resgate") || $0.contains("bronco") }.count ?? 0)
                                MedicationTypeCard(type: "Outros", count: max(0, (profile.healthData?.medications.count ?? 0) - 2))
                            }
                        }
                        .padding()
                        .background(.afiCardBackground)
                        .cornerRadius(12)
                        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
                        .gridCellColumns(2)
                        
                        // Contato de Emergência Card
                        InfoGridItem(
                            title: "Contato de Emergência",
                            value: profile.emergencyContact?.name ?? "Não informado",
                            icon: "phone.fill",
                            accentColor: .afiError
                        )
                        
                        // Protocolo Card
                        InfoGridItem(
                            title: "Protocolo de Crise",
                            value: "3 passos definidos",
                            icon: "list.clipboard.fill",
                            accentColor: .afiWarning
                        )
                    }
                    
                    // Quick Actions
                    AfilaxyCard {
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Ações Rápidas")
                                .font(.headline)
                                .fontWeight(.semibold)
                            
                            VStack(spacing: 12) {
                                QuickActionRow(icon: "heart.text.square", title: "Histórico Médico", subtitle: "Ver registros de saúde")
                                QuickActionRow(icon: "bell", title: "Notificações", subtitle: "Gerenciar alertas")
                                QuickActionRow(icon: "shield", title: "Privacidade", subtitle: "Configurações de dados")
                            }
                        }
                    }
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
        .onReceive(container.profile.$state) { s in
            guard !fieldsLoaded, let p = s?.profile else { return }
            fieldsLoaded = true
            name = p.name; phone = p.phone
            bloodType = p.healthData?.bloodType ?? ""
            allergies = p.healthData?.allergies.joined(separator: ", ") ?? ""
            medications = p.healthData?.medications.joined(separator: ", ") ?? ""
            conditions = p.healthData?.conditions.joined(separator: ", ") ?? ""
            healthNotes = p.healthData?.notes ?? ""
            emergencyName = p.emergencyContact?.name ?? ""
            emergencyPhone = p.emergencyContact?.phone ?? ""
            emergencyRelationship = p.emergencyContact?.relationship ?? ""
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
        container.profile.vm.updateProfile(profile: updated)
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
                    TextField("Tipo Sanguíneo (ex: O+)", text: $bloodType)
                    TextField("Alergias (separadas por vírgula)", text: $allergies)
                    TextField("Medicamentos em uso", text: $medications)
                    TextField("Condições médicas", text: $conditions)
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
