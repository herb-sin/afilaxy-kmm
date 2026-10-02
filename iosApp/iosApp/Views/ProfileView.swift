import SwiftUI
import shared
import FirebaseAuth

struct ProfileView: View {
    @EnvironmentObject var container: AppContainer
    @AppStorage("theme_preference") private var themePreference: String = "system"
    @State private var showEditSheet = false
    @State private var name = ""
    @State private var phone = ""
    @State private var bloodType = ""
    @State private var allergies = ""
    @State private var healthNotes = ""
    @State private var emergencyName = ""
    @State private var emergencyPhone = ""
    @State private var emergencyRelationship = ""
    @State private var hasGerd = false
    @State private var hasSleepApnea = false
    @State private var hasRhinitis = false
    @State private var hasObesity = false
    @State private var hasFoodAllergy = false
    @State private var hasNsaidAllergy = false
    @State private var hasInhalantAllergy = false
    @State private var hasWheelchair = false
    @State private var hasLowVision = false
    @State private var hasSpecialCondition = false
    @State private var showPhotoComingSoon = false

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
                            ZStack(alignment: .bottomTrailing) {
                                Circle()
                                    .fill(Color.white)
                                    .frame(width: 80, height: 80)
                                    .overlay {
                                        Image(systemName: "person.fill")
                                            .font(.system(size: 32))
                                            .foregroundColor(.afiPrimary)
                                    }
                                Button(action: { showPhotoComingSoon = true }) {
                                    Circle()
                                        .fill(Color.white)
                                        .frame(width: 28, height: 28)
                                        .overlay {
                                            Image(systemName: "camera.fill")
                                                .font(.system(size: 13))
                                                .foregroundColor(.afiPrimary)
                                        }
                                        .shadow(color: .black.opacity(0.15), radius: 2, x: 0, y: 1)
                                }
                                .accessibilityLabel("Alterar foto do perfil")
                            }

