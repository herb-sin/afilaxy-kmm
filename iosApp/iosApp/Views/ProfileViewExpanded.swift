import SwiftUI
import shared

struct ProfileViewExpanded: View {
    @StateObject private var viewModel = ViewModelProvider.shared.medicalProfileViewModel
    
    var body: some View {
        NavigationView {
            ScrollView {
                LazyVStack(spacing: 16) {
                    // Patient Profile Header
                    PatientProfileCardView(
                        profile: viewModel.state.medicalProfile
                    ) {
                        // Edit profile action
                    }
                    
                    // Asma Type Info
                    if let profile = viewModel.state.medicalProfile {
                        AsmaTypeCardView(profile: profile)
                    }
                    
                    // Last Exam
                    if let exam = viewModel.state.medicalProfile?.lastExam {
                        LastExamCardView(exam: exam)
                    }
                    
                    // Emergency Protocol
                    EmergencyProtocolCardView()
                    
                    // Emergency Contacts
                    EmergencyContactsCardView(
                        contacts: viewModel.state.emergencyContacts
                    ) {
                        // Add contact action
                    }
                    
                    // Medications
                    MedicationsCardView(
                        medications: viewModel.state.medications
                    ) {
                        // Add medication action
                    }
                }
                .padding(16)
            }
            .navigationTitle("Perfil")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

struct PatientProfileCardView: View {
    let profile: MedicalProfile?
    let onEditProfile: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Badge
            HStack {
                Text("PACIENTE VERIFICADO")
                    .font(.caption2)
                    .fontWeight(.medium)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.blue.opacity(0.1))
                    .foregroundColor(.blue)
                    .cornerRadius(4)
                
                Spacer()
            }
            
            // Name
            Text("Alex Johnson")
                .font(.title)
                .fontWeight(.bold)
            
            // Description
            Text("Gerenciando asma intermitente com foco em prevenção e qualidade de vida ativa.")
                .font(.body)
                .foregroundColor(.secondary)
                .lineLimit(nil)
            
            // Edit Button
            Button(action: onEditProfile) {
                HStack {
                    Image(systemName: "pencil")
                        .font(.system(size: 16))
                    Text("Editar Perfil")
                        .fontWeight(.medium)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.blue)
                .foregroundColor(.white)
                .cornerRadius(8)
            }
        }
        .padding(24)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
}

struct AsmaTypeCardView: View {
    let profile: MedicalProfile
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "heart.fill")
                    .foregroundColor(.blue)
                    .font(.system(size: 16))
                
                Text("TIPO DE ASMA")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.blue)
            }
            
            Text(profile.asmaType.displayName)
                .font(.title2)
                .fontWeight(.bold)
            
            Text("Sintomas leves ocorrendo menos de duas vezes por semana.")
                .font(.body)
                .foregroundColor(.secondary)
            
            // Status Bar
            HStack(spacing: 8) {
                Text("Status: Estável")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.green)
                
                ProgressView(value: 0.8)
                    .progressViewStyle(LinearProgressViewStyle(tint: .green))
                    .frame(height: 4)
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
}

struct LastExamCardView: View {
    let exam: MedicalExam
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "doc.text.fill")
                    .foregroundColor(.blue)
                    .font(.system(size: 16))
                
                Text("ÚLTIMO EXAME")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.blue)
            }
            
            Text(exam.type.displayName)
                .font(.title2)
                .fontWeight(.bold)
            
            Text("Realizado em \\(exam.date). \\(exam.results)")
                .font(.body)
                .foregroundColor(.secondary)
            
            Button("Ver histórico →") {
                // Navigate to history
            }
            .foregroundColor(.blue)
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
}

struct EmergencyProtocolCardView: View {
    let steps = [
        "Sentar em posição vertical e tentar manter a calma.",
        "Usar inalador de resgate (Salbutamol): 2 jatos.",
        "Se não houver melhora em 10 min, ligar para emergência."
    ]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(.red)
                    .font(.system(size: 16))
                
