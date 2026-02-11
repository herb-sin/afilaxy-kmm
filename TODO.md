# TODO - Afilaxy KMM

## 📋 Próximos Passos

---

## 🔴 Crítico (Bloqueadores para Produção)

### iOS Platform-Specific
- [ ] **CoreLocation** - Implementar GPS iOS
  - Criar `LocationManager` com `CLLocationManager`
  - Solicitar permissões em runtime
  - Integrar com `LocationRepositoryImpl` (iosMain)
  - Estimativa: 30 minutos

- [ ] **APNs** - Push Notifications iOS
  - Configurar certificado APNs no Firebase
  - Implementar `didRegisterForRemoteNotificationsWithDeviceToken`
  - Enviar token para Firebase
  - Estimativa: 30 minutos

- [ ] **Background Location** - Modo Helper iOS
  - Adicionar `UIBackgroundModes` no Info.plist
  - Configurar `allowsBackgroundLocationUpdates`
  - Estimativa: 15 minutos

### Testes
- [ ] **Unit Tests** - ViewModels
  - LoginViewModel
  - EmergencyViewModel
  - ChatViewModel
  - Estimativa: 2 horas

- [ ] **Integration Tests** - Repositories
  - AuthRepositoryImpl
  - EmergencyRepositoryImpl
  - ChatRepositoryImpl
  - Estimativa: 2 horas

---

## 🟡 Importante (Melhorias Significativas)

### Rating System
- [ ] **Navegação Automática** - Chat → Rating
  - Adicionar `helperId` no `ChatState`
  - Navegar automaticamente após resolver
  - Estimativa: 15 minutos

- [ ] **Total de Ratings** - Exibir no perfil
  - Modificar `getHelperRating()` para retornar objeto completo
  - Atualizar `ProfileScreen`
  - Estimativa: 15 minutos

### Features
- [ ] **Histórico de Ratings** - Ver avaliações recebidas
  - Tela com lista de ratings
  - Filtros por período
  - Estimativa: 1 hora

- [ ] **Modo Offline** - Funcionalidade básica
  - Cache de dados essenciais
  - Sincronização ao reconectar
  - Estimativa: 3 horas

- [ ] **Retry Logic** - Falhas de rede
  - Retry automático em operações críticas
  - Feedback visual ao usuário
  - Estimativa: 1 hora

### UX
- [ ] **Animações** - Transições suaves
  - Compose animations (Android)
  - SwiftUI animations (iOS)
  - Estimativa: 2 horas

- [ ] **Loading States** - Skeleton screens
  - Shimmer effect
  - Placeholders
  - Estimativa: 1 hora

- [ ] **Error Boundaries** - Tratamento robusto
  - Try-catch em todas operações
  - Mensagens amigáveis
  - Estimativa: 1 hora

---

## 🟢 Nice to Have (Futuro)

### Features Avançadas
- [ ] **Chamada de Voz** - WebRTC
  - Integração com Agora.io ou similar
  - Estimativa: 1 semana

- [ ] **Vídeo Chamada** - Suporte visual
  - Compartilhamento de tela
  - Estimativa: 1 semana

- [ ] **Compartilhar Localização** - Tempo real
  - Mapa no chat
  - Atualização contínua
  - Estimativa: 2 dias

- [ ] **SOS Automático** - Detecção de queda
  - Sensores do device
  - Alerta automático
  - Estimativa: 3 dias

- [ ] **Histórico Médico** - Prontuário
  - Upload de documentos
  - Compartilhamento seguro
  - Estimativa: 1 semana

### Analytics
- [ ] **Firebase Analytics** - Métricas
  - Eventos customizados
  - Funis de conversão
  - Estimativa: 1 dia

- [ ] **Crashlytics** - Relatórios de erro
  - Integração Firebase
  - Alertas automáticos
  - Estimativa: 2 horas

### Performance
- [ ] **Paginação** - Listas grandes
  - Firestore pagination
  - Infinite scroll
  - Estimativa: 1 dia

