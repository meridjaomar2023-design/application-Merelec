package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Scaffold
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
import com.example.data.model.OperationType
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.permissionCategory
import com.example.ui.components.OperationCard
import com.example.ui.theme.ClientColor
import com.example.ui.theme.FournisseurColor
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import com.example.ui.theme.ReglementColor
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.CurrencyUtils
import com.example.util.PdfGenerator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerDetailScreen(
    partnerId: String,
    viewModel: OperationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summaryFlow = remember(partnerId) { viewModel.getPartnerSummary(partnerId) }
    val summary by summaryFlow.collectAsStateWithLifecycle(initialValue = null)
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    var editRc by remember { mutableStateOf("") }
    var editArticleImposition by remember { mutableStateOf("") }
    var editNif by remember { mutableStateOf("") }
    var editNis by remember { mutableStateOf("") }
    var editNin by remember { mutableStateOf("") }
    var editRib by remember { mutableStateOf("") }

    var generatedStatementFile by remember { mutableStateOf<File?>(null) }
    var showPdfStatementDialog by remember { mutableStateOf(false) }
    var blToConvertToFacture by remember { mutableStateOf<com.example.data.model.OperationWithLignes?>(null) }
    var showBatchConvertBlsDialog by remember { mutableStateOf(false) }
    var devisToConvertToBl by remember { mutableStateOf<com.example.data.model.OperationWithLignes?>(null) }
    var showBatchConvertDevisDialog by remember { mutableStateOf(false) }
    var pdfDialogPair by remember { mutableStateOf<Pair<com.example.data.model.OperationWithLignes, File>?>(null) }
    var partnerOpSearchQuery by remember { mutableStateOf("") }
    var partnerFilterOnlyBls by remember { mutableStateOf(false) }

    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    var operationToDelete by remember { mutableStateOf<com.example.data.model.OperationWithLignes?>(null) }

    if (operationToDelete != null) {
        val opTarget = operationToDelete!!
        val catLabel = opTarget.operation.typeOperation.permissionCategory().label
        AlertDialog(
            onDismissRequest = { operationToDelete = null },
            title = { Text("Supprimer ce $catLabel ?") },
            text = {
                Text(
                    "Confirmez-vous la suppression de ${opTarget.operation.typeOperation.label} #${opTarget.operation.id} (${CurrencyUtils.formatDa(opTarget.totalTtc)}) ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOperation(opTarget.operation.id)
                        operationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    if (devisToConvertToBl != null) {
        ConvertDevisToBlDialog(
            source = devisToConvertToBl!!,
            viewModel = viewModel,
            onDismiss = { devisToConvertToBl = null }
        )
    }

    if (showBatchConvertBlsDialog && summary != null) {
        val clientBls = summary!!.operations.filter { it.operation.typeOperation == OperationType.BL_CLIENT }
        SelectAndConvertBlsDialog(
            availableBls = clientBls,
            viewModel = viewModel,
            onDismiss = { showBatchConvertBlsDialog = false }
        )
    }

    if (showBatchConvertDevisDialog && summary != null) {
        val partnerDevis = summary!!.operations.filter {
            it.operation.typeOperation == OperationType.DEVIS_CLIENT ||
                it.operation.typeOperation == OperationType.DEVIS_FOURNISSEUR
        }
        SelectAndConvertDevisToBlDialog(
            availableDevis = partnerDevis,
            viewModel = viewModel,
            onDismiss = { showBatchConvertDevisDialog = false }
        )
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }

    // Modal Modification Partenaire
    if (showEditDialog && summary != null) {
        val partner = summary!!.partner
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
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
                        label = { Text("Nom de l'entreprise / Contact *") },
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
                        label = { Text("Notes / Observations") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_partner_notes"),
                        maxLines = 3
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
                                    notes = editNotes.trim(),
                                    rc = editRc.trim(),
                                    articleImposition = editArticleImposition.trim(),
                                    nif = editNif.trim(),
                                    nis = editNis.trim(),
                                    nin = editNin.trim(),
                                    rib = editRib.trim()
                                )
                            )
                            showEditDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_edit_partner_btn")
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Confirmation Suppression
    if (showDeleteConfirmDialog && summary != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Supprimer cette fiche ?") },
            text = {
                Text("Voulez-vous supprimer « ${summary!!.partner.name} » du répertoire ? Les bons de livraison et règlements enregistrés seront conservés.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePartner(summary!!.partner.id)
                        showDeleteConfirmDialog = false
                        viewModel.navigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Confirmation & Actions PDF Relevé
    if (showPdfStatementDialog && generatedStatementFile != null && summary != null) {
        AlertDialog(
            onDismissRequest = { showPdfStatementDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Relevé de Compte PDF Prêt", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column {
                    Text(
                        text = "Le relevé de compte officiel « ${generatedStatementFile!!.name} » pour ${summary!!.partner.name} a été généré avec succès.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Total Débits : ${CurrencyUtils.formatDa(summary!!.totalBl)}", style = MaterialTheme.typography.labelMedium)
                            Text(text = "Total Règlements : ${CurrencyUtils.formatDa(summary!!.totalReglements)}", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "Solde Restant : ${CurrencyUtils.formatDa(summary!!.soldeRestantDu)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (summary!!.soldeRestantDu > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfStatementDialog = false
                        PdfGenerator.openPdf(context, generatedStatementFile!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    modifier = Modifier.testTag("open_statement_pdf_btn")
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Visualiser / Ouvrir")
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = {
                        showPdfStatementDialog = false
                        PdfGenerator.sharePdf(context, generatedStatementFile!!, "Relevé de Compte - ${summary!!.partner.name}")
                    },
                    modifier = Modifier.testTag("share_statement_pdf_btn")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = summary?.partner?.name ?: "Fiche Partenaire",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = if (summary?.partner?.type == PartnerType.CLIENT) "Fiche Client & Historique" else "Fiche Fournisseur & Historique",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("partner_detail_back_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            summary?.let { s ->
                                val file = PdfGenerator.generatePartnerStatementPdf(
                                    context = context,
                                    partner = s.partner,
                                    operations = s.operations,
                                    totalBl = s.totalBl,
                                    totalReglements = s.totalReglements,
                                    soldeRestantDu = s.soldeRestantDu
                                )
                                generatedStatementFile = file
                                showPdfStatementDialog = true
                            }
                        },
                        modifier = Modifier.testTag("partner_pdf_statement_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Relevé PDF",
                            tint = Color(0xFF16A34A)
                        )
                    }
                    IconButton(
                        onClick = {
                            summary?.partner?.let { p ->
                                editName = p.name
                                editPhone = p.phone
                                editAddress = p.address
                                editNotes = p.notes
                                editRc = p.rc
                                editArticleImposition = p.articleImposition
                                editNif = p.nif
                                editNis = p.nis
                                editNin = p.nin
                                editRib = p.rib
                                showEditDialog = true
                            }
                        },
                        modifier = Modifier.testTag("edit_partner_action")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", tint = Color(0xFFFFA726))
                    }
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("delete_partner_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            summary?.partner?.let { p ->
                FloatingActionButton(
                    onClick = {
                        viewModel.startNewOperationForPartner(p)
                    },
                    containerColor = if (p.type == PartnerType.CLIENT) ClientColor else FournisseurColor,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("partner_new_operation_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nouveau bon")
                }
            }
        }
    ) { innerPadding ->
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Chargement de la fiche...", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val s = summary!!
            val partner = s.partner
            val isClient = partner.type == PartnerType.CLIENT

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Fiche Contact & Coordonnées
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background((if (isClient) ClientColor else FournisseurColor).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isClient) Icons.Default.Person else Icons.Default.Business,
                                        contentDescription = null,
                                        tint = if (isClient) ClientColor else FournisseurColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = partner.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isClient) "Client Professionnel / Particulier" else "Fournisseur Matériel Électrique",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (partner.phone.isNotBlank() || partner.address.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            if (partner.phone.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${partner.phone.trim()}"))
                                            context.startActivity(intent)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = partner.phone,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(Appeler)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (partner.address.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = partner.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (partner.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Notes: ${partner.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            // Coordonnées Fiscales (Algérie)
                            val hasFiscal = partner.rc.isNotBlank() || partner.articleImposition.isNotBlank() ||
                                partner.nif.isNotBlank() || partner.nis.isNotBlank() || partner.nin.isNotBlank() || partner.rib.isNotBlank()
                            if (hasFiscal) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Coordonnées Fiscales & Bancaires",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (partner.rc.isNotBlank() || partner.articleImposition.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        if (partner.rc.isNotBlank()) {
                                            Text(text = "RC N°: ${partner.rc}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (partner.articleImposition.isNotBlank()) {
                                            Text(text = "N° article: ${partner.articleImposition}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                if (partner.nif.isNotBlank() || partner.nis.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        if (partner.nif.isNotBlank()) {
                                            Text(text = "NIF N°: ${partner.nif}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (partner.nis.isNotBlank()) {
                                            Text(text = "NIS N °: ${partner.nis}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                if (partner.nin.isNotBlank()) {
                                    Text(text = "NIN N°: ${partner.nin}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (partner.rib.isNotBlank()) {
                                    Text(text = "RIB: ${partner.rib}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Synthèse Financière du Partenaire
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Synthèse Financière & Encours",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total Facturé BL
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isClient) "Total Facturé BL" else "Total Achats",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CurrencyUtils.formatDaSmart(s.totalBl),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isClient) ClientColor else FournisseurColor
                                    )
                                    Text(
                                        text = "${s.countBl} bon(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Total Réglé
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Total Règlements",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CurrencyUtils.formatDaSmart(s.totalReglements),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ReglementColor
                                    )
                                    Text(
                                        text = "${s.countReglements} versement(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Solde Restant Dû / Encours
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (s.soldeRestantDu > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isClient) "Solde Restant Dû (Créance)" else "Solde Fournisseur",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (s.soldeRestantDu > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (s.soldeRestantDu > 0) "Montant restant à encaisser" else "Compte parfaitement à jour",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = CurrencyUtils.formatDaSmart(s.soldeRestantDu),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (s.soldeRestantDu > 0) MaterialTheme.colorScheme.error else ReglementColor
                                )
                            }
                        }
                    }
                }

                // Carte Action Relevé de Compte PDF
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("partner_pdf_statement_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
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
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isClient) "Relevé de Compte Client (PDF)" else "Relevé de Compte Fournisseur (PDF)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Éditer l'historique complet avec débits, crédits & solde",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    val file = PdfGenerator.generatePartnerStatementPdf(
                                        context = context,
                                        partner = s.partner,
                                        operations = s.operations,
                                        totalBl = s.totalBl,
                                        totalReglements = s.totalReglements,
                                        soldeRestantDu = s.soldeRestantDu
                                    )
                                    generatedStatementFile = file
                                    showPdfStatementDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("partner_generate_pdf_btn")
                            ) {
                                Text("Relevé PDF", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }
                }

                // Boutons d'actions rapides : Nouveau BL / Facturer BL / Nouveau Règlement
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.startNewOperationForPartner(
                                        partner = partner,
                                        specificType = if (isClient) OperationType.BL_CLIENT else OperationType.BL_FOURNISSEUR
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("partner_add_bl_btn")
                            ) {
                                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isClient) "Nouveau BL" else "Nouveau Bon", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    viewModel.startNewOperationForPartner(
                                        partner = partner,
                                        specificType = OperationType.REGLEMENT
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ReglementColor),
                                modifier = Modifier.weight(1f).testTag("partner_add_reglement_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Wallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Règlement", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        val clientBlsCount = s.operations.count { it.operation.typeOperation == OperationType.BL_CLIENT }
                        if (isClient && clientBlsCount > 0) {
                            Button(
                                onClick = { showBatchConvertBlsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("partner_convert_bls_btn")
                            ) {
                                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Convertir BL en Facture ($clientBlsCount BL disponible${if (clientBlsCount > 1) "s" else ""})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        val devisCount = s.operations.count {
                            it.operation.typeOperation == OperationType.DEVIS_CLIENT ||
                                it.operation.typeOperation == OperationType.DEVIS_FOURNISSEUR
                        }
                        if (devisCount > 0) {
                            Button(
                                onClick = { showBatchConvertDevisDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("partner_convert_devis_btn")
                            ) {
                                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Convertir Devis en BL ($devisCount Devis disponible${if (devisCount > 1) "s" else ""})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // En-tête & Barre de recherche des Bons de Livraison et opérations du partenaire
                val filteredPartnerOps = s.operations.filter { opItem ->
                    val matchesType = !partnerFilterOnlyBls ||
                        opItem.operation.typeOperation == OperationType.BL_CLIENT ||
                        opItem.operation.typeOperation == OperationType.BL_FOURNISSEUR
                    val q = partnerOpSearchQuery.trim().lowercase()
                    val matchesQuery = q.isBlank() ||
                        opItem.operation.id.lowercase().contains(q) ||
                        opItem.operation.clientFournisseur.lowercase().contains(q) ||
                        opItem.lignes.any { l -> l.produit.lowercase().contains(q) } ||
                        opItem.operation.notes.lowercase().contains(q)
                    matchesType && matchesQuery
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bons de Livraison & Historique (${filteredPartnerOps.size}/${s.operations.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = partnerOpSearchQuery,
                            onValueChange = { partnerOpSearchQuery = it },
                            placeholder = { Text("Filtrer par N° Bon de Livraison, article, note...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (partnerOpSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { partnerOpSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("partner_operations_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !partnerFilterOnlyBls,
                                onClick = { partnerFilterOnlyBls = false },
                                label = { Text("Toutes les opérations (${s.operations.size})") }
                            )
                            val blCount = s.operations.count {
                                it.operation.typeOperation == OperationType.BL_CLIENT ||
                                    it.operation.typeOperation == OperationType.BL_FOURNISSEUR
                            }
                            FilterChip(
                                selected = partnerFilterOnlyBls,
                                onClick = { partnerFilterOnlyBls = !partnerFilterOnlyBls },
                                label = { Text("Bons de Livraison uniquement ($blCount)") },
                                leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("partner_filter_bls_chip")
                            )
                        }
                    }
                }

                // Liste des opérations associées
                if (filteredPartnerOps.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (partnerOpSearchQuery.isNotBlank() || partnerFilterOnlyBls)
                                    "Aucun Bon de Livraison correspondant au filtre"
                                else
                                    "Aucune opération enregistrée pour ce partenaire",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredPartnerOps, key = { it.operation.id }) { item ->
                        val canEditOp = currentEmployee.canModifyOperation(item.operation.typeOperation)
                        val canDeleteOp = currentEmployee.canDeleteOperation(item.operation.typeOperation)
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            OperationCard(
                                item = item,
                                onClick = {
                                    viewModel.navigateTo(AppScreen.Detail(item.operation.id))
                                },
                                onConvertToFacture = if (item.operation.typeOperation == OperationType.BL_CLIENT) {
                                    { blToConvertToFacture = item }
                                } else null,
                                onConvertToBl = if (
                                    item.operation.typeOperation == OperationType.DEVIS_CLIENT ||
                                    item.operation.typeOperation == OperationType.DEVIS_FOURNISSEUR
                                ) {
                                    { devisToConvertToBl = item }
                                } else null,
                                onGeneratePdf = {
                                    val file = PdfGenerator.generatePdf(
                                        context = context,
                                        item = item,
                                        partner = s.partner
                                    )
                                    pdfDialogPair = item to file
                                },
                                onEdit = {
                                    viewModel.editOperation(item)
                                },
                                canEdit = canEditOp,
                                onDelete = {
                                    if (canDeleteOp) {
                                        operationToDelete = item
                                    } else {
                                        viewModel.showMessage(
                                            currentEmployee.permissionDeniedMessage(
                                                action = "supprimer",
                                                category = item.operation.typeOperation.permissionCategory()
                                            )
                                        )
                                    }
                                },
                                canDelete = canDeleteOp
                            )
                        }
                    }
                }
            }
        }
    }
}
