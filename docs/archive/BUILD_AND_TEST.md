# 🚀 Build e Teste - Afilaxy KMM Android

## ✅ Pré-requisitos Completos

- ✅ Código Android UI implementado
- ✅ `google-services.json` copiado para `androidApp/`
- ✅ Todas as dependências configuradas

---

## 📦 Passo 1: Build do Projeto

Execute o comando abaixo para compilar o app Android em modo debug:

```bash
cd /home/afilaxy/Projetos/afilaxy-kmm
./gradlew :androidApp:assembleDebug --no-daemon
```

**O que este comando faz:**
- Compila os módulos `shared` e `androidApp`
- Valida toda a arquitetura KMM
- Gera APK em `androidApp/build/outputs/apk/debug/`

**Tempo estimado:** 2-5 minutos (primeira vez)

---

## 📱 Passo 2: Instalar no Dispositivo

### Opção A: Instalar automaticamente

```bash
./gradlew :androidApp:installDebug
```

### Opção B: Instalar manualmente

```bash
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

---

## 🧪 Passo 3: Testar Fluxos

### Teste 1: Login
1. ✅ Abrir app
2. ✅ Ver `LoginScreen`
3. ✅ Inserir email/senha
4. ✅ Clicar "Entrar"
5. ✅ Verificar navegação para `HomeScreen`

**Email de teste:** (use uma conta Firebase válida)

---

### Teste 2: Modo Helper
1. ✅ Na `HomeScreen`
2. ✅ Ativar Switch "Modo Ajudante"
3. ✅ Verificar que `isHelperMode` fica `true` no Firestore
4. ✅ FAB de emergência deve sumir

---

### Teste 3: Criar Emergência
1. ✅ Desativar modo helper
2. ✅ Clicar no FAB vermelho
3. ✅ Ver `EmergencyScreen`
4. ✅ Clicar "Criar Emergência"
5. ✅ Verificar emergência no Firestore: `emergencies/{id}`

---

### Teste 4: Chat
1. ✅ Em outro dispositivo/conta, ativar modo helper
2. ✅ Helper aceita emergência
3. ✅ Navegação automática para `ChatScreen`
4. ✅ Enviar mensagens
5. ✅ Verificar recebimento em tempo real

---

## 🐛 Troubleshooting

### Erro: "SDK location not found"

```bash
echo "sdk.dir=/home/afilaxy/Android/Sdk" > local.properties
```

### Erro: "AAPT: error: resource mipmap/ic_launcher not found"

Copie ícones do projeto original:

```bash
cp -r "/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/app/src/main/res/mipmap-"* \
      androidApp/src/main/res/
```

### Erro de compilação no módulo shared

Verifique Kotlin e Gradle:

```bash
./gradlew clean
./gradlew :shared:build
```

### Firebase não conecta

Verifique `google-services.json`:

```bash
cat androidApp/google-services.json | grep project_id
# Deve mostrar: "project_id": "afilaxy-app"
```

---

## 🔍 Verificação de Logs

### Ver logs do app

```bash
adb logcat | grep -E "Afilaxy|FCM|Koin"
```

### Ver logs do Firebase

```bash
adb logcat | grep -E "FirebaseAuth|Firestore"
```

---

## ✅ Checklist de Validação

- [ ] Build completa sem erros
- [ ] APK instalado no dispositivo
- [ ] App abre sem crash
- [ ] LoginScreen aparece
- [ ] Login funciona com Firebase
- [ ] Navegação entre telas funciona
- [ ] Koin DI injeta ViewModels corretamente
- [ ] Firestore lê/escreve dados
- [ ] Chat real-time funciona

---

## 📊 Resultados Esperados

### LoginScreen
```
✅ Campos de email/senha
✅ Botão de login
✅ Loading indicator durante autenticação
✅ Navegação para Home após sucesso
```

### HomeScreen
```
✅ Switch de modo helper
✅ FAB de emergência (quando não é helper)
✅ Card de status
✅ Botão de logout
```

### EmergencyScreen
```
✅ Botão criar emergência
✅ Lista de helpers próximos
✅ Cálculo de distância
✅ Navegação para chat quando aceito
```

### ChatScreen
```
✅ Lista de mensagens
✅ Campo de envio
✅ Mensagens diferentes cores (user vs helper)
✅ Auto-scroll para novas mensagens
```

---

## 🎯 Próximos Passos (Após Validação)

1. **Adicionar mais ViewModels**: Profile, Comunidade
2. **Implementar UI iOS**: SwiftUI com ViewModels compartilhados
3. **Testes Automatizados**: Unit tests, UI tests
4. **Performance**: Otimizar queries Firestore
5. **Release**: Gerar AAB para Play Store

---

**Pronto para testar!** 🚀

Execute os comandos acima e reporte quaisquer erros encontrados.
