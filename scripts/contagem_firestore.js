// Contagem agregada (somente leitura) do volume de dados de pesquisa no Firestore:
// ACT, crises graves (GINA), check-ins e pós-crise. Imprime apenas totais —
// nenhum nome, e-mail, ID ou valor individual sai na tela.
// Rodar no Cloud Shell (credenciais da própria conta, sem chave de serviço):
//   cd scripts && npm ci && node contagem_firestore.js
const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
initializeApp({ projectId: 'afilaxy-app' });
const db = getFirestore();

const n = async (q) => (await q.count().get()).data().count;

(async () => {
  const users = db.collection('users');
  const em = db.collection('emergency_requests');

  // ACT: act/{uid}/results/{id}
  const actUsers = await db.collection('act').listDocuments();
  let actTotal = 0, actUsersCom1 = 0, actUsersCom2 = 0, actComPerguntasExtras = 0, actCorticoide = 0, actPS = 0;
  for (const u of actUsers) {
    const r = u.collection('results');
    const c = await n(r);
    actTotal += c;
    if (c >= 1) actUsersCom1++;
    if (c >= 2) actUsersCom2++;
    actComPerguntasExtras += await n(r.where('oralSteroidCourse4w', 'in', [true, false]));
    actCorticoide += await n(r.where('oralSteroidCourse4w', '==', true));
    actPS += await n(r.where('emergencyVisit4w', '==', true));
  }

  // Check-ins: checkins/{uid}/responses/{id}
  const ciUsers = await db.collection('checkins').listDocuments();
  let ciTotal = 0, ciUsersCom1 = 0, ciUsersCom14 = 0;
  for (const u of ciUsers) {
    const c = await n(u.collection('responses'));
    ciTotal += c;
    if (c >= 1) ciUsersCom1++;
    if (c >= 14) ciUsersCom14++;
  }

  const out = {
    'Usuários (users)': await n(users),
    'Perfil: informou crises graves 12m (qualquer valor)': await n(users.where('healthData.severeExacerbationsUpdatedAt', '>', 0)),
    'Perfil: ≥1 crise grave nos últimos 12m': await n(users.where('healthData.severeExacerbations12m', '>', 0)),
    'Perfil: UTI/intubação alguma vez': await n(users.where('healthData.everIcuOrIntubated', '==', true)),
    'ACT: respostas no total': actTotal,
    'ACT: usuários com ≥1 resposta': actUsersCom1,
    'ACT: usuários com ≥2 respostas (dado longitudinal)': actUsersCom2,
    'ACT: respostas com as 2 perguntas extras': actComPerguntasExtras,
    'ACT: corticoide oral ≥3 dias nas últimas 4 semanas': actCorticoide,
    'ACT: pronto-socorro/internação nas últimas 4 semanas': actPS,
    'Check-ins: respostas no total': ciTotal,
    'Check-ins: usuários com ≥1': ciUsersCom1,
    'Check-ins: usuários com ≥14': ciUsersCom14,
    'Emergências: pedidos no total': await n(em),
    'Pós-crise: respondeu sobre pronto-socorro': await n(em.where('emergencyVisitAfter', 'in', [true, false])),
    'Pós-crise: foi ao pronto-socorro': await n(em.where('emergencyVisitAfter', '==', true)),
  };
  console.table(out);
})().catch((e) => { console.error('ERRO:', e.message); process.exit(1); });
