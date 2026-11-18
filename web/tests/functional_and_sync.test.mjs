import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const indexHtml = fs.readFileSync(path.resolve('index.html'), 'utf8');
const firestoreRules = fs.readFileSync(path.resolve('firestore.rules'), 'utf8');

test('Étape 7.1 — Vérification de la connexion à la base Firebase ai-studio-android-remixges-eaf649c7-b982-4cf3-8da2-da71b4dcafb4', () => {
  assert.ok(
    indexHtml.includes('const FIRESTORE_DATABASE_ID = "ai-studio-android-remixges-eaf649c7-b982-4cf3-8da2-da71b4dcafb4";'),
    'FIRESTORE_DATABASE_ID doit pointer vers ai-studio-android-remixges-eaf649c7-b982-4cf3-8da2-da71b4dcafb4'
  );
  assert.ok(
    indexHtml.includes('Base Firebase : ai-studio-android-remixges-eaf649c7-b982-4cf3-8da2-da71b4dcafb4'),
    'L’interface Web doit afficher clairement la base ai-studio-android-remixges-eaf649c7-b982-4cf3-8da2-da71b4dcafb4'
  );
});

test('Étape 7.2 — Vérification des comptes utilisateurs et rôles (identiques entre Android et Web)', () => {
  const requiredEmails = [
    'meridjaomar2023@gmail.com',
    'merelec.bejaia@gmail.com',
    'abranis@merelec.dz',
    'salah@merelec.dz',
    'mami@merelec.dz',
    'vendeur@merelec.dz',
    'vendeur2@merelec.dz',
    'caisse@merelec.dz'
  ];
  for (const email of requiredEmails) {
    assert.ok(indexHtml.includes(email), `Le compte ${email} doit être présent et actif`);
  }
  assert.ok(
    indexHtml.includes('function mergeEmployeesWithDefaults(customList)'),
    'La fusion des comptes personnalisés et par défaut doit être active'
  );
});

test('Étape 7.3 — Validation de la conformité stricte des payloads Web avec firestore.rules', () => {
  // Vérifier que les clés générées par buildFirestoreValidPayload correspondent exactement à firestore.rules
  const expectedPartnerKeys = [
    'name', 'type', 'phone', 'address', 'solde',
    'notes', 'rc', 'articleImposition', 'nif', 'nis', 'rib'
  ];
  for (const k of expectedPartnerKeys) {
    assert.ok(firestoreRules.includes(`'${k}'`), `firestore.rules attend la clé partner '${k}'`);
    assert.ok(indexHtml.includes(`${k}:`), `index.html doit fournir la clé partner '${k}'`);
  }

  const expectedProductKeys = [
    'name', 'reference', 'barcode', 'category', 'unit',
    'defaultUnitPrice', 'prixAchatMoyen', 'stockActuel', 'seuilAlerte',
    'isArticleEclate', 'isArticleKit'
  ];
  for (const k of expectedProductKeys) {
    assert.ok(firestoreRules.includes(`'${k}'`), `firestore.rules attend la clé product '${k}'`);
    assert.ok(indexHtml.includes(`${k}:`), `index.html doit fournir la clé product '${k}'`);
  }

  const expectedOperationKeys = [
    'dateTime', 'typeOperation', 'clientFournisseur', 'montantVerse',
    'montantTotalTtc', 'modeReglement', 'vendeur', 'notes',
    'appliquerTva', 'tauxTva', 'statut'
  ];
  for (const k of expectedOperationKeys) {
    assert.ok(firestoreRules.includes(`'${k}'`), `firestore.rules attend la clé operation '${k}'`);
    assert.ok(indexHtml.includes(`${k}:`), `index.html doit fournir la clé operation '${k}'`);
  }
});

test('Étape 7.4 — Vérification du Scanner OCR Intelligent (choix de l’ordre des colonnes 1er, 2e, 3e, 4e et sauvegarde)', () => {
  assert.ok(indexHtml.includes('id="ocr-col-1"'), 'Sélecteur 1re colonne présent');
  assert.ok(indexHtml.includes('id="ocr-col-2"'), 'Sélecteur 2e colonne présent');
  assert.ok(indexHtml.includes('id="ocr-col-3"'), 'Sélecteur 3e colonne présent');
  assert.ok(indexHtml.includes('id="ocr-col-4"'), 'Sélecteur 4e colonne présent');
  assert.ok(
    indexHtml.includes('window.saveOcrBonAndExit ='),
    'La fonction saveOcrBonAndExit doit enregistrer le bon et fermer la fenêtre OCR'
  );
});

test('Étape 7.5 — Vérification de la synchronisation bidirectionnelle des opérations et lignes (operation_lines)', () => {
  assert.ok(
    indexHtml.includes('collection(db, "operation_lines")'),
    'L’écouteur temps réel sur operation_lines doit être actif pour reconstruire les lignes créées sur Android'
  );
  assert.ok(
    indexHtml.includes('await updateDoc(ref, {') && indexHtml.includes('await setDoc(ref, {'),
    'L’écriture Firestore doit utiliser getDoc + updateDoc / setDoc pour respecter createdAt == existing().createdAt'
  );
});
