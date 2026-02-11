# iOS App - Afilaxy

## 📱 Estrutura

```
iosApp/
├── iosApp/
│   ├── AfilaxyApp.swift          # Entry point
│   ├── ContentView.swift         # Root view com auth check
│   ├── Info.plist                # Configurações e permissões
│   ├── Views/                    # 13 telas SwiftUI
│   │   ├── LoginView.swift
│   │   ├── RegisterView.swift
│   │   ├── HomeView.swift
│   │   ├── EmergencyView.swift
│   │   ├── EmergencyRequestView.swift
│   │   ├── ChatView.swift
│   │   ├── ProfileView.swift
│   │   ├── SettingsView.swift
│   │   ├── AboutView.swift
│   │   ├── TermsView.swift
│   │   ├── PrivacyView.swift
│   │   └── HelpView.swift
│   ├── ViewModels/
│   │   └── ViewModelProvider.swift  # Wrappers KMM ViewModels
│   └── Helpers/
│       └── KoinHelper.swift
```

## 🚀 Build

### Pré-requisitos
- macOS 12+
- Xcode 14+
- CocoaPods

### Passos

1. **Instalar CocoaPods**
```bash
cd iosApp
pod install
```

2. **Abrir Workspace**
```bash
open iosApp.xcworkspace
```

3. **Configurar Firebase**
- Adicionar `GoogleService-Info.plist` em `iosApp/iosApp/`

4. **Build no Xcode**
- Selecionar target `iosApp`
- Selecionar simulador ou device
- Cmd+R para rodar

## 🎨 Telas Implementadas

### Core (5 telas)
- ✅ LoginView - Autenticação
- ✅ RegisterView - Cadastro
- ✅ HomeView - Menu principal
- ✅ EmergencyView - Criar emergência
- ✅ EmergencyRequestView - Aguardar helper

### Features (2 telas)
- ✅ ChatView - Chat em tempo real
- ✅ ProfileView - Perfil do usuário

### Settings (5 telas)
- ✅ SettingsView - Configurações
- ✅ AboutView - Sobre o app
- ✅ TermsView - Termos de uso
- ✅ PrivacyView - Política de privacidade
- ✅ HelpView - Ajuda e FAQ

## 🔧 Integração KMM

### ViewModels Compartilhados
```swift
// Injeção via Koin
let viewModel = KoinHelperKt.getKoin()
    .get(objCClass: LoginViewModel.self) as! LoginViewModel

// Wrapper ObservableObject para SwiftUI
class AuthViewModelWrapper: ObservableObject {
    private let viewModel: LoginViewModel
    @Published var email = ""
    @Published var isLoading = false
    
    func login() {
        viewModel.onLoginClick()
    }
}
```

### Uso nas Views
```swift
struct LoginView: View {
    @StateObject private var viewModel = ViewModelProvider.shared.getAuthViewModel()
    
    var body: some View {
        TextField("Email", text: $viewModel.email)
        Button("Login") { viewModel.login() }
    }
}
```

## 📋 TODO

- [ ] Conectar ViewModels KMM com @Published properties
- [ ] Implementar observação de Flow/StateFlow
- [ ] Adicionar LocationManager iOS
- [ ] Configurar Push Notifications (APNs)
- [ ] Implementar deep linking
- [ ] Adicionar testes unitários
- [ ] Configurar CI/CD

## 🎯 Status

**13/13 telas implementadas (100%)**

Falta apenas:
- Binding completo com ViewModels KMM
- Observação de estados reativos
- Permissões de localização CoreLocation
- Push notifications

## 📝 Notas

- ViewModels KMM são compartilhados via `shared` framework
- Wrappers necessários para compatibilidade SwiftUI
- Permissões configuradas no Info.plist
- UI 100% SwiftUI nativo
