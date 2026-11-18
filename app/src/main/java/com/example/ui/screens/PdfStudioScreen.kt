package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CompanySettings
import com.example.data.model.LigneOperationEntity
import com.example.data.model.OperationEntity
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import com.example.ui.viewmodel.CreancesSummary
import com.example.ui.viewmodel.OperationViewModel
import com.example.ui.viewmodel.PartnerBalanceItem
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.PdfGenerator
import java.io.File
import java.util.Locale

enum class PdfStudioTab(val index: Int, val label: String, val subtitle: String) {
    DEVIS(0, "Devis", "Devis Client & Fournisseur (Logo HD & Cachet PNG)"),
    BON(1, "Bons & Règlements", "BL, BC, BR & Règlements A4 (Logo HD & Cachet PNG)"),
    FACTURE(2, "Factures", "Facture & Facture d'Avoir (Logo HD & Cachet PNG)"),
    CREANCE(3, "Créances", "État Global & Relevé de Compte"),
    CATALOGUE(4, "Catalogue Article", "Liste & Tarifs des Articles")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfStudioScreen(
    viewModel: OperationViewModel,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedSettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val logoVersion by viewModel.logoVersion.collectAsStateWithLifecycle()
    val allOperations by viewModel.allOperations.collectAsStateWithLifecycle()
    val allProducts by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    val clientBalances by viewModel.clientsBalances.collectAsStateWithLifecycle()
    val supplierBalances by viewModel.suppliersBalances.collectAsStateWithLifecycle()
    val creancesSummary by viewModel.creancesSummary.collectAsStateWithLifecycle()
    val compositeArticles by viewModel.articlesEclatesWithComposition.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val isAdminUser = currentEmployee.isAdmin
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }

    var selectedTab by remember(initialTab) {
        mutableStateOf(PdfStudioTab.entries.find { it.index == initialTab } ?: PdfStudioTab.DEVIS)
    }

    val stampVersion by viewModel.stampVersion.collectAsStateWithLifecycle()

    // Logo, Cachet Humide PNG & Signature PNG state
    val customLogoFile = remember(logoVersion) { File(context.filesDir, "custom_logo.png") }
    val customBitmap = remember(logoVersion, customLogoFile.lastModified()) {
        if (customLogoFile.exists()) {
            try {
                BitmapFactory.decodeFile(customLogoFile.absolutePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }
    val stampBitmap = remember(stampVersion, logoVersion, savedSettings) {
        PdfGenerator.loadOrCreateWetStampBitmap(context, savedSettings)?.asImageBitmap()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.saveCustomLogo(context, it) }
    }

    val stampPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.saveCustomStampPng(context, it) }
    }

    // Editable Company Coordinates & PDF Header state
    var companyName by remember(savedSettings) { mutableStateOf(savedSettings.companyName) }
    var activitySlogan by remember(savedSettings) { mutableStateOf(savedSettings.activitySlogan) }
    var address by remember(savedSettings) { mutableStateOf(savedSettings.address) }
    var phone by remember(savedSettings) { mutableStateOf(savedSettings.phone) }
    var phoneSecondary by remember(savedSettings) { mutableStateOf(savedSettings.phoneSecondary) }
    var email by remember(savedSettings) { mutableStateOf(savedSettings.email) }
    var website by remember(savedSettings) { mutableStateOf(savedSettings.website) }
    var rc by remember(savedSettings) { mutableStateOf(savedSettings.rc) }
    var articleImposition by remember(savedSettings) { mutableStateOf(savedSettings.articleImposition) }
    var nif by remember(savedSettings) { mutableStateOf(savedSettings.nif) }
    var nis by remember(savedSettings) { mutableStateOf(savedSettings.nis) }
    var nin by remember(savedSettings) { mutableStateOf(savedSettings.nin) }
    var bankName by remember(savedSettings) { mutableStateOf(savedSettings.bankName) }
    var bankRib by remember(savedSettings) { mutableStateOf(savedSettings.bankRib) }
    var pdfCustomFooter by remember(savedSettings) { mutableStateOf(savedSettings.pdfCustomFooter) }

