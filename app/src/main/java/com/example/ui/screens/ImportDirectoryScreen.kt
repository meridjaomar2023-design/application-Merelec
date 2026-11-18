package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberUpdatedState
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.ArticleColumnSpec
import com.example.util.CreanceColumnSpec
import com.example.util.CurrencyUtils
import com.example.util.ExportUtils
import com.example.util.ImportUtils
import com.example.util.PartnerColumnSpec
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDirectoryScreen(
    viewModel: OperationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    val products by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val clientsBalances by viewModel.clientsBalances.collectAsStateWithLifecycle()
    val suppliersBalances by viewModel.suppliersBalances.collectAsStateWithLifecycle()
    val lastImportSummary by viewModel.lastImportSummary.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val isAdminUser = currentEmployee.isAdmin

    val allCreancesItems = remember(clientsBalances, suppliersBalances) {
        (clientsBalances + suppliersBalances).filter { it.soldeRestant > 0.01 }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Clients, 1: Fournisseurs, 2: Articles, 3: Créances
    var searchQuery by remember { mutableStateOf("") }

    // Dialogs state
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var showColumnsConfigPanel by remember { mutableStateOf(false) }

    var partnerImportColumns by remember { mutableStateOf(ImportUtils.defaultPartnerColumns()) }
    var articleImportColumns by remember { mutableStateOf(ImportUtils.defaultArticleColumns()) }
    var creanceImportColumns by remember { mutableStateOf(ImportUtils.defaultCreanceColumns()) }

    val currentPartnerColsState by rememberUpdatedState(partnerImportColumns)
    val currentArticleColsState by rememberUpdatedState(articleImportColumns)
    val currentCreanceColsState by rememberUpdatedState(creanceImportColumns)

    var showAddManualDialog by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualRef by remember { mutableStateOf("") }
    var manualCodeBarre by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var manualAddress by remember { mutableStateOf("") }
    var manualPrixAchat by remember { mutableStateOf("") }
    var manualPrice by remember { mutableStateOf("") }
    var manualCategory by remember { mutableStateOf("") }
    var manualMarque by remember { mutableStateOf("") }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var partnerToDelete by remember { mutableStateOf<PartnerEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductCatalogEntity?>(null) }

    if (partnerToDelete != null) {
        val target = partnerToDelete!!
        val typeLabel = if (target.type == PartnerType.CLIENT) "le client" else "le fournisseur"
        AlertDialog(
            onDismissRequest = { partnerToDelete = null },
            title = { Text(if (target.type == PartnerType.CLIENT) "Supprimer le client ?" else "Supprimer le fournisseur ?") },
            text = { Text("Confirmez-vous la suppression définitive de $typeLabel « ${target.name} » ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePartner(target.id)
                        partnerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    if (productToDelete != null) {
        val target = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Supprimer l'article ?") },
            text = { Text("Confirmez-vous la suppression définitive de l'article « ${target.name} » du catalogue ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCatalogProduct(target.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    BackHandler {
        viewModel.navigateBack()
    }

    // File picker launcher for CSV/Text/Excel (.xlsx / .xls) files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            when (selectedTabIndex) {
                0 -> viewModel.importPartnersFromUri(context, it, PartnerType.CLIENT, currentPartnerColsState)
                1 -> viewModel.importPartnersFromUri(context, it, PartnerType.FOURNISSEUR, currentPartnerColsState)
                2 -> viewModel.importProductsFromUri(context, it, currentArticleColsState)
                3 -> viewModel.importCreancesFromUri(context, it, PartnerType.CLIENT, currentCreanceColsState)
            }
        }
    }

    // Dialog de rapport d'importation
    lastImportSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportSummary() },
            title = {
                Text(
                    text = if (summary.isSuccess) "Importation Réussie (${summary.entityLabel})" else "Résultat de l'Importation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Format détecté : ${summary.fileFormat}", style = MaterialTheme.typography.bodySmall)
                    Text("Lignes lues : ${summary.totalRowsRead}", style = MaterialTheme.typography.bodySmall)
                    Text("Créés : ${summary.createdCount}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF059669)))
                    Text("Mis à jour : ${summary.updatedCount}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    if (summary.skippedCount > 0) {
                        Text("Lignes ignorées : ${summary.skippedCount}", style = MaterialTheme.typography.bodySmall)
                    }
                    summary.errorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearImportSummary() }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog Coller une liste
    if (showPasteDialog) {
        val targetLabel = when (selectedTabIndex) {
            0 -> "clients ('nom; telephone; adresse; rc; ai; nif; nis; nin; rib; notes; solde_initial')"
            1 -> "fournisseurs ('nom; telephone; adresse; rc; ai; nif; nis; nin; rib; notes; solde_initial')"
            2 -> "articles ('désignation; réf; code-barre; famille; marque; prix achat; prix de vente; stock; unité; seuil alerte')"
            else -> "créances ('nom; telephone; adresse; rc; ai; nif; nis; nin; rib; reference; montant_facture; montant_verse; reste_a_payer; notes')"
        }

        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Coller une liste CSV / Excel") },
            text = {
                Column {
                    Text(
                        text = "Collez votre texte copié depuis un tableur Excel, CSV ou WhatsApp pour les $targetLabel :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("paste_text_input"),
                        placeholder = {
                            Text(
                                when (selectedTabIndex) {
                                    2 -> "Disjoncteur 16A; REF-01; 360648001; Protection; Schneider; 480; 680; 50; U; 5\nCâble 2.5mm; REF-02; ; Câblage; Elsewedy; 3600; 4500; 25; Rlx; 5"
                                    3 -> "SARL ElecPro Algérie; 0550123456; Alger; ; ; ; ; ; ; BL-2025-104; 85000; 25000; 60000\nEURL Bâtiment; 0661987654; Béjaïa; ; ; ; ; ; ; FAC-042; 42000; 0; 42000"
                                    else -> "SARL Batiment Alger; 0550123456; Rouiba\nEts Oran Elec; 0561987654; Oran"
                                }
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
                                2 -> viewModel.importProducts(pasteText, articleImportColumns)
                                3 -> viewModel.importCreances(pasteText, PartnerType.CLIENT, creanceImportColumns)
                            }
                            pasteText = ""
                            showPasteDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_paste_import_btn")
                ) {
                    Text("Importer la liste")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialog Ajout Manuel Rapide
    if (showAddManualDialog) {
        val title = when (selectedTabIndex) {
            0 -> "Ajouter un Client"
            1 -> "Ajouter un Fournisseur"
            else -> "Ajouter un Article au Catalogue"
        }

        AlertDialog(
            onDismissRequest = { showAddManualDialog = false },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text(if (selectedTabIndex == 2) "Désignation de l'article *" else "Nom de l'entreprise / Contact *") },
                        modifier = Modifier.fillMaxWidth().testTag("manual_entry_name"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    if (selectedTabIndex == 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualRef,
                                onValueChange = { manualRef = it },
                                label = { Text("Réf.") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = manualCodeBarre,
                                onValueChange = { manualCodeBarre = it },
                                label = { Text("Code-barres") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualCategory,
                                onValueChange = { manualCategory = it },
                                label = { Text("Famille") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = manualMarque,
                                onValueChange = { manualMarque = it },
                                label = { Text("Marque") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isAdminUser) {
                                OutlinedTextField(
                                    value = manualPrixAchat,
                                    onValueChange = { manualPrixAchat = it },
                                    label = { Text("Prix d'achat") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                            OutlinedTextField(
                                value = manualPrice,
                                onValueChange = { manualPrice = it },
                                label = { Text("Prix de vente") },
                                modifier = Modifier.weight(1f).testTag("manual_entry_price"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = manualPhone,
                            onValueChange = { manualPhone = it },
                            label = { Text("Numéro de Téléphone (Facultatif)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        OutlinedTextField(
                            value = manualAddress,
                            onValueChange = { manualAddress = it },
                            label = { Text("Adresse / Ville (Facultatif)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualName.isNotBlank()) {
                            when (selectedTabIndex) {
                                0 -> viewModel.addSinglePartner(manualName, PartnerType.CLIENT, manualPhone, manualAddress)
                                1 -> viewModel.addSinglePartner(manualName, PartnerType.FOURNISSEUR, manualPhone, manualAddress)
                                2 -> viewModel.addSingleProduct(
                                    name = manualName,
                                    priceText = manualPrice,
                                    category = manualCategory,
                                    famille = manualCategory,
                                    marque = manualMarque,
                                    prixAchatText = manualPrixAchat,
                                    prixVenteText = manualPrice,
                                    reference = manualRef,
                                    barcode = manualCodeBarre
                                )
                            }
                            manualName = ""
                            manualRef = ""
                            manualCodeBarre = ""
                            manualPhone = ""
                            manualAddress = ""
                            manualPrixAchat = ""
                            manualPrice = ""
                            manualCategory = ""
                            manualMarque = ""
                            showAddManualDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_manual_btn")
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirmation d'effacement
    if (showClearConfirmDialog) {
        val label = when (selectedTabIndex) {
            0 -> "la liste des clients"
            1 -> "la liste des fournisseurs"
            else -> "le catalogue d'articles"
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
                            2 -> viewModel.clearCatalogProducts()
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Importer Base de Données (CSV / Excel)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Articles • Clients • Fournisseurs • Créances",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("import_back_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Sauvegarde Google Drive
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.BackupDrive) },
                        modifier = Modifier.testTag("directory_drive_backup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddToDrive,
                            contentDescription = "Sauvegarder dans Google Drive",
                            tint = Color(0xFF0F9D58)
                        )
                    }

                    // Télécharger modèle Excel/CSV vierge
                    IconButton(
                        onClick = {
                            val template = when (selectedTabIndex) {
                                0 -> ImportUtils.generatePartnersTemplateCsv(PartnerType.CLIENT, partnerImportColumns)
                                1 -> ImportUtils.generatePartnersTemplateCsv(PartnerType.FOURNISSEUR, partnerImportColumns)
                                2 -> ImportUtils.generateProductsTemplateCsv(articleImportColumns)
                                else -> ImportUtils.generateCreancesTemplateCsv(PartnerType.CLIENT, creanceImportColumns)
                            }

                            val title = when (selectedTabIndex) {
                                0 -> "Modele_Clients_Merelec.csv"
                                1 -> "Modele_Fournisseurs_Merelec.csv"
                                2 -> "Modele_Articles_Merelec.csv"
                                else -> "Modele_Creances_Merelec.csv"
                            }
                            ExportUtils.shareText(context, title, template)
                        },
                        modifier = Modifier.testTag("download_template_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Modèle Excel / CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 4 Tabs: Clients, Fournisseurs, Articles, Créances
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("directory_tabs")
            ) {
                val tabs = listOf(
                    "Clients (${clients.size})",
                    "Fourn. (${suppliers.size})",
                    "Articles (${products.size})",
                    "Créances (${allCreancesItems.size})"
                )
                tabs.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            searchQuery = ""
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        label = { Text(title, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    )
                }
            }

            // Action Buttons Bar: Importer fichier, Coller liste, Ajouter manuellement
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bouton 1 : Importer fichier CSV / Excel
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f).testTag("import_file_picker_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importer Fichier", style = MaterialTheme.typography.labelSmall)
                    }

                    // Bouton 2 : Coller liste
                    FilledTonalButton(
                        onClick = { showPasteDialog = true },
                        modifier = Modifier.weight(1f).testTag("paste_list_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Coller Liste", style = MaterialTheme.typography.labelSmall)
                    }

                    // Bouton 3 : Option des colonnes
                    IconButton(
                        onClick = { showColumnsConfigPanel = !showColumnsConfigPanel },
                        modifier = Modifier.testTag("toggle_columns_config_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Option des colonnes",
                            tint = if (showColumnsConfigPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bouton 4 : + Manuel
                    IconButton(
                        onClick = { showAddManualDialog = true },
                        modifier = Modifier.testTag("add_manual_item_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Ajout manuel", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (showColumnsConfigPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("import_columns_config_panel"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ordre des colonnes (identique à la création)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    when (selectedTabIndex) {
                                        0, 1 -> partnerImportColumns = ImportUtils.defaultPartnerColumns()
                                        2 -> articleImportColumns = ImportUtils.defaultArticleColumns()
                                        else -> creanceImportColumns = ImportUtils.defaultCreanceColumns()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Réinitialiser", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            when (selectedTabIndex) {
                                0, 1 -> {
                                    partnerImportColumns.forEachIndexed { idx, col ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Checkbox(
                                                checked = col.enabled,
                                                onCheckedChange = { checked ->
                                                    if (col.field != com.example.util.PartnerImportField.NOM || checked) {
                                                        partnerImportColumns = partnerImportColumns.toMutableList().also { list ->
                                                            list[idx] = col.copy(enabled = checked)
                                                        }
                                                    }
                                                }
                                            )
                                            Text(
                                                text = "${idx + 1}. ${col.field.displayLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (idx > 0) {
                                                        partnerImportColumns = partnerImportColumns.toMutableList().also { list ->
                                                            val tmp = list[idx - 1]
                                                            list[idx - 1] = list[idx]
                                                            list[idx] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = idx > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Monter", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (idx < partnerImportColumns.lastIndex) {
                                                        partnerImportColumns = partnerImportColumns.toMutableList().also { list ->
                                                            val tmp = list[idx + 1]
                                                            list[idx + 1] = list[idx]
                                                            list[idx] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = idx < partnerImportColumns.lastIndex,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    articleImportColumns.forEachIndexed { idx, col ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Checkbox(
                                                checked = col.enabled,
                                                onCheckedChange = { checked ->
                                                    if (col.field != com.example.util.ArticleImportField.DESIGNATION || checked) {
                                                        articleImportColumns = articleImportColumns.toMutableList().also { list ->
                                                            list[idx] = col.copy(enabled = checked)
                                                        }
                                                    }
                                                }
                                            )
                                            Text(
                                                text = "${idx + 1}. ${col.field.displayLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (idx > 0) {
                                                        articleImportColumns = articleImportColumns.toMutableList().also { list ->
                                                            val tmp = list[idx - 1]
                                                            list[idx - 1] = list[idx]
                                                            list[idx] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = idx > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Monter", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (idx < articleImportColumns.lastIndex) {
                                                        articleImportColumns = articleImportColumns.toMutableList().also { list ->
                                                            val tmp = list[idx + 1]
                                                            list[idx + 1] = list[idx]
                                                            list[idx] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = idx < articleImportColumns.lastIndex,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    creanceImportColumns.forEachIndexed { idx, col ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Checkbox(
                                                checked = col.enabled,
                                                onCheckedChange = { checked ->
                                                    if (col.field != com.example.util.CreanceImportField.NOM || checked) {
                                                        creanceImportColumns = creanceImportColumns.toMutableList().also { list ->
                                                            list[idx] = col.copy(enabled = checked)
                                                        }
                                                    }
                                                }
                                            )
                                            Text(
                                                text = "${idx + 1}. ${col.field.displayLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (idx > 0) {
                                                        creanceImportColumns = creanceImportColumns.toMutableList().also { list ->
                                                            val tmp = list[idx - 1]
                                                            list[idx - 1] = list[idx]
                                                            list[idx] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = idx > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Monter", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (idx < creanceImportColumns.lastIndex) {
                                                        creanceImportColumns = creanceImportColumns.toMutableList().also { list ->
                                                            val tmp = list[idx + 1]
                                                            list[idx + 1] = list[idx]
                                                            list[idx] = tmp
                                                        }
                                                    }
                                                },
                                                enabled = idx < creanceImportColumns.lastIndex,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search bar in directory
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            when (selectedTabIndex) {
                                0 -> "Rechercher parmi les clients..."
                                1 -> "Rechercher parmi les fournisseurs..."
                                else -> "Rechercher un article ou prix..."
                            }
                        )
                    },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("directory_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Clean list of items
            when (selectedTabIndex) {
                0 -> {
                    val filtered = clients.filter {
                        searchQuery.isBlank() || it.name.lowercase(Locale.FRENCH).contains(searchQuery.lowercase(Locale.FRENCH)) ||
                                it.phone.contains(searchQuery) || it.address.lowercase(Locale.FRENCH).contains(searchQuery.lowercase(Locale.FRENCH))
                    }
                    PartnersListContent(
                        items = filtered,
                        isSupplier = false,
                        onDelete = { partnerToDelete = it },
                        onClearAll = { showClearConfirmDialog = true }
                    )
                }
                1 -> {
                    val filtered = suppliers.filter {
                        searchQuery.isBlank() || it.name.lowercase(Locale.FRENCH).contains(searchQuery.lowercase(Locale.FRENCH)) ||
                                it.phone.contains(searchQuery) || it.address.lowercase(Locale.FRENCH).contains(searchQuery.lowercase(Locale.FRENCH))
                    }
                    PartnersListContent(
                        items = filtered,
                        isSupplier = true,
                        onDelete = { partnerToDelete = it },
                        onClearAll = { showClearConfirmDialog = true }
                    )
                }
                2 -> {
                    val filtered = products.filter {
                        searchQuery.isBlank() || it.name.lowercase(Locale.FRENCH).contains(searchQuery.lowercase(Locale.FRENCH)) ||
                                it.category.lowercase(Locale.FRENCH).contains(searchQuery.lowercase(Locale.FRENCH))
                    }
                    ProductsListContent(
                        items = filtered,
                        isAdmin = isAdminUser,
                        onDelete = { productToDelete = it },
                        onClearAll = { showClearConfirmDialog = true }
                    )
                }
                3 -> {
                    val filtered = allCreancesItems.filter {
                        searchQuery.isBlank() ||
                                it.partner.name.lowercase(Locale.FRENCH).contains(searchQuery.lowercase(Locale.FRENCH)) ||
                                it.partner.phone.contains(searchQuery)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Format CSV / Excel attendu pour les Créances & Dettes (même ordre que la création) :",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFDC2626)
                                    )
                                    Text(
                                        text = "nom ; telephone ; adresse ; rc ; ai ; nif ; nis ; nin ; rib ; reference ; montant_facture ; montant_verse ; reste_a_payer ; notes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            ExportUtils.shareText(
                                                context,
                                                "Modele_Creances_Merelec.csv",
                                                ImportUtils.generateCreancesTemplateCsv(PartnerType.CLIENT)
                                            )
                                        }
                                    ) {
                                        Text("Télécharger / Partager le modèle CSV Créances")
                                    }
                                }
                            }
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Aucune créance ou dette enregistrée. Cliquez sur « Importer Fichier » (.xlsx ou .csv) ou « Coller Liste ».",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(filtered, key = { it.partner.id }) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.partner.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${item.partner.type.label} • Facturé: ${CurrencyUtils.formatDa(item.totalTransactions)} • Versé: ${CurrencyUtils.formatDa(item.totalReglements)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = CurrencyUtils.formatDa(item.soldeRestant),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color(0xFFDC2626)
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
private fun PartnersListContent(
    items: List<PartnerEntity>,
    isSupplier: Boolean,
    onDelete: (PartnerEntity) -> Unit,
    onClearAll: () -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isSupplier) Icons.Default.Business else Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aucun ${if (isSupplier) "fournisseur" else "client"} trouvé",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Importez un fichier CSV ou collez votre liste pour alimenter le répertoire",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${items.size} enregistré(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onClearAll) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Effacer la liste", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            items(items, key = { it.id }) { partner ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSupplier) Icons.Default.Business else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = partner.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (partner.phone.isNotBlank() || partner.address.isNotBlank()) {
                                    val details = listOfNotNull(
                                        partner.phone.ifBlank { null },
                                        partner.address.ifBlank { null }
                                    ).joinToString(" • ")
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { onDelete(partner) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductsListContent(
    items: List<ProductCatalogEntity>,
    isAdmin: Boolean,
    onDelete: (ProductCatalogEntity) -> Unit,
    onClearAll: () -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aucun article dans le catalogue",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Importez votre catalogue de matériel électrique / quincaillerie",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${items.size} article(s) au catalogue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onClearAll) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vider le catalogue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            items(items, key = { it.id }) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Famille: ${product.category}",
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
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (product.defaultUnitPrice != null && product.defaultUnitPrice > 0) {
                                    Text(
                                        text = "Prix de vente: ${CurrencyUtils.formatDa(product.defaultUnitPrice)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isAdmin && product.prixAchatMoyen != null && product.prixAchatMoyen > 0) {
                                    Text(
                                        text = "Prix d'achat: ${CurrencyUtils.formatDa(product.prixAchatMoyen)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { onDelete(product) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
