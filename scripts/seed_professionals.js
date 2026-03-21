#!/usr/bin/env node

/**
 * Seed de Profissionais de Saúde para demo
 *
 * Uso:
 *   node seed_professionals.js <serviceAccountKey.json>
 *
 * Popula a coleção health_professionals com dados realistas
 * cobrindo diferentes especialidades, planos e regiões do Brasil.
 */

const admin = require('firebase-admin');
const path = require('path');

if (process.argv.length < 3) {
  console.error('Uso: node seed_professionals.js <serviceAccountKey.json>');
  process.exit(1);
}

const serviceAccount = require(path.resolve(process.argv[2]));
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

const now = Date.now();
const months = (n) => now + n * 30 * 24 * 60 * 60 * 1000;

const professionals = [
  // ── PREMIUM ────────────────────────────────────────────────────────────────
  {
    name: 'Dra. Ana Paula Ferreira',
    specialty: 'PNEUMOLOGIST',
    crm: 'CRM/SP 87432',
    subscriptionPlan: 'PREMIUM_ANNUAL',
    subscriptionExpiry: months(12),
    bio: 'Pneumologista com 18 anos de experiência em asma grave e doenças obstrutivas. Doutora pela USP. Atendimento humanizado com foco em adesão ao tratamento.',
    phone: '11987654321',
    whatsapp: '11987654321',
    clinicAddress: 'Av. Paulista, 1374 - Bela Vista, São Paulo - SP',
    location: { latitude: -23.5613, longitude: -46.6558 },
    rating: 4.9,
    totalReviews: 127,
    isVerified: true,
    profilePhoto: null,
  },
  {
    name: 'Dr. Ricardo Mendes',
    specialty: 'ALLERGIST',
    crm: 'CRM/RJ 54219',
    subscriptionPlan: 'PREMIUM_SEMIANNUAL',
    subscriptionExpiry: months(6),
    bio: 'Alergologista e imunologista. Especialista em asma alérgica, rinite e dermatite atópica. Membro da ASBAI. Consultas presenciais e por telemedicina.',
    phone: '21996543210',
    whatsapp: '21996543210',
    clinicAddress: 'R. Visconde de Pirajá, 550 - Ipanema, Rio de Janeiro - RJ',
    location: { latitude: -22.9838, longitude: -43.2096 },
    rating: 4.8,
    totalReviews: 89,
    isVerified: true,
    profilePhoto: null,
  },

  // ── PRO ────────────────────────────────────────────────────────────────────
  {
    name: 'Dra. Camila Souza',
    specialty: 'PHYSIOTHERAPIST',
    crm: 'CREFITO/SP 112847',
    subscriptionPlan: 'PRO_QUARTERLY',
    subscriptionExpiry: months(3),
    bio: 'Fisioterapeuta respiratória especializada em reabilitação pulmonar para pacientes com asma e DPOC. Técnicas de controle respiratório e exercícios funcionais.',
    phone: '11945678901',
    whatsapp: '11945678901',
    clinicAddress: 'R. Vergueiro, 3185 - Vila Mariana, São Paulo - SP',
    location: { latitude: -23.5934, longitude: -46.6378 },
    rating: 4.7,
    totalReviews: 63,
    isVerified: true,
    profilePhoto: null,
  },
  {
    name: 'Dr. Marcos Oliveira',
    specialty: 'PNEUMOLOGIST',
    crm: 'CRM/MG 67891',
    subscriptionPlan: 'PRO_SEMIANNUAL',
    subscriptionExpiry: months(6),
    bio: 'Pneumologista pediátrico e de adultos. Especialista em asma de difícil controle e imunoterapia. Atende pelo SUS e convênios.',
    phone: '31988776655',
    whatsapp: '31988776655',
    clinicAddress: 'Av. do Contorno, 4747 - Funcionários, Belo Horizonte - MG',
    location: { latitude: -19.9245, longitude: -43.9352 },
    rating: 4.6,
    totalReviews: 54,
    isVerified: true,
    profilePhoto: null,
  },
  {
    name: 'Dra. Juliana Costa',
    specialty: 'ALLERGIST',
    crm: 'CRM/PR 43210',
    subscriptionPlan: 'PRO_ANNUAL',
    subscriptionExpiry: months(12),
    bio: 'Alergista com foco em asma ocupacional e alergia alimentar. Professora universitária. Atendimento em Curitiba e região metropolitana.',
    phone: '41977665544',
    whatsapp: '41977665544',
    clinicAddress: 'R. XV de Novembro, 700 - Centro, Curitiba - PR',
    location: { latitude: -25.4284, longitude: -49.2733 },
    rating: 4.7,
    totalReviews: 41,
    isVerified: true,
    profilePhoto: null,
  },

  // ── BÁSICO ─────────────────────────────────────────────────────────────────
  {
    name: 'Dr. Felipe Nunes',
    specialty: 'PNEUMOLOGIST',
    crm: 'CRM/BA 29876',
    subscriptionPlan: 'BASIC_QUARTERLY',
    subscriptionExpiry: months(3),
    bio: 'Pneumologista com atuação em Salvador e região. Atendimento pelo SUS, convênios e particular. Comprometido com a saúde respiratória da população.',
    phone: '71966554433',
    whatsapp: '71966554433',
    clinicAddress: 'Av. Tancredo Neves, 1632 - Caminho das Árvores, Salvador - BA',
    location: { latitude: -12.9777, longitude: -38.4816 },
    rating: 4.4,
    totalReviews: 28,
    isVerified: false,
    profilePhoto: null,
  },
  {
    name: 'Dra. Patrícia Lima',
    specialty: 'PHYSIOTHERAPIST',
    crm: 'CREFITO/CE 78432',
    subscriptionPlan: 'BASIC_ANNUAL',
    subscriptionExpiry: months(12),
    bio: 'Fisioterapeuta respiratória em Fortaleza. Atendimento domiciliar disponível. Especialista em reabilitação de crianças e adultos com asma.',
    phone: '85955443322',
    whatsapp: '85955443322',
    clinicAddress: 'Av. Santos Dumont, 2828 - Aldeota, Fortaleza - CE',
    location: { latitude: -3.7327, longitude: -38.5270 },
    rating: 4.5,
    totalReviews: 19,
    isVerified: false,
    profilePhoto: null,
  },
  {
    name: 'Dr. André Carvalho',
    specialty: 'ALLERGIST',
    crm: 'CRM/RS 61234',
    subscriptionPlan: 'BASIC_SEMIANNUAL',
    subscriptionExpiry: months(6),
    bio: 'Alergologista em Porto Alegre. Especialista em asma brônquica e rinite alérgica. Atendimento pelo plano de saúde e particular.',
    phone: '51944332211',
    whatsapp: '51944332211',
    clinicAddress: 'Av. Ipiranga, 6681 - Partenon, Porto Alegre - RS',
    location: { latitude: -30.0619, longitude: -51.1739 },
    rating: 4.3,
    totalReviews: 15,
    isVerified: false,
    profilePhoto: null,
  },
];