    var pdfShowLogo by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowLogo) }
    var pdfLogoPosition by remember(savedSettings) { mutableStateOf(savedSettings.pdfLogoPosition) }
    var pdfLogoSize by remember(savedSettings) { mutableIntStateOf(savedSettings.pdfLogoSize) }

    var headerBlocksOrder by remember(savedSettings) {
        mutableStateOf(PdfGenerator.HeaderBlockType.fromOrderString(savedSettings.pdfHeaderBlocksOrder))
    }
    var pdfShowSlogan by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowSlogan) }
    var pdfShowAddress by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowAddress) }
    var pdfShowPhone by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowPhone) }
    var pdfShowEmailWebsite by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowEmailWebsite) }
    var pdfShowFiscalIds by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowFiscalIds) }
    var pdfShowBankRib by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowBankRib) }

    // Devis / Bon / Facture column order & options
    val initialDocCols = remember(savedSettings) {
        val active = PdfGenerator.DocPdfColumn.fromOrderString(savedSettings.pdfDocColumnsOrder)
        val remaining = PdfGenerator.DocPdfColumn.entries.filter { !active.contains(it) }
        active + remaining
    }
    var orderedDocColumns by remember(savedSettings) { mutableStateOf(initialDocCols) }
    var selectedDocColumns by remember(savedSettings) {
        mutableStateOf(PdfGenerator.DocPdfColumn.fromOrderString(savedSettings.pdfDocColumnsOrder).toSet())
    }
    var pdfShowPartnerFiscal by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowPartnerFiscal) }
    var pdfShowPaymentMethod by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowPaymentMethod) }
    var pdfShowKitComponents by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowKitComponents) }
    var pdfShowNotes by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowNotes) }
    var pdfShowConversionNote by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowConversionNote) }
    var pdfShowAmountInWords by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowAmountInWords) }
    var pdfShowSignatures by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowSignatures) }
    var pdfShowStampAndSignature by remember(savedSettings) { mutableStateOf(savedSettings.pdfShowStampAndSignature) }
    var pdfStampScale by remember(savedSettings) { mutableIntStateOf(savedSettings.pdfStampScale) }

    // Créances column order & options
    val initialCreanceCols = remember(savedSettings) {
        val active = PdfGenerator.CreancePdfColumn.fromOrderString(savedSettings.pdfCreanceColumnsOrder)
        val remaining = PdfGenerator.CreancePdfColumn.entries.filter { !active.contains(it) }
        active + remaining
    }
    var orderedCreanceColumns by remember(savedSettings) { mutableStateOf(initialCreanceCols) }
    var selectedCreanceColumns by remember(savedSettings) {
        mutableStateOf(PdfGenerator.CreancePdfColumn.fromOrderString(savedSettings.pdfCreanceColumnsOrder).toSet())
    }
    var pdfCreanceShowSummary by remember(savedSettings) { mutableStateOf(savedSettings.pdfCreanceShowSummary) }
    var creancePdfMode by remember { mutableStateOf("GLOBAL") } // "GLOBAL" or "PARTNER"
    var selectedCreancePartnerId by remember { mutableStateOf<String?>(null) }
    var creanceFilterMode by remember { mutableStateOf("DEBITEURS") } // "DEBITEURS", "TOUS_CLIENTS", "CLIENTS_ET_FOURN"
    var creanceSortMode by remember { mutableStateOf("SOLDE_DESC") } // "SOLDE_DESC", "NOM_ASC", "FACTURE_DESC"

    // Catalogue Articles column order & options
    val availableArticleColumns = remember(isAdminUser) {
        PdfGenerator.ArticlePdfColumn.entries.filter {
            isAdminUser || it != PdfGenerator.ArticlePdfColumn.PRIX_ACHAT
        }
    }
    val initialArticleCols = remember(savedSettings, availableArticleColumns) {
        val active = PdfGenerator.ArticlePdfColumn.fromOrderString(savedSettings.pdfArticleColumnsOrder)
            .filter { availableArticleColumns.contains(it) }
        val remaining = availableArticleColumns.filter { !active.contains(it) }
        active + remaining
    }
    var orderedArticleColumns by remember(savedSettings, isAdminUser) { mutableStateOf(initialArticleCols) }
    var selectedArticleColumns by remember(savedSettings, isAdminUser) {
        mutableStateOf(
            PdfGenerator.ArticlePdfColumn.fromOrderString(savedSettings.pdfArticleColumnsOrder)
                .filter { availableArticleColumns.contains(it) }
                .toSet()
        )
    }
    var pdfCatalogShowSummary by remember(savedSettings) { mutableStateOf(savedSettings.pdfCatalogShowSummary) }
    var catalogCategoryFilter by remember { mutableStateOf("Toutes") }
    var catalogStockFilter by remember { mutableStateOf("TOUS") } // "TOUS", "EN_STOCK", "ALERTE_RUPTURE"
    var catalogSortMode by remember { mutableStateOf("FAMILLE_NOM") } // "FAMILLE_NOM", "NOM_ASC", "PRIX_VENTE_DESC", "STOCK_DESC"

    var showCoordinatesEditor by remember { mutableStateOf(false) }

    // Selected operation for Devis / Bon / Règlement / Facture preview & export
    val matchingOperations = remember(allOperations, selectedTab) {
        allOperations.filter { item ->
            when (selectedTab) {
                PdfStudioTab.DEVIS -> item.operation.typeOperation == OperationType.DEVIS_CLIENT ||
                    item.operation.typeOperation == OperationType.DEVIS_FOURNISSEUR
                PdfStudioTab.BON -> item.operation.typeOperation == OperationType.BL_CLIENT ||
                    item.operation.typeOperation == OperationType.BON_COMMANDE ||
                    item.operation.typeOperation == OperationType.BL_FOURNISSEUR ||
                    item.operation.typeOperation == OperationType.REGLEMENT
                PdfStudioTab.FACTURE -> item.operation.typeOperation == OperationType.FACTURE ||
                    item.operation.typeOperation == OperationType.AVOIR_CLIENT ||
                    item.operation.typeOperation == OperationType.AVOIR_FOURNISSEUR
                else -> false
            }
        }
    }
    var selectedOperationId by remember(matchingOperations, selectedTab) {
        mutableStateOf(matchingOperations.firstOrNull()?.operation?.id)
    }

    val activeDocColsList = remember(orderedDocColumns, selectedDocColumns) {
        orderedDocColumns.filter { selectedDocColumns.contains(it) }.ifEmpty { listOf(PdfGenerator.DocPdfColumn.DESIGNATION) }
    }
    val activeCreanceColsList = remember(orderedCreanceColumns, selectedCreanceColumns) {
        orderedCreanceColumns.filter { selectedCreanceColumns.contains(it) }.ifEmpty { listOf(PdfGenerator.CreancePdfColumn.CLIENT, PdfGenerator.CreancePdfColumn.CREANCE_DUE) }
    }
    val activeArticleColsList = remember(orderedArticleColumns, selectedArticleColumns, isAdminUser) {
        orderedArticleColumns
            .filter { selectedArticleColumns.contains(it) && (isAdminUser || it != PdfGenerator.ArticlePdfColumn.PRIX_ACHAT) }
            .ifEmpty { listOf(PdfGenerator.ArticlePdfColumn.DESIGNATION) }
    }

    // Live effective CompanySettings reflecting all current UI controls
    val liveSettings = remember(
        savedSettings, companyName, activitySlogan, address, phone, phoneSecondary, email, website,
        rc, articleImposition, nif, nis, nin, bankName, bankRib, pdfCustomFooter,
        pdfShowLogo, pdfLogoPosition, pdfLogoSize, headerBlocksOrder,
        pdfShowSlogan, pdfShowAddress, pdfShowPhone, pdfShowEmailWebsite, pdfShowFiscalIds, pdfShowBankRib,
        activeDocColsList, pdfShowPartnerFiscal, pdfShowPaymentMethod, pdfShowKitComponents,
        pdfShowNotes, pdfShowConversionNote, pdfShowAmountInWords, pdfShowSignatures, pdfShowStampAndSignature, pdfStampScale,
        activeCreanceColsList, pdfCreanceShowSummary,
        activeArticleColsList, pdfCatalogShowSummary
    ) {
        savedSettings.copy(
            companyName = companyName.trim().ifEmpty { "GESTION MERÉLEC" },
            activitySlogan = activitySlogan.trim(),
            address = address.trim(),
            phone = phone.trim(),
            phoneSecondary = phoneSecondary.trim(),
            email = email.trim(),
            website = website.trim(),
            rc = rc.trim(),
            articleImposition = articleImposition.trim(),
            nif = nif.trim(),
            nis = nis.trim(),
            nin = nin.trim(),
            bankName = bankName.trim(),
            bankRib = bankRib.trim(),
            pdfCustomFooter = pdfCustomFooter.trim(),
            pdfShowLogo = pdfShowLogo,
            pdfLogoPosition = pdfLogoPosition,
            pdfLogoSize = pdfLogoSize,
            pdfHeaderBlocksOrder = headerBlocksOrder.joinToString(",") { it.name },
            pdfShowSlogan = pdfShowSlogan,
            pdfShowAddress = pdfShowAddress,
            pdfShowPhone = pdfShowPhone,
            pdfShowEmailWebsite = pdfShowEmailWebsite,
            pdfShowFiscalIds = pdfShowFiscalIds,
            pdfShowBankRib = pdfShowBankRib,
            pdfDocColumnsOrder = activeDocColsList.joinToString(",") { it.name },
            pdfShowPartnerFiscal = pdfShowPartnerFiscal,
            pdfShowPaymentMethod = pdfShowPaymentMethod,
            pdfShowKitComponents = pdfShowKitComponents,
            pdfShowNotes = pdfShowNotes,
            pdfShowConversionNote = pdfShowConversionNote,
            pdfShowAmountInWords = pdfShowAmountInWords,
            pdfShowSignatures = pdfShowSignatures,
            pdfShowStampAndSignature = pdfShowStampAndSignature,
            pdfStampScale = pdfStampScale,
            pdfCreanceColumnsOrder = activeCreanceColsList.joinToString(",") { it.name },
            pdfCreanceShowSummary = pdfCreanceShowSummary,
            pdfArticleColumnsOrder = activeArticleColsList.joinToString(",") { it.name },
            pdfCatalogShowSummary = pdfCatalogShowSummary
        )
    }

    val kitCompositionsMap = remember(compositeArticles) {
        compositeArticles
            .filter { it.product.isArticleKit }
            .associate { it.product.name.trim().lowercase(Locale.FRENCH) to it.composantes }
    }
    val productCatalogMap = remember(allProducts) {
        allProducts.associateBy { it.name.trim().lowercase(Locale.FRENCH) }
    }

    // Generate the live PDF File based on the selected tab and options
    val generatedPdfFile = remember(
        selectedTab,
        liveSettings,
        logoVersion,
        stampVersion,
        selectedOperationId,
        matchingOperations,
        allProducts,
        catalogCategoryFilter,
        catalogStockFilter,
        catalogSortMode,
        clientBalances,
        supplierBalances,
        creancesSummary,
        creancePdfMode,
        selectedCreancePartnerId,
        creanceFilterMode,
        creanceSortMode
    ) {
        when (selectedTab) {
            PdfStudioTab.DEVIS, PdfStudioTab.BON, PdfStudioTab.FACTURE -> {
                val targetOp = matchingOperations.find { it.operation.id == selectedOperationId }
                    ?: matchingOperations.firstOrNull()
                    ?: buildSampleOperationForTab(selectedTab, liveSettings)
                val partner = (clients + suppliers).find {
                    it.name.trim().equals(targetOp.operation.clientFournisseur.trim(), ignoreCase = true)
                }
                PdfGenerator.generatePdf(
                    context = context,
                    item = targetOp,
                    settings = liveSettings,
                    partner = partner,
                    kitCompositionsMap = kitCompositionsMap,
                    forcePrintKitComponents = liveSettings.pdfShowKitComponents,
                    selectedColumns = activeDocColsList,
                    productCatalogMap = productCatalogMap
                )
            }
            PdfStudioTab.CREANCE -> {
                val allPartners = clients + suppliers
                val chosenPartner = allPartners.find { it.id == selectedCreancePartnerId } ?: clients.firstOrNull()
                if (creancePdfMode == "PARTNER" && chosenPartner != null) {
                    val partnerOps = allOperations.filter {
                        it.operation.clientFournisseur.trim().equals(chosenPartner.name.trim(), ignoreCase = true)
                    }
                    val matchedBalance = (clientBalances + supplierBalances).find { it.partner.id == chosenPartner.id }
                    val totalBlOps = partnerOps.filter { it.operation.typeOperation != OperationType.REGLEMENT }.sumOf { it.totalTtc }
                    val totalRegOps = partnerOps.sumOf { it.operation.montantVerse }
                    val effectiveTotalBl = matchedBalance?.totalTransactions ?: totalBlOps
                    val effectiveTotalReg = matchedBalance?.totalReglements ?: totalRegOps
                    val effectiveSoldeRestant = matchedBalance?.soldeRestant
                        ?: (chosenPartner.solde.coerceAtLeast(0.0) + totalBlOps - totalRegOps).coerceAtLeast(0.0)
                    PdfGenerator.generatePartnerStatementPdf(
                        context = context,
                        partner = chosenPartner,
                        operations = partnerOps,
                        totalBl = effectiveTotalBl,
                        totalReglements = effectiveTotalReg,
                        soldeRestantDu = effectiveSoldeRestant,
                        settings = liveSettings
                    )
                } else {
                    val baseList = when (creanceFilterMode) {
                        "TOUS_CLIENTS" -> clientBalances
                        "CLIENTS_ET_FOURN" -> clientBalances + supplierBalances
                        else -> clientBalances.filter { it.soldeRestant > 0.01 }
                    }
                    val sortedList = when (creanceSortMode) {
                        "NOM_ASC" -> baseList.sortedBy { it.partner.name.lowercase(Locale.FRENCH) }
                        "FACTURE_DESC" -> baseList.sortedByDescending { it.totalTransactions }
                        else -> baseList.sortedByDescending { it.soldeRestant }
                    }
                    val finalList = sortedList.ifEmpty {
                        listOf(
                            PartnerBalanceItem(
                                partner = PartnerEntity(
                                    id = "SPECIMEN-CLI",
                                    name = "SARL ÉLECTRO DISTRIBUTION (Spécimen)",
                                    type = PartnerType.CLIENT,
                                    phone = "0550 11 22 33",
                                    address = "Zone Industrielle Rouiba, Alger",
                                    rc = "16/00-987654B16",
                                    nif = "000116098765432"
                                ),
                                totalTransactions = 485000.0,
                                totalReglements = 300000.0,
                                soldeRestant = 185000.0,
                                countOperations = 4
                            )
                        )
                    }
                    PdfGenerator.generateGlobalCreancesPdf(
                        context = context,
                        clientsBalances = finalList,
                        summary = creancesSummary,
                        settings = liveSettings,
                        selectedColumns = activeCreanceColsList
                    )
                }
            }
            PdfStudioTab.CATALOGUE -> {
                val filtered = allProducts.filter { prod ->
                    val catOk = catalogCategoryFilter == "Toutes" || prod.category.equals(catalogCategoryFilter, ignoreCase = true)
                    val stockOk = when (catalogStockFilter) {
                        "EN_STOCK" -> prod.stockActuel > 0.0
                        "ALERTE_RUPTURE" -> prod.stockActuel <= prod.seuilAlerte
                        else -> true
                    }
                    catOk && stockOk
                }
                val sorted = when (catalogSortMode) {
                    "NOM_ASC" -> filtered.sortedBy { it.name.lowercase(Locale.FRENCH) }
                    "PRIX_VENTE_DESC" -> filtered.sortedByDescending { it.prixVente ?: it.defaultUnitPrice ?: 0.0 }
                    "STOCK_DESC" -> filtered.sortedByDescending { it.stockActuel }
                    else -> filtered.sortedWith(compareBy({ it.category }, { it.name }))
                }
                PdfGenerator.generateArticlesCatalogPdf(
                    context = context,
                    products = sorted,
                    settings = liveSettings,
                    selectedColumns = activeArticleColsList
                )
            }
        }
    }

    val previewBitmap = remember(generatedPdfFile.absolutePath, generatedPdfFile.lastModified(), liveSettings, logoVersion, stampVersion) {
        PdfGenerator.renderPdfPreviewBitmap(generatedPdfFile)
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri: Uri? ->
        if (destUri != null) {
            val ok = PdfGenerator.savePdfToUri(context, generatedPdfFile, destUri)
            Toast.makeText(
                context,
                if (ok) "PDF enregistré avec succès" else "Erreur lors de l'enregistrement du PDF",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val categories = remember(allProducts) {
        listOf("Toutes") + allProducts.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("pdf_studio_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Studio & Réorganisation PDF",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Logo, coordonnées & options : Devis, Bon, Facture, Créance, Catalogue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.updateCompanySettings(liveSettings)
                            Toast.makeText(context, "Mise en page & coordonnées PDF enregistrées !", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("pdf_studio_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrer", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Sélecteur du Type de PDF (Devis, Bon, Facture, Créance, Catalogue Article)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "1. Choisissez le type de document PDF à organiser ou créer :",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PdfStudioTab.entries.forEach { tab ->
                                val isSelected = selectedTab == tab
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (tab) {
                                                PdfStudioTab.DEVIS -> Icons.Default.Description
                                                PdfStudioTab.BON -> Icons.Default.ReceiptLong
                                                PdfStudioTab.FACTURE -> Icons.Default.Receipt
                                                PdfStudioTab.CREANCE -> Icons.Default.Business
                                                PdfStudioTab.CATALOGUE -> Icons.Default.Inventory2
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("pdf_studio_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // 2. Carte Logo & Réorganisation des Coordonnées de l'Entreprise
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pdf_studio_logo_coords_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "2. Mon Logo & Organisation de mes Coordonnées",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "S'applique aux Devis, Bons, Factures, Créances et Catalogue PDF",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = pdfShowLogo,
                                onCheckedChange = { pdfShowLogo = it },
                                modifier = Modifier.testTag("pdf_studio_show_logo_switch")
                            )
                        }

                        // Logo Picker + Position + Size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (customBitmap != null) {
                                    Image(
                                        bitmap = customBitmap,
                                        contentDescription = "Logo personnalisé",
                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.logo_merelec),
                                        contentDescription = "Logo par défaut",
                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("pdf_studio_pick_logo_btn")
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ajouter / Changer mon Logo", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (customBitmap != null) {
                                        OutlinedButton(
                                            onClick = { viewModel.resetToDefaultLogo(context) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                // Position du logo
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    Text("Position :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    listOf("LEFT" to "Gauche", "CENTER" to "Centré", "RIGHT" to "Droite").forEach { (pos, label) ->
                                        FilterChip(
                                            selected = pdfLogoPosition == pos,
                                            onClick = { pdfLogoPosition = pos },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                // Taille du logo
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    Text("Taille :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    listOf(38 to "Petit", 52 to "Moyen", 68 to "Grand").forEach { (sz, label) ->
                                        FilterChip(
                                            selected = pdfLogoSize == sz,
                                            onClick = { pdfLogoSize = sz },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // Cachet Humide HD en format PNG avec agrandissement (sans signature stylo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mon Cachet Humide HD (Format PNG) — Taille : ${pdfStampScale}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Apposé sur vos Devis, Factures, Bons (BL, BC, BR) et Règlements (sans signature stylo)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = pdfShowStampAndSignature,
                                onCheckedChange = {
                                    pdfShowStampAndSignature = it
                                    if (it) pdfShowSignatures = true
                                },
                                modifier = Modifier.testTag("pdf_studio_stamp_signature_switch")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(74.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (stampBitmap != null) {
                                        Image(
                                            bitmap = stampBitmap,
                                            contentDescription = "Cachet humide PNG HD",
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }

                                // Contrôle d'agrandissement du Cachet Humide sur les Bons / Devis / Factures
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Agrandir le Cachet Humide : ${pdfStampScale}%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { pdfStampScale = (pdfStampScale - 15).coerceAtLeast(80) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp).testTag("pdf_studio_stamp_scale_minus_btn")
                                        ) {
                                            Icon(Icons.Default.ZoomOut, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("-15%", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Button(
                                            onClick = { pdfStampScale = (pdfStampScale + 15).coerceAtMost(250) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp).testTag("pdf_studio_stamp_scale_plus_btn")
                                        ) {
                                            Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("+15%", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        100 to "100%",
                                        130 to "130%",
                                        160 to "160%",
                                        200 to "200%",
                                        225 to "225%",
                                        250 to "250%"
                                    ).forEach { (scaleVal, label) ->
                                        FilterChip(
                                            selected = pdfStampScale == scaleVal,
                                            onClick = { pdfStampScale = scaleVal },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.testTag("pdf_studio_stamp_zoom_${scaleVal}")
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            stampPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).testTag("pdf_studio_pick_stamp_btn")
                                    ) {
                                        Text("Changer mon Cachet PNG HD", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.resetStampToDefaultPng(context) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = "Par défaut", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Par défaut", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // Réorganisation de l'ordre des blocs de coordonnées (↑ / ↓)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ordre & visibilité de mes coordonnées dans l'en-tête PDF :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    headerBlocksOrder = PdfGenerator.HeaderBlockType.entries.toList()
                                    pdfShowSlogan = true
                                    pdfShowAddress = true
                                    pdfShowPhone = true
                                    pdfShowEmailWebsite = true
                                    pdfShowFiscalIds = true
                                    pdfShowBankRib = true
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Réinitialiser", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            headerBlocksOrder.forEachIndexed { idx, block ->
                                val isBlockEnabled = when (block) {
                                    PdfGenerator.HeaderBlockType.SLOGAN -> pdfShowSlogan
                                    PdfGenerator.HeaderBlockType.CONTACT -> pdfShowAddress || pdfShowPhone || pdfShowEmailWebsite
                                    PdfGenerator.HeaderBlockType.FISCAL -> pdfShowFiscalIds
                                    PdfGenerator.HeaderBlockType.BANQUE -> pdfShowBankRib
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isBlockEnabled) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = isBlockEnabled,
                                                onCheckedChange = { checked ->
                                                    when (block) {
                                                        PdfGenerator.HeaderBlockType.SLOGAN -> pdfShowSlogan = checked
                                                        PdfGenerator.HeaderBlockType.CONTACT -> {
                                                            pdfShowAddress = checked
                                                            pdfShowPhone = checked
                                                            pdfShowEmailWebsite = checked
                                                        }
                                                        PdfGenerator.HeaderBlockType.FISCAL -> pdfShowFiscalIds = checked
                                                        PdfGenerator.HeaderBlockType.BANQUE -> pdfShowBankRib = checked
                                                    }
                                                }
                                            )
                                            Column {
                                                Text(
                                                    text = "${idx + 1}. ${block.label}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = block.description,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (idx > 0) {
                                                        val mutable = headerBlocksOrder.toMutableList()
                                                        val tmp = mutable[idx - 1]
                                                        mutable[idx - 1] = mutable[idx]
                                                        mutable[idx] = tmp
                                                        headerBlocksOrder = mutable
                                                    }
                                                },
                                                enabled = idx > 0,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Monter", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (idx < headerBlocksOrder.lastIndex) {
                                                        val mutable = headerBlocksOrder.toMutableList()
                                                        val tmp = mutable[idx + 1]
                                                        mutable[idx + 1] = mutable[idx]
                                                        mutable[idx] = tmp
                                                        headerBlocksOrder = mutable
                                                    }
                                                },
                                                enabled = idx < headerBlocksOrder.lastIndex,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Filtres fins pour Adresse / Tél / Email
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = pdfShowAddress,
                                onClick = { pdfShowAddress = !pdfShowAddress },
                                label = { Text("Adresse", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = pdfShowPhone,
                                onClick = { pdfShowPhone = !pdfShowPhone },
                                label = { Text("Téléphones", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = pdfShowEmailWebsite,
                                onClick = { pdfShowEmailWebsite = !pdfShowEmailWebsite },
                                label = { Text("Email & Web", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = pdfShowFiscalIds,
                                onClick = { pdfShowFiscalIds = !pdfShowFiscalIds },
                                label = { Text("RC / NIF / NIS / AI", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = pdfShowBankRib,
                                onClick = { pdfShowBankRib = !pdfShowBankRib },
                                label = { Text("Banque & RIB", style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        // Bouton pour déplier/replier l'édition du texte des coordonnées
                        OutlinedButton(
                            onClick = { showCoordinatesEditor = !showCoordinatesEditor },
                            modifier = Modifier.fillMaxWidth().testTag("pdf_studio_toggle_coords_edit_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showCoordinatesEditor) "Masquer la saisie des coordonnées" else "Modifier mes coordonnées (Nom, Adresse, Tél, RC, NIF, RIB...)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showCoordinatesEditor) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = showCoordinatesEditor) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = companyName,
                                    onValueChange = { companyName = it },
                                    label = { Text("Nom / Raison Sociale de l'entreprise") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = activitySlogan,
                                    onValueChange = { activitySlogan = it },
                                    label = { Text("Slogan / Activité") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    label = { Text("Adresse complète") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it },
                                        label = { Text("Téléphone 1") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = phoneSecondary,
                                        onValueChange = { phoneSecondary = it },
                                        label = { Text("Téléphone 2") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = website,
                                        onValueChange = { website = it },
                                        label = { Text("Site Web") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = rc,
                                        onValueChange = { rc = it },
                                        label = { Text("RC N°") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = articleImposition,
                                        onValueChange = { articleImposition = it },
                                        label = { Text("N° Article (AI)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = nif,
                                        onValueChange = { nif = it },
                                        label = { Text("NIF N°") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = nis,
                                        onValueChange = { nis = it },
                                        label = { Text("NIS N°") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = nin,
                                    onValueChange = { nin = it },
                                    label = { Text("NIN N°") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = bankName,
                                        onValueChange = { bankName = it },
                                        label = { Text("Banque") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = bankRib,
                                        onValueChange = { bankRib = it },
                                        label = { Text("RIB Bancaire") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = pdfCustomFooter,
                                    onValueChange = { pdfCustomFooter = it },
                                    label = { Text("Mention personnalisée de pied de page PDF (facultatif)") },
                                    placeholder = { Text("Ex: Garantie 12 mois • Marchandise non reprise après 48h") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // 3. Options de Création & Réorganisation des Colonnes selon l'onglet sélectionné
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pdf_studio_options_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {
                            PdfStudioTab.DEVIS, PdfStudioTab.BON, PdfStudioTab.FACTURE -> {
                                Text(
                                    text = "3. Création & Organisation PDF : ${selectedTab.label}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Boutons de création directe d'un nouveau document
                                Text(
                                    text = "Créer un nouveau document avec cette mise en page :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (selectedTab) {
                                        PdfStudioTab.DEVIS -> {
                                            Button(
                                                onClick = {
                                                    viewModel.updateCompanySettings(liveSettings)
                                                    viewModel.startNewOperation(OperationType.DEVIS_CLIENT)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("pdf_studio_create_devis_client_btn")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Nouveau Devis Client")
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.updateCompanySettings(liveSettings)
                                                    viewModel.startNewOperation(OperationType.DEVIS_FOURNISSEUR)
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Nouveau Devis Fournisseur")
                                            }
                                        }
                                        PdfStudioTab.BON -> {
                                            Button(
                                                onClick = {
                                                    viewModel.updateCompanySettings(liveSettings)
                                                    viewModel.startNewOperation(OperationType.BL_CLIENT)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("pdf_studio_create_bl_btn")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Nouveau Bon de Livraison (BL)")
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.updateCompanySettings(liveSettings)
                                                    viewModel.startNewOperation(OperationType.REGLEMENT)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("pdf_studio_create_reglement_from_bon_btn")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Nouveau Règlement")
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.updateCompanySettings(liveSettings)
                                                    viewModel.startNewOperation(OperationType.BON_COMMANDE)
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Nouveau Bon de Commande (BC)")
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.updateCompanySettings(liveSettings)
                                                    viewModel.startNewOperation(OperationType.BL_FOURNISSEUR)
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Bon Réception Fournisseur")
                                            }
                                        }
                                        PdfStudioTab.FACTURE -> {
                                            Button(
                                                onClick = {
                                                    viewModel.updateCompanySettings(liveSettings)
                                                    viewModel.startNewOperation(OperationType.FACTURE)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("pdf_studio_create_facture_btn")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Nouvelle Facture")
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.updateCompanySettings(liveSettings)
                                                    viewModel.startNewOperation(OperationType.AVOIR_CLIENT)
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Nouvelle Facture d'Avoir")
                                            }
                                        }
                                        else -> {}
                                    }
                                }

                                // Sélection d'un document existant à générer en PDF
                                if (matchingOperations.isNotEmpty()) {
                                    Text(
                                        text = "Ou choisissez un document existant (${matchingOperations.size}) pour générer son PDF :",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        matchingOperations.take(15).forEach { opItem ->
                                            val isChosen = selectedOperationId == opItem.operation.id
                                            FilterChip(
                                                selected = isChosen,
                                                onClick = { selectedOperationId = opItem.operation.id },
                                                label = {
                                                    Text(
                                                        text = "${opItem.operation.id} • ${opItem.operation.clientFournisseur.take(16)} (${CurrencyUtils.formatDaSmart(opItem.totalTtc)})",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Aucun document enregistré dans cette catégorie — un spécimen est affiché dans l'aperçu PDF ci-dessous.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider()

                                // Réorganisation des colonnes du tableau PDF (↑ / ↓ + Checkbox)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Réorganiser les colonnes du tableau (↑ / ↓) :",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    TextButton(
                                        onClick = {
                                            orderedDocColumns = PdfGenerator.DocPdfColumn.entries.toList()
                                            selectedDocColumns = PdfGenerator.DocPdfColumn.defaultColumns().toSet()
                                        }
                                    ) {
                                        Text("Par défaut", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    orderedDocColumns.forEachIndexed { idx, col ->
                                        val isChecked = selectedDocColumns.contains(col)
                                        ReorderableColumnRow(
                                            index = idx,
                                            totalCount = orderedDocColumns.size,
                                            label = col.label,
                                            shortHeader = col.shortHeader,
                                            isChecked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedDocColumns = if (checked) {
                                                    selectedDocColumns + col
                                                } else if (selectedDocColumns.size > 1) {
                                                    selectedDocColumns - col
                                                } else selectedDocColumns
                                            },
                                            onMoveUp = {
                                                if (idx > 0) {
                                                    val m = orderedDocColumns.toMutableList()
                                                    val tmp = m[idx - 1]
                                                    m[idx - 1] = m[idx]
                                                    m[idx] = tmp
                                                    orderedDocColumns = m
                                                }
                                            },
                                            onMoveDown = {
                                                if (idx < orderedDocColumns.lastIndex) {
                                                    val m = orderedDocColumns.toMutableList()
                                                    val tmp = m[idx + 1]
                                                    m[idx + 1] = m[idx]
                                                    m[idx] = tmp
                                                    orderedDocColumns = m
                                                }
                                            }
                                        )
                                    }
                                }

                                HorizontalDivider()

                                // Options de sections du document
                                Text(
                                    text = "Options d'affichage sur le PDF :",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = pdfShowPartnerFiscal,
                                        onClick = { pdfShowPartnerFiscal = !pdfShowPartnerFiscal },
                                        label = { Text("NIF/RC du Client", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = pdfShowPaymentMethod,
                                        onClick = { pdfShowPaymentMethod = !pdfShowPaymentMethod },
                                        label = { Text("Mode de Règlement", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = pdfShowKitComponents,
                                        onClick = { pdfShowKitComponents = !pdfShowKitComponents },
                                        label = { Text("Détail Composants Kit", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = pdfShowNotes,
                                        onClick = { pdfShowNotes = !pdfShowNotes },
                                        label = { Text("Encadré Notes", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = pdfShowConversionNote,
                                        onClick = { pdfShowConversionNote = !pdfShowConversionNote },
                                        label = { Text("Note « Document converti N° »", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = pdfShowAmountInWords,
                                        onClick = { pdfShowAmountInWords = !pdfShowAmountInWords },
                                        label = { Text("Somme en Lettres", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = pdfShowSignatures,
                                        onClick = { pdfShowSignatures = !pdfShowSignatures },
                                        label = { Text("Cadre Signatures", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = pdfShowStampAndSignature,
                                        onClick = {
                                            pdfShowStampAndSignature = !pdfShowStampAndSignature
                                            if (pdfShowStampAndSignature) pdfShowSignatures = true
                                        },
                                        label = { Text("Cachet Humide HD (${pdfStampScale}%)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            PdfStudioTab.CREANCE -> {
                                Text(
                                    text = "3. Création & Organisation PDF : Créances & Relevés",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.updateCompanySettings(liveSettings)
                                            viewModel.startNewOperation(OperationType.REGLEMENT)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("pdf_studio_new_reglement_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nouveau Règlement Client")
                                    }
                                }

                                // Choix du type de PDF Créance : État Global vs Relevé Individuel
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = creancePdfMode == "GLOBAL",
                                        onClick = { creancePdfMode = "GLOBAL" },
                                        label = { Text("État Global des Créances") }
                                    )
                                    FilterChip(
                                        selected = creancePdfMode == "PARTNER",
                                        onClick = { creancePdfMode = "PARTNER" },
                                        label = { Text("Relevé d'un Partenaire") }
                                    )
                                }

                                if (creancePdfMode == "PARTNER") {
                                    val allPartners = clients + suppliers
                                    if (allPartners.isNotEmpty()) {
                                        Text(
                                            text = "Sélectionnez le Client ou Fournisseur :",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            allPartners.forEach { p ->
                                                FilterChip(
                                                    selected = selectedCreancePartnerId == p.id || (selectedCreancePartnerId == null && p == allPartners.first()),
                                                    onClick = { selectedCreancePartnerId = p.id },
                                                    label = { Text(p.name, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Filtres & Tri de l'état global des créances
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = creanceFilterMode == "DEBITEURS",
                                            onClick = { creanceFilterMode = "DEBITEURS" },
                                            label = { Text("Créances > 0 DA", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = creanceFilterMode == "TOUS_CLIENTS",
                                            onClick = { creanceFilterMode = "TOUS_CLIENTS" },
                                            label = { Text("Tous les Clients", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = creanceFilterMode == "CLIENTS_ET_FOURN",
                                            onClick = { creanceFilterMode = "CLIENTS_ET_FOURN" },
                                            label = { Text("Clients & Fournisseurs", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Tri :", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterVertically))
                                        FilterChip(
                                            selected = creanceSortMode == "SOLDE_DESC",
                                            onClick = { creanceSortMode = "SOLDE_DESC" },
                                            label = { Text("Créance décroissante", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = creanceSortMode == "NOM_ASC",
                                            onClick = { creanceSortMode = "NOM_ASC" },
                                            label = { Text("Nom A-Z", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = creanceSortMode == "FACTURE_DESC",
                                            onClick = { creanceSortMode = "FACTURE_DESC" },
                                            label = { Text("Facturé décroissant", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = pdfCreanceShowSummary,
                                            onClick = { pdfCreanceShowSummary = !pdfCreanceShowSummary },
                                            label = { Text("Cartes Synthèse", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }

                                    HorizontalDivider()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Réorganiser les colonnes du PDF Créances (↑ / ↓) :",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        TextButton(
                                            onClick = {
                                                orderedCreanceColumns = PdfGenerator.CreancePdfColumn.entries.toList()
                                                selectedCreanceColumns = PdfGenerator.CreancePdfColumn.defaultColumns().toSet()
                                            }
                                        ) {
                                            Text("Par défaut", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        orderedCreanceColumns.forEachIndexed { idx, col ->
                                            val isChecked = selectedCreanceColumns.contains(col)
                                            ReorderableColumnRow(
                                                index = idx,
                                                totalCount = orderedCreanceColumns.size,
                                                label = col.label,
                                                shortHeader = col.shortHeader,
                                                isChecked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedCreanceColumns = if (checked) {
                                                        selectedCreanceColumns + col
                                                    } else if (selectedCreanceColumns.size > 1) {
                                                        selectedCreanceColumns - col
                                                    } else selectedCreanceColumns
                                                },
                                                onMoveUp = {
                                                    if (idx > 0) {
                                                        val m = orderedCreanceColumns.toMutableList()
                                                        val tmp = m[idx - 1]
                                                        m[idx - 1] = m[idx]
                                                        m[idx] = tmp
                                                        orderedCreanceColumns = m
                                                    }
                                                },
                                                onMoveDown = {
                                                    if (idx < orderedCreanceColumns.lastIndex) {
                                                        val m = orderedCreanceColumns.toMutableList()
                                                        val tmp = m[idx + 1]
                                                        m[idx + 1] = m[idx]
                                                        m[idx] = tmp
                                                        orderedCreanceColumns = m
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            PdfStudioTab.CATALOGUE -> {
                                Text(
                                    text = "3. Création & Organisation PDF : Catalogue Articles (${allProducts.size} réf.)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Filtres par famille
                                Text(
                                    text = "Filtrer par Famille d'articles :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    categories.forEach { cat ->
                                        FilterChip(
                                            selected = catalogCategoryFilter == cat,
                                            onClick = { catalogCategoryFilter = cat },
                                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                // Filtre de stock & Tri
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = catalogStockFilter == "TOUS",
                                        onClick = { catalogStockFilter = "TOUS" },
                                        label = { Text("Tous les articles", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = catalogStockFilter == "EN_STOCK",
                                        onClick = { catalogStockFilter = "EN_STOCK" },
                                        label = { Text("En stock (> 0)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = catalogStockFilter == "ALERTE_RUPTURE",
                                        onClick = { catalogStockFilter = "ALERTE_RUPTURE" },
                                        label = { Text("Alertes & Ruptures", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = pdfCatalogShowSummary,
                                        onClick = { pdfCatalogShowSummary = !pdfCatalogShowSummary },
                                        label = { Text("Bandeau Valorisation", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Tri :", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterVertically))
                                    FilterChip(
                                        selected = catalogSortMode == "FAMILLE_NOM",
                                        onClick = { catalogSortMode = "FAMILLE_NOM" },
                                        label = { Text("Famille & Nom", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = catalogSortMode == "NOM_ASC",
                                        onClick = { catalogSortMode = "NOM_ASC" },
                                        label = { Text("Désignation A-Z", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = catalogSortMode == "PRIX_VENTE_DESC",
                                        onClick = { catalogSortMode = "PRIX_VENTE_DESC" },
                                        label = { Text("Prix de vente", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = catalogSortMode == "STOCK_DESC",
                                        onClick = { catalogSortMode = "STOCK_DESC" },
                                        label = { Text("Stock actuel", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Réorganiser les colonnes du Catalogue PDF (↑ / ↓) :",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TextButton(
                                            onClick = { selectedArticleColumns = availableArticleColumns.toSet() }
                                        ) {
                                            Text("Tout cocher", style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(
                                            onClick = {
                                                orderedArticleColumns = availableArticleColumns
                                                selectedArticleColumns = PdfGenerator.ArticlePdfColumn.defaultColumns()
                                                    .filter { availableArticleColumns.contains(it) }
                                                    .toSet()
                                            }
                                        ) {
                                            Text("Par défaut", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    orderedArticleColumns.forEachIndexed { idx, col ->
                                        val isChecked = selectedArticleColumns.contains(col)
                                        ReorderableColumnRow(
                                            index = idx,
                                            totalCount = orderedArticleColumns.size,
                                            label = col.label,
                                            shortHeader = col.shortHeader,
                                            isChecked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedArticleColumns = if (checked) {
                                                    selectedArticleColumns + col
                                                } else if (selectedArticleColumns.size > 1) {
                                                    selectedArticleColumns - col
                                                } else selectedArticleColumns
                                            },
                                            onMoveUp = {
                                                if (idx > 0) {
                                                    val m = orderedArticleColumns.toMutableList()
                                                    val tmp = m[idx - 1]
                                                    m[idx - 1] = m[idx]
                                                    m[idx] = tmp
                                                    orderedArticleColumns = m
                                                }
                                            },
                                            onMoveDown = {
                                                if (idx < orderedArticleColumns.lastIndex) {
                                                    val m = orderedArticleColumns.toMutableList()
                                                    val tmp = m[idx + 1]
                                                    m[idx + 1] = m[idx]
                                                    m[idx] = tmp
                                                    orderedArticleColumns = m
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Carte Aperçu A4 en Direct & Boutons de Génération PDF
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pdf_studio_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "4. Aperçu A4 en Direct & Génération PDF",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Fichier prêt : ${generatedPdfFile.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (previewBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        bitmap = previewBitmap.asImageBitmap(),
                                        contentDescription = "Aperçu A4 du PDF",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.updateCompanySettings(liveSettings)
                                PdfGenerator.printPdfA4(
                                    context = context,
                                    file = generatedPdfFile,
                                    jobName = "${selectedTab.label}_A4_${liveSettings.companyName}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier.fillMaxWidth().testTag("pdf_studio_print_a4_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Imprimer en Format A4 (${selectedTab.label})")
                        }

                        if (selectedTab == PdfStudioTab.BON && matchingOperations.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.updateCompanySettings(liveSettings)
                                    val allBonsFile = PdfGenerator.generateFullA4BonsBatchPdf(
                                        context = context,
                                        operations = matchingOperations,
                                        partners = clients + suppliers,
                                        kitCompositionsMap = kitCompositionsMap,
                                        productCatalogMap = productCatalogMap,
                                        settings = liveSettings
                                    )
                                    PdfGenerator.printPdfA4(
                                        context = context,
                                        file = allBonsFile,
                                        jobName = "Lot_Bons_A4_${matchingOperations.size}"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("pdf_studio_print_all_bons_a4_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Imprimer tous les Bons en A4 (${matchingOperations.size} bon(s) — 1 Bon/page)")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateCompanySettings(liveSettings)
                                    PdfGenerator.sharePdf(
                                        context,
                                        generatedPdfFile,
                                        "${selectedTab.label} - ${liveSettings.companyName}"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("pdf_studio_share_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Partager PDF")
                            }

                            FilledTonalButton(
                                onClick = {
                                    viewModel.updateCompanySettings(liveSettings)
                                    savePdfLauncher.launch(generatedPdfFile.name)
                                },
                                modifier = Modifier.weight(1f).testTag("pdf_studio_download_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddToDrive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enregistrer sous...")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.updateCompanySettings(liveSettings)
                                PdfGenerator.openPdf(context, generatedPdfFile)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("pdf_studio_open_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ouvrir le PDF en plein écran")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableColumnRow(
    index: Int,
    totalCount: Int,
    label: String,
    shortHeader: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isChecked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCheckedChange(!isChecked) }
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${index + 1}. $label",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = shortHeader,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Monter la colonne", modifier = Modifier.size(15.dp))
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre la colonne", modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

private fun buildSampleOperationForTab(
    tab: PdfStudioTab,
    settings: CompanySettings
): OperationWithLignes {
    val opType = when (tab) {
        PdfStudioTab.DEVIS -> OperationType.DEVIS_CLIENT
        PdfStudioTab.BON -> OperationType.BL_CLIENT
        PdfStudioTab.FACTURE -> OperationType.FACTURE
        else -> OperationType.FACTURE
    }
    val prefix = when (opType) {
        OperationType.DEVIS_CLIENT -> settings.prefixDevis
        OperationType.BL_CLIENT -> settings.prefixBonLivraison
        else -> settings.prefixFacture
    }
    val opId = "${prefix}2026-001"
    return OperationWithLignes(
        operation = OperationEntity(
            id = opId,
            dateTime = System.currentTimeMillis(),
            typeOperation = opType,
            clientFournisseur = "SARL ÉLECTRO PRO (Spécimen)",
            montantVerse = 15000.0,
            modeReglement = "Chèque / Virement",
            notes = "Livraison sur chantier • Validité de l'offre : 30 jours.",
            appliquerTva = settings.tvaEnabledByDefault,
            tauxTva = settings.defaultTvaRate,
            statut = "Validé"
        ),
        lignes = listOf(
            LigneOperationEntity(
                id = "L1",
                operationId = opId,
                produit = "Câble Cuivre 1*150 CU/PVC/PVC 0.6/1KV",
                quantite = 10.0,
                prixUnitaire = 3468.03
            ),
            LigneOperationEntity(
                id = "L2",
                operationId = opId,
                produit = "Disjoncteur Différentiel 4P 63A 300mA",
                quantite = 2.0,
                prixUnitaire = 12450.00
            ),
            LigneOperationEntity(
                id = "L3",
                operationId = opId,
                produit = "Coffret Étanche IP65 24 Modules",
                quantite = 1.0,
                prixUnitaire = 8900.00
            )
        )
    )
}
