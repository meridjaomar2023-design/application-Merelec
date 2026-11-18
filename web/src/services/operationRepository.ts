import {
  collection,
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  onSnapshot,
  query,
  where,
  serverTimestamp,
  Unsubscribe
} from 'firebase/firestore';
import { db } from '../config/firebase';
import {
  OperationEntity,
  LigneOperationEntity,
  PartnerEntity,
  ProductCatalogEntity,
  EmployeeEntity
} from '../types/models';
import { calculateOperationTotalTtc, isImportedCreanceOperation } from '../utils/calculations';

/**
 * Service de synchronisation bidirectionnelle Cloud Firestore strictement conforme
 * à /firestore.rules et compatible à 100% avec OperationRepository.kt sur Android.
 */
export function sanitizeDocId(rawId: string): string {
  const cleaned = String(rawId || '')
    .trim()
    .replace(/\//g, '_')
    .replace(/\s+/g, '_');
  return cleaned.slice(0, 120) || `DOC_${Date.now()}`;
}

export function stripUidPrefix(rawId: string): string {
  return String(rawId || '').replace(/^[^_]+_/, '');
}

export async function upsertFirestoreDocument(
  colName: string,
  docId: string,
  uid: string,
  cleanFields: Record<string, unknown>
): Promise<void> {
  if (!db || !uid) return;
  const cleanId = sanitizeDocId(docId);
  const ref = doc(db, colName, cleanId);
  const snap = await getDoc(ref);
  if (snap.exists()) {
    await updateDoc(ref, {
      ...cleanFields,
      userId: uid,
      updatedAt: serverTimestamp()
    });
  } else {
    await setDoc(ref, {
      ...cleanFields,
      userId: uid,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  }
}

export async function syncPartnerToFirestore(uid: string, p: PartnerEntity): Promise<void> {
  await upsertFirestoreDocument('partners', p.id, uid, {
    name: String(p.name || 'Partenaire').trim().slice(0, 200) || 'Partenaire',
    type: p.type === 'FOURNISSEUR' ? 'FOURNISSEUR' : 'CLIENT',
    phone: String(p.phone || '').slice(0, 60),
    address: String(p.address || '').slice(0, 300),
    solde: Number(p.soldeActuel ?? p.soldeInitial ?? 0) || 0,
    notes: String(p.notes || '').slice(0, 1000),
    rc: String(p.rc || '').slice(0, 100),
    articleImposition: String(p.articleImposition || p.ai || '').slice(0, 100),
    nif: String(p.nif || '').slice(0, 100),
    nis: String(p.nis || '').slice(0, 100),
    rib: String(p.rib || '').slice(0, 120)
  });
}

export async function syncProductToFirestore(
  uid: string,
  prod: ProductCatalogEntity
): Promise<void> {
  await upsertFirestoreDocument('products', prod.id, uid, {
    name: String(prod.name || 'Article').trim().slice(0, 200) || 'Article',
    reference: String(prod.reference || '').slice(0, 100),
    barcode: String(prod.barcode || '').slice(0, 100),
    category: String(prod.category || 'Général').slice(0, 100),
    unit: String(prod.unit || 'U').trim().slice(0, 30) || 'U',
    defaultUnitPrice: Math.max(0, Number(prod.defaultUnitPrice) || 0),
    prixAchatMoyen: Math.max(0, Number(prod.prixAchatMoyen) || 0),
    stockActuel: Number(prod.stockActuel) || 0,
    seuilAlerte: Math.max(0, Number(prod.seuilAlerte ?? 5) || 0),
    isArticleEclate: Boolean(prod.isArticleEclate),
    isArticleKit: Boolean(prod.isArticleKit)
  });
}

export async function syncOperationWithLinesToFirestore(
  uid: string,
  op: OperationEntity,
  sellerName: string
): Promise<void> {
  const opDocId = sanitizeDocId(op.id);
  const totalTtc = calculateOperationTotalTtc(op);

  await upsertFirestoreDocument('operations', opDocId, uid, {
    dateTime: Math.max(1, Math.floor(Number(op.dateTime) || Date.now())),
    typeOperation: String(op.typeOperation || 'BL_CLIENT').trim().slice(0, 50) || 'BL_CLIENT',
    clientFournisseur:
      String(op.clientFournisseur || 'Client Comptoir').trim().slice(0, 200) ||
      'Client Comptoir',
    montantVerse: Math.max(0, Number(op.montantVerse) || 0),
    montantTotalTtc: Math.max(0, totalTtc),
    modeReglement: String(op.modeReglement || 'Espèces').trim().slice(0, 60) || 'Espèces',
    vendeur: String(op.vendeur || sellerName || 'Poste Web').slice(0, 150),
    notes: String(op.notes || '').slice(0, 1000),
    appliquerTva: Boolean(op.appliquerTva),
    tauxTva: Math.max(0, Number(op.tauxTva ?? 19) || 0),
    statut: String(op.statut || 'Validé').trim().slice(0, 60) || 'Validé'
  });

  if (Array.isArray(op.lignes)) {
    for (let i = 0; i < op.lignes.length; i++) {
      const l = op.lignes[i];
      const lineId = sanitizeDocId(l.id || `${opDocId}-L${i + 1}`);
      await upsertFirestoreDocument('operation_lines', lineId, uid, {
        idOperation: opDocId,
        produit: String(l.produit || 'Article').trim().slice(0, 200) || 'Article',
        quantite: Math.max(0.001, Number(l.quantite) || 1),
        prixUnitaire: Math.max(0, Number(l.prixUnitaire) || 0),
        parentArticle: String(l.parentArticle || '').slice(0, 200)
      });
    }
  }
}

export async function syncEmployeeToFirestore(
  uid: string,
  emp: EmployeeEntity
): Promise<void> {
  const cleanEmail = String(emp.email || 'user@merelec.dz').trim().slice(0, 150);
  const cleanPass = String(emp.password || '123456').trim().slice(0, 150);
  await upsertFirestoreDocument('employees', emp.id, uid, {
    name: String(emp.name || 'Employé').trim().slice(0, 150) || 'Employé',
    email: cleanEmail.length >= 3 ? cleanEmail : 'user@merelec.dz',
    password: cleanPass.length >= 3 ? cleanPass : '123456',
    role: emp.role || 'VENDEUR',
    isActive: emp.isActive !== false,
    canModifyBon: Boolean(emp.canModifyBon),
    canDeleteBon: Boolean(emp.canDeleteBon),
    canModifyFacture: Boolean(emp.canModifyFacture),
    canDeleteFacture: Boolean(emp.canDeleteFacture),
    canModifyCreance: Boolean(emp.canModifyCreance),
    canDeleteCreance: Boolean(emp.canDeleteCreance),
    canManageEmployees: Boolean(emp.canManageEmployees)
  });
}

export async function deleteFirestoreDocument(
  colName: string,
  docId: string
): Promise<void> {
  if (!db) return;
  try {
    await deleteDoc(doc(db, colName, sanitizeDocId(docId)));
  } catch (e) {
    console.warn(`Suppression Firestore (${colName}/${docId}) ignorée:`, e);
  }
}

export interface RealtimeListenersCallbacks {
  onOperationsChanged: (ops: OperationEntity[]) => void;
  onPartnersChanged: (partners: PartnerEntity[]) => void;
  onProductsChanged: (products: ProductCatalogEntity[]) => void;
  onEmployeesChanged: (employees: EmployeeEntity[]) => void;
}

export function subscribeToFirestoreCollections(
  uid: string,
  callbacks: RealtimeListenersCallbacks
): () => void {
  if (!db || !uid) return () => {};
  const unsubscribes: Unsubscribe[] = [];
  const remoteOpsMap = new Map<string, OperationEntity>();
  const remoteLinesByOpId = new Map<string, LigneOperationEntity[]>();

  const emitMergedOperations = () => {
    const merged: OperationEntity[] = [];
    remoteOpsMap.forEach((op, opId) => {
      if (isImportedCreanceOperation(op)) return;
      const lines =
        remoteLinesByOpId.get(opId) ||
        remoteLinesByOpId.get(stripUidPrefix(opId)) ||
        op.lignes ||
        [];
      merged.push({
        ...op,
        id: opId,
        lignes: lines
      });
    });
    merged.sort((a, b) => (b.dateTime || 0) - (a.dateTime || 0));
    callbacks.onOperationsChanged(merged);
  };

  unsubscribes.push(
    onSnapshot(
      query(collection(db, 'operations'), where('userId', '==', uid)),
      (snap) => {
        remoteOpsMap.clear();
        snap.docs.forEach((d) => {
          remoteOpsMap.set(d.id, { id: d.id, ...(d.data() as Omit<OperationEntity, 'id'>) });
        });
        emitMergedOperations();
      },
      () => {}
    )
  );

  unsubscribes.push(
    onSnapshot(
      query(collection(db, 'operation_lines'), where('userId', '==', uid)),
      (snap) => {
        remoteLinesByOpId.clear();
        snap.docs.forEach((d) => {
          const data = d.data();
          const opId = String(data.idOperation || '').trim();
          if (!opId) return;
          const lineObj: LigneOperationEntity = {
            id: d.id,
            idOperation: opId,
            produit: String(data.produit || 'Article'),
            quantite: Number(data.quantite) || 1,
            prixUnitaire: Number(data.prixUnitaire) || 0,
            parentArticle: String(data.parentArticle || '')
          };
          if (!remoteLinesByOpId.has(opId)) remoteLinesByOpId.set(opId, []);
          remoteLinesByOpId.get(opId)!.push(lineObj);
          const stripped = stripUidPrefix(opId);
          if (stripped !== opId) {
            if (!remoteLinesByOpId.has(stripped)) remoteLinesByOpId.set(stripped, []);
            remoteLinesByOpId.get(stripped)!.push(lineObj);
          }
        });
        if (remoteOpsMap.size > 0) emitMergedOperations();
      },
      () => {}
    )
  );

  unsubscribes.push(
    onSnapshot(
      query(collection(db, 'partners'), where('userId', '==', uid)),
      (snap) => {
        if (!snap.empty) {
          callbacks.onPartnersChanged(
            snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<PartnerEntity, 'id'>) }))
          );
        }
      },
      () => {}
    )
  );

  unsubscribes.push(
    onSnapshot(
      query(collection(db, 'products'), where('userId', '==', uid)),
      (snap) => {
        callbacks.onProductsChanged(
          snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<ProductCatalogEntity, 'id'>) }))
        );
      },
      () => {}
    )
  );

  unsubscribes.push(
    onSnapshot(
      query(collection(db, 'employees'), where('userId', '==', uid)),
      (snap) => {
        if (!snap.empty) {
          callbacks.onEmployeesChanged(
            snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<EmployeeEntity, 'id'>) }))
          );
        }
      },
      () => {}
    )
  );

  return () => {
    unsubscribes.forEach((u) => u());
  };
}
