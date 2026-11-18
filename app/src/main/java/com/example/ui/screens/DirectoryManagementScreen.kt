package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import com.example.data.model.ArticleCompositionEntity
import com.example.ui.components.OperationCard
import com.example.util.ArticleColumnSpec
import com.example.util.ArticleImportField
import com.example.util.BarcodeScannerHelper
import com.example.util.DateUtils
import com.example.util.PartnerColumnSpec
import com.example.util.PartnerImportField
import com.example.util.PdfGenerator
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeRole
import com.example.data.model.NomenclatureType
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import com.example.ui.theme.ClientColor
import com.example.ui.theme.FournisseurColor
import com.example.ui.theme.ReglementColor
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.CurrencyUtils
import com.example.util.ExportUtils
import com.example.util.ImportUtils
import androidx.compose.material.icons.filled.MoreVert
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryManagementScreen(
    viewModel: OperationViewModel,
    initialTab: Int = 0,
    showBackButton: Boolean = true,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    val products by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val allOperations by viewModel.allOperations.collectAsStateWithLifecycle()

    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val isAdminUser = currentEmployee.role == EmployeeRole.ADMIN ||
        currentEmployee.email.equals(EmployeeEntity.PRIMARY_ADMIN_EMAIL, ignoreCase = true)

    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isImportingFile by viewModel.isImportingFile.collectAsStateWithLifecycle()
    val lastImportSummary by viewModel.lastImportSummary.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExcelCsvImportHubDialog by remember { mutableStateOf(false) }
    var showAdminResetDbDialog by remember { mutableStateOf(false) }

    // Volet d'importation organisé (Articles / Clients / Fournisseurs) avec choix, noms et ordre des colonnes
    var showImportDrawerPanel by remember(initialTab) { mutableStateOf(false) }
    var articleImportColumns by remember { mutableStateOf(ImportUtils.defaultArticleColumns()) }
    var partnerImportColumns by remember { mutableStateOf(ImportUtils.defaultPartnerColumns()) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val isArticlesScreenMode = initialTab >= 2
    var selectedTabIndex by remember(initialTab) {
        mutableIntStateOf(if (isArticlesScreenMode) initialTab.coerceIn(2, 3) else initialTab.coerceIn(0, 1))
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var clientFilterDebtorsOnly by remember { mutableStateOf(false) }
    var showOnlyBonsDeLivraison by remember { mutableStateOf(false) }
    var productFilterOnlyKits by remember { mutableStateOf(false) }
    var productFilterOnlyArticleKits by remember { mutableStateOf(false) }
    var stockFilterOnlyAlerts by remember { mutableStateOf(false) }
    var pdfDialogPair by remember { mutableStateOf<Pair<OperationWithLignes, File>?>(null) }
    var blToConvertToFacture by remember { mutableStateOf<OperationWithLignes?>(null) }

    if (pdfDialogPair != null) {
        PdfDocumentActionDialog(
            item = pdfDialogPair!!.first,
            pdfFile = pdfDialogPair!!.second,
            onDismiss = { pdfDialogPair = null }
        )
    }

    if (blToConvertToFacture != null) {
        ConvertBlToFactureDialog(
            source = blToConvertToFacture!!,
            viewModel = viewModel,
            onDismiss = { blToConvertToFacture = null }
        )
    }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditPartnerDialog by remember { mutableStateOf<PartnerEntity?>(null) }
    var showEditProductDialog by remember { mutableStateOf<ProductCatalogEntity?>(null) }
    var partnerToDelete by remember { mutableStateOf<PartnerEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductCatalogEntity?>(null) }
    var operationToDelete by remember { mutableStateOf<OperationWithLignes?>(null) }
    var showKitCompositionDialog by remember { mutableStateOf<ProductCatalogEntity?>(null) }
    var showStockAdjustDialog by remember { mutableStateOf<ProductCatalogEntity?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Confirmation de suppression d'un Client ou Fournisseur
    if (partnerToDelete != null) {
        val target = partnerToDelete!!
        val typeLabel = if (target.type == PartnerType.CLIENT) "le client" else "le fournisseur"
        AlertDialog(
            onDismissRequest = { partnerToDelete = null },
            title = {
                Text(if (target.type == PartnerType.CLIENT) "Supprimer le client ?" else "Supprimer le fournisseur ?")
            },
            text = {
                Text("Confirmez-vous la suppression définitive de $typeLabel « ${target.name} » ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePartner(target.id)
                        partnerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_partner_btn")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { partnerToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirmation de suppression d'un Article
    if (productToDelete != null) {
        val target = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Supprimer l'article ?") },
            text = {
                Text("Confirmez-vous la suppression définitive de l'article « ${target.name} » du catalogue ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCatalogProduct(target.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_product_btn")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirmation de suppression d'un Bon / Opération
    if (operationToDelete != null) {
        val target = operationToDelete!!
        AlertDialog(
            onDismissRequest = { operationToDelete = null },
            title = { Text("Supprimer ${target.operation.typeOperation.label} ?") },
            text = {
                Text("Confirmez-vous la suppression définitive de « ${target.operation.id} » (${target.operation.clientFournisseur}) ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOperation(target.operation.id)
                        operationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_operation_btn")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { operationToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Fields for manual add
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var prixAchatInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") } // Prix de vente
    var categoryInput by remember { mutableStateOf("") } // Famille
    var marqueInput by remember { mutableStateOf("") } // Marque
    var unitInput by remember { mutableStateOf("U") }
    var stockInput by remember { mutableStateOf("0") }
    var seuilInput by remember { mutableStateOf("5") }
    var isKitInput by remember { mutableStateOf(false) }
    var isArticleKitInput by remember { mutableStateOf(false) }
    var notesInput by remember { mutableStateOf("") }

    // Coordonnées fiscales (RC N°, N° article, NIF N°, NIS N °, NIN N°, RIB)
    var rcInput by remember { mutableStateOf("") }
    var articleImpositionInput by remember { mutableStateOf("") }
    var nifInput by remember { mutableStateOf("") }
    var nisInput by remember { mutableStateOf("") }
    var ninInput by remember { mutableStateOf("") }
    var ribInput by remember { mutableStateOf("") }

    // Code-barre & Référence
    var barcodeInput by remember { mutableStateOf("") }
    var referenceInput by remember { mutableStateOf("") }

    // Dialog PDF Catalogue Articles avec choix des colonnes
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    var selectedPdfColumns by remember {
        mutableStateOf(PdfGenerator.ArticlePdfColumn.defaultColumns())
    }
    var generatedArticlesPdfFile by remember { mutableStateOf<File?>(null) }
    var showArticlesPdfDialog by remember { mutableStateOf(false) }

    // Ajout de colonne en plus dans le choix des colonnes lors de l'importation
    var showAddExtraImportColumnDialog by remember { mutableStateOf(false) }
    var extraColumnHeaderInput by remember { mutableStateOf("") }
    var selectedExtraArticleField by remember { mutableStateOf(ArticleImportField.COLONNE_PLUS) }
    var selectedExtraPartnerField by remember { mutableStateOf(PartnerImportField.COLONNE_PLUS) }

    BackHandler {
        if (showBackButton) {
            viewModel.navigateBack()
        } else {
            viewModel.selectBottomTab(com.example.ui.viewmodel.BottomNavTab.VENTES)
        }
    }

    val currentTabIndexState by rememberUpdatedState(selectedTabIndex)
    val currentArticleColumnsState by rememberUpdatedState(articleImportColumns)
    val currentPartnerColumnsState by rememberUpdatedState(partnerImportColumns)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (isArticlesScreenMode || currentTabIndexState >= 2) {
                viewModel.importProductsFromUri(context, it, currentArticleColumnsState)
            } else if (currentTabIndexState == 1) {
                viewModel.importPartnersFromUri(context, it, PartnerType.FOURNISSEUR, currentPartnerColumnsState)
            } else {
                viewModel.importPartnersFromUri(context, it, PartnerType.CLIENT, currentPartnerColumnsState)
            }
        }
    }

    val importProductsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importProductsFromUri(context, it, currentArticleColumnsState)
            showImportDrawerPanel = false
        }
    }

    val importClientsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (!isArticlesScreenMode) selectedTabIndex = 0
            viewModel.importPartnersFromUri(context, it, PartnerType.CLIENT, currentPartnerColumnsState)
            showImportDrawerPanel = false
        }
    }

    val importSuppliersLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (!isArticlesScreenMode) selectedTabIndex = 1
            viewModel.importPartnersFromUri(context, it, PartnerType.FOURNISSEUR, currentPartnerColumnsState)
            showImportDrawerPanel = false
        }
    }

    val importClientCreancesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (!isArticlesScreenMode) selectedTabIndex = 0
            viewModel.importCreancesFromUri(context, it, PartnerType.CLIENT)
            showImportDrawerPanel = false
        }
    }

    val importSupplierDettesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (!isArticlesScreenMode) selectedTabIndex = 1
            viewModel.importCreancesFromUri(context, it, PartnerType.FOURNISSEUR)
            showImportDrawerPanel = false
        }
    }

    if (showExcelCsvImportHubDialog) {
        AlertDialog(
            onDismissRequest = { showExcelCsvImportHubDialog = false },
            title = {
                Text(
                    text = "Importer Base de Données (Excel .xlsx / .xls & CSV)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Importez vos Articles, Clients, Fournisseurs et Créances depuis Excel (.xlsx / .xls) ou CSV. Vous pouvez aussi personnaliser et réorganiser les colonnes dans le volet d'importation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Section 1 : Articles (Stock)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "1. Articles & Stock (Collection 'products')",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Ordre configuré :\n${articleImportColumns.filter { it.enabled }.joinToString(" ; ") { it.customHeader }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showExcelCsvImportHubDialog = false
                                        importProductsLauncher.launch("*/*")
                                    },
                                    modifier = Modifier.weight(1f).testTag("hub_import_articles_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Importer Articles", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        ExportUtils.shareText(
                                            context,
                                            "Modele_Articles_Merelec.csv",
                                            ImportUtils.generateProductsTemplateCsv(articleImportColumns)
                                        )
                                    }
                                ) {
                                    Text("Modèle CSV", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Section 2 : Tiers (Clients & Fournisseurs)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "2. Tiers Clients / Fournisseurs (Collection 'partners')",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Ordre configuré :\n${partnerImportColumns.filter { it.enabled }.joinToString(" ; ") { it.customHeader }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showExcelCsvImportHubDialog = false
                                        importClientsLauncher.launch("*/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ClientColor),
                                    modifier = Modifier.weight(1f).testTag("hub_import_clients_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Importer Clients", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = {
                                        showExcelCsvImportHubDialog = false
                                        importSuppliersLauncher.launch("*/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FournisseurColor),
                                    modifier = Modifier.weight(1f).testTag("hub_import_suppliers_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fournisseurs", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            TextButton(
                                onClick = {
                                    ExportUtils.shareText(
                                        context,
                                        "Modele_Clients_Merelec.csv",
                                        ImportUtils.generatePartnersTemplateCsv(PartnerType.CLIENT, partnerImportColumns)
                                    )
                                }
                            ) {
                                Text("Télécharger / Partager le modèle CSV Clients", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Section 3 : Créances Clients & Dettes Fournisseurs
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "3. Créances & Dettes (Impayés Clients / Fournisseurs)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = "Colonnes attendues :\nnom ; type ; telephone ; reference ; montant_facture ; montant_verse ; reste_a_payer ; notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showExcelCsvImportHubDialog = false
                                        importClientCreancesLauncher.launch("*/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    modifier = Modifier.weight(1f).testTag("hub_import_creances_clients_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Créances Clients", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = {
                                        showExcelCsvImportHubDialog = false
                                        importSupplierDettesLauncher.launch("*/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                    modifier = Modifier.weight(1f).testTag("hub_import_dettes_suppliers_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dettes Fourn.", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            TextButton(
                                onClick = {
                                    ExportUtils.shareText(
                                        context,
                                        "Modele_Creances_Merelec.csv",
                                        ImportUtils.generateCreancesTemplateCsv(PartnerType.CLIENT)
                                    )
                                }
                            ) {
                                Text("Télécharger / Partager le modèle CSV Créances", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExcelCsvImportHubDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Modal Ajout Manuel (Client / Fournisseur / Article)
    if (showAddDialog) {
        val title = when (selectedTabIndex) {
            0 -> "Nouveau Client"
            1 -> "Nouveau Fournisseur"
            else -> "Nouvel Article au Catalogue"
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedTabIndex >= 2) {
                        FilledTonalButton(
                            onClick = {
                                showAddDialog = false
                                showImportDrawerPanel = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_dialog_open_import_articles_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Importer des articles (CSV / XLS / XLSX)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(if (selectedTabIndex >= 2) "Désignation de l'article *" else "Nom de l'entreprise / Contact *") },
                        modifier = Modifier.fillMaxWidth().testTag("add_dialog_name_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    if (selectedTabIndex >= 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = referenceInput,
                                onValueChange = { referenceInput = it },
                                label = { Text("Réf.") },
                                modifier = Modifier.weight(0.8f).testTag("add_dialog_ref_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = barcodeInput,
                                onValueChange = { barcodeInput = it },
                                label = { Text("Code-barres") },
                                placeholder = { Text("Scanner ou taper...") },
                                modifier = Modifier.weight(1.2f).testTag("add_dialog_barcode_input"),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            BarcodeScannerHelper.scanBarcode(
                                                context = context,
                                                onSuccess = { code -> barcodeInput = code },
                                                onError = { err -> viewModel.showMessage(err) }
                                            )
                                        },
                                        modifier = Modifier.testTag("add_dialog_scan_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Scanner Code-barre",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }

                        // Famille & Marque
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = categoryInput,
                                onValueChange = { categoryInput = it },
                                label = { Text("Famille") },
                                placeholder = { Text("Ex: Protection") },
                                modifier = Modifier.weight(1f).testTag("add_dialog_category_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = marqueInput,
                                onValueChange = { marqueInput = it },
                                label = { Text("Marque") },
                                placeholder = { Text("Ex: Schneider") },
                                modifier = Modifier.weight(1f).testTag("add_dialog_marque_input"),
                                singleLine = true
                            )
                        }

                        // Prix d'achat (réservé Admin) & Prix de vente
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isAdminUser) {
                                OutlinedTextField(
                                    value = prixAchatInput,
                                    onValueChange = { prixAchatInput = it },
                                    label = { Text("Prix d'achat") },
                                    modifier = Modifier.weight(1f).testTag("add_dialog_prix_achat_input"),
                                    singleLine = true,
                                    trailingIcon = { Text("DA", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 6.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                            OutlinedTextField(
                                value = priceInput,
                                onValueChange = { priceInput = it },
                                label = { Text("Prix de vente") },
                                modifier = Modifier.weight(1f).testTag("add_dialog_price_input"),
                                singleLine = true,
                                trailingIcon = { Text("DA", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 6.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = stockInput,
                                onValueChange = { stockInput = it },
                                label = { Text("Stock") },
                                modifier = Modifier.weight(1f).testTag("add_dialog_stock_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = unitInput,
                                onValueChange = { unitInput = it },
                                label = { Text("Unité") },
                                modifier = Modifier.weight(0.7f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = seuilInput,
                                onValueChange = { seuilInput = it },
                                label = { Text("Seuil alerte") },
                                modifier = Modifier.weight(0.9f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        Text(
                            text = "Type d'article / Nomenclature :",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Option 1: Article Standard
                            val isStandardSelected = !isArticleKitInput && !isKitInput
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isStandardSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isArticleKitInput = false
                                        isKitInput = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Article Standard",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Article simple sans nomenclature de sous-composants",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option 2: Article Kit (Nomenclature commerciale)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isArticleKitInput) Color(0xFF0D9488).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isArticleKitInput = true
                                        isKitInput = false
                                    }
                                    .testTag("add_dialog_article_kit_option")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🎁 Article Kit (Nomenclature commerciale)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isArticleKitInput) Color(0xFF0D9488) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "1 seule ligne parent affichée • Prix forfaitaire du pack • Déstocke automatiquement les composants en arrière-plan • Rigide (éclatement manuel requis)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option 3: Article Éclaté (ou Kit éclaté)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isKitInput) Color(0xFF7C3AED).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isKitInput = true
                                        isArticleKitInput = false
                                    }
                                    .testTag("add_dialog_is_kit_switch")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "⚡ Article Éclaté (ou Kit éclaté)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isKitInput) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Toutes les lignes des composants remplacent l'article parent dès sa sélection • Flexible à la volée • Prix = somme des composants",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Numéro de téléphone") },
                            modifier = Modifier.fillMaxWidth().testTag("add_dialog_phone_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text("Adresse / Ville / Zone") },
                            modifier = Modifier.fillMaxWidth().testTag("add_dialog_address_input"),
                            singleLine = true
                        )

                        // Coordonnées Fiscales (Algérie)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Coordonnées Fiscales & Bancaires",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rcInput,
                                onValueChange = { rcInput = it },
                                label = { Text("RC N°") },
                                modifier = Modifier.weight(1f).testTag("add_dialog_rc"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = articleImpositionInput,
                                onValueChange = { articleImpositionInput = it },
                                label = { Text("N° article") },
                                modifier = Modifier.weight(1f).testTag("add_dialog_ai"),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = nifInput,
                                onValueChange = { nifInput = it },
                                label = { Text("NIF N°") },
                                modifier = Modifier.weight(1f).testTag("add_dialog_nif"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = nisInput,
                                onValueChange = { nisInput = it },
                                label = { Text("NIS N °") },
                                modifier = Modifier.weight(1f).testTag("add_dialog_nis"),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = ninInput,
                            onValueChange = { ninInput = it },
                            label = { Text("NIN N° (Numéro d'Identification National)") },
                            placeholder = { Text("Ex: 109801600012345001") },
                            modifier = Modifier.fillMaxWidth().testTag("add_dialog_nin"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = ribInput,
                            onValueChange = { ribInput = it },
                            label = { Text("RIB") },
                            placeholder = { Text("Ex: 001 00234 1234567890 12") },
                            modifier = Modifier.fillMaxWidth().testTag("add_dialog_rib"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Remarques / Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            when (selectedTabIndex) {
                                0 -> viewModel.addSinglePartner(
                                    name = nameInput,
                                    type = PartnerType.CLIENT,
                                    phone = phoneInput,
                                    address = addressInput,
                                    rc = rcInput,
                                    articleImposition = articleImpositionInput,
                                    nif = nifInput,
                                    nis = nisInput,
                                    nin = ninInput,
                                    rib = ribInput,
                                    notes = notesInput
                                )
                                1 -> viewModel.addSinglePartner(
                                    name = nameInput,
                                    type = PartnerType.FOURNISSEUR,
                                    phone = phoneInput,
                                    address = addressInput,
                                    rc = rcInput,
                                    articleImposition = articleImpositionInput,
                                    nif = nifInput,
                                    nis = nisInput,
                                    nin = ninInput,
                                    rib = ribInput,
                                    notes = notesInput
                                )
                                else -> viewModel.addSingleProduct(
                                    name = nameInput,
                                    priceText = priceInput,
                                    category = categoryInput,
                                    famille = categoryInput,
                                    marque = marqueInput,
                                    prixAchatText = prixAchatInput,
                                    prixVenteText = priceInput,
                                    unit = unitInput,
                                    stock = CurrencyUtils.parseAmount(stockInput),
                                    seuilAlerte = CurrencyUtils.parseAmount(seuilInput),
                                    isArticleEclate = isKitInput,
                                    isArticleKit = isArticleKitInput,
                                    barcode = barcodeInput,
                                    reference = referenceInput
                                )
                            }
                            nameInput = ""
                            phoneInput = ""
                            addressInput = ""
                            prixAchatInput = ""
                            priceInput = ""
                            categoryInput = ""
                            marqueInput = ""
                            unitInput = "U"
                            stockInput = "0"
                            seuilInput = "5"
                            isKitInput = false
                            isArticleKitInput = false
                            notesInput = ""
                            rcInput = ""
                            articleImpositionInput = ""
                            nifInput = ""
                            nisInput = ""
                            ninInput = ""
                            ribInput = ""
                            barcodeInput = ""
                            referenceInput = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_item_btn")
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Modification Partenaire
    showEditPartnerDialog?.let { partner ->
        var editName by remember(partner) { mutableStateOf(partner.name) }
        var editPhone by remember(partner) { mutableStateOf(partner.phone) }
        var editAddress by remember(partner) { mutableStateOf(partner.address) }
        var editRc by remember(partner) { mutableStateOf(partner.rc) }
        var editArticleImposition by remember(partner) { mutableStateOf(partner.articleImposition) }
        var editNif by remember(partner) { mutableStateOf(partner.nif) }
        var editNis by remember(partner) { mutableStateOf(partner.nis) }
        var editNin by remember(partner) { mutableStateOf(partner.nin) }
        var editRib by remember(partner) { mutableStateOf(partner.rib) }
        var editNotes by remember(partner) { mutableStateOf(partner.notes) }

        AlertDialog(
            onDismissRequest = { showEditPartnerDialog = null },
            title = {
                Text(if (partner.type == PartnerType.CLIENT) "Modifier le Client" else "Modifier le Fournisseur")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nom de l'entreprise *") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_partner_name"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_partner_phone"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Adresse / Ville") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_partner_address"),
                        singleLine = true
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Coordonnées Fiscales & Bancaires",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editRc,
                            onValueChange = { editRc = it },
                            label = { Text("RC N°") },
                            modifier = Modifier.weight(1f).testTag("edit_partner_rc"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editArticleImposition,
                            onValueChange = { editArticleImposition = it },
                            label = { Text("N° article") },
                            modifier = Modifier.weight(1f).testTag("edit_partner_ai"),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editNif,
                            onValueChange = { editNif = it },
                            label = { Text("NIF N°") },
                            modifier = Modifier.weight(1f).testTag("edit_partner_nif"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editNis,
                            onValueChange = { editNis = it },
                            label = { Text("NIS N °") },
                            modifier = Modifier.weight(1f).testTag("edit_partner_nis"),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = editNin,
                        onValueChange = { editNin = it },
                        label = { Text("NIN N° (Numéro d'Identification National)") },
                        placeholder = { Text("Ex: 109801600012345001") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_partner_nin"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editRib,
                        onValueChange = { editRib = it },
                        label = { Text("RIB") },
                        placeholder = { Text("Ex: 001 00234 1234567890 12") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_partner_rib"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updatePartner(
                                partner.copy(
                                    name = editName.trim(),
                                    phone = editPhone.trim(),
                                    address = editAddress.trim(),
                                    rc = editRc.trim(),
                                    articleImposition = editArticleImposition.trim(),
                                    nif = editNif.trim(),
                                    nis = editNis.trim(),
                                    nin = editNin.trim(),
                                    rib = editRib.trim(),
                                    notes = editNotes.trim()
                                )
                            )
                            showEditPartnerDialog = null
                        }
                    },
                    modifier = Modifier.testTag("save_edit_partner_btn")
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPartnerDialog = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Modification Article
    showEditProductDialog?.let { prod ->
        var editName by remember(prod) { mutableStateOf(prod.name) }
        var editPrixAchat by remember(prod) { mutableStateOf(prod.prixAchatMoyen?.let { CurrencyUtils.formatQty(it) } ?: "") }
        var editPrice by remember(prod) { mutableStateOf(prod.defaultUnitPrice?.let { CurrencyUtils.formatQty(it) } ?: "") }
        var editCategory by remember(prod) { mutableStateOf(prod.category) }
        var editMarque by remember(prod) { mutableStateOf(prod.marque) }
        var editUnit by remember(prod) { mutableStateOf(prod.unit) }
        var editStock by remember(prod) { mutableStateOf(CurrencyUtils.formatQty(prod.stockActuel)) }
        var editSeuil by remember(prod) { mutableStateOf(CurrencyUtils.formatQty(prod.seuilAlerte)) }
        var editIsKit by remember(prod) { mutableStateOf(prod.isArticleEclate) }
        var editIsArticleKit by remember(prod) { mutableStateOf(prod.isArticleKit) }
        var editBarcode by remember(prod) { mutableStateOf(prod.barcode) }
        var editReference by remember(prod) { mutableStateOf(prod.reference) }

        AlertDialog(
            onDismissRequest = { showEditProductDialog = null },
            title = { Text("Modifier l'Article") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Désignation *") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_product_name"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editReference,
                            onValueChange = { editReference = it },
                            label = { Text("Réf.") },
                            modifier = Modifier.weight(0.8f).testTag("edit_product_ref"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editBarcode,
                            onValueChange = { editBarcode = it },
                            label = { Text("Code-barres") },
                            modifier = Modifier.weight(1.2f).testTag("edit_product_barcode"),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        BarcodeScannerHelper.scanBarcode(
                                            context = context,
                                            onSuccess = { code -> editBarcode = code },
                                            onError = { err -> viewModel.showMessage(err) }
                                        )
                                    },
                                    modifier = Modifier.testTag("edit_product_scan_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scanner", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }

                    // Famille & Marque
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("Famille") },
                            modifier = Modifier.weight(1f).testTag("edit_product_famille"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editMarque,
                            onValueChange = { editMarque = it },
                            label = { Text("Marque") },
                            modifier = Modifier.weight(1f).testTag("edit_product_marque"),
                            singleLine = true
                        )
                    }

                    // Prix d'achat (réservé Admin) & Prix de vente
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isAdminUser) {
                            OutlinedTextField(
                                value = editPrixAchat,
                                onValueChange = { editPrixAchat = it },
                                label = { Text("Prix d'achat") },
                                modifier = Modifier.weight(1f).testTag("edit_product_prix_achat"),
                                singleLine = true,
                                trailingIcon = { Text("DA", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 6.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        OutlinedTextField(
                            value = editPrice,
                            onValueChange = { editPrice = it },
                            label = { Text("Prix de vente") },
                            modifier = Modifier.weight(1f).testTag("edit_product_price"),
                            singleLine = true,
                            trailingIcon = { Text("DA", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 6.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editStock,
                            onValueChange = { editStock = it },
                            label = { Text("Stock") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = editUnit,
                            onValueChange = { editUnit = it },
                            label = { Text("Unité") },
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editSeuil,
                            onValueChange = { editSeuil = it },
                            label = { Text("Seuil d'alerte") },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    Text(
                        text = "Type de Nomenclature :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val isStandard = !editIsArticleKit && !editIsKit
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isStandard) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editIsArticleKit = false
                                    editIsKit = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("Article Standard", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text("Article simple unitaire", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (editIsArticleKit) Color(0xFF0D9488).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editIsArticleKit = true
                                    editIsKit = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    "🎁 Article Kit (Nomenclature commerciale)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (editIsArticleKit) Color(0xFF0D9488) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "1 seule ligne parent forfaitaire • Déstocke automatiquement les composants en arrière-plan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (editIsKit) Color(0xFF7C3AED).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editIsKit = true
                                    editIsArticleKit = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    "⚡ Article Éclaté (ou Kit éclaté)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (editIsKit) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Toutes les lignes des composants remplacent l'article parent dès sa sélection • Prix = somme des composants",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            val parsedPrixAchat = if (isAdminUser) {
                                if (editPrixAchat.isNotBlank()) CurrencyUtils.parseAmount(editPrixAchat) else null
                            } else {
                                prod.prixAchatMoyen
                            }
                            val parsedPrice = if (editPrice.isNotBlank()) CurrencyUtils.parseAmount(editPrice) else null
                            viewModel.updateCatalogProduct(
                                prod.copy(
                                    name = editName.trim(),
                                    prixAchatMoyen = parsedPrixAchat,
                                    defaultUnitPrice = parsedPrice,
                                    category = editCategory.trim().ifEmpty { "Général" },
                                    marque = editMarque.trim(),
                                    unit = editUnit.trim().ifEmpty { "U" },
                                    stockActuel = CurrencyUtils.parseAmount(editStock),
                                    seuilAlerte = CurrencyUtils.parseAmount(editSeuil),
                                    isArticleEclate = editIsKit,
                                    isArticleKit = editIsArticleKit,
                                    barcode = editBarcode.trim(),
                                    reference = editReference.trim()
                                )
                            )
                            showEditProductDialog = null
                        }
                    },
                    modifier = Modifier.testTag("save_edit_product_btn")
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProductDialog = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Composition d'Article Éclaté
    showKitCompositionDialog?.let { prod ->
        KitCompositionDialog(
            product = prod,
            viewModel = viewModel,
            allProducts = products,
            onDismiss = { showKitCompositionDialog = null }
        )
    }

    // Modal Ajustement Rapide du Stock
    showStockAdjustDialog?.let { prod ->
        StockAdjustDialog(
            product = prod,
            viewModel = viewModel,
            onDismiss = { showStockAdjustDialog = null }
        )
    }

    // Modal Coller Liste
    if (showPasteDialog) {
        val activeArticleHeader = articleImportColumns.filter { it.enabled }.joinToString(" ; ") { it.customHeader }
        val activePartnerHeader = partnerImportColumns.filter { it.enabled }.joinToString(" ; ") { it.customHeader }
        val targetLabel = when (selectedTabIndex) {
            0 -> "clients ($activePartnerHeader)"
            1 -> "fournisseurs ($activePartnerHeader)"
            else -> "articles ($activeArticleHeader)"
        }

        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Coller une liste CSV / Excel") },
            text = {
                Column {
                    Text(
                        text = "Ordre des colonnes configuré :\n$targetLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        placeholder = {
                            Text(
                                if (selectedTabIndex >= 2)
                                    "Disjoncteur 16A; REF-001; 613000123456; Protection; Schneider; 520; 680; 50; U; 5\nCâble 2.5mm; REF-002; 613000100025; Câblage; Elsewedy; 3800; 4500; 25; Rlx; 5"
                                else
                                    "SARL Batiment Alger; 0550123456; Rouiba; 16/00-1234567B19; 16151234567; 000116123456789; 16123456789; 109801600012345001; 00500100123456789012; Client; 0"
                            )
                        },
                        minLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteText.isNotBlank()) {
                            when (selectedTabIndex) {
                                0 -> viewModel.importPartners(pasteText, PartnerType.CLIENT, partnerImportColumns)
                                1 -> viewModel.importPartners(pasteText, PartnerType.FOURNISSEUR, partnerImportColumns)
                                else -> viewModel.importProducts(pasteText, articleImportColumns)
                            }
                            pasteText = ""
                            showPasteDialog = false
                            showImportDrawerPanel = false
                        }
                    }
                ) {
                    Text("Importer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Confirmation d'effacement
    if (showClearConfirmDialog) {
        val label = when (selectedTabIndex) {
            0 -> "tous les clients"
            1 -> "tous les fournisseurs"
            else -> "tous les articles"
        }
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Confirmer l'effacement") },
            text = { Text("Voulez-vous vraiment effacer $label ? Les bons existants ne seront pas modifiés.") },
            confirmButton = {
                Button(
                    onClick = {
                        when (selectedTabIndex) {
                            0 -> viewModel.clearPartners(PartnerType.CLIENT)
                            1 -> viewModel.clearPartners(PartnerType.FOURNISSEUR)
                            else -> viewModel.clearCatalogProducts()
                        }
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tout effacer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Remise à zéro de la Base de Données (Administrateur uniquement)
    if (showAdminResetDbDialog && isAdminUser) {
        AlertDialog(
            onDismissRequest = { showAdminResetDbDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Remise à zéro de la Base de Données (Admin)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Cette action est réservée à l'administrateur (${currentEmployee.email}). Choisissez le niveau de remise à zéro souhaité :",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "1. Remise à zéro complète (Toute la base)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Supprime tous les Articles, Clients, Fournisseurs, Bons de livraison, Factures, Créances et Mouvements de stock (local & Cloud Firestore).",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Button(
                                onClick = {
                                    viewModel.resetDatabaseForAdmin(onlyDirectory = false)
                                    showAdminResetDbDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().testTag("confirm_full_reset_db_btn")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remettre TOUTE la base à zéro")
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "2. Vider uniquement Articles & Clients/Fournisseurs",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Vide le catalogue Articles ainsi que la liste des Clients et Fournisseurs pour repartir d'une importation propre.",
                                style = MaterialTheme.typography.labelSmall
                            )
                            OutlinedButton(
                                onClick = {
                                    viewModel.resetDatabaseForAdmin(onlyDirectory = true)
                                    showAdminResetDbDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().testTag("confirm_directory_reset_db_btn")
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Vider Articles + Clients/Fourn.")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAdminResetDbDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal d'ajout de colonne en plus lors de l'importation (Articles ou Clients/Fournisseurs)
    if (showAddExtraImportColumnDialog) {
        val isArticleTab = isArticlesScreenMode || selectedTabIndex >= 2
        AlertDialog(
            onDismissRequest = { showAddExtraImportColumnDialog = false },
            modifier = Modifier.testTag("add_extra_import_column_dialog"),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ajouter une colonne en plus",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choisissez le type de donnée ou ajoutez une colonne libre supplémentaire dans l'ordre de votre fichier CSV / XLSX :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Type de champ associé :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isArticleTab) {
                            ArticleImportField.entries.forEach { field ->
                                val isSelected = selectedExtraArticleField == field
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedExtraArticleField = field
                                            if (extraColumnHeaderInput.isBlank()) {
                                                extraColumnHeaderInput = field.defaultHeader
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                selectedExtraArticleField = field
                                                if (extraColumnHeaderInput.isBlank()) {
                                                    extraColumnHeaderInput = field.defaultHeader
                                                }
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${field.displayName} (ex: ${field.defaultHeader})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            PartnerImportField.entries.forEach { field ->
                                val isSelected = selectedExtraPartnerField == field
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedExtraPartnerField = field
                                            if (extraColumnHeaderInput.isBlank()) {
                                                extraColumnHeaderInput = field.defaultHeader
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                selectedExtraPartnerField = field
                                                if (extraColumnHeaderInput.isBlank()) {
                                                    extraColumnHeaderInput = field.defaultHeader
                                                }
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${field.displayName} (ex: ${field.defaultHeader})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = extraColumnHeaderInput,
                        onValueChange = { extraColumnHeaderInput = it },
                        label = { Text("Nom de la colonne dans votre fichier") },
                        placeholder = { Text("Ex: Observation, Emplacement, Remise...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extra_column_header_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isArticleTab) {
                            val headerName = extraColumnHeaderInput.trim().ifBlank { selectedExtraArticleField.defaultHeader }
                            articleImportColumns = articleImportColumns + ArticleColumnSpec(
                                field = selectedExtraArticleField,
                                customHeader = headerName,
                                enabled = true,
                                isExtraColumn = true
                            )
                        } else {
                            val headerName = extraColumnHeaderInput.trim().ifBlank { selectedExtraPartnerField.defaultHeader }
                            partnerImportColumns = partnerImportColumns + PartnerColumnSpec(
                                field = selectedExtraPartnerField,
                                customHeader = headerName,
                                enabled = true,
                                isExtraColumn = true
                            )
                        }
                        extraColumnHeaderInput = ""
                        showAddExtraImportColumnDialog = false
                    },
                    modifier = Modifier.testTag("confirm_add_extra_column_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter la colonne")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExtraImportColumnDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Exportation Liste des Articles en PDF avec choix des colonnes
    if (showArticlesPdfDialog) {
        val articlesToExport = remember(products, searchQuery, selectedCategoryFilter) {
            products.filter { prod ->
                val matchesCat = selectedCategoryFilter == null || prod.category.equals(selectedCategoryFilter, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.reference.contains(searchQuery, ignoreCase = true) ||
                    prod.category.contains(searchQuery, ignoreCase = true) ||
                    prod.marque.contains(searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(searchQuery, ignoreCase = true)
                matchesCat && matchesSearch
            }
        }

        val availablePdfColumns = remember(isAdminUser) {
            PdfGenerator.ArticlePdfColumn.entries.filter {
                isAdminUser || it != PdfGenerator.ArticlePdfColumn.PRIX_ACHAT
            }
        }

        val currentPdfFile = remember(articlesToExport, selectedPdfColumns, companySettings, isAdminUser) {
            val activeCols = selectedPdfColumns
                .filter { isAdminUser || it != PdfGenerator.ArticlePdfColumn.PRIX_ACHAT }
                .ifEmpty { listOf(PdfGenerator.ArticlePdfColumn.DESIGNATION) }
            PdfGenerator.generateArticlesCatalogPdf(
                context = context,
                products = articlesToExport,
                settings = companySettings,
                selectedColumns = activeCols
            ).also { generatedArticlesPdfFile = it }
        }

        val previewBitmap = remember(currentPdfFile.absolutePath, currentPdfFile.lastModified()) {
            PdfGenerator.renderPdfPreviewBitmap(currentPdfFile)
        }

        val savePdfLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/pdf")
        ) { destUri: Uri? ->
            if (destUri != null) {
                val ok = PdfGenerator.savePdfToUri(context, currentPdfFile, destUri)
                Toast.makeText(
                    context,
                    if (ok) "Liste des articles PDF enregistrée avec succès" else "Erreur lors de l'enregistrement",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        AlertDialog(
            onDismissRequest = { showArticlesPdfDialog = false },
            modifier = Modifier.testTag("export_articles_pdf_dialog"),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Exporter Liste des Articles en PDF",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${articlesToExport.size} article(s) • Choisissez les colonnes à générer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sélection des colonnes à inclure dans le PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Colonnes à afficher dans le PDF :",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = { selectedPdfColumns = availablePdfColumns },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Tout cocher", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = {
                                    selectedPdfColumns = PdfGenerator.ArticlePdfColumn.defaultColumns()
                                        .filter { isAdminUser || it != PdfGenerator.ArticlePdfColumn.PRIX_ACHAT }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Par défaut", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Grille / Liste des colonnes disponibles
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availablePdfColumns.chunked(2).forEach { rowCols ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowCols.forEach { col ->
                                        val isChecked = selectedPdfColumns.contains(col)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isChecked) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    selectedPdfColumns = if (isChecked) {
                                                        if (selectedPdfColumns.size > 1) {
                                                            selectedPdfColumns - col
                                                        } else {
                                                            selectedPdfColumns
                                                        }
                                                    } else {
                                                        availablePdfColumns.filter {
                                                            it == col || selectedPdfColumns.contains(it)
                                                        }
                                                    }
                                                }
                                                .testTag("pdf_col_toggle_${col.name.lowercase()}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        selectedPdfColumns = if (!checked) {
                                                            if (selectedPdfColumns.size > 1) {
                                                                selectedPdfColumns - col
                                                            } else {
                                                                selectedPdfColumns
                                                            }
                                                        } else {
                                                            availablePdfColumns.filter {
                                                                it == col || selectedPdfColumns.contains(it)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = col.label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    if (rowCols.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Aperçu visuel de la page A4 générée
                    if (previewBitmap != null) {
                        Text(
                            text = "Aperçu du PDF (${selectedPdfColumns.size} colonnes) :",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Aperçu PDF",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    }

                    // Boutons d'actions PDF : Partager, Enregistrer, Ouvrir
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                PdfGenerator.sharePdf(
                                    context,
                                    currentPdfFile,
                                    "Liste des Articles - ${companySettings.companyName}"
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("share_articles_pdf_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Partager PDF", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        FilledTonalButton(
                            onClick = { savePdfLauncher.launch(currentPdfFile.name) },
                            modifier = Modifier.weight(1f).testTag("save_articles_pdf_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AddToDrive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Enregistrer", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    OutlinedButton(
                        onClick = { PdfGenerator.openPdf(context, currentPdfFile) },
                        modifier = Modifier.fillMaxWidth().testTag("open_articles_pdf_btn")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ouvrir dans le lecteur PDF")
                    }

                    FilledTonalButton(
                        onClick = {
                            showArticlesPdfDialog = false
                            viewModel.navigateTo(AppScreen.PdfStudio(4))
                        },
                        modifier = Modifier.fillMaxWidth().testTag("open_catalog_pdf_studio_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Réorganiser Logo, Coordonnées & Ordre des Colonnes PDF")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showArticlesPdfDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = when (selectedTabIndex) {
                    0 -> ClientColor
                    1 -> FournisseurColor
                    else -> MaterialTheme.colorScheme.primary
                },
                contentColor = Color.White,
                modifier = Modifier.testTag("management_fab_add")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barre d'actions directe SANS bandeau en haut : Menu/Retour + Importer CSV/XLSX + Modèle + Coller + Effacer + RAZ Admin
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.size(34.dp).testTag("management_back_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                } else if (onOpenDrawer != null) {
                    IconButton(
                        onClick = { onOpenDrawer.invoke() },
                        modifier = Modifier.size(34.dp).testTag("top_left_drawer_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu latéral (Dashboard & Configuration)",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (isArticlesScreenMode || selectedTabIndex >= 2) {
                    Button(
                        onClick = { showImportDrawerPanel = !showImportDrawerPanel },
                        modifier = Modifier.weight(1f).testTag("btn_import_articles"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showImportDrawerPanel) "Masquer volet import" else "Importer Articles (CSV / XLS)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                } else {
                    Button(
                        onClick = { showImportDrawerPanel = !showImportDrawerPanel },
                        modifier = Modifier.weight(1f).testTag("btn_import_clients"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTabIndex == 1) FournisseurColor else ClientColor
                        )
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showImportDrawerPanel) {
                                "Masquer volet import"
                            } else if (selectedTabIndex == 1) {
                                "Importer Fourn. (CSV / XLS)"
                            } else {
                                "Importer Clients (CSV / XLS)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val template = if (isArticlesScreenMode || selectedTabIndex >= 2)
                            ImportUtils.generateProductsTemplateCsv(articleImportColumns)
                        else
                            ImportUtils.generatePartnersTemplateCsv(
                                if (selectedTabIndex == 0) PartnerType.CLIENT else PartnerType.FOURNISSEUR,
                                partnerImportColumns
                            )

                        val title = if (isArticlesScreenMode || selectedTabIndex >= 2) "Modele_Articles_Merelec.csv" else "Modele_Partenaires_Merelec.csv"
                        ExportUtils.shareText(context, title, template)
                    },
                    modifier = Modifier.size(34.dp).testTag("management_download_template_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Modèle CSV selon colonnes",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showPasteDialog = true },
                    modifier = Modifier.size(34.dp).testTag("management_paste_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Coller texte",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.size(34.dp).testTag("management_clear_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Effacer la liste",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }

                if (isAdminUser) {
                    IconButton(
                        onClick = { showAdminResetDbDialog = true },
                        modifier = Modifier.size(34.dp).testTag("admin_reset_db_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Remise à zéro de la base de données (Administrateur)",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // VOLET D'IMPORTATION ORGANISÉ (CSV / XLS / XLSX) : Choix des colonnes, leurs noms et leur ordre
            if (showImportDrawerPanel) {
                val isArticleTab = isArticlesScreenMode || selectedTabIndex >= 2
                val accentColor = when (selectedTabIndex) {
                    0 -> ClientColor
                    1 -> FournisseurColor
                    else -> MaterialTheme.colorScheme.primary
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("organized_import_panel"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArticleTab) {
                                        "Volet d'Importation Articles (CSV / XLS / XLSX)"
                                    } else if (selectedTabIndex == 1) {
                                        "Volet d'Importation Fournisseurs (CSV / XLS / XLSX)"
                                    } else {
                                        "Volet d'Importation Clients (CSV / XLS / XLSX)"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                                Text(
                                    text = "Cochez les colonnes à importer, renommez-les si besoin et organisez leur ordre (▲ / ▼) :",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (isArticleTab) {
                                            articleImportColumns = ImportUtils.defaultArticleColumns()
                                        } else {
                                            partnerImportColumns = ImportUtils.defaultPartnerColumns()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp).testTag("reset_import_columns_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "Réinitialiser l'ordre par défaut",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showImportDrawerPanel = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Fermer le volet",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Aperçu en temps réel de l'ordre des colonnes sélectionné
                        val previewOrderText = if (isArticleTab) {
                            articleImportColumns.filter { it.enabled }.mapIndexed { idx, col -> "${idx + 1}. ${col.customHeader}" }.joinToString("  ;  ")
                        } else {
                            partnerImportColumns.filter { it.enabled }.mapIndexed { idx, col -> "${idx + 1}. ${col.customHeader}" }.joinToString("  ;  ")
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.10f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = "Ordre actif des colonnes (CSV / Excel) :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                                Text(
                                    text = previewOrderText.ifBlank { "Aucune colonne sélectionnée" },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Liste interactive des colonnes avec case à cocher, champ de nom personnalisable et flèches d'ordre
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isArticleTab) {
                                articleImportColumns.forEachIndexed { index, colSpec ->
                                    val enabledRank = if (colSpec.enabled) {
                                        articleImportColumns.take(index + 1).count { it.enabled }
                                    } else null

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (colSpec.enabled) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Checkbox(
                                                checked = colSpec.enabled,
                                                onCheckedChange = { checked ->
                                                    articleImportColumns = articleImportColumns.toMutableList().also {
                                                        it[index] = colSpec.copy(enabled = checked)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            )

                                            Surface(
                                                shape = CircleShape,
                                                color = if (enabledRank != null) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = enabledRank?.toString() ?: "-",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = colSpec.customHeader,
                                                onValueChange = { newHeader ->
                                                    articleImportColumns = articleImportColumns.toMutableList().also {
                                                        it[index] = colSpec.copy(customHeader = newHeader)
                                                    }
                                                },
                                                label = { Text(colSpec.field.displayName, style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (index > 0) {
                                                        articleImportColumns = articleImportColumns.toMutableList().also { list ->
                                                            val tmp = list[index - 1]
                                                            list[index - 1] = list[index]
                                                            list[index] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = index > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Monter la colonne",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    if (index < articleImportColumns.lastIndex) {
                                                        articleImportColumns = articleImportColumns.toMutableList().also { list ->
                                                            val tmp = list[index + 1]
                                                            list[index + 1] = list[index]
                                                            list[index] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = index < articleImportColumns.lastIndex,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Descendre la colonne",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            if (colSpec.isExtraColumn) {
                                                IconButton(
                                                    onClick = {
                                                        articleImportColumns = articleImportColumns.toMutableList().also { list ->
                                                            list.removeAt(index)
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Supprimer cette colonne ajoutée",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                partnerImportColumns.forEachIndexed { index, colSpec ->
                                    val enabledRank = if (colSpec.enabled) {
                                        partnerImportColumns.take(index + 1).count { it.enabled }
                                    } else null

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (colSpec.enabled) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Checkbox(
                                                checked = colSpec.enabled,
                                                onCheckedChange = { checked ->
                                                    partnerImportColumns = partnerImportColumns.toMutableList().also {
                                                        it[index] = colSpec.copy(enabled = checked)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            )

                                            Surface(
                                                shape = CircleShape,
                                                color = if (enabledRank != null) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = enabledRank?.toString() ?: "-",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = colSpec.customHeader,
                                                onValueChange = { newHeader ->
                                                    partnerImportColumns = partnerImportColumns.toMutableList().also {
                                                        it[index] = colSpec.copy(customHeader = newHeader)
                                                    }
                                                },
                                                label = { Text(colSpec.field.displayName, style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (index > 0) {
                                                        partnerImportColumns = partnerImportColumns.toMutableList().also { list ->
                                                            val tmp = list[index - 1]
                                                            list[index - 1] = list[index]
                                                            list[index] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = index > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Monter la colonne",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    if (index < partnerImportColumns.lastIndex) {
                                                        partnerImportColumns = partnerImportColumns.toMutableList().also { list ->
                                                            val tmp = list[index + 1]
                                                            list[index + 1] = list[index]
                                                            list[index] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = index < partnerImportColumns.lastIndex,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Descendre la colonne",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            if (colSpec.isExtraColumn) {
                                                IconButton(
                                                    onClick = {
                                                        partnerImportColumns = partnerImportColumns.toMutableList().also { list ->
                                                            list.removeAt(index)
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Supprimer cette colonne ajoutée",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bouton pour ajouter une colonne en plus dans les choix d'importation
                        OutlinedButton(
                            onClick = {
                                extraColumnHeaderInput = ""
                                selectedExtraArticleField = ArticleImportField.COLONNE_PLUS
                                selectedExtraPartnerField = PartnerImportField.COLONNE_PLUS
                                showAddExtraImportColumnDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_extra_import_column_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = accentColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ajouter une colonne en plus dans les choix",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }

                        // Boutons d'action du volet d'importation : Choisir Fichier CSV/XLSX, Coller Texte, Modèle CSV
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (isArticleTab) {
                                        importProductsLauncher.launch("*/*")
                                    } else if (selectedTabIndex == 1) {
                                        importSuppliersLauncher.launch("*/*")
                                    } else {
                                        importClientsLauncher.launch("*/*")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                modifier = Modifier.weight(1.3f).testTag("panel_pick_file_btn"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choisir fichier CSV / XLSX", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = { showPasteDialog = true },
                                modifier = Modifier.weight(0.9f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Coller", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isArticleTab) {
                                        ExportUtils.shareText(
                                            context,
                                            "Modele_Articles_Merelec.csv",
                                            ImportUtils.generateProductsTemplateCsv(articleImportColumns)
                                        )
                                    } else {
                                        val pType = if (selectedTabIndex == 1) PartnerType.FOURNISSEUR else PartnerType.CLIENT
                                        ExportUtils.shareText(
                                            context,
                                            "Modele_Partenaires_Merelec.csv",
                                            ImportUtils.generatePartnersTemplateCsv(pType, partnerImportColumns)
                                        )
                                    }
                                },
                                modifier = Modifier.weight(0.9f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Modèle CSV", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Retour visuel d'importation Excel / CSV vers Firestore (progression, compteur de succès ou erreur)
            if (isImportingFile) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Lecture du fichier Excel/CSV et synchronisation Firestore en cours...",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            lastImportSummary?.let { summary ->
                val isErr = summary.errorMessage != null
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isErr) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        Color(0xFF0D9488).copy(alpha = 0.14f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("import_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (isErr) {
                                Text(
                                    text = "Erreur d'importation (${summary.entityLabel})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = summary.errorMessage ?: "Format de fichier incorrect",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            } else {
                                Text(
                                    text = "Importation ${summary.entityLabel} réussie (${summary.fileFormat})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF0F766E)
                                )
                                Text(
                                    text = "${summary.totalImported} synchronisé(s) dans Firestore : ${summary.createdCount} créé(s), ${summary.updatedCount} mis à jour" +
                                            if (summary.skippedCount > 0) " (${summary.skippedCount} ligne(s) ignorée(s))" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.clearImportSummary() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Fermer le résumé",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Champ de recherche (permettant de filtrer instantanément les Bons de Livraison par nom de Client / Fournisseur)
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = {
                        Text(
                            when (selectedTabIndex) {
                                0 -> "Rechercher client & filtrer ses Bons de Livraison"
                                1 -> "Rechercher fournisseur & filtrer ses Bons de Livraison"
                                2 -> "Rechercher réf, désignation, famille ou marque"
                                else -> "Rechercher un stock d'article, famille ou marque"
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            when (selectedTabIndex) {
                                0 -> "Tapez le nom d'un client, N° BL, téléphone..."
                                1 -> "Tapez le nom d'un fournisseur, N° BL..."
                                2 -> "Réf, désignation, famille, marque..."
                                else -> "Rechercher un stock d'article..."
                            }
                        )
                    },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("management_search_clear_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("management_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Filtres secondaires & sélection rapide par nom de Client / Fournisseur
            if (selectedTabIndex == 0) {
                val allClientBlsCount = remember(allOperations) {
                    allOperations.count { it.operation.typeOperation == OperationType.BL_CLIENT }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !clientFilterDebtorsOnly && !showOnlyBonsDeLivraison,
                            onClick = {
                                selectedTabIndex = 0
                                clientFilterDebtorsOnly = false
                                showOnlyBonsDeLivraison = false
                            },
                            label = { Text("Clients (${clients.size})") },
                            modifier = Modifier.testTag("client_filter_all")
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                selectedTabIndex = 1
                                searchQuery = ""
                            },
                            label = { Text("Fournisseurs (${suppliers.size})") },
                            modifier = Modifier.testTag("switch_to_suppliers_chip")
                        )
                        FilterChip(
                            selected = showOnlyBonsDeLivraison,
                            onClick = { showOnlyBonsDeLivraison = !showOnlyBonsDeLivraison },
                            label = { Text("Bons de Livraison ($allClientBlsCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("client_filter_bls_only")
                        )
                        FilterChip(
                            selected = clientFilterDebtorsOnly,
                            onClick = { clientFilterDebtorsOnly = !clientFilterDebtorsOnly },
                            label = { Text("Avec reste à payer") },
                            modifier = Modifier.testTag("client_filter_debtors")
                        )
                    }

                    // Sélection rapide d'un nom de client spécifique pour filtrer ses BL en 1 clic
                    if (clients.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Client :",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            clients.forEach { client ->
                                val isSelectedClient = searchQuery.trim().equals(client.name.trim(), ignoreCase = true)
                                FilterChip(
                                    selected = isSelectedClient,
                                    onClick = {
                                        searchQuery = if (isSelectedClient) "" else client.name
                                    },
                                    label = { Text(client.name, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("quick_client_chip_${client.id}")
                                )
                            }
                        }
                    }
                }
            } else if (selectedTabIndex == 1) {
                val allSupplierBlsCount = remember(allOperations) {
                    allOperations.count { it.operation.typeOperation == OperationType.BL_FOURNISSEUR }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                selectedTabIndex = 0
                                searchQuery = ""
                            },
                            label = { Text("Clients (${clients.size})") },
                            modifier = Modifier.testTag("switch_to_clients_chip")
                        )
                        FilterChip(
                            selected = !showOnlyBonsDeLivraison,
                            onClick = { showOnlyBonsDeLivraison = false },
                            label = { Text("Fournisseurs (${suppliers.size})") },
                            modifier = Modifier.testTag("supplier_filter_all")
                        )
                        FilterChip(
                            selected = showOnlyBonsDeLivraison,
                            onClick = { showOnlyBonsDeLivraison = !showOnlyBonsDeLivraison },
                            label = { Text("Bons de Livraison ($allSupplierBlsCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("supplier_filter_bls_only")
                        )
                    }

                    // Sélection rapide d'un nom de fournisseur spécifique pour filtrer ses BL en 1 clic
                    if (suppliers.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fournisseur :",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            suppliers.forEach { supplier ->
                                val isSelectedSupplier = searchQuery.trim().equals(supplier.name.trim(), ignoreCase = true)
                                FilterChip(
                                    selected = isSelectedSupplier,
                                    onClick = {
                                        searchQuery = if (isSelectedSupplier) "" else supplier.name
                                    },
                                    label = { Text(supplier.name, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("quick_supplier_chip_${supplier.id}")
                                )
                            }
                        }
                    }
                }
            } else if (selectedTabIndex == 2) {
                // Filtre Articles & Kits
                val categories = remember(products) {
                    products.map { it.category }.distinct().filter { it.isNotBlank() }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !productFilterOnlyKits && !productFilterOnlyArticleKits && !stockFilterOnlyAlerts && selectedCategoryFilter == null,
                        onClick = {
                            productFilterOnlyKits = false
                            productFilterOnlyArticleKits = false
                            stockFilterOnlyAlerts = false
                            selectedCategoryFilter = null
                        },
                        label = { Text("Tous") }
                    )
                    FilterChip(
                        selected = productFilterOnlyArticleKits,
                        onClick = {
                            productFilterOnlyArticleKits = !productFilterOnlyArticleKits
                            if (productFilterOnlyArticleKits) productFilterOnlyKits = false
                        },
                        label = { Text("🎁 Articles Kits (${products.count { it.isArticleKit }})") },
                        modifier = Modifier.testTag("filter_chip_article_kits")
                    )
                    FilterChip(
                        selected = productFilterOnlyKits,
                        onClick = {
                            productFilterOnlyKits = !productFilterOnlyKits
                            if (productFilterOnlyKits) productFilterOnlyArticleKits = false
                        },
                        label = { Text("⚡ Articles Éclatés (${products.count { it.isArticleEclate }})") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("filter_chip_articles_eclates")
                    )
                    FilterChip(
                        selected = stockFilterOnlyAlerts,
                        onClick = { stockFilterOnlyAlerts = !stockFilterOnlyAlerts },
                        label = { Text("⚠️ Stock Faible (${products.count { it.stockActuel <= it.seuilAlerte }})") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                            },
                            label = { Text(cat) }
                        )
                    }
                }

                // Boutons rapides : Importer Articles (CSV / XLS), Exporter PDF, Nouveau Article Kit & Nouveau Article Éclaté
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showImportDrawerPanel = !showImportDrawerPanel },
                        modifier = Modifier.weight(1.1f).testTag("btn_quick_import_articles_row"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Importer CSV/XLS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }

                    FilledTonalButton(
                        onClick = { showArticlesPdfDialog = true },
                        modifier = Modifier.weight(0.9f).testTag("btn_export_articles_pdf"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Exporter liste des articles en PDF",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Exporter PDF",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            isArticleKitInput = true
                            isKitInput = false
                            categoryInput = "Kits & Packs"
                            unitInput = "Pack"
                            showAddDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("btn_quick_create_article_kit"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "+ Créer Article Kit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0D9488)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            isKitInput = true
                            isArticleKitInput = false
                            categoryInput = "Articles Éclatés"
                            unitInput = "Lot"
                            showAddDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("btn_quick_create_article_eclate"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "+ Créer Article Éclaté",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF7C3AED)
                        )
                    }
                }
            } else if (selectedTabIndex == 3) {
                // Filtre Vue Stock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !stockFilterOnlyAlerts && !productFilterOnlyKits,
                        onClick = {
                            stockFilterOnlyAlerts = false
                            productFilterOnlyKits = false
                        },
                        label = { Text("Tout le Stock (${products.size})") }
                    )
                    FilterChip(
                        selected = stockFilterOnlyAlerts,
                        onClick = {
                            stockFilterOnlyAlerts = !stockFilterOnlyAlerts
                            if (stockFilterOnlyAlerts) productFilterOnlyKits = false
                        },
                        label = { Text("⚠️ Alertes & Ruptures (${products.count { it.stockActuel <= it.seuilAlerte }})") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = productFilterOnlyArticleKits,
                        onClick = {
                            productFilterOnlyArticleKits = !productFilterOnlyArticleKits
                            if (productFilterOnlyArticleKits) {
                                productFilterOnlyKits = false
                                stockFilterOnlyAlerts = false
                            }
                        },
                        label = { Text("🎁 Articles Kits (${products.count { it.isArticleKit }})") }
                    )
                    FilterChip(
                        selected = productFilterOnlyKits,
                        onClick = {
                            productFilterOnlyKits = !productFilterOnlyKits
                            if (productFilterOnlyKits) {
                                productFilterOnlyArticleKits = false
                                stockFilterOnlyAlerts = false
                            }
                        },
                        label = { Text("⚡ Articles Éclatés (${products.count { it.isArticleEclate }})") }
                    )
                }
            }

            // Contenu de la liste selon l'onglet
            when (selectedTabIndex) {
                0 -> {
                    // Clients + Bons de Livraison Clients filtrés instantanément par nom de client
                    val qLower = searchQuery.trim().lowercase(Locale.FRENCH)
                    val filteredClientBls = remember(allOperations, qLower) {
                        allOperations.filter { item ->
                            item.operation.typeOperation == OperationType.BL_CLIENT && (
                                qLower.isBlank() ||
                                    item.operation.clientFournisseur.lowercase(Locale.FRENCH).contains(qLower) ||
                                    item.operation.id.lowercase(Locale.FRENCH).contains(qLower) ||
                                    item.lignes.any { l -> l.produit.lowercase(Locale.FRENCH).contains(qLower) }
                            )
                        }
                    }

                    val filteredClients = clients.filter { client ->
                        val opsForClient = allOperations.filter { op ->
                            op.operation.clientFournisseur.trim().lowercase(Locale.FRENCH) == client.name.trim().lowercase(Locale.FRENCH)
                        }
                        val hasMatchingBl = qLower.isNotBlank() && opsForClient.any { op ->
                            op.operation.typeOperation == OperationType.BL_CLIENT && (
                                op.operation.id.lowercase(Locale.FRENCH).contains(qLower) ||
                                    op.lignes.any { l -> l.produit.lowercase(Locale.FRENCH).contains(qLower) }
                            )
                        }
                        val matchesSearch = qLower.isBlank() ||
                                client.name.lowercase(Locale.FRENCH).contains(qLower) ||
                                client.phone.contains(qLower) ||
                                client.address.lowercase(Locale.FRENCH).contains(qLower) ||
                                hasMatchingBl

                        val totalBl = opsForClient.filter { op -> op.operation.typeOperation == OperationType.BL_CLIENT }.sumOf { op -> op.totalProduits }
                        val totalReg = opsForClient.filter { op -> op.operation.typeOperation == OperationType.REGLEMENT }.sumOf { op -> op.operation.montantVerse } +
                                opsForClient.filter { op -> op.operation.typeOperation == OperationType.BL_CLIENT }.sumOf { op -> op.operation.montantVerse }
                        val restant = (totalBl - totalReg).coerceAtLeast(0.0)

                        val matchesDebtors = !clientFilterDebtorsOnly || restant > 0.0
                        matchesSearch && matchesDebtors
                    }

                    ClientsListSection(
                        clients = filteredClients,
                        filteredBls = filteredClientBls,
                        searchQuery = searchQuery,
                        showOnlyBls = showOnlyBonsDeLivraison,
                        allOperations = allOperations,
                        onFilterByClientName = { name -> searchQuery = name },
                        onClientClick = { viewModel.navigateTo(AppScreen.PartnerDetail(it.id)) },
                        onOperationClick = { opId -> viewModel.navigateTo(AppScreen.Detail(opId)) },
                        onConvertBlToFacture = { blItem -> blToConvertToFacture = blItem },
                        onGeneratePdf = { blItem, partner ->
                            val file = PdfGenerator.generatePdf(context, blItem, partner = partner)
                            pdfDialogPair = blItem to file
                        },
                        onEdit = { showEditPartnerDialog = it },
                        onDelete = { partnerToDelete = it },
                        onDeleteOperation = { operationToDelete = it }
                    )
                }
                1 -> {
                    // Fournisseurs + Bons de Livraison Fournisseurs filtrés instantanément par nom de fournisseur
                    val qLower = searchQuery.trim().lowercase(Locale.FRENCH)
                    val filteredSupplierBls = remember(allOperations, qLower) {
                        allOperations.filter { item ->
                            item.operation.typeOperation == OperationType.BL_FOURNISSEUR && (
                                qLower.isBlank() ||
                                    item.operation.clientFournisseur.lowercase(Locale.FRENCH).contains(qLower) ||
                                    item.operation.id.lowercase(Locale.FRENCH).contains(qLower) ||
                                    item.lignes.any { l -> l.produit.lowercase(Locale.FRENCH).contains(qLower) }
                            )
                        }
                    }

                    val filteredSuppliers = suppliers.filter { supplier ->
                        val opsForSupplier = allOperations.filter { op ->
                            op.operation.clientFournisseur.trim().lowercase(Locale.FRENCH) == supplier.name.trim().lowercase(Locale.FRENCH)
                        }
                        val hasMatchingBl = qLower.isNotBlank() && opsForSupplier.any { op ->
                            op.operation.typeOperation == OperationType.BL_FOURNISSEUR && (
                                op.operation.id.lowercase(Locale.FRENCH).contains(qLower) ||
                                    op.lignes.any { l -> l.produit.lowercase(Locale.FRENCH).contains(qLower) }
                            )
                        }
                        qLower.isBlank() ||
                                supplier.name.lowercase(Locale.FRENCH).contains(qLower) ||
                                supplier.phone.contains(qLower) ||
                                supplier.address.lowercase(Locale.FRENCH).contains(qLower) ||
                                hasMatchingBl
                    }

                    SuppliersListSection(
                        suppliers = filteredSuppliers,
                        filteredBls = filteredSupplierBls,
                        searchQuery = searchQuery,
                        showOnlyBls = showOnlyBonsDeLivraison,
                        allOperations = allOperations,
                        onFilterBySupplierName = { name -> searchQuery = name },
                        onSupplierClick = { viewModel.navigateTo(AppScreen.PartnerDetail(it.id)) },
                        onOperationClick = { opId -> viewModel.navigateTo(AppScreen.Detail(opId)) },
                        onGeneratePdf = { blItem, partner ->
                            val file = PdfGenerator.generatePdf(context, blItem, partner = partner)
                            pdfDialogPair = blItem to file
                        },
                        onEdit = { showEditPartnerDialog = it },
                        onDelete = { partnerToDelete = it },
                        onDeleteOperation = { operationToDelete = it }
                    )
                }
                2 -> {
                    // Articles
                    val qProd = searchQuery.trim().lowercase(Locale.FRENCH)
                    val filteredProducts = products.filter {
                        val matchesSearch = qProd.isBlank() ||
                                it.name.lowercase(Locale.FRENCH).contains(qProd) ||
                                it.category.lowercase(Locale.FRENCH).contains(qProd) ||
                                it.marque.lowercase(Locale.FRENCH).contains(qProd) ||
                                it.reference.lowercase(Locale.FRENCH).contains(qProd) ||
                                it.barcode.lowercase(Locale.FRENCH).contains(qProd)

                        val matchesCategory = selectedCategoryFilter == null || it.category == selectedCategoryFilter
                        val matchesKit = !productFilterOnlyKits || it.isArticleEclate
                        val matchesArticleKit = !productFilterOnlyArticleKits || it.isArticleKit
                        val matchesStock = !stockFilterOnlyAlerts || it.stockActuel <= it.seuilAlerte
                        matchesSearch && matchesCategory && matchesKit && matchesArticleKit && matchesStock
                    }

                    ProductsListSection(
                        products = filteredProducts,
                        isAdmin = isAdminUser,
                        onEdit = { showEditProductDialog = it },
                        onDelete = { productToDelete = it },
                        onManageKit = { showKitCompositionDialog = it },
                        onAdjustStock = { showStockAdjustDialog = it },
                        onQuickDelta = { prod, delta -> viewModel.adjustStock(prod.id, delta) }
                    )
                }
                3 -> {
                    // Stock & Alertes
                    val qProd = searchQuery.trim().lowercase(Locale.FRENCH)
                    val filteredStockProducts = products.filter {
                        val matchesSearch = qProd.isBlank() ||
                                it.name.lowercase(Locale.FRENCH).contains(qProd) ||
                                it.category.lowercase(Locale.FRENCH).contains(qProd) ||
                                it.marque.lowercase(Locale.FRENCH).contains(qProd) ||
                                it.reference.lowercase(Locale.FRENCH).contains(qProd) ||
                                it.barcode.lowercase(Locale.FRENCH).contains(qProd)

                        val matchesCategory = selectedCategoryFilter == null || it.category == selectedCategoryFilter
                        val matchesKit = !productFilterOnlyKits || it.isArticleEclate
                        val matchesArticleKit = !productFilterOnlyArticleKits || it.isArticleKit
                        val matchesAlert = !stockFilterOnlyAlerts || it.stockActuel <= it.seuilAlerte
                        matchesSearch && matchesCategory && matchesKit && matchesArticleKit && matchesAlert
                    }

                    StockManagementSection(
                        allProducts = products,
                        products = filteredStockProducts,
                        isAdmin = isAdminUser,
                        onAdjustStock = { showStockAdjustDialog = it },
                        onQuickDelta = { prod, delta -> viewModel.adjustStock(prod.id, delta) },
                        onManageKit = { showKitCompositionDialog = it },
                        onAddProduct = { showAddDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientsListSection(
    clients: List<PartnerEntity>,
    filteredBls: List<OperationWithLignes>,
    searchQuery: String,
    showOnlyBls: Boolean,
    allOperations: List<OperationWithLignes>,
    onFilterByClientName: (String) -> Unit,
    onClientClick: (PartnerEntity) -> Unit,
    onOperationClick: (String) -> Unit,
    onConvertBlToFacture: (OperationWithLignes) -> Unit,
    onGeneratePdf: (OperationWithLignes, PartnerEntity?) -> Unit,
    onEdit: (PartnerEntity) -> Unit,
    onDelete: (PartnerEntity) -> Unit,
    onDeleteOperation: (OperationWithLignes) -> Unit = {}
) {
    if (clients.isEmpty() && filteredBls.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) "Aucun client ni Bon de Livraison trouvé pour « $searchQuery »" else "Aucun client trouvé",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section instantanée des Bons de Livraison filtrés (affichée dès qu'une recherche client est saisie ou que le filtre BL est actif)
            if (showOnlyBls || searchQuery.isNotBlank()) {
                item(key = "filtered_client_bls_header") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ClientColor.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = ClientColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank())
                                        "Bons de Livraison Client « $searchQuery » (${filteredBls.size})"
                                    else
                                        "Tous les Bons de Livraison Clients (${filteredBls.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ClientColor
                                )
                            }
                            if (filteredBls.isNotEmpty()) {
                                Text(
                                    text = CurrencyUtils.formatDaCompact(filteredBls.sumOf { it.totalTtc }),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ClientColor
                                )
                            }
                        }
                    }
                }

                if (filteredBls.isEmpty()) {
                    item(key = "no_client_bl_match") {
                        Text(
                            text = "Aucun Bon de Livraison trouvé pour ce client.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    items(filteredBls, key = { "bl_client_${it.operation.id}" }) { blItem ->
                        val matchedPartner = clients.find {
                            it.name.trim().equals(blItem.operation.clientFournisseur.trim(), ignoreCase = true)
                        }
                        OperationCard(
                            item = blItem,
                            onClick = { onOperationClick(blItem.operation.id) },
                            onConvertToFacture = { onConvertBlToFacture(blItem) },
                            onGeneratePdf = { onGeneratePdf(blItem, matchedPartner) },
                            onDelete = { onDeleteOperation(blItem) }
                        )
                    }
                }
            }

            // Liste des fiches Clients avec leurs Bons de Livraison associés
            if (!showOnlyBls) {
                items(clients, key = { it.id }) { client ->
                    val clientOps = remember(client.name, allOperations) {
                        val clientNameLower = client.name.trim().lowercase(Locale.FRENCH)
                        allOperations.filter {
                            it.operation.clientFournisseur.trim().lowercase(Locale.FRENCH) == clientNameLower
                        }
                    }

                    val clientBls = remember(clientOps) {
                        clientOps.filter { it.operation.typeOperation == OperationType.BL_CLIENT }
                    }
                    val totalBl = remember(clientBls) {
                        clientBls.sumOf { it.totalProduits }
                    }
                    val totalReglements = remember(clientOps, clientBls) {
                        val direct = clientOps.filter { it.operation.typeOperation == OperationType.REGLEMENT }.sumOf { it.operation.montantVerse }
                        val acomptes = clientBls.sumOf { it.operation.montantVerse }
                        direct + acomptes
                    }
                    val restant = (totalBl - totalReglements).coerceAtLeast(0.0)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClientClick(client) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(ClientColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = ClientColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = client.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (client.phone.isNotBlank() || client.address.isNotBlank()) {
                                            val sub = listOfNotNull(client.phone.ifBlank { null }, client.address.ifBlank { null }).joinToString(" • ")
                                            Text(
                                                text = sub,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        val fiscalSub = listOfNotNull(
                                            client.rc.ifBlank { null }?.let { "RC: $it" },
                                            client.nif.ifBlank { null }?.let { "NIF: $it" },
                                            client.nin.ifBlank { null }?.let { "NIN N°: $it" }
                                        ).joinToString(" • ")
                                        if (fiscalSub.isNotBlank()) {
                                            Text(
                                                text = fiscalSub,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (clientBls.isNotEmpty()) {
                                        TextButton(
                                            onClick = { onFilterByClientName(client.name) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("filter_bls_for_client_${client.id}")
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${clientBls.size} BL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                    IconButton(onClick = { onEdit(client) }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDelete(client) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Aperçu direct des Bons de Livraison associés à ce client
                            if (clientBls.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    clientBls.take(3).forEach { bl ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ClientColor.copy(alpha = 0.06f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onOperationClick(bl.operation.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "BL ${bl.operation.id} • ${DateUtils.formatDate(bl.operation.dateTime)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = ClientColor
                                                )
                                                Text(
                                                    text = CurrencyUtils.formatDa(bl.totalTtc),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Financial bar for this client
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total BL: ${CurrencyUtils.formatDaCompact(totalBl)} (${clientBls.size} BL / ${clientOps.size} op.)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (restant > 0.0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                    ) {
                                        Text(
                                            text = "Reste: ${CurrencyUtils.formatDaCompact(restant)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ReglementColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "À jour",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ReglementColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuppliersListSection(
    suppliers: List<PartnerEntity>,
    filteredBls: List<OperationWithLignes>,
    searchQuery: String,
    showOnlyBls: Boolean,
    allOperations: List<OperationWithLignes>,
    onFilterBySupplierName: (String) -> Unit,
    onSupplierClick: (PartnerEntity) -> Unit,
    onOperationClick: (String) -> Unit,
    onGeneratePdf: (OperationWithLignes, PartnerEntity?) -> Unit,
    onEdit: (PartnerEntity) -> Unit,
    onDelete: (PartnerEntity) -> Unit,
    onDeleteOperation: (OperationWithLignes) -> Unit = {}
) {
    if (suppliers.isEmpty() && filteredBls.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) "Aucun fournisseur ni Bon de Livraison trouvé pour « $searchQuery »" else "Aucun fournisseur trouvé",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section instantanée des Bons de Livraison Fournisseurs filtrés
            if (showOnlyBls || searchQuery.isNotBlank()) {
                item(key = "filtered_supplier_bls_header") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = FournisseurColor.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = FournisseurColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank())
                                        "Bons de Livraison Fournisseur « $searchQuery » (${filteredBls.size})"
                                    else
                                        "Tous les Bons de Livraison Fournisseurs (${filteredBls.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = FournisseurColor
                                )
                            }
                            if (filteredBls.isNotEmpty()) {
                                Text(
                                    text = CurrencyUtils.formatDaCompact(filteredBls.sumOf { it.totalTtc }),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = FournisseurColor
                                )
                            }
                        }
                    }
                }

                if (filteredBls.isEmpty()) {
                    item(key = "no_supplier_bl_match") {
                        Text(
                            text = "Aucun Bon de Livraison trouvé pour ce fournisseur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    items(filteredBls, key = { "bl_supplier_${it.operation.id}" }) { blItem ->
                        val matchedPartner = suppliers.find {
                            it.name.trim().equals(blItem.operation.clientFournisseur.trim(), ignoreCase = true)
                        }
                        OperationCard(
                            item = blItem,
                            onClick = { onOperationClick(blItem.operation.id) },
                            onGeneratePdf = { onGeneratePdf(blItem, matchedPartner) },
                            onDelete = { onDeleteOperation(blItem) }
                        )
                    }
                }
            }

            // Liste des fiches Fournisseurs avec leurs Bons de Livraison associés
            if (!showOnlyBls) {
                items(suppliers, key = { it.id }) { supplier ->
                    val supplierOps = remember(supplier.name, allOperations) {
                        val supplierNameLower = supplier.name.trim().lowercase(Locale.FRENCH)
                        allOperations.filter {
                            it.operation.clientFournisseur.trim().lowercase(Locale.FRENCH) == supplierNameLower
                        }
                    }

                    val supplierBls = remember(supplierOps) {
                        supplierOps.filter { it.operation.typeOperation == OperationType.BL_FOURNISSEUR }
                    }

                    val totalAchats = remember(supplierBls) {
                        supplierBls.sumOf { it.totalProduits }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSupplierClick(supplier) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(FournisseurColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = FournisseurColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = supplier.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (supplier.phone.isNotBlank() || supplier.address.isNotBlank()) {
                                            val sub = listOfNotNull(supplier.phone.ifBlank { null }, supplier.address.ifBlank { null }).joinToString(" • ")
                                            Text(
                                                text = sub,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        val supplierFiscalSub = listOfNotNull(
                                            supplier.rc.ifBlank { null }?.let { "RC: $it" },
                                            supplier.nif.ifBlank { null }?.let { "NIF: $it" },
                                            supplier.nin.ifBlank { null }?.let { "NIN N°: $it" }
                                        ).joinToString(" • ")
                                        if (supplierFiscalSub.isNotBlank()) {
                                            Text(
                                                text = supplierFiscalSub,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                            )
                                        }
                                        Text(
                                            text = "Total BL: ${CurrencyUtils.formatDaSmart(totalAchats)} (${supplierBls.size} BL / ${supplierOps.size} op.)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = FournisseurColor
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (supplierBls.isNotEmpty()) {
                                        TextButton(
                                            onClick = { onFilterBySupplierName(supplier.name) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("filter_bls_for_supplier_${supplier.id}")
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${supplierBls.size} BL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                    IconButton(onClick = { onEdit(supplier) }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDelete(supplier) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Aperçu direct des Bons de Livraison associés à ce fournisseur
                            if (supplierBls.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    supplierBls.take(3).forEach { bl ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = FournisseurColor.copy(alpha = 0.06f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onOperationClick(bl.operation.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "BL ${bl.operation.id} • ${DateUtils.formatDate(bl.operation.dateTime)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = FournisseurColor
                                                )
                                                Text(
                                                    text = CurrencyUtils.formatDa(bl.totalTtc),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductsListSection(
    products: List<ProductCatalogEntity>,
    isAdmin: Boolean,
    onEdit: (ProductCatalogEntity) -> Unit,
    onDelete: (ProductCatalogEntity) -> Unit,
    onManageKit: (ProductCatalogEntity) -> Unit,
    onAdjustStock: (ProductCatalogEntity) -> Unit,
    onQuickDelta: (ProductCatalogEntity, Double) -> Unit
) {
    if (products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aucun article dans le catalogue",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products, key = { it.id }) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (product.reference.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = product.reference,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (product.isArticleKit) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF0D9488).copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "🎁 Article Kit",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF0D9488),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else if (product.isArticleEclate) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF7C3AED).copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "⚡ Article Éclaté",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF7C3AED),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "Famille: ${product.category} • ${product.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (product.marque.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        ) {
                                            Text(
                                                text = "Marque: ${product.marque}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (product.defaultUnitPrice != null && product.defaultUnitPrice > 0) {
                                        Text(
                                            text = "Prix de vente: ${CurrencyUtils.formatDa(product.defaultUnitPrice)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (isAdmin && product.prixAchatMoyen != null && product.prixAchatMoyen > 0) {
                                        Text(
                                            text = "Prix d'achat: ${CurrencyUtils.formatDa(product.prixAchatMoyen)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onEdit(product) }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDelete(product) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Stock Bar & Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val isRupture = product.stockActuel <= 0
                            val isAlert = product.stockActuel <= product.seuilAlerte && !isRupture
                            val stockColor = when {
                                isRupture -> Color(0xFFDC2626)
                                isAlert -> Color(0xFFD97706)
                                else -> Color(0xFF16A34A)
                            }
                            val stockBg = when {
                                isRupture -> Color(0xFFFEE2E2)
                                isAlert -> Color(0xFFFEF3C7)
                                else -> Color(0xFFDCFCE7)
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = stockBg
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isRupture -> "Rupture (0 ${product.unit})"
                                            isAlert -> "Faible: ${CurrencyUtils.formatQty(product.stockActuel)} / ${product.seuilAlerte} ${product.unit}"
                                            else -> "En Stock: ${CurrencyUtils.formatQty(product.stockActuel)} ${product.unit}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = stockColor
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (product.isArticleKit || product.isArticleEclate) {
                                    val badgeColor = if (product.isArticleKit) Color(0xFF0D9488) else Color(0xFF7C3AED)
                                    FilledTonalButton(
                                        onClick = { onManageKit(product) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (product.isArticleKit) "Composants Kit" else "Composants Éclaté",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = badgeColor
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onAdjustStock(product) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Ajuster Stock", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockManagementSection(
    allProducts: List<ProductCatalogEntity>,
    products: List<ProductCatalogEntity>,
    isAdmin: Boolean,
    onAdjustStock: (ProductCatalogEntity) -> Unit,
    onQuickDelta: (ProductCatalogEntity, Double) -> Unit,
    onManageKit: (ProductCatalogEntity) -> Unit,
    onAddProduct: () -> Unit
) {
    val totalStockValue = remember(allProducts) {
        allProducts.sumOf { (it.stockActuel * (it.defaultUnitPrice ?: 0.0)) }
    }
    val totalUnits = remember(allProducts) {
        allProducts.sumOf { it.stockActuel }
    }
    val countRuptures = remember(allProducts) {
        allProducts.count { it.stockActuel <= 0 }
    }
    val countAlerts = remember(allProducts) {
        allProducts.count { it.stockActuel > 0 && it.stockActuel <= it.seuilAlerte }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Stock KPI Overview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stock_kpi_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "État Général du Stock",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${allProducts.size} réf.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Valorisation
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Valeur Totale (DA)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyUtils.formatDaSmart(totalStockValue),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${CurrencyUtils.formatQty(totalUnits)} unités au total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Ruptures & Alertes
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (countRuptures > 0 || countAlerts > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Alertes Stock", style = MaterialTheme.typography.labelSmall, color = Color(0xFF92400E))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${countRuptures + countAlerts} alertes",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (countRuptures > 0) Color(0xFFDC2626) else Color(0xFFD97706)
                                )
                                Text(
                                    text = "$countRuptures en rupture • $countAlerts faibles",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun article correspondant aux filtres de stock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(products, key = { it.id }) { product ->
                val isRupture = product.stockActuel <= 0
                val isAlert = product.stockActuel <= product.seuilAlerte && !isRupture
                val stockColor = when {
                    isRupture -> Color(0xFFDC2626)
                    isAlert -> Color(0xFFD97706)
                    else -> Color(0xFF16A34A)
                }
                val stockBg = when {
                    isRupture -> Color(0xFFFEE2E2)
                    isAlert -> Color(0xFFFEF3C7)
                    else -> Color(0xFFDCFCE7)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (product.isArticleKit) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF0D9488).copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "🎁 Article Kit",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF0D9488),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else if (product.isArticleEclate) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF7C3AED).copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "⚡ Éclaté",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF7C3AED),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Famille: ${product.category}" +
                                            (if (product.marque.isNotBlank()) " • Marque: ${product.marque}" else "") +
                                            " • Seuil: ${product.seuilAlerte} ${product.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (product.defaultUnitPrice != null && product.defaultUnitPrice > 0) {
                                    Text(
                                        text = "Prix de vente: ${CurrencyUtils.formatDa(product.defaultUnitPrice)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isAdmin && product.prixAchatMoyen != null && product.prixAchatMoyen > 0) {
                                    Text(
                                        text = "Prix d'achat: ${CurrencyUtils.formatDa(product.prixAchatMoyen)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Stock status pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = stockBg
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${CurrencyUtils.formatQty(product.stockActuel)} ${product.unit}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = stockColor
                                    )
                                    Text(
                                        text = when {
                                            isRupture -> "Rupture"
                                            isAlert -> "Stock Faible"
                                            else -> "En Stock"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = stockColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Actions: Quick +1 / -1 buttons and Ajuster dialog button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = { onQuickDelta(product, 1.0) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(height = 32.dp, width = 64.dp)
                                ) {
                                    Text("+ 1", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }

                                FilledTonalButton(
                                    onClick = { onQuickDelta(product, -1.0) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = product.stockActuel > 0,
                                    modifier = Modifier.size(height = 32.dp, width = 64.dp)
                                ) {
                                    Text("- 1", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (product.isArticleKit || product.isArticleEclate) {
                                    val badgeColor = if (product.isArticleKit) Color(0xFF0D9488) else Color(0xFF7C3AED)
                                    OutlinedButton(
                                        onClick = { onManageKit(product) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (product.isArticleKit) "Kit" else "Éclaté",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onAdjustStock(product) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Ajuster", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitCompositionDialog(
    product: ProductCatalogEntity,
    viewModel: OperationViewModel,
    allProducts: List<ProductCatalogEntity>,
    onDismiss: () -> Unit
) {
    val compositionsFlow = remember(product.id) { viewModel.getCompositionsForProduct(product.id) }
    val compositions by compositionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedIsArticleKit by remember(product.id, product.isArticleKit, product.isArticleEclate) {
        mutableStateOf(product.isArticleKit)
    }

    var newComponentName by remember { mutableStateOf("") }
    var newComponentQty by remember { mutableStateOf("1") }
    var newComponentPrice by remember { mutableStateOf("") }
    var newComponentUnit by remember { mutableStateOf("U") }
    var showSuggestions by remember { mutableStateOf(false) }

    val themeColor = if (selectedIsArticleKit) Color(0xFF0D9488) else Color(0xFF7C3AED)
    val sumComponentsValue = remember(compositions) {
        compositions.sumOf { (it.prixUnitaireEstime ?: 0.0) * it.quantite }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (selectedIsArticleKit)
                            "🎁 Article Kit (Nomenclature commerciale)"
                        else
                            "⚡ Article Éclaté (ou Kit éclaté)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColor
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bascule directe entre Article Kit et Article Éclaté
                Text(
                    text = "Mode de Nomenclature :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedIsArticleKit,
                        onClick = {
                            selectedIsArticleKit = true
                            val updated = product.copy(isArticleKit = true, isArticleEclate = false)
                            viewModel.updateCatalogProduct(updated)
                        },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text("🎁 Article Kit", style = MaterialTheme.typography.labelSmall) }
                    )
                    SegmentedButton(
                        selected = !selectedIsArticleKit,
                        onClick = {
                            selectedIsArticleKit = false
                            val updated = product.copy(isArticleKit = false, isArticleEclate = true)
                            viewModel.updateCatalogProduct(updated)
                        },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text("⚡ Article Éclaté", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Tableau récapitulatif des 5 critères
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = themeColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (selectedIsArticleKit) {
                            Text(
                                text = "Comportement — Article Kit (Nomenclature commerciale) :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = themeColor
                            )
                            Text("• Affichage à la saisie : 1 seule ligne par défaut (l'article parent).", style = MaterialTheme.typography.labelSmall)
                            Text("• Impression client : Le client voit le nom du Kit (option possible pour forcer l'impression du détail).", style = MaterialTheme.typography.labelSmall)
                            Text("• Modification possible : Rigide (impossible de retirer un composant sans éclater le kit).", style = MaterialTheme.typography.labelSmall)
                            Text("• Prix de vente : Fixé au niveau de l'article parent (prix du pack : ${CurrencyUtils.formatDa(product.defaultUnitPrice ?: 0.0)}).", style = MaterialTheme.typography.labelSmall)
                            Text("• Sortie de stock : Déstocke automatiquement les composants en tâche de fond lors de la validation.", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text(
                                text = "Comportement — Article Éclaté (ou Kit éclaté) :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = themeColor
                            )
                            Text("• Affichage à la saisie : Plusieurs lignes : l'article parent disparaît et est remplacé par ses composants dès la validation.", style = MaterialTheme.typography.labelSmall)
                            Text("• Impression client : Le client voit uniquement la liste détaillée de tous les composants (comme s'ils avaient été saisis un par un).", style = MaterialTheme.typography.labelSmall)
                            Text("• Modification possible : Totalement flexible (chaque composant devient une ligne indépendante modifiable ou supprimable).", style = MaterialTheme.typography.labelSmall)
                            Text("• Prix de vente : Calculé par l'addition du prix de chaque composant individuel (Total actuel : ${CurrencyUtils.formatDa(sumComponentsValue)}).", style = MaterialTheme.typography.labelSmall)
                            Text("• Sortie de stock : Déstocke chaque composant indépendamment, ligne par ligne.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Composants définis (${compositions.size}) :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (compositions.isNotEmpty()) {
                        Text(
                            text = "Somme comp. : ${CurrencyUtils.formatDa(sumComponentsValue)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = themeColor
                        )
                    }
                }

                if (compositions.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Aucun sous-article défini. Ajoutez les composants ci-dessous.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    compositions.forEach { comp ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comp.componentName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Quantité : ${CurrencyUtils.formatQty(comp.quantite)} ${comp.unit}" +
                                                (comp.prixUnitaireEstime?.let { " • PU: ${CurrencyUtils.formatDa(it)} (Sous-total: ${CurrencyUtils.formatDa(it * comp.quantite)})" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteComposition(comp.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Ajouter un composant :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = newComponentName,
                    onValueChange = {
                        newComponentName = it
                        showSuggestions = it.isNotBlank()
                    },
                    label = { Text("Nom du sous-article / composant *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Suggestions from catalog
                val matchingProducts = remember(newComponentName, allProducts) {
                    if (newComponentName.isBlank()) emptyList()
                    else allProducts.filter {
                        it.id != product.id && it.name.contains(newComponentName, ignoreCase = true)
                    }.take(3)
                }

                if (showSuggestions && matchingProducts.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        matchingProducts.forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable {
                                    newComponentName = suggestion.name
                                    newComponentUnit = suggestion.unit
                                    suggestion.defaultUnitPrice?.let {
                                        newComponentPrice = CurrencyUtils.formatQty(it)
                                    }
                                    showSuggestions = false
                                }
                            ) {
                                Text(
                                    text = suggestion.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newComponentQty,
                        onValueChange = { newComponentQty = it },
                        label = { Text("Quantité *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = newComponentUnit,
                        onValueChange = { newComponentUnit = it },
                        label = { Text("Unité") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = newComponentPrice,
                    onValueChange = { newComponentPrice = it },
                    label = { Text("Prix unitaire du composant (DA)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Button(
                    onClick = {
                        if (newComponentName.isNotBlank()) {
                            val qty = CurrencyUtils.parseAmount(newComponentQty).coerceAtLeast(0.1)
                            val price = if (newComponentPrice.isNotBlank()) CurrencyUtils.parseAmount(newComponentPrice) else null
                            val newComp = ArticleCompositionEntity(
                                parentProductId = product.id,
                                componentName = newComponentName.trim(),
                                quantite = qty,
                                prixUnitaireEstime = price,
                                unit = newComponentUnit.trim().ifEmpty { "U" }
                            )
                            viewModel.saveArticleEclateWithComposition(
                                product = product.copy(
                                    isArticleKit = selectedIsArticleKit,
                                    isArticleEclate = !selectedIsArticleKit
                                ),
                                components = compositions + newComp
                            )
                            newComponentName = ""
                            newComponentQty = "1"
                            newComponentPrice = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (selectedIsArticleKit) "Ajouter au Kit Commercial" else "Ajouter à l'Article Éclaté")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun StockAdjustDialog(
    product: ProductCatalogEntity,
    viewModel: OperationViewModel,
    onDismiss: () -> Unit
) {
    var adjustMode by remember { mutableIntStateOf(0) } // 0: Ajouter, 1: Retirer, 2: Remplacer/Inventaire
    var amountText by remember { mutableStateOf("1") }
    var alertThresholdText by remember { mutableStateOf(CurrencyUtils.formatQty(product.seuilAlerte)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Ajuster le Stock", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(product.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Stock Actuel :", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${CurrencyUtils.formatQty(product.stockActuel)} ${product.unit}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (product.stockActuel <= 0) MaterialTheme.colorScheme.error
                            else if (product.stockActuel <= product.seuilAlerte) Color(0xFFD97706)
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(text = "Opération à effectuer :", style = MaterialTheme.typography.labelSmall)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = listOf("+ Entrée", "- Sortie", "= Régulariser")
                    modes.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = adjustMode == index,
                            onClick = { adjustMode = index },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = {
                        Text(
                            when (adjustMode) {
                                0 -> "Quantité à ajouter (${product.unit})"
                                1 -> "Quantité à retirer (${product.unit})"
                                else -> "Nouveau stock réel compté (${product.unit})"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = alertThresholdText,
                    onValueChange = { alertThresholdText = it },
                    label = { Text("Seuil d'alerte stock minimum") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = CurrencyUtils.parseAmount(amountText)
                    val newThreshold = CurrencyUtils.parseAmount(alertThresholdText)

                    val updatedProduct = product.copy(seuilAlerte = newThreshold)
                    if (newThreshold != product.seuilAlerte) {
                        viewModel.updateCatalogProduct(updatedProduct)
                    }

                    when (adjustMode) {
                        0 -> viewModel.adjustStock(product.id, amount)
                        1 -> viewModel.adjustStock(product.id, -amount)
                        2 -> viewModel.updateStock(product.id, amount)
                    }
                    onDismiss()
                }
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