                            VStack(spacing: 8) {
                                Text(profile.name) // Using name instead of displayName
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                                
                                if profile.isHealthProfessional {
                                    StatusBadge(text: "Profissional de Saúde", status: .success)
                                }
                                
                                Text(profile.email)
                                    .font(.subheadline)
                                    .foregroundColor(.white.opacity(0.8))
                            }
                        }
                    }

                    
                    agendaDeSaudeSection(profile: profile)
                }

                themeCard

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
        .onAppear {
            // ProfileViewModel.init() runs before Firebase Auth restores the session on iOS.
            // Force a reload here — auth is always ready when a tab's view appears.
            if container.profile.state?.profile == nil {
                container.profile.vm?.loadProfile()
            }
        }
        .sheet(isPresented: $showEditSheet) {
            EditProfileSheet(
                name: $name, phone: $phone,
                allergies: $allergies, healthNotes: $healthNotes,
                emergencyName: $emergencyName, emergencyPhone: $emergencyPhone,
                emergencyRelationship: $emergencyRelationship,
                hasGerd: $hasGerd, hasSleepApnea: $hasSleepApnea, hasRhinitis: $hasRhinitis,
                hasObesity: $hasObesity,
                hasFoodAllergy: $hasFoodAllergy, hasNsaidAllergy: $hasNsaidAllergy,
                hasInhalantAllergy: $hasInhalantAllergy,
                hasWheelchair: $hasWheelchair, hasLowVision: $hasLowVision,
                hasSpecialCondition: $hasSpecialCondition,
                onSave: saveProfile
            )
        }
        .alert("Upload de foto em breve", isPresented: $showPhotoComingSoon) {
            Button("OK", role: .cancel) {}
        }
    }

    @ViewBuilder
    private var themeCard: some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 8) {
                    Image(systemName: "paintbrush.fill").foregroundColor(.afiPrimary)
                    Text("Aparência").font(.headline).fontWeight(.semibold)
                }
                Divider()
                HStack(spacing: 8) {
                    ForEach([("system", "Sistema"), ("light", "Claro"), ("dark", "Escuro")], id: \.0) { value, label in
                        Button(action: { themePreference = value }) {
                            Text(label)
                                .font(.subheadline)
                                .fontWeight(themePreference == value ? .semibold : .regular)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                                .background(themePreference == value ? Color.afiPrimary : Color(UIColor.secondarySystemBackground))
                                .foregroundColor(themePreference == value ? .white : .primary)
                                .cornerRadius(8)
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func agendaDeSaudeSection(profile: UserProfile) -> some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 14) {
                // Cabeçalho
                HStack {
                    Image(systemName: "calendar.badge.clock").foregroundColor(.afiPrimary)
                    Text("Meu Perfil").font(.headline).fontWeight(.semibold)
                    Spacer()
                    Button {
                        populateFields(from: profile)
                        showEditSheet = true
                    } label: {
                        Image(systemName: "pencil.circle").font(.title3).foregroundColor(.afiPrimary)
                    }
                }
                Divider()
                VStack(spacing: 12) {
                    // Contato + Protocolo lado a lado
                    HStack(spacing: 12) {
                        let contactValue: String = {
                            let name = profile.emergencyContact?.name ?? ""
                            let phone = profile.emergencyContact?.phone ?? ""
                            if !name.isEmpty && !phone.isEmpty { return "\(name)\n\(phone)" }
                            if !name.isEmpty { return name }
                            if !phone.isEmpty { return phone }
                            return "Não informado"
                        }()
                        InfoGridItem(title: "Contato de Emergência", value: contactValue, icon: "phone.fill", accentColor: .afiError)
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

    // Preenche os campos de edição a partir do perfil atual — chamado de forma
    // determinística ao abrir o formulário (não depende de reagir a um evento
    // assíncrono que pode não chegar a tempo, como a atualização pós-salvamento).
    private func populateFields(from profile: UserProfile) {
        name = profile.name; phone = profile.phone
        bloodType = profile.healthData?.bloodType ?? ""
        allergies = profile.healthData?.allergies.joined(separator: ", ") ?? ""
        healthNotes = profile.healthData?.notes ?? ""
        emergencyName = profile.emergencyContact?.name ?? ""
        emergencyPhone = profile.emergencyContact?.phone ?? ""
        emergencyRelationship = profile.emergencyContact?.relationship ?? ""
        hasGerd = profile.healthData?.hasGerd ?? false
        hasSleepApnea = profile.healthData?.hasSleepApnea ?? false
        hasRhinitis = profile.healthData?.hasRhinitis ?? false
        hasObesity = profile.healthData?.hasObesity ?? false
        hasFoodAllergy = profile.healthData?.hasFoodAllergy ?? false
        hasNsaidAllergy = profile.healthData?.hasNsaidAllergy ?? false
        hasInhalantAllergy = profile.healthData?.hasInhalantAllergy ?? false
        hasWheelchair = profile.healthData?.hasWheelchair ?? false
        hasLowVision = profile.healthData?.hasLowVision ?? false
        hasSpecialCondition = profile.healthData?.hasSpecialCondition ?? false
    }

    private func saveProfile() {
        // Obtém o perfil existente OU constrói um mínimo para contas novas
        // onde o documento Firestore ainda não foi criado (profile == nil).
        let existingProfile = container.profile.state?.profile
        let currentUid = existingProfile?.uid ?? Auth.auth().currentUser?.uid ?? ""
        let currentEmail = existingProfile?.email ?? Auth.auth().currentUser?.email ?? ""

        guard !currentUid.isEmpty else {
            // Sem UID não há como salvar — não deveria ocorrer se o usuário está autenticado
            return
        }

        let updated: UserProfile
        let safeName = name.trimmingCharacters(in: .whitespaces).isEmpty
            ? (existingProfile?.name ?? name)
            : name.trimmingCharacters(in: .whitespaces)

        if let profile = existingProfile {
            updated = profile.doCopy(
                uid: profile.uid, name: safeName, email: profile.email, phone: phone,
                photoUrl: profile.photoUrl,
                healthData: UserHealthData(
                    bloodType: bloodType,
                    allergies: split(allergies),
                    medications: profile.healthData?.medications ?? [],
                    conditions: profile.healthData?.conditions ?? [],
                    notes: healthNotes,
                    hasGerd: hasGerd, hasSleepApnea: hasSleepApnea, hasRhinitis: hasRhinitis,
                    hasObesity: hasObesity,
                    hasFoodAllergy: hasFoodAllergy, hasNsaidAllergy: hasNsaidAllergy,
                    hasInhalantAllergy: hasInhalantAllergy,
                    hasWheelchair: hasWheelchair, hasLowVision: hasLowVision,
                    hasSpecialCondition: hasSpecialCondition
                ),
                emergencyContact: EmergencyContact(
                    name: emergencyName, phone: emergencyPhone, relationship: emergencyRelationship
                ),
                isHealthProfessional: profile.isHealthProfessional
            )
        } else {
            // Conta nova: cria perfil com os dados preenchidos agora
            updated = UserProfile(
                uid: currentUid,
                name: safeName.isEmpty ? (Auth.auth().currentUser?.displayName ?? "") : safeName,
                email: currentEmail,
                phone: phone,
                photoUrl: nil,
                healthData: UserHealthData(
                    bloodType: bloodType,
                    allergies: split(allergies), medications: [], conditions: [], notes: healthNotes,
                    hasGerd: hasGerd, hasSleepApnea: hasSleepApnea, hasRhinitis: hasRhinitis,
                    hasObesity: hasObesity,
                    hasFoodAllergy: hasFoodAllergy, hasNsaidAllergy: hasNsaidAllergy,
                    hasInhalantAllergy: hasInhalantAllergy,
                    hasWheelchair: hasWheelchair, hasLowVision: hasLowVision,
                    hasSpecialCondition: hasSpecialCondition
                ),
                emergencyContact: EmergencyContact(
                    name: emergencyName, phone: emergencyPhone, relationship: emergencyRelationship
                ),
                isHealthProfessional: false
            )
        }

        container.profile.vm?.updateProfile(profile: updated)
        showEditSheet = false
    }

    private func split(_ s: String) -> [String] {
        s.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
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
    @Binding var name: String
    @Binding var phone: String
    @Binding var allergies: String
    @Binding var healthNotes: String
    @Binding var emergencyName: String
    @Binding var emergencyPhone: String
    @Binding var emergencyRelationship: String
    @Binding var hasGerd: Bool
    @Binding var hasSleepApnea: Bool
    @Binding var hasRhinitis: Bool
    @Binding var hasObesity: Bool
    @Binding var hasFoodAllergy: Bool
    @Binding var hasNsaidAllergy: Bool
    @Binding var hasInhalantAllergy: Bool
    @Binding var hasWheelchair: Bool
    @Binding var hasLowVision: Bool
    @Binding var hasSpecialCondition: Bool
    let onSave: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 12) {
                    EditProfileSection(icon: "person.fill", title: "Informações Pessoais") {
                        FloatingLabelField(label: "Nome completo", text: $name)
                        FloatingLabelField(label: "Telefone", text: $phone, keyboardType: .phonePad)
                    }

                    EditProfileSection(icon: "cross.case.fill", title: "Comorbidades (opcional)") {
                        ProfileInfoBox("Marque as que se aplicam. Usado só para calcular seu nível de risco com mais precisão, nunca compartilhado.")
                        Toggle("Refluxo / DRGE", isOn: $hasGerd)
                        Toggle("Apneia do sono", isOn: $hasSleepApnea)
                        Toggle("Rinite alérgica", isOn: $hasRhinitis)
                        Toggle("Obesidade", isOn: $hasObesity)
                        Divider()
                        HStack(spacing: 6) {
                            Image(systemName: "lock.fill").font(.caption2).foregroundColor(.secondary)
                            Text("Dados sensíveis protegidos conforme a LGPD")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }

                    EditProfileSection(icon: "bandage.fill", title: "Alergias (opcional)") {
                        ProfileInfoBox("Marque as que se aplicam. Usado só para calcular seu nível de risco com mais precisão, nunca compartilhado.")
                        Toggle("Alergia alimentar confirmada", isOn: $hasFoodAllergy)
                        Toggle("Alergia a anti-inflamatórios (AINEs/aspirina)", isOn: $hasNsaidAllergy)
                        Toggle("Alergia a ácaros, pólen, mofo ou pelos de animais", isOn: $hasInhalantAllergy)
                        Divider()
                        FloatingLabelField(label: "Outras alergias (opcional)", text: $allergies)
                        Text("Visível apenas para você no seu perfil pessoal.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Divider()
                        HStack(spacing: 6) {
                            Image(systemName: "lock.fill").font(.caption2).foregroundColor(.secondary)
                            Text("Dados sensíveis protegidos conforme a LGPD")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }

                    EditProfileSection(icon: "questionmark.circle.fill", title: "Informações para quem for te ajudar") {
                        ProfileInfoBox("Mostradas só para quem aceitar seu pedido de ajuda numa emergência.")
                        IconToggleRow(icon: "figure.roll", label: "Uso cadeira de rodas", isOn: $hasWheelchair)
                        IconToggleRow(icon: "eye.slash.fill", label: "Baixa visão ou cegueira", isOn: $hasLowVision)
                        IconToggleRow(
                            icon: nil, label: "Outra condição que exija atenção especial",
                            subtitle: "Descreva no campo de detalhes abaixo", isOn: $hasSpecialCondition
                        )
                        FloatingLabelField(label: "Detalhes complementares (opcional)", text: $healthNotes)
                    }

                    EditProfileSection(icon: "person.2.fill", title: "Contato de Emergência") {
                        FloatingLabelField(label: "Nome", text: $emergencyName)
                        FloatingLabelField(label: "Telefone", text: $emergencyPhone, keyboardType: .phonePad)
                        FloatingLabelField(label: "Parentesco (ex: Mãe)", text: $emergencyRelationship)
                    }

                    Button(action: onSave) {
                        HStack {
                            Image(systemName: "checkmark")
                            Text("Salvar Alterações").fontWeight(.bold)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.afiPrimary)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .padding(.top, 4)
                }
                .padding(16)
            }
            .background(Color.afiBackground)
            .navigationTitle("Editar Perfil")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - EditProfileSection (card com ícone em badge circular)

struct EditProfileSection<Content: View>: View {
    let icon: String
    let title: String
    let content: Content

    init(icon: String, title: String, @ViewBuilder content: () -> Content) {
        self.icon = icon
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                ZStack {
                    Circle().fill(Color.afiPrimary.opacity(0.15))
                    Image(systemName: icon)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.afiPrimary)
                }
                .frame(width: 24, height: 24)
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.afiPrimary)
            }
            Divider()
            VStack(alignment: .leading, spacing: 12) {
                content
            }
        }
        .padding(14)
        .background(Color.afiSurface)
        .cornerRadius(16)
    }
}

// MARK: - FloatingLabelField (label sobe quando focado ou preenchido)

struct FloatingLabelField: View {
    let label: String
    @Binding var text: String
    var keyboardType: UIKeyboardType = .default

    @FocusState private var isFocused: Bool
    private var isFloating: Bool { isFocused || !text.isEmpty }

    var body: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 14)
                .stroke(isFocused ? Color.afiPrimary : Color(.systemGray4), lineWidth: isFocused ? 1.5 : 1)

            TextField("", text: $text)
                .keyboardType(keyboardType)
                .focused($isFocused)
                .padding(.horizontal, 14)
                .padding(.top, isFloating ? 8 : 0)

            Text(label)
                .font(isFloating ? .caption2 : .body)
                .foregroundColor(isFocused ? .afiPrimary : .secondary)
                .padding(.horizontal, isFloating ? 4 : 0)
                .background(isFloating ? Color.afiSurface : Color.clear)
                .padding(.leading, 10)
                .offset(y: isFloating ? -22 : 0)
                .animation(.easeOut(duration: 0.15), value: isFloating)
        }
        .frame(height: 52)
    }
}

// MARK: - ProfileInfoBox (caixa de aviso clara — contexto de uso do dado)

struct ProfileInfoBox: View {
    let text: String

    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.caption)
            .foregroundColor(.secondary)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.afiPrimary.opacity(0.08))
            .cornerRadius(12)
    }
}

// MARK: - IconToggleRow (toggle com ícone opcional + subtítulo opcional)

struct IconToggleRow: View {
    let icon: String?
    let label: String
    var subtitle: String? = nil
    @Binding var isOn: Bool

    var body: some View {
        Toggle(isOn: $isOn) {
            HStack(spacing: 10) {
                if let icon {
                    ZStack {
                        RoundedRectangle(cornerRadius: 8).fill(Color(.systemGray6))
                        Image(systemName: icon).font(.system(size: 14)).foregroundColor(.secondary)
                    }
                    .frame(width: 28, height: 28)
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text(label).font(.subheadline)
                    if let subtitle {
                        Text(subtitle).font(.caption2).foregroundColor(.secondary)
                    }
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
