import {
  OperationEntity,
  ProductCatalogEntity,
  PartnerEntity,
  DashboardAnalytics,
  OcrFieldType,
  LigneOperationEntity
} from '../types/models';

export function formatDa(amount: number): string {
  const num = Number(amount) || 0;
  return (
    num.toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }) + ' DA'
  );
}

export function formatDateFr(timestamp: number): string {
  return new Date(Number(timestamp) || Date.now()).toLocaleDateString('fr-FR');
}

export function isSaleOperation(type: string): boolean {
  return (
    type === 'VENTE' ||
    type === 'BON_LIVRAISON_CLIENT' ||
    type === 'FACTURE_VENTE' ||
    type === 'BON_COMMANDE_CLIENT' ||
    type === 'DEVIS_CLIENT'
  );
}

export function isPurchaseOperation(type: string): boolean {
  return (
    type === 'ACHAT' ||
    type === 'BON_RECEPTION_FOURNISSEUR' ||
    type === 'FACTURE_ACHAT' ||
    type === 'BON_COMMANDE_FOURNISSEUR' ||
    type === 'DEMANDE_PRIX_FOURNISSEUR'
  );
}

export function isImportedCreanceOperation(op: Partial<OperationEntity>): boolean {
  const notes = String(op.notes || '').toUpperCase();
  const hasSoldeNote =
    notes.includes('SOLDE INITIAL IMPORT') ||
    notes.includes('SOLDE INITIAL CLIENT') ||
    notes.includes('SOLDE INITIAL FOURNISSEUR');
  if (hasSoldeNote) return true;
  if (Array.isArray(op.lignes) && op.lignes.length > 0) {
    return op.lignes.every((l) =>
      String(l.produit || '')
        .toUpperCase()
        .includes('SOLDE INITIAL IMPORT')
    );
  }
  return false;
}

export function calculateOperationTotalHt(op: OperationEntity): number {
  if (op.lignes && op.lignes.length > 0) {
    return op.lignes.reduce(
      (acc, l) => acc + (Number(l.quantite) || 0) * (Number(l.prixUnitaire) || 0),
      0
    );
  }
  return Number(op.montantTotalTtc) || 0;
}

export function calculateOperationTotalTtc(op: OperationEntity): number {
  const ht = calculateOperationTotalHt(op);
  if (op.appliquerTva) {
    const rate = Number(op.tauxTva ?? 19) || 0;
    return ht * (1 + rate / 100);
  }
  return ht;
}

/**
 * Calcul du Prix Moyen Pondéré (PMP) identique à OperationRepository.kt lors d'un achat fournisseur.
 */
export function calculateUpdatedPmp(
  currentStock: number,
  currentPmp: number,
  incomingQty: number,
  incomingUnitPrice: number
): number {
  if (incomingQty <= 0 || incomingUnitPrice <= 0) return currentPmp;
  const safeCurrentStock = Math.max(0, currentStock);
  if (safeCurrentStock <= 0 || currentPmp <= 0) return incomingUnitPrice;
  const totalValue = safeCurrentStock * currentPmp + incomingQty * incomingUnitPrice;
  return totalValue / (safeCurrentStock + incomingQty);
}

/**
 * Calcul des indicateurs analytiques du Tableau de Bord (portage exact de AnalyticsCalculator.kt).
 */
export function computeDashboardAnalytics(
  operations: OperationEntity[],
  partners: PartnerEntity[],
  products: ProductCatalogEntity[]
): DashboardAnalytics {
  let totalSalesTtc = 0;
  let totalPurchasesTtc = 0;
  let totalCollected = 0;
  let totalDisbursed = 0;
  let estimatedNetProfit = 0;

  const productByName = new Map<string, ProductCatalogEntity>();
  products.forEach((p) => productByName.set(p.name.trim().toLowerCase(), p));

  for (const op of operations) {
    if (isImportedCreanceOperation(op)) continue;
    const ttc = calculateOperationTotalTtc(op);
    const verse = Number(op.montantVerse) || 0;

    if (
      op.typeOperation === 'VENTE' ||
      op.typeOperation === 'BON_LIVRAISON_CLIENT' ||
      op.typeOperation === 'FACTURE_VENTE'
    ) {
      totalSalesTtc += ttc;
      totalCollected += verse;
      for (const line of op.lignes || []) {
        const prod = productByName.get(String(line.produit || '').trim().toLowerCase());
        const pmp = prod?.prixAchatMoyen || (Number(line.prixUnitaire) || 0) * 0.78;
        estimatedNetProfit +=
          ((Number(line.prixUnitaire) || 0) - pmp) * (Number(line.quantite) || 0);
      }
    } else if (
      op.typeOperation === 'ACHAT' ||
      op.typeOperation === 'BON_RECEPTION_FOURNISSEUR' ||
      op.typeOperation === 'FACTURE_ACHAT'
    ) {
      totalPurchasesTtc += ttc;
      totalDisbursed += verse;
    } else if (op.typeOperation === 'ENCAISSEMENT') {
      totalCollected += verse;
    } else if (op.typeOperation === 'DECAISSEMENT') {
      totalDisbursed += verse;
    }
  }

  let totalClientReceivables = 0;
  let totalSupplierPayables = 0;
  for (const p of partners) {
    if (p.type === 'FOURNISSEUR') {
      totalSupplierPayables += Math.max(0, Number(p.soldeActuel) || 0);
    } else {
      totalClientReceivables += Math.max(0, Number(p.soldeActuel) || 0);
    }
  }

  const lowStockProductsCount = products.filter(
    (p) => Number(p.stockActuel) <= Number(p.seuilAlerte ?? 5)
  ).length;

  return {
    totalSalesTtc,
    totalPurchasesTtc,
    totalCollected,
    totalDisbursed,
    totalClientReceivables,
    totalSupplierPayables,
    estimatedNetProfit,
    lowStockProductsCount
  };
}

