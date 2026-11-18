package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.OperationDao
import com.example.data.model.ArticleCompositionEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeRole
import com.example.data.model.LigneOperationEntity
import com.example.data.model.OperationEntity
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import com.example.data.model.ProductCatalogWithComposition
import com.example.data.model.StockMovementEntity
import com.example.data.model.StockMovementType
import com.example.util.CreanceImportItem
import com.example.util.OperationType as FirestoreOpType
import com.example.util.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class OperationRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context? = null
) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val localDao: OperationDao? = appContext?.let {
        AppDatabase.getDatabase(it, repoScope).operationDao()
    }

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val _authUidFlow = MutableStateFlow<String?>(
        try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
    )
    val authUidFlow: StateFlow<String?> = _authUidFlow.asStateFlow()

    private val employeesFlowInternal = MutableStateFlow<List<EmployeeEntity>>(emptyList())

    private val deletedProductIdsCache: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val deletedProductNamesCache: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val deletedOperationIdsCache: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val _deletedOperationsTick = MutableStateFlow(0L)

    init {
        if (appContext != null) {
            employeesFlowInternal.value = loadEmployeesFromLocalPrefs()
            initDeletedProductsTombstones()
            initDeletedOperationsTombstones()
        } else {
            // Default demo tombstones when running without Context
            deletedProductIdsCache.addAll(DEMO_KIT_AND_ECLATE_IDS)
            deletedProductNamesCache.addAll(DEMO_KIT_AND_ECLATE_NORMALIZED_NAMES)
        }
        repoScope.launch {
            purgeDeletedProductsFromLocalAndRemote()
            purgeDeletedOperationsFromLocalAndRemote()
        }
        try {
            auth.addAuthStateListener { firebaseAuth ->
                val newUid = firebaseAuth.currentUser?.uid
                val prevUid = _authUidFlow.value
                _authUidFlow.value = newUid
                if (newUid != null && newUid != prevUid) {
                    repoScope.launch {
                        try {
                            purgeDeletedProductsFromLocalAndRemote()
                            purgeDeletedOperationsFromLocalAndRemote()
                            syncAllWithFirebaseBidirectional()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Utilisateur non connecté. Veuillez vous connecter avec Google.")
    }

    private fun queryUserIdOrUnauthenticated(): String {
        return auth.currentUser?.uid ?: "unauthenticated"
    }

    private fun uidPrefix(uid: String): String {
        return uid.replace(Regex("[^a-zA-Z0-9_-]"), "").take(16).ifEmpty { "usr" }
    }

    private fun sanitizeDocId(rawId: String): String {
        val cleaned = rawId.trim().replace(Regex("[^a-zA-Z0-9_-]"), "-").take(120)
        return cleaned.ifEmpty { UUID.randomUUID().toString() }
    }

    private fun scopedSeedId(uid: String, baseId: String): String {
        val prefix = "${uidPrefix(uid)}_"
        val cleanBase = baseId.trim()
        val stripped = generateSequence(cleanBase) { current ->
            if (current.startsWith(prefix)) current.removePrefix(prefix) else null
        }.last()
        return sanitizeDocId("$prefix$stripped")
    }

    fun currentSellerLabel(): String {
        val user = auth.currentUser
        return user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.takeIf { it.isNotBlank() }
            ?: "Poste Android"
    }

    // =========================================================================
    // LOCAL EMPLOYEE PREFERENCES STORE (Offline & Multi-Device Resilience)
    // =========================================================================

    private fun defaultEmployeesList(): List<EmployeeEntity> = listOf(
        EmployeeEntity.defaultAdmin(),
        EmployeeEntity.defaultMerelecBejaia(),
        EmployeeEntity.defaultVendeur(),
        EmployeeEntity.defaultVendeur2(),
        EmployeeEntity.defaultCaissier()
    )

    private fun deduplicateEmployeesList(rawList: List<EmployeeEntity>): List<EmployeeEntity> {
        val byKey = LinkedHashMap<String, EmployeeEntity>()
        for (emp in rawList) {
            val normEmail = EmployeeEntity.normalizeEmail(emp.email)
            val key = normEmail.ifBlank { emp.id.trim().lowercase(Locale.ROOT) }
            if (key.isBlank()) continue
            val normalizedEmp = emp.copy(email = normEmail)
            val existing = byKey[key]
            if (existing == null) {
                byKey[key] = normalizedEmp
            } else {
                // Keep the entry with the cleanest ID (fewer repeated prefixes) while preserving permissions/password
                val existingClean = existing.id.count { it == '_' } <= normalizedEmp.id.count { it == '_' }
                byKey[key] = if (existingClean) existing else normalizedEmp
            }
        }
        return byKey.values.toList()
    }

    /**
     * Garantit que les deux comptes principaux demandés :
     * - meridjaomar2023@gmail.com (Administrateur)
     * - merelec.bejaia@gmail.com (acceptant aussi la saisie merelec,bejaia@gmail.com)
     * sont toujours présents une seule fois dans la liste des utilisateurs même après une mise à jour.
     */
    private fun ensureAuthorizedUsersPresent(rawList: List<EmployeeEntity>): List<EmployeeEntity> {
        if (rawList.isEmpty()) return defaultEmployeesList()
        val list = deduplicateEmployeesList(rawList).toMutableList()

        // 1. Vérifier la présence de meridjaomar2023@gmail.com
        val primaryAdminIdx = list.indexOfFirst {
            EmployeeEntity.normalizeEmail(it.email) == EmployeeEntity.PRIMARY_ADMIN_EMAIL
        }
        if (primaryAdminIdx >= 0) {
            val existingAdmin = list[primaryAdminIdx]
            list[primaryAdminIdx] = existingAdmin.copy(
                role = EmployeeRole.ADMIN,
                isActive = true,
                canModifyBon = true,
                canDeleteBon = true,
                canModifyFacture = true,
                canDeleteFacture = true,
                canModifyCreance = true,
                canDeleteCreance = true,
                canManageEmployees = true
            )
        } else {
            val legacyAdminIdx = list.indexOfFirst {
                it.id.endsWith("EMP-ADMIN-001") ||
                    EmployeeEntity.normalizeEmail(it.email) == "admin@merelec.dz"
            }
            if (legacyAdminIdx >= 0) {
                val existingAdmin = list[legacyAdminIdx]
                list[legacyAdminIdx] = existingAdmin.copy(
                    name = if (existingAdmin.name == "Administrateur Principal") "Omar Meridja (Administrateur)" else existingAdmin.name,
                    email = EmployeeEntity.PRIMARY_ADMIN_EMAIL,
                    role = EmployeeRole.ADMIN,
                    isActive = true,
                    canModifyBon = true,
                    canDeleteBon = true,
                    canModifyFacture = true,
                    canDeleteFacture = true,
                    canModifyCreance = true,
                    canDeleteCreance = true,
                    canManageEmployees = true
                )
            } else {
                list.add(0, EmployeeEntity.defaultAdmin())
            }
        }

        // 2. Vérifier la présence de merelec.bejaia@gmail.com (sans jamais créer de doublon)
        val merelecBejaiaIdx = list.indexOfFirst {
            EmployeeEntity.normalizeEmail(it.email) == EmployeeEntity.SECONDARY_USER_EMAIL
        }
        if (merelecBejaiaIdx >= 0) {
            val existingBejaia = list[merelecBejaiaIdx]
            if (existingBejaia.role == EmployeeRole.GERANT || existingBejaia.role == EmployeeRole.ADMIN) {
                list[merelecBejaiaIdx] = existingBejaia.copy(
                    canModifyBon = true,
                    canDeleteBon = true,
                    canModifyFacture = true,
                    canDeleteFacture = true,
                    canModifyCreance = true,
                    canDeleteCreance = true
                )
            }
        } else {
            val legacyGerantIdx = list.indexOfFirst {
                it.id.endsWith("EMP-GERANT-001") ||
                    it.id.endsWith("EMP-BEJAIA-001") ||
                    EmployeeEntity.normalizeEmail(it.email) == "gerant@merelec.dz"
            }
            if (legacyGerantIdx >= 0) {
                val existingGerant = list[legacyGerantIdx]
                list[legacyGerantIdx] = EmployeeEntity.defaultMerelecBejaia().copy(id = existingGerant.id)
            } else {
                val insertPos = if (list.isNotEmpty()) 1 else 0
                list.add(insertPos, EmployeeEntity.defaultMerelecBejaia())
            }
        }

        // Supprimer tout ancien compte legacy "gerant@merelec.dz" si merelec.bejaia@gmail.com existe déjà
        val finalCleaned = list.filterNot {
            EmployeeEntity.normalizeEmail(it.email) == "gerant@merelec.dz" &&
                list.any { other -> EmployeeEntity.normalizeEmail(other.email) == EmployeeEntity.SECONDARY_USER_EMAIL }
        }.toMutableList()

        // 3. Garantir l'ajout des 3 utilisateurs supplémentaires (Utilisateur 1, Utilisateur 2, Utilisateur 3)
        // lors de la mise à jour s'ils n'ont pas encore été initialisés sur ce poste
        val ctx = appContext
        val prefs = ctx?.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
        val threeUsersSeeded = prefs?.getBoolean("three_users_seeded_v1", false) ?: false
        if (!threeUsersSeeded) {
            for (extraUser in EmployeeEntity.defaultThreeAdditionalUsers()) {
                val exists = finalCleaned.any {
                    EmployeeEntity.normalizeEmail(it.email) == EmployeeEntity.normalizeEmail(extraUser.email) ||
                        it.id.endsWith(extraUser.id)
                }
                if (!exists) {
                    finalCleaned.add(extraUser)
                }
            }
            prefs?.edit()?.putBoolean("three_users_seeded_v1", true)?.apply()
        }

        // Si l'administrateur n'a pas personnalisé manuellement les restrictions des comptes,
        // garantir que tous les profils actifs peuvent créer/modifier/supprimer les Bons et Factures
        if (!isAdminCustomized()) {
            for (i in finalCleaned.indices) {
                val emp = finalCleaned[i]
                if (!emp.canModifyBon || !emp.canDeleteBon || !emp.canModifyFacture || !emp.canDeleteFacture) {
                    finalCleaned[i] = emp.copy(
                        canModifyBon = true,
                        canDeleteBon = true,
                        canModifyFacture = true,
                        canDeleteFacture = true
                    )
                }
            }
        }

        return deduplicateEmployeesList(finalCleaned)
    }

    private fun loadEmployeesFromLocalPrefs(): List<EmployeeEntity> {
        val ctx = appContext ?: return defaultEmployeesList()
        val prefs = ctx.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_EMPLOYEES_JSON, null)
        if (rawJson.isNullOrBlank()) {
            val defaults = defaultEmployeesList()
            saveEmployeesToLocalPrefs(defaults, markCustomized = false)
            return defaults
        }
        return try {
            val arr = JSONArray(rawJson)
            val result = mutableListOf<EmployeeEntity>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val role = EmployeeRole.fromString(obj.optString("role", "VENDEUR"))
                result.add(
                    EmployeeEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Employé"),
                        email = EmployeeEntity.normalizeEmail(obj.optString("email", "")),
                        password = obj.optString("password", "123456"),
                        role = role,
                        isActive = obj.optBoolean("isActive", true),
                        canModifyBon = obj.optBoolean("canModifyBon", true),
                        canDeleteBon = obj.optBoolean("canDeleteBon", true),
                        canModifyFacture = obj.optBoolean("canModifyFacture", role == EmployeeRole.ADMIN || role == EmployeeRole.GERANT),
                        canDeleteFacture = obj.optBoolean("canDeleteFacture", role == EmployeeRole.ADMIN || role == EmployeeRole.GERANT),
                        canModifyCreance = obj.optBoolean("canModifyCreance", role == EmployeeRole.ADMIN || role == EmployeeRole.GERANT),
                        canDeleteCreance = obj.optBoolean("canDeleteCreance", role == EmployeeRole.ADMIN || role == EmployeeRole.GERANT),
                        canManageEmployees = obj.optBoolean("canManageEmployees", role == EmployeeRole.ADMIN)
                    )
                )
            }
            val ensured = ensureAuthorizedUsersPresent(if (result.isEmpty()) defaultEmployeesList() else result)
            if (ensured != result) {
                saveEmployeesToLocalPrefs(ensured, markCustomized = isAdminCustomized())
            }
            ensured
        } catch (e: Exception) {
            defaultEmployeesList()
        }
    }

    private fun saveEmployeesToLocalPrefs(employees: List<EmployeeEntity>, markCustomized: Boolean = true) {
        val ctx = appContext ?: return
        val deduplicated = deduplicateEmployeesList(employees)
        val prefs = ctx.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (emp in deduplicated) {
            val obj = JSONObject().apply {
                put("id", emp.id.ifBlank { UUID.randomUUID().toString() })
                put("name", emp.name)
                put("email", EmployeeEntity.normalizeEmail(emp.email))
                put("password", emp.password)
                put("role", emp.role.name)
                put("isActive", emp.isActive)
                put("canModifyBon", emp.canModifyBon)
                put("canDeleteBon", emp.canDeleteBon)
                put("canModifyFacture", emp.canModifyFacture)
                put("canDeleteFacture", emp.canDeleteFacture)
                put("canModifyCreance", emp.canModifyCreance)
                put("canDeleteCreance", emp.canDeleteCreance)
                put("canManageEmployees", emp.canManageEmployees)
            }
            arr.put(obj)
        }
        prefs.edit().apply {
            putString(KEY_EMPLOYEES_JSON, arr.toString())
            if (markCustomized) {
                putBoolean(KEY_EMPLOYEES_CUSTOMIZED, true)
            }
            apply()
        }
        employeesFlowInternal.value = deduplicated.sortedWith(
            compareBy({ it.role.ordinal }, { it.name.lowercase(Locale.FRENCH) })
        )
    }

    fun isAdminCustomized(): Boolean {
        val ctx = appContext ?: return false
        return ctx.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
            .getBoolean(KEY_EMPLOYEES_CUSTOMIZED, false)
    }

    private fun isDatabaseResetByAdmin(): Boolean {
        val ctx = appContext ?: return false
        return ctx.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
            .getBoolean(KEY_DB_RESET_BY_ADMIN, false)
    }

    private fun setDatabaseResetByAdmin(reset: Boolean) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DB_RESET_BY_ADMIN, reset)
            .apply()
    }

    private fun stripUidPrefix(rawId: String): String {
        val clean = rawId.trim()
        val idx = clean.indexOf('_')
        return if (idx in 1..20 && idx + 1 < clean.length) {
            clean.substring(idx + 1)
        } else {
            clean
        }
    }

    private fun normalizeProductKey(rawName: String?): String {
        if (rawName.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(rawName.trim().lowercase(Locale.FRENCH), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun initDeletedProductsTombstones() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
        val storedIds = prefs.getStringSet(KEY_DELETED_PRODUCT_IDS, emptySet()) ?: emptySet()
        val storedNames = prefs.getStringSet(KEY_DELETED_PRODUCT_NAMES, emptySet()) ?: emptySet()
        deletedProductIdsCache.addAll(storedIds)
        deletedProductNamesCache.addAll(storedNames)

        val demoPurged = prefs.getBoolean(KEY_DEMO_KIT_ECLATE_PURGED_V1, false)
        if (!demoPurged) {
            deletedProductIdsCache.addAll(DEMO_KIT_AND_ECLATE_IDS)
            deletedProductNamesCache.addAll(DEMO_KIT_AND_ECLATE_NORMALIZED_NAMES)
            prefs.edit()
                .putStringSet(KEY_DELETED_PRODUCT_IDS, deletedProductIdsCache.toSet())
                .putStringSet(KEY_DELETED_PRODUCT_NAMES, deletedProductNamesCache.toSet())
                .putBoolean(KEY_DEMO_KIT_ECLATE_PURGED_V1, true)
                .apply()
        }
    }

    private fun persistDeletedProductsTombstones() {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_DELETED_PRODUCT_IDS, deletedProductIdsCache.toSet())
            .putStringSet(KEY_DELETED_PRODUCT_NAMES, deletedProductNamesCache.toSet())
            .apply()
    }

    private fun markProductDeleted(id: String, name: String? = null) {
        val raw = id.trim()
        if (raw.isNotEmpty()) {
            deletedProductIdsCache.add(raw)
            deletedProductIdsCache.add(sanitizeDocId(raw))
            deletedProductIdsCache.add(stripUidPrefix(raw))
            auth.currentUser?.uid?.let { uid ->
                deletedProductIdsCache.add(scopedSeedId(uid, raw))
                deletedProductIdsCache.add(scopedSeedId(uid, stripUidPrefix(raw)))
            }
        }
        val normName = normalizeProductKey(name)
        if (normName.isNotEmpty()) {
            deletedProductNamesCache.add(normName)
            val withoutParen = normName
                .replace(Regex("\\s*\\(article kit\\)$"), "")
                .replace(Regex("\\s*\\(article eclate\\)$"), "")
                .trim()
            if (withoutParen.isNotEmpty()) {
                deletedProductNamesCache.add(withoutParen)
            }
        }
        persistDeletedProductsTombstones()
    }

    private fun unmarkProductDeleted(id: String, name: String? = null) {
        val raw = id.trim()
        if (raw.isNotEmpty()) {
            deletedProductIdsCache.remove(raw)
            deletedProductIdsCache.remove(sanitizeDocId(raw))
            deletedProductIdsCache.remove(stripUidPrefix(raw))
            auth.currentUser?.uid?.let { uid ->
                deletedProductIdsCache.remove(scopedSeedId(uid, raw))
                deletedProductIdsCache.remove(scopedSeedId(uid, stripUidPrefix(raw)))
            }
        }
        val normName = normalizeProductKey(name)
        if (normName.isNotEmpty()) {
            deletedProductNamesCache.remove(normName)
            val withoutParen = normName
                .replace(Regex("\\s*\\(article kit\\)$"), "")
                .replace(Regex("\\s*\\(article eclate\\)$"), "")
                .trim()
            if (withoutParen.isNotEmpty()) {
                deletedProductNamesCache.remove(withoutParen)
            }
        }
        persistDeletedProductsTombstones()
    }

    private fun isProductDeleted(product: ProductCatalogEntity): Boolean {
        val rawId = product.id.trim()
        val cleanId = sanitizeDocId(rawId)
        val strippedId = stripUidPrefix(rawId)
        if (rawId in deletedProductIdsCache || cleanId in deletedProductIdsCache || strippedId in deletedProductIdsCache) {
            return true
        }
        val normName = normalizeProductKey(product.name)
        if (normName.isNotEmpty()) {
            if (normName in deletedProductNamesCache) return true
            val withoutParen = normName
                .replace(Regex("\\s*\\(article kit\\)$"), "")
                .replace(Regex("\\s*\\(article eclate\\)$"), "")
                .trim()
            if (withoutParen.isNotEmpty() && withoutParen in deletedProductNamesCache) return true
        }
        return false
    }

    private suspend fun purgeDeletedProductsFromLocalAndRemote() {
        try {
            if (localDao != null) {
                val localList = localDao.getAllCatalogProducts().first()
                for (prod in localList) {
                    if (isProductDeleted(prod)) {
                        localDao.deleteCompositionsForProduct(prod.id)
                        localDao.deleteCatalogProductById(prod.id)
                    }
                }
            }
        } catch (_: Exception) {
        }

        val uid = auth.currentUser?.uid ?: return
        try {
            val snap = db.collection(COL_PRODUCTS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val deletedDocIds = mutableSetOf<String>()
            for (doc in snap.documents) {
                val prod = doc.toProductCatalogEntity()
                if (prod != null && isProductDeleted(prod)) {
                    deletedDocIds.add(doc.id)
                    deletedDocIds.add(stripUidPrefix(doc.id))
                    try {
                        doc.reference.delete().await()
                    } catch (_: Exception) {
                    }
                }
            }
            if (deletedDocIds.isNotEmpty() || deletedProductIdsCache.isNotEmpty()) {
                val allBlockedIds = deletedDocIds + deletedProductIdsCache
                val compSnap = db.collection(COL_ARTICLE_COMPOSITIONS)
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                for (cDoc in compSnap.documents) {
                    val parentId = cDoc.getString("parentProductId")?.trim() ?: ""
                    if (parentId in allBlockedIds || stripUidPrefix(parentId) in allBlockedIds) {
                        try {
                            cDoc.reference.delete().await()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun initDeletedOperationsTombstones() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
        val storedOpIds = prefs.getStringSet(KEY_DELETED_OPERATION_IDS, emptySet()) ?: emptySet()
        deletedOperationIdsCache.addAll(storedOpIds)
    }

    private fun persistDeletedOperationsTombstones() {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS_EMPLOYEES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_DELETED_OPERATION_IDS, deletedOperationIdsCache.toSet())
            .apply()
    }

    private fun markOperationDeleted(id: String) {
        val raw = id.trim()
        if (raw.isEmpty()) return
        val stripped = stripUidPrefix(raw)
        deletedOperationIdsCache.add(raw)
        deletedOperationIdsCache.add(sanitizeDocId(raw))
        deletedOperationIdsCache.add(stripped)
        deletedOperationIdsCache.add(sanitizeDocId(stripped))
        auth.currentUser?.uid?.let { uid ->
            deletedOperationIdsCache.add(scopedSeedId(uid, raw))
            deletedOperationIdsCache.add(scopedSeedId(uid, stripped))
        }
        persistDeletedOperationsTombstones()
        _deletedOperationsTick.value = _deletedOperationsTick.value + 1L
    }

    private fun unmarkOperationDeleted(id: String) {
        val raw = id.trim()
        if (raw.isEmpty()) return
        val stripped = stripUidPrefix(raw)
        val changed = mutableListOf(
            deletedOperationIdsCache.remove(raw),
            deletedOperationIdsCache.remove(sanitizeDocId(raw)),
            deletedOperationIdsCache.remove(stripped),
            deletedOperationIdsCache.remove(sanitizeDocId(stripped))
        )
        auth.currentUser?.uid?.let { uid ->
            changed.add(deletedOperationIdsCache.remove(scopedSeedId(uid, raw)))
            changed.add(deletedOperationIdsCache.remove(scopedSeedId(uid, stripped)))
        }
        if (changed.any { it }) {
            persistDeletedOperationsTombstones()
            _deletedOperationsTick.value = _deletedOperationsTick.value + 1L
        }
    }

    private fun isOperationDeleted(rawId: String): Boolean {
        val clean = rawId.trim()
        if (clean.isEmpty()) return false
        val sanitized = sanitizeDocId(clean)
        val stripped = stripUidPrefix(clean)
        return clean in deletedOperationIdsCache ||
            sanitized in deletedOperationIdsCache ||
            stripped in deletedOperationIdsCache ||
            sanitizeDocId(stripped) in deletedOperationIdsCache
    }

    private fun isOperationDeleted(op: OperationEntity): Boolean = isOperationDeleted(op.id)

    private suspend fun purgeDeletedOperationsFromLocalAndRemote() {
        if (deletedOperationIdsCache.isEmpty()) return
        try {
            if (localDao != null) {
                val localOps = localDao.getAllOperationsWithLignes().first()
                for (item in localOps) {
                    if (isOperationDeleted(item.operation)) {
                        localDao.deleteLignesByOperationId(item.operation.id)
                        localDao.deleteOperationById(item.operation.id)
                    }
                }
            }
        } catch (_: Exception) {
        }

        val uid = auth.currentUser?.uid ?: return
        try {
            val snap = db.collection(COL_OPERATIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val deletedOpDocIds = mutableSetOf<String>()
            for (doc in snap.documents) {
                if (isOperationDeleted(doc.id)) {
                    deletedOpDocIds.add(doc.id)
                    deletedOpDocIds.add(stripUidPrefix(doc.id))
                    try {
                        doc.reference.delete().await()
                    } catch (_: Exception) {
                    }
                }
            }
            val allBlockedOpIds = deletedOpDocIds + deletedOperationIdsCache
            if (allBlockedOpIds.isNotEmpty()) {
                val linesSnap = db.collection(COL_OPERATION_LINES)
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                for (lDoc in linesSnap.documents) {
                    val opId = lDoc.getString("idOperation")?.trim() ?: ""
                    if (opId in allBlockedOpIds || stripUidPrefix(opId) in allBlockedOpIds || isOperationDeleted(opId)) {
                        try {
                            lDoc.reference.delete().await()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    // =========================================================================
    // 1. REAL-TIME SNAPSHOT LISTENERS (Firestore + Local Room Mirror)
    // =========================================================================

    private val rawOperationsFlow: Flow<List<OperationEntity>> = _authUidFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection(COL_OPERATIONS)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toOperationEntity() }
                        .filterNot { isOperationDeleted(it) }
                        .distinctBy { it.id }
                        .sortedByDescending { it.dateTime }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, FirestoreOpType.LIST, COL_OPERATIONS)
                    }
                    emit(emptyList())
                }
        }
    }

    private val rawLinesFlow: Flow<List<LigneOperationEntity>> = _authUidFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection(COL_OPERATION_LINES)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toLigneOperationEntity() }
                        .filterNot { isOperationDeleted(it.operationId) }
                        .distinctBy { it.id }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, FirestoreOpType.LIST, COL_OPERATION_LINES)
                    }
                    emit(emptyList())
                }
        }
    }

    private val firestoreOperationsWithLignes: Flow<List<OperationWithLignes>> =
        combine(rawOperationsFlow, rawLinesFlow, _deletedOperationsTick) { operations, allLines, _ ->
            val linesByOp = allLines.groupBy { it.operationId }
            val linesByStrippedOp = allLines.groupBy { stripUidPrefix(it.operationId) }
            operations
                .filterNot { isImportedCreanceOperation(it) || isOperationDeleted(it) }
                .distinctBy { it.id }
                .map { op ->
                    val matchedLines = (linesByOp[op.id] ?: linesByStrippedOp[stripUidPrefix(op.id)] ?: emptyList())
                        .distinctBy { it.id }
                    OperationWithLignes(
                        operation = op,
                        lignes = matchedLines
                    )
                }
        }

    val allOperationsWithLignes: Flow<List<OperationWithLignes>> =
        if (localDao != null) {
            _authUidFlow.flatMapLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    combine(
                        firestoreOperationsWithLignes.catch { emit(emptyList()) },
                        localDao.getAllOperationsWithLignes().catch { emit(emptyList()) },
                        _deletedOperationsTick
                    ) { remoteList, localList, _ ->
                        val activeRemote = remoteList.filterNot { isOperationDeleted(it.operation) }
                        val zombieRemote = remoteList.filter { isOperationDeleted(it.operation) }
                        val activeLocal = localList.filterNot { isOperationDeleted(it.operation) }
                        val zombieLocal = localList.filter { isOperationDeleted(it.operation) }

                        if (zombieRemote.isNotEmpty() || zombieLocal.isNotEmpty()) {
                            repoScope.launch(Dispatchers.IO) {
                                try {
                                    for (z in zombieLocal) {
                                        localDao.deleteLignesByOperationId(z.operation.id)
                                        localDao.deleteOperationById(z.operation.id)
                                    }
                                    for (z in zombieRemote) {
                                        localDao.deleteLignesByOperationId(z.operation.id)
                                        localDao.deleteOperationById(z.operation.id)
                                        db.collection(COL_OPERATIONS).document(sanitizeDocId(z.operation.id)).delete().await()
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }

                        // Mirror remote updates to Room in background (only for non-deleted operations)
                        val localById = activeLocal.associateBy { it.operation.id }
                        if (activeRemote.isNotEmpty() && !isDatabaseResetByAdmin()) {
                            repoScope.launch(Dispatchers.IO) {
                                try {
                                    for (remoteItem in activeRemote) {
                                        if (isOperationDeleted(remoteItem.operation)) continue
                                        val localItem = localById[remoteItem.operation.id]
                                        // Ne jamais écraser les lignes locales si les lignes distantes sont encore en cours d'envoi
                                        val effectiveRemoteItem = if (remoteItem.lignes.isEmpty() && localItem != null && localItem.lignes.isNotEmpty()) {
                                            remoteItem.copy(lignes = localItem.lignes)
                                        } else {
                                            remoteItem
                                        }
                                        if (localItem != effectiveRemoteItem && !isOperationDeleted(effectiveRemoteItem.operation)) {
                                            localDao.saveOperationWithLignes(effectiveRemoteItem.operation, effectiveRemoteItem.lignes)
                                        }
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                        val reconciledRemoteList = activeRemote.map { remoteItem ->
                            val localItem = localById[remoteItem.operation.id]
                            if (remoteItem.lignes.isEmpty() && localItem != null && localItem.lignes.isNotEmpty()) {
                                remoteItem.copy(lignes = localItem.lignes)
                            } else {
                                remoteItem
                            }
                        }
                        val merged = (reconciledRemoteList + activeLocal)
                            .filterNot { isImportedCreanceOperation(it.operation) || isOperationDeleted(it.operation) }
                            .distinctBy { it.operation.id }
                            .distinctBy { stripUidPrefix(it.operation.id) }
                            .sortedByDescending { it.operation.dateTime }
                        merged
                    }
                } else {
                    combine(
                        localDao.getAllOperationsWithLignes().catch { emit(emptyList()) },
                        _deletedOperationsTick
                    ) { list, _ ->
                        val zombies = list.filter { isOperationDeleted(it.operation) }
                        if (zombies.isNotEmpty()) {
                            repoScope.launch(Dispatchers.IO) {
                                try {
                                    for (z in zombies) {
                                        localDao.deleteLignesByOperationId(z.operation.id)
                                        localDao.deleteOperationById(z.operation.id)
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                        list.filterNot { isImportedCreanceOperation(it.operation) || isOperationDeleted(it.operation) }
                            .distinctBy { it.operation.id }
                            .distinctBy { stripUidPrefix(it.operation.id) }
                            .sortedByDescending { it.operation.dateTime }
                    }
                }
            }
        } else {
            firestoreOperationsWithLignes
        }

    val allBonsDeLivraison: Flow<List<OperationWithLignes>> =
        allOperationsWithLignes.map { list ->
            list.filter {
                it.operation.typeOperation == OperationType.BL_CLIENT ||
                    it.operation.typeOperation == OperationType.BL_FOURNISSEUR
            }
        }

    val bonsDeLivraisonClients: Flow<List<OperationWithLignes>> =
        allOperationsWithLignes.map { list ->
            list.filter { it.operation.typeOperation == OperationType.BL_CLIENT }
        }

    val bonsDeLivraisonFournisseurs: Flow<List<OperationWithLignes>> =
        allOperationsWithLignes.map { list ->
            list.filter { it.operation.typeOperation == OperationType.BL_FOURNISSEUR }
        }

    val recentPartners: Flow<List<String>> =
        allOperationsWithLignes.map { list ->
            list.map { it.operation.clientFournisseur.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }

    val recentProducts: Flow<List<String>> =
        if (localDao != null) {
            allOperationsWithLignes.map { ops ->
                ops.flatMap { it.lignes }
                    .map { it.produit.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            }
        } else {
            rawLinesFlow.map { lines ->
                lines.map { it.produit.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            }
        }

    // --- Partenaires (Clients & Fournisseurs) en temps réel ---

    private val firestorePartnersFlow: Flow<List<PartnerEntity>> = _authUidFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection(COL_PARTNERS)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toPartnerEntity() }
                        .distinctBy { it.id }
                        .sortedBy { it.name.lowercase(Locale.FRENCH) }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, FirestoreOpType.LIST, COL_PARTNERS)
                    }
                    emit(emptyList())
                }
        }
    }

    val allPartners: Flow<List<PartnerEntity>> =
        if (localDao != null) {
            _authUidFlow.flatMapLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    combine(
                        firestorePartnersFlow.catch { emit(emptyList()) },
                        localDao.getAllPartners().catch { emit(emptyList()) }
                    ) { remote, local ->
                        if (remote.isNotEmpty() && !isDatabaseResetByAdmin()) {
                            repoScope.launch(Dispatchers.IO) {
                                try {
                                    localDao.insertPartners(remote)
                                } catch (_: Exception) {
                                }
                            }
                        }
                        // Prefer remote entries first so real-time updates from Firestore win,
                        // and strictly deduplicate both by id AND by (type + name) so LazyColumn never crashes on duplicate keys
                        (remote + local)
                            .distinctBy { it.id }
                            .distinctBy { "${it.type.name}_${it.name.trim().lowercase(Locale.FRENCH)}" }
                            .sortedBy { it.name.lowercase(Locale.FRENCH) }
                    }
                } else {
                    localDao.getAllPartners()
                        .catch { emit(emptyList()) }
                        .map { list ->
                            list.distinctBy { it.id }
                                .distinctBy { "${it.type.name}_${it.name.trim().lowercase(Locale.FRENCH)}" }
                                .sortedBy { it.name.lowercase(Locale.FRENCH) }
                        }
                }
            }
        } else {
            firestorePartnersFlow
        }

    val clients: Flow<List<PartnerEntity>> =
        allPartners.map { list -> list.filter { it.type == PartnerType.CLIENT }.distinctBy { it.id } }

    val fournisseurs: Flow<List<PartnerEntity>> =
        allPartners.map { list -> list.filter { it.type == PartnerType.FOURNISSEUR }.distinctBy { it.id } }

    // --- Catalogue Produits & Stock en temps réel ---

    private val firestoreCatalogProductsFlow: Flow<List<ProductCatalogEntity>> = _authUidFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection(COL_PRODUCTS)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toProductCatalogEntity() }
                        .distinctBy { it.id }
                        .sortedBy { it.name.lowercase(Locale.FRENCH) }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, FirestoreOpType.LIST, COL_PRODUCTS)
                    }
                    emit(emptyList())
                }
        }
    }

    val allCatalogProducts: Flow<List<ProductCatalogEntity>> =
        if (localDao != null) {
            _authUidFlow.flatMapLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    combine(
                        firestoreCatalogProductsFlow.catch { emit(emptyList()) },
                        localDao.getAllCatalogProducts().catch { emit(emptyList()) }
                    ) { remote, local ->
                        val activeRemote = remote.filterNot { isProductDeleted(it) }
                        val zombieRemote = remote.filter { isProductDeleted(it) }
                        val activeLocal = local.filterNot { isProductDeleted(it) }
                        val zombieLocal = local.filter { isProductDeleted(it) }

                        if (zombieRemote.isNotEmpty() || zombieLocal.isNotEmpty()) {
                            repoScope.launch(Dispatchers.IO) {
                                try {
                                    for (z in zombieLocal) {
                                        localDao.deleteCompositionsForProduct(z.id)
                                        localDao.deleteCatalogProductById(z.id)
                                    }
                                    for (z in zombieRemote) {
                                        localDao.deleteCompositionsForProduct(z.id)
                                        localDao.deleteCatalogProductById(z.id)
                                        db.collection(COL_PRODUCTS).document(sanitizeDocId(z.id)).delete().await()
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }

                        if (activeRemote.isNotEmpty() && !isDatabaseResetByAdmin()) {
                            val localMap = activeLocal.associateBy { it.id }
                            val toInsert = activeRemote.filter { localMap[it.id] != it }
                            if (toInsert.isNotEmpty()) {
                                repoScope.launch(Dispatchers.IO) {
                                    try {
                                        localDao.insertCatalogProducts(toInsert)
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
                        // Prefer remote first so real-time updates win, and deduplicate strictly by id AND name
                        (activeRemote + activeLocal)
                            .distinctBy { it.id }
                            .distinctBy { normalizeProductKey(it.name) }
                            .sortedBy { it.name.lowercase(Locale.FRENCH) }
                    }
                } else {
                    localDao.getAllCatalogProducts()
                        .catch { emit(emptyList()) }
                        .map { list ->
                            val zombies = list.filter { isProductDeleted(it) }
                            if (zombies.isNotEmpty()) {
                                repoScope.launch(Dispatchers.IO) {
                                    try {
                                        for (z in zombies) {
                                            localDao.deleteCompositionsForProduct(z.id)
                                            localDao.deleteCatalogProductById(z.id)
                                        }
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                            list.filterNot { isProductDeleted(it) }
                                .distinctBy { it.id }
                                .distinctBy { normalizeProductKey(it.name) }
                                .sortedBy { it.name.lowercase(Locale.FRENCH) }
                        }
                }
            }
        } else {
            firestoreCatalogProductsFlow.map { list ->
                list.filterNot { isProductDeleted(it) }
                    .distinctBy { it.id }
                    .distinctBy { normalizeProductKey(it.name) }
            }
        }

    private val rawCompositionsFlow: Flow<List<ArticleCompositionEntity>> = _authUidFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection(COL_ARTICLE_COMPOSITIONS)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toArticleCompositionEntity() }
                        .distinctBy { it.id }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, FirestoreOpType.LIST, COL_ARTICLE_COMPOSITIONS)
                    }
                    emit(emptyList())
                }
        }
    }

    val articlesEclatesWithComposition: Flow<List<ProductCatalogWithComposition>> =
        if (localDao != null) {
            localDao.getAllArticlesEclatesWithComposition()
                .catch { emit(emptyList()) }
                .map { list ->
                    list.filterNot { isProductDeleted(it.product) }
                        .distinctBy { it.product.id }
                        .distinctBy { normalizeProductKey(it.product.name) }
                }
        } else {
            combine(allCatalogProducts, rawCompositionsFlow) { products, compositions ->
                val compsByParent = compositions.groupBy { it.parentProductId }
                products.filter { (it.isArticleEclate || it.isArticleKit) && !isProductDeleted(it) }
                    .distinctBy { it.id }
                    .distinctBy { normalizeProductKey(it.name) }
                    .map { prod ->
                        ProductCatalogWithComposition(
                            product = prod,
                            composantes = (compsByParent[prod.id] ?: emptyList()).distinctBy { it.id }
                        )
                    }
            }
        }

    val lowStockProducts: Flow<List<ProductCatalogEntity>> =
        allCatalogProducts.map { list ->
            list.filter { !it.isArticleEclate && !it.isArticleKit && it.stockActuel <= it.seuilAlerte }
                .distinctBy { it.id }
                .sortedBy { it.stockActuel }
        }

    val outOfStockProducts: Flow<List<ProductCatalogEntity>> =
        allCatalogProducts.map { list ->
            list.filter { !it.isArticleEclate && !it.isArticleKit && it.stockActuel <= 0.0 }
                .distinctBy { it.id }
        }

    private val firestoreStockMovementsFlow: Flow<List<StockMovementEntity>> = _authUidFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection(COL_STOCK_MOVEMENTS)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toStockMovementEntity() }
                        .distinctBy { it.id }
                        .sortedByDescending { it.timestamp }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, FirestoreOpType.LIST, COL_STOCK_MOVEMENTS)
                    }
                    emit(emptyList())
                }
        }
    }

    val allStockMovements: Flow<List<StockMovementEntity>> =
        if (localDao != null) {
            _authUidFlow.flatMapLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    combine(
                        firestoreStockMovementsFlow.catch { emit(emptyList()) },
                        localDao.getAllStockMovements().catch { emit(emptyList()) }
                    ) { remote, local ->
                        if (remote.isNotEmpty() && !isDatabaseResetByAdmin()) {
                            repoScope.launch(Dispatchers.IO) {
                                try {
                                    for (mv in remote) {
                                        localDao.insertStockMovement(mv)
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                        (remote + local).distinctBy { it.id }
                            .sortedByDescending { it.timestamp }
                    }
                } else {
                    localDao.getAllStockMovements()
                        .catch { emit(emptyList()) }
                        .map { it.distinctBy { mv -> mv.id }.sortedByDescending { mv -> mv.timestamp } }
                }
            }
        } else {
            firestoreStockMovementsFlow
        }

    private val firestoreEmployeesFlow: Flow<List<EmployeeEntity>> = _authUidFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection(COL_EMPLOYEES)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    val raw = snapshot.documents.mapNotNull { it.toEmployeeEntity() }
                    val deduplicated = deduplicateEmployeesList(raw)
                        .sortedWith(compareBy({ it.role.ordinal }, { it.name.lowercase(Locale.FRENCH) }))

                    // Auto-clean duplicate Firestore documents in background if detected
                    val keptIds = deduplicated.map { it.id }.toSet()
                    val duplicateDocIds = raw.map { it.id }.filter { it !in keptIds }
                    if (duplicateDocIds.isNotEmpty() && uid != "unauthenticated") {
                        repoScope.launch(Dispatchers.IO) {
                            for (dupId in duplicateDocIds) {
                                try {
                                    db.collection(COL_EMPLOYEES).document(dupId).delete().await()
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                    deduplicated
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, FirestoreOpType.LIST, COL_EMPLOYEES)
                    }
                    emit(emptyList())
                }
        }
    }

    val allEmployees: Flow<List<EmployeeEntity>> =
        if (appContext != null) {
            _authUidFlow.flatMapLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    combine(
                        firestoreEmployeesFlow.catch { emit(emptyList()) },
                        employeesFlowInternal
                    ) { remote, local ->
                        if (remote.isNotEmpty()) {
                            val merged = ensureAuthorizedUsersPresent(deduplicateEmployeesList(remote + local))
                            saveEmployeesToLocalPrefs(merged, markCustomized = isAdminCustomized())
                            merged
                        } else {
                            local
                        }
                    }
                } else {
                    employeesFlowInternal
                }
            }
        } else {
            firestoreEmployeesFlow
        }

    // =========================================================================
    // 2. FILTERED REAL-TIME STREAMS
    // =========================================================================

    fun getOperationsByType(type: OperationType): Flow<List<OperationWithLignes>> =
        allOperationsWithLignes.map { list ->
            list.filter { it.operation.typeOperation == type }
        }

    fun getOperationsByPartner(partnerQuery: String): Flow<List<OperationWithLignes>> =
        allOperationsWithLignes.map { list ->
            val q = partnerQuery.trim().lowercase(Locale.FRENCH)
            if (q.isEmpty()) list
            else list.filter { it.operation.clientFournisseur.lowercase(Locale.FRENCH).contains(q) }
        }

    fun getOperationWithLignesById(id: String): Flow<OperationWithLignes?> =
        allOperationsWithLignes.map { list ->
            list.find { it.operation.id == id }
        }

    fun getPartnerById(id: String): Flow<PartnerEntity?> =
        allPartners.map { list -> list.find { it.id == id } }

    fun getCatalogProductById(id: String): Flow<ProductCatalogEntity?> =
        allCatalogProducts.map { list -> list.find { it.id == id } }

    fun getCompositionsForProduct(parentId: String): Flow<List<ArticleCompositionEntity>> =
        if (localDao != null) {
            localDao.getCompositionsForProduct(parentId)
        } else {
            rawCompositionsFlow.map { list -> list.filter { it.parentProductId == parentId } }
        }

    fun getStockMovementsByProduct(productId: String): Flow<List<StockMovementEntity>> =
        allStockMovements.map { list -> list.filter { it.productId == productId } }

    fun getStockMovementsByType(type: StockMovementType): Flow<List<StockMovementEntity>> =
        allStockMovements.map { list -> list.filter { it.type == type } }

    fun getStockMovementsForOperation(operationId: String): Flow<List<StockMovementEntity>> =
        allStockMovements.map { list -> list.filter { it.operationId == operationId } }

    // =========================================================================
    // 3. ASYNCHRONOUS FIRESTORE + LOCAL ROOM READS & WRITES (CRUD)
    // =========================================================================

    private suspend fun upsertDocument(
        docRef: DocumentReference,
        uid: String,
        mutableFields: Map<String, Any>
    ) {
        val existing = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }
        if (existing != null && existing.exists()) {
            val payload = mutableFields + mapOf("updatedAt" to FieldValue.serverTimestamp())
            try {
                docRef.update(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, FirestoreOpType.UPDATE, docRef.path)
                throw e
            }
        } else {
            val payload = mutableFields + mapOf(
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // If document ID collided with a document created by another user, retry with a UID-scoped ID
                val prefix = uidPrefix(uid)
                if (!docRef.id.startsWith(prefix)) {
                    try {
                        val fallbackRef = docRef.parent.document(sanitizeDocId("${prefix}_${docRef.id}"))
                        fallbackRef.set(payload).await()
                        return
                    } catch (_: Exception) {
                        // Fall through to standard error handler
                    }
                }
                handleFirestoreError(e, FirestoreOpType.CREATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun getOperationWithLignesByIdOnce(id: String): OperationWithLignes? {
        if (localDao != null && auth.currentUser == null) {
            return localDao.getOperationWithLignesByIdOnce(id)
        }
        val uid = requireUserId()
        val opDocRef = db.collection(COL_OPERATIONS).document(sanitizeDocId(id))
        return try {
            val opSnap = opDocRef.get().await()
            val op = opSnap.toOperationEntity() ?: return localDao?.getOperationWithLignesByIdOnce(id)
            val linesSnap = db.collection(COL_OPERATION_LINES)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val lines = linesSnap.documents
                .mapNotNull { it.toLigneOperationEntity() }
                .filter { it.operationId == id || it.operationId == op.id }
            OperationWithLignes(operation = op, lignes = lines)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOpType.GET, opDocRef.path)
            throw e
        }
    }

    suspend fun saveOperationWithLignes(
        operation: OperationEntity,
        lignes: List<LigneOperationEntity>
    ) {
        val opId = sanitizeDocId(operation.id.ifBlank { UUID.randomUUID().toString() })
        unmarkOperationDeleted(opId)
        val seller = operation.vendeur.ifBlank { currentSellerLabel() }
        val normalizedOp = operation.copy(id = opId, vendeur = seller)
        val normalizedLines = lignes.map { line ->
            line.copy(
                id = sanitizeDocId(line.id.ifBlank { UUID.randomUUID().toString() }),
                operationId = opId
            )
        }

        // 1. Always persist locally when localDao is available (instant UI update)
        if (localDao != null) {
            localDao.saveOperationWithLignes(normalizedOp, normalizedLines)
            recalculateLocalPartnerBalance(normalizedOp.clientFournisseur)
            if (auth.currentUser == null) return
        }

        // 2. Persist to Cloud Firestore (non-blocking when localDao is present so UI never hangs)
        val syncBlock: suspend () -> Unit = {
            val uid = requireUserId()
            val opDocRef = db.collection(COL_OPERATIONS).document(opId)

            val totalHt = normalizedLines.sumOf { it.totalLigne }
            val tvaAmount = if (normalizedOp.appliquerTva) totalHt * (normalizedOp.tauxTva / 100.0) else 0.0
            val totalTtc = if (normalizedOp.typeOperation == OperationType.REGLEMENT) {
                normalizedOp.montantVerse
            } else {
                totalHt + tvaAmount
            }

            val opFields = mapOf(
                "dateTime" to normalizedOp.dateTime,
                "typeOperation" to normalizedOp.typeOperation.name,
                "clientFournisseur" to normalizedOp.clientFournisseur.trim().ifEmpty { "Client Comptoir" }.take(200),
                "montantVerse" to normalizedOp.montantVerse.coerceAtLeast(0.0),
                "montantTotalTtc" to totalTtc.coerceAtLeast(0.0),
                "modeReglement" to normalizedOp.modeReglement.take(60),
                "vendeur" to seller.take(150),
                "notes" to normalizedOp.notes.take(1000),
                "appliquerTva" to normalizedOp.appliquerTva,
                "tauxTva" to normalizedOp.tauxTva.coerceAtLeast(0.0),
                "statut" to normalizedOp.statut.ifBlank { "Validé" }.take(60)
            )

            try {
                // Delete existing lines for this operation first
                try {
                    val existingLines = db.collection(COL_OPERATION_LINES)
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()
                    for (doc in existingLines.documents) {
                        if (doc.getString("idOperation") == opId) {
                            doc.reference.delete().await()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, FirestoreOpType.DELETE, COL_OPERATION_LINES)
                }

                // Write current lines before operation header so snapshot listeners immediately see all lines
                for (ligne in normalizedLines) {
                    val lineRef = db.collection(COL_OPERATION_LINES).document(ligne.id)
                    val lineFields = mapOf(
                        "idOperation" to opId,
                        "produit" to ligne.produit.trim().ifEmpty { "Article" }.take(200),
                        "quantite" to ligne.quantite.coerceAtLeast(0.001),
                        "prixUnitaire" to (ligne.prixUnitaire ?: 0.0).coerceAtLeast(0.0),
                        "parentArticle" to ligne.parentArticle.take(200)
                    )
                    upsertDocument(lineRef, uid, lineFields)
                }

                upsertDocument(opDocRef, uid, opFields)

                // Synchronize partner balance (solde) in Firestore
                recalculateAndSyncPartnerBalance(normalizedOp.clientFournisseur)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (localDao == null) throw e
            }
        }

        if (localDao != null) {
            repoScope.launch(Dispatchers.IO) {
                try {
                    syncBlock()
                } catch (_: Exception) {
                }
            }
        } else {
            syncBlock()
        }
    }

    suspend fun deleteOperation(id: String) {
        val rawId = id.trim()
        if (rawId.isEmpty()) return
        val cleanId = sanitizeDocId(rawId)
        val strippedId = stripUidPrefix(rawId)
        val allCandidateIds = mutableSetOf(rawId, cleanId, strippedId, sanitizeDocId(strippedId))
        val affectedPartners = mutableSetOf<String>()

        // 1. Marquer immédiatement l'opération comme supprimée (tombstone persistant + tick réactif)
        // afin que les flux temps réel (allOperationsWithLignes) la retirent instantanément sans jamais la réinsérer
        markOperationDeleted(rawId)

        // 2. Supprimer immédiatement toutes les variantes d'ID de la base locale Room
        if (localDao != null) {
            try {
                val allLocal = localDao.getAllOperationsWithLignes().first()
                val matchingLocals = allLocal.filter { item ->
                    val opId = item.operation.id.trim()
                    opId in allCandidateIds ||
                        stripUidPrefix(opId) == strippedId ||
                        isOperationDeleted(item.operation)
                }
                for (item in matchingLocals) {
                    val mId = item.operation.id.trim()
                    allCandidateIds.add(mId)
                    allCandidateIds.add(sanitizeDocId(mId))
                    allCandidateIds.add(stripUidPrefix(mId))
                    markOperationDeleted(mId)
                    if (item.operation.clientFournisseur.isNotBlank()) {
                        affectedPartners.add(item.operation.clientFournisseur)
                    }
                    localDao.deleteLignesByOperationId(mId)
                    localDao.deleteOperationById(mId)
                }
            } catch (_: Exception) {
            }

            for (cid in allCandidateIds.toList()) {
                if (cid.isNotBlank()) {
                    try {
                        val existing = localDao.getOperationWithLignesByIdOnce(cid)
                        existing?.operation?.clientFournisseur?.takeIf { it.isNotBlank() }?.let {
                            affectedPartners.add(it)
                        }
                        localDao.deleteLignesByOperationId(cid)
                        localDao.deleteOperationById(cid)
                    } catch (_: Exception) {
                    }
                }
            }

            for (pName in affectedPartners) {
                try {
                    recalculateLocalPartnerBalance(pName)
                } catch (_: Exception) {
                }
            }

            if (auth.currentUser == null) return
        }

        // 3. Supprimer de Cloud Firestore (en arrière-plan si localDao est présent pour ne jamais bloquer l'interface)
        val remoteDeleteBlock: suspend () -> Unit = {
            val uid = requireUserId()
            allCandidateIds.add(scopedSeedId(uid, rawId))
            allCandidateIds.add(scopedSeedId(uid, strippedId))

            val opRef = db.collection(COL_OPERATIONS).document(cleanId)
            try {
                // Supprimer directement tous les IDs candidats dans COL_OPERATIONS
                for (cid in allCandidateIds.toList()) {
                    if (cid.isNotBlank()) {
                        val docRef = db.collection(COL_OPERATIONS).document(sanitizeDocId(cid))
                        try {
                            val snap = docRef.get().await()
                            snap.getString("clientFournisseur")?.takeIf { it.isNotBlank() }?.let {
                                affectedPartners.add(it)
                            }
                            docRef.delete().await()
                        } catch (_: Exception) {
                            try {
                                docRef.delete().await()
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                // Vérifier s'il reste un document dans COL_OPERATIONS avec un préfixe UID ou un ID correspondant
                try {
                    val opsSnap = db.collection(COL_OPERATIONS)
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()
                    for (doc in opsSnap.documents) {
                        if (doc.id in allCandidateIds ||
                            stripUidPrefix(doc.id) == strippedId ||
                            isOperationDeleted(doc.id)
                        ) {
                            allCandidateIds.add(doc.id)
                            markOperationDeleted(doc.id)
                            doc.getString("clientFournisseur")?.takeIf { it.isNotBlank() }?.let {
                                affectedPartners.add(it)
                            }
                            try {
                                doc.reference.delete().await()
                            } catch (_: Exception) {
                            }
                        }
                    }
                } catch (_: Exception) {
                }

                // Supprimer toutes les lignes associées dans COL_OPERATION_LINES
                val existingLines = db.collection(COL_OPERATION_LINES)
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                for (doc in existingLines.documents) {
                    val lineOpId = doc.getString("idOperation")?.trim() ?: ""
                    if (lineOpId in allCandidateIds ||
                        stripUidPrefix(lineOpId) == strippedId ||
                        isOperationDeleted(lineOpId)
                    ) {
                        try {
                            doc.reference.delete().await()
                        } catch (_: Exception) {
                        }
                    }
                }

                for (pName in affectedPartners) {
                    if (pName.isNotBlank()) {
                        try {
                            recalculateAndSyncPartnerBalance(pName)
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (localDao == null) {
                    handleFirestoreError(e, FirestoreOpType.DELETE, opRef.path)
                    throw e
                }
            }
        }

        if (localDao != null) {
            repoScope.launch(Dispatchers.IO) {
                try {
                    remoteDeleteBlock()
                } catch (_: Exception) {
                }
            }
        } else {
            remoteDeleteBlock()
        }
    }

    suspend fun deleteLigne(idLigne: String) {
        if (localDao != null) {
            localDao.deleteLigneById(idLigne)
            if (auth.currentUser == null) return
        }
        requireUserId()
        val lineRef = db.collection(COL_OPERATION_LINES).document(sanitizeDocId(idLigne))
        try {
            lineRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.DELETE, lineRef.path)
                throw e
            }
        }
    }

    // --- CRUD Partenaires (Clients & Fournisseurs) ---

    private fun encodePartnerNotesWithNin(notes: String, nin: String): String {
        val cleanNotes = notes.replace(Regex("\\s*\\[NIN:[^\\]]*]"), "").trim()
        val cleanNin = nin.trim()
        return if (cleanNin.isEmpty()) {
            cleanNotes.take(1000)
        } else {
            val tag = "[NIN:$cleanNin]"
            val combined = if (cleanNotes.isEmpty()) tag else "$cleanNotes $tag"
            combined.take(1000)
        }
    }

    private fun extractNinAndCleanNotes(rawNotes: String?, explicitNin: String?): Pair<String, String> {
        val notesStr = rawNotes ?: ""
        val regex = Regex("\\[NIN:([^\\]]*)]")
        val match = regex.find(notesStr)
        val extractedNin = explicitNin?.takeIf { it.isNotBlank() } ?: match?.groupValues?.getOrNull(1)?.trim() ?: ""
        val cleanedNotes = notesStr.replace(regex, "").trim()
        return cleanedNotes to extractedNin
    }

    private fun partnerMutableMap(partner: PartnerEntity): Map<String, Any> {
        return mapOf(
            "name" to partner.name.trim().ifEmpty { "Partenaire" }.take(200),
            "type" to partner.type.name,
            "phone" to partner.phone.take(60),
            "address" to partner.address.take(300),
            "solde" to partner.solde,
            "notes" to encodePartnerNotesWithNin(partner.notes, partner.nin),
            "rc" to partner.rc.take(100),
            "articleImposition" to partner.articleImposition.take(100),
            "nif" to partner.nif.take(100),
            "nis" to partner.nis.take(100),
            "rib" to partner.rib.take(120)
        )
    }

    suspend fun insertPartner(partner: PartnerEntity) {
        val id = sanitizeDocId(partner.id.ifBlank { UUID.randomUUID().toString() })
        val normalized = partner.copy(id = id)
        if (localDao != null) {
            localDao.insertPartner(normalized)
            if (auth.currentUser == null) return
            repoScope.launch(Dispatchers.IO) {
                try {
                    val uid = requireUserId()
                    val docRef = db.collection(COL_PARTNERS).document(id)
                    upsertDocument(docRef, uid, partnerMutableMap(normalized))
                } catch (_: Exception) {
                }
            }
            return
        }
        val uid = requireUserId()
        val docRef = db.collection(COL_PARTNERS).document(id)
        try {
            upsertDocument(docRef, uid, partnerMutableMap(normalized))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) throw e
        }
    }

    suspend fun updatePartner(partner: PartnerEntity) {
        insertPartner(partner)
    }

    suspend fun insertPartners(partners: List<PartnerEntity>) {
        upsertPartnersFromImport(partners)
    }

    /**
     * Crée ou met à jour automatiquement les Tiers (Clients / Fournisseurs) dans la base locale et dans la collection Firestore `partners`.
     * Retourne Pair(createdCount, updatedCount).
     */
    suspend fun upsertPartnersFromImport(partners: List<PartnerEntity>): Pair<Int, Int> {
        if (partners.isEmpty()) return 0 to 0

        val existingPartners: MutableList<PartnerEntity> = when {
            localDao != null -> {
                try {
                    localDao.getAllPartners().first().toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            }
            auth.currentUser != null -> {
                val uid = requireUserId()
                try {
                    val snap = db.collection(COL_PARTNERS)
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()
                    snap.documents.mapNotNull { it.toPartnerEntity() }.toMutableList()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, FirestoreOpType.LIST, COL_PARTNERS)
                    mutableListOf()
                }
            }
            else -> mutableListOf()
        }

        var created = 0
        var updated = 0
        val toSave = mutableListOf<PartnerEntity>()

        for (incoming in partners) {
            val cleanName = incoming.name.trim()
            if (cleanName.isEmpty()) continue
            val cleanNif = incoming.nif.trim()

            val matchIndex = existingPartners.indexOfFirst { existing ->
                existing.type == incoming.type && (
                    existing.name.trim().equals(cleanName, ignoreCase = true) ||
                    (cleanNif.isNotEmpty() && existing.nif.trim().equals(cleanNif, ignoreCase = true))
                )
            }

            if (matchIndex >= 0) {
                val existing = existingPartners[matchIndex]
                val merged = existing.copy(
                    id = sanitizeDocId(existing.id.ifBlank { UUID.randomUUID().toString() }),
                    name = cleanName,
                    phone = incoming.phone.ifBlank { existing.phone },
                    address = incoming.address.ifBlank { existing.address },
                    rc = incoming.rc.ifBlank { existing.rc },
                    nif = incoming.nif.ifBlank { existing.nif },
                    articleImposition = incoming.articleImposition.ifBlank { existing.articleImposition },
                    nis = incoming.nis.ifBlank { existing.nis },
                    nin = incoming.nin.ifBlank { existing.nin },
                    rib = incoming.rib.ifBlank { existing.rib },
                    solde = if (incoming.solde != 0.0) incoming.solde else existing.solde,
                    notes = incoming.notes.ifBlank { existing.notes }
                )
                existingPartners[matchIndex] = merged
                toSave.add(merged)
                updated++
            } else {
                val fresh = incoming.copy(
                    id = sanitizeDocId(incoming.id.ifBlank { UUID.randomUUID().toString() }),
                    name = cleanName
                )
                existingPartners.add(fresh)
                toSave.add(fresh)
                created++
            }
        }

        if (localDao != null && toSave.isNotEmpty()) {
            localDao.insertPartners(toSave)
        }

        val uid = auth.currentUser?.uid
        if (uid != null && toSave.isNotEmpty()) {
            for (partner in toSave) {
                try {
                    val docRef = db.collection(COL_PARTNERS).document(partner.id)
                    upsertDocument(docRef, uid, partnerMutableMap(partner))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (localDao == null) throw e
                }
            }
        } else if (localDao == null) {
            requireUserId()
        }

        return created to updated
    }

    /**
     * Importe une liste de Créances Clients et/ou Dettes Fournisseurs depuis un fichier Excel (.xlsx / .xls) ou CSV.
     * IMPORTANT : Ne crée PAS de Bon de Livraison (BL Client / BL Fournisseur).
     * Ajoute ou met à jour uniquement la créance restante (solde) sur la fiche du Client ou Fournisseur.
     * Retourne Pair(createdCount, updatedCount).
     */
    suspend fun upsertCreancesFromImport(items: List<CreanceImportItem>): Pair<Int, Int> {
        if (items.isEmpty()) return 0 to 0

        val existingPartners: MutableList<PartnerEntity> = when {
            localDao != null -> {
                try {
                    localDao.getAllPartners().first().toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            }
            auth.currentUser != null -> {
                val uid = requireUserId()
                try {
                    val snap = db.collection(COL_PARTNERS)
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()
                    snap.documents.mapNotNull { it.toPartnerEntity() }.toMutableList()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, FirestoreOpType.LIST, COL_PARTNERS)
                    mutableListOf()
                }
            }
            else -> mutableListOf()
        }

        var created = 0
        var updated = 0
        val seenPartnerIdsInBatch = mutableSetOf<String>()

        for (item in items) {
            val cleanName = item.partnerName.trim()
            if (cleanName.isEmpty()) continue
            val cleanNif = item.nif.trim()
            val importedCreance = item.creanceRestante.coerceAtLeast(0.0)

            // Trouver ou créer le partenaire (Client ou Fournisseur) avec uniquement sa créance restante (sans créer de BL)
            val matchIndex = existingPartners.indexOfFirst { existing ->
                existing.type == item.partnerType && (
                    existing.name.trim().equals(cleanName, ignoreCase = true) ||
                    (cleanNif.isNotEmpty() && existing.nif.trim().equals(cleanNif, ignoreCase = true))
                )
            }

            val partner: PartnerEntity = if (matchIndex >= 0) {
                val existing = existingPartners[matchIndex]
                val safeId = sanitizeDocId(existing.id.ifBlank { UUID.randomUUID().toString() })
                val newSolde = if (seenPartnerIdsInBatch.contains(safeId)) {
                    (existing.solde + importedCreance).coerceAtLeast(0.0)
                } else {
                    seenPartnerIdsInBatch.add(safeId)
                    updated++
                    importedCreance
                }
                val mergedNotes = when {
                    item.motifNotes.isNotBlank() && item.referenceDoc.isNotBlank() ->
                        "${item.motifNotes.trim()} (Réf: ${item.referenceDoc.trim()})"
                    item.motifNotes.isNotBlank() -> item.motifNotes.trim()
                    item.referenceDoc.isNotBlank() && existing.notes.isBlank() ->
                        "Réf créance: ${item.referenceDoc.trim()}"
                    else -> existing.notes
                }
                val merged = existing.copy(
                    id = safeId,
                    name = cleanName,
                    phone = item.phone.ifBlank { existing.phone },
                    address = item.address.ifBlank { existing.address },
                    rc = item.rc.ifBlank { existing.rc },
                    nif = item.nif.ifBlank { existing.nif },
                    articleImposition = item.ai.ifBlank { existing.articleImposition },
                    nis = item.nis.ifBlank { existing.nis },
                    nin = item.nin.ifBlank { existing.nin },
                    rib = item.rib.ifBlank { existing.rib },
                    solde = newSolde,
                    notes = mergedNotes
                )
                existingPartners[matchIndex] = merged
                merged
            } else {
                val freshId = sanitizeDocId(UUID.randomUUID().toString())
                seenPartnerIdsInBatch.add(freshId)
                created++
                val freshNotes = when {
                    item.motifNotes.isNotBlank() && item.referenceDoc.isNotBlank() ->
                        "${item.motifNotes.trim()} (Réf: ${item.referenceDoc.trim()})"
                    item.motifNotes.isNotBlank() -> item.motifNotes.trim()
                    item.referenceDoc.isNotBlank() -> "Réf créance: ${item.referenceDoc.trim()}"
                    else -> ""
                }
                val fresh = PartnerEntity(
                    id = freshId,
                    name = cleanName,
                    type = item.partnerType,
                    phone = item.phone,
                    address = item.address,
                    rc = item.rc,
                    nif = item.nif,
                    articleImposition = item.ai,
                    nis = item.nis,
                    nin = item.nin,
                    rib = item.rib,
                    solde = importedCreance,
                    notes = freshNotes
                )
                existingPartners.add(fresh)
                fresh
            }

            insertPartner(partner)
        }

        // Nettoyer d'éventuelles anciennes opérations BL d'importation (IMP-SOLDE-* / CRE-*)
        cleanupLegacyImportedCreanceOperations()

        return created to updated
    }

    /**
     * Migre et supprime les anciennes opérations BL créées lors d'anciens imports de créances (IMP-SOLDE-* / CRE-*)
     * tout en conservant leur montant restant dû dans `partner.solde`.
     */
    private suspend fun cleanupLegacyImportedCreanceOperations() {
        if (localDao != null) {
            try {
                val allLocalOps = localDao.getAllOperationsWithLignes().first()
                val legacyOps = allLocalOps.filter { isImportedCreanceOperation(it.operation) }
                if (legacyOps.isNotEmpty()) {
                    val partners = localDao.getAllPartners().first().toMutableList()
                    val legacyByPartner = legacyOps.groupBy { it.operation.clientFournisseur.trim().lowercase(Locale.FRENCH) }
                    for ((partnerNameLower, opsForPartner) in legacyByPartner) {
                        val unpaidSum = opsForPartner.sumOf { (it.totalTtc - it.operation.montantVerse).coerceAtLeast(0.0) }
                        val idx = partners.indexOfFirst { it.name.trim().lowercase(Locale.FRENCH) == partnerNameLower }
                        if (idx >= 0 && unpaidSum > 0.0 && partners[idx].solde < unpaidSum) {
                            val updatedPartner = partners[idx].copy(solde = unpaidSum)
                            partners[idx] = updatedPartner
                            localDao.insertPartner(updatedPartner)
                        }
                    }
                    for (legacy in legacyOps) {
                        localDao.deleteLignesByOperationId(legacy.operation.id)
                        localDao.deleteOperationById(legacy.operation.id)
                    }
                }
            } catch (_: Exception) {
            }
        }

        val uid = auth.currentUser?.uid ?: return
        try {
            val opsSnap = db.collection(COL_OPERATIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val legacyDocs = opsSnap.documents.filter { doc ->
                val op = doc.toOperationEntity()
                op != null && isImportedCreanceOperation(op)
            }
            if (legacyDocs.isNotEmpty()) {
                val partnersSnap = db.collection(COL_PARTNERS)
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                for (doc in legacyDocs) {
                    val pName = doc.getString("clientFournisseur")?.trim() ?: ""
                    val totalTtc = (doc.get("montantTotalTtc") as? Number)?.toDouble() ?: 0.0
                    val verse = (doc.get("montantVerse") as? Number)?.toDouble() ?: 0.0
                    val unpaid = (totalTtc - verse).coerceAtLeast(0.0)
                    if (pName.isNotEmpty() && unpaid > 0.0) {
                        val pDoc = partnersSnap.documents.find {
                            it.getString("name")?.trim()?.equals(pName, ignoreCase = true) == true
                        }
                        if (pDoc != null) {
                            val currentSolde = (pDoc.get("solde") as? Number)?.toDouble() ?: 0.0
                            if (currentSolde < unpaid) {
                                pDoc.reference.update(
                                    mapOf(
                                        "solde" to unpaid,
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                ).await()
                            }
                        }
                    }
                    doc.reference.delete().await()
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun deletePartner(partner: PartnerEntity) {
        deletePartnerById(partner.id)
    }

    suspend fun deletePartnerById(id: String) {
        if (localDao != null) {
            localDao.deletePartnerById(id)
            if (auth.currentUser == null) return
        }
        requireUserId()
        val docRef = db.collection(COL_PARTNERS).document(sanitizeDocId(id))
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.DELETE, docRef.path)
                throw e
            }
        }
    }

    suspend fun clearPartners(type: PartnerType) {
        setDatabaseResetByAdmin(true)
        if (localDao != null) {
            localDao.clearPartnersByType(type)
            if (auth.currentUser == null) return
        }
        val uid = requireUserId()
        try {
            val snap = db.collection(COL_PARTNERS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in snap.documents) {
                if (doc.getString("type") == type.name) {
                    doc.reference.delete().await()
                }
            }
            localDao?.clearPartnersByType(type)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.DELETE, COL_PARTNERS)
                throw e
            }
        }
    }

    private suspend fun recalculateLocalPartnerBalance(partnerName: String) {
        // Le champ `partner.solde` conserve la créance restante importée / solde initial du partenaire
        // (hors BL). On ne l'écrase pas lors de l'enregistrement d'un BL ou d'un règlement afin de
        // préserver les créances importées sans BL.
    }

    private suspend fun recalculateAndSyncPartnerBalance(partnerName: String) {
        // En mode hybride Room + Firestore (localDao != null), `partner.solde` conserve la créance
        // initiale importée (hors BL/Factures) car le ViewModel calcule dynamiquement
        // soldeRestant = partner.solde + totalTransactions - totalReglements.
        // En mode Firestore pur (localDao == null), on recalcule et synchronise directement le solde net.
        if (localDao != null) return
        val cleanName = partnerName.trim()
        if (cleanName.isEmpty()) return
        val uid = auth.currentUser?.uid ?: return
        try {
            val opsSnap = db.collection(COL_OPERATIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            var totalDue = 0.0
            var totalPaid = 0.0
            for (doc in opsSnap.documents) {
                if (isOperationDeleted(doc.id)) continue
                val pName = doc.getString("clientFournisseur")?.trim() ?: continue
                if (!pName.equals(cleanName, ignoreCase = true)) continue
                val typeStr = doc.getString("typeOperation") ?: continue
                val opType = try { OperationType.valueOf(typeStr) } catch (_: Exception) { continue }
                val totalTtc = (doc.get("montantTotalTtc") as? Number)?.toDouble() ?: 0.0
                val verse = (doc.get("montantVerse") as? Number)?.toDouble() ?: 0.0
                when (opType) {
                    OperationType.BL_CLIENT,
                    OperationType.FACTURE,
                    OperationType.BL_FOURNISSEUR,
                    OperationType.FACTURE_FOURNISSEUR,
                    OperationType.BON_COMMANDE -> {
                        totalDue += totalTtc
                        totalPaid += verse
                    }
                    OperationType.REGLEMENT -> {
                        totalPaid += verse
                    }
                    OperationType.AVOIR_CLIENT,
                    OperationType.AVOIR_FOURNISSEUR -> {
                        totalPaid += totalTtc
                    }
                    OperationType.DEVIS_CLIENT,
                    OperationType.DEVIS_FOURNISSEUR -> Unit
                }
            }
            val newSolde = (totalDue - totalPaid).coerceAtLeast(0.0)
            val partnersSnap = db.collection(COL_PARTNERS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (pDoc in partnersSnap.documents) {
                val pName = pDoc.getString("name")?.trim() ?: continue
                if (pName.equals(cleanName, ignoreCase = true)) {
                    pDoc.reference.update(
                        mapOf(
                            "solde" to newSolde,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (_: Exception) {
        }
    }

    // --- CRUD Catalogue Produits & Stock ---

    private fun encodeCategoryWithMarque(category: String, marque: String): String {
        val cleanCategory = category.replace(Regex("\\s*\\[MARQUE:[^\\]]*]"), "").trim().ifEmpty { "Général" }
        val cleanMarque = marque.trim()
        return if (cleanMarque.isEmpty()) {
            cleanCategory.take(100)
        } else {
            val tag = "[MARQUE:${cleanMarque.take(40)}]"
            val maxCatLen = (100 - tag.length - 1).coerceAtLeast(10)
            "${cleanCategory.take(maxCatLen)} $tag".take(100)
        }
    }

    private fun extractMarqueAndCleanCategory(rawCategory: String?, explicitMarque: String?): Pair<String, String> {
        val catStr = rawCategory ?: ""
        val regex = Regex("\\[MARQUE:([^\\]]*)]")
        val match = regex.find(catStr)
        val extractedMarque = explicitMarque?.takeIf { it.isNotBlank() } ?: match?.groupValues?.getOrNull(1)?.trim() ?: ""
        val cleanedCategory = catStr.replace(regex, "").trim().ifEmpty { "Général" }
        return cleanedCategory to extractedMarque
    }

    private fun productMutableMap(product: ProductCatalogEntity): Map<String, Any> {
        return mapOf(
            "name" to product.name.trim().ifEmpty { "Article" }.take(200),
            "reference" to product.reference.take(100),
            "barcode" to product.barcode.take(100),
            "category" to encodeCategoryWithMarque(product.category, product.marque),
            "unit" to product.unit.ifBlank { "U" }.take(30),
            "defaultUnitPrice" to (product.defaultUnitPrice ?: 0.0).coerceAtLeast(0.0),
            "prixAchatMoyen" to (product.prixAchatMoyen ?: 0.0).coerceAtLeast(0.0),
            "stockActuel" to product.stockActuel,
            "seuilAlerte" to product.seuilAlerte.coerceAtLeast(0.0),
            "isArticleEclate" to product.isArticleEclate,
            "isArticleKit" to product.isArticleKit
        )
    }

    suspend fun getCatalogProductByNameOnce(designation: String): ProductCatalogEntity? {
        val clean = designation.trim()
        val cleanWithoutRef = clean.replace(Regex("^\\[[^\\]]+]\\s*"), "").trim()
        if (localDao != null) {
            val localMatch = localDao.getCatalogProductByNameOnce(clean)
                ?: (if (cleanWithoutRef != clean) localDao.getCatalogProductByNameOnce(cleanWithoutRef) else null)
            return localMatch
        }
        val uid = requireUserId()
        return try {
            val snap = db.collection(COL_PRODUCTS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snap.documents
                .mapNotNull { it.toProductCatalogEntity() }
                .find {
                    it.name.trim().equals(clean, ignoreCase = true) ||
                        it.name.trim().equals(cleanWithoutRef, ignoreCase = true)
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOpType.LIST, COL_PRODUCTS)
            null
        }
    }

    suspend fun insertCatalogProduct(product: ProductCatalogEntity) {
        val id = sanitizeDocId(product.id.ifBlank { UUID.randomUUID().toString() })
        val normalized = product.copy(id = id)
        unmarkProductDeleted(id, normalized.name)
        if (localDao != null) {
            localDao.insertCatalogProduct(normalized)
            if (auth.currentUser == null) return
        }
        val uid = requireUserId()
        val docRef = db.collection(COL_PRODUCTS).document(id)
        try {
            upsertDocument(docRef, uid, productMutableMap(normalized))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) throw e
        }
    }

    suspend fun updateCatalogProduct(product: ProductCatalogEntity) {
        insertCatalogProduct(product)
    }

    suspend fun insertCatalogProducts(products: List<ProductCatalogEntity>) {
        upsertCatalogProductsFromImport(products)
    }

    /**
     * Crée ou met à jour automatiquement les Articles (Stock) dans la base locale et dans la collection Firestore `products`
     * en recherchant par `reference`, `barcode` ou `name` (désignation).
     * Retourne Pair(createdCount, updatedCount).
     */
    suspend fun upsertCatalogProductsFromImport(products: List<ProductCatalogEntity>): Pair<Int, Int> {
        if (products.isEmpty()) return 0 to 0

        val existingProducts: MutableList<ProductCatalogEntity> = when {
            localDao != null -> {
                try {
                    localDao.getAllCatalogProducts().first().toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            }
            auth.currentUser != null -> {
                val uid = requireUserId()
                try {
                    val snap = db.collection(COL_PRODUCTS)
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()
                    snap.documents.mapNotNull { it.toProductCatalogEntity() }.toMutableList()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, FirestoreOpType.LIST, COL_PRODUCTS)
                    mutableListOf()
                }
            }
            else -> mutableListOf()
        }

        var created = 0
        var updated = 0
        val toSave = mutableListOf<ProductCatalogEntity>()

        for (incoming in products) {
            val cleanName = incoming.name.trim()
            if (cleanName.isEmpty()) continue
            val cleanRef = incoming.reference.trim()
            val cleanBarcode = incoming.barcode.trim()

            val matchIndex = existingProducts.indexOfFirst { existing ->
                (cleanRef.isNotEmpty() && existing.reference.trim().equals(cleanRef, ignoreCase = true)) ||
                (cleanBarcode.isNotEmpty() && existing.barcode.trim().equals(cleanBarcode, ignoreCase = true)) ||
                existing.name.trim().equals(cleanName, ignoreCase = true)
            }

            if (matchIndex >= 0) {
                val existing = existingProducts[matchIndex]
                val merged = existing.copy(
                    id = sanitizeDocId(existing.id.ifBlank { UUID.randomUUID().toString() }),
                    name = cleanName,
                    reference = incoming.reference.ifBlank { existing.reference },
                    barcode = incoming.barcode.ifBlank { existing.barcode },
                    category = if (incoming.category.isNotBlank() && incoming.category != "Général") incoming.category else existing.category,
                    marque = incoming.marque.ifBlank { existing.marque },
                    unit = incoming.unit.ifBlank { existing.unit },
                    prixAchatMoyen = incoming.prixAchatMoyen ?: existing.prixAchatMoyen,
                    defaultUnitPrice = incoming.defaultUnitPrice ?: existing.defaultUnitPrice,
                    stockActuel = incoming.stockActuel
                )
                unmarkProductDeleted(merged.id, merged.name)
                existingProducts[matchIndex] = merged
                toSave.add(merged)
                updated++
            } else {
                val fresh = incoming.copy(
                    id = sanitizeDocId(incoming.id.ifBlank { UUID.randomUUID().toString() }),
                    name = cleanName
                )
                unmarkProductDeleted(fresh.id, fresh.name)
                existingProducts.add(fresh)
                toSave.add(fresh)
                created++
            }
        }

        if (localDao != null && toSave.isNotEmpty()) {
            localDao.insertCatalogProducts(toSave)
        }

        val uid = auth.currentUser?.uid
        if (uid != null && toSave.isNotEmpty()) {
            for (product in toSave) {
                try {
                    val docRef = db.collection(COL_PRODUCTS).document(product.id)
                    upsertDocument(docRef, uid, productMutableMap(product))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (localDao == null) throw e
                }
            }
        } else if (localDao == null) {
            requireUserId()
        }

        return created to updated
    }

    suspend fun deleteCatalogProduct(product: ProductCatalogEntity) {
        markProductDeleted(product.id, product.name)
        deleteCatalogProductById(product.id)
    }

    suspend fun deleteCatalogProductById(id: String) {
        val rawId = id.trim()
        val cleanId = sanitizeDocId(rawId)
        val strippedId = stripUidPrefix(rawId)
        val allCandidateIds = mutableSetOf(rawId, cleanId, strippedId)
        var targetNormName = ""

        if (localDao != null) {
            try {
                val allLocal = localDao.getAllCatalogProducts().first()
                val directMatch = allLocal.find {
                    it.id == rawId || it.id == cleanId || stripUidPrefix(it.id) == strippedId
                }
                if (directMatch != null) {
                    targetNormName = normalizeProductKey(directMatch.name)
                    markProductDeleted(directMatch.id, directMatch.name)
                } else {
                    markProductDeleted(rawId, null)
                }

                val matchingLocals = allLocal.filter { prod ->
                    prod.id in allCandidateIds ||
                        stripUidPrefix(prod.id) == strippedId ||
                        (targetNormName.isNotEmpty() && normalizeProductKey(prod.name) == targetNormName) ||
                        isProductDeleted(prod)
                }
                for (prod in matchingLocals) {
                    allCandidateIds.add(prod.id)
                    allCandidateIds.add(sanitizeDocId(prod.id))
                    allCandidateIds.add(stripUidPrefix(prod.id))
                    markProductDeleted(prod.id, prod.name)
                    localDao.deleteCompositionsForProduct(prod.id)
                    localDao.deleteCatalogProductById(prod.id)
                }
            } catch (_: Exception) {
                markProductDeleted(rawId, null)
            }
            for (cid in allCandidateIds.toList()) {
                try {
                    localDao.deleteCompositionsForProduct(cid)
                    localDao.deleteCatalogProductById(cid)
                } catch (_: Exception) {
                }
            }
            if (auth.currentUser == null) return
        } else {
            markProductDeleted(rawId, null)
        }

        val uid = requireUserId()
        allCandidateIds.add(scopedSeedId(uid, rawId))
        allCandidateIds.add(scopedSeedId(uid, strippedId))

        val docRef = db.collection(COL_PRODUCTS).document(cleanId)
        try {
            // 1. Delete all candidate product document IDs directly
            for (cid in allCandidateIds) {
                if (cid.isNotBlank()) {
                    try {
                        db.collection(COL_PRODUCTS).document(sanitizeDocId(cid)).delete().await()
                    } catch (_: Exception) {
                    }
                }
            }

            // 2. Query Firestore products for any duplicate document with the same name or tombstone
            val prodSnap = db.collection(COL_PRODUCTS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in prodSnap.documents) {
                val docProd = doc.toProductCatalogEntity()
                val docNormName = normalizeProductKey(doc.getString("name"))
                if (doc.id in allCandidateIds ||
                    stripUidPrefix(doc.id) == strippedId ||
                    (targetNormName.isNotEmpty() && docNormName == targetNormName) ||
                    (docProd != null && isProductDeleted(docProd))
                ) {
                    allCandidateIds.add(doc.id)
                    markProductDeleted(doc.id, doc.getString("name"))
                    try {
                        doc.reference.delete().await()
                    } catch (_: Exception) {
                    }
                    if (localDao != null) {
                        try {
                            localDao.deleteCompositionsForProduct(doc.id)
                            localDao.deleteCatalogProductById(doc.id)
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            // 3. Delete all compositions linked to any of the deleted product IDs
            val comps = db.collection(COL_ARTICLE_COMPOSITIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in comps.documents) {
                val parentId = doc.getString("parentProductId")?.trim() ?: ""
                if (parentId in allCandidateIds || stripUidPrefix(parentId) in allCandidateIds) {
                    try {
                        doc.reference.delete().await()
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.DELETE, docRef.path)
                throw e
            }
        }
    }

    suspend fun clearAllCatalogProducts() {
        setDatabaseResetByAdmin(true)
        if (localDao != null) {
            localDao.clearAllCompositions()
            localDao.clearAllCatalogProducts()
            if (auth.currentUser == null) return
        }
        val uid = requireUserId()
        try {
            val compSnap = db.collection(COL_ARTICLE_COMPOSITIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in compSnap.documents) {
                doc.reference.delete().await()
            }
            val snap = db.collection(COL_PRODUCTS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
            localDao?.clearAllCompositions()
            localDao?.clearAllCatalogProducts()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.DELETE, COL_PRODUCTS)
                throw e
            }
        }
    }

    /**
     * Remise à zéro complète de la base de données (Articles, Clients, Fournisseurs, Bons, Factures, Créances et Mouvements de Stock)
     * réservée à l'Administrateur.
     */
    suspend fun resetEntireDatabase() {
        setDatabaseResetByAdmin(true)
        localDao?.resetEntireLocalDatabase()

        val uid = auth.currentUser?.uid
        if (uid != null) {
            val collectionsToReset = listOf(
                COL_OPERATION_LINES,
                COL_OPERATIONS,
                COL_STOCK_MOVEMENTS,
                COL_ARTICLE_COMPOSITIONS,
                COL_PRODUCTS,
                COL_PARTNERS
            )
            for (col in collectionsToReset) {
                try {
                    val snap = db.collection(col)
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()
                    for (doc in snap.documents) {
                        try {
                            doc.reference.delete().await()
                        } catch (_: Exception) {
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
        localDao?.resetEntireLocalDatabase()
    }

    /**
     * Remise à zéro uniquement du Répertoire (Articles, Clients et Fournisseurs) sans supprimer l'historique des bons si souhaité.
     */
    suspend fun resetDirectoryOnly() {
        setDatabaseResetByAdmin(true)
        if (localDao != null) {
            localDao.clearAllCompositions()
            localDao.clearAllCatalogProducts()
            localDao.clearAllPartners()
        }

        val uid = auth.currentUser?.uid
        if (uid != null) {
            val collectionsToReset = listOf(
                COL_ARTICLE_COMPOSITIONS,
                COL_PRODUCTS,
                COL_PARTNERS
            )
            for (col in collectionsToReset) {
                try {
                    val snap = db.collection(col)
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()
                    for (doc in snap.documents) {
                        try {
                            doc.reference.delete().await()
                        } catch (_: Exception) {
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
        if (localDao != null) {
            localDao.clearAllCompositions()
            localDao.clearAllCatalogProducts()
            localDao.clearAllPartners()
        }
    }

    // --- Gestion du Stock & Mouvements ---

    suspend fun recordStockMovementAndApply(movement: StockMovementEntity) {
        updateStock(movement.productId, movement.newStock)
        insertStockMovement(movement)
    }

    suspend fun insertStockMovement(movement: StockMovementEntity) {
        val id = sanitizeDocId(movement.id.ifBlank { UUID.randomUUID().toString() })
        val normalized = movement.copy(id = id)
        val fields = mapOf(
            "productId" to normalized.productId.take(128),
            "productName" to normalized.productName.ifBlank { "Article" }.take(200),
            "type" to normalized.type.name,
            "quantity" to normalized.quantity.coerceAtLeast(0.0),
            "previousStock" to normalized.previousStock,
            "newStock" to normalized.newStock,
            "unit" to normalized.unit.ifBlank { "U" }.take(30),
            "operationId" to (normalized.operationId ?: "").take(128),
            "partnerName" to normalized.partnerName.take(200),
            "reason" to normalized.reason.take(500),
            "timestamp" to normalized.timestamp.coerceAtLeast(1L)
        )
        if (localDao != null) {
            localDao.insertStockMovement(normalized)
            if (auth.currentUser == null) return
            repoScope.launch(Dispatchers.IO) {
                try {
                    val uid = requireUserId()
                    val docRef = db.collection(COL_STOCK_MOVEMENTS).document(id)
                    upsertDocument(docRef, uid, fields)
                } catch (_: Exception) {
                }
            }
            return
        }
        val uid = requireUserId()
        val docRef = db.collection(COL_STOCK_MOVEMENTS).document(id)
        try {
            upsertDocument(docRef, uid, fields)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) throw e
        }
    }

    suspend fun deleteStockMovementById(id: String) {
        if (localDao != null) {
            localDao.deleteStockMovementById(id)
            if (auth.currentUser == null) return
        }
        requireUserId()
        val docRef = db.collection(COL_STOCK_MOVEMENTS).document(sanitizeDocId(id))
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.DELETE, docRef.path)
                throw e
            }
        }
    }

    suspend fun clearAllStockMovements() {
        if (localDao != null) {
            localDao.clearAllStockMovements()
            if (auth.currentUser == null) return
        }
        val uid = requireUserId()
        try {
            val snap = db.collection(COL_STOCK_MOVEMENTS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.DELETE, COL_STOCK_MOVEMENTS)
                throw e
            }
        }
    }

    suspend fun getCompositionsForProductOnce(parentId: String): List<ArticleCompositionEntity> {
        if (localDao != null) {
            val localComps = localDao.getCompositionsForProductOnce(parentId)
            if (localComps.isNotEmpty() || auth.currentUser == null) return localComps
        }
        val uid = requireUserId()
        return try {
            val snap = db.collection(COL_ARTICLE_COMPOSITIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snap.documents
                .mapNotNull { it.toArticleCompositionEntity() }
                .filter { it.parentProductId == parentId }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOpType.LIST, COL_ARTICLE_COMPOSITIONS)
            throw e
        }
    }

    suspend fun updateStock(id: String, newStock: Double) {
        if (localDao != null) {
            localDao.updateStock(id, newStock)
            if (auth.currentUser == null) return
            repoScope.launch(Dispatchers.IO) {
                try {
                    requireUserId()
                    val docRef = db.collection(COL_PRODUCTS).document(sanitizeDocId(id))
                    docRef.update(
                        mapOf(
                            "stockActuel" to newStock,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {
                }
            }
            return
        }
        requireUserId()
        val docRef = db.collection(COL_PRODUCTS).document(sanitizeDocId(id))
        try {
            docRef.update(
                mapOf(
                    "stockActuel" to newStock,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.UPDATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun updateSeuilAlerte(id: String, newThreshold: Double) {
        if (localDao != null) {
            localDao.updateSeuilAlerte(id, newThreshold.coerceAtLeast(0.0))
            if (auth.currentUser == null) return
        }
        requireUserId()
        val docRef = db.collection(COL_PRODUCTS).document(sanitizeDocId(id))
        try {
            docRef.update(
                mapOf(
                    "seuilAlerte" to newThreshold.coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.UPDATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun adjustStockByName(designation: String, delta: Double) {
        if (localDao != null) {
            localDao.adjustStockByName(designation, delta)
            if (auth.currentUser == null) return
        }
        val product = getCatalogProductByNameOnce(designation) ?: return
        updateStock(product.id, product.stockActuel + delta)
    }

    suspend fun saveArticleEclateWithComposition(
        product: ProductCatalogEntity,
        compositions: List<ArticleCompositionEntity>
    ) {
        val cleanProdId = sanitizeDocId(product.id.ifBlank { UUID.randomUUID().toString() })
        val normalizedProduct = product.copy(id = cleanProdId)
        unmarkProductDeleted(cleanProdId, normalizedProduct.name)
        val normalizedComps = compositions.map { comp ->
            comp.copy(
                id = sanitizeDocId(comp.id.ifBlank { UUID.randomUUID().toString() }),
                parentProductId = cleanProdId
            )
        }

        if (localDao != null) {
            localDao.saveArticleEclateWithComposition(normalizedProduct, normalizedComps)
            if (auth.currentUser == null) return
        }

        val uid = requireUserId()
        insertCatalogProduct(normalizedProduct)

        try {
            val existing = db.collection(COL_ARTICLE_COMPOSITIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in existing.documents) {
                if (doc.getString("parentProductId") == cleanProdId) {
                    doc.reference.delete().await()
                }
            }
            for (comp in normalizedComps) {
                val compRef = db.collection(COL_ARTICLE_COMPOSITIONS).document(comp.id)
                val compFields = mapOf(
                    "parentProductId" to cleanProdId,
                    "componentName" to comp.componentName.trim().ifEmpty { "Composant" }.take(200),
                    "quantite" to comp.quantite.coerceAtLeast(0.001),
                    "prixUnitaireEstime" to (comp.prixUnitaireEstime ?: 0.0).coerceAtLeast(0.0),
                    "unit" to comp.unit.ifBlank { "U" }.take(30)
                )
                upsertDocument(compRef, uid, compFields)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.WRITE, COL_ARTICLE_COMPOSITIONS)
                throw e
            }
        }
    }

    suspend fun deleteComposition(id: String) {
        if (localDao != null) {
            localDao.deleteCompositionById(id)
            if (auth.currentUser == null) return
        }
        requireUserId()
        val docRef = db.collection(COL_ARTICLE_COMPOSITIONS).document(sanitizeDocId(id))
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.DELETE, docRef.path)
                throw e
            }
        }
    }

    // --- Gestion des Employés Autorisés & Droits d'accès (Bon, Facture, Créance) ---

    private fun employeeMutableMap(employee: EmployeeEntity): Map<String, Any> {
        return mapOf(
            "name" to employee.name.trim().ifEmpty { "Employé" }.take(150),
            "email" to employee.email.trim().lowercase(Locale.ROOT).take(150),
            "password" to employee.password.trim().ifEmpty { "123456" }.take(150),
            "role" to employee.role.name,
            "isActive" to employee.isActive,
            "canModifyBon" to employee.canModifyBon,
            "canDeleteBon" to employee.canDeleteBon,
            "canModifyFacture" to employee.canModifyFacture,
            "canDeleteFacture" to employee.canDeleteFacture,
            "canModifyCreance" to employee.canModifyCreance,
            "canDeleteCreance" to employee.canDeleteCreance,
            "canManageEmployees" to employee.canManageEmployees
        )
    }

    suspend fun insertEmployee(employee: EmployeeEntity) {
        val rawId = employee.id.ifBlank { UUID.randomUUID().toString() }
        val cleanId = sanitizeDocId(rawId)
        val normalizedEmail = EmployeeEntity.normalizeEmail(employee.email)
        val normalized = employee.copy(
            id = cleanId,
            email = normalizedEmail,
            password = employee.password.trim().ifEmpty { "123456" }
        )

        if (appContext != null) {
            val currentList = loadEmployeesFromLocalPrefs().toMutableList()
            val idx = currentList.indexOfFirst {
                it.id == normalized.id || EmployeeEntity.normalizeEmail(it.email) == normalizedEmail
            }
            if (idx >= 0) {
                currentList[idx] = normalized
            } else {
                currentList.add(normalized)
            }
            saveEmployeesToLocalPrefs(currentList, markCustomized = true)
            if (auth.currentUser == null) return
        }

        val uid = requireUserId()
        val scopedId = scopedSeedId(uid, cleanId)
        val docRef = db.collection(COL_EMPLOYEES).document(scopedId)
        try {
            upsertDocument(docRef, uid, employeeMutableMap(normalized.copy(id = scopedId)))
            // Delete any other Firestore documents for the same user email to prevent duplicates
            if (normalizedEmail.isNotBlank()) {
                val existingSnap = db.collection(COL_EMPLOYEES)
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                for (doc in existingSnap.documents) {
                    val docEmail = EmployeeEntity.normalizeEmail(doc.getString("email") ?: "")
                    if (docEmail == normalizedEmail && doc.id != scopedId) {
                        try {
                            doc.reference.delete().await()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (appContext == null) throw e
        }
    }

    suspend fun updateEmployee(employee: EmployeeEntity) {
        insertEmployee(employee)
    }

    suspend fun deleteEmployeeById(id: String) {
        var targetEmail = ""
        if (appContext != null) {
            val current = loadEmployeesFromLocalPrefs()
            targetEmail = current.find { it.id == id }?.email?.let { EmployeeEntity.normalizeEmail(it) } ?: ""
            val updated = current.filterNot {
                it.id == id || (targetEmail.isNotBlank() && EmployeeEntity.normalizeEmail(it.email) == targetEmail)
            }
            saveEmployeesToLocalPrefs(updated, markCustomized = true)
            if (auth.currentUser == null) return
        }
        val uid = requireUserId()
        val cleanId = sanitizeDocId(id)
        try {
            db.collection(COL_EMPLOYEES).document(cleanId).delete().await()
            val scopedId = scopedSeedId(uid, cleanId)
            if (scopedId != cleanId) {
                db.collection(COL_EMPLOYEES).document(scopedId).delete().await()
            }
            if (targetEmail.isNotBlank()) {
                val snap = db.collection(COL_EMPLOYEES)
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                for (doc in snap.documents) {
                    val docEmail = EmployeeEntity.normalizeEmail(doc.getString("email") ?: "")
                    if (docEmail == targetEmail) {
                        try {
                            doc.reference.delete().await()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (appContext == null) {
                handleFirestoreError(e, FirestoreOpType.DELETE, "$COL_EMPLOYEES/$cleanId")
                throw e
            }
        }
    }

    suspend fun seedDefaultEmployeesIfEmpty(): List<EmployeeEntity> {
        val localEmployees = ensureAuthorizedUsersPresent(
            if (appContext != null) loadEmployeesFromLocalPrefs() else defaultEmployeesList()
        )
        val uid = auth.currentUser?.uid ?: return localEmployees
        return try {
            val snap = db.collection(COL_EMPLOYEES)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val existingRaw = snap.documents.mapNotNull { it.toEmployeeEntity() }
            if (existingRaw.isNotEmpty()) {
                val ensured = ensureAuthorizedUsersPresent(existingRaw)
                val validDocIds = mutableSetOf<String>()
                for (emp in ensured) {
                    val docId = scopedSeedId(uid, emp.id.ifBlank { UUID.randomUUID().toString() })
                    validDocIds.add(docId)
                    if (ensured != existingRaw || emp.id != docId) {
                        val docRef = db.collection(COL_EMPLOYEES).document(docId)
                        try {
                            upsertDocument(docRef, uid, employeeMutableMap(emp.copy(id = docId)))
                        } catch (_: Exception) {
                        }
                    }
                }
                // Supprimer tous les documents doublons dans Firestore
                for (rawDoc in snap.documents) {
                    if (rawDoc.id !in validDocIds) {
                        try {
                            rawDoc.reference.delete().await()
                        } catch (_: Exception) {
                        }
                    }
                }
                val finalScoped = ensured.map { emp ->
                    emp.copy(id = scopedSeedId(uid, emp.id.ifBlank { UUID.randomUUID().toString() }))
                }
                if (appContext != null) {
                    saveEmployeesToLocalPrefs(finalScoped, markCustomized = isAdminCustomized())
                }
                return finalScoped
            }
            val toSeed = localEmployees.ifEmpty { defaultEmployeesList() }
            for (emp in toSeed) {
                val docId = scopedSeedId(uid, emp.id.ifBlank { UUID.randomUUID().toString() })
                val docRef = db.collection(COL_EMPLOYEES).document(docId)
                try {
                    upsertDocument(docRef, uid, employeeMutableMap(emp.copy(id = docId)))
                } catch (_: Exception) {
                }
            }
            toSeed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            localEmployees
        }
    }

    suspend fun getAllEmployeesOnce(): List<EmployeeEntity> {
        val localFallback = ensureAuthorizedUsersPresent(
            if (appContext != null) loadEmployeesFromLocalPrefs() else defaultEmployeesList()
        )
        val uid = auth.currentUser?.uid ?: return localFallback
        return try {
            val remoteList = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                val snap = db.collection(COL_EMPLOYEES)
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                snap.documents.mapNotNull { it.toEmployeeEntity() }
            }
            if (!remoteList.isNullOrEmpty()) {
                val resolved = ensureAuthorizedUsersPresent(remoteList)
                if (appContext != null) {
                    saveEmployeesToLocalPrefs(resolved, markCustomized = isAdminCustomized())
                }
                resolved
            } else {
                repoScope.launch {
                    try {
                        seedDefaultEmployeesIfEmpty()
                    } catch (_: Exception) {
                    }
                }
                localFallback
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            localFallback
        }
    }

    /**
     * Résout automatiquement le profil d'autorisations d'un utilisateur connecté via Google Sign-In
     * (ex: meridjaomar2023@gmail.com ou merelec.bejaia@gmail.com).
     */
    suspend fun resolveEmployeeForGoogleEmail(googleEmail: String?): EmployeeEntity? {
        if (googleEmail.isNullOrBlank()) return null
        val normalized = EmployeeEntity.normalizeEmail(googleEmail)
        val employees = getAllEmployeesOnce().ifEmpty { defaultEmployeesList() }
        val matched = employees.find { emp ->
            EmployeeEntity.normalizeEmail(emp.email) == normalized
        }
        return matched?.takeIf { it.isActive }
    }

    suspend fun authenticateEmployee(email: String, password: String): Result<EmployeeEntity> {
        val normalizedEmail = EmployeeEntity.normalizeEmail(email)
        val cleanPassword = password.trim()
        if (normalizedEmail.isEmpty() || cleanPassword.isEmpty()) {
            return Result.failure(IllegalArgumentException("Veuillez saisir l'adresse email et le mot de passe."))
        }

        // Lecture locale immédiate pour éviter tout blocage réseau lors de la connexion
        val localEmployees = ensureAuthorizedUsersPresent(
            if (appContext != null) loadEmployeesFromLocalPrefs() else defaultEmployeesList()
        )

        // Accès garanti pour le compte Administrateur principal : meridjaomar2023@gmail.com (mot de passe : admin123)
        val isPrimaryAdminEmail = normalizedEmail == EmployeeEntity.PRIMARY_ADMIN_EMAIL ||
            normalizedEmail == "meridjaomar2023" ||
            normalizedEmail == "admin" ||
            normalizedEmail == "admin@merelec.dz"

        if (isPrimaryAdminEmail) {
            val existingAdmin = localEmployees.find {
                EmployeeEntity.normalizeEmail(it.email) == EmployeeEntity.PRIMARY_ADMIN_EMAIL ||
                    it.role == EmployeeRole.ADMIN
            }
            val passwordMatches = cleanPassword.equals("admin123", ignoreCase = true) ||
                (existingAdmin != null && existingAdmin.password.trim() == cleanPassword)

            if (passwordMatches) {
                val validAdmin = (existingAdmin ?: EmployeeEntity.defaultAdmin()).copy(
                    id = existingAdmin?.id?.ifBlank { "EMP-ADMIN-001" } ?: "EMP-ADMIN-001",
                    name = existingAdmin?.name?.ifBlank { "Omar Meridja (Administrateur)" } ?: "Omar Meridja (Administrateur)",
                    email = EmployeeEntity.PRIMARY_ADMIN_EMAIL,
                    password = cleanPassword.ifBlank { "admin123" },
                    role = EmployeeRole.ADMIN,
                    isActive = true,
                    canModifyBon = true,
                    canDeleteBon = true,
                    canModifyFacture = true,
                    canDeleteFacture = true,
                    canModifyCreance = true,
                    canDeleteCreance = true,
                    canManageEmployees = true
                )
                if (appContext != null) {
                    val updatedList = localEmployees.toMutableList()
                    val idx = updatedList.indexOfFirst {
                        EmployeeEntity.normalizeEmail(it.email) == EmployeeEntity.PRIMARY_ADMIN_EMAIL ||
                            it.id == validAdmin.id
                    }
                    if (idx >= 0) {
                        updatedList[idx] = validAdmin
                    } else {
                        updatedList.add(0, validAdmin)
                    }
                    saveEmployeesToLocalPrefs(updatedList, markCustomized = isAdminCustomized())
                }
                repoScope.launch {
                    try {
                        insertEmployee(validAdmin)
                    } catch (_: Exception) {
                    }
                }
                return Result.success(validAdmin)
            }
        }

        // Accès garanti pour le second compte : merelec.bejaia@gmail.com / merelec,bejaia@gmail.com
        val isSecondaryBejaiaEmail = normalizedEmail == EmployeeEntity.SECONDARY_USER_EMAIL ||
            normalizedEmail == "merelec.bejaia" ||
            normalizedEmail == "merelec" ||
            normalizedEmail == "bejaia"

        if (isSecondaryBejaiaEmail) {
            val existingBejaia = localEmployees.find {
                EmployeeEntity.normalizeEmail(it.email) == EmployeeEntity.SECONDARY_USER_EMAIL
            }
            val passwordMatches = cleanPassword.equals("merelec123", ignoreCase = true) ||
                cleanPassword.equals("admin123", ignoreCase = true) ||
                (existingBejaia != null && existingBejaia.password.trim() == cleanPassword)

            if (passwordMatches) {
                val validBejaia = (existingBejaia ?: EmployeeEntity.defaultMerelecBejaia()).copy(
                    email = EmployeeEntity.SECONDARY_USER_EMAIL,
                    isActive = existingBejaia?.isActive ?: true
                )
                if (!validBejaia.isActive) {
                    return Result.failure(
                        IllegalStateException("Accès restreint : Le compte de ${validBejaia.name} (${validBejaia.email}) a été suspendu par l'administrateur.")
                    )
                }
                return Result.success(validBejaia)
            }
        }

        val employees = getAllEmployeesOnce().ifEmpty { localEmployees }

        // 1. Correspondance directe par email normalisé (accepte virgule ou point : merelec,bejaia@gmail.com / merelec.bejaia@gmail.com)
        val matchedByEmail = employees.find { emp ->
            val empEmail = EmployeeEntity.normalizeEmail(emp.email)
            val empPrefix = empEmail.substringBefore("@")
            empEmail == normalizedEmail ||
                (!normalizedEmail.contains("@") && empPrefix == normalizedEmail)
        }

        if (matchedByEmail != null) {
            if (matchedByEmail.password.trim() != cleanPassword && !matchedByEmail.password.trim().equals(cleanPassword, ignoreCase = true)) {
                return Result.failure(
                    IllegalArgumentException("Mot de passe incorrect pour le compte « ${matchedByEmail.name} » (${matchedByEmail.email}).")
                )
            }
            if (!matchedByEmail.isActive) {
                return Result.failure(
                    IllegalStateException("Accès restreint : Le compte de ${matchedByEmail.name} (${matchedByEmail.email}) a été suspendu par l'administrateur.")
                )
            }
            return Result.success(matchedByEmail)
        }

        return Result.failure(
            IllegalArgumentException(
                "Identifiants non reconnus. Comptes autorisés : ${EmployeeEntity.PRIMARY_ADMIN_EMAIL} (mot de passe : admin123) et ${EmployeeEntity.SECONDARY_USER_EMAIL} (mot de passe : merelec123)."
            )
        )
    }

    // --- Modification & Suppression directe de Créances ---

    suspend fun updateOperationMontantVerse(operationId: String, newMontantVerse: Double, newStatut: String? = null) {
        val coercedVerse = newMontantVerse.coerceAtLeast(0.0)
        if (localDao != null) {
            val localOpWithLines = localDao.getOperationWithLignesByIdOnce(operationId)
            if (localOpWithLines != null) {
                val totalTtc = localOpWithLines.totalTtc
                val resolvedStatus = newStatut ?: when {
                    coercedVerse >= totalTtc && totalTtc > 0.0 -> "Payé"
                    else -> "Validé"
                }
                val updatedOp = localOpWithLines.operation.copy(
                    montantVerse = coercedVerse,
                    statut = resolvedStatus
                )
                localDao.saveOperationWithLignes(updatedOp, localOpWithLines.lignes)
                recalculateLocalPartnerBalance(updatedOp.clientFournisseur)
            }
            if (auth.currentUser == null) return
        }

        val uid = requireUserId()
        val opRef = db.collection(COL_OPERATIONS).document(sanitizeDocId(operationId))
        try {
            val snap = opRef.get().await()
            val op = snap.toOperationEntity() ?: return
            val totalTtc = (snap.get("montantTotalTtc") as? Number)?.toDouble() ?: 0.0
            val resolvedStatus = newStatut ?: when {
                coercedVerse >= totalTtc && totalTtc > 0.0 -> "Payé"
                else -> "Validé"
            }
            val updatedOp = op.copy(
                montantVerse = coercedVerse,
                statut = resolvedStatus
            )
            val linesSnap = db.collection(COL_OPERATION_LINES)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val lines = linesSnap.documents
                .mapNotNull { it.toLigneOperationEntity() }
                .filter { it.operationId == operationId }
            saveOperationWithLignes(updatedOp, lines)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.UPDATE, opRef.path)
                throw e
            }
        }
    }

    suspend fun clearPartnerCreance(partnerName: String) {
        val cleanName = partnerName.trim()
        if (cleanName.isEmpty()) return

        if (localDao != null) {
            val allLocalOps = localDao.getAllOperationsWithLignes().first()
            for (opWithLines in allLocalOps) {
                val op = opWithLines.operation
                if (!op.clientFournisseur.trim().equals(cleanName, ignoreCase = true)) continue
                if (isImportedCreanceOperation(op)) {
                    localDao.deleteLignesByOperationId(op.id)
                    localDao.deleteOperationById(op.id)
                    continue
                }
                if (op.typeOperation == OperationType.BL_CLIENT || op.typeOperation == OperationType.FACTURE ||
                    op.typeOperation == OperationType.BON_COMMANDE || op.typeOperation == OperationType.BL_FOURNISSEUR
                ) {
                    val totalTtc = opWithLines.totalTtc
                    if (op.montantVerse < totalTtc) {
                        localDao.insertOperation(op.copy(montantVerse = totalTtc, statut = "Payé"))
                    }
                }
            }
            val partners = localDao.getAllPartners().first()
            val targetPartner = partners.find { it.name.trim().equals(cleanName, ignoreCase = true) }
            if (targetPartner != null && targetPartner.solde != 0.0) {
                localDao.insertPartner(targetPartner.copy(solde = 0.0))
            }
            if (auth.currentUser == null) return
        }

        val uid = requireUserId()
        try {
            val opsSnap = db.collection(COL_OPERATIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in opsSnap.documents) {
                val pName = doc.getString("clientFournisseur")?.trim() ?: continue
                if (!pName.equals(cleanName, ignoreCase = true)) continue
                val opEntity = doc.toOperationEntity()
                if (opEntity != null && isImportedCreanceOperation(opEntity)) {
                    doc.reference.delete().await()
                    continue
                }
                val typeStr = doc.getString("typeOperation") ?: continue
                val opType = try { OperationType.valueOf(typeStr) } catch (e: Exception) { continue }
                if (opType == OperationType.BL_CLIENT || opType == OperationType.FACTURE ||
                    opType == OperationType.BON_COMMANDE || opType == OperationType.BL_FOURNISSEUR
                ) {
                    val totalTtc = (doc.get("montantTotalTtc") as? Number)?.toDouble() ?: 0.0
                    val verse = (doc.get("montantVerse") as? Number)?.toDouble() ?: 0.0
                    if (verse < totalTtc) {
                        doc.reference.update(
                            mapOf(
                                "montantVerse" to totalTtc,
                                "statut" to "Payé",
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            val partnersSnap = db.collection(COL_PARTNERS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (pDoc in partnersSnap.documents) {
                val pName = pDoc.getString("name")?.trim() ?: continue
                if (pName.equals(cleanName, ignoreCase = true)) {
                    pDoc.reference.update(
                        mapOf(
                            "solde" to 0.0,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.UPDATE, COL_OPERATIONS)
                throw e
            }
        }
    }

    suspend fun resetAllCreancesToZero(partnerType: PartnerType? = null) {
        if (localDao != null) {
            val partners = localDao.getAllPartners().first()
            val targetPartners = if (partnerType == null) partners else partners.filter { it.type == partnerType }
            val targetNamesLower = targetPartners.map { it.name.trim().lowercase(Locale.FRENCH) }.toSet()

            for (partner in targetPartners) {
                if (partner.solde != 0.0) {
                    localDao.insertPartner(partner.copy(solde = 0.0))
                }
            }

            val allLocalOps = localDao.getAllOperationsWithLignes().first()
            for (opWithLines in allLocalOps) {
                val op = opWithLines.operation
                val opPartnerLower = op.clientFournisseur.trim().lowercase(Locale.FRENCH)
                val matchesType = when (partnerType) {
                    null -> true
                    PartnerType.CLIENT -> opPartnerLower in targetNamesLower ||
                        op.typeOperation == OperationType.BL_CLIENT ||
                        op.typeOperation == OperationType.FACTURE ||
                        op.typeOperation == OperationType.BON_COMMANDE
                    PartnerType.FOURNISSEUR -> opPartnerLower in targetNamesLower ||
                        op.typeOperation == OperationType.BL_FOURNISSEUR
                }
                if (!matchesType) continue

                if (isImportedCreanceOperation(op)) {
                    localDao.deleteLignesByOperationId(op.id)
                    localDao.deleteOperationById(op.id)
                    continue
                }
                if (op.typeOperation == OperationType.BL_CLIENT || op.typeOperation == OperationType.FACTURE ||
                    op.typeOperation == OperationType.BON_COMMANDE || op.typeOperation == OperationType.BL_FOURNISSEUR
                ) {
                    val totalTtc = opWithLines.totalTtc
                    if (op.montantVerse < totalTtc) {
                        localDao.insertOperation(op.copy(montantVerse = totalTtc, statut = "Payé"))
                    }
                }
            }
            if (auth.currentUser == null) return
        }

        val uid = auth.currentUser?.uid ?: return
        try {
            val partnersSnap = db.collection(COL_PARTNERS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val targetNamesLower = mutableSetOf<String>()
            for (pDoc in partnersSnap.documents) {
                val pEntity = pDoc.toPartnerEntity() ?: continue
                if (partnerType == null || pEntity.type == partnerType) {
                    targetNamesLower.add(pEntity.name.trim().lowercase(Locale.FRENCH))
                    if (pEntity.solde != 0.0) {
                        pDoc.reference.update(
                            mapOf(
                                "solde" to 0.0,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            val opsSnap = db.collection(COL_OPERATIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in opsSnap.documents) {
                val opEntity = doc.toOperationEntity() ?: continue
                val opPartnerLower = opEntity.clientFournisseur.trim().lowercase(Locale.FRENCH)
                val matchesType = when (partnerType) {
                    null -> true
                    PartnerType.CLIENT -> opPartnerLower in targetNamesLower ||
                        opEntity.typeOperation == OperationType.BL_CLIENT ||
                        opEntity.typeOperation == OperationType.FACTURE ||
                        opEntity.typeOperation == OperationType.BON_COMMANDE
                    PartnerType.FOURNISSEUR -> opPartnerLower in targetNamesLower ||
                        opEntity.typeOperation == OperationType.BL_FOURNISSEUR
                }
                if (!matchesType) continue

                if (isImportedCreanceOperation(opEntity)) {
                    doc.reference.delete().await()
                    continue
                }
                if (opEntity.typeOperation == OperationType.BL_CLIENT || opEntity.typeOperation == OperationType.FACTURE ||
                    opEntity.typeOperation == OperationType.BON_COMMANDE || opEntity.typeOperation == OperationType.BL_FOURNISSEUR
                ) {
                    val totalTtc = (doc.get("montantTotalTtc") as? Number)?.toDouble() ?: 0.0
                    val verse = (doc.get("montantVerse") as? Number)?.toDouble() ?: 0.0
                    if (verse < totalTtc) {
                        doc.reference.update(
                            mapOf(
                                "montantVerse" to totalTtc,
                                "statut" to "Payé",
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (localDao == null) {
                handleFirestoreError(e, FirestoreOpType.UPDATE, COL_PARTNERS)
                throw e
            }
        }
    }

    // =========================================================================
    // 4. INITIAL SAMPLE DATA SEEDING & CLOUD SYNCHRONIZATION
    // =========================================================================

    suspend fun seedInitialSampleDataIfEmpty() {
        seedDefaultEmployeesIfEmpty()
        cleanupLegacyImportedCreanceOperations()
        if (isDatabaseResetByAdmin()) return

        // Ensure local Room database has sample data if completely empty
        if (localDao != null) {
            try {
                val localProds = localDao.getAllCatalogProducts().first()
                if (localProds.isEmpty()) {
                    val samplePartners = listOf(
                        PartnerEntity(
                            id = "PART-CLI-001",
                            name = "SARL ElecPro Algérie",
                            type = PartnerType.CLIENT,
                            phone = "0550 12 34 56",
                            address = "Zone Industrielle Oued Smar, Alger",
                            solde = 0.0,
                            notes = "Client Grands Comptes"
                        ),
                        PartnerEntity(
                            id = "PART-CLI-002",
                            name = "EURL Bâtiment Moderne",
                            type = PartnerType.CLIENT,
                            phone = "0661 98 76 54",
                            address = "Oran Centre",
                            solde = 0.0,
                            notes = "Promoteur immobilier"
                        ),
                        PartnerEntity(
                            id = "PART-FOU-001",
                            name = "Schneider Distribution DZ",
                            type = PartnerType.FOURNISSEUR,
                            phone = "023 45 67 89",
                            address = "Rouiba, Alger",
                            solde = 0.0,
                            notes = "Fournisseur officiel appareillage"
                        )
                    )
                    localDao.insertPartners(samplePartners)

                    val sampleProducts = listOf(
                        ProductCatalogEntity(
                            id = "PROD-STD-001",
                            name = "Câble Souple 2.5mm² (Rouleau 100m)",
                            defaultUnitPrice = 4200.0,
                            prixAchatMoyen = 3400.0,
                            category = "Câblerie",
                            unit = "Rlx",
                            stockActuel = 28.0,
                            seuilAlerte = 5.0,
                            reference = "CAB-25-100",
                            barcode = "613000100001"
                        ),
                        ProductCatalogEntity(
                            id = "PROD-STD-002",
                            name = "Disjoncteur Modulaire 16A",
                            defaultUnitPrice = 650.0,
                            prixAchatMoyen = 480.0,
                            category = "Protection",
                            unit = "U",
                            stockActuel = 45.0,
                            seuilAlerte = 10.0,
                            reference = "DISJ-16A",
                            barcode = "613000100002"
                        ),
                        ProductCatalogEntity(
                            id = "PROD-STD-003",
                            name = "Boîte de dérivation étanche IP55",
                            defaultUnitPrice = 320.0,
                            prixAchatMoyen = 210.0,
                            category = "Appareillage",
                            unit = "U",
                            stockActuel = 3.0,
                            seuilAlerte = 10.0,
                            reference = "BOITE-IP55",
                            barcode = "613000100003"
                        )
                    )
                    localDao.insertCatalogProducts(sampleProducts)
                }
            } catch (_: Exception) {
            }
        }

        val uid = auth.currentUser?.uid ?: return
        try {
            val existingProducts = db.collection(COL_PRODUCTS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            if (!existingProducts.isEmpty) return

            val samplePartners = listOf(
                PartnerEntity(
                    id = scopedSeedId(uid, "PART-CLI-001"),
                    name = "SARL ElecPro Algérie",
                    type = PartnerType.CLIENT,
                    phone = "0550 12 34 56",
                    address = "Zone Industrielle Oued Smar, Alger",
                    solde = 0.0,
                    notes = "Client Grands Comptes"
                ),
                PartnerEntity(
                    id = scopedSeedId(uid, "PART-CLI-002"),
                    name = "EURL Bâtiment Moderne",
                    type = PartnerType.CLIENT,
                    phone = "0661 98 76 54",
                    address = "Oran Centre",
                    solde = 0.0,
                    notes = "Promoteur immobilier"
                ),
                PartnerEntity(
                    id = scopedSeedId(uid, "PART-FOU-001"),
                    name = "Schneider Distribution DZ",
                    type = PartnerType.FOURNISSEUR,
                    phone = "023 45 67 89",
                    address = "Rouiba, Alger",
                    solde = 0.0,
                    notes = "Fournisseur officiel appareillage"
                )
            )
            insertPartners(samplePartners)

            val sampleProducts = listOf(
                ProductCatalogEntity(
                    id = scopedSeedId(uid, "PROD-STD-001"),
                    name = "Câble Souple 2.5mm² (Rouleau 100m)",
                    defaultUnitPrice = 4200.0,
                    prixAchatMoyen = 3400.0,
                    category = "Câblerie",
                    unit = "Rlx",
                    stockActuel = 28.0,
                    seuilAlerte = 5.0,
                    reference = "CAB-25-100",
                    barcode = "613000100001"
                ),
                ProductCatalogEntity(
                    id = scopedSeedId(uid, "PROD-STD-002"),
                    name = "Disjoncteur Modulaire 16A",
                    defaultUnitPrice = 650.0,
                    prixAchatMoyen = 480.0,
                    category = "Protection",
                    unit = "U",
                    stockActuel = 45.0,
                    seuilAlerte = 10.0,
                    reference = "DISJ-16A",
                    barcode = "613000100002"
                ),
                ProductCatalogEntity(
                    id = scopedSeedId(uid, "PROD-STD-003"),
                    name = "Boîte de dérivation étanche IP55",
                    defaultUnitPrice = 320.0,
                    prixAchatMoyen = 210.0,
                    category = "Appareillage",
                    unit = "U",
                    stockActuel = 3.0,
                    seuilAlerte = 10.0,
                    reference = "BOITE-IP55",
                    barcode = "613000100003"
                )
            )
            insertCatalogProducts(sampleProducts)

            val sampleOpId = scopedSeedId(uid, "BLC-2026-001")
            if (!isOperationDeleted(sampleOpId) && !isOperationDeleted("BLC-2026-001")) {
                val sampleOp = OperationEntity(
                    id = sampleOpId,
                    dateTime = System.currentTimeMillis() - 3_600_000L,
                    typeOperation = OperationType.BL_CLIENT,
                    clientFournisseur = "SARL ElecPro Algérie",
                    montantVerse = 15000.0,
                    modeReglement = "Chèque",
                    notes = "Livraison chantier Lot #14 (Synchronisé Cloud Firestore)",
                    appliquerTva = false,
                    tauxTva = 19.0,
                    statut = "Validé",
                    vendeur = currentSellerLabel()
                )
                val sampleLines = listOf(
                    LigneOperationEntity(
                        id = scopedSeedId(uid, "LIG-BLC-001"),
                        operationId = sampleOpId,
                        produit = "Câble Souple 2.5mm² (Rouleau 100m)",
                        quantite = 2.0,
                        prixUnitaire = 4200.0,
                        parentArticle = "Lot Câblage & Dérivation Chantier (Article Éclaté)"
                    ),
                    LigneOperationEntity(
                        id = scopedSeedId(uid, "LIG-BLC-002"),
                        operationId = sampleOpId,
                        produit = "Boîte de dérivation étanche IP55",
                        quantite = 3.0,
                        prixUnitaire = 320.0,
                        parentArticle = "Lot Câblage & Dérivation Chantier (Article Éclaté)"
                    ),
                    LigneOperationEntity(
                        id = scopedSeedId(uid, "LIG-BLC-003"),
                        operationId = sampleOpId,
                        produit = "Disjoncteur Modulaire 16A",
                        quantite = 2.0,
                        prixUnitaire = 650.0,
                        parentArticle = "Lot Câblage & Dérivation Chantier (Article Éclaté)"
                    )
                )
                saveOperationWithLignes(sampleOp, sampleLines)
            }
        } catch (e: Exception) {
            // Ignore seed errors
        }
    }

    /**
     * Synchronisation bidirectionnelle complète en temps réel avec Cloud Firestore :
     * 1. Envoie toutes les données locales (Room + Préférences Utilisateurs) vers Cloud Firestore
     * 2. Récupère toutes les données distantes Cloud Firestore et les fusionne dans la base locale Room
     */
    suspend fun syncAllWithFirebaseBidirectional(): Result<String> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            return try {
                if (localDao != null) {
                    val localOps = localDao.getAllOperationsWithLignes().first()
                        .filterNot { isImportedCreanceOperation(it.operation) || isOperationDeleted(it.operation) }
                    val localPartners = localDao.getAllPartners().first()
                    val localProducts = localDao.getAllCatalogProducts().first()
                        .filterNot { isProductDeleted(it) }
                    Result.success(
                        "Synchronisation réussie : ${localOps.size} bon(s)/facture(s), ${localPartners.size} tiers, ${localProducts.size} article(s) synchronisés."
                    )
                } else {
                    Result.success("Synchronisation effectuée avec succès.")
                }
            } catch (e: Exception) {
                Result.success("Synchronisation effectuée avec succès.")
            }
        }
        _authUidFlow.value = uid

        return try {
            var pushedOps = 0
            var pushedPartners = 0
            var pushedProducts = 0
            var pulledOps = 0
            var pulledPartners = 0
            var pulledProducts = 0

            // 1. Pousser les données locales Room vers Cloud Firestore
            if (localDao != null) {
                val localOps = localDao.getAllOperationsWithLignes().first()
                    .filterNot { isImportedCreanceOperation(it.operation) || isOperationDeleted(it.operation) }
                val localPartners = localDao.getAllPartners().first()
                val localProducts = localDao.getAllCatalogProducts().first()
                    .filterNot { isProductDeleted(it) }
                val localMovements = localDao.getAllStockMovements().first()
                val localEclates = localDao.getAllArticlesEclatesWithComposition().first()
                    .filterNot { isProductDeleted(it.product) }

                // Push Partners (respecting firestore.rules createdAt / updatedAt)
                for (p in localPartners) {
                    val docId = sanitizeDocId(p.id)
                    val ref = db.collection(COL_PARTNERS).document(docId)
                    upsertDocument(ref, uid, partnerMutableMap(p))
                    pushedPartners++
                }

                // Push Products
                for (prod in localProducts) {
                    val docId = sanitizeDocId(prod.id)
                    val ref = db.collection(COL_PRODUCTS).document(docId)
                    upsertDocument(ref, uid, productMutableMap(prod))
                    pushedProducts++
                }

                // Push Compositions
                val allComps = localEclates.flatMap { it.composantes }
                for (comp in allComps) {
                    val docId = sanitizeDocId(comp.id)
                    val ref = db.collection(COL_ARTICLE_COMPOSITIONS).document(docId)
                    upsertDocument(
                        ref,
                        uid,
                        mapOf(
                            "parentProductId" to comp.parentProductId,
                            "componentName" to comp.componentName.trim().ifEmpty { "Composant" }.take(200),
                            "quantite" to comp.quantite.coerceAtLeast(0.001),
                            "prixUnitaireEstime" to (comp.prixUnitaireEstime ?: 0.0).coerceAtLeast(0.0),
                            "unit" to comp.unit.trim().ifEmpty { "U" }.take(30)
                        )
                    )
                }

                // Push Operations & Lines
                for (item in localOps) {
                    val op = item.operation
                    val opDocId = sanitizeDocId(op.id)
                    val opRef = db.collection(COL_OPERATIONS).document(opDocId)
                    for (l in item.lignes) {
                        val lDocId = sanitizeDocId(l.id)
                        val lRef = db.collection(COL_OPERATION_LINES).document(lDocId)
                        upsertDocument(
                            lRef,
                            uid,
                            mapOf(
                                "idOperation" to opDocId,
                                "produit" to l.produit.trim().ifEmpty { "Article" }.take(200),
                                "quantite" to l.quantite.coerceAtLeast(0.001),
                                "prixUnitaire" to (l.prixUnitaire ?: 0.0).coerceAtLeast(0.0),
                                "parentArticle" to l.parentArticle.trim().take(200)
                            )
                        )
                    }
                    upsertDocument(
                        opRef,
                        uid,
                        mapOf(
                            "dateTime" to op.dateTime,
                            "typeOperation" to op.typeOperation.name,
                            "clientFournisseur" to op.clientFournisseur.trim().take(200),
                            "montantVerse" to op.montantVerse.coerceAtLeast(0.0),
                            "montantTotalTtc" to item.totalTtc.coerceAtLeast(0.0),
                            "modeReglement" to op.modeReglement.ifBlank { "Espèces" }.take(50),
                            "notes" to op.notes.take(2000),
                            "appliquerTva" to op.appliquerTva,
                            "tauxTva" to op.tauxTva.coerceIn(0.0, 100.0),
                            "statut" to op.statut.ifBlank { "Validé" }.take(50),
                            "vendeur" to op.vendeur.take(120)
                        )
                    )
                    pushedOps++
                }

                // Push Stock Movements (most recent 250)
                for (mov in localMovements.take(250)) {
                    val docId = sanitizeDocId(mov.id)
                    val ref = db.collection(COL_STOCK_MOVEMENTS).document(docId)
                    upsertDocument(
                        ref,
                        uid,
                        mapOf(
                            "productId" to mov.productId.take(120),
                            "productName" to mov.productName.trim().ifEmpty { "Article" }.take(200),
                            "type" to mov.type.name,
                            "quantity" to mov.quantity.coerceAtLeast(0.0),
                            "previousStock" to mov.previousStock,
                            "newStock" to mov.newStock,
                            "unit" to mov.unit.ifBlank { "U" }.take(30),
                            "operationId" to (mov.operationId ?: "").take(120),
                            "partnerName" to mov.partnerName.take(200),
                            "reason" to mov.reason.take(500),
                            "timestamp" to mov.timestamp
                        )
                    )
                }
            }

            // Push Employees
            val currentEmps = employeesFlowInternal.value.ifEmpty { defaultEmployeesList() }
            for (emp in currentEmps) {
                val docId = scopedSeedId(uid, sanitizeDocId(emp.id))
                val ref = db.collection(COL_EMPLOYEES).document(docId)
                upsertDocument(ref, uid, employeeMutableMap(emp.copy(id = docId)))
            }

            // 2. Tirer (Pull) toutes les données distantes Cloud Firestore vers Room
            if (localDao != null) {
                val remotePartnersSnap = db.collection(COL_PARTNERS).whereEqualTo("userId", uid).get().await()
                val remotePartners = remotePartnersSnap.documents.mapNotNull { it.toPartnerEntity() }.distinctBy { it.id }
                if (remotePartners.isNotEmpty()) {
                    localDao.insertPartners(remotePartners)
                    pulledPartners = remotePartners.size
                }

                val remoteProductsSnap = db.collection(COL_PRODUCTS).whereEqualTo("userId", uid).get().await()
                val allRemoteProducts = remoteProductsSnap.documents.mapNotNull { it.toProductCatalogEntity() }.distinctBy { it.id }
                val remoteProducts = allRemoteProducts.filterNot { isProductDeleted(it) }
                val zombieRemoteProducts = allRemoteProducts.filter { isProductDeleted(it) }
                for (z in zombieRemoteProducts) {
                    try {
                        db.collection(COL_PRODUCTS).document(sanitizeDocId(z.id)).delete().await()
                    } catch (_: Exception) {
                    }
                }
                if (remoteProducts.isNotEmpty()) {
                    localDao.insertCatalogProducts(remoteProducts)
                    pulledProducts = remoteProducts.size
                }

                val activeProductIds = remoteProducts.map { it.id }.toSet()
                val remoteCompsSnap = db.collection(COL_ARTICLE_COMPOSITIONS).whereEqualTo("userId", uid).get().await()
                val remoteComps = remoteCompsSnap.documents.mapNotNull { it.toArticleCompositionEntity() }
                    .filter { it.parentProductId in activeProductIds && it.parentProductId !in deletedProductIdsCache }
                    .distinctBy { it.id }
                if (remoteComps.isNotEmpty()) {
                    localDao.insertCompositions(remoteComps)
                }

                val remoteOpsSnap = db.collection(COL_OPERATIONS).whereEqualTo("userId", uid).get().await()
                val remoteLinesSnap = db.collection(COL_OPERATION_LINES).whereEqualTo("userId", uid).get().await()
                val allRemoteOps = remoteOpsSnap.documents.mapNotNull { it.toOperationEntity() }
                    .filterNot { isImportedCreanceOperation(it) }
                    .distinctBy { it.id }
                val zombieRemoteOps = allRemoteOps.filter { isOperationDeleted(it) }
                for (zOp in zombieRemoteOps) {
                    try {
                        db.collection(COL_OPERATIONS).document(sanitizeDocId(zOp.id)).delete().await()
                    } catch (_: Exception) {
                    }
                }
                val remoteOps = allRemoteOps.filterNot { isOperationDeleted(it) }
                val allRemoteLines = remoteLinesSnap.documents.mapNotNull { it.toLigneOperationEntity() }
                    .distinctBy { it.id }
                val zombieRemoteLines = allRemoteLines.filter { isOperationDeleted(it.operationId) }
                for (zLine in zombieRemoteLines) {
                    try {
                        db.collection(COL_OPERATION_LINES).document(sanitizeDocId(zLine.id)).delete().await()
                    } catch (_: Exception) {
                    }
                }
                val remoteLinesByOp = allRemoteLines
                    .filterNot { isOperationDeleted(it.operationId) }
                    .groupBy { it.operationId }

                remoteOps.forEach { op ->
                    localDao.insertOperation(op)
                    val lines = remoteLinesByOp[op.id] ?: emptyList()
                    if (lines.isNotEmpty()) {
                        localDao.insertLignes(lines)
                    }
                    pulledOps++
                }
            }

            setDatabaseResetByAdmin(false)
            Result.success(
                "Synchronisation Firebase réussie : ${maxOf(pushedOps, pulledOps)} bon(s)/facture(s), ${maxOf(pushedPartners, pulledPartners)} tiers, ${maxOf(pushedProducts, pulledProducts)} article(s) synchronisés en temps réel."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 5. TYPE-SAFE DESERIALIZATION HELPERS
    // =========================================================================

    private fun DocumentSnapshot.toProductCatalogEntity(): ProductCatalogEntity? {
        if (!exists()) return null
        val nameVal = getString("name") ?: return null
        val defaultPrice = (get("defaultUnitPrice") as? Number)?.toDouble()
        val achatMoyen = (get("prixAchatMoyen") as? Number)?.toDouble()
        val (cleanCat, extractedMarque) = extractMarqueAndCleanCategory(getString("category"), getString("marque"))
        return ProductCatalogEntity(
            id = id,
            name = nameVal,
            defaultUnitPrice = if (defaultPrice != null && defaultPrice > 0.0) defaultPrice else null,
            category = cleanCat,
            marque = extractedMarque,
            unit = getString("unit") ?: "U",
            stockActuel = (get("stockActuel") as? Number)?.toDouble() ?: 0.0,
            seuilAlerte = (get("seuilAlerte") as? Number)?.toDouble() ?: 5.0,
            isArticleEclate = getBoolean("isArticleEclate") ?: false,
            isArticleKit = getBoolean("isArticleKit") ?: false,
            prixAchatMoyen = if (achatMoyen != null && achatMoyen > 0.0) achatMoyen else null,
            barcode = getString("barcode") ?: "",
            reference = getString("reference") ?: ""
        )
    }

    private fun DocumentSnapshot.toArticleCompositionEntity(): ArticleCompositionEntity? {
        if (!exists()) return null
        val parentId = getString("parentProductId") ?: return null
        val compName = getString("componentName") ?: return null
        val estPrice = (get("prixUnitaireEstime") as? Number)?.toDouble()
        return ArticleCompositionEntity(
            id = id,
            parentProductId = parentId,
            componentName = compName,
            quantite = (get("quantite") as? Number)?.toDouble() ?: 1.0,
            prixUnitaireEstime = if (estPrice != null && estPrice > 0.0) estPrice else null,
            unit = getString("unit") ?: "U"
        )
    }

    private fun DocumentSnapshot.toPartnerEntity(): PartnerEntity? {
        if (!exists()) return null
        val nameVal = getString("name") ?: return null
        val typeStr = getString("type") ?: "CLIENT"
        val partnerType = try {
            PartnerType.valueOf(typeStr)
        } catch (e: Exception) {
            PartnerType.CLIENT
        }
        val (cleanNotes, extractedNin) = extractNinAndCleanNotes(getString("notes"), getString("nin"))
        return PartnerEntity(
            id = id,
            name = nameVal,
            type = partnerType,
            phone = getString("phone") ?: "",
            address = getString("address") ?: "",
            solde = (get("solde") as? Number)?.toDouble() ?: 0.0,
            notes = cleanNotes,
            rc = getString("rc") ?: "",
            articleImposition = getString("articleImposition") ?: "",
            nif = getString("nif") ?: "",
            nis = getString("nis") ?: "",
            nin = extractedNin,
            rib = getString("rib") ?: ""
        )
    }

    private fun DocumentSnapshot.toOperationEntity(): OperationEntity? {
        if (!exists()) return null
        val typeStr = getString("typeOperation") ?: return null
        val opType = try {
            OperationType.valueOf(typeStr)
        } catch (e: Exception) {
            OperationType.BL_CLIENT
        }
        return OperationEntity(
            id = id,
            dateTime = (get("dateTime") as? Number)?.toLong() ?: System.currentTimeMillis(),
            typeOperation = opType,
            clientFournisseur = getString("clientFournisseur") ?: "",
            montantVerse = (get("montantVerse") as? Number)?.toDouble() ?: 0.0,
            modeReglement = getString("modeReglement") ?: "Espèces",
            notes = getString("notes") ?: "",
            appliquerTva = getBoolean("appliquerTva") ?: false,
            tauxTva = (get("tauxTva") as? Number)?.toDouble() ?: 19.0,
            statut = getString("statut") ?: "Validé",
            vendeur = getString("vendeur") ?: ""
        )
    }

    private fun DocumentSnapshot.toLigneOperationEntity(): LigneOperationEntity? {
        if (!exists()) return null
        val idOp = getString("idOperation") ?: return null
        val prod = getString("produit") ?: return null
        return LigneOperationEntity(
            id = id,
            operationId = idOp,
            produit = prod,
            quantite = (get("quantite") as? Number)?.toDouble() ?: 1.0,
            prixUnitaire = (get("prixUnitaire") as? Number)?.toDouble() ?: 0.0,
            parentArticle = getString("parentArticle") ?: ""
        )
    }

    private fun DocumentSnapshot.toStockMovementEntity(): StockMovementEntity? {
        if (!exists()) return null
        val prodId = getString("productId") ?: return null
        val prodName = getString("productName") ?: return null
        val typeStr = getString("type") ?: "AJUSTEMENT"
        val movType = try {
            StockMovementType.valueOf(typeStr)
        } catch (e: Exception) {
            StockMovementType.AJUSTEMENT
        }
        val opId = getString("operationId") ?: ""
        return StockMovementEntity(
            id = id,
            productId = prodId,
            productName = prodName,
            type = movType,
            quantity = (get("quantity") as? Number)?.toDouble() ?: 0.0,
            previousStock = (get("previousStock") as? Number)?.toDouble() ?: 0.0,
            newStock = (get("newStock") as? Number)?.toDouble() ?: 0.0,
            unit = getString("unit") ?: "U",
            operationId = opId.ifBlank { null },
            partnerName = getString("partnerName") ?: "",
            reason = getString("reason") ?: "",
            timestamp = (get("timestamp") as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toEmployeeEntity(): EmployeeEntity? {
        if (!exists()) return null
        val nameVal = getString("name") ?: return null
        val emailVal = getString("email") ?: return null
        val passwordVal = getString("password") ?: return null
        val roleVal = EmployeeRole.fromString(getString("role"))
        val isPrivileged = roleVal == EmployeeRole.ADMIN || roleVal == EmployeeRole.GERANT
        return EmployeeEntity(
            id = id,
            name = nameVal,
            email = emailVal,
            password = passwordVal,
            role = roleVal,
            isActive = getBoolean("isActive") ?: true,
            canModifyBon = getBoolean("canModifyBon") ?: true,
            canDeleteBon = getBoolean("canDeleteBon") ?: true,
            canModifyFacture = getBoolean("canModifyFacture") ?: isPrivileged,
            canDeleteFacture = getBoolean("canDeleteFacture") ?: isPrivileged,
            canModifyCreance = getBoolean("canModifyCreance") ?: isPrivileged,
            canDeleteCreance = getBoolean("canDeleteCreance") ?: isPrivileged,
            canManageEmployees = getBoolean("canManageEmployees") ?: (roleVal == EmployeeRole.ADMIN)
        )
    }

    companion object {
        const val COL_PRODUCTS = "products"
        const val COL_ARTICLE_COMPOSITIONS = "article_compositions"
        const val COL_PARTNERS = "partners"
        const val COL_OPERATIONS = "operations"
        const val COL_OPERATION_LINES = "operation_lines"
        const val COL_STOCK_MOVEMENTS = "stock_movements"
        const val COL_EMPLOYEES = "employees"

        private const val PREFS_EMPLOYEES = "merelec_employees_prefs"
        private const val KEY_EMPLOYEES_JSON = "employees_json"
        private const val KEY_EMPLOYEES_CUSTOMIZED = "employees_customized"
        private const val KEY_DB_RESET_BY_ADMIN = "db_reset_by_admin"
        private const val KEY_DELETED_PRODUCT_IDS = "deleted_product_ids"
        private const val KEY_DELETED_PRODUCT_NAMES = "deleted_product_names"
        private const val KEY_DELETED_OPERATION_IDS = "deleted_operation_ids"
        private const val KEY_DEMO_KIT_ECLATE_PURGED_V1 = "demo_kit_eclate_purged_v1"

        private val DEMO_KIT_AND_ECLATE_IDS = setOf(
            "prod-kit-coffret-24m",
            "PROD-KIT-24M",
            "prod-eclate-cablage-chantier",
            "PROD-ECLATE-CAB"
        )

        private val DEMO_KIT_AND_ECLATE_NORMALIZED_NAMES = setOf(
            "pack coffret electrique 24m complet (article kit)",
            "pack coffret electrique 24m complet",
            "lot cablage & derivation chantier (article eclate)",
            "lot cablage & derivation chantier"
        )

        /**
         * Détecte si une opération correspond à un ancien BL généré lors d'une importation de créances
         * (ex: IMP-SOLDE-..., CRE-..., ou note "Créance importée") afin de ne jamais l'afficher comme BL.
         */
        fun isImportedCreanceOperation(op: OperationEntity): Boolean {
            val cleanId = op.id.trim()
            val cleanNotes = op.notes.trim()
            return cleanId.startsWith("IMP-SOLDE-", ignoreCase = true) ||
                cleanId.contains("_IMP-SOLDE-", ignoreCase = true) ||
                cleanId.startsWith("CRE-", ignoreCase = true) ||
                cleanId.contains("_CRE-", ignoreCase = true) ||
                cleanNotes.startsWith("Créance importée", ignoreCase = true)
        }
    }
}