async function seed() {
  console.log(`\n🫁 Afilaxy — Seed de Profissionais de Saúde`);
  console.log(`📋 ${professionals.length} profissionais a inserir...\n`);

  // Limpa coleção existente para evitar duplicatas em re-execuções
  const existing = await db.collection('health_professionals').get();
  if (!existing.empty) {
    const deleteBatch = db.batch();
    existing.docs.forEach((doc) => deleteBatch.delete(doc.ref));
    await deleteBatch.commit();
    console.log(`🗑  ${existing.size} registros anteriores removidos\n`);
  }

  const batch = db.batch();

  for (const prof of professionals) {
    const docRef = db.collection('health_professionals').doc();
    batch.set(docRef, {
      ...prof,
      id: docRef.id,
      location: prof.location
        ? new admin.firestore.GeoPoint(prof.location.latitude, prof.location.longitude)
        : null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    const planLabel = prof.subscriptionPlan.padEnd(22);
    console.log(`  ✓ [${planLabel}] ${prof.name} — ${prof.specialty}`);
  }

  await batch.commit();

  console.log(`\n✅ ${professionals.length} profissionais inseridos com sucesso!`);
  console.log(`\n📊 Resumo por plano:`);

  const byPlan = professionals.reduce((acc, p) => {
    const tier = p.subscriptionPlan.split('_')[0];
    acc[tier] = (acc[tier] || 0) + 1;
    return acc;
  }, {});
  Object.entries(byPlan).forEach(([plan, count]) =>
    console.log(`   ${plan.padEnd(10)} ${count} profissional(is)`)
  );

  console.log(`\n📍 Cidades cobertas: São Paulo, Rio de Janeiro, Belo Horizonte, Curitiba, Salvador, Fortaleza, Porto Alegre\n`);
}

seed()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('\n❌ Erro:', err.message);
    process.exit(1);
  });
