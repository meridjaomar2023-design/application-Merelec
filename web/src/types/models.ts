/**
 * Modèles de données TypeScript reproduisant à l'identique les data classes Kotlin
 * de l'application Android (app/src/main/java/com/example/data/model/*).
 */

export type OperationType =
  | 'ACHAT'
  | 'VENTE'
  | 'ENCAISSEMENT'
  | 'DECAISSEMENT'
  | 'DEVIS_CLIENT'
  | 'BON_COMMANDE_CLIENT'
  | 'BON_LIVRAISON_CLIENT'
  | 'FACTURE_VENTE'
  | 'AVOIR_VENTE'
  | 'DEMANDE_PRIX_FOURNISSEUR'
  | 'BON_COMMANDE_FOURNISSEUR'
  | 'BON_RECEPTION_FOURNISSEUR'
  | 'FACTURE_ACHAT'
  | 'AVOIR_ACHAT';

export const OPERATION_TYPE_LABELS: Record<OperationType, string> = {
  BON_LIVRAISON_CLIENT: 'Bon de Livraison Client (BL)',
  FACTURE_VENTE: 'Facture de Vente',
  DEVIS_CLIENT: 'Devis Client',
  BON_COMMANDE_CLIENT: 'Bon de Commande Client',
  AVOIR_VENTE: 'Avoir Client',
  BON_RECEPTION_FOURNISSEUR: 'Bon de Réception Fournisseur (BL Achat)',
  FACTURE_ACHAT: 'Facture Fournisseur',
  BON_COMMANDE_FOURNISSEUR: 'Bon de Commande Fournisseur',
  DEMANDE_PRIX_FOURNISSEUR: 'Demande de Prix Fournisseur',
  AVOIR_ACHAT: 'Avoir Fournisseur',
  ENCAISSEMENT: 'Encaissement / Règlement Client',
  DECAISSEMENT: 'Décaissement / Règlement Fournisseur',
  VENTE: 'Vente Comptoir',
  ACHAT: 'Achat Direct'
};

export interface LigneOperationEntity {
  id: string;
  idOperation?: string;
  produit: string;
  quantite: number;
  prixUnitaire: number;
  parentArticle?: string;
}

export interface OperationEntity {
  id: string;
  userId?: string;
  dateTime: number;
  typeOperation: OperationType;
  clientFournisseur: string;
  montantVerse: number;
  montantTotalTtc: number;
  modeReglement: string;
  notes: string;
  appliquerTva: boolean;
  tauxTva: number;
  statut: string;
  vendeur: string;
  lignes: LigneOperationEntity[];
}

export type PartnerType = 'CLIENT' | 'FOURNISSEUR' | 'CLIENT_ET_FOURNISSEUR';

export interface PartnerEntity {
  id: string;
  userId?: string;
  name: string;
  type: PartnerType;
  phone: string;
  email: string;
  address: string;
  soldeInitial: number;
  soldeActuel: number;
  notes: string;
  rc: string;
  nif: string;
  ai: string;
  articleImposition: string;
  nis: string;
  rib: string;
}

export interface ArticleCompositionEntity {
  id: string;
  parentProductId: string;
  componentName: string;
  quantite: number;
  prixUnitaireEstime: number;
  unit: string;
}

export interface ProductCatalogEntity {
  id: string;
  userId?: string;
  name: string;
  defaultUnitPrice: number;
  category: string;
  marque: string;
  unit: string;
  stockActuel: number;
  seuilAlerte: number;
  isArticleEclate: boolean;
  isArticleKit: boolean;
  prixAchatMoyen: number;
  barcode: string;
  reference: string;
  composantes?: ArticleCompositionEntity[];
}

export type StockMovementType = 'ENTREE' | 'SORTIE' | 'AJUSTEMENT' | 'RETOUR';

export interface StockMovementEntity {
  id: string;
  productId: string;
  productName: string;
  type: StockMovementType;
  quantity: number;
  previousStock: number;
  newStock: number;
  unit: string;
  operationId: string;
  partnerName: string;
  reason: string;
  timestamp: number;
}

export type EmployeeRole = 'ADMIN' | 'GERANT' | 'VENDEUR' | 'CAISSIER';

export interface EmployeeEntity {
  id: string;
  name: string;
  email: string;
  password: string;
  pinCode?: string;
  role: EmployeeRole;
  isActive?: boolean;
  canModifyBon: boolean;
  canDeleteBon: boolean;
  canModifyFacture: boolean;
  canDeleteFacture: boolean;
  canModifyCreance: boolean;
  canDeleteCreance: boolean;
  canManageEmployees: boolean;
}

export interface CompanySettings {
  companyName: string;
  address: string;
  phone: string;
  email: string;
  rc: string;
  nif: string;
  ai: string;
  nis: string;
  rib: string;
  customLogoDataUrl: string;
  customStampDataUrl: string;
}

export type OcrFieldType = 'QTS' | 'REF' | 'DESIGNATION' | 'PU_HT' | 'IGNORE';

export interface DashboardAnalytics {
  totalSalesTtc: number;
  totalPurchasesTtc: number;
  totalCollected: number;
  totalDisbursed: number;
  totalClientReceivables: number;
  totalSupplierPayables: number;
  estimatedNetProfit: number;
  lowStockProductsCount: number;
}
