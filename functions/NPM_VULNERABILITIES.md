# ⚠️ Vulnerabilidades npm - Ação Necessária

## Problema
Detectadas vulnerabilidades em dependências npm:
- `fast-xml-parser` (critical)
- `qs` (low)

## Solução

Execute no seu ambiente com npm instalado:

```bash
cd functions
npm audit fix
git add package-lock.json
git commit -m "fix: update npm dependencies to fix vulnerabilities"
```

## Alternativa
As vulnerabilidades estão em dependências transitivas do Firebase.
Aguarde atualização do `firebase-admin` ou `@google-cloud/storage`.

## Status
- ✅ Não afeta segurança do app (apenas Functions)
- ⚠️ Corrigir antes de tornar repo público
