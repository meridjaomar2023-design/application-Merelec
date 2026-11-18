# DOCUMENTATION TECHNIQUE : RECONSTRUCTION WEB (REACT + TYPESCRIPT) & SYNCHRONISATION ANDROID KOTLIN

## 1. Analyse Intégrale du Projet Android Kotlin Existant (`app/src/main/java/com/example/`)

L'application Android **Merélec ERP** reste **100 % intacte et non modifiée** (`MainActivity.kt`, `build.gradle.kts`, `AndroidManifest.xml`, `google-services.json`). Son architecture repose sur :

### 1.1 Point d'entrée et Navigation (`MainActivity.kt`)
- Initialise Firebase (`FirebaseApp.initializeApp(this)`) et Firebase App Check (`configureAppCheck(this, intent)`).
- `RootAuthGate()` gère la session combinée :
  - Authentification Google Cloud via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
  - Authentification multi-utilisateurs par Email & Mot de passe avec contrôle granulaire des permissions (`EmployeeEntity`).
- Navigation d'écrans pilotée par `AppScreen` dans `OperationViewModel.kt`.

### 1.2 Écrans Jetpack Compose (`ui/screens/*`) et Équivalents Web React
| Écran Android (Jetpack Compose) | Rôle Métier | Équivalent Web Desktop (`index.html` & `/web/src/`) |
|---|---|---|
| `AuthScreen.kt` | Connexion Utilisateur (onglet gauche) & Espace Administrateur (onglet droit, carte Cloud Firestore, gestion des utilisateurs) | Écran de connexion à double onglet (`Connexion Utilisateur` / `Espace Administrateur`) + bouton **☁️ Se connecter avec Firebase Cloud (Google)** |
| `MainOperationsScreen.kt` | Liste des Bons de livraison, Factures, Devis, Commandes et Règlements + filtres & KPI | Onglet `📄 Bons & Factures` avec barre de recherche, filtres par type et KPI en temps réel |
| `OperationFormScreen.kt` & `OperationDetailScreen.kt` | Création/édition de Bons et Factures, calcul HT/TVA/TTC, remise, versement et reste à payer | Formulaire modal multi-lignes avec calcul automatique HT/TVA/TTC et statut (`Payé`, `Validé`) |
| `CreancesScreen.kt` & `PartnerDetailScreen.kt` | Suivi des créances clients et dettes fournisseurs, encaissements/décaissements rapides | Onglet `💳 Créances & Dettes` avec filtrage par partenaire et encaissement en 1 clic |
| `StockManagementScreen.kt` | Catalogue articles, alertes de stock bas, Prix Moyen Pondéré (PMP), Articles Kits & Éclatés | Onglet `📦 Articles & Stock` (avec masquage automatique du Prix d'Achat pour les non-administrateurs) |
| `DirectoryManagementScreen.kt` | Répertoire Clients & Fournisseurs avec coordonnées fiscales (RC, NIF, AI, NIS, RIB) | Onglet `👥 Clients & Fournisseurs` avec CRUD complet et soldes recalculés |
| `IntelligentOcrScreen.kt` | Scanner OCR intelligent Gemini + ML Kit avec choix de l'ordre des colonnes (`Qts`, `Réf`, `Désignation`, `P.U. HT`) | Fenêtre modale `📷 Scanner OCR Intelligent` avec sélecteurs `1er, 2e, 3e, 4e` et bouton `💾 Enregistrer le Bon & Quitter` |
| `PdfStudioScreen.kt` | Génération de Factures et Bons A4 avec cachet humide officiel (sans signature stylo) | Onglet `🖨️ Studio PDF & Cachet Humide` avec aperçu A4, impression navigateur et cachet Canvas PNG HD |
| `EmployeeManagementScreen.kt` | Gestion des employés, mots de passe et droits (`canModifyBon`, `canDeleteBon`, `canModifyFacture`, `canDeleteFacture`, `canModifyCreance`, `canDeleteCreance`) | Onglet `🛡️ Utilisateurs & Droits` réservé à l'Administrateur |
| `ConfigurationScreen.kt` & `GoogleDriveBackupScreen.kt` | Coordonnées de l'entreprise, export Excel `.xls`, sauvegarde JSON et synchronisation Firebase | Onglet `⚙️ Configuration & Sauvegarde` (avec interdiction de suppression de sauvegarde pour les non-admins) |

---

## 2. Connexion au Projet Firebase Existant & Base Cloud Firestore

La version Web se connecte **exactement au même projet Firebase et à la même base Cloud Firestore** que l'application Android :

- **Project ID** : `gen-lang-client-0345431815`
- **Firestore Database ID** : `ai-studio-android-remixges-eaf649c7-b982-4cf3-8da2-da71b4dcafb4`
- **Web App ID** : `1:105483567356:web:de698cc230161552ba5779`
- **Auth Domain** : `gen-lang-client-0345431815.firebaseapp.com`

### 2.1 Collections Cloud Firestore et Règles de Sécurité (`/firestore.rules`)
Toutes les lectures et écritures Web respectent strictement les schémas validés par `/firestore.rules` :
1. **`operations`** : `userId`, `dateTime`, `typeOperation`, `clientFournisseur`, `montantVerse`, `montantTotalTtc`, `modeReglement`, `notes`, `appliquerTva`, `tauxTva`, `statut`, `vendeur`, `createdAt`, `updatedAt`.
2. **`operation_lines`** : `userId`, `idOperation`, `produit`, `quantite`, `prixUnitaire`, `parentArticle`, `createdAt`, `updatedAt`.
3. **`partners`** : `userId`, `name`, `type`, `phone`, `email`, `address`, `soldeInitial`, `soldeActuel`, `notes`, `rc`, `nif`, `ai`, `articleImposition`, `nis`, `rib`, `createdAt`, `updatedAt`.
4. **`products`** : `userId`, `name`, `defaultUnitPrice`, `category`, `marque`, `unit`, `stockActuel`, `seuilAlerte`, `isArticleEclate`, `isArticleKit`, `prixAchatMoyen`, `barcode`, `reference`, `createdAt`, `updatedAt`.
5. **`employees`** : `userId`, `name`, `role`, `pinCode`, `email`, `password`, `isActive`, `canModifyBon`, `canDeleteBon`, `canModifyFacture`, `canDeleteFacture`, `canModifyCreance`, `canDeleteCreance`, `canManageEmployees`, `createdAt`, `updatedAt`.

Pour respecter la règle `incoming().createdAt == existing().createdAt` lors des mises à jour, le service Web vérifie systématiquement `getDoc(ref)` :
- Si le document existe : appel à `updateDoc(ref, { ...fields, userId, updatedAt: serverTimestamp() })`.
- Si le document est nouveau : appel à `setDoc(ref, { ...fields, userId, createdAt: serverTimestamp(), updatedAt: serverTimestamp() })`.

---

## 3. Correspondance des Fonctionnalités Android Spécifiques sur le Web

| Fonctionnalité Android Native | API Android Utilisée | Équivalent Web Implémenté |
|---|---|---|
| Authentification Google Cloud | Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) | Firebase Web SDK `signInWithPopup(auth, GoogleAuthProvider)` + session Cloud directe sur Mac Safari / fichier local |
| Base de données hors-ligne | Room SQLite (`AppDatabase`, `LocalErpDao`) | `localStorage` (`merelec_standalone_state_v2`) + synchronisation temps réel inter-onglets via `BroadcastChannel("merelec_realtime_sync")` |
| Scanner OCR de factures | CameraX / Photo Picker + ML Kit Text Recognition + Firebase AI Logic (Gemini) | Import d'image/photo (`<input type="file" accept="image/*">`), prévisualisation Canvas avec réglage de contraste, extraction intelligente et mappage dynamique des colonnes (`1er, 2e, 3e, 4e`) |
| Génération PDF & Cachet Humide | Android `PdfDocument` + `Canvas` (`PdfGenerator.kt`) | Générateur A4 haute définition HTML/CSS `@media print` + rendu vectoriel `<canvas>` du cachet humide officiel (sans signature stylo) |
| Partage de fichiers (`.xls`, `.json`, `.pdf`) | Android `FileProvider` + `Intent.ACTION_SEND` | Téléchargement direct navigateur via `Blob` et `URL.createObjectURL` |

---

## 4. Déploiement sur Google Cloud Run

Le dossier `/web` contient un `Dockerfile` multi-étapes prêt pour **Google Cloud Run** :

```bash
cd web
gcloud run deploy merelec-erp-web \
  --source . \
  --project gen-lang-client-0345431815 \
  --region europe-west2 \
  --allow-unauthenticated
```

Une fois déployé, ajoutez le domaine Cloud Run généré (`*.run.app`) dans **Firebase Console → Authentication → Settings → Authorized domains** pour autoriser la fenêtre popup Google OAuth sur tous les navigateurs Windows et macOS.
