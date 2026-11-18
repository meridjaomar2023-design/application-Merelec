import React, { useState } from 'react';
import { useOperationViewModel, DEFAULT_EMPLOYEES } from './hooks/useOperationViewModel';
import {
  OperationEntity,
  OperationType,
  OPERATION_TYPE_LABELS,
  OcrFieldType,
  LigneOperationEntity
} from './types/models';
import {
  formatDa,
  formatDateFr,
  calculateOperationTotalTtc,
  parseOcrTextWithColumnOrder
} from './utils/calculations';

export const App: React.FC = () => {
  const vm = useOperationViewModel();
  const [activeTab, setActiveTab] = useState<
    'dashboard' | 'operations' | 'creances' | 'stock' | 'partners' | 'ocr' | 'users'
  >('dashboard');
  const [loginEmail, setLoginEmail] = useState('meridjaomar2023@gmail.com');
  const [loginPassword, setLoginPassword] = useState('admin123');
  const [showPass, setShowPass] = useState(false);

  // États OCR avec ordre des colonnes (1er, 2e, 3e, 4e)
  const [ocrColOrder, setOcrColOrder] = useState<OcrFieldType[]>([
    'QTS',
    'DESIGNATION',
    'REF',
    'PU_HT'
  ]);
  const [ocrRawText, setOcrRawText] = useState('');
  const [ocrPartner, setOcrPartner] = useState('Client Comptoir');
  const [ocrType, setOcrType] = useState<OperationType>('BON_LIVRAISON_CLIENT');
  const [ocrLines, setOcrLines] = useState<LigneOperationEntity[]>([]);

  if (!vm.currentUser) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <h2>Merélec ERP — Version Web Desktop (Windows & macOS)</h2>
          <p className="subtitle">
            Connecté à la base Firebase :{' '}
            <strong>ai-studio-android-remixges-eaf649c7-b982-4cf3-8da2-da71b4dcafb4</strong>
          </p>

          <div className="quick-accounts">
            {DEFAULT_EMPLOYEES.map((emp) => (
              <button
                key={emp.id}
                type="button"
                className="chip-btn"
                onClick={() => {
                  setLoginEmail(emp.email);
                  setLoginPassword(emp.password);
                }}
              >
                <strong>{emp.name}</strong>
                <span>{emp.email}</span>
              </button>
            ))}
          </div>

          <div className="form-group">
            <label>Email de l'utilisateur</label>
            <input
              type="email"
              value={loginEmail}
              onChange={(e) => setLoginEmail(e.target.value)}
              autoCapitalize="none"
              autoCorrect="off"
            />
          </div>

          <div className="form-group">
            <label>
              Mot de passe{' '}
              <button
                type="button"
                className="link-btn"
                onClick={() => setShowPass((s) => !s)}
              >
                {showPass ? '🙈 Masquer' : '👁️ Afficher'}
              </button>
            </label>
            <input
              type={showPass ? 'text' : 'password'}
              value={loginPassword}
              onChange={(e) => setLoginPassword(e.target.value)}
            />
          </div>

          <button
            type="button"
            className="primary-btn"
            onClick={() => vm.loginWithEmailPassword(loginEmail, loginPassword)}
          >
            Se connecter à l'application
          </button>

          <button
            type="button"
            className="cloud-btn"
            onClick={() => vm.connectGoogleFirebase()}
          >
            ☁️ Se connecter avec Firebase Cloud (Google)
          </button>
        </div>
      </div>
    );
  }

  const handleSaveOcrBonAndExit = async () => {
    const validLines = ocrLines.filter((l) => l.produit.trim().length > 0);
    if (validLines.length === 0) {
      vm.showToast('Ajoutez au moins une ligne avant d’enregistrer.');
      return;
    }
    const newOp: OperationEntity = {
      id: `OP-OCR-${Date.now()}`,
      dateTime: Date.now(),
      typeOperation: ocrType,
      clientFournisseur: ocrPartner || 'Client Comptoir',
      montantVerse: 0,
      montantTotalTtc: 0,
      modeReglement: 'Espèces',
      notes: `Importé via Scanner OCR Intelligent (${ocrColOrder.join(' → ')})`,
      appliquerTva: ocrType === 'FACTURE_VENTE' || ocrType === 'FACTURE_ACHAT',
      tauxTva: 19,
      statut: 'Validé',
      vendeur: vm.currentUser.name,
      lignes: validLines
    };
    await vm.saveOperation(newOp);
    setActiveTab('operations');
  };

  return (
    <div className="desktop-layout">
      <aside className="sidebar">
        <div className="brand">
          <h1>MERÉLEC ERP</h1>
          <span className="badge-cloud">
            🟢 Cloud : {vm.cloudEmail || vm.currentUser.email}
          </span>
        </div>
        <nav>
          <button
            className={activeTab === 'dashboard' ? 'active' : ''}
            onClick={() => setActiveTab('dashboard')}
          >
            📊 Tableau de Bord
          </button>
          <button
            className={activeTab === 'operations' ? 'active' : ''}
            onClick={() => setActiveTab('operations')}
          >
            📄 Bons & Factures ({vm.operations.length})
          </button>
          <button
            className={activeTab === 'creances' ? 'active' : ''}
            onClick={() => setActiveTab('creances')}
          >
            💳 Créances & Dettes
          </button>
          <button
            className={activeTab === 'stock' ? 'active' : ''}
            onClick={() => setActiveTab('stock')}
          >
            📦 Articles & Stock ({vm.products.length})
          </button>
          <button
            className={activeTab === 'partners' ? 'active' : ''}
            onClick={() => setActiveTab('partners')}
          >
            👥 Clients & Fournisseurs ({vm.partners.length})
          </button>
          <button
            className={activeTab === 'ocr' ? 'active' : ''}
            onClick={() => setActiveTab('ocr')}
          >
            📷 Scanner OCR Intelligent
          </button>
          {vm.currentUser.role === 'ADMIN' && (
            <button
              className={activeTab === 'users' ? 'active' : ''}
              onClick={() => setActiveTab('users')}
            >
              🛡️ Utilisateurs & Droits ({vm.employees.length})
            </button>
          )}
        </nav>
        <div className="sidebar-footer">
          <button className="sync-btn" onClick={() => vm.syncAllBidirectional()}>
            🔄 Synchroniser Temps Réel Firebase
          </button>
          <button className="logout-btn" onClick={() => vm.setCurrentUser(null)}>
            Se déconnecter ({vm.currentUser.name})
          </button>
        </div>
      </aside>

      <main className="workspace">
        {vm.toastMessage && <div className="toast">{vm.toastMessage}</div>}

        {activeTab === 'dashboard' && (
          <section>
            <h2>Tableau de Bord Analytique (Synchronisé Android ↔ Web)</h2>
            <div className="kpi-grid">
              <div className="kpi-card">
                <span>Total Ventes TTC</span>
                <strong>{formatDa(vm.analytics.totalSalesTtc)}</strong>
              </div>
              <div className="kpi-card">
                <span>Total Encaissé</span>
                <strong>{formatDa(vm.analytics.totalCollected)}</strong>
              </div>
              <div className="kpi-card">
                <span>Créances Clients</span>
                <strong>{formatDa(vm.analytics.totalClientReceivables)}</strong>
              </div>
              <div className="kpi-card">
                <span>Dettes Fournisseurs</span>
                <strong>{formatDa(vm.analytics.totalSupplierPayables)}</strong>
              </div>
            </div>
          </section>
        )}

        {activeTab === 'operations' && (
          <section>
            <h2>Bons de Livraison, Factures, Devis & Règlements</h2>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Réf</th>
                  <th>Date</th>
                  <th>Type</th>
                  <th>Partenaire</th>
                  <th>Total TTC</th>
                  <th>Versé</th>
                  <th>Vendeur</th>
                </tr>
              </thead>
              <tbody>
                {vm.operations.map((op) => (
                  <tr key={op.id}>
                    <td>{op.id}</td>
                    <td>{formatDateFr(op.dateTime)}</td>
                    <td>{OPERATION_TYPE_LABELS[op.typeOperation] || op.typeOperation}</td>
                    <td>{op.clientFournisseur}</td>
                    <td>{formatDa(calculateOperationTotalTtc(op))}</td>
                    <td>{formatDa(op.montantVerse)}</td>
                    <td>{op.vendeur}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        )}

        {activeTab === 'ocr' && (
          <section>
            <h2>Scanner OCR Intelligent — Choix et Ordre des Colonnes</h2>
            <div className="ocr-order-bar">
              {[0, 1, 2, 3].map((idx) => (
                <label key={idx}>
                  {idx + 1}e colonne :
                  <select
                    value={ocrColOrder[idx]}
                    onChange={(e) => {
                      const next = [...ocrColOrder];
                      next[idx] = e.target.value as OcrFieldType;
                      setOcrColOrder(next);
                      setOcrLines(parseOcrTextWithColumnOrder(ocrRawText, next));
                    }}
                  >
                    <option value="QTS">Quantité (Qts)</option>
                    <option value="REF">Référence (Réf)</option>
                    <option value="DESIGNATION">Désignation</option>
                    <option value="PU_HT">P.U. HT</option>
                    <option value="IGNORE">-- Ignorer --</option>
                  </select>
                </label>
              ))}
            </div>
            <div className="form-row">
              <input
                placeholder="Client / Fournisseur"
                value={ocrPartner}
                onChange={(e) => setOcrPartner(e.target.value)}
              />
              <select
                value={ocrType}
                onChange={(e) => setOcrType(e.target.value as OperationType)}
              >
                <option value="BON_LIVRAISON_CLIENT">Bon de Livraison Client (BL)</option>
                <option value="BON_RECEPTION_FOURNISSEUR">Bon de Réception Fournisseur</option>
                <option value="FACTURE_VENTE">Facture de Vente</option>
                <option value="FACTURE_ACHAT">Facture d'Achat</option>
              </select>
            </div>
            <textarea
              rows={5}
              placeholder="Collez ou extrayez les lignes de la facture..."
              value={ocrRawText}
              onChange={(e) => {
                setOcrRawText(e.target.value);
                setOcrLines(parseOcrTextWithColumnOrder(e.target.value, ocrColOrder));
              }}
            />
            <button className="primary-btn" onClick={handleSaveOcrBonAndExit}>
              💾 Enregistrer le Bon & Quitter
            </button>
          </section>
        )}
      </main>
    </div>
  );
};
