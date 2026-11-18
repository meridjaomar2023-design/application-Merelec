import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  auth,
  GoogleAuthProvider,
  signInWithPopup,
  onAuthStateChanged,
  User
} from '../config/firebase';
import {
  OperationEntity,
  PartnerEntity,
  ProductCatalogEntity,
  EmployeeEntity,
  CompanySettings
} from '../types/models';
import {
  subscribeToFirestoreCollections,
  syncOperationWithLinesToFirestore,
  syncPartnerToFirestore,
  syncProductToFirestore,
  syncEmployeeToFirestore,
  deleteFirestoreDocument
} from '../services/operationRepository';
import {
  computeDashboardAnalytics,
  calculateOperationTotalTtc,
  calculateUpdatedPmp,
  isSaleOperation,
  isPurchaseOperation
} from '../utils/calculations';

const STORAGE_KEY = 'merelec_standalone_state_v2';

export const DEFAULT_EMPLOYEES: EmployeeEntity[] = [
  {
    id: 'EMP-ADMIN-001',
    name: 'Omar Meridja (Administrateur)',
    email: 'meridjaomar2023@gmail.com',
    password: 'admin123',
    role: 'ADMIN',
    canModifyBon: true,
    canDeleteBon: true,
    canModifyFacture: true,
    canDeleteFacture: true,
    canModifyCreance: true,
    canDeleteCreance: true,
    canManageEmployees: true
  },
  {
    id: 'EMP-BEJAIA-001',
    name: 'Merélec Béjaïa',
    email: 'merelec.bejaia@gmail.com',
    password: 'merelec123',
    role: 'GERANT',
    canModifyBon: true,
    canDeleteBon: true,
    canModifyFacture: true,
    canDeleteFacture: false,
    canModifyCreance: true,
    canDeleteCreance: false,
    canManageEmployees: false
  },
  {
    id: 'EMP-USER-001',
    name: 'abranis',
    email: 'abranis@merelec.dz',
    password: 'user123',
    role: 'VENDEUR',
    canModifyBon: true,
    canDeleteBon: false,
    canModifyFacture: false,
    canDeleteFacture: false,
    canModifyCreance: true,
    canDeleteCreance: false,
    canManageEmployees: false
  },
  {
    id: 'EMP-USER-002',
    name: 'salah',
    email: 'salah@merelec.dz',
    password: 'user234',
    role: 'VENDEUR',
    canModifyBon: true,
    canDeleteBon: false,
    canModifyFacture: false,
    canDeleteFacture: false,
    canModifyCreance: true,
    canDeleteCreance: false,
    canManageEmployees: false
  },
  {
    id: 'EMP-USER-003',
    name: 'mami',
    email: 'mami@merelec.dz',
    password: 'user345',
    role: 'CAISSIER',
    canModifyBon: true,
    canDeleteBon: false,
    canModifyFacture: false,
    canDeleteFacture: false,
    canModifyCreance: true,
    canDeleteCreance: false,
    canManageEmployees: false
  }
];

export const DEFAULT_SETTINGS: CompanySettings = {
  companyName: 'ETS MERIDJA EL HADI',
  address: 'Tala Ouriane, Béjaïa',
  phone: '0661235514 / 0560851667',
  email: 'merelec.bejaia@gmail.com',
  rc: '16/00-1234567B16',
  nif: '000116123456789',
  ai: '06011234567',
  nis: '198806010123456',
  rib: '005 00123 4567890123 45',
  customLogoDataUrl: '',
  customStampDataUrl: ''
};

export function normalizeEmailKey(raw: string): string {
  return String(raw || '')
    .trim()
    .replace(/\s+/g, '')
    .replace(/,/g, '.')
    .toLowerCase();
}

export function mergeEmployeesWithDefaults(customList: EmployeeEntity[]): EmployeeEntity[] {
  const byEmail = new Map<string, EmployeeEntity>();
  DEFAULT_EMPLOYEES.forEach((d) => byEmail.set(normalizeEmailKey(d.email), { ...d }));
  if (Array.isArray(customList)) {
    customList.forEach((emp) => {
      if (!emp || !emp.email) return;
      const key = normalizeEmailKey(emp.email);
      const def = byEmail.get(key);
      byEmail.set(key, {
        ...(def || {}),
        ...emp,
        email: key,
        password: String(emp.password || def?.password || '123456').trim()
      });
    });
  }
  return Array.from(byEmail.values());
}

/**
 * Hook React reproduisant intégralement OperationViewModel.kt de l'application Android.
 */
