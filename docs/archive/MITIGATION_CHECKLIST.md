# ✅ Checklist de Mitigação - Afilaxy

## 🔧 Técnico

### Build & Compilação
- [x] **iOS Koin.kt limpo**: Sem referências a `Context` Android
- [x] **Android Koin.kt criado**: Injeção de `Context` via platformModule
- [x] **LocationRepository consistente**: Assinaturas alinhadas entre plataformas
- [ ] **Testar build iOS**: `./gradlew linkDebugFrameworkIos`
- [ ] **Testar build Android**: `./gradlew androidApp:assembleDebug`

### Arquitetura
- [x] **Separação expect/actual**: LocationRepository usa padrão correto
- [x] **DI por plataforma**: Settings e LocationRepository no platformModule
- [ ] **Verificar startKoin no iOS**: MainViewController inicializa Koin corretamente

---

## 🔒 Legal & Compliance

### LGPD (Lei 13.709/2018)
- [x] **Disclaimer de privacidade criado**: `LegalDisclaimer.kt`
- [ ] **Consentimento explícito**: Tela de onboarding com aceite
- [ ] **Minimização de dados**: Coletar apenas lat/lng durante emergência
- [ ] **Direito ao esquecimento**: Botão "Apagar minha conta"
- [ ] **Política de privacidade**: Documento legal completo
- [ ] **Termo de uso**: Documento legal completo

### Responsabilidade Civil
- [x] **Disclaimer médico criado**: Aviso que não substitui SAMU/Bombeiros
- [ ] **Tela de aviso ao criar emergência**: Modal com botões SAMU/Continuar
- [ ] **Isenção de responsabilidade**: Cláusula no termo de uso
- [ ] **Seguro RC**: Avaliar necessidade de seguro de responsabilidade civil

### Regulatório (ANVISA)
- [ ] **Verificar classificação**: App é dispositivo médico? (Provavelmente não)
- [ ] **Consultar advogado**: Validar se precisa registro ANVISA
- [ ] **Documentar não-prescrição**: Deixar claro que não prescrevemos

---

## 🏥 Clínico & Ético

### Segurança do Paciente
- [ ] **Botão SAMU destacado**: Sempre visível em emergências
- [ ] **Timeout de emergência**: Cancelar automaticamente após 30min
- [ ] **Validação de medicação**: Apenas broncodilatadores (não corticoides)
- [ ] **Educação pós-crise**: Notificação 24h depois com link UBS

### Profissionais de Saúde
- [ ] **Validação de CRM/CRF**: Verificar registro profissional
- [ ] **Código de ética**: Profissionais não podem prescrever pelo app
- [ ] **Conteúdo revisado**: Pneumologista valida material educativo

---

## 💰 Negócio & Sustentabilidade

### Modelo de Receita
- [ ] **Integração Stripe**: Assinaturas para profissionais
- [ ] **Planos definidos**: Básico/Pro/Premium
- [ ] **Cancelamento fácil**: Compliance com CDC

### Parcerias
- [ ] **Contato SES-SP**: Apresentar projeto para Secretaria de Saúde
- [ ] **Contato DataSUS**: Avaliar integração de dados
- [ ] **Contato UBS**: Mapear postos de saúde em SP

---

## 📊 Dados & Analytics

### BI para Gestores Públicos
- [ ] **Dashboard georreferenciado**: Hotspots de crises
- [ ] **Anonimização**: Dados agregados, sem identificação
- [ ] **API pública**: Disponibilizar dados para pesquisa

### Métricas de Impacto
- [ ] **Taxa de reinserção**: % que voltou ao tratamento preventivo
- [ ] **Redução de internações**: Comparar com DataSUS
- [ ] **Economia SUS**: Calcular custo evitado

---

## 🚀 Pré-InovaHC

### Documentação
- [ ] **Pitch deck**: 10 slides máximo
- [ ] **Vídeo demo**: 2 minutos mostrando fluxo completo
- [ ] **Estudo de caso**: 1 paciente real (anonimizado)

### Validação
- [ ] **Beta test**: 20 usuários reais em SP
- [ ] **Feedback médico**: 3 pneumologistas revisam
- [ ] **Teste de usabilidade**: 5 pacientes testam UI

### Riscos Críticos
- [ ] **Backup jurídico**: Advogado revisa termos
- [ ] **Backup técnico**: App funciona 100% em iOS e Android
- [ ] **Backup clínico**: Médico apoia o projeto publicamente

---

## 🎯 Próximos Passos Imediatos

1. **Hoje**: Testar build iOS após correções Koin
2. **Esta semana**: Implementar tela de disclaimer legal
3. **Próxima semana**: Consultar advogado sobre LGPD/ANVISA
4. **Mês que vem**: Beta test com 20 usuários

---

**Status Geral**: 🟡 Em Progresso  
**Bloqueadores**: Nenhum crítico  
**Risco Maior**: Validação regulatória (ANVISA)  
**Próxima Milestone**: Build iOS funcionando + Disclaimer implementado
