package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentPermissionCategory
import com.example.data.model.OperationType
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.ui.theme.ClientColor
import com.example.ui.theme.FournisseurColor
import com.example.ui.theme.ReglementColor
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.OperationViewModel
import com.example.ui.viewmodel.PartnerBalanceItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import com.example.util.CurrencyUtils
import com.example.util.ExportUtils
import com.example.util.ImportUtils
import com.example.util.PdfGenerator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreancesTabView(
    viewModel: OperationViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val creancesSummary by viewModel.creancesSummary.collectAsStateWithLifecycle()
    val clientsBalances by viewModel.clientsBalances.collectAsStateWithLifecycle()
    val suppliersBalances by viewModel.suppliersBalances.collectAsStateWithLifecycle()
    val allOperations by viewModel.allOperations.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val lastImportSummary by viewModel.lastImportSummary.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Clients, 1: Fournisseurs
    var searchQuery by remember { mutableStateOf("") }
    var onlyUnpaid by remember { mutableStateOf(true) }
    var editingCreanceItem by remember { mutableStateOf<PartnerBalanceItem?>(null) }
    var deletingCreanceItem by remember { mutableStateOf<PartnerBalanceItem?>(null) }
    var showImportCreancesDialog by remember { mutableStateOf(false) }
    var showResetAllCreancesDialog by remember { mutableStateOf(false) }
    var creanceImportColumns by remember { mutableStateOf(ImportUtils.defaultCreanceColumns()) }

    val importClientCreancesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedTab = 0
            viewModel.importCreancesFromUri(context, it, PartnerType.CLIENT, creanceImportColumns)
        }
    }

    val importSupplierDettesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedTab = 1
            viewModel.importCreancesFromUri(context, it, PartnerType.FOURNISSEUR, creanceImportColumns)
        }
    }

    val currentList = if (selectedTab == 0) clientsBalances else suppliersBalances
    val filteredList = remember(currentList, searchQuery, onlyUnpaid) {
        val cleanQuery = searchQuery.trim().lowercase(Locale.FRENCH)
        currentList.filter { item ->
            val matchesQuery = cleanQuery.isEmpty() ||
                    item.partner.name.lowercase(Locale.FRENCH).contains(cleanQuery) ||
                    item.partner.phone.contains(cleanQuery) ||
                    item.partner.address.lowercase(Locale.FRENCH).contains(cleanQuery)
            val matchesUnpaid = !onlyUnpaid || item.soldeRestant > 0.01
            matchesQuery && matchesUnpaid
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("top_left_drawer_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu latéral (Dashboard & Configuration)",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Créances & Dettes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Suivi des impayés & recouvrement",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.PdfStudio(3)) },
                        modifier = Modifier.testTag("creances_pdf_studio_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Créer & Réorganiser PDF Créances",
                            tint = Color(0xFF16A34A)
                        )
                    }

                    IconButton(
                        onClick = { showImportCreancesDialog = true },
                        modifier = Modifier.testTag("creances_import_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Importer Créances (CSV / Excel)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.BackupDrive) },
                        modifier = Modifier.testTag("creances_drive_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddToDrive,
                            contentDescription = "Drive",
                            tint = Color(0xFF0F9D58)
                        )
                    }

                    IconButton(
                        onClick = { ExportUtils.exportToExcelFile(context, allOperations) },
                        modifier = Modifier.testTag("creances_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Export Excel",
                            tint = Color(0xFF107C41)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.startNewOperation(OperationType.REGLEMENT)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouveau Règlement") },
                containerColor = ReglementColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("creances_new_reglement_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // KPI Global
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("creances_kpi_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SYNTHÈSE DU RECOUVREMENT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = companySettings.currency,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Créances Clients
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (creancesSummary.totalCreancesClients > 0) Color(0xFFEF4444).copy(alpha = 0.08f) else Color(0xFF10B981).copy(alpha = 0.08f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = if (creancesSummary.totalCreancesClients > 0) Color(0xFFDC2626) else Color(0xFF059669),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Créances Clients",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (creancesSummary.totalCreancesClients > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyUtils.formatDa(creancesSummary.totalCreancesClients),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (creancesSummary.totalCreancesClients > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                    Text(
                                        text = "${creancesSummary.countDebiteursClients} client(s) débiteur(s)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Total Dettes Fournisseurs
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (creancesSummary.totalDettesFournisseurs > 0) Color(0xFFF97316).copy(alpha = 0.08f) else Color(0xFF10B981).copy(alpha = 0.08f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = if (creancesSummary.totalDettesFournisseurs > 0) Color(0xFFEA580C) else Color(0xFF059669),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Dettes Fournisseurs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (creancesSummary.totalDettesFournisseurs > 0) Color(0xFFEA580C) else Color(0xFF059669)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyUtils.formatDa(creancesSummary.totalDettesFournisseurs),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (creancesSummary.totalDettesFournisseurs > 0) Color(0xFFEA580C) else Color(0xFF059669)
                                    )
                                    Text(
                                        text = "${creancesSummary.countCrediteursFournisseurs} fournisseur(s) dû(s)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Solde net
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Balance Nette (Créances - Dettes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatDa(creancesSummary.balanceNette),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (creancesSummary.balanceNette >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bouton rapide d'importation des créances / dettes en Excel ou CSV
                        FilledTonalButton(
                            onClick = { showImportCreancesDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("creances_kpi_import_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Importer Créances & Dettes (CSV / Excel)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bouton de remise à zéro des créances
                        OutlinedButton(
                            onClick = {
                                if (currentEmployee.canDeleteCreance || currentEmployee.canModifyCreance || currentEmployee.isAdmin) {
                                    showResetAllCreancesDialog = true
                                } else {
                                    viewModel.showMessage(
                                        currentEmployee.permissionDeniedMessage(
                                            action = "remettre à zéro",
                                            category = DocumentPermissionCategory.CREANCE
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("creances_reset_all_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remise à zéro des créances (0,00 DA)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Onglets de bascule : Créances Clients / Dettes Fournisseurs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Clients (${clientsBalances.count { it.soldeRestant > 0.01 }})")
                            }
                        },
                        modifier = Modifier.testTag("tab_clients_creances")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Fournisseurs (${suppliersBalances.count { it.soldeRestant > 0.01 }})")
                            }
                        },
                        modifier = Modifier.testTag("tab_suppliers_dettes")
                    )
                }
            }

            // Barre de recherche et filtre "Uniquement impayés"
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("creances_search_input"),
                        placeholder = { Text(if (selectedTab == 0) "Rechercher client par nom, tél..." else "Rechercher fournisseur...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = onlyUnpaid,
                            onClick = { onlyUnpaid = !onlyUnpaid },
                            label = { Text("Soldes impayés uniquement") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (onlyUnpaid) Icons.Default.Warning else Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("chip_only_unpaid")
                        )

                        Text(
                            text = "${filteredList.size} résultat(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Liste des partenaires
            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (onlyUnpaid) "Aucun solde impayé pour le moment !" else "Aucun partenaire trouvé",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedTab == 0) "Tous vos clients sont à jour de règlement." else "Toutes vos dettes fournisseurs sont soldées.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.partner.id }) { item ->
                    PartnerBalanceCard(
                        item = item,
                        isClient = selectedTab == 0,
                        canModifyCreance = currentEmployee.canModifyCreance,
                        canDeleteCreance = currentEmployee.canDeleteCreance,
                        onNewPayment = {
                            if (currentEmployee.canModifyCreance) {
                                viewModel.startNewOperationForPartner(item.partner, OperationType.REGLEMENT)
                            } else {
                                viewModel.showMessage(
                                    currentEmployee.permissionDeniedMessage(
                                        action = "modifier / encaisser",
                                        category = DocumentPermissionCategory.CREANCE
                                    )
                                )
                            }
                        },
                        onModifyCreance = {
                            if (currentEmployee.canModifyCreance) {
                                editingCreanceItem = item
                            } else {
                                viewModel.showMessage(
                                    currentEmployee.permissionDeniedMessage(
                                        action = "modifier",
                                        category = DocumentPermissionCategory.CREANCE
                                    )
                                )
                            }
                        },
                        onDeleteCreance = {
                            if (currentEmployee.canDeleteCreance) {
                                deletingCreanceItem = item
                            } else {
                                viewModel.showMessage(
                                    currentEmployee.permissionDeniedMessage(
                                        action = "supprimer",
                                        category = DocumentPermissionCategory.CREANCE
                                    )
                                )
                            }
                        },
                        onViewStatement = {
                            viewModel.navigateTo(AppScreen.PartnerDetail(item.partner.id))
                        },
                        onSharePdf = {
                            val ops = allOperations.filter {
                                it.operation.clientFournisseur.trim().equals(item.partner.name.trim(), ignoreCase = true)
                            }
                            val pdfFile = PdfGenerator.generatePartnerStatementPdf(
                                context = context,
                                partner = item.partner,
                                operations = ops,
                                totalBl = item.totalTransactions,
                                totalReglements = item.totalReglements,
                                soldeRestantDu = item.soldeRestant,
                                settings = companySettings
                            )
                            PdfGenerator.sharePdf(
                                context = context,
                                file = pdfFile,
                                subject = "Relevé de Compte - ${item.partner.name}"
                            )
                        },
                        onCall = { phone ->
                            if (phone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialogue de modification directe de la créance d'un partenaire
    editingCreanceItem?.let { targetItem ->
        var newSoldeText by remember(targetItem.soldeRestant) {
            mutableStateOf(CurrencyUtils.formatQty(targetItem.soldeRestant))
        }
        var reasonText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { editingCreanceItem = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Modifier la Créance / Solde") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Partenaire : ${targetItem.partner.name}\nCréance actuelle : ${CurrencyUtils.formatDa(targetItem.soldeRestant)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newSoldeText,
                        onValueChange = { newSoldeText = it },
                        label = { Text("Nouveau montant de la créance (DA)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Motif de la modification (optionnel)") },
                        placeholder = { Text("ex: Remise commerciale, correction solde...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.deletePartnerCreance(targetItem.partner.name)
                            editingCreanceItem = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_reset_partner_creance_zero_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remise à zéro immédiate (0,00 DA)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedSolde = CurrencyUtils.parseAmount(newSoldeText).coerceAtLeast(0.0)
                        viewModel.adjustPartnerCreanceSolde(
                            item = targetItem,
                            newTargetSolde = parsedSolde,
                            reason = reasonText
                        )
                        editingCreanceItem = null
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCreanceItem = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue de confirmation de remise à zéro globale des créances
    if (showResetAllCreancesDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllCreancesDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remise à zéro des créances") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choisissez les créances à remettre à zéro (0,00 DA) :",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            viewModel.resetAllCreancesToZero(PartnerType.CLIENT)
                            showResetAllCreancesDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_reset_clients_creances_btn")
                    ) {
                        Text("Remettre à zéro les Créances Clients (0 DA)")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.resetAllCreancesToZero(PartnerType.FOURNISSEUR)
                            showResetAllCreancesDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("confirm_reset_suppliers_dettes_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remettre à zéro les Dettes Fournisseurs (0 DA)")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.resetAllCreancesToZero(null)
                            showResetAllCreancesDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("confirm_reset_all_creances_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Tout remettre à zéro (Clients + Fournisseurs)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showResetAllCreancesDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue de confirmation de remise à zéro / suppression de la créance d'un partenaire
    deletingCreanceItem?.let { targetItem ->
        AlertDialog(
            onDismissRequest = { deletingCreanceItem = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remise à zéro de la créance ?") },
            text = {
                Text(
                    "Voulez-vous vraiment remettre à zéro la créance de « ${targetItem.partner.name} » (${CurrencyUtils.formatDa(targetItem.soldeRestant)}) ?\n\nLe solde restant dû passera immédiatement à 0,00 DA."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePartnerCreance(targetItem.partner.name)
                        deletingCreanceItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remettre à 0,00 DA")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCreanceItem = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue d'importation des Créances & Dettes (Excel .xlsx / .xls ou CSV) avec choix et ordre des colonnes
    if (showImportCreancesDialog) {
        val enabledCreanceCols = creanceImportColumns.filter { it.enabled }
        AlertDialog(
            onDismissRequest = { showImportCreancesDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Importer Créances & Dettes (CSV / Excel)") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Choisissez et réorganisez les colonnes de votre fichier (organisées dans le même ordre que lors de la création) :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Ordre actif (${enabledCreanceCols.size} col.) :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = enabledCreanceCols.mapIndexed { i, c -> "${i + 1}. ${c.field.displayLabel}" }.joinToString("  •  "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    items(creanceImportColumns.size) { index ->
                        val colSpec = creanceImportColumns[index]
                        val activePos = enabledCreanceCols.indexOfFirst { it.id == colSpec.id }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (colSpec.enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = colSpec.enabled,
                                        onCheckedChange = { checked ->
                                            if (!checked && colSpec.field == com.example.util.CreanceImportField.NOM) return@Checkbox
                                            creanceImportColumns = creanceImportColumns.toMutableList().also {
                                                it[index] = colSpec.copy(enabled = checked)
                                            }
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = if (activePos >= 0) "Col. ${activePos + 1} : ${colSpec.field.displayLabel}" else colSpec.field.displayLabel,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "En-tête CSV : ${colSpec.customHeader}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    TextButton(
                                        onClick = {
                                            if (index > 0) {
                                                creanceImportColumns = creanceImportColumns.toMutableList().also { list ->
                                                    val tmp = list[index - 1]
                                                    list[index - 1] = list[index]
                                                    list[index] = tmp
                                                }
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Text("↑")
                                    }
                                    TextButton(
                                        onClick = {
                                            if (index < creanceImportColumns.lastIndex) {
                                                creanceImportColumns = creanceImportColumns.toMutableList().also { list ->
                                                    val tmp = list[index + 1]
                                                    list[index + 1] = list[index]
                                                    list[index] = tmp
                                                }
                                            }
                                        },
                                        enabled = index < creanceImportColumns.lastIndex
                                    ) {
                                        Text("↓")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                val nextNum = creanceImportColumns.count { it.field == com.example.util.CreanceImportField.COLONNE_PLUS } + 1
                                creanceImportColumns = creanceImportColumns + com.example.util.CreanceColumnSpec(
                                    field = com.example.util.CreanceImportField.COLONNE_PLUS,
                                    customHeader = "colonne_plus_$nextNum",
                                    enabled = true,
                                    isExtraColumn = true
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Ajouter une colonne en plus")
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                showImportCreancesDialog = false
                                importClientCreancesLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ClientColor),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_import_client_creances_btn")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importer Créances Clients (.xlsx / .csv)")
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                showImportCreancesDialog = false
                                importSupplierDettesLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FournisseurColor),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_import_supplier_dettes_btn")
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importer Dettes Fournisseurs (.xlsx / .csv)")
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    ExportUtils.shareText(
                                        context,
                                        "Modele_Creances_Clients.csv",
                                        ImportUtils.generateCreancesTemplateCsv(PartnerType.CLIENT, creanceImportColumns)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Modèle Clients", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    ExportUtils.shareText(
                                        context,
                                        "Modele_Dettes_Fournisseurs.csv",
                                        ImportUtils.generateCreancesTemplateCsv(PartnerType.FOURNISSEUR, creanceImportColumns)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Modèle Fourn.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    item {
                        TextButton(
                            onClick = {
                                showImportCreancesDialog = false
                                viewModel.navigateTo(AppScreen.ImportDirectory)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ouvrir le Centre d'Importation Complet (Articles, Tiers, Créances)")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportCreancesDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Dialogue de rapport d'importation
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
                    Text("Nouveaux enregistrements : ${summary.createdCount}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF059669)))
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
}

@Composable
private fun PartnerBalanceCard(
    item: PartnerBalanceItem,
    isClient: Boolean,
    canModifyCreance: Boolean,
    canDeleteCreance: Boolean,
    onNewPayment: () -> Unit,
    onModifyCreance: () -> Unit,
    onDeleteCreance: () -> Unit,
    onViewStatement: () -> Unit,
    onSharePdf: () -> Unit,
    onCall: (String) -> Unit
) {
    val partner = item.partner
    val hasDebt = item.soldeRestant > 0.01
    val totalDueBase = (item.totalReglements + item.soldeRestant).coerceAtLeast(item.totalTransactions)
    val progress = if (totalDueBase > 0.0) {
        (item.totalReglements / totalDueBase).coerceIn(0.0, 1.0).toFloat()
    } else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onViewStatement() }
            .testTag("partner_balance_card_${partner.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name & Solde
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isClient) ClientColor.copy(alpha = 0.12f) else FournisseurColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isClient) Icons.Default.Person else Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = if (isClient) ClientColor else FournisseurColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = partner.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (partner.phone.isNotBlank() || partner.address.isNotBlank()) {
                            Text(
                                text = listOf(partner.phone, partner.address).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Solde Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasDebt) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF10B981).copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (hasDebt) if (isClient) "Créance Due" else "Dette Due" else "Soldé",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = if (hasDebt) Color(0xFFDC2626) else Color(0xFF059669)
                        )
                        Text(
                            text = CurrencyUtils.formatDa(item.soldeRestant),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (hasDebt) Color(0xFFDC2626) else Color(0xFF059669)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transaction Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isClient) "Total Facturé / Livré" else "Total Acheté",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatDa(item.totalTransactions),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isClient) "Total Encaissé" else "Total Payé",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatDa(item.totalReglements),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ReglementColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (hasDebt) Color(0xFF3B82F6) else Color(0xFF10B981),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onNewPayment,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canModifyCreance) ReglementColor else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (canModifyCreance) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_pay_${partner.id}")
                    ) {
                        Icon(
                            imageVector = if (canModifyCreance) Icons.Default.Payments else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isClient) "Encaisser" else "Régler",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onViewStatement,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_statement_${partner.id}")
                    ) {
                        Text(
                            text = "Relevé",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (partner.phone.isNotBlank()) {
                        IconButton(
                            onClick = { onCall(partner.phone) },
                            modifier = Modifier.size(36.dp).testTag("btn_call_${partner.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Appeler",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSharePdf,
                        modifier = Modifier.size(36.dp).testTag("btn_pdf_${partner.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "PDF Relevé",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Boutons explicites de Modification et Suppression de Créance (protégés par droits employé)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onModifyCreance,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_edit_creance_${partner.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (canModifyCreance) {
                            Color(0xFFFFA726)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (canModifyCreance) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (canModifyCreance) Icons.Default.Edit else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (canModifyCreance) "Modifier Créance" else "Modif. Créance (Verr.)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                FilledTonalButton(
                    onClick = onDeleteCreance,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_delete_creance_${partner.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (canDeleteCreance) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (canDeleteCreance) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (canDeleteCreance) Icons.Default.Delete else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (canDeleteCreance) "Remise à zéro (0 DA)" else "Remise à 0 (Verr.)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
