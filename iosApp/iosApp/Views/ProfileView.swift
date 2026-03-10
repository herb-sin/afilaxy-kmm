import SwiftUI
import shared

struct ProfileView: View {
    @EnvironmentObject var container: AppContainer
    @State private var vmState: ProfileState? = nil

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

    var body: some View {
        Form {
            if vmState?.isLoading == true {
                ProgressView()
            }

            if let msg = vmState?.successMessage {
                Section { Text(msg).foregroundColor(.green) }
            }
            if let err = vmState?.error {
                Section { Text(err).foregroundColor(.red) }
            }

            Section("Informações Pessoais") {
                TextField("Nome completo", text: $name)
                TextField("Telefone", text: $phone)
                    .keyboardType(.phonePad)
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
                TextField("Telefone", text: $emergencyPhone)
                    .keyboardType(.phonePad)
                TextField("Parentesco (ex: Mãe)", text: $emergencyRelationship)
            }

            Section {
                Button(action: saveProfile) {
                    if vmState?.isSaving == true {
                        ProgressView()
                    } else {
                        Text("Salvar Alterações")
                            .frame(maxWidth: .infinity)
                    }
                }
                .disabled(vmState?.isSaving == true)
            }
        }
        .navigationTitle("Meu Perfil")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            for await state in container.profileViewModel.state {
                vmState = state
                if let p = state.profile {
                    name = p.name
                    phone = p.phone
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
        }
    }

    private func saveProfile() {
        guard let profile = vmState?.profile else { return }
        let updated = profile.doCopy(
            uid: profile.uid,
            name: name,
            email: profile.email,
            phone: phone,
            photoUrl: profile.photoUrl,
            healthData: UserHealthData(
                bloodType: bloodType,
                allergies: split(allergies),
                medications: split(medications),
                conditions: split(conditions),
                notes: healthNotes
            ),
            emergencyContact: EmergencyContact(
                name: emergencyName,
                phone: emergencyPhone,
                relationship: emergencyRelationship
            )
        )
        container.profileViewModel.updateProfile(profile: updated)
    }

    private func split(_ s: String) -> [String] {
        s.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }
}
