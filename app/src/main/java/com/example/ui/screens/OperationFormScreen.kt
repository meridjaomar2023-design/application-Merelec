package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import com.example.data.model.GroupedOperationLine
import com.example.data.model.LigneOperationEntity
import com.example.data.model.OperationType
import com.example.data.model.ProductCatalogEntity
import com.example.data.model.groupLignesByParent
import com.example.ui.components.InvoiceOcrScannerDialog
import com.example.ui.components.ProductLineDialog
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.BarcodeScannerHelper
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationFormScreen(
    viewModel: OperationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val lineEditor by viewModel.lineEditor.collectAsStateWithLifecycle()
    val recentPartners by viewModel.recentPartners.collectAsStateWithLifecycle()
    val recentProducts by viewModel.recentProducts.collectAsStateWithLifecycle()
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    val catalogProducts by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()

    val partnerSuggestions = remember(formState.typeOperation, clients, suppliers, recentPartners) {
        val list = mutableListOf<String>()
        if (formState.typeOperation.isPurchaseDocument) {
            suppliers.forEach { if (!list.contains(it.name)) list.add(it.name) }
        } else {
            clients.forEach { if (!list.contains(it.name)) list.add(it.name) }
        }
        recentPartners.forEach { if (!list.contains(it)) list.add(it) }
        list
    }

    var lineToReplaceInGrid by remember { mutableStateOf<LigneOperationEntity?>(null) }
    var replaceSearchQuery by remember { mutableStateOf("") }
    var showInvoiceOcrDialog by remember { mutableStateOf(false) }

    if (showInvoiceOcrDialog) {
        val allKnownPartners = remember(clients, suppliers) {
            (clients + suppliers).distinctBy { it.name.lowercase() }
        }
        InvoiceOcrScannerDialog(
            initialTargetType = if (formState.typeOperation == OperationType.REGLEMENT) {
                OperationType.FACTURE_FOURNISSEUR
            } else {
                formState.typeOperation
            },
            knownPartners = allKnownPartners,
            catalogProducts = catalogProducts,
            ownCompanyName = companySettings.companyName,
            defaultMarginCoefficient = companySettings.defaultMarginCoefficient,
            allowAppendToCurrentDraft = formState.lignes.isNotEmpty(),
            onOpenIntelligentOcr = {
                showInvoiceOcrDialog = false
                viewModel.navigateTo(
                    com.example.ui.viewmodel.AppScreen.IntelligentOcr(
                        if (formState.typeOperation == OperationType.REGLEMENT) {
                            OperationType.FACTURE_FOURNISSEUR
                        } else {
                            formState.typeOperation
                        }
                    )
                )
            },
            onDismiss = { showInvoiceOcrDialog = false },
            onImportResult = { targetType, partnerName, invNum, lines, tva, rate, margin, append, saveNow ->
                showInvoiceOcrDialog = false
                viewModel.importOcrInvoiceToForm(
                    targetType = targetType,
                    partnerName = partnerName,
                    detectedInvoiceNumber = invNum,
                    lines = lines,
                    appliquerTva = tva,
                    tauxTva = rate,
                    marginCoefficient = margin,
                    appendToExistingDraft = append,
                    saveImmediately = saveNow
                )
            }
        )
    }

    lineToReplaceInGrid?.let { targetLine ->
        val filteredReplacementProducts = remember(replaceSearchQuery, catalogProducts) {
            val q = replaceSearchQuery.trim().lowercase()
            if (q.isBlank()) {
                catalogProducts.take(40)
            } else {
                val tokens = q.split("\\s+".toRegex()).filter { it.isNotBlank() }
                catalogProducts.filter { prod ->
                    val s = "${prod.name} ${prod.reference} ${prod.category} ${prod.marque}".lowercase()
                    tokens.all { s.contains(it) }
                }.take(40)
            }
        }

        AlertDialog(
            onDismissRequest = {
                lineToReplaceInGrid = null
                replaceSearchQuery = ""
            },
            title = {
                Column {
                    Text(
                        text = "Remplacer l'article dans la grille",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Actuel : ${targetLine.produit} (Qté conservée : ${CurrencyUtils.formatQty(targetLine.quantite)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = replaceSearchQuery,
                        onValueChange = { replaceSearchQuery = it },
                        label = { Text("Rechercher un article du catalogue (réf, nom...)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("replace_article_search_input")
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredReplacementProducts.isEmpty()) {
                            Text(
                                text = "Aucun article correspondant dans le catalogue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            filteredReplacementProducts.forEach { prod ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.replaceLineProductInDraft(targetLine.id, prod)
                                            lineToReplaceInGrid = null
                                            replaceSearchQuery = ""
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (prod.reference.isNotBlank()) "[${prod.reference}] ${prod.name}" else prod.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Stock: ${CurrencyUtils.formatQty(prod.stockActuel)} ${prod.unit} • PV: ${CurrencyUtils.formatDa(prod.defaultUnitPrice ?: prod.prixVente ?: 0.0)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Choisir",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val lineToEdit = targetLine
                        lineToReplaceInGrid = null
                        replaceSearchQuery = ""
                        viewModel.openEditLineDialog(lineToEdit)
                    }
                ) {
                    Text("Saisie libre / Modifier")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        lineToReplaceInGrid = null
                        replaceSearchQuery = ""
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    val productSuggestions = remember(catalogProducts, recentProducts) {
        val list = mutableListOf<String>()
        catalogProducts.forEach { if (!list.contains(it.name)) list.add(it.name) }
        recentProducts.forEach { if (!list.contains(it)) list.add(it) }
        list
    }

    BackHandler {
        viewModel.navigateBack()
    }

    // Product Line Dialog (Supports Auto re-open & live suggestions)
    ProductLineDialog(
        state = lineEditor,
        recentProducts = productSuggestions,
        catalogProducts = catalogProducts,
        isPurchaseDocument = formState.typeOperation.isPurchaseDocument,
        onProduitChanged = viewModel::onLineProduitChanged,
        onQuantiteChanged = viewModel::onLineQuantiteChanged,
        onPrixUnitaireChanged = viewModel::onLinePrixUnitaireChanged,
        onAutoReopenChanged = viewModel::onLineAutoReopenChanged,
        onSaveAndNext = { viewModel.saveLine(forceClose = false) },
        onSaveAndClose = { viewModel.saveLine(forceClose = true) },
        onDismiss = viewModel::closeLineDialog
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (formState.isEditing) "Modifier Bon / Règlement" else "Nouveau Bon & Règlement",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInvoiceOcrDialog = true },
                        modifier = Modifier.testTag("form_ocr_scan_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scanner Facture en Image (OCR)",
                            tint = Color(0xFF4338CA)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.saveOperation() },
                        modifier = Modifier.testTag("form_save_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Enregistrer"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Finish View bar with summary
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        val label = if (formState.typeOperation == OperationType.REGLEMENT) {
                            "Montant Règlement (DA)"
                        } else if (formState.appliquerTva) {
                            "Total Net TTC (dont TVA ${formState.tauxTva}%)"
                        } else {
                            "Total du Bon (DA)"
                        }

                        val amount = if (formState.typeOperation == OperationType.REGLEMENT)
                            CurrencyUtils.formatDa(formState.montantVerseNumeric)
                        else
                            CurrencyUtils.formatDa(formState.calculatedTotalTtc)

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = amount,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (formState.appliquerTva && formState.showProductLines) {
                            Text(
                                text = "HT: ${CurrencyUtils.formatDa(formState.calculatedTotalProduits)} • TVA: ${CurrencyUtils.formatDa(formState.montantTva)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.saveOperation() },
                        modifier = Modifier.testTag("form_submit_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrer")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Entête du Bon / Opération (Table 1: 1_Saisie_Terrain)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Entête de l'Opération",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Type_Operation selector (Enum: BL Client, BL Fournisseur, Règlement)
                    Text(
                        text = "Type d'Opération :",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("type_operation_segmented"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OperationType.entries.forEach { opType ->
                            val isSelected = formState.typeOperation == opType
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onTypeChanged(opType) },
                                label = {
                                    Text(
                                        text = opType.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (opType) {
                                            OperationType.BL_CLIENT -> Icons.Default.Inventory2
                                            OperationType.FACTURE -> Icons.Default.Description
                                            OperationType.FACTURE_FOURNISSEUR -> Icons.Default.ReceiptLong
                                            OperationType.BON_COMMANDE -> Icons.Default.Assignment
                                            OperationType.DEVIS_CLIENT -> Icons.Default.Description
                                            OperationType.AVOIR_CLIENT -> Icons.Default.ReceiptLong
                                            OperationType.BL_FOURNISSEUR -> Icons.Default.Business
                                            OperationType.DEVIS_FOURNISSEUR -> Icons.Default.Assignment
                                            OperationType.AVOIR_FOURNISSEUR -> Icons.Default.ReceiptLong
                                            OperationType.REGLEMENT -> Icons.Default.Payments
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("type_chip_${opType.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ID_Unique (Key) & Date_Heure (DateTime)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.id,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ID Unique") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("operation_id_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = DateUtils.formatDateTime(formState.dateTime),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date & Heure") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("operation_datetime_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Client_Fournisseur (Text) avec liste de suggestions en temps réel selon la saisie
                    val isPurchaseDoc = formState.typeOperation.isPurchaseDocument
                    val partnerLabel = if (isPurchaseDoc) "Fournisseur *" else "Client *"
                    val partnerIcon = if (isPurchaseDoc) Icons.Default.Business else Icons.Default.Person
                    val activePartnerEntities = if (isPurchaseDoc) suppliers else clients
                    val typedPartnerQuery = formState.clientFournisseur.trim()

                    val matchingPartnerEntities = remember(typedPartnerQuery, activePartnerEntities) {
                        if (typedPartnerQuery.isBlank()) {
                            emptyList()
                        } else {
                            val tokens = typedPartnerQuery.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
                            activePartnerEntities.filter { p ->
                                val searchable = "${p.name} ${p.phone} ${p.address} ${p.rc} ${p.nif}".lowercase()
                                tokens.all { token -> searchable.contains(token) }
                            }.take(20)
                        }
                    }

                    val matchingRecentPartners = remember(typedPartnerQuery, recentPartners, activePartnerEntities) {
                        if (typedPartnerQuery.isBlank()) {
                            emptyList()
                        } else {
                            val entityNamesLower = activePartnerEntities.map { it.name.lowercase() }.toSet()
                            recentPartners.filter { name ->
                                name.contains(typedPartnerQuery, ignoreCase = true) && !entityNamesLower.contains(name.lowercase())
                            }.take(10)
                        }
                    }

                    val isExactPartnerMatch = matchingPartnerEntities.size == 1 &&
                        matchingPartnerEntities.first().name.equals(typedPartnerQuery, ignoreCase = true) &&
                        matchingRecentPartners.isEmpty()

                    OutlinedTextField(
                        value = formState.clientFournisseur,
                        onValueChange = viewModel::onClientFournisseurChanged,
                        label = { Text(partnerLabel) },
                        placeholder = {
                            Text(
                                if (isPurchaseDoc) "Tapez les premières lettres du fournisseur..."
                                else "Tapez les premières lettres du client..."
                            )
                        },
                        leadingIcon = { Icon(imageVector = partnerIcon, contentDescription = null) },
                        trailingIcon = {
                            if (formState.clientFournisseur.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onClientFournisseurChanged("") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Effacer",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_fournisseur_input"),
                        singleLine = true,
                        isError = formState.clientError != null,
                        supportingText = formState.clientError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    // Liste de suggestions en direct pour Client / Fournisseur pendant la saisie
                    if (typedPartnerQuery.isNotBlank() && !isExactPartnerMatch &&
                        (matchingPartnerEntities.isNotEmpty() || matchingRecentPartners.isNotEmpty())
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag("partner_live_suggestions_list")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 175.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Suggestions ${if (isPurchaseDoc) "Fournisseurs" else "Clients"} pour « $typedPartnerQuery » (${matchingPartnerEntities.size + matchingRecentPartners.size}) :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                                matchingPartnerEntities.forEachIndexed { idx, partner ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onClientFournisseurChanged(partner.name) }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = partner.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val meta = buildList {
                                                if (partner.phone.isNotBlank()) add("Tél: ${partner.phone}")
                                                if (partner.address.isNotBlank()) add(partner.address)
                                            }
                                            if (meta.isNotEmpty()) {
                                                Text(
                                                    text = meta.joinToString(" • "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionner",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (idx < matchingPartnerEntities.lastIndex || matchingRecentPartners.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }

                                matchingRecentPartners.forEachIndexed { idx, partnerName ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onClientFournisseurChanged(partnerName) }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = partnerIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = partnerName,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    if (idx < matchingRecentPartners.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }

                    // Partner chips from imported directory & recent history (quand le champ est vide)
                    if (typedPartnerQuery.isBlank() && partnerSuggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            partnerSuggestions.take(10).forEach { partner ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { viewModel.onClientFournisseurChanged(partner) }
                                ) {
                                    Text(
                                        text = partner,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. DYNAMIC SHOW_IF: Details des Produits (Table 2: 2_Lignes_Operation)
            // Condition AppSheet: [Type_Operation] <> "Règlement"
            AnimatedVisibility(
                visible = formState.showProductLines,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                var inlineArticleQuery by remember { mutableStateOf("") }
                val inlineMatchingArticles = remember(inlineArticleQuery, catalogProducts) {
                    val q = inlineArticleQuery.trim()
                    if (q.isBlank()) {
                        emptyList()
                    } else {
                        val tokens = q.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
                        catalogProducts.filter { prod ->
                            val searchable = "${prod.name} ${prod.reference} ${prod.category} ${prod.marque} ${prod.barcode}".lowercase()
                            tokens.all { token -> searchable.contains(token) }
                        }.take(25)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_lines_section"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "2. Articles du Bon",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = { showInvoiceOcrDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("form_scan_invoice_ocr_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = Color(0xFF4338CA),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Facture OCR",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF4338CA)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                FilledTonalButton(
                                    onClick = {
                                        BarcodeScannerHelper.scanBarcode(
                                            context = context,
                                            onSuccess = { code ->
                                                viewModel.onBarcodeScannedInForm(code)
                                            },
                                            onError = { err ->
                                                viewModel.showMessage(err)
                                            }
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("form_scan_barcode_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Code-barres", style = MaterialTheme.typography.labelMedium)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                FilledTonalIconButton(
                                    onClick = { viewModel.openAddLineDialog() },
                                    modifier = Modifier.testTag("add_product_line_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Ajouter"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bandeau de paramétrage en direct lors d'une conversion depuis Facture/Bon Fournisseur (ou sur Facture Client)
                        if (formState.convertedFromSupplierDocId.isNotBlank() ||
                            (formState.typeOperation == OperationType.FACTURE && formState.lignes.isNotEmpty())
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEEF2FF),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_conversion_pricing_banner")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Autorenew,
                                                contentDescription = null,
                                                tint = Color(0xFF4338CA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (formState.convertedFromSupplierDocId.isNotBlank())
                                                    "Converti depuis doc fournisseur N° ${formState.convertedFromSupplierDocId}"
                                                else
                                                    "Calcul des prix de vente (Marge / Catalogue)",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF312E81)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = !formState.useCatalogSalePrice,
                                            onClick = {
                                                viewModel.updateDraftMarginPricing(
                                                    marginCoeffText = formState.marginCoefficientText,
                                                    useCatalogPrice = false
                                                )
                                            },
                                            label = {
                                                Text("Coeff. marge sur prix d'achat", style = MaterialTheme.typography.labelSmall)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = formState.useCatalogSalePrice,
                                            onClick = {
                                                viewModel.updateDraftMarginPricing(
                                                    marginCoeffText = formState.marginCoefficientText,
                                                    useCatalogPrice = true
                                                )
                                            },
                                            label = {
                                                Text("Prix de vente catalogue", style = MaterialTheme.typography.labelSmall)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (!formState.useCatalogSalePrice) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = formState.marginCoefficientText,
                                                onValueChange = { newText ->
                                                    viewModel.updateDraftMarginPricing(
                                                        marginCoeffText = newText,
                                                        useCatalogPrice = false
                                                    )
                                                },
                                                label = { Text("Coefficient de marge (ex: 1.25)") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("form_margin_coefficient_input")
                                            )
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                listOf("1.15", "1.20", "1.25", "1.30", "1.50").forEach { preset ->
                                                    FilterChip(
                                                        selected = formState.marginCoefficientText == preset,
                                                        onClick = {
                                                            viewModel.updateDraftMarginPricing(
                                                                marginCoeffText = preset,
                                                                useCatalogPrice = false
                                                            )
                                                        },
                                                        label = { Text("x$preset", style = MaterialTheme.typography.labelSmall) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Barre de saisie rapide avec suggestions en temps réel (ex: tapez "disj" -> liste de tous les disjoncteurs)
                        OutlinedTextField(
                            value = inlineArticleQuery,
                            onValueChange = { inlineArticleQuery = it },
                            label = { Text("Rechercher & ajouter un article (ex: disj...)") },
                            placeholder = { Text("Tapez « disj » pour voir tous les disjoncteurs...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (inlineArticleQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { inlineArticleQuery = "" },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Effacer",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inline_article_search_input")
                        )

                        if (inlineArticleQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .testTag("inline_article_suggestions_list")
                            ) {
                                if (inlineMatchingArticles.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val customName = inlineArticleQuery.trim()
                                                inlineArticleQuery = ""
                                                viewModel.openAddLineDialog()
                                                viewModel.onLineProduitChanged(customName)
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Aucun article trouvé pour « $inlineArticleQuery » • Toucher pour saisir manuellement",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .verticalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Suggestions d'articles pour « ${inlineArticleQuery.trim()} » (${inlineMatchingArticles.size}) :",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                        inlineMatchingArticles.forEachIndexed { idx, prod ->
                                            val unitPriceToShow: Double = if (formState.typeOperation.isPurchaseDocument && (prod.prixAchat ?: 0.0) > 0.0) {
                                                prod.prixAchat ?: 0.0
                                            } else {
                                                prod.defaultUnitPrice ?: prod.prixVente ?: 0.0
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        inlineArticleQuery = ""
                                                        viewModel.openAddLineDialog()
                                                        viewModel.onLineProduitChanged(prod.name)
                                                        if (unitPriceToShow > 0.0 && !prod.isArticleEclate) {
                                                            viewModel.onLinePrixUnitaireChanged(CurrencyUtils.formatQty(unitPriceToShow))
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = prod.name,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    val details = buildList {
                                                        if (prod.reference.isNotBlank()) add("Réf: ${prod.reference}")
                                                        if (prod.category.isNotBlank()) add("Famille: ${prod.category}")
                                                        if (prod.marque.isNotBlank()) add("Marque: ${prod.marque}")
                                                        add("Stock: ${CurrencyUtils.formatQty(prod.stockActuel)} ${prod.unit}")
                                                    }
                                                    Text(
                                                        text = details.joinToString(" • "),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = CurrencyUtils.formatDa(unitPriceToShow),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            if (idx < inlineMatchingArticles.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Sélection rapide des Articles Kits (1 ligne pack) et Articles Éclatés (multi-lignes composants)
                        val compositeProducts = remember(catalogProducts) {
                            catalogProducts.filter { it.isArticleKit || it.isArticleEclate }
                        }
                        if (compositeProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Insertion rapide — Article Kit (1 ligne) & Article Éclaté (multi-lignes) :",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                compositeProducts.forEach { compProd ->
                                    val isKit = compProd.isArticleKit
                                    val chipColor = if (isKit) Color(0xFF0D9488) else Color(0xFF7C3AED)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = chipColor.copy(alpha = 0.12f),
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.onBarcodeScannedInForm(compProd.name)
                                            }
                                            .testTag("quick_composite_${compProd.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isKit) "🎁 ${compProd.name} (Kit 1 ligne)" else "⚡ ${compProd.name} (Éclater)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = chipColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // App Formula Virtual Column: SUM(Quantite * Prix_Unitaire)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Produits (App Formula) :",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = CurrencyUtils.formatDa(formState.calculatedTotalProduits),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // List of added lines
                        if (formState.lignes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(vertical = 20.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Aucun article ajouté à ce bon",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Touchez « Ajouter » pour enregistrer des articles",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            val groupedDraftLines = remember(formState.lignes) {
                                groupLignesByParent(formState.lignes)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                groupedDraftLines.forEach { group ->
                                    when (group) {
                                        is GroupedOperationLine.ExplodedParentGroup -> {
                                            // Carte Hiérarchique : Article Éclaté (PARENT) + ses composés (FILS)
                                            Card(
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0xFF7C3AED).copy(alpha = 0.08f)
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    // En-tête PARENT (Nom de l'Article Éclaté)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFF7C3AED).copy(alpha = 0.18f)
                                                                ) {
                                                                    Text(
                                                                        text = "ARTICLE ÉCLATÉ • PARENT",
                                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.ExtraBold
                                                                        ),
                                                                        color = Color(0xFF7C3AED),
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(3.dp))
                                                            Text(
                                                                text = "${group.displayIndex}. ⚡ ${group.parentName}",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = Color(0xFF5B21B6)
                                                            )
                                                            Text(
                                                                text = "${group.children.size} composé(s) (fils) • Somme : ${CurrencyUtils.formatDa(group.totalHt)}",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                                color = Color(0xFF6D28D9)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = { viewModel.deleteParentGroupFromDraft(group.parentName) },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.DeleteOutline,
                                                                contentDescription = "Supprimer le parent et ses composés",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    HorizontalDivider(color = Color(0xFF7C3AED).copy(alpha = 0.2f))
                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    // Liste des FILS (Composés) modifiables et supprimables individuellement
                                                    Column(
                                                        modifier = Modifier.padding(start = 10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        group.children.forEachIndexed { cIdx, child ->
                                                            val branch = if (cIdx == group.children.lastIndex) "└─" else "├─"
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = MaterialTheme.colorScheme.surface,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                                            Text(
                                                                                text = "$branch ${group.displayIndex}.${cIdx + 1} ${child.produit}",
                                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                                                            )
                                                                            Spacer(modifier = Modifier.width(6.dp))
                                                                            Surface(
                                                                                shape = RoundedCornerShape(4.dp),
                                                                                color = Color(0xFF7C3AED).copy(alpha = 0.12f)
                                                                            ) {
                                                                                Text(
                                                                                    text = "Fils",
                                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                                        fontSize = 9.sp,
                                                                                        fontWeight = FontWeight.Bold
                                                                                    ),
                                                                                    color = Color(0xFF7C3AED),
                                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                        val puStr = child.prixUnitaire?.let { CurrencyUtils.formatDa(it) } ?: "PU Facultatif"
                                                                        Text(
                                                                            text = "   Qté: ${CurrencyUtils.formatQty(child.quantite)} x $puStr • Sous-total: ${CurrencyUtils.formatDa(child.totalLigne)}",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                    }

                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        IconButton(
                                                                            onClick = { lineToReplaceInGrid = child },
                                                                            modifier = Modifier
                                                                                .size(30.dp)
                                                                                .testTag("replace_line_btn_${child.id}")
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Default.SwapHoriz,
                                                                                contentDescription = "Remplacer composé",
                                                                                tint = Color(0xFF0284C7),
                                                                                modifier = Modifier.size(16.dp)
                                                                            )
                                                                        }
                                                                        IconButton(
                                                                            onClick = { viewModel.openEditLineDialog(child) },
                                                                            modifier = Modifier.size(30.dp)
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Default.Edit,
                                                                                contentDescription = "Modifier composé",
                                                                                tint = MaterialTheme.colorScheme.primary,
                                                                                modifier = Modifier.size(16.dp)
                                                                            )
                                                                        }
                                                                        IconButton(
                                                                            onClick = { viewModel.deleteLineFromDraft(child.id) },
                                                                            modifier = Modifier.size(30.dp)
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Default.DeleteOutline,
                                                                                contentDescription = "Supprimer composé",
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
                                            }
                                        }

                                        is GroupedOperationLine.StandardLine -> {
                                            val ligne = group.ligne
                                            Card(
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${group.displayIndex}. ${ligne.produit}",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                        )
                                                        val puStr = ligne.prixUnitaire?.let { CurrencyUtils.formatDa(it) } ?: "PU Facultatif"
                                                        Text(
                                                            text = "Qté: ${CurrencyUtils.formatQty(ligne.quantite)} x $puStr",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "Sous-total: ${CurrencyUtils.formatDa(ligne.totalLigne)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )

                                                        // Si l'article est un Article Kit (Nomenclature commerciale) ou un Article Éclaté
                                                        val matchedCat = catalogProducts.find {
                                                            it.name.trim().equals(ligne.produit.trim(), ignoreCase = true)
                                                        }
                                                        if (matchedCat?.isArticleKit == true) {
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFF0D9488).copy(alpha = 0.12f)
                                                            ) {
                                                                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
                                                                    Text(
                                                                        text = "🎁 Article Kit (Nomenclature commerciale — 1 seule ligne)",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = Color(0xFF0D9488)
                                                                    )
                                                                    Text(
                                                                        text = "Prix pack fixe • Rigide (impossible de retirer un composant sans éclater le kit) • Déstockage auto des composants en tâche de fond",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                            TextButton(
                                                                onClick = { viewModel.explodeArticleInDraft(ligne.id) },
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                                modifier = Modifier.testTag("explode_kit_button_${ligne.id}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Bolt,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF0D9488),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = "Éclater le kit (pour modifier/retirer un composant)",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = Color(0xFF0D9488)
                                                                )
                                                            }
                                                        } else if (matchedCat?.isArticleEclate == true) {
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFF7C3AED).copy(alpha = 0.12f)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "⚡ Article Éclaté (Kit éclaté)",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = Color(0xFF7C3AED)
                                                                    )
                                                                }
                                                            }
                                                            TextButton(
                                                                onClick = { viewModel.explodeArticleInDraft(ligne.id) },
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                                modifier = Modifier.testTag("explode_kit_button_${ligne.id}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Bolt,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF7C3AED),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = "Remplacer par ses composants",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = Color(0xFF7C3AED)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        // Contrôle rapide de quantité (- / +) dans la grille
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            IconButton(
                                                                onClick = {
                                                                    if (ligne.quantite > 1.0) {
                                                                        viewModel.updateLineQuantityInDraft(ligne.id, ligne.quantite - 1.0)
                                                                    }
                                                                },
                                                                modifier = Modifier.size(26.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Remove,
                                                                    contentDescription = "Diminuer quantité",
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = CurrencyUtils.formatQty(ligne.quantite),
                                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                            )
                                                            IconButton(
                                                                onClick = {
                                                                    viewModel.updateLineQuantityInDraft(ligne.id, ligne.quantite + 1.0)
                                                                },
                                                                modifier = Modifier.size(26.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Add,
                                                                    contentDescription = "Augmenter quantité",
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            TextButton(
                                                                onClick = { lineToReplaceInGrid = ligne },
                                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                                modifier = Modifier.testTag("replace_line_btn_${ligne.id}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.SwapHoriz,
                                                                    contentDescription = "Remplacer l'article",
                                                                    tint = Color(0xFF0284C7),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(3.dp))
                                                                Text(
                                                                    text = "Remplacer",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = Color(0xFF0284C7)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { viewModel.openEditLineDialog(ligne) },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = "Modifier",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { viewModel.deleteLineFromDraft(ligne.id) },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.DeleteOutline,
                                                                    contentDescription = "Supprimer",
                                                                    tint = MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(18.dp)
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
            }

            // Indicator when products section is hidden due to Show_If rule
            if (!formState.showProductLines) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Règle AppSheet Show_If active : La section produits est automatiquement masquée pour l'opération Règlement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Spacer(modifier = Modifier.height(14.dp))

                // 2. bis : Section TVA et Fiscalité du Document (Facture & Bon de commande)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tva_section_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Percent,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2. Fiscalité & TVA",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (formState.appliquerTva) "TVA Active" else "Sans TVA (HT)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (formState.appliquerTva) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = formState.appliquerTva,
                                    onCheckedChange = viewModel::onAppliquerTvaChanged,
                                    modifier = Modifier.testTag("apply_tva_switch")
                                )
                            }
                        }

                        if (formState.appliquerTva) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Taux de TVA applicable :",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val tvaRates = listOf(19.0 to "19% (Standard)", 9.0 to "9% (Réduit)", 0.0 to "0% (Exonéré)")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tvaRates.forEach { (rate, label) ->
                                    val isSelected = formState.tauxTva == rate
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.onTauxTvaChanged(rate) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("tva_rate_${rate.toInt()}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Synthèse TVA
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Brut HT :", style = MaterialTheme.typography.bodySmall)
                                        Text(CurrencyUtils.formatDa(formState.calculatedTotalProduits), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Montant TVA (${formState.tauxTva}%) :", style = MaterialTheme.typography.bodySmall)
                                        Text(CurrencyUtils.formatDa(formState.montantTva), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Net TTC :", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(CurrencyUtils.formatDa(formState.calculatedTotalTtc), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Statut du document (Validé, Brouillon, Payé)
                        Text(
                            text = "Statut du document :",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val statusList = listOf("Validé", "Brouillon", "En attente", "Payé")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            statusList.forEach { st ->
                                val isSelected = formState.statut == st
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onStatutChanged(st) },
                                    label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Montant Versé & Modalités de Paiement
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val sectionTitle = if (formState.typeOperation == OperationType.REGLEMENT)
                        "3. Montant du Règlement *"
                    else
                        "3. Règlement / Acompte (Facultatif)"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Montant Versé input
                    OutlinedTextField(
                        value = formState.montantVerseText,
                        onValueChange = viewModel::onMontantVerseChanged,
                        label = { Text("Montant Versé en DA") },
                        placeholder = { Text("0.00") },
                        trailingIcon = { Text("DA", modifier = Modifier.padding(end = 12.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("montant_verse_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    // If it has products, display Reste à payer calculation
                    if (formState.showProductLines && formState.montantVerseNumeric > 0) {
                        val reste = formState.resteAPayer
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reste à payer : ${CurrencyUtils.formatDa(reste)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (reste > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode de règlement selector chips
                    Text(
                        text = "Mode de paiement :",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val paymentModes = listOf("Espèces", "Chèque", "Virement", "CCP", "Crédit")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentModes.forEach { mode ->
                            val isSelected = formState.modeReglement.equals(mode, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.onModeReglementChanged(mode) }
                                    .testTag("payment_mode_${mode.lowercase()}")
                            ) {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Notes / Remarques de terrain
                    OutlinedTextField(
                        value = formState.notes,
                        onValueChange = viewModel::onNotesChanged,
                        label = { Text("Notes / Observations terrain (Facultatif)") },
                        placeholder = { Text("Ex: Livré au dépôt, chèque n° 124578, etc.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("operation_notes_input"),
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    val draftConversionNote = remember(formState.notes, formState.convertedFromSupplierDocId) {
                        PdfGenerator.extractConversionNote(formState.notes)
                            ?: formState.convertedFromSupplierDocId?.let { "Document converti à partir de la facture N° $it" }
                    }
                    if (!draftConversionNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_bottom_conversion_note_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = draftConversionNote,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF312E81)
                                    )
                                    Text(
                                        text = if (companySettings.pdfShowConversionNote)
                                            "Note en bas du document : affichée lors de la génération PDF (désactivez pour masquer)"
                                        else
                                            "Note masquée lors de la génération PDF",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4338CA)
                                    )
                                }
                                Switch(
                                    checked = companySettings.pdfShowConversionNote,
                                    onCheckedChange = { enabled ->
                                        viewModel.updateCompanySettings(
                                            companySettings.copy(pdfShowConversionNote = enabled)
                                        )
                                    },
                                    modifier = Modifier.testTag("form_conversion_note_pdf_switch")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
