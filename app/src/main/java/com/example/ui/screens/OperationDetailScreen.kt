package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.example.data.model.ArticleCompositionEntity
import com.example.data.model.DocumentPermissionCategory
import com.example.data.model.GroupedOperationLine
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerEntity
import com.example.data.model.groupLignesByParent
import com.example.data.model.permissionCategory
import com.example.ui.components.OperationBadge
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.ExportUtils
import com.example.util.PdfGenerator
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationDetailScreen(
    operationId: String,
    viewModel: OperationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allOperations by viewModel.allOperations.collectAsStateWithLifecycle()
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    val compositeArticles by viewModel.articlesEclatesWithComposition.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val stampVersion by viewModel.stampVersion.collectAsStateWithLifecycle()
    val logoVersion by viewModel.logoVersion.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val item = allOperations.find {
        it.operation.id == operationId ||
            it.operation.id.trim().equals(operationId.trim(), ignoreCase = true)
    }

    val stampBitmap = remember(stampVersion, logoVersion, companySettings) {
        PdfGenerator.loadOrCreateWetStampBitmap(context, companySettings)?.asImageBitmap()
    }

    val stampPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.saveCustomStampPng(context, it) }
    }

    val canModifyDoc = item?.operation?.let { currentEmployee.canModifyOperation(it.typeOperation) } ?: false
    val canDeleteDoc = item?.operation?.let { currentEmployee.canDeleteOperation(it.typeOperation) } ?: false
    val canModifyCreance = currentEmployee.canModifyCreance

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val kitCompositionsMap = remember(compositeArticles) {
        compositeArticles
            .filter { it.product.isArticleKit }
            .associate { it.product.name.trim().lowercase(Locale.FRENCH) to it.composantes }
    }
    val eclateCompositionsMap = remember(compositeArticles) {
        compositeArticles
            .filter { it.product.isArticleEclate }
            .associate { it.product.name.trim().lowercase(Locale.FRENCH) to it.composantes }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showConvertToFactureDialog by remember { mutableStateOf(false) }
    var showConvertToBlDialog by remember { mutableStateOf(false) }
    var showEditCreanceVersementDialog by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateBack()
    }

    if (showDeleteConfirm && item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer l'opération ?") },
            text = { Text("Voulez-vous vraiment supprimer définitivement « ${item.operation.id} » et ses lignes associées ?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteOperation(item.operation.id)
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue de conversion Bon de Livraison -> Facture
    if (showConvertToFactureDialog && item != null) {
        ConvertBlToFactureDialog(
            source = item,
            viewModel = viewModel,
            onDismiss = { showConvertToFactureDialog = false }
        )
    }

    // Dialogue de conversion Devis -> Bon de Livraison (BL)
    if (showConvertToBlDialog && item != null) {
        ConvertDevisToBlDialog(
            source = item,
            viewModel = viewModel,
            onDismiss = { showConvertToBlDialog = false }
        )
    }

    // Dialogue de modification directe de la créance / montant versé sur ce document
    if (showEditCreanceVersementDialog && item != null) {
        var versementInput by remember(item.operation.montantVerse) {
            mutableStateOf(CurrencyUtils.formatQty(item.operation.montantVerse))
        }
        AlertDialog(
            onDismissRequest = { showEditCreanceVersementDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Modifier Créance / Montant Versé") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Document : ${item.operation.id} (${item.operation.clientFournisseur})\nTotal TTC : ${CurrencyUtils.formatDa(item.totalTtc)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = versementInput,
                        onValueChange = { versementInput = it },
                        label = { Text("Montant versé (DA)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val parsed = CurrencyUtils.parseAmount(versementInput)
                    val newReste = (item.totalTtc - parsed).coerceAtLeast(0.0)
                    Text(
                        text = "Nouvelle créance restante sur ce document : ${CurrencyUtils.formatDa(newReste)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (newReste > 0) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newVerse = CurrencyUtils.parseAmount(versementInput).coerceAtLeast(0.0)
                        viewModel.modifyOperationCreanceVersement(item, newVerse)
                        showEditCreanceVersementDialog = false
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCreanceVersementDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    val matchedPartner = remember(item, clients, suppliers) {
        item?.let { current ->
            (clients + suppliers).find {
                it.name.trim().equals(current.operation.clientFournisseur.trim(), ignoreCase = true)
            }
        }
    }

    // Modal Confirmation, Aperçu A4 & Actions PDF (Exporter / Partager avec Client)
    if (showPdfDialog && generatedPdfFile != null && item != null) {
        PdfDocumentActionDialog(
            item = item,
            pdfFile = generatedPdfFile!!,
            settings = companySettings,
            partner = matchedPartner,
            kitCompositionsMap = kitCompositionsMap,
            onUpdateSettings = { updated -> viewModel.updateCompanySettings(updated) },
            onDismiss = { showPdfDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item?.operation?.id ?: "Détail",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    if (item != null) {
                        // Action rapide Convertir en Facture dans la barre supérieure pour un BL
                        if (item.operation.typeOperation == OperationType.BL_CLIENT ||
                            item.operation.typeOperation == OperationType.DEVIS_CLIENT ||
                            item.operation.typeOperation == OperationType.BON_COMMANDE
                        ) {
                            IconButton(
                                onClick = { showConvertToFactureDialog = true },
                                modifier = Modifier.testTag("detail_convert_top_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Convertir en Facture",
                                    tint = Color(0xFF059669)
                                )
                            }
                        }

                        // Imprimer directement en A4
                        IconButton(
                            onClick = {
                                val file = PdfGenerator.generatePdf(
                                    context = context,
                                    item = item,
                                    settings = companySettings,
                                    partner = matchedPartner,
                                    kitCompositionsMap = kitCompositionsMap
                                )
                                generatedPdfFile = file
                                PdfGenerator.printPdfA4(
                                    context = context,
                                    file = file,
                                    jobName = "${item.operation.typeOperation.shortCode}_${item.operation.id}_A4"
                                )
                            },
                            modifier = Modifier.testTag("detail_print_a4_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Imprimer Bon A4",
                                tint = Color(0xFF0284C7)
                            )
                        }

                        // Générer & Exporter PDF Officiel
                        IconButton(
                            onClick = {
                                val file = PdfGenerator.generatePdf(
                                    context = context,
                                    item = item,
                                    settings = companySettings,
                                    partner = matchedPartner,
                                    kitCompositionsMap = kitCompositionsMap
                                )
                                generatedPdfFile = file
                                showPdfDialog = true
                            },
                            modifier = Modifier.testTag("detail_pdf_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Générer et Exporter PDF",
                                tint = Color(0xFF16A34A)
                            )
                        }

                        // Partager / Bordereau de terrain
                        IconButton(
                            onClick = {
                                val slip = ExportUtils.generateSlipText(item)
                                ExportUtils.shareText(
                                    context = context,
                                    title = "Bordereau_${item.operation.id}",
                                    text = slip
                                )
                            },
                            modifier = Modifier.testTag("detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Partager Bordereau",
                                tint = Color(0xFF16A34A)
                            )
                        }

                        // Modifier (protégé par droit Modification Bon / Facture / Créance)
                        IconButton(
                            onClick = { viewModel.editOperation(item) },
                            modifier = Modifier.testTag("detail_edit_button")
                        ) {
                            Icon(
                                imageVector = if (canModifyDoc) Icons.Default.Edit else Icons.Default.Lock,
                                contentDescription = "Modifier",
                                tint = if (canModifyDoc) Color(0xFFFFA726) else MaterialTheme.colorScheme.outline
                            )
                        }

                        // Supprimer (protégé par droit Suppression Bon / Facture / Créance)
                        IconButton(
                            onClick = {
                                if (canDeleteDoc) {
                                    showDeleteConfirm = true
                                } else {
                                    viewModel.showMessage(
                                        currentEmployee.permissionDeniedMessage(
                                            action = "supprimer",
                                            category = item.operation.typeOperation.permissionCategory()
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(
                                imageVector = if (canDeleteDoc) Icons.Default.Delete else Icons.Default.Lock,
                                contentDescription = "Supprimer",
                                tint = if (canDeleteDoc) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Opération introuvable ou supprimée.")
            }
            return@Scaffold
        }

        val op = item.operation
        val conversionNote = remember(op.notes) { PdfGenerator.extractConversionNote(op.notes) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bandeau de contrôle des droits employé (Modification / Suppression de Bon, Facture, Créance)
            val docCat = op.typeOperation.permissionCategory()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Employé : ${currentEmployee.name} (${currentEmployee.role.label})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = docCat.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.editOperation(item) },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (canModifyDoc) Color(0xFFFFA726) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (canModifyDoc) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (canModifyDoc) Icons.Default.Edit else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (canModifyDoc) "Modifier" else "Modif. verrouillée",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        if (op.typeOperation != OperationType.DEVIS_CLIENT && op.typeOperation != OperationType.DEVIS_FOURNISSEUR) {
                            FilledTonalButton(
                                onClick = {
                                    if (canModifyCreance) {
                                        showEditCreanceVersementDialog = true
                                    } else {
                                        viewModel.showMessage(
                                            currentEmployee.permissionDeniedMessage(
                                                action = "modifier",
                                                category = DocumentPermissionCategory.CREANCE
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (canModifyCreance) Color(0xFFFFA726) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (canModifyCreance) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (canModifyCreance) Icons.Default.Payments else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (canModifyCreance) "Modif. Créance" else "Créance verr.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                if (canDeleteDoc) {
                                    showDeleteConfirm = true
                                } else {
                                    viewModel.showMessage(
                                        currentEmployee.permissionDeniedMessage(
                                            action = "supprimer",
                                            category = docCat
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (canDeleteDoc) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (canDeleteDoc) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (canDeleteDoc) Icons.Default.Delete else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (canDeleteDoc) "Supprimer" else "Suppr. verr.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        OperationBadge(type = op.typeOperation)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = DateUtils.formatDateTime(op.dateTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (op.typeOperation.isPurchaseDocument) Icons.Default.Business else Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                val role = if (op.typeOperation.isPurchaseDocument) "Fournisseur" else "Client"
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = op.clientFournisseur,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                )
                            }
                        }

                        val matchedPartner = (clients + suppliers).find {
                            it.name.trim().equals(op.clientFournisseur.trim(), ignoreCase = true)
                        }

                        if (matchedPartner != null) {
                            OutlinedButton(
                                onClick = { viewModel.navigateTo(AppScreen.PartnerDetail(matchedPartner.id)) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("detail_view_partner_btn")
                            ) {
                                Text("Fiche & Historique", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    val matchedPartnerForCoords = (clients + suppliers).find {
                        it.name.trim().equals(op.clientFournisseur.trim(), ignoreCase = true)
                    }
                    if (matchedPartnerForCoords != null) {
                        val partnerCoords = listOfNotNull(
                            matchedPartnerForCoords.rc.ifBlank { null }?.let { "RC N°: $it" },
                            matchedPartnerForCoords.articleImposition.ifBlank { null }?.let { "N° article: $it" },
                            matchedPartnerForCoords.nif.ifBlank { null }?.let { "NIF N°: $it" },
                            matchedPartnerForCoords.nis.ifBlank { null }?.let { "NIS N°: $it" },
                            matchedPartnerForCoords.nin.ifBlank { null }?.let { "NIN N°: $it" }
                        ).joinToString(" • ")
                        if (partnerCoords.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = partnerCoords,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Carte Action : Conversion d'une Facture Fournisseur (ou Bon Fournisseur) en Facture Client ou Bon de Livraison Client
            val isSupplierDocForConversion = op.typeOperation == OperationType.FACTURE_FOURNISSEUR ||
                op.typeOperation == OperationType.BL_FOURNISSEUR ||
                op.typeOperation == OperationType.DEVIS_FOURNISSEUR ||
                (op.typeOperation == OperationType.FACTURE && suppliers.any {
                    it.name.trim().equals(op.clientFournisseur.trim(), ignoreCase = true)
                })

            if (isSupplierDocForConversion) {
                var marginInput by remember(companySettings.defaultMarginCoefficient) {
                    val c = companySettings.defaultMarginCoefficient
                    mutableStateOf(
                        if (c % 1.0 == 0.0) c.toInt().toString()
                        else String.format(Locale.US, "%.2f", c).trimEnd('0').trimEnd('.')
                    )
                }
                var useCatalogPrice by remember(companySettings.useCatalogSalePriceOnConvert) {
                    mutableStateOf(companySettings.useCatalogSalePriceOnConvert)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_supplier_conversion_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = Color(0xFF4338CA),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Convertir cette Facture / Bon Fournisseur",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF312E81)
                                    )
                                    Text(
                                        text = "Copie les ${item.lignes.size} article(s) (réf, désignation, qté) avec la date du jour et ouvre l'éditeur client",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4338CA)
                                    )
                                }
                            }
                        }

                        // Mode de calcul du prix de vente pour la conversion
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Tarification client lors de la conversion :",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF1E1B4B)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = !useCatalogPrice,
                                        onClick = { useCatalogPrice = false },
                                        label = {
                                            Text(
                                                "Coeff. de marge sur prix d'achat",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = useCatalogPrice,
                                        onClick = { useCatalogPrice = true },
                                        label = {
                                            Text(
                                                "Prix de vente du catalogue",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (!useCatalogPrice) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = marginInput,
                                            onValueChange = { marginInput = it },
                                            label = { Text("Coeff. de marge (ex: 1.25 = +25%)") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("supplier_convert_margin_input")
                                        )
                                        val parsedCoeff = marginInput.replace(',', '.').toDoubleOrNull() ?: 1.0
                                        val pctGain = ((parsedCoeff - 1.0) * 100.0)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = String.format(Locale.FRENCH, "%+.0f%%", pctGain),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF166534),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("1.10" to "+10%", "1.15" to "+15%", "1.20" to "+20%", "1.25" to "+25%", "1.30" to "+30%", "1.50" to "+50%").forEach { (coeffVal, pctLabel) ->
                                            FilterChip(
                                                selected = marginInput.replace(',', '.') == coeffVal,
                                                onClick = { marginInput = coeffVal },
                                                label = { Text("x$coeffVal ($pctLabel)", style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Les prix de vente seront récupérés depuis votre catalogue produit (avec repli sur le coefficient de marge si un article n'a pas de prix catalogue).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // Deux boutons d'action demandés : Convertir en Facture Client & Convertir en Bon de Livraison
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val coeff = marginInput.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
                                        ?: companySettings.defaultMarginCoefficient
                                    viewModel.convertSupplierDocToClientFactureInEditor(
                                        source = item,
                                        marginCoefficient = coeff,
                                        useCatalogSalePrice = useCatalogPrice
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("convert_supplier_to_client_facture_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Convertir en Facture Client",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    val coeff = marginInput.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
                                        ?: companySettings.defaultMarginCoefficient
                                    viewModel.convertSupplierDocToClientBlInEditor(
                                        source = item,
                                        marginCoefficient = coeff,
                                        useCatalogSalePrice = useCatalogPrice
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("convert_supplier_to_client_bl_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Convertir en Bon de Livraison",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Carte Action : Convertir Devis (Client ou Fournisseur) en Bon de Livraison (BL)
            if (op.typeOperation == OperationType.DEVIS_CLIENT ||
                op.typeOperation == OperationType.DEVIS_FOURNISSEUR
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_convert_to_bl_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                val blTitle = if (op.typeOperation == OperationType.DEVIS_FOURNISSEUR)
                                    "Convertir ce Devis en Bon Fournisseur (BL)"
                                else
                                    "Convertir ce Devis en Bon de Livraison (BL)"
                                Text(
                                    text = blTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF075985)
                                )
                                Text(
                                    text = "Reprend tous les articles, le tiers et met à jour le stock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF0369A1)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showConvertToBlDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("detail_convert_to_bl_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Convertir en BL",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Carte Action : Convertir Bon de Livraison (ou Devis / BC) en Facture
            if (op.typeOperation == OperationType.BL_CLIENT ||
                op.typeOperation == OperationType.DEVIS_CLIENT ||
                op.typeOperation == OperationType.BON_COMMANDE
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_convert_to_facture_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                val titleText = when (op.typeOperation) {
                                    OperationType.BL_CLIENT -> "Convertir ce BL en Facture"
                                    OperationType.DEVIS_CLIENT -> "Convertir ce Devis en Facture"
                                    else -> "Convertir ce BC en Facture"
                                }
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "Reprend tous les articles, le client et applique la TVA si souhaité",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showConvertToFactureDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("detail_convert_to_facture_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Convertir",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // PDF Generation & Client Sharing Action Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_pdf_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Générer & Exporter ${op.typeOperation.label} en PDF",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Document A4 officiel avec Cachet humide & Signature PNG, TVA, montant en lettres & partage client",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    // Option pour masquer ou afficher la note "Document converti à partir de la facture N°..." lors de la génération du PDF
                    if (!conversionNote.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("detail_pdf_conversion_note_toggle_card")
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
                                        text = conversionNote,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF312E81)
                                    )
                                    Text(
                                        text = if (companySettings.pdfShowConversionNote)
                                            "Note affichée en bas du PDF (désactivez pour masquer sur le PDF)"
                                        else
                                            "Note masquée lors de la génération du PDF",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (companySettings.pdfShowConversionNote) Color(0xFF4338CA) else Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = companySettings.pdfShowConversionNote,
                                    onCheckedChange = { enabled ->
                                        viewModel.updateCompanySettings(
                                            companySettings.copy(pdfShowConversionNote = enabled)
                                        )
                                    },
                                    modifier = Modifier.testTag("detail_pdf_conversion_note_switch")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val file = PdfGenerator.generatePdf(
                                    context = context,
                                    item = item,
                                    settings = companySettings,
                                    partner = matchedPartner,
                                    kitCompositionsMap = kitCompositionsMap
                                )
                                generatedPdfFile = file
                                showPdfDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detail_generate_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aperçu & Exporter PDF", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }

                        FilledTonalButton(
                            onClick = {
                                val file = PdfGenerator.generatePdf(
                                    context = context,
                                    item = item,
                                    settings = companySettings,
                                    partner = matchedPartner,
                                    kitCompositionsMap = kitCompositionsMap
                                )
                                generatedPdfFile = file
                                val subject = "${op.typeOperation.label} ${op.id} - ${op.clientFournisseur}"
                                PdfGenerator.sharePdf(context, file, subject)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detail_quick_share_pdf_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partager au Client", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Button(
                        onClick = {
                            val file = PdfGenerator.generatePdf(
                                context = context,
                                item = item,
                                settings = companySettings,
                                partner = matchedPartner,
                                kitCompositionsMap = kitCompositionsMap
                            )
                            generatedPdfFile = file
                            PdfGenerator.printPdfA4(
                                context = context,
                                file = file,
                                jobName = "${op.typeOperation.shortCode}_${op.id}_A4"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_print_a4_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Imprimer ce ${op.typeOperation.label} en A4 (Logo HD & Cachet Humide)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val studioTab = when (op.typeOperation) {
                                OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> 0
                                OperationType.BL_CLIENT, OperationType.BON_COMMANDE, OperationType.BL_FOURNISSEUR -> 1
                                else -> 2
                            }
                            viewModel.navigateTo(AppScreen.PdfStudio(studioTab))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_open_pdf_studio_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF92400E))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Logo, Coordonnées & Réorganisation PDF",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            // Financial Summary Card with TVA Support
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Synthèse Financière (Dinars Algériens)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (op.typeOperation != OperationType.REGLEMENT) {
                        // Total Brut HT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Brut HT :", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyUtils.formatDa(item.totalHt),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // TVA si appliquée
                        if (op.appliquerTva) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "TVA (${op.tauxTva}%) :", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = CurrencyUtils.formatDa(item.montantTva),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val ttcLabel = if (op.appliquerTva) "Total Net TTC :" else "Total Net à Payer :"
                            Text(
                                text = ttcLabel,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = CurrencyUtils.formatDa(item.totalTtc),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (op.montantVerse > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Acompte / Versé :", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = CurrencyUtils.formatDa(op.montantVerse),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Reste à payer :", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = CurrencyUtils.formatDa(item.resteAPayer),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (item.resteAPayer > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Montant Règlement :", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyUtils.formatDa(op.montantVerse),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Mode de règlement :", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = op.modeReglement,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val amountInWordsTarget = if (op.typeOperation == OperationType.REGLEMENT) op.montantVerse else item.totalTtc
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_arrete_somme_box")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = CurrencyUtils.formatArretePrefix(op.typeOperation),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.amountToFrenchWordsDa(amountInWordsTarget),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Product lines details (Table 2: 2_Lignes_Operation)
            if (op.typeOperation != OperationType.REGLEMENT) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Articles du Bon (${item.nombreArticles})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Text(
                                text = "Total Qté: ${CurrencyUtils.formatQty(item.quantiteTotale)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (item.lignes.isEmpty()) {
                            Text(
                                text = "Aucun article enregistré pour ce bon.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val groupedLines = remember(item.lignes, eclateCompositionsMap) {
                                groupLignesByParent(item.lignes, eclateCompositionsMap)
                            }
                            groupedLines.forEachIndexed { gIndex, group ->
                                Column {
                                    when (group) {
                                        is GroupedOperationLine.ExplodedParentGroup -> {
                                            // Bloc Hiérarchique : Article Éclaté (PARENT) + ses composés (FILS)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF7C3AED).copy(alpha = 0.08f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    // Ligne PARENT : Nom de l'Article Éclaté
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
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
                                                                            fontSize = 10.sp,
                                                                            fontWeight = FontWeight.ExtraBold
                                                                        ),
                                                                        color = Color(0xFF7C3AED),
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "${group.displayIndex}. ⚡ ${group.parentName}",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = Color(0xFF5B21B6)
                                                            )
                                                            Text(
                                                                text = "${group.children.size} composé(s) (fils) • Prix calculé par addition des composés",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }

                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(
                                                                text = "Total Parent",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color(0xFF7C3AED)
                                                            )
                                                            Text(
                                                                text = CurrencyUtils.formatDa(group.totalHt),
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                                color = Color(0xFF5B21B6)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    HorizontalDivider(color = Color(0xFF7C3AED).copy(alpha = 0.2f))
                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Text(
                                                        text = "Composés (Fils) de « ${group.parentName} » :",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color(0xFF6D28D9)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    // Liste indentée des FILS (Composés)
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 10.dp),
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
                                                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                                            Text(
                                                                                text = "$branch ${group.displayIndex}.${cIdx + 1} ${child.produit}",
                                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                                                color = MaterialTheme.colorScheme.onSurface
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
                                                                        val puStr = child.prixUnitaire?.let { CurrencyUtils.formatDa(it) } ?: "Facultatif"
                                                                        Text(
                                                                            text = "   Qté: ${CurrencyUtils.formatQty(child.quantite)} x $puStr",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                    }

                                                                    Text(
                                                                        text = CurrencyUtils.formatDa(child.totalLigne),
                                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        is GroupedOperationLine.StandardLine -> {
                                            val ligne = group.ligne
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${group.displayIndex}. ${ligne.produit}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                    )
                                                    val puStr = ligne.prixUnitaire?.let { CurrencyUtils.formatDa(it) } ?: "Facultatif"
                                                    Text(
                                                        text = "${CurrencyUtils.formatQty(ligne.quantite)} x $puStr",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    val kitComps = kitCompositionsMap[ligne.produit.trim().lowercase(Locale.FRENCH)]
                                                    if (!kitComps.isNullOrEmpty()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFF0D9488).copy(alpha = 0.10f)
                                                        ) {
                                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                                Text(
                                                                    text = "🎁 Article Kit (Nomenclature commerciale — Prix pack fixe)",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = Color(0xFF0D9488)
                                                                )
                                                                kitComps.forEach { comp ->
                                                                    Text(
                                                                        text = "↳ ${comp.componentName} : ${CurrencyUtils.formatQty(comp.quantite * ligne.quantite)} ${comp.unit} (déstocké en tâche de fond)",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = CurrencyUtils.formatDa(ligne.totalLigne),
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (gIndex < groupedLines.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Observations / Notes
            if (op.notes.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Notes / Remarques de terrain",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = op.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Cachet Humide HD (Format PNG) avec option d'agrandissement (sans signature stylo)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_stamp_signature_png_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cachet Humide HD (Format PNG)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Apposé sur ce ${op.typeOperation.label} (${op.id}) • Taille actuelle : ${companySettings.pdfStampScale}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = companySettings.pdfShowStampAndSignature,
                            onCheckedChange = { enabled ->
                                viewModel.updateCompanySettings(
                                    companySettings.copy(
                                        pdfShowStampAndSignature = enabled,
                                        pdfShowSignatures = if (enabled) true else companySettings.pdfShowSignatures
                                    )
                                )
                            },
                            modifier = Modifier.testTag("detail_stamp_signature_switch")
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
                                    .height(78.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (stampBitmap != null) {
                                    Image(
                                        bitmap = stampBitmap,
                                        contentDescription = "Cachet humide PNG HD",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            // Option d'agrandissement du Cachet Humide sur les Bons / Devis / Factures
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Agrandir le Cachet Humide : ${companySettings.pdfStampScale}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val next = (companySettings.pdfStampScale - 15).coerceAtLeast(80)
                                            viewModel.updateCompanySettings(companySettings.copy(pdfStampScale = next))
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp).testTag("detail_stamp_scale_minus_btn")
                                    ) {
                                        Icon(Icons.Default.ZoomOut, contentDescription = "Réduire cachet", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("-15%", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(
                                        onClick = {
                                            val next = (companySettings.pdfStampScale + 15).coerceAtMost(250)
                                            viewModel.updateCompanySettings(companySettings.copy(pdfStampScale = next))
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp).testTag("detail_stamp_scale_plus_btn")
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "Agrandir cachet", modifier = Modifier.size(14.dp))
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
                                        selected = companySettings.pdfStampScale == scaleVal,
                                        onClick = {
                                            viewModel.updateCompanySettings(companySettings.copy(pdfStampScale = scaleVal))
                                        },
                                        label = {
                                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                        },
                                        modifier = Modifier.testTag("detail_stamp_zoom_${scaleVal}")
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    stampPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_pick_stamp_png_btn")
                            ) {
                                Text("Importer mon Cachet Humide PNG HD", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Note en bas sur le document converti (BL ou Facture) : "Document converti à partir de la facture N°..."
            if (!conversionNote.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_conversion_bottom_note_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = Color(0xFF4338CA),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = conversionNote,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF312E81)
                                )
                                Text(
                                    text = if (companySettings.pdfShowConversionNote)
                                        "Affichée en bas du document PDF • Vous pouvez masquer cette note lors de la génération PDF"
                                    else
                                        "Masquée lors de la génération du PDF (visible uniquement dans l'application)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4338CA)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = companySettings.pdfShowConversionNote,
                            onCheckedChange = { enabled ->
                                viewModel.updateCompanySettings(
                                    companySettings.copy(pdfShowConversionNote = enabled)
                                )
                            },
                            modifier = Modifier.testTag("bottom_conversion_note_pdf_switch")
                        )
                    }
                }
            }

            // Quick Share Button
            OutlinedButton(
                onClick = {
                    val slip = ExportUtils.generateSlipText(item)
                    ExportUtils.shareText(
                        context = context,
                        title = "Bordereau_${item.operation.id}",
                        text = slip
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_slip_button")
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Partager Bordereau / Reçu de terrain")
            }

            FilledTonalButton(
                onClick = {
                    ExportUtils.sendExcelByEmail(context, listOf(item))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_single_op_excel_btn")
            ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Envoyer ce bon par Email (Excel)")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Dialogue partagé permettant de convertir un Bon de Livraison (ou Devis / BC) en Facture Client
 * avec configuration du numéro de facture, de la TVA (19% / 9%) et du remplacement du bon d'origine.
 */
@Composable
fun ConvertBlToFactureDialog(
    source: OperationWithLignes,
    viewModel: OperationViewModel,
    onDismiss: () -> Unit
) {
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val op = source.operation

    var factureId by remember(source) { mutableStateOf(viewModel.generateNextFactureId()) }
    var appliquerTva by remember(source, companySettings) {
        mutableStateOf(if (op.appliquerTva) true else companySettings.tvaEnabledByDefault)
    }
    var tauxTva by remember(source, companySettings) {
        mutableDoubleStateOf(if (op.appliquerTva) op.tauxTva else companySettings.defaultTvaRate)
    }
    var replaceOriginal by remember(source) {
        mutableStateOf(op.typeOperation == OperationType.BL_CLIENT)
    }

    val totalHt = source.totalHt
    val montantTva = if (appliquerTva) totalHt * (tauxTva / 100.0) else 0.0
    val totalTtc = totalHt + montantTva

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = null,
                tint = Color(0xFF059669),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Convertir en Facture",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Transformation de « ${op.id} » (${op.clientFournisseur}) en Facture Client :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = factureId,
                    onValueChange = { factureId = it },
                    label = { Text("N° de la nouvelle Facture") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("convert_dialog_facture_id_input")
                )

                // Option TVA
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Appliquer la TVA sur la facture",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Calcul automatique HT + TVA = TTC",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = appliquerTva,
                                onCheckedChange = { appliquerTva = it },
                                modifier = Modifier.testTag("convert_dialog_tva_switch")
                            )
                        }

                        if (appliquerTva) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(19.0 to "19%", 9.0 to "9%").forEach { (rate, label) ->
                                    FilterChip(
                                        selected = tauxTva == rate,
                                        onClick = { tauxTva = rate },
                                        label = { Text("TVA $label") }
                                    )
                                }
                            }
                        }
                    }
                }

                // Aperçu financier de la facture
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFDF5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Articles repris :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF065F46))
                            Text("${source.nombreArticles} ligne(s)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF065F46))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total HT :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF065F46))
                            Text(CurrencyUtils.formatDa(totalHt), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF065F46))
                        }
                        if (appliquerTva) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TVA ($tauxTva%) :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF065F46))
                                Text(CurrencyUtils.formatDa(montantTva), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF065F46))
                            }
                        }
                        HorizontalDivider(color = Color(0xFF059669).copy(alpha = 0.25f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Facture TTC :", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF065F46))
                            Text(CurrencyUtils.formatDa(totalTtc), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF059669))
                        }
                        Text(
                            text = "Arrêtée la présente facture à la somme de : ${CurrencyUtils.amountToFrenchWordsDa(totalTtc)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF065F46)
                        )
                    }
                }

                // Option Remplacer le BL d'origine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = replaceOriginal,
                        onCheckedChange = { replaceOriginal = it },
                        modifier = Modifier.testTag("convert_dialog_replace_checkbox")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Remplacer le BL d'origine par cette facture",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Évite de compter deux fois la créance client (la référence du BL est conservée en note)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        viewModel.openConvertToFactureInEditor(
                            source = source,
                            customFactureId = factureId,
                            appliquerTva = appliquerTva,
                            tauxTva = tauxTva
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_before_convert_to_facture_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Modifier les articles avant de valider", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    viewModel.convertToFacture(
                        source = source,
                        customFactureId = factureId,
                        appliquerTva = appliquerTva,
                        tauxTva = tauxTva,
                        replaceOriginal = replaceOriginal
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                modifier = Modifier.testTag("confirm_convert_to_facture_btn")
            ) {
                Text("Créer la Facture")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialogue permettant de sélectionner un ou plusieurs Bons de Livraison (BL)
 * pour les convertir / regrouper en une Facture Client unique.
 */
@Composable
fun SelectAndConvertBlsDialog(
    availableBls: List<OperationWithLignes>,
    viewModel: OperationViewModel,
    onDismiss: () -> Unit
) {
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()

    // Regrouper par client s'il y a plusieurs clients
    val clientNames = remember(availableBls) {
        availableBls.map { it.operation.clientFournisseur.trim() }.distinct()
    }
    var selectedClient by remember(clientNames) {
        mutableStateOf(clientNames.firstOrNull() ?: "")
    }

    val filteredBls = remember(availableBls, selectedClient) {
        availableBls.filter {
            selectedClient.isBlank() || it.operation.clientFournisseur.trim().equals(selectedClient, ignoreCase = true)
        }
    }

    var selectedIds by remember(filteredBls) {
        mutableStateOf(filteredBls.map { it.operation.id }.toSet())
    }
    var factureId by remember { mutableStateOf(viewModel.generateNextFactureId()) }
    var appliquerTva by remember(companySettings) { mutableStateOf(companySettings.tvaEnabledByDefault) }
    var tauxTva by remember(companySettings) { mutableDoubleStateOf(companySettings.defaultTvaRate) }
    var replaceOriginals by remember { mutableStateOf(true) }

    val selectedBls = filteredBls.filter { selectedIds.contains(it.operation.id) }
    val totalHt = selectedBls.sumOf { it.totalHt }
    val montantTva = if (appliquerTva) totalHt * (tauxTva / 100.0) else 0.0
    val totalTtc = totalHt + montantTva

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = null,
                tint = Color(0xFF059669),
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(
                text = "Convertir BL en Facture",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            if (availableBls.isEmpty()) {
                Text(
                    text = "Aucun Bon de Livraison (BL Client) disponible à convertir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (clientNames.size > 1) {
                        Text(
                            text = "1. Choisissez le client :",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            clientNames.forEach { clientName ->
                                FilterChip(
                                    selected = selectedClient.equals(clientName, ignoreCase = true),
                                    onClick = { selectedClient = clientName },
                                    label = { Text(clientName) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Text(
                        text = "Sélectionnez le(s) Bon(s) de Livraison à facturer (${selectedBls.size}/${filteredBls.size}) :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    filteredBls.forEach { bl ->
                        val isChecked = selectedIds.contains(bl.operation.id)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChecked) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) {
                                            selectedIds + bl.operation.id
                                        } else {
                                            selectedIds - bl.operation.id
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${bl.operation.id} • ${DateUtils.formatDateTime(bl.operation.dateTime)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${bl.nombreArticles} article(s) — ${CurrencyUtils.formatDa(bl.totalHt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = factureId,
                        onValueChange = { factureId = it },
                        label = { Text("N° de la Facture") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Appliquer la TVA ($tauxTva%)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Switch(
                            checked = appliquerTva,
                            onCheckedChange = { appliquerTva = it }
                        )
                    }

                    if (appliquerTva) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(19.0 to "19%", 9.0 to "9%").forEach { (rate, label) ->
                                FilterChip(
                                    selected = tauxTva == rate,
                                    onClick = { tauxTva = rate },
                                    label = { Text("TVA $label") }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = replaceOriginals,
                            onCheckedChange = { replaceOriginals = it }
                        )
                        Text(
                            text = "Remplacer le(s) BL d'origine par la facture",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Facture TTC :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = CurrencyUtils.formatDa(totalTtc),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (availableBls.isNotEmpty()) {
                Button(
                    onClick = {
                        if (selectedBls.isNotEmpty()) {
                            onDismiss()
                            viewModel.convertMultipleBlsToFacture(
                                sources = selectedBls,
                                customFactureId = factureId,
                                appliquerTva = appliquerTva,
                                tauxTva = tauxTva,
                                replaceOriginals = replaceOriginals
                            )
                        }
                    },
                    enabled = selectedBls.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.testTag("confirm_multi_bl_to_facture_btn")
                ) {
                    Text("Créer la Facture")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

/**
 * Dialogue de conversion d'un Devis (Client ou Fournisseur) en Bon de Livraison (BL)
 */
@Composable
fun ConvertDevisToBlDialog(
    source: OperationWithLignes,
    viewModel: OperationViewModel,
    onDismiss: () -> Unit
) {
    val op = source.operation
    val isSupplier = op.typeOperation == OperationType.DEVIS_FOURNISSEUR || op.typeOperation == OperationType.BL_FOURNISSEUR
    var blId by remember(source.operation.id) {
        mutableStateOf(viewModel.generateNextBlId(isSupplier))
    }
    var appliquerTva by remember(source) {
        mutableStateOf(op.appliquerTva)
    }
    var tauxTva by remember(source) {
        mutableDoubleStateOf(op.tauxTva)
    }
    var replaceOriginal by remember { mutableStateOf(true) }
    var deductStock by remember { mutableStateOf(true) }

    val totalHt = source.totalHt
    val montantTva = if (appliquerTva) totalHt * (tauxTva / 100.0) else 0.0
    val totalTtc = totalHt + montantTva

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = null,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(
                text = if (isSupplier) "Convertir Devis en Bon Fournisseur" else "Convertir Devis en Bon de Livraison (BL)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Source : ${op.typeOperation.label} N° ${op.id} (${op.clientFournisseur})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = blId,
                    onValueChange = { blId = it },
                    label = { Text(if (isSupplier) "N° du Bon Fournisseur (BL)" else "N° du Bon de Livraison (BL)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("convert_dialog_bl_id_input")
                )

                // Aperçu financier du BL
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE0F2FE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Articles repris :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF075985))
                            Text("${source.nombreArticles} ligne(s)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF075985))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Bon :", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF075985))
                            Text(CurrencyUtils.formatDa(totalTtc), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF0284C7))
                        }
                    }
                }

                // Option Mettre à jour le stock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = deductStock,
                        onCheckedChange = { deductStock = it },
                        modifier = Modifier.testTag("convert_bl_dialog_stock_checkbox")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSupplier) "Entrer les quantités en stock" else "Déduire les quantités du stock",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Le devis n'impactait pas le stock ; le BL mettra le stock à jour",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Option Remplacer le Devis d'origine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = replaceOriginal,
                        onCheckedChange = { replaceOriginal = it },
                        modifier = Modifier.testTag("convert_bl_dialog_replace_checkbox")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Remplacer le Devis d'origine par ce BL",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "La référence du devis est conservée dans les notes du BL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        viewModel.openConvertDevisToBlInEditor(
                            source = source,
                            customBlId = blId,
                            appliquerTva = appliquerTva,
                            tauxTva = tauxTva
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_before_convert_to_bl_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Modifier les articles avant de valider le BL", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    viewModel.convertDevisToBl(
                        source = source,
                        customBlId = blId,
                        appliquerTva = appliquerTva,
                        tauxTva = tauxTva,
                        replaceOriginal = replaceOriginal,
                        deductStock = deductStock
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.testTag("confirm_convert_to_bl_btn")
            ) {
                Text("Créer le BL")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialogue permettant de sélectionner un ou plusieurs Devis
 * pour les convertir en Bon de Livraison (BL).
 */
@Composable
fun SelectAndConvertDevisToBlDialog(
    availableDevis: List<OperationWithLignes>,
    viewModel: OperationViewModel,
    onDismiss: () -> Unit
) {
    val isSupplier = availableDevis.firstOrNull()?.operation?.typeOperation == OperationType.DEVIS_FOURNISSEUR
    val partnerNames = remember(availableDevis) {
        availableDevis.map { it.operation.clientFournisseur.trim() }.distinct()
    }
    var selectedPartner by remember(partnerNames) {
        mutableStateOf(partnerNames.firstOrNull() ?: "")
    }

    val filteredDevis = remember(availableDevis, selectedPartner) {
        availableDevis.filter {
            selectedPartner.isBlank() || it.operation.clientFournisseur.trim().equals(selectedPartner, ignoreCase = true)
        }
    }

    var selectedIds by remember(filteredDevis) {
        mutableStateOf(filteredDevis.map { it.operation.id }.toSet())
    }
    var blId by remember { mutableStateOf(viewModel.generateNextBlId(isSupplier)) }
    var replaceOriginals by remember { mutableStateOf(true) }
    var deductStock by remember { mutableStateOf(true) }

    val selectedDevisList = filteredDevis.filter { selectedIds.contains(it.operation.id) }
    val totalHt = selectedDevisList.sumOf { it.totalHt }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = null,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(
                text = "Convertir Devis en BL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            if (availableDevis.isEmpty()) {
                Text(
                    text = "Aucun Devis disponible à convertir en Bon de Livraison (BL).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (partnerNames.size > 1) {
                        Text(
                            text = "1. Choisissez le client / fournisseur :",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            partnerNames.forEach { pName ->
                                FilterChip(
                                    selected = selectedPartner.equals(pName, ignoreCase = true),
                                    onClick = { selectedPartner = pName },
                                    label = { Text(pName) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Text(
                        text = "Sélectionnez le(s) Devis à convertir en BL (${selectedDevisList.size}/${filteredDevis.size}) :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    filteredDevis.forEach { dv ->
                        val isChecked = selectedIds.contains(dv.operation.id)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChecked) Color(0xFFE0F2FE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) {
                                            selectedIds + dv.operation.id
                                        } else {
                                            selectedIds - dv.operation.id
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${dv.operation.id} • ${DateUtils.formatDateTime(dv.operation.dateTime)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${dv.nombreArticles} article(s) — ${CurrencyUtils.formatDa(dv.totalHt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = blId,
                        onValueChange = { blId = it },
                        label = { Text("N° du Bon de Livraison (BL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deductStock,
                            onCheckedChange = { deductStock = it }
                        )
                        Text(
                            text = "Mettre à jour le stock avec les articles du BL",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = replaceOriginals,
                            onCheckedChange = { replaceOriginals = it }
                        )
                        Text(
                            text = "Remplacer le(s) Devis d'origine par le BL",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total BL :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF075985)
                            )
                            Text(
                                text = CurrencyUtils.formatDa(totalHt),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (availableDevis.isNotEmpty()) {
                Button(
                    onClick = {
                        if (selectedDevisList.isNotEmpty()) {
                            onDismiss()
                            viewModel.convertMultipleDevisToBl(
                                sources = selectedDevisList,
                                customBlId = blId,
                                replaceOriginals = replaceOriginals,
                                deductStock = deductStock
                            )
                        }
                    },
                    enabled = selectedDevisList.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.testTag("confirm_multi_devis_to_bl_btn")
                ) {
                    Text("Créer le BL")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

/**
 * Dialogue complet d'aperçu A4, d'exportation locale (.pdf) et de partage client
 * pour un Bon de Livraison, une Facture ou tout autre document commercial.
 */
@Composable
fun PdfDocumentActionDialog(
    item: OperationWithLignes,
    pdfFile: File,
    settings: com.example.data.model.CompanySettings? = null,
    partner: PartnerEntity? = null,
    kitCompositionsMap: Map<String, List<ArticleCompositionEntity>> = emptyMap(),
    onUpdateSettings: ((com.example.data.model.CompanySettings) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val op = item.operation
    val conversionNote = remember(op.notes) { PdfGenerator.extractConversionNote(op.notes) }
    var activeSettings by remember(settings) {
        mutableStateOf(settings ?: com.example.data.repository.CompanySettingsManager(context).getSettings())
    }
    var forcePrintKitComponents by remember { mutableStateOf(false) }
    var includeStampAndSignature by remember(activeSettings) {
        mutableStateOf(activeSettings.pdfShowStampAndSignature)
    }
    var showConversionNoteOnPdf by remember(activeSettings) {
        mutableStateOf(activeSettings.pdfShowConversionNote)
    }
    var activePdfFile by remember(pdfFile) { mutableStateOf(pdfFile) }
    var renderVersion by remember { mutableStateOf(0) }

    val hasArticleKitLines = remember(item.lignes, kitCompositionsMap) {
        item.lignes.any { kitCompositionsMap.containsKey(it.produit.trim().lowercase(Locale.FRENCH)) }
    }

    val previewBitmap = remember(activePdfFile.absolutePath, activePdfFile.lastModified(), renderVersion) {
        PdfGenerator.renderPdfPreviewBitmap(activePdfFile)?.asImageBitmap()
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val saved = PdfGenerator.savePdfToUri(context, activePdfFile, uri)
            if (saved) {
                Toast.makeText(
                    context,
                    "PDF exporté et enregistré : ${activePdfFile.name}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Erreur lors de l'enregistrement du PDF.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${op.typeOperation.label} PDF Prêt",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = activePdfFile.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Aperçu visuel de la page A4 générée
                if (previewBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = "Aperçu PDF ${op.id}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Option Activer / Désactiver le Cachet Humide dans l'aperçu PDF
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Cachet Humide (Format PNG)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (includeStampAndSignature)
                                    "Activé sur ce ${op.typeOperation.label}"
                                else
                                    "Désactivé sur ce document PDF",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeStampAndSignature,
                            onCheckedChange = { checked ->
                                includeStampAndSignature = checked
                                val updatedSettings = activeSettings.copy(
                                    pdfShowStampAndSignature = checked,
                                    pdfShowSignatures = if (checked) true else activeSettings.pdfShowSignatures
                                )
                                activeSettings = updatedSettings
                                onUpdateSettings?.invoke(updatedSettings)
                                activePdfFile = PdfGenerator.generatePdf(
                                    context = context,
                                    item = item,
                                    settings = updatedSettings,
                                    partner = partner,
                                    kitCompositionsMap = kitCompositionsMap,
                                    forcePrintKitComponents = forcePrintKitComponents
                                )
                                renderVersion++
                            },
                            modifier = Modifier.testTag("dialog_pdf_stamp_signature_switch")
                        )
                    }
                }

                // Option Masquer / Afficher la note "Document converti à partir de la facture N°..." lors de la génération PDF
                if (!conversionNote.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEEF2FF),
                        modifier = Modifier.fillMaxWidth()
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
                                    text = conversionNote,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF312E81)
                                )
                                Text(
                                    text = if (showConversionNoteOnPdf)
                                        "Note affichée en bas du PDF (désactivez pour masquer)"
                                    else
                                        "Note masquée sur ce document PDF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4338CA)
                                )
                            }
                            Switch(
                                checked = showConversionNoteOnPdf,
                                onCheckedChange = { checked ->
                                    showConversionNoteOnPdf = checked
                                    val updatedSettings = activeSettings.copy(
                                        pdfShowConversionNote = checked
                                    )
                                    activeSettings = updatedSettings
                                    onUpdateSettings?.invoke(updatedSettings)
                                    activePdfFile = PdfGenerator.generatePdf(
                                        context = context,
                                        item = item,
                                        settings = updatedSettings,
                                        partner = partner,
                                        kitCompositionsMap = kitCompositionsMap,
                                        forcePrintKitComponents = forcePrintKitComponents
                                    )
                                    renderVersion++
                                },
                                modifier = Modifier.testTag("dialog_pdf_conversion_note_switch")
                            )
                        }
                    }
                }

                // Option pour un Article Kit : Forcer l'impression du détail des composants
                if (hasArticleKitLines) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0D9488).copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎁 Article Kit : Forcer l'impression du détail",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF0D9488)
                                )
                                Text(
                                    text = if (forcePrintKitComponents)
                                        "Le client voit le Kit + le détail de ses composants inclus"
                                    else
                                        "Par défaut : Le client voit uniquement le nom du Kit (1 ligne)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = forcePrintKitComponents,
                                onCheckedChange = { checked ->
                                    forcePrintKitComponents = checked
                                    activePdfFile = PdfGenerator.generatePdf(
                                        context = context,
                                        item = item,
                                        settings = activeSettings,
                                        partner = partner,
                                        kitCompositionsMap = kitCompositionsMap,
                                        forcePrintKitComponents = checked
                                    )
                                    renderVersion++
                                },
                                modifier = Modifier.testTag("force_print_kit_detail_switch")
                            )
                        }
                    }
                }

                // Résumé du document
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Document : ${op.typeOperation.label} N° ${op.id}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Client / Destinataire : ${op.clientFournisseur}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "Total TTC : ${CurrencyUtils.formatDa(item.totalTtc)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (op.appliquerTva) {
                            Text(
                                text = "Dont TVA ${op.tauxTva}% : ${CurrencyUtils.formatDa(item.montantTva)} (HT : ${CurrencyUtils.formatDa(item.totalHt)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val pdfAmountWords = if (op.typeOperation == OperationType.REGLEMENT) op.montantVerse else item.totalTtc
                        Text(
                            text = "${CurrencyUtils.formatArretePrefix(op.typeOperation)} ${CurrencyUtils.amountToFrenchWordsDa(pdfAmountWords)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Bouton d'impression directe A4
                Button(
                    onClick = {
                        PdfGenerator.printPdfA4(
                            context = context,
                            file = activePdfFile,
                            jobName = "${op.typeOperation.shortCode}_${op.id}_A4"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_print_a4_btn")
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Imprimer en Format A4 (${op.typeOperation.label})")
                }

                // Boutons d'actions PDF complets
                Button(
                    onClick = {
                        val subject = "${op.typeOperation.label} ${op.id} - ${op.clientFournisseur}"
                        PdfGenerator.sharePdf(context, activePdfFile, subject)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_share_pdf_btn")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager avec le Client (WhatsApp, Email...)")
                }

                OutlinedButton(
                    onClick = {
                        savePdfLauncher.launch(activePdfFile.name)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_save_pdf_btn")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exporter / Enregistrer sur l'appareil (.pdf)")
                }

                FilledTonalButton(
                    onClick = {
                        PdfGenerator.openPdf(context, activePdfFile)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_open_pdf_btn")
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ouvrir dans le lecteur PDF externe")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

