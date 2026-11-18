package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.LigneOperationEntity
import com.example.data.model.OperationEntity
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import com.example.data.repository.OperationRepository
import android.content.Context
import android.net.Uri
import com.example.util.BackupContent
import com.example.util.BackupUtils
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.ImportUtils
import com.example.util.OcrExtractedLine
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.example.data.model.ArticleCompositionEntity
import com.example.data.model.CompanySettings
import com.example.data.model.DocumentPermissionCategory
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeRole
import com.example.data.model.ProductCatalogWithComposition
import com.example.data.model.StockMovementEntity
import com.example.data.model.StockMovementType
import com.example.data.model.permissionCategory
import com.example.data.repository.CompanySettingsManager

enum class BottomNavTab(val label: String) {
    DASHBOARD("Dashboard"),
    VENTES("Vente"),
    ACHATS("Achat"),
    CLIENTS("Client"),
    FOURNISSEURS("Fournisseur"),
    CREANCES("Créances"),
    ARTICLES("Articles"),
    STOCK("Gestion des Stocks"),
    CONFIGURATION("Configuration")
}

data class PartnerBalanceItem(
    val partner: com.example.data.model.PartnerEntity,
    val totalTransactions: Double = 0.0,
    val totalReglements: Double = 0.0,
    val soldeRestant: Double = 0.0,
    val countOperations: Int = 0,
    val lastOperationDate: Long = 0L
)

data class CreancesSummary(
    val totalCreancesClients: Double = 0.0,
    val totalDettesFournisseurs: Double = 0.0,
    val balanceNette: Double = 0.0,
    val countDebiteursClients: Int = 0,
    val countCrediteursFournisseurs: Int = 0
) {
    val soldeNetCommercial: Double
        get() = balanceNette
}

data class SalesKpi(
    val totalVentesTtc: Double = 0.0,
    val totalEncaisse: Double = 0.0,
    val creancesRestantes: Double = 0.0,
    val countVentes: Int = 0,
    val countReglements: Int = 0
)

data class PurchasesKpi(
    val totalAchatsTtc: Double = 0.0,
    val totalPaye: Double = 0.0,
    val dettesRestantes: Double = 0.0,
    val countAchats: Int = 0,
    val countReglements: Int = 0
)

sealed interface AppScreen {
    data object MainList : AppScreen
    data class Form(val operationId: String? = null) : AppScreen
    data class Detail(val operationId: String) : AppScreen
    data object ImportDirectory : AppScreen
    data object BackupDrive : AppScreen
    data class Management(val initialTab: Int = 0) : AppScreen // 0: Clients, 1: Fournisseurs, 2: Articles, 3: Stock
    data class PartnerDetail(val partnerId: String) : AppScreen // Fiche détaillée avec historique opérations
    data object Dashboard : AppScreen
    data object EmployeeManagement : AppScreen
    data class PdfStudio(val initialTab: Int = 0) : AppScreen // 0: Devis, 1: Bon, 2: Facture, 3: Créance, 4: Catalogue
    data class IntelligentOcr(val initialTargetType: OperationType = OperationType.BL_CLIENT) : AppScreen
}

data class PartnerFinancialSummary(
    val partner: PartnerEntity,
    val totalBl: Double = 0.0,
    val totalReglements: Double = 0.0,
    val soldeRestantDu: Double = 0.0,
    val countBl: Int = 0,
    val countReglements: Int = 0,
    val operations: List<OperationWithLignes> = emptyList()
)

data class FinancialKpi(
    val totalBlClient: Double = 0.0,
    val totalBlFournisseur: Double = 0.0,
    val totalReglements: Double = 0.0,
    val soldeNet: Double = 0.0,
    val countBlClient: Int = 0,
    val countBlFournisseur: Int = 0,
    val countReglements: Int = 0
)

data class FormDraftState(
    val id: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val typeOperation: OperationType = OperationType.BL_CLIENT,
    val clientFournisseur: String = "",
    val montantVerseText: String = "",
    val modeReglement: String = "Espèces",
    val notes: String = "",
    val lignes: List<LigneOperationEntity> = emptyList(),
    val isEditing: Boolean = false,
    val clientError: String? = null,
    val appliquerTva: Boolean = false,
    val tauxTva: Double = 19.0,
    val statut: String = "Validé",
    val convertedFromSupplierDocId: String = "",
    val marginCoefficientText: String = "1.25",
    val useCatalogSalePrice: Boolean = false,
    val sourcePurchasePricesByLineId: Map<String, Double> = emptyMap()
) {
    val showProductLines: Boolean
        get() = typeOperation != OperationType.REGLEMENT

    val calculatedTotalProduits: Double
        get() = if (showProductLines) {
            lignes.sumOf { it.totalLigne }
        } else {
            0.0
        }

    val montantTva: Double
        get() = if (appliquerTva && showProductLines) {
            calculatedTotalProduits * (tauxTva / 100.0)
        } else {
            0.0
        }

    val calculatedTotalTtc: Double
        get() = if (typeOperation == OperationType.REGLEMENT) {
            montantVerseNumeric
        } else {
            calculatedTotalProduits + montantTva
        }

    val montantVerseNumeric: Double
        get() = CurrencyUtils.parseAmount(montantVerseText)

    val resteAPayer: Double
        get() = if (typeOperation == OperationType.REGLEMENT) {
            0.0
        } else {
            (calculatedTotalTtc - montantVerseNumeric).coerceAtLeast(0.0)
        }
}

data class LineEditorState(
    val isVisible: Boolean = false,
    val lineId: String = "",
    val produit: String = "",
    val quantiteText: String = "1",
    val prixUnitaireText: String = "",
    val autoReopen: Boolean = true,
    val produitError: String? = null
)

class OperationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OperationRepository = OperationRepository(application)

    val allEmployees: StateFlow<List<EmployeeEntity>> =
        repository.allEmployees
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _authenticatedEmployee = MutableStateFlow<EmployeeEntity?>(null)
    val authenticatedEmployee: StateFlow<EmployeeEntity?> = _authenticatedEmployee

    val currentEmployee: StateFlow<EmployeeEntity> =
        combine(_authenticatedEmployee, allEmployees) { active, list ->
            if (active != null) {
                list.find {
                    it.id == active.id || it.email.equals(active.email, ignoreCase = true)
                } ?: active
            } else {
                EmployeeEntity.defaultAdmin()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EmployeeEntity.defaultAdmin()
        )

    val currentSellerLabel: String
        get() = _authenticatedEmployee.value?.name?.takeIf { it.isNotBlank() }
            ?: repository.currentSellerLabel()

    private val _isSyncingFirebase = MutableStateFlow(false)
    val isSyncingFirebase: StateFlow<Boolean> = _isSyncingFirebase

    private val _lastFirebaseSyncTime = MutableStateFlow<String?>(null)
    val lastFirebaseSyncTime: StateFlow<String?> = _lastFirebaseSyncTime

    val isFirebaseConnected: StateFlow<Boolean> =
        combine(
            repository.authUidFlow,
            _authenticatedEmployee,
            _lastFirebaseSyncTime
        ) { uid, emp, lastSync ->
            !uid.isNullOrBlank() || emp != null || !lastSync.isNullOrBlank()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun syncWithFirebaseNow() {
        if (_isSyncingFirebase.value) return
        viewModelScope.launch {
            _isSyncingFirebase.value = true
            try {
                val res = repository.syncAllWithFirebaseBidirectional()
                res.onSuccess { msg ->
                    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.FRENCH).format(Date())
                    _lastFirebaseSyncTime.value = timeFmt
                    _snackbarMessage.value = msg
                }.onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Erreur lors de la synchronisation Firebase."
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Erreur de synchronisation Firebase : ${e.localizedMessage ?: "Échec"}"
            } finally {
                _isSyncingFirebase.value = false
            }
        }
    }

    init {
        viewModelScope.launch {
            try {
                repository.seedInitialSampleDataIfEmpty()
                if (!repository.authUidFlow.value.isNullOrBlank()) {
                    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.FRENCH).format(Date())
                    _lastFirebaseSyncTime.value = timeFmt
                }
            } catch (_: Exception) {
            }
        }
    }

    private val companySettingsManager = CompanySettingsManager(application)
    val companySettings: StateFlow<CompanySettings> = companySettingsManager.settings

    fun updateCompanySettings(newSettings: CompanySettings) {
        companySettingsManager.updateSettings(newSettings)
        _snackbarMessage.value = "Paramètres de l'entreprise enregistrés !"
    }

    // Navigation state
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.MainList)
    val currentScreen: StateFlow<AppScreen> = _currentScreen

    // Bottom Navigation State (Vente, Achat, Articles, Configuration)
    private val _currentBottomTab = MutableStateFlow(BottomNavTab.VENTES)
    val currentBottomTab: StateFlow<BottomNavTab> = _currentBottomTab

    fun selectBottomTab(tab: BottomNavTab) {
        _currentBottomTab.value = tab
        selectedFilterType.value = null
        if (_currentScreen.value != AppScreen.MainList) {
            _currentScreen.value = AppScreen.MainList
        }
    }

    // Search and filter state
    val searchQuery = MutableStateFlow("")
    val selectedFilterType = MutableStateFlow<OperationType?>(null)

    // Flow of all operations from Firestore (Real-time snapshots)
    val allOperations: StateFlow<List<OperationWithLignes>> =
        repository.allOperationsWithLignes
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val recentPartners: StateFlow<List<String>> =
        repository.recentPartners
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val recentProducts: StateFlow<List<String>> =
        repository.recentProducts
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // --- Imported Directory (Clients, Fournisseurs, Produits) ---

    val clientsList: StateFlow<List<PartnerEntity>> =
        repository.clients
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val suppliersList: StateFlow<List<PartnerEntity>> =
        repository.fournisseurs
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val catalogProductsList: StateFlow<List<ProductCatalogEntity>> =
        repository.allCatalogProducts
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val lowStockProducts: StateFlow<List<ProductCatalogEntity>> =
        repository.lowStockProducts
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val allStockMovements: StateFlow<List<StockMovementEntity>> =
        repository.allStockMovements
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val bonsDeLivraisonClients: StateFlow<List<OperationWithLignes>> =
        repository.bonsDeLivraisonClients
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val bonsDeLivraisonFournisseurs: StateFlow<List<OperationWithLignes>> =
        repository.bonsDeLivraisonFournisseurs
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val articlesEclatesWithComposition: StateFlow<List<ProductCatalogWithComposition>> =
        repository.articlesEclatesWithComposition
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Filtered operations
    val filteredOperations: StateFlow<List<OperationWithLignes>> =
        combine(allOperations, searchQuery, selectedFilterType) { ops, query, filter ->
            ops.filter { item ->
                val matchesFilter = (filter == null || item.operation.typeOperation == filter)
                val cleanQuery = query.trim().lowercase(Locale.FRENCH)
                val matchesQuery = cleanQuery.isEmpty() ||
                        item.operation.clientFournisseur.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                        item.operation.id.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                        item.operation.notes.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                        item.lignes.any { it.produit.lowercase(Locale.FRENCH).contains(cleanQuery) }
                matchesFilter && matchesQuery
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Sales specific operations
    val salesOperations: StateFlow<List<OperationWithLignes>> =
        combine(allOperations, searchQuery, selectedFilterType, suppliersList) { ops, query, filter, sups ->
            val supNames = sups.map { it.name.trim().lowercase(Locale.FRENCH) }.toSet()
            ops.filter { item ->
                val op = item.operation
                val isSales = op.typeOperation.isSalesDocument ||
                        (op.typeOperation == OperationType.REGLEMENT && !supNames.contains(op.clientFournisseur.trim().lowercase(Locale.FRENCH)))
                if (!isSales) return@filter false

                val matchesFilter = (filter == null || op.typeOperation == filter)
                val cleanQuery = query.trim().lowercase(Locale.FRENCH)
                val matchesQuery = cleanQuery.isEmpty() ||
                        op.clientFournisseur.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                        op.id.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                        op.notes.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                        item.lignes.any { it.produit.lowercase(Locale.FRENCH).contains(cleanQuery) }
                matchesFilter && matchesQuery
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Purchases specific operations
    val purchaseOperations: StateFlow<List<OperationWithLignes>> =
        combine(allOperations, searchQuery, selectedFilterType, suppliersList) { ops, query, filter, sups ->
            val supNames = sups.map { it.name.trim().lowercase(Locale.FRENCH) }.toSet()
            ops.filter { item ->
                val op = item.operation
                val isPurchase = op.typeOperation.isPurchaseDocument ||
                        (op.typeOperation == OperationType.REGLEMENT && supNames.contains(op.clientFournisseur.trim().lowercase(Locale.FRENCH)))
                if (!isPurchase) return@filter false

                val matchesFilter = (filter == null || op.typeOperation == filter)
                val cleanQuery = query.trim().lowercase(Locale.FRENCH)
                val matchesQuery = cleanQuery.isEmpty() ||
                        op.clientFournisseur.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                        op.id.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                        op.notes.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                        item.lignes.any { it.produit.lowercase(Locale.FRENCH).contains(cleanQuery) }
                matchesFilter && matchesQuery
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val salesKpi: StateFlow<SalesKpi> =
        combine(allOperations, suppliersList) { ops, sups ->
            val supNames = sups.map { it.name.trim().lowercase(Locale.FRENCH) }.toSet()
            var totalTtc = 0.0
            var totalEncaisse = 0.0
            var countVentes = 0
            var countRegl = 0

            for (item in ops) {
                val op = item.operation
                when (op.typeOperation) {
                    OperationType.BL_CLIENT, OperationType.FACTURE, OperationType.BON_COMMANDE -> {
                        totalTtc += item.totalTtc
                        totalEncaisse += op.montantVerse
                        countVentes++
                    }
                    OperationType.AVOIR_CLIENT -> {
                        totalTtc -= item.totalTtc
                    }
                    OperationType.REGLEMENT -> {
                        if (!supNames.contains(op.clientFournisseur.trim().lowercase(Locale.FRENCH))) {
                            totalEncaisse += op.montantVerse
                            countRegl++
                        }
                    }
                    else -> {}
                }
            }
            val creances = maxOf(0.0, totalTtc - totalEncaisse)
            SalesKpi(
                totalVentesTtc = totalTtc,
                totalEncaisse = totalEncaisse,
                creancesRestantes = creances,
                countVentes = countVentes,
                countReglements = countRegl
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SalesKpi()
        )

    val purchasesKpi: StateFlow<PurchasesKpi> =
        combine(allOperations, suppliersList) { ops, sups ->
            val supNames = sups.map { it.name.trim().lowercase(Locale.FRENCH) }.toSet()
            var totalTtc = 0.0
            var totalPaye = 0.0
            var countAchats = 0
            var countRegl = 0

            for (item in ops) {
                val op = item.operation
                when (op.typeOperation) {
                    OperationType.BL_FOURNISSEUR, OperationType.FACTURE_FOURNISSEUR -> {
                        totalTtc += item.totalTtc
                        totalPaye += op.montantVerse
                        countAchats++
                    }
                    OperationType.AVOIR_FOURNISSEUR -> {
                        totalTtc -= item.totalTtc
                    }
                    OperationType.REGLEMENT -> {
                        if (supNames.contains(op.clientFournisseur.trim().lowercase(Locale.FRENCH))) {
                            totalPaye += op.montantVerse
                            countRegl++
                        }
                    }
                    else -> {}
                }
            }
            val dettes = maxOf(0.0, totalTtc - totalPaye)
            PurchasesKpi(
                totalAchatsTtc = totalTtc,
                totalPaye = totalPaye,
                dettesRestantes = dettes,
                countAchats = countAchats,
                countReglements = countRegl
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PurchasesKpi()
        )

    // Balances clients pour l'onglet Créances
    val clientsBalances: StateFlow<List<PartnerBalanceItem>> =
        combine(clientsList, allOperations) { clients, ops ->
            clients.map { client ->
                val clientNameLower = client.name.trim().lowercase(Locale.FRENCH)
                val matchedOps = ops.filter {
                    it.operation.clientFournisseur.trim().lowercase(Locale.FRENCH) == clientNameLower &&
                        !OperationRepository.isImportedCreanceOperation(it.operation)
                }
                var totalFacture = 0.0
                var totalRegle = 0.0
                var latestDate = 0L

                for (opWithLignes in matchedOps) {
                    if (opWithLignes.operation.dateTime > latestDate) {
                        latestDate = opWithLignes.operation.dateTime
                    }
                    when (opWithLignes.operation.typeOperation) {
                        OperationType.BL_CLIENT, OperationType.FACTURE, OperationType.BON_COMMANDE -> {
                            totalFacture += opWithLignes.totalTtc
                            if (opWithLignes.operation.montantVerse > 0) {
                                totalRegle += opWithLignes.operation.montantVerse
                            }
                        }
                        OperationType.AVOIR_CLIENT -> {
                            totalFacture -= opWithLignes.totalTtc
                        }
                        OperationType.REGLEMENT -> {
                            totalRegle += opWithLignes.operation.montantVerse
                        }
                        else -> {}
                    }
                }
                val opsNetRestant = (totalFacture - totalRegle).coerceAtLeast(0.0)
                val baseCreance = if (totalFacture > 0.0 && kotlin.math.abs(client.solde - opsNetRestant) < 0.01) {
                    0.0
                } else {
                    client.solde.coerceAtLeast(0.0)
                }
                val solde = (baseCreance + totalFacture - totalRegle).coerceAtLeast(0.0)
                val effectiveTotal = totalFacture.coerceAtLeast(0.0)
                PartnerBalanceItem(
                    partner = client,
                    totalTransactions = effectiveTotal,
                    totalReglements = totalRegle,
                    soldeRestant = solde,
                    countOperations = matchedOps.size,
                    lastOperationDate = latestDate
                )
            }.sortedByDescending { it.soldeRestant }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dettes fournisseurs pour l'onglet Créances
    val suppliersBalances: StateFlow<List<PartnerBalanceItem>> =
        combine(suppliersList, allOperations) { suppliers, ops ->
            suppliers.map { supplier ->
                val supplierNameLower = supplier.name.trim().lowercase(Locale.FRENCH)
                val matchedOps = ops.filter {
                    it.operation.clientFournisseur.trim().lowercase(Locale.FRENCH) == supplierNameLower &&
                        !OperationRepository.isImportedCreanceOperation(it.operation)
                }
                var totalAchat = 0.0
                var totalPaye = 0.0
                var latestDate = 0L

                for (opWithLignes in matchedOps) {
                    if (opWithLignes.operation.dateTime > latestDate) {
                        latestDate = opWithLignes.operation.dateTime
                    }
                    when (opWithLignes.operation.typeOperation) {
                        OperationType.BL_FOURNISSEUR, OperationType.FACTURE_FOURNISSEUR -> {
                            totalAchat += opWithLignes.totalTtc
                            if (opWithLignes.operation.montantVerse > 0) {
                                totalPaye += opWithLignes.operation.montantVerse
                            }
                        }
                        OperationType.AVOIR_FOURNISSEUR -> {
                            totalAchat -= opWithLignes.totalTtc
                        }
                        OperationType.REGLEMENT -> {
                            totalPaye += opWithLignes.operation.montantVerse
                        }
                        else -> {}
                    }
                }
                val opsNetRestant = (totalAchat - totalPaye).coerceAtLeast(0.0)
                val baseDette = if (totalAchat > 0.0 && kotlin.math.abs(supplier.solde - opsNetRestant) < 0.01) {
                    0.0
                } else {
                    supplier.solde.coerceAtLeast(0.0)
                }
                val solde = (baseDette + totalAchat - totalPaye).coerceAtLeast(0.0)
                val effectiveTotal = totalAchat.coerceAtLeast(0.0)
                PartnerBalanceItem(
                    partner = supplier,
                    totalTransactions = effectiveTotal,
                    totalReglements = totalPaye,
                    soldeRestant = solde,
                    countOperations = matchedOps.size,
                    lastOperationDate = latestDate
                )
            }.sortedByDescending { it.soldeRestant }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Synthèse globale des créances et dettes
    val creancesSummary: StateFlow<CreancesSummary> =
        combine(clientsBalances, suppliersBalances) { cBalances, sBalances ->
            val totalCreances = cBalances.sumOf { it.soldeRestant }
            val totalDettes = sBalances.sumOf { it.soldeRestant }
            CreancesSummary(
                totalCreancesClients = totalCreances,
                totalDettesFournisseurs = totalDettes,
                balanceNette = totalCreances - totalDettes,
                countDebiteursClients = cBalances.count { it.soldeRestant > 0.0 },
                countCrediteursFournisseurs = sBalances.count { it.soldeRestant > 0.0 }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CreancesSummary()
        )

    // Financial KPIs (Global)
    val financialKpi: StateFlow<FinancialKpi> =
        allOperations.combine(searchQuery) { ops, _ ->
            var blClientSum = 0.0
            var blFournSum = 0.0
            var reglSum = 0.0
            var blcCount = 0
            var blfCount = 0
            var regCount = 0

            for (item in ops) {
                when (item.operation.typeOperation) {
                    OperationType.BL_CLIENT, OperationType.FACTURE, OperationType.BON_COMMANDE -> {
                        blClientSum += item.totalTtc
                        blcCount++
                    }
                    OperationType.AVOIR_CLIENT -> {
                        blClientSum -= item.totalTtc
                    }
                    OperationType.BL_FOURNISSEUR, OperationType.FACTURE_FOURNISSEUR -> {
                        blFournSum += item.totalTtc
                        blfCount++
                    }
                    OperationType.AVOIR_FOURNISSEUR -> {
                        blFournSum -= item.totalTtc
                    }
                    OperationType.REGLEMENT -> {
                        reglSum += item.operation.montantVerse
                        regCount++
                    }
                    OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> {
                        // Devis pro forma
                    }
                }
            }

            FinancialKpi(
                totalBlClient = blClientSum,
                totalBlFournisseur = blFournSum,
                totalReglements = reglSum,
                soldeNet = blClientSum - blFournSum,
                countBlClient = blcCount,
                countBlFournisseur = blfCount,
                countReglements = regCount
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FinancialKpi()
        )

    // Form Draft State
    private val _formState = MutableStateFlow(FormDraftState())
    val formState: StateFlow<FormDraftState> = _formState

    // Line Editor Modal State
    private val _lineEditor = MutableStateFlow(LineEditorState())
    val lineEditor: StateFlow<LineEditorState> = _lineEditor

    // Notification message (Snackbar)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun navigateBack() {
        _currentScreen.value = AppScreen.MainList
    }

    // Prepare new form
    fun startNewOperation(initialType: OperationType = OperationType.BL_CLIENT) {
        val dateCode = SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())
        val settings = companySettings.value
        val prefix = when (initialType) {
            OperationType.FACTURE -> settings.prefixFacture
            OperationType.FACTURE_FOURNISSEUR -> settings.prefixFactureFournisseur
            OperationType.BON_COMMANDE -> settings.prefixBonCommande
            OperationType.BL_CLIENT -> settings.prefixBonLivraison
            OperationType.BL_FOURNISSEUR -> settings.prefixBonFournisseur
            OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> settings.prefixDevis
            OperationType.AVOIR_CLIENT, OperationType.AVOIR_FOURNISSEUR -> settings.prefixAvoir
            OperationType.REGLEMENT -> settings.prefixReglement
        }
        val generatedId = "$prefix$dateCode"
        val applyTva = if (initialType == OperationType.FACTURE) settings.tvaEnabledByDefault else false
        val tauxTva = settings.defaultTvaRate

        _formState.value = FormDraftState(
            id = generatedId,
            dateTime = System.currentTimeMillis(),
            typeOperation = initialType,
            clientFournisseur = "",
            montantVerseText = "",
            modeReglement = "Espèces",
            notes = "",
            lignes = emptyList(),
            isEditing = false,
            appliquerTva = applyTva,
            tauxTva = tauxTva
        )
        _currentScreen.value = AppScreen.Form(null)
    }

    // Prepare edit form (avec vérification des droits de modification Bon / Facture / Créance)
    fun editOperation(opWithLignes: OperationWithLignes) {
        val op = opWithLignes.operation
        val emp = currentEmployee.value
        if (!emp.canModifyOperation(op.typeOperation)) {
            _snackbarMessage.value = emp.permissionDeniedMessage(
                action = "modifier",
                category = op.typeOperation.permissionCategory()
            )
            return
        }
        _formState.value = FormDraftState(
            id = op.id,
            dateTime = op.dateTime,
            typeOperation = op.typeOperation,
            clientFournisseur = op.clientFournisseur,
            montantVerseText = if (op.montantVerse > 0) CurrencyUtils.formatQty(op.montantVerse) else "",
            modeReglement = op.modeReglement,
            notes = op.notes,
            lignes = opWithLignes.lignes,
            isEditing = true,
            appliquerTva = op.appliquerTva,
            tauxTva = op.tauxTva,
            statut = op.statut
        )
        _currentScreen.value = AppScreen.Form(op.id)
    }

    fun generateNextFactureId(): String {
        val dateCode = SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())
        val prefix = companySettings.value.prefixFacture.ifBlank { "FAC-" }
        val baseId = "$prefix$dateCode"
        val existingIds = allOperations.value.map { it.operation.id }.toSet()
        if (!existingIds.contains(baseId)) return baseId
        var counter = 1
        while (existingIds.contains("$baseId-$counter")) {
            counter++
        }
        return "$baseId-$counter"
    }

    private fun buildConversionNotes(existingNotes: String, sourceLabel: String, sourceId: String): String {
        val isFacture = sourceLabel.contains("Facture", ignoreCase = true) || sourceLabel.contains("FAC", ignoreCase = true)
        val refLine = if (isFacture) {
            "Document converti à partir de la facture N° $sourceId"
        } else {
            "Document converti à partir de $sourceLabel N° $sourceId"
        }
        return if (existingNotes.isBlank()) {
            refLine
        } else if (existingNotes.contains(sourceId)) {
            existingNotes
        } else {
            "$existingNotes\n$refLine"
        }
    }

    /**
     * Convertit directement un Bon de Livraison (ou Devis / BC) en Facture Client.
     * Si replaceOriginal = true, l'ancien BL est remplacé pour éviter le double comptage du solde client et du stock.
     */
    fun convertToFacture(
        source: OperationWithLignes,
        customFactureId: String = "",
        appliquerTva: Boolean = companySettings.value.tvaEnabledByDefault,
        tauxTva: Double = companySettings.value.defaultTvaRate,
        replaceOriginal: Boolean = true,
        onCreated: (String) -> Unit = {}
    ) {
        val emp = currentEmployee.value
        if (!emp.canModifyFacture && !emp.canModifyBon) {
            _snackbarMessage.value = emp.permissionDeniedMessage("convertir / modifier", DocumentPermissionCategory.FACTURE)
            return
        }
        val newId = customFactureId.trim().ifEmpty { generateNextFactureId() }
        val op = source.operation
        val updatedNotes = buildConversionNotes(op.notes, op.typeOperation.shortCode, op.id)

        val newOperation = OperationEntity(
            id = newId,
            dateTime = System.currentTimeMillis(),
            typeOperation = OperationType.FACTURE,
            clientFournisseur = op.clientFournisseur,
            montantVerse = op.montantVerse,
            modeReglement = op.modeReglement,
            notes = updatedNotes,
            appliquerTva = appliquerTva,
            tauxTva = tauxTva,
            statut = "Validé"
        )

        val newLignes = source.lignes.map { ligne ->
            ligne.copy(
                id = UUID.randomUUID().toString(),
                operationId = newId
            )
        }

        viewModelScope.launch {
            repository.saveOperationWithLignes(newOperation, newLignes)
            if (replaceOriginal && op.id != newId) {
                repository.deleteOperation(op.id)
            }
            _snackbarMessage.value = "${op.typeOperation.label} (${op.id}) converti en Facture ($newId) !"
            _currentScreen.value = AppScreen.Detail(newId)
            onCreated(newId)
        }
    }

    /**
     * Ouvre le formulaire d'édition pré-rempli en tant que Facture à partir d'un Bon de Livraison.
     */
    fun openConvertToFactureInEditor(
        source: OperationWithLignes,
        customFactureId: String = "",
        appliquerTva: Boolean = companySettings.value.tvaEnabledByDefault,
        tauxTva: Double = companySettings.value.defaultTvaRate
    ) {
        val newId = customFactureId.trim().ifEmpty { generateNextFactureId() }
        val op = source.operation
        val updatedNotes = buildConversionNotes(op.notes, op.typeOperation.shortCode, op.id)
        val clonedLignes = source.lignes.map { ligne ->
            ligne.copy(
                id = UUID.randomUUID().toString(),
                operationId = newId
            )
        }

        _formState.value = FormDraftState(
            id = newId,
            dateTime = System.currentTimeMillis(),
            typeOperation = OperationType.FACTURE,
            clientFournisseur = op.clientFournisseur,
            montantVerseText = if (op.montantVerse > 0) CurrencyUtils.formatQty(op.montantVerse) else "",
            modeReglement = op.modeReglement,
            notes = updatedNotes,
            lignes = clonedLignes,
            isEditing = true, // évite de décrémenter une 2e fois le stock déjà sorti lors du BL
            appliquerTva = appliquerTva,
            tauxTva = tauxTva,
            statut = "Validé"
        )
        _currentScreen.value = AppScreen.Form(null)
    }

    /**
     * Regroupe et convertit un ou plusieurs Bons de Livraison d'un même client en une seule Facture Client.
     */
    fun convertMultipleBlsToFacture(
        sources: List<OperationWithLignes>,
        customFactureId: String = "",
        appliquerTva: Boolean = companySettings.value.tvaEnabledByDefault,
        tauxTva: Double = companySettings.value.defaultTvaRate,
        replaceOriginals: Boolean = true,
        onCreated: (String) -> Unit = {}
    ) {
        if (sources.isEmpty()) return
        if (sources.size == 1) {
            convertToFacture(
                source = sources.first(),
                customFactureId = customFactureId,
                appliquerTva = appliquerTva,
                tauxTva = tauxTva,
                replaceOriginal = replaceOriginals,
                onCreated = onCreated
            )
            return
        }

        val newId = customFactureId.trim().ifEmpty { generateNextFactureId() }
        val firstOp = sources.first().operation
        val blIds = sources.joinToString(", ") { it.operation.id }
        val combinedExistingNotes = sources.mapNotNull { it.operation.notes.takeIf { n -> n.isNotBlank() } }.joinToString(" | ")
        val updatedNotes = if (combinedExistingNotes.isBlank()) {
            "Facture regroupant les BL : $blIds"
        } else {
            "$combinedExistingNotes\nFacture regroupant les BL : $blIds"
        }

        val totalMontantVerse = sources.sumOf { it.operation.montantVerse }

        val newOperation = OperationEntity(
            id = newId,
            dateTime = System.currentTimeMillis(),
            typeOperation = OperationType.FACTURE,
            clientFournisseur = firstOp.clientFournisseur,
            montantVerse = totalMontantVerse,
            modeReglement = firstOp.modeReglement,
            notes = updatedNotes,
            appliquerTva = appliquerTva,
            tauxTva = tauxTva,
            statut = "Validé"
        )

        val newLignes = sources.flatMap { src ->
            src.lignes.map { ligne ->
                ligne.copy(
                    id = UUID.randomUUID().toString(),
                    operationId = newId
                )
            }
        }

        viewModelScope.launch {
            repository.saveOperationWithLignes(newOperation, newLignes)
            if (replaceOriginals) {
                for (src in sources) {
                    if (src.operation.id != newId) {
                        repository.deleteOperation(src.operation.id)
                    }
                }
            }
            _snackbarMessage.value = "${sources.size} Bons de Livraison convertis en Facture ($newId) !"
            _currentScreen.value = AppScreen.Detail(newId)
            onCreated(newId)
        }
    }

    fun generateNextBlId(isSupplier: Boolean = false): String {
        val dateCode = SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())
        val prefix = if (isSupplier) "BLF-" else "BLC-"
        val baseId = "$prefix$dateCode"
        val existingIds = allOperations.value.map { it.operation.id }.toSet()
        if (!existingIds.contains(baseId)) return baseId
        var counter = 1
        while (existingIds.contains("$baseId-$counter")) {
            counter++
        }
        return "$baseId-$counter"
    }

    /**
     * Convertit directement un Devis (Client ou Fournisseur) ou Bon de Commande en Bon de Livraison (BL).
     * Comme le Devis n'avait pas impacté le stock, on met à jour le stock si deductStock = true.
     */
    fun convertDevisToBl(
        source: OperationWithLignes,
        customBlId: String = "",
        appliquerTva: Boolean = source.operation.appliquerTva,
        tauxTva: Double = source.operation.tauxTva,
        replaceOriginal: Boolean = true,
        deductStock: Boolean = true,
        onCreated: (String) -> Unit = {}
    ) {
        val emp = currentEmployee.value
        if (!emp.canModifyBon) {
            _snackbarMessage.value = emp.permissionDeniedMessage("convertir / créer un BL", DocumentPermissionCategory.BON)
            return
        }
        val op = source.operation
        val isSupplier = op.typeOperation == OperationType.DEVIS_FOURNISSEUR || op.typeOperation == OperationType.BL_FOURNISSEUR
        val targetType = if (isSupplier) OperationType.BL_FOURNISSEUR else OperationType.BL_CLIENT
        val newId = customBlId.trim().ifEmpty { generateNextBlId(isSupplier) }
        val updatedNotes = buildConversionNotes(op.notes, op.typeOperation.shortCode, op.id)

        val newOperation = OperationEntity(
            id = newId,
            dateTime = System.currentTimeMillis(),
            typeOperation = targetType,
            clientFournisseur = op.clientFournisseur,
            vendeur = op.vendeur,
            montantVerse = op.montantVerse,
            modeReglement = op.modeReglement,
            notes = updatedNotes,
            appliquerTva = appliquerTva,
            tauxTva = tauxTva,
            statut = "Validé"
        )

        val newLignes = source.lignes.map { ligne ->
            ligne.copy(
                id = UUID.randomUUID().toString(),
                operationId = newId
            )
        }

        viewModelScope.launch {
            repository.saveOperationWithLignes(newOperation, newLignes)

            if (deductStock) {
                val catalog = catalogProductsList.value
                for (line in newLignes) {
                    val matchedProd = catalog.find {
                        it.name.trim().equals(line.produit.trim(), ignoreCase = true)
                    }
                    if (matchedProd != null) {
                        val prevStock = matchedProd.stockActuel
                        val newStock = if (isSupplier) {
                            prevStock + line.quantite
                        } else {
                            (prevStock - line.quantite).coerceAtLeast(0.0)
                        }
                        val movType = if (isSupplier) StockMovementType.ENTREE else StockMovementType.SORTIE
                        repository.recordStockMovementAndApply(
                            StockMovementEntity(
                                productId = matchedProd.id,
                                productName = matchedProd.name,
                                type = movType,
                                quantity = line.quantite,
                                previousStock = prevStock,
                                newStock = newStock,
                                unit = matchedProd.unit,
                                operationId = newId,
                                partnerName = op.clientFournisseur,
                                reason = "Conversion ${op.typeOperation.shortCode} ${op.id} → ${targetType.shortCode} $newId"
                            )
                        )
                    }
                }
            }

            if (replaceOriginal && op.id != newId) {
                repository.deleteOperation(op.id)
            }
            _snackbarMessage.value = "${op.typeOperation.label} (${op.id}) converti en ${targetType.label} ($newId) !"
            _currentScreen.value = AppScreen.Detail(newId)
            onCreated(newId)
        }
    }

    /**
     * Ouvre le formulaire d'édition pré-rempli en tant que Bon de Livraison à partir d'un Devis.
     */
    fun openConvertDevisToBlInEditor(
        source: OperationWithLignes,
        customBlId: String = "",
        appliquerTva: Boolean = source.operation.appliquerTva,
        tauxTva: Double = source.operation.tauxTva
    ) {
        val op = source.operation
        val isSupplier = op.typeOperation == OperationType.DEVIS_FOURNISSEUR || op.typeOperation == OperationType.BL_FOURNISSEUR
        val targetType = if (isSupplier) OperationType.BL_FOURNISSEUR else OperationType.BL_CLIENT
        val newId = customBlId.trim().ifEmpty { generateNextBlId(isSupplier) }
        val updatedNotes = buildConversionNotes(op.notes, op.typeOperation.shortCode, op.id)
        val clonedLignes = source.lignes.map { ligne ->
            ligne.copy(
                id = UUID.randomUUID().toString(),
                operationId = newId
            )
        }

        _formState.value = FormDraftState(
            id = newId,
            dateTime = System.currentTimeMillis(),
            typeOperation = targetType,
            clientFournisseur = op.clientFournisseur,
            montantVerseText = if (op.montantVerse > 0) CurrencyUtils.formatQty(op.montantVerse) else "",
            modeReglement = op.modeReglement,
            notes = updatedNotes,
            lignes = clonedLignes,
            isEditing = false, // isEditing = false permet de mettre à jour le stock à la sauvegarde du BL
            appliquerTva = appliquerTva,
            tauxTva = tauxTva,
            statut = "Validé"
        )
        _currentScreen.value = AppScreen.Form(null)
    }

    /**
     * Regroupe et convertit un ou plusieurs Devis d'un même client/fournisseur en un seul Bon de Livraison (BL).
     */
    fun convertMultipleDevisToBl(
        sources: List<OperationWithLignes>,
        customBlId: String = "",
        appliquerTva: Boolean = false,
        tauxTva: Double = companySettings.value.defaultTvaRate,
        replaceOriginals: Boolean = true,
        deductStock: Boolean = true,
        onCreated: (String) -> Unit = {}
    ) {
        if (sources.isEmpty()) return
        if (sources.size == 1) {
            convertDevisToBl(
                source = sources.first(),
                customBlId = customBlId,
                appliquerTva = appliquerTva,
                tauxTva = tauxTva,
                replaceOriginal = replaceOriginals,
                deductStock = deductStock,
                onCreated = onCreated
            )
            return
        }

        val emp = currentEmployee.value
        if (!emp.canModifyBon) {
            _snackbarMessage.value = emp.permissionDeniedMessage("convertir / créer un BL", DocumentPermissionCategory.BON)
            return
        }

        val firstOp = sources.first().operation
        val isSupplier = firstOp.typeOperation == OperationType.DEVIS_FOURNISSEUR || firstOp.typeOperation == OperationType.BL_FOURNISSEUR
        val targetType = if (isSupplier) OperationType.BL_FOURNISSEUR else OperationType.BL_CLIENT
        val newId = customBlId.trim().ifEmpty { generateNextBlId(isSupplier) }
        val devisIds = sources.joinToString(", ") { it.operation.id }
        val combinedExistingNotes = sources.mapNotNull { it.operation.notes.takeIf { n -> n.isNotBlank() } }.joinToString(" | ")
        val updatedNotes = if (combinedExistingNotes.isBlank()) {
            "Bon de Livraison regroupant les Devis : $devisIds"
        } else {
            "$combinedExistingNotes\nBon de Livraison regroupant les Devis : $devisIds"
        }

        val totalMontantVerse = sources.sumOf { it.operation.montantVerse }

        val newOperation = OperationEntity(
            id = newId,
            dateTime = System.currentTimeMillis(),
            typeOperation = targetType,
            clientFournisseur = firstOp.clientFournisseur,
            vendeur = firstOp.vendeur,
            montantVerse = totalMontantVerse,
            modeReglement = firstOp.modeReglement,
            notes = updatedNotes,
            appliquerTva = appliquerTva,
            tauxTva = tauxTva,
            statut = "Validé"
        )

        val newLignes = sources.flatMap { src ->
            src.lignes.map { ligne ->
                ligne.copy(
                    id = UUID.randomUUID().toString(),
                    operationId = newId
                )
            }
        }

        viewModelScope.launch {
            repository.saveOperationWithLignes(newOperation, newLignes)
            if (deductStock) {
                val catalog = catalogProductsList.value
                for (line in newLignes) {
                    val matchedProd = catalog.find {
                        it.name.trim().equals(line.produit.trim(), ignoreCase = true)
                    }
                    if (matchedProd != null) {
                        val prevStock = matchedProd.stockActuel
                        val newStock = if (isSupplier) {
                            prevStock + line.quantite
                        } else {
                            (prevStock - line.quantite).coerceAtLeast(0.0)
                        }
                        val movType = if (isSupplier) StockMovementType.ENTREE else StockMovementType.SORTIE
                        repository.recordStockMovementAndApply(
                            StockMovementEntity(
                                productId = matchedProd.id,
                                productName = matchedProd.name,
                                type = movType,
                                quantity = line.quantite,
                                previousStock = prevStock,
                                newStock = newStock,
                                unit = matchedProd.unit,
                                operationId = newId,
                                partnerName = firstOp.clientFournisseur,
                                reason = "Conversion Devis ($devisIds) → ${targetType.shortCode} $newId"
                            )
                        )
                    }
                }
            }
            if (replaceOriginals) {
                for (src in sources) {
                    if (src.operation.id != newId) {
                        repository.deleteOperation(src.operation.id)
                    }
                }
            }
            _snackbarMessage.value = "${sources.size} Devis convertis en ${targetType.label} ($newId) !"
            _currentScreen.value = AppScreen.Detail(newId)
            onCreated(newId)
        }
    }

    /**
     * Recherche un article du catalogue correspondant au libellé d'une ligne (par désignation ou par référence).
     */
    private fun findCatalogProductForLineLabel(rawLabel: String): ProductCatalogEntity? {
        val clean = rawLabel.trim()
        if (clean.isEmpty()) return null
        val catalog = catalogProductsList.value
        // Correspondance exacte sur le nom
        catalog.find { it.name.trim().equals(clean, ignoreCase = true) }?.let { return it }
        // Si le libellé commence par "[REF] Nom" ou "REF - Nom"
        if (clean.startsWith("[") && clean.contains("]")) {
            val refPart = clean.substringAfter("[").substringBefore("]").trim()
            val namePart = clean.substringAfter("]").trim()
            if (refPart.isNotEmpty()) {
                catalog.find { it.reference.trim().equals(refPart, ignoreCase = true) }?.let { return it }
            }
            if (namePart.isNotEmpty()) {
                catalog.find { it.name.trim().equals(namePart, ignoreCase = true) }?.let { return it }
            }
        }
        val dashRef = clean.substringBefore(" - ", missingDelimiterValue = "").trim()
        val dashName = clean.substringAfter(" - ", missingDelimiterValue = "").trim()
        if (dashRef.isNotEmpty()) {
            catalog.find { it.reference.trim().equals(dashRef, ignoreCase = true) }?.let { return it }
        }
        if (dashName.isNotEmpty()) {
            catalog.find { it.name.trim().equals(dashName, ignoreCase = true) }?.let { return it }
        }
        return catalog.find {
            it.reference.isNotBlank() && clean.contains(it.reference.trim(), ignoreCase = true)
        }
    }

    /**
     * Formate la désignation produit en incluant la référence catalogue si elle existe et n'est pas déjà présente.
     */
    private fun formatLineProductWithRef(rawLabel: String, matchedCatalog: ProductCatalogEntity?): String {
        val clean = rawLabel.trim()
        val ref = matchedCatalog?.reference?.trim().orEmpty()
        if (ref.isEmpty()) return clean
        if (clean.contains(ref, ignoreCase = true)) return clean
        return "[$ref] $clean"
    }

    /**
     * Convertit une Facture Fournisseur (ou Bon Fournisseur) en Facture Client
     * et ouvre directement l'écran d'édition pour sélectionner le client et enregistrer.
     */
    fun convertSupplierDocToClientFactureInEditor(
        source: OperationWithLignes,
        marginCoefficient: Double = companySettings.value.defaultMarginCoefficient,
        useCatalogSalePrice: Boolean = companySettings.value.useCatalogSalePriceOnConvert
    ) {
        val emp = currentEmployee.value
        if (!emp.canModifyFacture) {
            _snackbarMessage.value = emp.permissionDeniedMessage("convertir en Facture Client", DocumentPermissionCategory.FACTURE)
            return
        }

        val safeMargin = if (marginCoefficient.isNaN() || marginCoefficient <= 0.0) 1.0 else marginCoefficient
        val currentSettings = companySettings.value
        if (currentSettings.defaultMarginCoefficient != safeMargin || currentSettings.useCatalogSalePriceOnConvert != useCatalogSalePrice) {
            updateCompanySettings(
                currentSettings.copy(
                    defaultMarginCoefficient = safeMargin,
                    useCatalogSalePriceOnConvert = useCatalogSalePrice
                )
            )
        }

        val newId = generateNextFactureId()
        val now = System.currentTimeMillis()
        val purchasePricesMap = mutableMapOf<String, Double>()

        val convertedLines = source.lignes
            .filter { srcLine ->
                val cleanLabel = srcLine.produit.replace(Regex("^\\[[^\\]]+]\\s*"), "").trim()
                cleanLabel.isNotBlank() &&
                    !com.example.util.InvoiceOcrHelper.isTableHeaderRow(cleanLabel) &&
                    !com.example.util.InvoiceOcrHelper.isTableFooterOrTotalRow(cleanLabel)
            }
            .map { srcLine ->
            val newLineId = UUID.randomUUID().toString()
            val matchedCatalog = findCatalogProductForLineLabel(srcLine.produit)
            val srcPu = srcLine.prixUnitaire ?: 0.0
            val catPa = matchedCatalog?.prixAchatMoyen ?: matchedCatalog?.prixAchat ?: 0.0
            val catPv = matchedCatalog?.defaultUnitPrice ?: matchedCatalog?.prixVente ?: 0.0

            val basePurchasePrice: Double = when {
                srcPu > 0.0 -> srcPu
                catPa > 0.0 -> catPa
                catPv > 0.0 -> catPv
                else -> 0.0
            }
            purchasePricesMap[newLineId] = basePurchasePrice

            val computedUnitPrice: Double = if (useCatalogSalePrice && catPv > 0.0) {
                catPv
            } else {
                kotlin.math.round(basePurchasePrice * safeMargin * 100.0) / 100.0
            }

            LigneOperationEntity(
                id = newLineId,
                operationId = newId,
                produit = formatLineProductWithRef(srcLine.produit, matchedCatalog),
                quantite = srcLine.quantite,
                prixUnitaire = computedUnitPrice,
                parentArticle = srcLine.parentArticle
            )
        }

        val marginFormatted = if (safeMargin % 1.0 == 0.0) {
            safeMargin.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", safeMargin).trimEnd('0').trimEnd('.')
        }

        val originNote = "Document converti à partir de la facture N° ${source.operation.id}"
        val cleanedSourceNotes = source.operation.notes.lines()
            .filterNot { it.trim().startsWith("Document converti à partir", ignoreCase = true) || it.trim().startsWith("Converti depuis", ignoreCase = true) }
            .joinToString("\n")
            .trim()
        val mergedNotes = if (cleanedSourceNotes.isBlank()) originNote else "$cleanedSourceNotes\n$originNote"

        _formState.value = FormDraftState(
            id = newId,
            dateTime = now,
            typeOperation = OperationType.FACTURE,
            clientFournisseur = "",
            montantVerseText = "",
            modeReglement = source.operation.modeReglement.ifBlank { "Espèces" },
            notes = mergedNotes,
            lignes = convertedLines,
            isEditing = false,
            clientError = null,
            appliquerTva = currentSettings.tvaEnabledByDefault || source.operation.appliquerTva,
            tauxTva = if (source.operation.tauxTva > 0.0) source.operation.tauxTva else currentSettings.defaultTvaRate,
            statut = "Validé",
            convertedFromSupplierDocId = source.operation.id,
            marginCoefficientText = marginFormatted,
            useCatalogSalePrice = useCatalogSalePrice,
            sourcePurchasePricesByLineId = purchasePricesMap
        )

        _snackbarMessage.value = "Facture Client $newId préparée (${convertedLines.size} article(s)). Sélectionnez le client puis enregistrez."
        _currentScreen.value = AppScreen.Form(null)
    }

    /**
     * Convertit une Facture Fournisseur (ou Bon Fournisseur) en Bon de Livraison Client
     * et ouvre directement l'écran d'édition pour modifier/remplacer un article, sélectionner le client et enregistrer.
     */
    fun convertSupplierDocToClientBlInEditor(
        source: OperationWithLignes,
        marginCoefficient: Double = companySettings.value.defaultMarginCoefficient,
        useCatalogSalePrice: Boolean = companySettings.value.useCatalogSalePriceOnConvert
    ) {
        val emp = currentEmployee.value
        if (!emp.canModifyBon) {
            _snackbarMessage.value = emp.permissionDeniedMessage("convertir en Bon de Livraison", DocumentPermissionCategory.BON)
            return
        }

        val safeMargin = if (marginCoefficient.isNaN() || marginCoefficient <= 0.0) 1.0 else marginCoefficient
        val newId = generateNextBlId(isSupplier = false)
        val now = System.currentTimeMillis()
        val purchasePricesMap = mutableMapOf<String, Double>()

        val convertedLines = source.lignes
            .filter { srcLine ->
                val cleanLabel = srcLine.produit.replace(Regex("^\\[[^\\]]+]\\s*"), "").trim()
                cleanLabel.isNotBlank() &&
                    !com.example.util.InvoiceOcrHelper.isTableHeaderRow(cleanLabel) &&
                    !com.example.util.InvoiceOcrHelper.isTableFooterOrTotalRow(cleanLabel)
            }
            .map { srcLine ->
            val newLineId = UUID.randomUUID().toString()
            val matchedCatalog = findCatalogProductForLineLabel(srcLine.produit)
            val srcPu = srcLine.prixUnitaire ?: 0.0
            val catPa = matchedCatalog?.prixAchatMoyen ?: matchedCatalog?.prixAchat ?: 0.0
            val catPv = matchedCatalog?.defaultUnitPrice ?: matchedCatalog?.prixVente ?: 0.0

            val basePurchasePrice: Double = when {
                srcPu > 0.0 -> srcPu
                catPa > 0.0 -> catPa
                else -> 0.0
            }
            purchasePricesMap[newLineId] = basePurchasePrice

            val computedUnitPrice: Double? = when {
                useCatalogSalePrice && catPv > 0.0 -> catPv
                catPv > 0.0 && safeMargin == 1.0 -> catPv
                basePurchasePrice > 0.0 -> kotlin.math.round(basePurchasePrice * safeMargin * 100.0) / 100.0
                else -> srcLine.prixUnitaire
            }

            LigneOperationEntity(
                id = newLineId,
                operationId = newId,
                produit = formatLineProductWithRef(srcLine.produit, matchedCatalog),
                quantite = srcLine.quantite,
                prixUnitaire = computedUnitPrice,
                parentArticle = srcLine.parentArticle
            )
        }

        val marginFormatted = if (safeMargin % 1.0 == 0.0) {
            safeMargin.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", safeMargin).trimEnd('0').trimEnd('.')
        }

        val originNote = "Document converti à partir de la facture N° ${source.operation.id}"
        val cleanedSourceNotes = source.operation.notes.lines()
            .filterNot { it.trim().startsWith("Document converti à partir", ignoreCase = true) || it.trim().startsWith("Converti depuis", ignoreCase = true) }
            .joinToString("\n")
            .trim()
        val mergedNotes = if (cleanedSourceNotes.isBlank()) originNote else "$cleanedSourceNotes\n$originNote"

        _formState.value = FormDraftState(
            id = newId,
            dateTime = now,
            typeOperation = OperationType.BL_CLIENT,
            clientFournisseur = "",
            montantVerseText = "",
            modeReglement = source.operation.modeReglement.ifBlank { "Espèces" },
            notes = mergedNotes,
            lignes = convertedLines,
            isEditing = false,
            clientError = null,
            appliquerTva = false,
            tauxTva = companySettings.value.defaultTvaRate,
            statut = "Validé",
            convertedFromSupplierDocId = source.operation.id,
            marginCoefficientText = marginFormatted,
            useCatalogSalePrice = useCatalogSalePrice,
            sourcePurchasePricesByLineId = purchasePricesMap
        )

        _snackbarMessage.value = "Bon de Livraison $newId préparé (${convertedLines.size} article(s)). Sélectionnez le client puis enregistrez."
        _currentScreen.value = AppScreen.Form(null)
    }

    /**
     * Importe le résultat d'une reconnaissance OCR de facture en image directement dans l'éditeur
     * de document (Facture Fournisseur, Facture Client, Bon de Livraison, Devis, etc.).
     */
    fun importOcrInvoiceToForm(
        targetType: OperationType,
        partnerName: String,
        detectedInvoiceNumber: String,
        lines: List<OcrExtractedLine>,
        appliquerTva: Boolean,
        tauxTva: Double,
        marginCoefficient: Double = 1.0,
        appendToExistingDraft: Boolean = false,
        saveImmediately: Boolean = false
    ) {
        // Préserver tous les articles sélectionnés par l'utilisateur sans re-filtrage destructeur
        val normalizedInputLines = lines.map { line ->
            val safeDesignation = line.designation.trim().ifBlank {
                line.reference.trim().ifBlank { "Article scanné" }
            }
            line.copy(designation = safeDesignation)
        }
        val userSelectedLines = normalizedInputLines.filter { it.selected }
        val selectedLines = when {
            userSelectedLines.isNotEmpty() -> userSelectedLines
            normalizedInputLines.isNotEmpty() -> normalizedInputLines
            else -> listOf(
                OcrExtractedLine(
                    designation = if (detectedInvoiceNumber.isNotBlank()) "Bon / Facture N° ${detectedInvoiceNumber.trim()}" else "Bon scanné OCR",
                    quantite = 1.0,
                    prixUnitaire = 0.0,
                    selected = true
                )
            )
        }
        val settings = companySettings.value
        val currentDraft = _formState.value
        val timestamp = System.currentTimeMillis()
        val dateCode = SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date(timestamp))

        val prefix = when (targetType) {
            OperationType.FACTURE -> settings.prefixFacture.ifBlank { "FAC-" }
            OperationType.FACTURE_FOURNISSEUR -> settings.prefixFactureFournisseur.ifBlank { "FACF-" }
            OperationType.BON_COMMANDE -> settings.prefixBonCommande.ifBlank { "BC-" }
            OperationType.BL_CLIENT -> settings.prefixBonLivraison.ifBlank { "BLC-" }
            OperationType.BL_FOURNISSEUR -> settings.prefixBonFournisseur.ifBlank { "BLF-" }
            OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> settings.prefixDevis.ifBlank { "DEV-" }
            OperationType.AVOIR_CLIENT, OperationType.AVOIR_FOURNISSEUR -> settings.prefixAvoir.ifBlank { "AVR-" }
            OperationType.REGLEMENT -> settings.prefixReglement.ifBlank { "REG-" }
        }

        val docId = if (appendToExistingDraft && currentDraft.id.isNotBlank()) {
            currentDraft.id
        } else {
            val baseId = "$prefix$dateCode"
            val existingIds = allOperations.value.map { it.operation.id }.toSet()
            if (!existingIds.contains(baseId)) {
                baseId
            } else {
                var counter = 1
                while (existingIds.contains("$baseId-$counter")) counter++
                "$baseId-$counter"
            }
        }

        val safeMargin = if (marginCoefficient.isNaN() || marginCoefficient <= 0.0) 1.0 else marginCoefficient
        val purchasePricesMap = if (appendToExistingDraft) {
            currentDraft.sourcePurchasePricesByLineId.toMutableMap()
        } else {
            mutableMapOf()
        }

        val newEntities = selectedLines.map { ocrLine ->
            val lineId = UUID.randomUUID().toString()
            val basePu = ocrLine.prixUnitaire.coerceAtLeast(0.0)
            purchasePricesMap[lineId] = basePu
            val finalPu = kotlin.math.round(basePu * safeMargin * 100.0) / 100.0
            val labelWithRef = if (ocrLine.reference.isNotBlank() &&
                !ocrLine.designation.contains(ocrLine.reference, ignoreCase = true)
            ) {
                "[${ocrLine.reference.trim()}] ${ocrLine.designation.trim()}"
            } else {
                ocrLine.designation.trim()
            }
            LigneOperationEntity(
                id = lineId,
                operationId = docId,
                produit = labelWithRef,
                quantite = if (ocrLine.quantite > 0.0) ocrLine.quantite else 1.0,
                prixUnitaire = finalPu
            )
        }

        val ocrNote = if (detectedInvoiceNumber.isNotBlank()) {
            if (!targetType.isPurchaseDocument) {
                "Document converti à partir de la facture N° ${detectedInvoiceNumber.trim()}"
            } else {
                "Facture scannée par OCR (Réf origine : ${detectedInvoiceNumber.trim()})"
            }
        } else {
            "Facture scannée par reconnaissance OCR"
        }

        val finalNotes = if (appendToExistingDraft && currentDraft.notes.isNotBlank()) {
            if (currentDraft.notes.contains(ocrNote)) currentDraft.notes else "${currentDraft.notes}\n$ocrNote"
        } else {
            ocrNote
        }

        val marginFormatted = if (safeMargin % 1.0 == 0.0) {
            safeMargin.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", safeMargin).trimEnd('0').trimEnd('.')
        }

        val resolvedPartner = partnerName.trim().ifBlank {
            if (appendToExistingDraft && currentDraft.clientFournisseur.isNotBlank()) {
                currentDraft.clientFournisseur
            } else if (targetType.isPurchaseDocument) {
                "Fournisseur Divers"
            } else {
                "Client Comptoir"
            }
        }

        val effectiveLines = if (appendToExistingDraft) currentDraft.lignes + newEntities else newEntities

        _formState.value = FormDraftState(
            id = docId,
            dateTime = if (appendToExistingDraft) currentDraft.dateTime else timestamp,
            typeOperation = targetType,
            clientFournisseur = resolvedPartner,
            montantVerseText = if (appendToExistingDraft) currentDraft.montantVerseText else "",
            modeReglement = if (appendToExistingDraft) currentDraft.modeReglement else "Espèces",
            notes = finalNotes,
            lignes = effectiveLines,
            isEditing = if (appendToExistingDraft) currentDraft.isEditing else false,
            clientError = null,
            appliquerTva = appliquerTva,
            tauxTva = tauxTva,
            statut = "Validé",
            convertedFromSupplierDocId = detectedInvoiceNumber.trim().takeIf { it.isNotBlank() && !targetType.isPurchaseDocument } ?: "",
            marginCoefficientText = marginFormatted,
            useCatalogSalePrice = false,
            sourcePurchasePricesByLineId = purchasePricesMap
        )

        if (saveImmediately) {
            val operationEntity = OperationEntity(
                id = docId,
                dateTime = if (appendToExistingDraft) currentDraft.dateTime else timestamp,
                typeOperation = targetType,
                clientFournisseur = resolvedPartner,
                montantVerse = 0.0,
                modeReglement = if (appendToExistingDraft) currentDraft.modeReglement else "Espèces",
                notes = finalNotes,
                appliquerTva = appliquerTva,
                tauxTva = tauxTva,
                statut = "Validé",
                vendeur = currentSellerLabel
            )

            // Déplacer immédiatement sur le bon onglet (Vente vs Achat) et quitter la fenêtre OCR / Formulaire
            if (targetType.isSalesDocument) {
                _currentBottomTab.value = BottomNavTab.VENTES
            } else if (targetType.isPurchaseDocument) {
                _currentBottomTab.value = BottomNavTab.ACHATS
            }
            selectedFilterType.value = null
            searchQuery.value = ""
            val tabName = if (targetType.isSalesDocument) "Vente" else "Achat"
            _snackbarMessage.value = "✓ ${targetType.label} ($docId) enregistré avec succès ! Visible dans l'onglet $tabName."
            _currentScreen.value = AppScreen.MainList

            viewModelScope.launch {
                // 1. Sauvegarde directe garantie dans Room SQLite et Cloud Firestore
                repository.saveOperationWithLignes(operationEntity, effectiveLines)

                // 2. Enregistrement automatique du tiers si nouveau (non bloquant)
                try {
                    val cleanPartner = resolvedPartner.trim()
                    if (cleanPartner.isNotBlank() && cleanPartner != "Client Comptoir" && cleanPartner != "Fournisseur Divers") {
                        val isSup = targetType.isPurchaseDocument
                        val partnerList = if (isSup) suppliersList.value else clientsList.value
                        if (partnerList.none { it.name.trim().equals(cleanPartner, ignoreCase = true) }) {
                            repository.insertPartner(
                                PartnerEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = cleanPartner,
                                    type = if (isSup) PartnerType.FOURNISSEUR else PartnerType.CLIENT
                                )
                            )
                        }
                    }
                } catch (_: Exception) {
                }

                // 3. Mise à jour automatique des stocks et traçabilité des mouvements
                try {
                    applyStockMovementsForOperation(
                        operationEntity = operationEntity,
                        linesToSave = newEntities,
                        isEditing = false
                    )
                } catch (_: Exception) {
                }
            }
        } else {
            _snackbarMessage.value = "Facture OCR importée : ${newEntities.size} article(s) prêt(s) dans l'éditeur (${targetType.label})."
            _currentScreen.value = AppScreen.Form(if (appendToExistingDraft && currentDraft.isEditing) docId else null)
        }
    }

    /**
     * Exécute le workflow complet du module « OCR Intelligent » :
     * - Action A (KEEP_ORIGINAL) : Conserver le document fournisseur/client original tel quel.
     * - Action B (TRANSFORM_TO_CLIENT) : Transformer le document fournisseur en document client (Facture, BL, Devis, BC) avec majoration, remises, TVA et frais.
     * - Action C (SAVE_AND_TRANSFORM) : Enregistrer le document fournisseur original dans Achats ET créer un document client lié dans Ventes avec traçabilité croisée.
     */
    fun executeIntelligentOcrAction(
        context: Context,
        action: com.example.util.OcrTransformationAction,
        ocrResult: com.example.util.InvoiceOcrResult,
        editedLines: List<OcrExtractedLine>,
        supplierDocType: OperationType,
        clientDocType: OperationType,
        supplierNameInput: String,
        clientNameInput: String,
        invoiceNumberInput: String,
        bonNumberInput: String,
        globalMajorationPct: Double,
        remiseGlobalePct: Double,
        appliquerTva: Boolean,
        tauxTvaGlobal: Double,
        fraisTransport: Double,
        fraisSupplementaires: Double,
        autresFrais: Double,
        savedPagePaths: List<String>,
        manualCorrectionsSummary: String,
        openInEditorInsteadOfSave: Boolean = false
    ) {
        val normalizedEdited = (editedLines.ifEmpty { ocrResult.extractedLines }).map { line ->
            val safeDesignation = line.designation.trim().ifBlank {
                line.reference.trim().ifBlank { "Article scanné" }
            }
            line.copy(designation = safeDesignation, userVerified = true)
        }
        val activeLines = normalizedEdited.filter { it.selected }
            .ifEmpty { normalizedEdited }
            .ifEmpty {
                listOf(
                    OcrExtractedLine(
                        designation = "Bon / Facture scanné(e)",
                        quantite = 1.0,
                        prixUnitaire = ocrResult.extractedTotalHt.coerceAtLeast(0.0),
                        selected = true,
                        userVerified = true
                    )
                )
            }

        val settings = companySettings.value
        val now = System.currentTimeMillis()
        val dateCode = SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date(now))
        val existingIds = allOperations.value.map { it.operation.id }.toSet().toMutableSet()

        fun nextUniqueDocId(opType: OperationType): String {
            val prefix = when (opType) {
                OperationType.FACTURE -> settings.prefixFacture.ifBlank { "FAC-" }
                OperationType.FACTURE_FOURNISSEUR -> settings.prefixFactureFournisseur.ifBlank { "FACF-" }
                OperationType.BON_COMMANDE -> settings.prefixBonCommande.ifBlank { "BC-" }
                OperationType.BL_CLIENT -> settings.prefixBonLivraison.ifBlank { "BLC-" }
                OperationType.BL_FOURNISSEUR -> settings.prefixBonFournisseur.ifBlank { "BLF-" }
                OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> settings.prefixDevis.ifBlank { "DEV-" }
                OperationType.AVOIR_CLIENT, OperationType.AVOIR_FOURNISSEUR -> settings.prefixAvoir.ifBlank { "AVR-" }
                OperationType.REGLEMENT -> settings.prefixReglement.ifBlank { "REG-" }
            }
            val baseId = "$prefix$dateCode"
            if (!existingIds.contains(baseId)) {
                existingIds.add(baseId)
                return baseId
            }
            var c = 1
            while (existingIds.contains("$baseId-$c")) c++
            val chosen = "$baseId-$c"
            existingIds.add(chosen)
            return chosen
        }

        val resolvedSupplierName = supplierNameInput.trim().ifBlank {
            ocrResult.detectedPartnerName.trim().ifBlank { "Fournisseur Divers" }
        }
        val resolvedClientName = clientNameInput.trim().ifBlank { "Client Comptoir" }
        val invNum = invoiceNumberInput.trim().ifBlank { ocrResult.detectedInvoiceNumber.trim() }
        val bonNum = bonNumberInput.trim().ifBlank { ocrResult.detectedBonNumber.trim() }

        // Construction des lignes pour le document original ou transformé
        fun buildLinesForDoc(docId: String, applyClientMarkup: Boolean): List<LigneOperationEntity> {
            val resultLines = mutableListOf<LigneOperationEntity>()
            val globalRemiseFactor = (1.0 - (remiseGlobalePct.coerceIn(0.0, 100.0) / 100.0))

            activeLines.forEach { ocrLine ->
                val basePu = ocrLine.prixUnitaire.coerceAtLeast(0.0)
                val puWithMarkup = if (applyClientMarkup) {
                    ocrLine.puHtNet
                } else {
                    val lineRemiseFactor = (1.0 - (ocrLine.remisePct.coerceIn(0.0, 100.0) / 100.0))
                    kotlin.math.round(basePu * lineRemiseFactor * 100.0) / 100.0
                }
                val finalPu = kotlin.math.round(puWithMarkup * globalRemiseFactor * 100.0) / 100.0
                val labelWithRef = if (ocrLine.reference.isNotBlank() &&
                    !ocrLine.designation.contains(ocrLine.reference, ignoreCase = true)
                ) {
                    "[${ocrLine.reference.trim()}] ${ocrLine.designation.trim()}"
                } else {
                    ocrLine.designation.trim()
                }
                resultLines.add(
                    LigneOperationEntity(
                        id = UUID.randomUUID().toString(),
                        operationId = docId,
                        produit = labelWithRef,
                        quantite = if (ocrLine.quantite > 0.0) ocrLine.quantite else 1.0,
                        prixUnitaire = finalPu
                    )
                )
            }

            // Ajout des lignes de frais commerciaux s'ils sont renseignés
            if (fraisTransport > 0.0) {
                resultLines.add(
                    LigneOperationEntity(
                        id = UUID.randomUUID().toString(),
                        operationId = docId,
                        produit = "[FRAIS] Frais de transport",
                        quantite = 1.0,
                        prixUnitaire = kotlin.math.round(fraisTransport * 100.0) / 100.0
                    )
                )
            }
            if (fraisSupplementaires > 0.0) {
                resultLines.add(
                    LigneOperationEntity(
                        id = UUID.randomUUID().toString(),
                        operationId = docId,
                        produit = "[FRAIS] Frais supplémentaires",
                        quantite = 1.0,
                        prixUnitaire = kotlin.math.round(fraisSupplementaires * 100.0) / 100.0
                    )
                )
            }
            if (autresFrais > 0.0) {
                resultLines.add(
                    LigneOperationEntity(
                        id = UUID.randomUUID().toString(),
                        operationId = docId,
                        produit = "[FRAIS] Autres frais commerciaux",
                        quantite = 1.0,
                        prixUnitaire = kotlin.math.round(autresFrais * 100.0) / 100.0
                    )
                )
            }
            return resultLines
        }

        // Si l'utilisateur souhaite ouvrir dans l'éditeur de formulaire avant validation finale
        if (openInEditorInsteadOfSave) {
            val targetDocType = when (action) {
                com.example.util.OcrTransformationAction.KEEP_ORIGINAL -> supplierDocType
                com.example.util.OcrTransformationAction.TRANSFORM_TO_CLIENT,
                com.example.util.OcrTransformationAction.SAVE_AND_TRANSFORM -> clientDocType
            }
            val docId = nextUniqueDocId(targetDocType)
            val applyMarkup = action != com.example.util.OcrTransformationAction.KEEP_ORIGINAL
            val draftLines = buildLinesForDoc(docId, applyMarkup)
            val targetPartner = if (targetDocType.isPurchaseDocument) resolvedSupplierName else resolvedClientName
            val traceNotes = buildString {
                if (invNum.isNotBlank()) append("Facture origine N° $invNum")
                if (bonNum.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append("Bon N° $bonNum")
                }
                if (ocrResult.detectedOrderRef.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append("BC: ${ocrResult.detectedOrderRef}")
                }
                if (isEmpty()) append("Importé via OCR Intelligent IA")
            }

            _formState.value = FormDraftState(
                id = docId,
                dateTime = now,
                typeOperation = targetDocType,
                clientFournisseur = targetPartner,
                montantVerseText = "",
                modeReglement = ocrResult.detectedPaymentConditions.ifBlank { "Espèces" },
                notes = traceNotes,
                lignes = draftLines,
                isEditing = false,
                appliquerTva = appliquerTva,
                tauxTva = tauxTvaGlobal,
                statut = "Validé",
                convertedFromSupplierDocId = invNum,
                marginCoefficientText = String.format(Locale.US, "%.2f", 1.0 + globalMajorationPct / 100.0)
            )
            _currentScreen.value = AppScreen.Form(null)
            return
        }

        // Préparation synchrone des documents à enregistrer pour quitter immédiatement la fenêtre OCR
        val shouldSaveSupplierDoc = action == com.example.util.OcrTransformationAction.KEEP_ORIGINAL ||
            action == com.example.util.OcrTransformationAction.SAVE_AND_TRANSFORM
        val shouldSaveClientDoc = action == com.example.util.OcrTransformationAction.TRANSFORM_TO_CLIENT ||
            action == com.example.util.OcrTransformationAction.SAVE_AND_TRANSFORM

        val createdSupplierDocId = if (shouldSaveSupplierDoc) nextUniqueDocId(supplierDocType) else ""
        val createdClientDocId = if (shouldSaveClientDoc) nextUniqueDocId(clientDocType) else ""

        // Navigation immédiate hors de la fenêtre OCR vers la liste principale (Ventes ou Achats)
        selectedFilterType.value = null
        searchQuery.value = ""
        when (action) {
            com.example.util.OcrTransformationAction.KEEP_ORIGINAL -> {
                _currentBottomTab.value = if (supplierDocType.isPurchaseDocument) BottomNavTab.ACHATS else BottomNavTab.VENTES
                _snackbarMessage.value = "✓ ${supplierDocType.label} ($createdSupplierDocId) enregistré avec succès !"
            }
            com.example.util.OcrTransformationAction.TRANSFORM_TO_CLIENT -> {
                _currentBottomTab.value = BottomNavTab.VENTES
                _snackbarMessage.value = "✓ ${clientDocType.label} ($createdClientDocId) enregistré pour $resolvedClientName dans Vente !"
            }
            com.example.util.OcrTransformationAction.SAVE_AND_TRANSFORM -> {
                _currentBottomTab.value = BottomNavTab.VENTES
                _snackbarMessage.value = "✓ Achat ($createdSupplierDocId) conservé + ${clientDocType.label} ($createdClientDocId) créé dans Vente !"
            }
        }
        _currentScreen.value = AppScreen.MainList

        viewModelScope.launch {
            // 1. Enregistrement immédiat du document fournisseur original (Action A ou Action C)
            var savedSupplierOp: OperationEntity? = null
            var savedSupplierLines: List<LigneOperationEntity> = emptyList()
            if (shouldSaveSupplierDoc) {
                val supplierLines = buildLinesForDoc(createdSupplierDocId, applyClientMarkup = false)
                val supNotes = buildString {
                    append("Document original scanné par OCR Intelligent")
                    if (invNum.isNotBlank()) append(" • Facture N° $invNum")
                    if (bonNum.isNotBlank()) append(" • Bon N° $bonNum")
                    if (ocrResult.detectedOrderRef.isNotBlank()) append(" • Réf Cde: ${ocrResult.detectedOrderRef}")
                }
                val supplierOp = OperationEntity(
                    id = createdSupplierDocId,
                    dateTime = now,
                    typeOperation = supplierDocType,
                    clientFournisseur = resolvedSupplierName,
                    montantVerse = 0.0,
                    modeReglement = ocrResult.detectedPaymentConditions.ifBlank { "Espèces" },
                    notes = supNotes,
                    appliquerTva = appliquerTva,
                    tauxTva = tauxTvaGlobal,
                    statut = "Validé",
                    vendeur = currentSellerLabel
                )
                repository.saveOperationWithLignes(supplierOp, supplierLines)
                savedSupplierOp = supplierOp
                savedSupplierLines = supplierLines
            }

            // 2. Enregistrement immédiat du document client transformé (Action B ou Action C)
            var savedClientOp: OperationEntity? = null
            var savedClientLines: List<LigneOperationEntity> = emptyList()
            if (shouldSaveClientDoc) {
                val clientLines = buildLinesForDoc(createdClientDocId, applyClientMarkup = true)
                val clientNotes = buildString {
                    if (createdSupplierDocId.isNotBlank()) {
                        append("Document converti à partir de ${supplierDocType.label} N° $createdSupplierDocId ($resolvedSupplierName)")
                    } else if (invNum.isNotBlank()) {
                        append("Document converti à partir de la facture fournisseur N° $invNum ($resolvedSupplierName)")
                    } else {
                        append("Document client généré par transformation OCR ($resolvedSupplierName)")
                    }
                    if (globalMajorationPct > 0.0) {
                        append(" • Majoration appliquée : +${CurrencyUtils.formatQty(globalMajorationPct)}%")
                    }
                    if (remiseGlobalePct > 0.0) {
                        append(" • Remise globale : -${CurrencyUtils.formatQty(remiseGlobalePct)}%")
                    }
                }
                val clientOp = OperationEntity(
                    id = createdClientDocId,
                    dateTime = now + 100L, // léger décalage pour apparaître tout en haut
                    typeOperation = clientDocType,
                    clientFournisseur = resolvedClientName,
                    montantVerse = 0.0,
                    modeReglement = ocrResult.detectedPaymentConditions.ifBlank { "Espèces" },
                    notes = clientNotes,
                    appliquerTva = appliquerTva,
                    tauxTva = tauxTvaGlobal,
                    statut = "Validé",
                    vendeur = currentSellerLabel
                )
                repository.saveOperationWithLignes(clientOp, clientLines)
                savedClientOp = clientOp
                savedClientLines = clientLines
            }

            // 3. Enregistrement ou mise à jour des Tiers (Fournisseur & Client)
            try {
                if (resolvedSupplierName.isNotBlank() && resolvedSupplierName != "Fournisseur Divers") {
                    val existingSup = suppliersList.value.find {
                        it.name.trim().equals(resolvedSupplierName, ignoreCase = true)
                    }
                    val updatedSupplier = PartnerEntity(
                        id = existingSup?.id ?: UUID.randomUUID().toString(),
                        name = resolvedSupplierName,
                        type = PartnerType.FOURNISSEUR,
                        phone = ocrResult.detectedPartnerPhone.ifBlank { existingSup?.phone.orEmpty() },
                        address = ocrResult.detectedPartnerAddress.ifBlank { existingSup?.address.orEmpty() },
                        solde = existingSup?.solde ?: 0.0,
                        notes = existingSup?.notes.orEmpty(),
                        rc = ocrResult.detectedPartnerRc.ifBlank { existingSup?.rc.orEmpty() },
                        nif = ocrResult.detectedPartnerNif.ifBlank { existingSup?.nif.orEmpty() },
                        nis = ocrResult.detectedPartnerNis.ifBlank { existingSup?.nis.orEmpty() },
                        articleImposition = ocrResult.detectedPartnerAi.ifBlank { existingSup?.articleImposition.orEmpty() },
                        rib = ocrResult.detectedPartnerRib.ifBlank { existingSup?.rib.orEmpty() }
                    )
                    repository.insertPartner(updatedSupplier)
                }

                if (shouldSaveClientDoc &&
                    resolvedClientName.isNotBlank() &&
                    resolvedClientName != "Client Comptoir"
                ) {
                    val existingCli = clientsList.value.find {
                        it.name.trim().equals(resolvedClientName, ignoreCase = true)
                    }
                    if (existingCli == null) {
                        repository.insertPartner(
                            PartnerEntity(
                                id = UUID.randomUUID().toString(),
                                name = resolvedClientName,
                                type = PartnerType.CLIENT
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }

            // 4. Mise à jour des mouvements de stock en arrière-plan
            try {
                if (savedSupplierOp != null) {
                    applyStockMovementsForOperation(
                        operationEntity = savedSupplierOp,
                        linesToSave = savedSupplierLines,
                        isEditing = false
                    )
                }
                if (savedClientOp != null) {
                    applyStockMovementsForOperation(
                        operationEntity = savedClientOp,
                        linesToSave = savedClientLines,
                        isEditing = false
                    )
                }
            } catch (_: Exception) {
            }

            // 5. Mémoire documentaire intelligente du fournisseur & Historique d'archivage
            try {
                withContext(Dispatchers.IO) {
                    com.example.util.IntelligentOcrEngine.recordPartnerLearning(
                        context = context,
                        partnerName = resolvedSupplierName,
                        ocrResult = ocrResult,
                        correctedLines = activeLines,
                        appliedMarginPct = globalMajorationPct,
                        appliedTvaRate = tauxTvaGlobal
                    )

                    val totalHtFinal = activeLines.sumOf {
                        if (action == com.example.util.OcrTransformationAction.KEEP_ORIGINAL) {
                            it.quantite * it.prixUnitaire
                        } else {
                            it.totalHt
                        }
                    } + fraisTransport + fraisSupplementaires + autresFrais
                    val totalTtcFinal = if (appliquerTva) {
                        totalHtFinal * (1.0 + tauxTvaGlobal / 100.0)
                    } else {
                        totalHtFinal
                    }

                    com.example.util.IntelligentOcrEngine.saveArchiveRecord(
                        context = context,
                        record = com.example.util.OcrArchiveRecord(
                            id = UUID.randomUUID().toString(),
                            importedAt = now,
                            supplierOrPartnerName = resolvedSupplierName,
                            targetClientName = if (action != com.example.util.OcrTransformationAction.KEEP_ORIGINAL) resolvedClientName else "",
                            invoiceNumber = invNum,
                            bonNumber = bonNum,
                            orderReference = ocrResult.detectedOrderReference,
                            sourceDocumentType = supplierDocType,
                            targetDocumentType = clientDocType,
                            actionMode = action,
                            totalHt = totalHtFinal,
                            totalTtc = totalTtcFinal,
                            linesCount = activeLines.size,
                            pageCount = ocrResult.pageCount,
                            rawOcrText = ocrResult.rawText.take(4000),
                            manualCorrectionsSummary = manualCorrectionsSummary,
                            savedImagePaths = savedPagePaths,
                            linkedSupplierDocId = createdSupplierDocId,
                            linkedClientDocId = createdClientDocId,
                            status = "Validé",
                            documentDate = ocrResult.detectedDateText
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Recalcule en direct les prix de vente du brouillon selon le coefficient de marge ou le prix catalogue.
     */
    fun updateDraftMarginPricing(marginCoeffText: String, useCatalogPrice: Boolean) {
        val current = _formState.value
        val parsedCoeff = marginCoeffText.replace(',', '.').toDoubleOrNull()
        val safeCoeff = if (parsedCoeff != null && parsedCoeff > 0.0) parsedCoeff else null

        val updatedLines = current.lignes.map { line ->
            val matchedCatalog = findCatalogProductForLineLabel(line.produit)
            val catPa = matchedCatalog?.prixAchatMoyen ?: matchedCatalog?.prixAchat ?: 0.0
            val catPv = matchedCatalog?.defaultUnitPrice ?: matchedCatalog?.prixVente ?: 0.0
            val basePurchasePrice: Double = current.sourcePurchasePricesByLineId[line.id]
                ?: (if (catPa > 0.0) catPa else (line.prixUnitaire ?: 0.0))

            val newUnitPrice: Double? = when {
                useCatalogPrice && catPv > 0.0 -> catPv
                safeCoeff != null && basePurchasePrice > 0.0 ->
                    kotlin.math.round(basePurchasePrice * safeCoeff * 100.0) / 100.0
                else -> line.prixUnitaire
            }
            line.copy(prixUnitaire = newUnitPrice)
        }

        _formState.value = current.copy(
            marginCoefficientText = marginCoeffText,
            useCatalogSalePrice = useCatalogPrice,
            lignes = updatedLines
        )

        if (safeCoeff != null) {
            val s = companySettings.value
            updateCompanySettings(
                s.copy(
                    defaultMarginCoefficient = safeCoeff,
                    useCatalogSalePriceOnConvert = useCatalogPrice
                )
            )
        }
    }

    /**
     * Remplace directement un article sur une ligne de la grille tout en conservant la quantité.
     */
    fun replaceLineProductInDraft(lineId: String, newProduct: ProductCatalogEntity) {
        val current = _formState.value
        val coeff = current.marginCoefficientText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val newLabel = if (newProduct.reference.isNotBlank()) {
            "[${newProduct.reference.trim()}] ${newProduct.name.trim()}"
        } else {
            newProduct.name.trim()
        }

        val isPurchase = current.typeOperation.isPurchaseDocument
        val prodPa = newProduct.prixAchatMoyen ?: newProduct.prixAchat ?: 0.0
        val prodPv = newProduct.defaultUnitPrice ?: newProduct.prixVente ?: 0.0

        val newUnitPrice: Double = when {
            isPurchase -> if (prodPa > 0.0) prodPa else prodPv
            current.useCatalogSalePrice && prodPv > 0.0 -> prodPv
            prodPv > 0.0 -> prodPv
            prodPa > 0.0 -> kotlin.math.round(prodPa * coeff * 100.0) / 100.0
            else -> 0.0
        }

        val updatedPurchaseMap = current.sourcePurchasePricesByLineId.toMutableMap()
        if (prodPa > 0.0) {
            updatedPurchaseMap[lineId] = prodPa
        }

        val updatedLines = current.lignes.map { line ->
            if (line.id == lineId) {
                line.copy(
                    produit = newLabel,
                    prixUnitaire = if (newUnitPrice > 0.0) newUnitPrice else line.prixUnitaire
                )
            } else {
                line
            }
        }
        _formState.value = current.copy(
            lignes = updatedLines,
            sourcePurchasePricesByLineId = updatedPurchaseMap
        )
    }

    /**
     * Modifie rapidement la quantité d'une ligne directement dans la grille d'édition.
     */
    fun updateLineQuantityInDraft(lineId: String, newQuantite: Double) {
        val safeQty = newQuantite.coerceAtLeast(0.01)
        val current = _formState.value
        _formState.value = current.copy(
            lignes = current.lignes.map { line ->
                if (line.id == lineId) line.copy(quantite = safeQty) else line
            }
        )
    }

    fun onTypeChanged(newType: OperationType) {
        val currentId = _formState.value.id
        val updatedId = if (!_formState.value.isEditing && (currentId.startsWith("BLC-") || currentId.startsWith("BLF-") || currentId.startsWith("FAC-") || currentId.startsWith("FACF-") || currentId.startsWith("BC-") || currentId.startsWith("REG-"))) {
            val suffix = currentId.substringAfter("-")
            "${newType.shortCode}-$suffix"
        } else {
            currentId
        }

        _formState.value = _formState.value.copy(
            typeOperation = newType,
            id = updatedId
        )
    }

    fun onAppliquerTvaChanged(enabled: Boolean) {
        _formState.value = _formState.value.copy(appliquerTva = enabled)
    }

    fun onTauxTvaChanged(taux: Double) {
        _formState.value = _formState.value.copy(tauxTva = taux)
    }

    fun onStatutChanged(statut: String) {
        _formState.value = _formState.value.copy(statut = statut)
    }

    fun onClientFournisseurChanged(value: String) {
        _formState.value = _formState.value.copy(
            clientFournisseur = value,
            clientError = null
        )
    }

    fun onMontantVerseChanged(value: String) {
        _formState.value = _formState.value.copy(montantVerseText = value)
    }

    fun onModeReglementChanged(value: String) {
        _formState.value = _formState.value.copy(modeReglement = value)
    }

    fun onNotesChanged(value: String) {
        _formState.value = _formState.value.copy(notes = value)
    }

    fun onDateTimeChanged(timestamp: Long) {
        _formState.value = _formState.value.copy(dateTime = timestamp)
    }

    // Line Editor Actions
    fun openAddLineDialog() {
        _lineEditor.value = LineEditorState(
            isVisible = true,
            lineId = UUID.randomUUID().toString(),
            produit = "",
            quantiteText = "1",
            prixUnitaireText = "",
            autoReopen = _lineEditor.value.autoReopen,
            produitError = null
        )
    }

    fun openEditLineDialog(ligne: LigneOperationEntity) {
        _lineEditor.value = LineEditorState(
            isVisible = true,
            lineId = ligne.id,
            produit = ligne.produit,
            quantiteText = CurrencyUtils.formatQty(ligne.quantite),
            prixUnitaireText = ligne.prixUnitaire?.let { CurrencyUtils.formatQty(it) } ?: "",
            autoReopen = false,
            produitError = null
        )
    }

    fun onLineProduitChanged(value: String) {
        // If product matches catalog, auto-fill default unit price if empty
        val matchedCatalogProduct = catalogProductsList.value.find {
            it.name.equals(value.trim(), ignoreCase = true)
        }
        val currentPu = _lineEditor.value.prixUnitaireText
        val autoPu = if (currentPu.isBlank() && matchedCatalogProduct?.defaultUnitPrice != null) {
            CurrencyUtils.formatQty(matchedCatalogProduct.defaultUnitPrice)
        } else {
            currentPu
        }

        _lineEditor.value = _lineEditor.value.copy(
            produit = value,
            prixUnitaireText = autoPu,
            produitError = null
        )
    }

    fun onLineQuantiteChanged(value: String) {
        _lineEditor.value = _lineEditor.value.copy(quantiteText = value)
    }

    fun onLinePrixUnitaireChanged(value: String) {
        _lineEditor.value = _lineEditor.value.copy(prixUnitaireText = value)
    }

    fun onLineAutoReopenChanged(enabled: Boolean) {
        _lineEditor.value = _lineEditor.value.copy(autoReopen = enabled)
    }

    fun closeLineDialog() {
        _lineEditor.value = _lineEditor.value.copy(isVisible = false)
    }

    fun onBarcodeScannedInForm(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return
        val matched = catalogProductsList.value.find {
            it.barcode.equals(trimmed, ignoreCase = true) ||
            it.reference.equals(trimmed, ignoreCase = true) ||
            it.name.equals(trimmed, ignoreCase = true)
        }
        if (matched != null) {
            if (matched.isArticleEclate) {
                // Article Éclaté : Toutes les lignes des composants remplacent immédiatement l'article parent dès sa sélection
                viewModelScope.launch {
                    val comps = repository.getCompositionsForProductOnce(matched.id)
                    val updatedLignes = _formState.value.lignes.toMutableList()
                    if (comps.isNotEmpty()) {
                        for (comp in comps) {
                            val compCatalog = catalogProductsList.value.find {
                                it.name.equals(comp.componentName.trim(), ignoreCase = true)
                            }
                            updatedLignes.add(
                                LigneOperationEntity(
                                    id = UUID.randomUUID().toString(),
                                    operationId = _formState.value.id,
                                    produit = comp.componentName,
                                    quantite = comp.quantite,
                                    prixUnitaire = comp.prixUnitaireEstime ?: compCatalog?.defaultUnitPrice,
                                    parentArticle = matched.name
                                )
                            )
                        }
                        _formState.value = _formState.value.copy(lignes = updatedLignes)
                        _snackbarMessage.value = "Article Éclaté « ${matched.name} » (Parent) ajouté avec ses ${comps.size} composés (Fils) !"
                    } else {
                        updatedLignes.add(
                            LigneOperationEntity(
                                id = UUID.randomUUID().toString(),
                                operationId = _formState.value.id,
                                produit = matched.name,
                                quantite = 1.0,
                                prixUnitaire = matched.defaultUnitPrice
                            )
                        )
                        _formState.value = _formState.value.copy(lignes = updatedLignes)
                        _snackbarMessage.value = "${matched.name} ajouté (Quantité: 1)"
                    }
                }
                return
            }

            val updatedLignes = _formState.value.lignes.toMutableList()
            val existingIndex = updatedLignes.indexOfFirst {
                it.produit.equals(matched.name, ignoreCase = true)
            }
            if (existingIndex >= 0) {
                val existing = updatedLignes[existingIndex]
                val updated = existing.copy(quantite = existing.quantite + 1.0)
                updatedLignes[existingIndex] = updated
                _formState.value = _formState.value.copy(lignes = updatedLignes)
                _snackbarMessage.value = "${matched.name} (+1 ajouté, Total: ${CurrencyUtils.formatQty(updated.quantite)})"
            } else {
                val newLine = LigneOperationEntity(
                    id = UUID.randomUUID().toString(),
                    operationId = _formState.value.id,
                    produit = matched.name,
                    quantite = 1.0,
                    prixUnitaire = matched.defaultUnitPrice
                )
                updatedLignes.add(newLine)
                _formState.value = _formState.value.copy(lignes = updatedLignes)
                _snackbarMessage.value = if (matched.isArticleKit) {
                    "Article Kit « ${matched.name} » ajouté (1 ligne forfaitaire, déstockage auto en arrière-plan)"
                } else {
                    "${matched.name} ajouté (Quantité: 1)"
                }
            }
        } else {
            _lineEditor.value = LineEditorState(
                isVisible = true,
                lineId = UUID.randomUUID().toString(),
                produit = trimmed,
                quantiteText = "1",
                prixUnitaireText = "",
                autoReopen = false,
                produitError = null
            )
            _snackbarMessage.value = "Code '$trimmed' non répertorié. Veuillez ajuster les détails."
        }
    }

    fun saveLine(forceClose: Boolean = false) {
        val current = _lineEditor.value
        val prodTrimmed = current.produit.trim()
        if (prodTrimmed.isEmpty()) {
            _lineEditor.value = current.copy(produitError = "Veuillez saisir le nom du produit")
            return
        }

        val qte = CurrencyUtils.parseAmount(current.quantiteText).coerceAtLeast(0.01)
        val pu = if (current.prixUnitaireText.isNotBlank()) {
            CurrencyUtils.parseAmount(current.prixUnitaireText)
        } else {
            null
        }

        val matchedCatalogProduct = catalogProductsList.value.find {
            it.name.equals(prodTrimmed, ignoreCase = true)
        }

        // Si l'article sélectionné est un "Article Éclaté" (et non un "Article Kit") :
        // Toutes les lignes des composants remplacent immédiatement l'article parent dès sa sélection.
        val isNewInsertion = _formState.value.lignes.none { it.id == current.lineId }
        if (matchedCatalogProduct != null && matchedCatalogProduct.isArticleEclate && isNewInsertion) {
            viewModelScope.launch {
                val comps = repository.getCompositionsForProductOnce(matchedCatalogProduct.id)
                val updatedLignes = _formState.value.lignes.toMutableList()
                if (comps.isNotEmpty()) {
                    for (comp in comps) {
                        val compQty = comp.quantite * qte
                        val compCatalog = catalogProductsList.value.find {
                            it.name.equals(comp.componentName.trim(), ignoreCase = true)
                        }
                        val unitPrice = comp.prixUnitaireEstime ?: compCatalog?.defaultUnitPrice
                        updatedLignes.add(
                            LigneOperationEntity(
                                id = UUID.randomUUID().toString(),
                                operationId = _formState.value.id,
                                produit = comp.componentName,
                                quantite = compQty,
                                prixUnitaire = unitPrice,
                                parentArticle = matchedCatalogProduct.name
                            )
                        )
                    }
                    _formState.value = _formState.value.copy(lignes = updatedLignes)
                    _snackbarMessage.value = "Article Éclaté « ${matchedCatalogProduct.name} » (Parent) ajouté avec ses ${comps.size} composés (Fils) !"
                } else {
                    updatedLignes.add(
                        LigneOperationEntity(
                            id = current.lineId.ifEmpty { UUID.randomUUID().toString() },
                            operationId = _formState.value.id,
                            produit = prodTrimmed,
                            quantite = qte,
                            prixUnitaire = pu ?: matchedCatalogProduct.defaultUnitPrice
                        )
                    )
                    _formState.value = _formState.value.copy(lignes = updatedLignes)
                }

                if (current.autoReopen && !forceClose) {
                    _lineEditor.value = LineEditorState(
                        isVisible = true,
                        lineId = UUID.randomUUID().toString(),
                        produit = "",
                        quantiteText = "1",
                        prixUnitaireText = "",
                        autoReopen = true,
                        produitError = null
                    )
                } else {
                    closeLineDialog()
                }
            }
            return
        }

        // Pour un Article Standard ou un Article Kit (Nomenclature commerciale) :
        // 1 seule ligne (l'article parent) s'affiche par défaut avec son prix forfaitaire.
        val updatedLignes = _formState.value.lignes.toMutableList()
        val existingIndex = updatedLignes.indexOfFirst { it.id == current.lineId }
        val preservedParent = if (existingIndex >= 0) updatedLignes[existingIndex].parentArticle else ""

        val effectivePu = pu ?: matchedCatalogProduct?.defaultUnitPrice
        val newLine = LigneOperationEntity(
            id = current.lineId.ifEmpty { UUID.randomUUID().toString() },
            operationId = _formState.value.id,
            produit = prodTrimmed,
            quantite = qte,
            prixUnitaire = effectivePu,
            parentArticle = preservedParent
        )

        if (existingIndex >= 0) {
            updatedLignes[existingIndex] = newLine
        } else {
            updatedLignes.add(newLine)
        }
        _formState.value = _formState.value.copy(lignes = updatedLignes)

        if (current.autoReopen && !forceClose) {
            _lineEditor.value = LineEditorState(
                isVisible = true,
                lineId = UUID.randomUUID().toString(),
                produit = "",
                quantiteText = "1",
                prixUnitaireText = "",
                autoReopen = true,
                produitError = null
            )
            _snackbarMessage.value = if (matchedCatalogProduct?.isArticleKit == true) {
                "Article Kit ajouté (1 ligne parent forfaitaire) ! Saisissez le suivant."
            } else {
                "Article ajouté ! Saisissez le suivant."
            }
        } else {
            if (matchedCatalogProduct?.isArticleKit == true) {
                _snackbarMessage.value = "Article Kit « ${matchedCatalogProduct.name} » ajouté (1 seule ligne forfaitaire)"
            }
            closeLineDialog()
        }
    }

    fun deleteLineFromDraft(lineId: String) {
        val updated = _formState.value.lignes.filterNot { it.id == lineId }
        _formState.value = _formState.value.copy(lignes = updated)
    }

    fun deleteParentGroupFromDraft(parentName: String) {
        val updated = _formState.value.lignes.filterNot {
            it.parentArticle.trim().equals(parentName.trim(), ignoreCase = true)
        }
        _formState.value = _formState.value.copy(lignes = updated)
    }

    fun saveOperation() {
        val draft = _formState.value
        val rawPartner = draft.clientFournisseur.trim()

        if (rawPartner.isEmpty() && draft.typeOperation == OperationType.REGLEMENT) {
            val label = "le client ou fournisseur"
            _formState.value = draft.copy(clientError = "Veuillez renseigner $label")
            _snackbarMessage.value = "Veuillez renseigner $label avant d'enregistrer"
            return
        }

        val partner = rawPartner.ifEmpty {
            if (draft.typeOperation.isPurchaseDocument) "Fournisseur Divers" else "Client Comptoir"
        }

        val verseAmount = draft.montantVerseNumeric
        if (draft.typeOperation == OperationType.REGLEMENT && verseAmount <= 0.0) {
            _snackbarMessage.value = "Veuillez saisir un montant pour le règlement"
            return
        }

        val emp = currentEmployee.value
        if (!emp.canModifyOperation(draft.typeOperation)) {
            _snackbarMessage.value = emp.permissionDeniedMessage(
                action = if (draft.isEditing) "modifier" else "écrire / créer",
                category = draft.typeOperation.permissionCategory()
            )
            return
        }

        val operationEntity = OperationEntity(
            id = draft.id.ifEmpty { "${draft.typeOperation.shortCode}-${System.currentTimeMillis()}" },
            dateTime = draft.dateTime,
            typeOperation = draft.typeOperation,
            clientFournisseur = partner,
            montantVerse = verseAmount,
            modeReglement = draft.modeReglement,
            notes = draft.notes.trim(),
            appliquerTva = draft.appliquerTva,
            tauxTva = draft.tauxTva,
            statut = draft.statut,
            vendeur = currentSellerLabel
        )

        val linesToSave = if (draft.showProductLines) draft.lignes else emptyList()

        viewModelScope.launch {
            repository.saveOperationWithLignes(operationEntity, linesToSave)

            // Basculer automatiquement sur le bon onglet (Ventes Client vs Achats) et réinitialiser les filtres
            // pour que le document enregistré soit immédiatement visible en haut de la liste
            if (operationEntity.typeOperation.isSalesDocument) {
                _currentBottomTab.value = BottomNavTab.VENTES
                selectedFilterType.value = null
                searchQuery.value = ""
            } else if (operationEntity.typeOperation.isPurchaseDocument) {
                _currentBottomTab.value = BottomNavTab.ACHATS
                selectedFilterType.value = null
                searchQuery.value = ""
            }

            val destTabLabel = if (operationEntity.typeOperation.isPurchaseDocument) "Achats" else "Ventes Client"
            _snackbarMessage.value = "${operationEntity.typeOperation.label} (${operationEntity.id}) enregistré dans $destTabLabel !"
            _currentScreen.value = AppScreen.MainList

            // Mise à jour automatique des stocks et traçabilité des Entrées / Sorties (Mouvements de Stock) en arrière-plan
            applyStockMovementsForOperation(
                operationEntity = operationEntity,
                linesToSave = linesToSave,
                isEditing = draft.isEditing
            )
        }
    }

    private suspend fun applyStockMovementsForOperation(
        operationEntity: OperationEntity,
        linesToSave: List<LigneOperationEntity>,
        isEditing: Boolean
    ) {
        try {
            val isProForma = operationEntity.typeOperation == OperationType.DEVIS_CLIENT ||
                    operationEntity.typeOperation == OperationType.DEVIS_FOURNISSEUR ||
                    operationEntity.typeOperation == OperationType.BON_COMMANDE

            if (!isEditing && !isProForma) {
                val isStockExit = operationEntity.typeOperation == OperationType.BL_CLIENT ||
                        operationEntity.typeOperation == OperationType.FACTURE ||
                        operationEntity.typeOperation == OperationType.AVOIR_FOURNISSEUR
                val isStockEntry = operationEntity.typeOperation == OperationType.BL_FOURNISSEUR ||
                        operationEntity.typeOperation == OperationType.FACTURE_FOURNISSEUR ||
                        operationEntity.typeOperation == OperationType.AVOIR_CLIENT

                for (ligne in linesToSave) {
                    val catProduct = findCatalogProductForLineLabel(ligne.produit)
                        ?: repository.getCatalogProductByNameOnce(ligne.produit.trim())

                    if (catProduct != null && (catProduct.isArticleKit || catProduct.isArticleEclate)) {
                        val compos = repository.getCompositionsForProductOnce(catProduct.id)
                        for (c in compos) {
                            val compQty = c.quantite * ligne.quantite
                            val compProd = repository.getCatalogProductByNameOnce(c.componentName)
                            if (compProd != null && (isStockExit || isStockEntry)) {
                                val prevStock = compProd.stockActuel
                                val newStock = if (isStockExit) {
                                    (prevStock - compQty).coerceAtLeast(0.0)
                                } else {
                                    prevStock + compQty
                                }
                                repository.recordStockMovementAndApply(
                                    StockMovementEntity(
                                        productId = compProd.id,
                                        productName = compProd.name,
                                        type = if (isStockExit) StockMovementType.SORTIE else StockMovementType.ENTREE,
                                        quantity = compQty,
                                        previousStock = prevStock,
                                        newStock = newStock,
                                        unit = compProd.unit,
                                        operationId = operationEntity.id,
                                        partnerName = operationEntity.clientFournisseur,
                                        reason = "${operationEntity.typeOperation.label} (${operationEntity.id}) - Déstockage auto arrière-plan Kit « ${catProduct.name} »"
                                    )
                                )
                            } else {
                                val compDelta = if (isStockExit) -compQty else if (isStockEntry) compQty else 0.0
                                if (compDelta != 0.0) {
                                    repository.adjustStockByName(c.componentName, compDelta)
                                }
                            }
                        }
                    }

                    if (catProduct != null && (isStockExit || isStockEntry)) {
                        val prevStock = catProduct.stockActuel
                        val newStock = if (isStockExit) {
                            (prevStock - ligne.quantite).coerceAtLeast(0.0)
                        } else {
                            prevStock + ligne.quantite
                        }
                        repository.recordStockMovementAndApply(
                            StockMovementEntity(
                                productId = catProduct.id,
                                productName = catProduct.name,
                                type = if (isStockExit) StockMovementType.SORTIE else StockMovementType.ENTREE,
                                quantity = ligne.quantite,
                                previousStock = prevStock,
                                newStock = newStock,
                                unit = catProduct.unit,
                                operationId = operationEntity.id,
                                partnerName = operationEntity.clientFournisseur,
                                reason = "${operationEntity.typeOperation.label} ${operationEntity.id}"
                            )
                        )
                    } else {
                        val delta = if (isStockExit) -ligne.quantite else if (isStockEntry) ligne.quantite else 0.0
                        if (delta != 0.0) {
                            repository.adjustStockByName(ligne.produit, delta)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Fonction d'éclatement manuel :
     * Pour un Article Kit (rigide par défaut), permet de décomposer manuellement la ligne parente
     * en ses lignes de composants indépendantes afin de pouvoir modifier leurs quantités ou supprimer un composant à la volée.
     */
    fun explodeArticleInDraft(ligneId: String) {
        val currentLignes = _formState.value.lignes.toMutableList()
        val index = currentLignes.indexOfFirst { it.id == ligneId }
        if (index < 0) return
        val targetLigne = currentLignes[index]

        viewModelScope.launch {
            val catProduct = catalogProductsList.value.find {
                it.name.equals(targetLigne.produit.trim(), ignoreCase = true)
            }
            if (catProduct == null || (!catProduct.isArticleEclate && !catProduct.isArticleKit)) {
                _snackbarMessage.value = "Cet article n'est pas configuré comme Article Kit ou Article Éclaté."
                return@launch
            }

            val components = repository.getCompositionsForProductOnce(catProduct.id)
            if (components.isEmpty()) {
                _snackbarMessage.value = "Aucun sous-composant défini pour cet article."
                return@launch
            }

            currentLignes.removeAt(index)
            components.forEachIndexed { cIndex, comp ->
                val compQty = comp.quantite * targetLigne.quantite
                val compCatalog = catalogProductsList.value.find {
                    it.name.equals(comp.componentName.trim(), ignoreCase = true)
                }
                currentLignes.add(
                    index + cIndex,
                    LigneOperationEntity(
                        id = UUID.randomUUID().toString(),
                        operationId = _formState.value.id,
                        produit = comp.componentName,
                        quantite = compQty,
                        prixUnitaire = comp.prixUnitaireEstime ?: compCatalog?.defaultUnitPrice,
                        parentArticle = targetLigne.produit
                    )
                )
            }
            _formState.value = _formState.value.copy(lignes = currentLignes)
            _snackbarMessage.value = "Éclatement effectué : « ${targetLigne.produit} » (Parent) décomposé en ${components.size} composés (Fils) !"
        }
    }

    fun deleteOperation(id: String) {
        val cleanId = id.trim()
        val targetOp = allOperations.value.find {
            it.operation.id.trim().equals(cleanId, ignoreCase = true) ||
                it.operation.id == id ||
                it.operation.id.endsWith(cleanId) ||
                cleanId.endsWith(it.operation.id)
        }?.operation
        val emp = currentEmployee.value
        if (targetOp != null && !emp.canDeleteOperation(targetOp.typeOperation)) {
            _snackbarMessage.value = emp.permissionDeniedMessage(
                action = "supprimer",
                category = targetOp.typeOperation.permissionCategory()
            )
            return
        }
        viewModelScope.launch {
            repository.deleteOperation(cleanId)
            targetOp?.id?.let {
                if (it.trim() != cleanId) repository.deleteOperation(it)
            }
            _snackbarMessage.value = "Opération « $cleanId » supprimée avec succès"
            if (_currentScreen.value is AppScreen.Detail) {
                _currentScreen.value = AppScreen.MainList
            }
        }
    }

    // --- Authentification Employé & Gestion des Droits (Bon, Facture, Créance) ---

    fun setAuthenticatedEmployee(employee: EmployeeEntity?) {
        _authenticatedEmployee.value = employee
    }

    fun authenticateEmployee(
        email: String,
        password: String,
        onSuccess: (EmployeeEntity) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.authenticateEmployee(email, password)
            result.onSuccess { emp ->
                _authenticatedEmployee.value = emp
                _snackbarMessage.value = "Bienvenue ${emp.name} (${emp.role.label})"
                onSuccess(emp)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Identifiants invalides"
                onError(msg)
            }
        }
    }

    fun saveEmployeeAccount(employee: EmployeeEntity) {
        val emp = currentEmployee.value
        if (!emp.canManageEmployees && emp.role != EmployeeRole.ADMIN) {
            _snackbarMessage.value = "Accès refusé : Seul un administrateur peut modifier les comptes employés et leurs droits."
            return
        }
        viewModelScope.launch {
            repository.insertEmployee(employee)
            if (_authenticatedEmployee.value?.id == employee.id) {
                _authenticatedEmployee.value = employee
            }
            _snackbarMessage.value = "Droits et profil de « ${employee.name} » enregistrés !"
        }
    }

    fun saveMultipleEmployeeAccounts(employeesToSave: List<EmployeeEntity>) {
        val emp = currentEmployee.value
        if (!emp.canManageEmployees && emp.role != EmployeeRole.ADMIN) {
            _snackbarMessage.value = "Accès refusé : Seul un administrateur peut ajouter des comptes utilisateurs."
            return
        }
        viewModelScope.launch {
            var count = 0
            for (item in employeesToSave) {
                if (item.name.isNotBlank() && item.email.isNotBlank()) {
                    repository.insertEmployee(item)
                    count++
                }
            }
            _snackbarMessage.value = "$count utilisateur(s) ajouté(s) et synchronisé(s) avec succès !"
        }
    }

    fun deleteEmployeeAccount(employee: EmployeeEntity) {
        val emp = currentEmployee.value
        if (!emp.canManageEmployees && emp.role != EmployeeRole.ADMIN) {
            _snackbarMessage.value = "Accès refusé : Seul un administrateur peut supprimer un compte employé."
            return
        }
        val activeAdmins = allEmployees.value.count { it.role == EmployeeRole.ADMIN && it.isActive }
        if (employee.role == EmployeeRole.ADMIN && activeAdmins <= 1) {
            _snackbarMessage.value = "Impossible de supprimer le dernier compte Administrateur actif."
            return
        }
        viewModelScope.launch {
            repository.deleteEmployeeById(employee.id)
            _snackbarMessage.value = "Compte employé « ${employee.name} » supprimé."
        }
    }

    // --- Actions protégées de Modification et Suppression de Créance ---

    fun modifyOperationCreanceVersement(
        opWithLignes: OperationWithLignes,
        newMontantVerse: Double
    ) {
        val emp = currentEmployee.value
        if (!emp.canModifyCreance) {
            _snackbarMessage.value = emp.permissionDeniedMessage("modifier", DocumentPermissionCategory.CREANCE)
            return
        }
        viewModelScope.launch {
            repository.updateOperationMontantVerse(
                operationId = opWithLignes.operation.id,
                newMontantVerse = newMontantVerse
            )
            _snackbarMessage.value = "Créance / Versement modifié pour ${opWithLignes.operation.id} (${CurrencyUtils.formatDa(newMontantVerse)})"
        }
    }

    fun adjustPartnerCreanceSolde(
        item: PartnerBalanceItem,
        newTargetSolde: Double,
        reason: String
    ) {
        val emp = currentEmployee.value
        if (!emp.canModifyCreance) {
            _snackbarMessage.value = emp.permissionDeniedMessage("modifier", DocumentPermissionCategory.CREANCE)
            return
        }
        val currentSolde = item.soldeRestant
        val cleanTarget = newTargetSolde.coerceAtLeast(0.0)
        val diff = cleanTarget - currentSolde
        if (kotlin.math.abs(diff) < 0.01) {
            _snackbarMessage.value = "Aucun changement sur le solde."
            return
        }
        viewModelScope.launch {
            val dateCode = SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())
            val noteText = reason.trim().ifEmpty { "Modification de créance par ${emp.name}" }
            val opsDiff = item.totalTransactions - item.totalReglements
            if (cleanTarget >= opsDiff.coerceAtLeast(0.0)) {
                // Mise à jour directe de la créance restante du partenaire (sans créer de BL)
                val newBaseSolde = (cleanTarget - opsDiff).coerceAtLeast(0.0)
                repository.updatePartner(
                    item.partner.copy(
                        solde = newBaseSolde,
                        notes = if (reason.isNotBlank()) reason.trim() else item.partner.notes
                    )
                )
            } else {
                // Le nouveau solde cible est inférieur au reste dû des BL existants : on met la créance de base à 0 et on enregistre un règlement d'ajustement
                repository.updatePartner(item.partner.copy(solde = 0.0))
                val reductionAmount = (opsDiff - cleanTarget).coerceAtLeast(0.0)
                if (reductionAmount > 0.01) {
                    val regOp = OperationEntity(
                        id = "REG-ADJ-$dateCode",
                        dateTime = System.currentTimeMillis(),
                        typeOperation = OperationType.REGLEMENT,
                        clientFournisseur = item.partner.name,
                        montantVerse = reductionAmount,
                        modeReglement = "Ajustement Créance",
                        notes = "$noteText (Ancien solde: ${CurrencyUtils.formatDa(currentSolde)} → Nouveau: ${CurrencyUtils.formatDa(cleanTarget)})",
                        vendeur = currentSellerLabel,
                        statut = "Validé"
                    )
                    repository.saveOperationWithLignes(regOp, emptyList())
                }
            }
            _snackbarMessage.value = "Créance restante de « ${item.partner.name} » mise à jour à ${CurrencyUtils.formatDa(cleanTarget)} !"
        }
    }

    fun deletePartnerCreance(partnerName: String) {
        val emp = currentEmployee.value
        if (!emp.canDeleteCreance && !emp.canModifyCreance) {
            _snackbarMessage.value = emp.permissionDeniedMessage("remettre à zéro / supprimer", DocumentPermissionCategory.CREANCE)
            return
        }
        viewModelScope.launch {
            repository.clearPartnerCreance(partnerName)
            _snackbarMessage.value = "Créance de « $partnerName » remise à zéro (0,00 DA) avec succès !"
        }
    }

    fun resetAllCreancesToZero(partnerType: PartnerType? = null) {
        val emp = currentEmployee.value
        if (!emp.canDeleteCreance && !emp.canModifyCreance && !emp.isAdmin) {
            _snackbarMessage.value = emp.permissionDeniedMessage("remettre à zéro", DocumentPermissionCategory.CREANCE)
            return
        }
        viewModelScope.launch {
            repository.resetAllCreancesToZero(partnerType)
            _snackbarMessage.value = when (partnerType) {
                PartnerType.CLIENT -> "Toutes les créances clients ont été remises à zéro (0,00 DA) !"
                PartnerType.FOURNISSEUR -> "Toutes les dettes fournisseurs ont été remises à zéro (0,00 DA) !"
                null -> "Toutes les créances et dettes ont été remises à zéro (0,00 DA) !"
            }
        }
    }

    // --- Import & Directory Management Methods (Excel .xlsx & CSV -> Firestore) ---

    private val _isImportingFile = MutableStateFlow(false)
    val isImportingFile: StateFlow<Boolean> = _isImportingFile

    private val _lastImportSummary = MutableStateFlow<com.example.util.FirestoreImportSummary?>(null)
    val lastImportSummary: StateFlow<com.example.util.FirestoreImportSummary?> = _lastImportSummary

    fun clearImportSummary() {
        _lastImportSummary.value = null
    }

    fun importPartnersFromUri(
        context: Context,
        uri: Uri,
        type: PartnerType,
        customColumns: List<com.example.util.PartnerColumnSpec>? = null
    ) {
        val label = if (type == PartnerType.CLIENT) "Clients" else "Fournisseurs"
        viewModelScope.launch {
            _isImportingFile.value = true
            try {
                val readResult = withContext(Dispatchers.IO) {
                    ImportUtils.readRowsFromUri(context, uri)
                }
                val pair = readResult.getOrElse { err ->
                    val errMsg = err.localizedMessage ?: "Impossible de lire le fichier sélectionné."
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = label,
                        fileFormat = "Fichier",
                        totalRowsRead = 0,
                        createdCount = 0,
                        updatedCount = 0,
                        skippedCount = 0,
                        errorMessage = errMsg
                    )
                    _snackbarMessage.value = errMsg
                    return@launch
                }
                val formatLabel = pair.first
                val rows = pair.second

                // Protection anti-mélange : si le fichier contient clairement des colonnes Articles (réf, désignation, famille, marque, prix...),
                // on l'importe automatiquement dans le catalogue Articles (products) et JAMAIS dans les Clients (partners).
                if (ImportUtils.isClearlyArticlesRows(rows)) {
                    val articleBatch = withContext(Dispatchers.IO) {
                        ImportUtils.parseProductsFromRows(rows, formatLabel, null)
                    }
                    if (articleBatch.items.isNotEmpty()) {
                        val (created, updated) = repository.upsertCatalogProductsFromImport(articleBatch.items)
                        val summary = com.example.util.FirestoreImportSummary(
                            entityLabel = "Articles (Stock)",
                            fileFormat = articleBatch.fileFormat,
                            totalRowsRead = articleBatch.totalRowsRead,
                            createdCount = created,
                            updatedCount = updated,
                            skippedCount = articleBatch.skippedRowsCount,
                            errorMessage = null
                        )
                        _lastImportSummary.value = summary
                        _snackbarMessage.value = "Fichier d'Articles détecté : ${summary.totalImported} article(s) ajouté(s) dans Articles ($created créés, $updated mis à jour) !"
                        return@launch
                    }
                }

                val batch = withContext(Dispatchers.IO) {
                    ImportUtils.parsePartnersFromRows(rows, type, formatLabel, customColumns)
                }
                if (batch.errorMessage != null || batch.items.isEmpty()) {
                    val errMsg = batch.errorMessage ?: "Aucun $label valide trouvé dans le fichier."
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = label,
                        fileFormat = batch.fileFormat,
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = 0,
                        updatedCount = 0,
                        skippedCount = batch.skippedRowsCount,
                        errorMessage = errMsg
                    )
                    _snackbarMessage.value = errMsg
                } else {
                    val (created, updated) = repository.upsertPartnersFromImport(batch.items)
                    val summary = com.example.util.FirestoreImportSummary(
                        entityLabel = label,
                        fileFormat = batch.fileFormat,
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = created,
                        updatedCount = updated,
                        skippedCount = batch.skippedRowsCount,
                        errorMessage = null
                    )
                    _lastImportSummary.value = summary
                    _snackbarMessage.value = "${summary.totalImported} $label synchronisé(s) sur Firestore ($created créés, $updated mis à jour) !"
                }
            } catch (e: Exception) {
                val errMsg = "Erreur d'importation Firestore : ${e.localizedMessage ?: "Échec"}"
                _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                    entityLabel = label,
                    fileFormat = "Fichier",
                    totalRowsRead = 0,
                    createdCount = 0,
                    updatedCount = 0,
                    skippedCount = 0,
                    errorMessage = errMsg
                )
                _snackbarMessage.value = errMsg
            } finally {
                _isImportingFile.value = false
            }
        }
    }

    fun importProductsFromUri(
        context: Context,
        uri: Uri,
        customColumns: List<com.example.util.ArticleColumnSpec>? = null
    ) {
        viewModelScope.launch {
            _isImportingFile.value = true
            try {
                val readResult = withContext(Dispatchers.IO) {
                    ImportUtils.readRowsFromUri(context, uri)
                }
                val pair = readResult.getOrElse { err ->
                    val errMsg = err.localizedMessage ?: "Impossible de lire le fichier sélectionné."
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = "Articles (Stock)",
                        fileFormat = "Fichier",
                        totalRowsRead = 0,
                        createdCount = 0,
                        updatedCount = 0,
                        skippedCount = 0,
                        errorMessage = errMsg
                    )
                    _snackbarMessage.value = errMsg
                    return@launch
                }
                val formatLabel = pair.first
                val rows = pair.second

                // Protection anti-mélange : si le fichier contient clairement des colonnes Clients/Fournisseurs (téléphone, adresse, nif, rc...),
                // on l'importe dans Clients (partners) et JAMAIS dans Articles.
                if (ImportUtils.isClearlyPartnersRows(rows)) {
                    val partnerBatch = withContext(Dispatchers.IO) {
                        ImportUtils.parsePartnersFromRows(rows, PartnerType.CLIENT, formatLabel, null)
                    }
                    if (partnerBatch.items.isNotEmpty()) {
                        val (created, updated) = repository.upsertPartnersFromImport(partnerBatch.items)
                        val summary = com.example.util.FirestoreImportSummary(
                            entityLabel = "Clients",
                            fileFormat = partnerBatch.fileFormat,
                            totalRowsRead = partnerBatch.totalRowsRead,
                            createdCount = created,
                            updatedCount = updated,
                            skippedCount = partnerBatch.skippedRowsCount,
                            errorMessage = null
                        )
                        _lastImportSummary.value = summary
                        _snackbarMessage.value = "Fichier Clients détecté : ${summary.totalImported} client(s) ajouté(s) dans Clients ($created créés, $updated mis à jour) !"
                        return@launch
                    }
                }

                val batch = withContext(Dispatchers.IO) {
                    ImportUtils.parseProductsFromRows(rows, formatLabel, customColumns)
                }
                if (batch.errorMessage != null || batch.items.isEmpty()) {
                    val errMsg = batch.errorMessage ?: "Aucun article valide trouvé dans le fichier."
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = "Articles (Stock)",
                        fileFormat = batch.fileFormat,
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = 0,
                        updatedCount = 0,
                        skippedCount = batch.skippedRowsCount,
                        errorMessage = errMsg
                    )
                    _snackbarMessage.value = errMsg
                } else {
                    val (created, updated) = repository.upsertCatalogProductsFromImport(batch.items)
                    val summary = com.example.util.FirestoreImportSummary(
                        entityLabel = "Articles (Stock)",
                        fileFormat = batch.fileFormat,
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = created,
                        updatedCount = updated,
                        skippedCount = batch.skippedRowsCount,
                        errorMessage = null
                    )
                    _lastImportSummary.value = summary
                    _snackbarMessage.value = "${summary.totalImported} article(s) synchronisé(s) sur Firestore ($created créés, $updated mis à jour) !"
                }
            } catch (e: Exception) {
                val errMsg = "Erreur d'importation Firestore : ${e.localizedMessage ?: "Échec"}"
                _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                    entityLabel = "Articles (Stock)",
                    fileFormat = "Fichier",
                    totalRowsRead = 0,
                    createdCount = 0,
                    updatedCount = 0,
                    skippedCount = 0,
                    errorMessage = errMsg
                )
                _snackbarMessage.value = errMsg
            } finally {
                _isImportingFile.value = false
            }
        }
    }

    fun importPartners(
        text: String,
        type: PartnerType,
        customColumns: List<com.example.util.PartnerColumnSpec>? = null
    ) {
        val label = if (type == PartnerType.CLIENT) "Clients" else "Fournisseurs"
        viewModelScope.launch {
            _isImportingFile.value = true
            try {
                val rows = ImportUtils.parseCsvOrTextToRows(text)
                if (ImportUtils.isClearlyArticlesRows(rows)) {
                    val articleBatch = ImportUtils.parseProductsFromRows(rows, "CSV / Texte", null)
                    if (articleBatch.items.isNotEmpty()) {
                        val (created, updated) = repository.upsertCatalogProductsFromImport(articleBatch.items)
                        _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                            entityLabel = "Articles (Stock)",
                            fileFormat = articleBatch.fileFormat,
                            totalRowsRead = articleBatch.totalRowsRead,
                            createdCount = created,
                            updatedCount = updated,
                            skippedCount = articleBatch.skippedRowsCount
                        )
                        _snackbarMessage.value = "Liste d'Articles détectée : ${articleBatch.items.size} article(s) importé(s) dans Articles ($created créés, $updated mis à jour) !"
                        return@launch
                    }
                }

                val batch = ImportUtils.parsePartnersFromRows(rows, type, "CSV / Texte", customColumns)
                if (batch.items.isNotEmpty()) {
                    val (created, updated) = repository.upsertPartnersFromImport(batch.items)
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = label,
                        fileFormat = batch.fileFormat,
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = created,
                        updatedCount = updated,
                        skippedCount = batch.skippedRowsCount
                    )
                    val typeName = if (type == PartnerType.CLIENT) "clients" else "fournisseurs"
                    _snackbarMessage.value = "${batch.items.size} $typeName importé(s) avec succès ($created créés, $updated mis à jour) !"
                } else {
                    val err = batch.errorMessage ?: "Aucun contact valide détecté dans le texte ou fichier."
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = label,
                        fileFormat = "CSV / Texte",
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = 0,
                        updatedCount = 0,
                        skippedCount = batch.skippedRowsCount,
                        errorMessage = err
                    )
                    _snackbarMessage.value = err
                }
            } finally {
                _isImportingFile.value = false
            }
        }
    }

    fun importProducts(
        text: String,
        customColumns: List<com.example.util.ArticleColumnSpec>? = null
    ) {
        viewModelScope.launch {
            _isImportingFile.value = true
            try {
                val rows = ImportUtils.parseCsvOrTextToRows(text)
                if (ImportUtils.isClearlyPartnersRows(rows)) {
                    val partnerBatch = ImportUtils.parsePartnersFromRows(rows, PartnerType.CLIENT, "CSV / Texte", null)
                    if (partnerBatch.items.isNotEmpty()) {
                        val (created, updated) = repository.upsertPartnersFromImport(partnerBatch.items)
                        _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                            entityLabel = "Clients",
                            fileFormat = partnerBatch.fileFormat,
                            totalRowsRead = partnerBatch.totalRowsRead,
                            createdCount = created,
                            updatedCount = updated,
                            skippedCount = partnerBatch.skippedRowsCount
                        )
                        _snackbarMessage.value = "Liste de Clients détectée : ${partnerBatch.items.size} client(s) importé(s) dans Clients ($created créés, $updated mis à jour) !"
                        return@launch
                    }
                }

                val batch = ImportUtils.parseProductsFromRows(rows, "CSV / Texte", customColumns)
                if (batch.items.isNotEmpty()) {
                    val (created, updated) = repository.upsertCatalogProductsFromImport(batch.items)
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = "Articles (Stock)",
                        fileFormat = batch.fileFormat,
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = created,
                        updatedCount = updated,
                        skippedCount = batch.skippedRowsCount
                    )
                    _snackbarMessage.value = "${batch.items.size} article(s) importé(s) avec succès ($created créés, $updated mis à jour) !"
                } else {
                    val err = batch.errorMessage ?: "Aucun article valide détecté dans le texte ou fichier."
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = "Articles (Stock)",
                        fileFormat = "CSV / Texte",
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = 0,
                        updatedCount = 0,
                        skippedCount = batch.skippedRowsCount,
                        errorMessage = err
                    )
                    _snackbarMessage.value = err
                }
            } finally {
                _isImportingFile.value = false
            }
        }
    }

    fun importCreancesFromUri(
        context: Context,
        uri: Uri,
        defaultType: PartnerType = PartnerType.CLIENT,
        customColumns: List<com.example.util.CreanceColumnSpec>? = null
    ) {
        val label = if (defaultType == PartnerType.CLIENT) "Créances Clients" else "Dettes Fournisseurs"
        viewModelScope.launch {
            _isImportingFile.value = true
            try {
                val batch = withContext(Dispatchers.IO) {
                    ImportUtils.parseCreancesFromUri(context, uri, defaultType, customColumns)
                }
                if (batch.errorMessage != null || batch.items.isEmpty()) {
                    val errMsg = batch.errorMessage ?: "Aucune créance valide trouvée dans le fichier."
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = label,
                        fileFormat = batch.fileFormat,
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = 0,
                        updatedCount = 0,
                        skippedCount = batch.skippedRowsCount,
                        errorMessage = errMsg
                    )
                    _snackbarMessage.value = errMsg
                } else {
                    val (created, updated) = repository.upsertCreancesFromImport(batch.items)
                    val summary = com.example.util.FirestoreImportSummary(
                        entityLabel = label,
                        fileFormat = batch.fileFormat,
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = created,
                        updatedCount = updated,
                        skippedCount = batch.skippedRowsCount,
                        errorMessage = null
                    )
                    _lastImportSummary.value = summary
                    _snackbarMessage.value = "${summary.totalImported} créance(s) importée(s) et synchronisée(s) ($created créées, $updated mises à jour) !"
                }
            } catch (e: Exception) {
                val errMsg = "Erreur d'importation des créances : ${e.localizedMessage ?: "Échec"}"
                _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                    entityLabel = label,
                    fileFormat = "Fichier",
                    totalRowsRead = 0,
                    createdCount = 0,
                    updatedCount = 0,
                    skippedCount = 0,
                    errorMessage = errMsg
                )
                _snackbarMessage.value = errMsg
            } finally {
                _isImportingFile.value = false
            }
        }
    }

    fun importCreances(
        text: String,
        defaultType: PartnerType = PartnerType.CLIENT,
        customColumns: List<com.example.util.CreanceColumnSpec>? = null
    ) {
        val label = if (defaultType == PartnerType.CLIENT) "Créances Clients" else "Dettes Fournisseurs"
        viewModelScope.launch {
            _isImportingFile.value = true
            try {
                val rows = ImportUtils.parseCsvOrTextToRows(text)
                val batch = ImportUtils.parseCreancesFromRows(rows, defaultType, "CSV / Texte", customColumns)
                if (batch.items.isNotEmpty()) {
                    val (created, updated) = repository.upsertCreancesFromImport(batch.items)
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = label,
                        fileFormat = batch.fileFormat,
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = created,
                        updatedCount = updated,
                        skippedCount = batch.skippedRowsCount
                    )
                    _snackbarMessage.value = "${batch.items.size} créance(s) importée(s) avec succès ($created créées, $updated mises à jour) !"
                } else {
                    val err = batch.errorMessage ?: "Aucune créance valide détectée dans le texte ou fichier."
                    _lastImportSummary.value = com.example.util.FirestoreImportSummary(
                        entityLabel = label,
                        fileFormat = "CSV / Texte",
                        totalRowsRead = batch.totalRowsRead,
                        createdCount = 0,
                        updatedCount = 0,
                        skippedCount = batch.skippedRowsCount,
                        errorMessage = err
                    )
                    _snackbarMessage.value = err
                }
            } finally {
                _isImportingFile.value = false
            }
        }
    }

    fun addSinglePartner(
        name: String,
        type: PartnerType,
        phone: String = "",
        address: String = "",
        rc: String = "",
        articleImposition: String = "",
        nif: String = "",
        nis: String = "",
        nin: String = "",
        rib: String = "",
        notes: String = ""
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val partner = PartnerEntity(
                name = name.trim(),
                type = type,
                phone = phone.trim(),
                address = address.trim(),
                rc = rc.trim(),
                articleImposition = articleImposition.trim(),
                nif = nif.trim(),
                nis = nis.trim(),
                nin = nin.trim(),
                rib = rib.trim(),
                notes = notes.trim()
            )
            repository.insertPartner(partner)
            _snackbarMessage.value = "${partner.type.label} « ${partner.name} » ajouté !"
        }
    }

    fun addSingleProduct(
        name: String,
        priceText: String = "",
        category: String = "Général",
        famille: String = category,
        unit: String = "U",
        stock: Double = 0.0,
        seuilAlerte: Double = 5.0,
        isArticleEclate: Boolean = false,
        isArticleKit: Boolean = false,
        barcode: String = "",
        reference: String = "",
        marque: String = "",
        prixAchatText: String = "",
        prixVenteText: String = priceText
    ) {
        if (name.isBlank()) return
        val effectivePrixVenteText = prixVenteText.ifBlank { priceText }
        val prixVente = if (effectivePrixVenteText.isNotBlank()) CurrencyUtils.parseAmount(effectivePrixVenteText) else null
        val prixAchat = if (prixAchatText.isNotBlank()) CurrencyUtils.parseAmount(prixAchatText) else null
        val effectiveFamille = famille.trim().ifEmpty { category.trim() }.ifEmpty { "Général" }
        viewModelScope.launch {
            val product = ProductCatalogEntity(
                name = name.trim(),
                defaultUnitPrice = prixVente,
                prixAchatMoyen = prixAchat,
                category = effectiveFamille,
                marque = marque.trim(),
                unit = unit.trim().ifEmpty { "U" },
                stockActuel = stock,
                seuilAlerte = seuilAlerte,
                isArticleEclate = isArticleEclate,
                isArticleKit = isArticleKit,
                barcode = barcode.trim(),
                reference = reference.trim()
            )
            repository.insertCatalogProduct(product)
            _snackbarMessage.value = "Article « ${product.name} » ajouté au catalogue !"
        }
    }

    fun updatePartner(partner: PartnerEntity) {
        viewModelScope.launch {
            repository.updatePartner(partner)
            _snackbarMessage.value = "Fiche ${partner.name} mise à jour !"
        }
    }

    fun updateCatalogProduct(product: ProductCatalogEntity) {
        viewModelScope.launch {
            repository.updateCatalogProduct(product)
            _snackbarMessage.value = "Article « ${product.name} » mis à jour !"
        }
    }

    fun startNewOperationForPartner(partner: PartnerEntity, specificType: OperationType? = null) {
        val targetType = specificType ?: if (partner.type == PartnerType.CLIENT) OperationType.BL_CLIENT else OperationType.BL_FOURNISSEUR
        val dateCode = SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())
        val generatedId = "${targetType.shortCode}-$dateCode"
        _formState.value = FormDraftState(
            id = generatedId,
            dateTime = System.currentTimeMillis(),
            typeOperation = targetType,
            clientFournisseur = partner.name,
            montantVerseText = "",
            modeReglement = "Espèces",
            notes = "",
            lignes = emptyList(),
            isEditing = false
        )
        _currentScreen.value = AppScreen.Form(null)
    }

    fun getPartnerSummary(partnerId: String): kotlinx.coroutines.flow.Flow<PartnerFinancialSummary?> {
        return repository.getPartnerById(partnerId).combine(allOperations) { partner, ops ->
            if (partner == null) return@combine null

            val partnerNameLower = partner.name.trim().lowercase(Locale.FRENCH)
            val matchedOps = ops.filter {
                it.operation.clientFournisseur.trim().lowercase(Locale.FRENCH) == partnerNameLower &&
                    !OperationRepository.isImportedCreanceOperation(it.operation)
            }

            var blSum = 0.0
            var regSum = 0.0
            var blCount = 0
            var regCount = 0

            for (item in matchedOps) {
                when (item.operation.typeOperation) {
                    OperationType.BL_CLIENT, OperationType.FACTURE, OperationType.BON_COMMANDE -> {
                        blSum += item.totalTtc
                        blCount++
                        if (item.operation.montantVerse > 0) {
                            regSum += item.operation.montantVerse
                        }
                    }
                    OperationType.AVOIR_CLIENT -> {
                        blSum -= item.totalTtc
                        blCount++
                    }
                    OperationType.BL_FOURNISSEUR, OperationType.FACTURE_FOURNISSEUR -> {
                        blSum += item.totalTtc
                        blCount++
                        if (item.operation.montantVerse > 0) {
                            regSum += item.operation.montantVerse
                        }
                    }
                    OperationType.AVOIR_FOURNISSEUR -> {
                        blSum -= item.totalTtc
                        blCount++
                    }
                    OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> {
                        // Devis pro forma (non comptabilisé dans le solde ferme)
                    }
                    OperationType.REGLEMENT -> {
                        regSum += item.operation.montantVerse
                        regCount++
                    }
                }
            }

            val opsNetRestant = (blSum - regSum).coerceAtLeast(0.0)
            val baseCreance = if (blSum > 0.0 && kotlin.math.abs(partner.solde - opsNetRestant) < 0.01) {
                0.0
            } else {
                partner.solde.coerceAtLeast(0.0)
            }
            val restantDu = (baseCreance + blSum - regSum).coerceAtLeast(0.0)

            PartnerFinancialSummary(
                partner = partner,
                totalBl = blSum.coerceAtLeast(0.0),
                totalReglements = regSum,
                soldeRestantDu = restantDu,
                countBl = blCount,
                countReglements = regCount,
                operations = matchedOps
            )
        }
    }

    // --- Gestion du Stock, Mouvements Entrées/Sorties & Alertes Seuil Bas ---

    fun updateStock(productId: String, newStock: Double, reason: String = "Ajustement d'inventaire") {
        val prod = catalogProductsList.value.find { it.id == productId }
        viewModelScope.launch {
            if (prod != null) {
                val prev = prod.stockActuel
                val diff = kotlin.math.abs(newStock - prev)
                val movType = when {
                    newStock > prev -> StockMovementType.ENTREE
                    newStock < prev -> StockMovementType.SORTIE
                    else -> StockMovementType.AJUSTEMENT
                }
                repository.recordStockMovementAndApply(
                    StockMovementEntity(
                        productId = prod.id,
                        productName = prod.name,
                        type = movType,
                        quantity = diff,
                        previousStock = prev,
                        newStock = newStock.coerceAtLeast(0.0),
                        unit = prod.unit,
                        reason = reason
                    )
                )
            } else {
                repository.updateStock(productId, newStock.coerceAtLeast(0.0))
            }
            _snackbarMessage.value = "Stock mis à jour !"
        }
    }

    fun adjustStock(productId: String, delta: Double, reason: String = "") {
        val prod = catalogProductsList.value.find { it.id == productId } ?: return
        val prev = prod.stockActuel
        val newStock = (prev + delta).coerceAtLeast(0.0)
        val movType = if (delta >= 0) StockMovementType.ENTREE else StockMovementType.SORTIE
        val defaultReason = if (delta >= 0) "Entrée rapide de stock" else "Sortie rapide de stock"
        viewModelScope.launch {
            repository.recordStockMovementAndApply(
                StockMovementEntity(
                    productId = prod.id,
                    productName = prod.name,
                    type = movType,
                    quantity = kotlin.math.abs(delta),
                    previousStock = prev,
                    newStock = newStock,
                    unit = prod.unit,
                    reason = reason.ifBlank { defaultReason }
                )
            )
            _snackbarMessage.value = "${movType.label} enregistrée pour « ${prod.name} » (${CurrencyUtils.formatQty(newStock)} ${prod.unit})"
        }
    }

    fun recordManualStockMovement(
        product: ProductCatalogEntity,
        type: StockMovementType,
        quantity: Double,
        partnerName: String = "",
        reason: String = "",
        operationId: String? = null
    ) {
        if (quantity <= 0.0 && type != StockMovementType.AJUSTEMENT) return
        val prevStock = product.stockActuel
        val newStock = when (type) {
            StockMovementType.ENTREE -> prevStock + quantity
            StockMovementType.SORTIE -> (prevStock - quantity).coerceAtLeast(0.0)
            StockMovementType.AJUSTEMENT -> quantity.coerceAtLeast(0.0)
        }
        val moveQty = if (type == StockMovementType.AJUSTEMENT) kotlin.math.abs(newStock - prevStock) else quantity

        viewModelScope.launch {
            repository.recordStockMovementAndApply(
                StockMovementEntity(
                    productId = product.id,
                    productName = product.name,
                    type = type,
                    quantity = moveQty,
                    previousStock = prevStock,
                    newStock = newStock,
                    unit = product.unit,
                    operationId = operationId?.takeIf { it.isNotBlank() },
                    partnerName = partnerName.trim(),
                    reason = reason.trim().ifEmpty { type.label }
                )
            )
            _snackbarMessage.value = "${type.label} enregistrée : ${product.name} → Stock : ${CurrencyUtils.formatQty(newStock)} ${product.unit}"
        }
    }

    fun updateSeuilAlerte(productId: String, newThreshold: Double) {
        viewModelScope.launch {
            repository.updateSeuilAlerte(productId, newThreshold.coerceAtLeast(0.0))
            _snackbarMessage.value = "Seuil d'alerte bas mis à jour (${CurrencyUtils.formatQty(newThreshold)}) !"
        }
    }

    fun deleteStockMovement(movementId: String) {
        val emp = currentEmployee.value
        if (!emp.canDeleteCatalogAndPartners) {
            _snackbarMessage.value = "Accès refusé (${emp.name}) : Vous n'avez pas l'autorisation de suppression."
            return
        }
        viewModelScope.launch {
            repository.deleteStockMovementById(movementId)
            _snackbarMessage.value = "Mouvement de stock supprimé de l'historique"
        }
    }

    fun saveArticleEclateWithComposition(product: ProductCatalogEntity, components: List<ArticleCompositionEntity>) {
        val emp = currentEmployee.value
        if (!emp.canWriteCatalogAndPartners) {
            _snackbarMessage.value = "Accès refusé (${emp.name}) : Vous n'avez pas l'autorisation d'écriture."
            return
        }
        viewModelScope.launch {
            repository.saveArticleEclateWithComposition(product, components)
            _snackbarMessage.value = "Article composé « ${product.name} » enregistré !"
        }
    }

    fun getCompositionsForProduct(parentId: String): kotlinx.coroutines.flow.Flow<List<ArticleCompositionEntity>> {
        return repository.getCompositionsForProduct(parentId)
    }

    fun deleteComposition(id: String) {
        val emp = currentEmployee.value
        if (!emp.canDeleteCatalogAndPartners) {
            _snackbarMessage.value = "Accès refusé (${emp.name}) : Vous n'avez pas l'autorisation de suppression."
            return
        }
        viewModelScope.launch {
            repository.deleteComposition(id)
            _snackbarMessage.value = "Composant retiré de l'article composé"
        }
    }

    fun deletePartner(id: String) {
        val emp = currentEmployee.value
        if (!emp.canDeleteCatalogAndPartners) {
            _snackbarMessage.value = "Accès refusé (${emp.name}) : Vous n'avez pas l'autorisation de supprimer un client/fournisseur."
            return
        }
        viewModelScope.launch {
            repository.deletePartnerById(id)
            _snackbarMessage.value = "Partenaire supprimé"
        }
    }

    fun deleteCatalogProduct(id: String) {
        val emp = currentEmployee.value
        if (!emp.canDeleteCatalogAndPartners) {
            _snackbarMessage.value = "Accès refusé (${emp.name}) : Vous n'avez pas l'autorisation de supprimer un article."
            return
        }
        viewModelScope.launch {
            repository.deleteCatalogProductById(id)
            _snackbarMessage.value = "Article supprimé du catalogue"
        }
    }

    fun clearPartners(type: PartnerType) {
        val emp = currentEmployee.value
        if (!emp.canDeleteCatalogAndPartners) {
            _snackbarMessage.value = "Accès refusé (${emp.name}) : Vous n'avez pas l'autorisation de suppression."
            return
        }
        viewModelScope.launch {
            repository.clearPartners(type)
            val typeName = if (type == PartnerType.CLIENT) "clients" else "fournisseurs"
            _snackbarMessage.value = "Liste des $typeName effacée"
        }
    }

    fun clearCatalogProducts() {
        val emp = currentEmployee.value
        if (!emp.canDeleteCatalogAndPartners) {
            _snackbarMessage.value = "Accès refusé (${emp.name}) : Vous n'avez pas l'autorisation de suppression."
            return
        }
        viewModelScope.launch {
            repository.clearAllCatalogProducts()
            _snackbarMessage.value = "Catalogue d'articles effacé"
        }
    }

    /**
     * Remise à zéro complète de la base de données (Articles, Clients, Fournisseurs, Bons, Factures, Créances et Mouvements de Stock)
     * strictement réservée à l'Administrateur.
     */
    fun resetDatabaseForAdmin(onlyDirectory: Boolean = false) {
        val emp = currentEmployee.value
        val isAdmin = emp.role == EmployeeRole.ADMIN ||
            emp.email.equals(EmployeeEntity.PRIMARY_ADMIN_EMAIL, ignoreCase = true)
        if (!isAdmin) {
            _snackbarMessage.value = "Accès refusé (${emp.name}) : Seul l'administrateur peut remettre la base de données à zéro."
            return
        }
        viewModelScope.launch {
            _isImportingFile.value = true
            try {
                if (onlyDirectory) {
                    repository.resetDirectoryOnly()
                    _lastImportSummary.value = null
                    _snackbarMessage.value = "Répertoire (Articles, Clients & Fournisseurs) remis à zéro avec succès !"
                } else {
                    repository.resetEntireDatabase()
                    _lastImportSummary.value = null
                    _snackbarMessage.value = "Base de données entièrement remise à zéro (Articles, Clients, Fournisseurs, Bons, Factures & Créances) !"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Erreur lors de la remise à zéro : ${e.localizedMessage ?: "Échec"}"
            } finally {
                _isImportingFile.value = false
            }
        }
    }

    // --- Sauvegarde & Restauration Google Drive ---

    private val prefs = getApplication<Application>().getSharedPreferences("gestion_merelec_backup", Context.MODE_PRIVATE)

    private val _lastBackupDate = MutableStateFlow<String?>(prefs.getString("last_backup_date", null))
    val lastBackupDate: StateFlow<String?> = _lastBackupDate

    private val _driveAccessToken = MutableStateFlow<String?>(null)
    val driveAccessToken: StateFlow<String?> = _driveAccessToken

    private val _driveBackups = MutableStateFlow<List<com.example.util.DriveBackupFile>>(emptyList())
    val driveBackups: StateFlow<List<com.example.util.DriveBackupFile>> = _driveBackups

    private val _isDriveBusy = MutableStateFlow(false)
    val isDriveBusy: StateFlow<Boolean> = _isDriveBusy

    fun setDriveAccessToken(token: String) {
        _driveAccessToken.value = token
        refreshDriveBackups(token)
    }

    fun refreshDriveBackups(token: String? = _driveAccessToken.value) {
        val activeToken = token ?: return
        viewModelScope.launch {
            _isDriveBusy.value = true
            val result = com.example.util.GoogleDriveService.listBackupFiles(activeToken)
            _isDriveBusy.value = false
            result.onSuccess { files ->
                _driveBackups.value = files
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Erreur lors de la synchronisation Google Drive"
            }
        }
    }

    fun uploadBackupToGoogleDrive(token: String? = _driveAccessToken.value) {
        val activeToken = token ?: return
        viewModelScope.launch {
            _isDriveBusy.value = true
            val fileName = "Gestion_Merelec_Backup_${DateUtils.formatForExport(System.currentTimeMillis())}.json"
            val json = generateBackupJson()
            val result = com.example.util.GoogleDriveService.uploadBackupFile(activeToken, fileName, json)
            result.onSuccess { uploadedFile ->
                recordBackupSuccess()
                _snackbarMessage.value = "Sauvegarde synchronisée sur Google Drive (${uploadedFile.name}) !"
                val listRes = com.example.util.GoogleDriveService.listBackupFiles(activeToken)
                listRes.onSuccess { _driveBackups.value = it }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Échec de l'envoi vers Google Drive"
            }
            _isDriveBusy.value = false
        }
    }

    fun downloadBackupFromGoogleDrive(
        fileId: String,
        onBackupReady: (BackupContent) -> Unit
    ) {
        val activeToken = _driveAccessToken.value ?: return
        viewModelScope.launch {
            _isDriveBusy.value = true
            val result = com.example.util.GoogleDriveService.downloadBackupFile(activeToken, fileId)
            _isDriveBusy.value = false
            result.onSuccess { jsonText ->
                val parsed = BackupUtils.parseBackupJson(jsonText)
                if (parsed != null) {
                    onBackupReady(parsed)
                } else {
                    _snackbarMessage.value = "Ce fichier Google Drive n'est pas une sauvegarde Gestion Merelec valide."
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Impossible de télécharger la sauvegarde Google Drive"
            }
        }
    }

    fun deleteBackupFromGoogleDrive(fileId: String) {
        val emp = currentEmployee.value
        if (!emp.isAdmin) {
            _snackbarMessage.value = "Accès refusé (${emp.name}) : Seul l'administrateur peut supprimer une sauvegarde."
            return
        }
        val activeToken = _driveAccessToken.value ?: return
        viewModelScope.launch {
            _isDriveBusy.value = true
            val result = com.example.util.GoogleDriveService.deleteBackupFile(activeToken, fileId)
            result.onSuccess {
                _driveBackups.value = _driveBackups.value.filterNot { it.id == fileId }
                _snackbarMessage.value = "Sauvegarde supprimée de Google Drive"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Impossible de supprimer le fichier"
            }
            _isDriveBusy.value = false
        }
    }

    fun recordBackupSuccess() {
        val nowFormatted = DateUtils.formatDateTime(System.currentTimeMillis())
        prefs.edit().putString("last_backup_date", nowFormatted).apply()
        _lastBackupDate.value = nowFormatted
    }

    fun generateBackupJson(): String {
        val allCompositions = articlesEclatesWithComposition.value.flatMap { it.composantes }
        return BackupUtils.createBackupJson(
            operations = allOperations.value,
            clients = clientsList.value,
            suppliers = suppliersList.value,
            products = catalogProductsList.value,
            compositions = allCompositions
        )
    }

    fun restoreDatabase(backup: BackupContent, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                // 1. Restore partners (clients & suppliers)
                if (backup.partners.isNotEmpty()) {
                    repository.insertPartners(backup.partners)
                }

                // 2. Restore products catalog
                if (backup.products.isNotEmpty()) {
                    repository.insertCatalogProducts(backup.products)
                }

                // 2b. Restore kit compositions if present
                if (backup.compositions.isNotEmpty()) {
                    val groupedByParent = backup.compositions.groupBy { it.parentProductId }
                    for ((parentId, comps) in groupedByParent) {
                        val parentProd = backup.products.find { it.id == parentId }
                            ?: catalogProductsList.value.find { it.id == parentId }
                        if (parentProd != null) {
                            repository.saveArticleEclateWithComposition(parentProd, comps)
                        }
                    }
                }

                // 3. Restore operations and product lines
                for (pair in backup.operations) {
                    repository.saveOperationWithLignes(pair.first, pair.second)
                }

                recordBackupSuccess()
                _snackbarMessage.value = "Restauration réussie : ${backup.operationsCount} bon(s), ${backup.clientsCount + backup.suppliersCount} contact(s) et ${backup.productsCount} article(s) restaurés !"
                onComplete()
            } catch (e: Exception) {
                _snackbarMessage.value = "Erreur de restauration : ${e.localizedMessage}"
            }
        }
    }

    // --- Personnalisation du Logo, Cachet Humide (PNG) & Signature (PNG) ---

    private val _logoVersion = MutableStateFlow(0)
    val logoVersion: StateFlow<Int> = _logoVersion

    private val _stampVersion = MutableStateFlow(0)
    val stampVersion: StateFlow<Int> = _stampVersion

    fun saveCustomLogo(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destFile = File(context.filesDir, "custom_logo.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                _logoVersion.value += 1
                _snackbarMessage.value = "Logo personnalisé appliqué avec succès !"
            } catch (e: Exception) {
                _snackbarMessage.value = "Erreur lors de l'enregistrement du logo : ${e.localizedMessage}"
            }
        }
    }

    fun resetToDefaultLogo(context: Context) {
        val destFile = File(context.filesDir, "custom_logo.png")
        if (destFile.exists()) {
            destFile.delete()
        }
        _logoVersion.value += 1
        _snackbarMessage.value = "Logo officiel Merélec rétabli"
    }

    fun saveCustomStampPng(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destFile = File(context.filesDir, "custom_stamp.png")
                val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input)
                }
                if (decoded != null) {
                    FileOutputStream(destFile).use { output ->
                        decoded.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
                    }
                    _stampVersion.value += 1
                    _logoVersion.value += 1
                    _snackbarMessage.value = "Cachet humide (format PNG) enregistré et appliqué à vos Bons et Règlements !"
                } else {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    _stampVersion.value += 1
                    _logoVersion.value += 1
                    _snackbarMessage.value = "Cachet humide (format PNG) enregistré avec succès !"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Erreur lors de l'enregistrement du cachet humide PNG : ${e.localizedMessage}"
            }
        }
    }

    fun resetStampToDefaultPng(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val customFile = File(context.filesDir, "custom_stamp.png")
            if (customFile.exists()) customFile.delete()
            val defaultFile = File(context.filesDir, "default_stamp.png")
            if (defaultFile.exists()) defaultFile.delete()
            com.example.util.PdfGenerator.getOrCreateWetStampPngFile(context, companySettings.value)
            _stampVersion.value += 1
            _logoVersion.value += 1
            _snackbarMessage.value = "Cachet humide officiel (PNG HD) régénéré"
        }
    }
}

