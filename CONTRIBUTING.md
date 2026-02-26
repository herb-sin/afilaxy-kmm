# Contribuindo para o Afilaxy

Obrigado por considerar contribuir com o Afilaxy! 🎉

## 🎯 Visão do Projeto

Afilaxy é uma plataforma de Health Equity focada em asma que usa a urgência da crise respiratória como porta de entrada para reinserir pacientes no tratamento preventivo do SUS.

## 🤝 Como Contribuir

### 1. Fork o Repositório
```bash
git clone https://github.com/seu-usuario/afilaxy-kmm.git
cd afilaxy-kmm
```

### 2. Crie uma Branch
```bash
git checkout -b feature/nova-funcionalidade
```

### 3. Faça suas Alterações
- Siga o padrão de código existente
- Adicione testes quando aplicável
- Atualize a documentação se necessário

### 4. Commit suas Mudanças
```bash
git commit -m "feat: adiciona nova funcionalidade"
```

Usamos [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` nova funcionalidade
- `fix:` correção de bug
- `docs:` documentação
- `test:` testes
- `refactor:` refatoração

### 5. Push e Abra um Pull Request
```bash
git push origin feature/nova-funcionalidade
```

## 🏗️ Arquitetura

### Shared Module (KMM)
- **Domain Layer:** Models, Repositories (interfaces), Use Cases
- **Data Layer:** Implementações de repositórios
- **Presentation Layer:** ViewModels compartilhados

### Android App
- **UI:** Jetpack Compose
- **DI:** Koin
- **Backend:** Firebase

### iOS App
- **UI:** SwiftUI
- **DI:** Koin (via KoinHelper)
- **Backend:** Firebase

## 🧪 Testes

### Rodar testes compartilhados:
```bash
./gradlew shared:allTests
```

### Rodar testes Android:
```bash
./gradlew androidApp:testDebugUnitTest
```

## 📝 Diretrizes de Código

- Use Kotlin idiomático
- Prefira imutabilidade
- Documente funções públicas
- Mantenha funções pequenas e focadas
- Siga Clean Architecture

## 🐛 Reportar Bugs

Abra uma issue com:
- Descrição clara do problema
- Passos para reproduzir
- Comportamento esperado vs. atual
- Screenshots (se aplicável)
- Versão do Android/iOS

## 💡 Sugerir Funcionalidades

Abra uma issue com:
- Descrição da funcionalidade
- Justificativa (por que é importante?)
- Exemplos de uso

## 📄 Código de Conduta

Este projeto segue o [Contributor Covenant](CODE_OF_CONDUCT.md). Ao participar, você concorda em seguir suas diretrizes.

## 🙏 Agradecimentos

Toda contribuição é valiosa, seja código, documentação, design ou feedback!