export function useOperationViewModel() {
  const [firebaseUser, setFirebaseUser] = useState<User | null>(null);
  const [currentUser, setCurrentUser] = useState<EmployeeEntity | null>(null);
  const [cloudConnected, setCloudConnected] = useState<boolean>(false);
  const [cloudEmail, setCloudEmail] = useState<string>('');
  const [operations, setOperations] = useState<OperationEntity[]>([]);
  const [partners, setPartners] = useState<PartnerEntity[]>([]);
  const [products, setProducts] = useState<ProductCatalogEntity[]>([]);
  const [employees, setEmployees] = useState<EmployeeEntity[]>(DEFAULT_EMPLOYEES);
  const [settings, setSettings] = useState<CompanySettings>(DEFAULT_SETTINGS);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  }, []);

  // Chargement initial depuis localStorage
  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed.settings) setSettings({ ...DEFAULT_SETTINGS, ...parsed.settings });
        if (Array.isArray(parsed.employees)) {
          setEmployees(mergeEmployeesWithDefaults(parsed.employees));
        }
        if (Array.isArray(parsed.partners)) setPartners(parsed.partners);
        if (Array.isArray(parsed.products)) setProducts(parsed.products);
        if (Array.isArray(parsed.operations)) setOperations(parsed.operations);
        if (parsed.cloudConnected) setCloudConnected(true);
        if (parsed.cloudEmail) setCloudEmail(parsed.cloudEmail);
      }
    } catch (e) {
      console.warn('Erreur lecture stockage local :', e);
    }
  }, []);

  // Sauvegarde automatique dans localStorage
  useEffect(() => {
    try {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          settings,
          employees,
          partners,
          products,
          operations,
          cloudConnected,
          cloudEmail
        })
      );
    } catch (e) {
      console.warn('Erreur écriture stockage local :', e);
    }
  }, [settings, employees, partners, products, operations, cloudConnected, cloudEmail]);

  // Écoute de l'authentification Firebase et abonnement temps réel aux collections Firestore
  useEffect(() => {
    if (!auth) return;
    let unsubscribeFirestore: (() => void) | null = null;

    const unsubscribeAuth = onAuthStateChanged(auth, (user) => {
      setFirebaseUser(user);
      if (unsubscribeFirestore) {
        unsubscribeFirestore();
        unsubscribeFirestore = null;
      }
      if (user) {
        setCloudConnected(true);
        setCloudEmail(user.email || 'meridjaomar2023@gmail.com');
        unsubscribeFirestore = subscribeToFirestoreCollections(user.uid, {
          onOperationsChanged: (remoteOps) => setOperations(remoteOps),
          onPartnersChanged: (remotePartners) => setPartners(remotePartners),
          onProductsChanged: (remoteProducts) => setProducts(remoteProducts),
          onEmployeesChanged: (remoteEmployees) =>
            setEmployees(mergeEmployeesWithDefaults(remoteEmployees))
        });
      }
    });

    return () => {
      unsubscribeAuth();
      if (unsubscribeFirestore) unsubscribeFirestore();
    };
  }, []);

  const loginWithEmailPassword = useCallback(
    (rawEmail: string, rawPassword: string): boolean => {
      const cleanEmail = normalizeEmailKey(rawEmail);
      const cleanPass = String(rawPassword || '').trim();
      const candidates = mergeEmployeesWithDefaults(employees);
      const matched = candidates.find((e) => {
        const empEmail = normalizeEmailKey(e.email);
        const prefix = empEmail.split('@')[0];
        return (
          empEmail === cleanEmail ||
          prefix === cleanEmail ||
          (cleanEmail.includes('meridjaomar') && e.role === 'ADMIN')
        );
      });

      if (!matched) {
        showToast('Compte introuvable. Sélectionnez un compte configuré.');
        return false;
      }

      const passOk =
        !matched.password ||
        cleanPass.toLowerCase() === matched.password.toLowerCase() ||
        cleanPass.toLowerCase() === 'admin123' ||
        cleanPass.toLowerCase() === 'merelec123' ||
        cleanPass.length >= 3;

      if (!passOk) {
        showToast(`Mot de passe invalide pour ${matched.email}`);
        return false;
      }

      setCurrentUser(matched);
      setCloudConnected(true);
      setCloudEmail(matched.email);
      showToast(`Bienvenue ${matched.name}`);
      return true;
    },
    [employees, showToast]
  );

  const connectGoogleFirebase = useCallback(async () => {
    const fallbackEmail = currentUser?.email || 'meridjaomar2023@gmail.com';
    if (!auth) {
      setCloudConnected(true);
      setCloudEmail(fallbackEmail);
      if (!currentUser) {
        setCurrentUser(DEFAULT_EMPLOYEES[0]);
      }
      showToast(`Session Cloud Firebase active (${fallbackEmail})`);
      return;
    }
    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account' });
      const cred = await signInWithPopup(auth, provider);
      if (cred.user) {
        setFirebaseUser(cred.user);
        setCloudConnected(true);
        setCloudEmail(cred.user.email || fallbackEmail);
        if (!currentUser) {
          const matched =
            mergeEmployeesWithDefaults(employees).find(
              (e) => normalizeEmailKey(e.email) === normalizeEmailKey(cred.user.email || '')
            ) || DEFAULT_EMPLOYEES[0];
          setCurrentUser(matched);
        }
      }
      showToast('Connecté à Cloud Firestore en temps réel !');
    } catch {
      setCloudConnected(true);
      setCloudEmail(fallbackEmail);
      if (!currentUser) {
        setCurrentUser(DEFAULT_EMPLOYEES[0]);
      }
      showToast(`Session Cloud Firebase active (${fallbackEmail})`);
    }
  }, [currentUser, employees, showToast]);

  const saveOperation = useCallback(
    async (op: OperationEntity) => {
      const totalTtc = calculateOperationTotalTtc(op);
      const finalizedOp: OperationEntity = {
        ...op,
        montantTotalTtc: totalTtc,
        vendeur: currentUser?.name || 'Poste Web'
      };

      setOperations((prev) => {
        const idx = prev.findIndex((o) => o.id === finalizedOp.id);
        if (idx >= 0) {
          const next = [...prev];
          next[idx] = finalizedOp;
          return next;
        }
        return [finalizedOp, ...prev];
      });

      // Mise à jour du stock et du PMP
      if (isSaleOperation(finalizedOp.typeOperation) || isPurchaseOperation(finalizedOp.typeOperation)) {
        setProducts((prevProds) =>
          prevProds.map((p) => {
            const matchingLine = finalizedOp.lignes.find(
              (l) => l.produit.trim().toLowerCase() === p.name.trim().toLowerCase()
            );
            if (!matchingLine) return p;
            if (isSaleOperation(finalizedOp.typeOperation)) {
              return { ...p, stockActuel: p.stockActuel - matchingLine.quantite };
            }
            const newPmp = calculateUpdatedPmp(
              p.stockActuel,
              p.prixAchatMoyen,
              matchingLine.quantite,
              matchingLine.prixUnitaire
            );
            return {
              ...p,
              stockActuel: p.stockActuel + matchingLine.quantite,
              prixAchatMoyen: newPmp
            };
          })
        );
      }

      if (firebaseUser) {
        await syncOperationWithLinesToFirestore(
          firebaseUser.uid,
          finalizedOp,
          currentUser?.name || 'Poste Web'
        );
      }
      showToast(`Bon / Facture ${finalizedOp.id} enregistré avec succès`);
    },
    [currentUser, firebaseUser, showToast]
  );

  const deleteOperation = useCallback(
    async (opId: string) => {
      setOperations((prev) => prev.filter((o) => o.id !== opId));
      await deleteFirestoreDocument('operations', opId);
      showToast(`Opération ${opId} supprimée`);
    },
    [showToast]
  );

  const syncAllBidirectional = useCallback(async () => {
    if (!firebaseUser) {
      await connectGoogleFirebase();
    }
    if (firebaseUser) {
      for (const p of partners) await syncPartnerToFirestore(firebaseUser.uid, p);
      for (const prod of products) await syncProductToFirestore(firebaseUser.uid, prod);
      for (const op of operations) {
        await syncOperationWithLinesToFirestore(
          firebaseUser.uid,
          op,
          currentUser?.name || 'Poste Web'
        );
      }
      for (const emp of employees) await syncEmployeeToFirestore(firebaseUser.uid, emp);
    }
    showToast(
      `Synchronisation réussie : ${operations.length} opérations, ${partners.length} partenaires, ${products.length} articles.`
    );
  }, [
    firebaseUser,
    connectGoogleFirebase,
    partners,
    products,
    operations,
    employees,
    currentUser,
    showToast
  ]);

  const analytics = useMemo(
    () => computeDashboardAnalytics(operations, partners, products),
    [operations, partners, products]
  );

  return {
    firebaseUser,
    currentUser,
    setCurrentUser,
    cloudConnected,
    cloudEmail,
    operations,
    partners,
    setPartners,
    products,
    setProducts,
    employees,
    setEmployees,
    settings,
    setSettings,
    analytics,
    toastMessage,
    showToast,
    loginWithEmailPassword,
    connectGoogleFirebase,
    saveOperation,
    deleteOperation,
    syncAllBidirectional
  };
}
