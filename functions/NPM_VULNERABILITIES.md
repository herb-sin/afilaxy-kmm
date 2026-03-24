# ⚠️ Vulnerabilidades npm - Dependências Transitivas

## Status atual

### fast-xml-parser (high) — sem fix seguro disponível
- **CVEs:** GHSA-jp2q-39xq-3w4g, GHSA-8gc5-j5rx-235r
- **Cadeia:** `fast-xml-parser` ← `@google-cloud/storage` ← `firebase-admin >=12.0.0`
- **Fix disponível:** `npm audit fix --force` regressaria `firebase-admin` para `11.11.1` — breaking change, não aplicar
- **Impacto real:** zero — nenhuma Cloud Function processa XML de entrada externa; vetor de ataque não existe no contexto do projeto
- **Ação:** aguardar `firebase-admin` publicar versão `>=12.x` com `fast-xml-parser >=5.5.7`

## Histórico

| Data | Pacote | Severidade | Ação tomada |
|------|--------|------------|-------------|
| 2025 | `fast-xml-parser` | high | Monitorando — fix regressivo, não aplicado |
| 2025 | `qs` | low | Resolvido via `npm audit fix` |

## Verificar periodicamente

```bash
cd functions
npm audit
```
