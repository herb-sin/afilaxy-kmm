# ⚠️ Vulnerabilidades npm — Dependências Transitivas

## Estado atual: **RESOLVIDO — 0 vulnerabilidades**

```
npm audit  →  found 0 vulnerabilities
npx tsc --noEmit → ok
npm ls firebase-admin firebase-functions → sem ERESOLVE / sem "invalid"
```

Última verificação: 2026-10-12.

## Antes → Depois

| | Antes | Depois |
|---|---|---|
| **Total** | **22** | **0** |
| critical | 3 | 0 |
| high | 6 | 0 |
| moderate | 13 | 0 |

### Pacotes que compunham as 22 falhas

- `fast-xml-parser` (high) via `@google-cloud/storage` ← `firebase-admin`
- `node-forge` (high) via `firebase-admin`
- `websocket-driver`, `uuid`, `retry-request`, `teeny-request`, `gaxios`, `google-gax`, `@google-cloud/firestore`, `@google-cloud/storage` (critical/high/moderate) via `firebase-admin`
- `qs` (moderate) via `express` ← `firebase-functions`
- demais transitivos de `firebase-admin`/`firebase-functions`

## Método aplicado

1. **Overrides removidos** do `package.json`:
   - `"fast-xml-parser": "5.5.5"` (pinnava justamente a versão vulnerável)
   - `"qs": "6.15.0"`
   - mantido `"@tootallnate/once": "3.0.1"` (não é vetor de CVE)
2. **`npm audit fix` (SEM `--force`)** — resolveu a maior parte que não exigia breaking change.
3. **Bumps de dependência direta:**
   - `firebase-admin`: `^12.0.0` → **`^14.5.0`** (elimina `node-forge` + cadeia `uuid`/`google-gax`/`teeny-request`)
   - `firebase-functions`: `^7.2.2` → **`^7.3.0`** (instalado `7.4.0`) — necessário porque o peer range de `7.2.x` só aceitava `firebase-admin ^11||^12||^13`; a partir de `7.3.0` o peer inclui `^14.0.0`, permitindo instalação limpa sem ERESOLVE.
4. **Ajuste de tipos no código (mecânico, `src/index.ts`):** o export raiz do
   `firebase-admin@14` não expõe mais `firestore`/`messaging`. Substituído:
   - `import { getFunctions } from 'firebase-admin/functions'` + novos
     `import { getFirestore, FieldValue, FieldPath } from 'firebase-admin/firestore'`
     e `import { getMessaging } from 'firebase-admin/messaging'`
   - `admin.firestore()` → `getFirestore()`
   - `admin.firestore.FieldValue.*` → `FieldValue.*`
   - `admin.firestore.FieldPath` → `FieldPath`
   - `admin.messaging()` → `getMessaging()`
   - `admin.initializeApp()` permanece (ainda exportado pela raiz)
   Sem mudança de comportamento: mesmas instâncias/SDK, apenas outra forma de import.

## Resíduos restantes

**Nenhum.** `npm audit` reporta 0 vulnerabilities (era 22 antes desta rodada).

## Histórico

| Data | Pacote | Severidade | Ação tomada |
|------|--------|------------|-------------|
| 2026-10-12 | total 22 (3 crit/6 high/13 mod) | crítico/high/moderate | Resolvido — overrides removidos + `npm audit fix` + `firebase-admin ^14.5.0` + `firebase-functions ^7.3.0` → **0** |
| 2025 | `fast-xml-parser` | high | Monitorando — fix regressivo, não aplicado (superado por esta rodada) |
| 2025 | `qs` | low | Resolvido via `npm audit fix` |

## Verificar periodicamente

```bash
cd functions
npm audit
npx tsc --noEmit
npm ls firebase-admin firebase-functions   # deve sair sem "invalid"/ERESOLVE
```