                Text("PROTOCOLO DE EMERGÊNCIA")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.red)
            }
            
            ForEach(Array(steps.enumerated()), id: \.offset) { index, step in
                HStack(alignment: .top, spacing: 12) {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 24, height: 24)
                        .overlay(
                            Text("\\(index + 1)")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                        )
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("PASSO \\(index + 1)")
                            .font(.caption2)
                            .fontWeight(.medium)
                            .foregroundColor(.red)
                        
                        Text(step)
                            .font(.body)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .padding(16)
        .background(Color.red.opacity(0.05))
        .cornerRadius(12)
    }
}

struct EmergencyContactsCardView: View {
    let contacts: [MedicalEmergencyContact]
    let onAddContact: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 8) {
                    Image(systemName: "person.2.fill")
                        .foregroundColor(.blue)
                        .font(.system(size: 16))
                    
                    Text("CONTATOS")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.blue)
                }
                
                Spacer()
                
                Button(action: onAddContact) {
                    Image(systemName: "plus")
                        .foregroundColor(.blue)
                }
            }
            
            ForEach(contacts, id: \.id) { contact in
                ContactItemView(contact: contact)
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
}

struct ContactItemView: View {
    let contact: MedicalEmergencyContact
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: contact.isProfessional ? "cross.fill" : "person.fill")
                .foregroundColor(.blue)
                .font(.system(size: 20))
            
            VStack(alignment: .leading, spacing: 2) {
                Text(contact.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text("\\(contact.relationship) • \\(contact.phone)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
    }
}

struct MedicationsCardView: View {
    let medications: [Medication]
    let onAddMedication: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Medicação Atual")
                        .font(.headline)
                        .fontWeight(.bold)
                    
                    Text("Cronograma diário e medicamentos de alívio rápido.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Button("Histórico de Uso") {
                    // Navigate to history
                }
                .foregroundColor(.blue)
                .font(.caption)
            }
            
            ForEach(medications, id: \.id) { medication in
                MedicationItemView(medication: medication)
            }
            
            Button(action: onAddMedication) {
                HStack {
                    Image(systemName: "plus")
                        .font(.system(size: 16))
                    Text("Adicionar Medicação")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.clear)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.blue, lineWidth: 1)
                )
                .foregroundColor(.blue)
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
}

struct MedicationItemView: View {
    let medication: Medication
    
    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(medication.type.color.opacity(0.1))
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: medication.type.iconName)
                        .foregroundColor(medication.type.color)
                        .font(.system(size: 16))
                )
            
            VStack(alignment: .leading, spacing: 2) {
                Text(medication.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text("\\(medication.dosage) • \\(medication.frequency)\\(medication.timing.map { " (\\($0))" } ?? "")")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Text(medication.type.displayName)
                .font(.caption2)
                .fontWeight(.bold)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(medication.type.color)
                .foregroundColor(.white)
                .cornerRadius(4)
        }
    }
}

// Extensions for display
extension AsmaType {
    var displayName: String {
        switch self {
        case .intermitente: return "Intermitente"
        case .persistenteLeve: return "Persistente Leve"
        case .persistenteModerada: return "Persistente Moderada"
        case .persistenteGrave: return "Persistente Grave"
        }
    }
}

extension ExamType {
    var displayName: String {
        switch self {
        case .espirometria: return "Espirometria"
        case .raioX: return "Raio-X"
        case .testeAlergia: return "Teste de Alergia"
        case .consultaRotina: return "Consulta de Rotina"
        }
    }
}

extension MedicationType {
    var displayName: String {
        switch self {
        case .controle: return "CONTROLE"
        case .manutencao: return "MANUTENÇÃO"
        case .resgate: return "RESGATE"
        }
    }
    
    var color: Color {
        switch self {
        case .controle: return .blue
        case .manutencao: return .cyan
        case .resgate: return .red
        }
    }
    
    var iconName: String {
        switch self {
        case .controle: return "link"
        case .manutencao: return "arrow.clockwise"
        case .resgate: return "bolt.fill"
        }
    }
}

#Preview {
    ProfileViewExpanded()
}