- [ ] **Image Optimization** - Fotos de perfil
  - Compressão automática
  - Cache de imagens
  - Estimativa: 1 dia

### Segurança
- [ ] **Biometria** - Login rápido
  - Fingerprint/Face ID
  - Estimativa: 1 dia

- [ ] **2FA** - Autenticação dois fatores
  - SMS ou app authenticator
  - Estimativa: 2 dias

- [ ] **Criptografia E2E** - Chat privado
  - Signal Protocol
  - Estimativa: 1 semana

---

## 🔧 Refatorações

### Código
- [ ] **Extrair Constantes** - Magic numbers
  - Criar `Constants.kt`
  - Centralizar valores
  - Estimativa: 1 hora

- [ ] **Documentação** - KDoc
  - Documentar classes públicas
  - Exemplos de uso
  - Estimativa: 2 horas

- [ ] **Code Review** - Qualidade
  - Remover código duplicado
  - Simplificar lógica complexa
  - Estimativa: 1 dia

### Arquitetura
- [ ] **Use Cases** - Lógica complexa
  - Extrair para use cases dedicados
  - Melhorar testabilidade
  - Estimativa: 1 dia

- [ ] **Mappers** - DTOs
  - Separar models de domínio e rede
  - Estimativa: 1 dia

---

## 📱 Plataformas

### Android
- [ ] **Wear OS** - Smartwatch
  - Botão SOS rápido
  - Notificações
  - Estimativa: 1 semana

- [ ] **Android Auto** - Carro
  - Emergência por voz
  - Estimativa: 1 semana

### iOS
- [ ] **watchOS** - Apple Watch
  - Complicações
  - SOS rápido
  - Estimativa: 1 semana

- [ ] **CarPlay** - Carro
  - Interface simplificada
  - Estimativa: 1 semana

### Web
- [ ] **Web App** - Compose for Web
  - Dashboard admin
  - Monitoramento
  - Estimativa: 2 semanas

---

## 🌍 Internacionalização

- [ ] **Strings Resources** - i18n
  - Português (BR) ✅
  - Inglês (US)
  - Espanhol (ES)
  - Estimativa: 2 dias

- [ ] **Formatação** - Datas/números
  - Locale-aware
  - Estimativa: 1 dia

---

## 📊 Monitoramento

- [ ] **Dashboard Admin** - Web
  - Emergências ativas
  - Estatísticas
  - Estimativa: 1 semana

- [ ] **Logs Centralizados** - Debugging
  - Firebase Logging
  - Filtros avançados
  - Estimativa: 1 dia

---

## 🎯 Priorização

### Sprint 1 (1 semana)
1. CoreLocation iOS
2. APNs iOS
3. Unit Tests ViewModels
4. Navegação automática Rating

### Sprint 2 (1 semana)
1. Integration Tests
2. Retry Logic
3. Animações básicas
4. Firebase Analytics

### Sprint 3 (1 semana)
1. Modo Offline
2. Histórico de Ratings
3. Loading States
4. Crashlytics

---

## 📝 Notas

### Dependências
- CoreLocation/APNs requerem macOS
- Testes podem ser feitos em paralelo
- Features avançadas são opcionais

### Estimativas
- Baseadas em desenvolvedor experiente
- Podem variar conforme complexidade
- Incluem tempo de testes

---

## ✅ Concluído

### v2.1.0-kmm
- ✅ Domain Layer completo
- ✅ Data Layer completo
- ✅ Presentation Layer completo
- ✅ Android UI (20 telas)
- ✅ iOS UI (13 telas)
- ✅ iOS ViewModels binding
- ✅ Sistema de Rating
- ✅ FCM Android
- ✅ GPS Android
- ✅ EmergencyOverlayActivity
- ✅ Navegação completa
- ✅ Clean Architecture
- ✅ Koin DI

---

**Desenvolvido com ❤️ usando Kotlin Multiplatform** 🚀
