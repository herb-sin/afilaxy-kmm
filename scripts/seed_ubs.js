#!/usr/bin/env node

/**
 * Script para popular o Firestore com dados de UBS
 * 
 * Uso:
 * 1. npm install firebase-admin
 * 2. Baixe o service account JSON do Firebase Console
 * 3. node seed_ubs.js path/to/serviceAccountKey.json
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Verifica argumentos
if (process.argv.length < 3) {
  console.error('Uso: node seed_ubs.js <serviceAccountKey.json>');
  process.exit(1);
}

const serviceAccountPath = process.argv[2];

// Inicializa Firebase Admin
const serviceAccount = JSON.parse(fs.readFileSync(path.resolve(serviceAccountPath), 'utf8'));
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Lê dados de seed
const ubsData = JSON.parse(
  fs.readFileSync(path.join(__dirname, 'ubs_seed_data.json'), 'utf8')
);

async function seedUBS() {
  console.log(`🏥 Populando ${ubsData.length} UBS no Firestore...`);
  
  const batch = db.batch();
  
  for (const ubs of ubsData) {
    const docRef = db.collection('ubs').doc();
    
    // Converte location para GeoPoint
    const data = {
      ...ubs,
      location: new admin.firestore.GeoPoint(
        ubs.location.latitude,
        ubs.location.longitude
      ),
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    };
    
    batch.set(docRef, data);
    console.log(`✓ ${ubs.name}`);
  }
  
  await batch.commit();
  console.log('\n✅ Dados inseridos com sucesso!');
}

seedUBS()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('❌ Erro:', error);
    process.exit(1);
  });