/**
 * Conversion d'un montant en toutes lettres en français (identique à CurrencyUtils.kt).
 */
export function numberToFrenchWords(n: number): string {
  const units = [
    'zéro', 'un', 'deux', 'trois', 'quatre', 'cinq', 'six', 'sept', 'huit', 'neuf',
    'dix', 'onze', 'douze', 'treize', 'quatorze', 'quinze', 'seize', 'dix-sept', 'dix-huit', 'dix-neuf'
  ];
  const tens = [
    '', '', 'vingt', 'trente', 'quarante', 'cinquante', 'soixante', 'soixante', 'quatre-vingt', 'quatre-vingt'
  ];
  function convertBelow100(num: number): string {
    if (num < 20) return units[num];
    const t = Math.floor(num / 10);
    const u = num % 10;
    if (t === 7 || t === 9) return tens[t] + '-' + units[10 + u];
    if (u === 0) return tens[t] + (t === 8 ? 's' : '');
    if (u === 1 && t < 8) return tens[t] + ' et un';
    return tens[t] + '-' + units[u];
  }
  function convertBelow1000(num: number): string {
    if (num < 100) return convertBelow100(num);
    const h = Math.floor(num / 100);
    const rem = num % 100;
    const hStr = h === 1 ? 'cent' : units[h] + ' cent' + (rem === 0 ? 's' : '');
    return rem === 0 ? hStr : hStr + ' ' + convertBelow100(rem);
  }
  const intPart = Math.floor(Math.abs(Number(n) || 0));
  if (intPart === 0) return 'Zéro Dinar Algérien';
  let rem = intPart;
  const parts: string[] = [];
  const millions = Math.floor(rem / 1000000);
  rem %= 1000000;
  const thousands = Math.floor(rem / 1000);
  rem %= 1000;
  if (millions > 0) {
    parts.push(millions === 1 ? 'un million' : convertBelow1000(millions) + ' millions');
  }
  if (thousands > 0) {
    parts.push(thousands === 1 ? 'mille' : convertBelow1000(thousands) + ' mille');
  }
  if (rem > 0) parts.push(convertBelow1000(rem));
  const str = parts.join(' ') + ' Dinars Algériens';
  return str.charAt(0).toUpperCase() + str.slice(1);
}

/**
 * Analyseur OCR intelligent avec ordre de colonnes personnalisable (1er, 2e, 3e, 4e)
 * reproduisant la logique de IntelligentOcrEngine.kt.
 */
export function parseOcrTextWithColumnOrder(
  rawText: string,
  colOrder: OcrFieldType[]
): LigneOperationEntity[] {
  const activeCols = colOrder.filter((c) => c !== 'IGNORE');
  const lines = String(rawText || '')
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean);
  const parsed: LigneOperationEntity[] = [];

  for (const line of lines) {
    const tokens = line
      .split(/\t+|\s{2,}|\s*\|\s*|\s*;\s*/)
      .map((t) => t.trim())
      .filter(Boolean);
    let qts = 1;
    let ref = '';
    let des = '';
    let pu = 0;

    if (tokens.length >= 2 && tokens.length >= activeCols.length) {
      activeCols.forEach((fieldType, idx) => {
        const val = tokens[idx] || '';
        if (fieldType === 'QTS') {
          const n = parseFloat(val.replace(/[^\d.,]/g, '').replace(',', '.'));
          if (!isNaN(n) && n > 0) qts = n;
        } else if (fieldType === 'REF') {
          ref = val;
        } else if (fieldType === 'DESIGNATION') {
          des = val;
        } else if (fieldType === 'PU_HT') {
          const n = parseFloat(val.replace(/\s+/g, '').replace(/[^\d.,]/g, '').replace(',', '.'));
          if (!isNaN(n) && n >= 0) pu = n;
        }
      });
    } else {
      const numMatches = [...line.matchAll(/\d+(?:[\s.]\d{3})*(?:[.,]\d{1,2})?/g)].map((m) => ({
        raw: m[0],
        val: parseFloat(m[0].replace(/\s+/g, '').replace(',', '.'))
      }));
      const textPart = line
        .replace(/\d+(?:[\s.]\d{3})*(?:[.,]\d{1,2})?/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
      des = textPart || line;
      const numericOrder = activeCols.filter((c) => c === 'QTS' || c === 'PU_HT');
      if (numMatches.length >= 2) {
        if (numericOrder[0] === 'QTS') {
          qts = numMatches[0].val || 1;
          pu = numMatches[1].val || 0;
        } else {
          pu = numMatches[0].val || 0;
          qts = numMatches[1].val || 1;
        }
      } else if (numMatches.length === 1) {
        pu = numMatches[0].val || 0;
      }
    }

    const fullDesignation = ref && des ? `${ref} - ${des}` : des || ref || 'Article scanné';
    parsed.push({
      id: `OCR-L-${Date.now()}-${parsed.length + 1}`,
      produit: fullDesignation,
      quantite: qts,
      prixUnitaire: pu
    });
  }

  return parsed;
}
