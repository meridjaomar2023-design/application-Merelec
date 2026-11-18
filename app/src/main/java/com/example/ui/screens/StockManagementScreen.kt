package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNotifications
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.OperationType
import com.example.data.model.ProductCatalogEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.StockMovementType
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.ArticleColumnSpec
import com.example.util.ArticleImportField
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.ExportUtils
import com.example.util.ImportUtils
import java.util.Locale

private val EntryGreen = Color(0xFF059669)
private val ExitRed = Color(0xFFDC2626)
private val AlertAmber = Color(0xFFD97706)

private enum class StockFilterMode(val label: String) {
    ALL("Tous les articles"),
    LOW_STOCK("Seuil bas & Rupture"),
    OUT_OF_STOCK("En rupture (0)"),
    OK_STOCK("Stock suffisant")
}

private enum class MovementFilterMode(val label: String) {
    ALL("Tous"),
    ENTREES("Entrées (+)"),
    SORTIES("Sorties (-)"),
    BONS_LIVRAISON("Bons de Livraison")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockManagementScreen(
    viewModel: OperationViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val movements by viewModel.allStockMovements.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val allOperations by viewModel.allOperations.collectAsStateWithLifecycle()
    val lastImportSummary by viewModel.lastImportSummary.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val isAdminUser = currentEmployee.isAdmin

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var stockFilter by remember { mutableStateOf(StockFilterMode.ALL) }
    var movementFilter by remember { mutableStateOf(MovementFilterMode.ALL) }

    // Dialog states
    var showMovementDialog by remember { mutableStateOf(false) }
    var preselectedProductForMovement by remember { mutableStateOf<ProductCatalogEntity?>(null) }
    var preselectedMovementType by remember { mutableStateOf(StockMovementType.ENTREE) }

    var productForThresholdDialog by remember { mutableStateOf<ProductCatalogEntity?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductCatalogEntity?>(null) }
    var movementToDelete by remember { mutableStateOf<StockMovementEntity?>(null) }

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

    if (movementToDelete != null) {
        val target = movementToDelete!!
        AlertDialog(
            onDismissRequest = { movementToDelete = null },
            title = { Text("Supprimer le mouvement de stock ?") },
            text = { Text("Confirmez-vous la suppression du mouvement pour « ${target.productName} » (${CurrencyUtils.formatQty(target.quantity)} ${target.unit}) ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStockMovement(target.id)
                        movementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { movementToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Importation d'articles (CSV / XLS / XLSX) avec mêmes options de colonnes que Client
    var showArticleImportPanel by remember { mutableStateOf(false) }
    var articleImportColumns by remember { mutableStateOf(ImportUtils.defaultArticleColumns()) }
    val currentArticleColumnsState by rememberUpdatedState(articleImportColumns)
    var showAddExtraImportColumnDialog by remember { mutableStateOf(false) }
    var extraColumnHeaderInput by remember { mutableStateOf("") }
    var selectedExtraArticleField by remember { mutableStateOf(ArticleImportField.COLONNE_PLUS) }
    var showPasteArticlesDialog by remember { mutableStateOf(false) }
    var pasteArticlesText by remember { mutableStateOf("") }

    val importProductsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importProductsFromUri(context, it, currentArticleColumnsState)
            showArticleImportPanel = false
        }
    }

    BackHandler {
        viewModel.selectBottomTab(BottomNavTab.VENTES)
    }

    // Calcul des KPIs de Stock
    val totalStockValuation = remember(products) {
        products.sumOf { (it.defaultUnitPrice ?: 0.0) * it.stockActuel }
    }
    val totalEntriesQty = remember(movements) {
        movements.filter { it.type == StockMovementType.ENTREE }.sumOf { it.quantity }
    }
    val totalExitsQty = remember(movements) {
        movements.filter { it.type == StockMovementType.SORTIE }.sumOf { it.quantity }
    }
    val outOfStockCount = remember(products) {
        products.count { it.stockActuel <= 0.0 }
    }

    // Filtrage des articles
    val filteredProducts = remember(products, searchQuery, stockFilter) {
        val q = searchQuery.trim().lowercase(Locale.FRENCH)
        products.filter { prod ->
            val matchesSearch = q.isEmpty() ||
                    prod.name.lowercase(Locale.FRENCH).contains(q) ||
                    prod.category.lowercase(Locale.FRENCH).contains(q) ||
                    prod.marque.lowercase(Locale.FRENCH).contains(q) ||
                    prod.reference.lowercase(Locale.FRENCH).contains(q) ||
                    prod.barcode.lowercase(Locale.FRENCH).contains(q)

            val matchesFilter = when (stockFilter) {
                StockFilterMode.ALL -> true
                StockFilterMode.LOW_STOCK -> prod.stockActuel <= prod.seuilAlerte
                StockFilterMode.OUT_OF_STOCK -> prod.stockActuel <= 0.0
                StockFilterMode.OK_STOCK -> prod.stockActuel > prod.seuilAlerte
            }
            matchesSearch && matchesFilter
        }
    }

    // Filtrage des mouvements
    val filteredMovements = remember(movements, searchQuery, movementFilter) {
        val q = searchQuery.trim().lowercase(Locale.FRENCH)
        movements.filter { mov ->
            val matchesSearch = q.isEmpty() ||
                    mov.productName.lowercase(Locale.FRENCH).contains(q) ||
                    mov.partnerName.lowercase(Locale.FRENCH).contains(q) ||
                    (mov.operationId?.lowercase(Locale.FRENCH)?.contains(q) == true) ||
                    mov.reason.lowercase(Locale.FRENCH).contains(q)

            val matchesType = when (movementFilter) {
                MovementFilterMode.ALL -> true
                MovementFilterMode.ENTREES -> mov.type == StockMovementType.ENTREE
                MovementFilterMode.SORTIES -> mov.type == StockMovementType.SORTIE
                MovementFilterMode.BONS_LIVRAISON -> !mov.operationId.isNullOrBlank()
            }
            matchesSearch && matchesType
        }
    }

    // Dialogue Enregistrement Entrée / Sortie de Marchandise
    if (showMovementDialog) {
        RecordStockMovementDialog(
            products = products,
            initialProduct = preselectedProductForMovement,
            initialType = preselectedMovementType,
            availableBls = allOperations.map { it.operation.id },
            onDismiss = { showMovementDialog = false },
            onConfirm = { product, type, qty, partner, reason, blId ->
                showMovementDialog = false
                viewModel.recordManualStockMovement(
                    product = product,
                    type = type,
                    quantity = qty,
                    partnerName = partner,
                    reason = reason,
                    operationId = blId
                )
            }
        )
    }

    // Dialogue Configuration du Seuil d'Alerte Bas
    if (productForThresholdDialog != null) {
        EditThresholdDialog(
            product = productForThresholdDialog!!,
            onDismiss = { productForThresholdDialog = null },
            onSave = { newThreshold ->
                viewModel.updateSeuilAlerte(productForThresholdDialog!!.id, newThreshold)
                productForThresholdDialog = null
            }
        )
    }

    // Dialogue Création Rapide d'un Article avec Stock Initial & Seuil
    if (showAddProductDialog) {
        QuickAddStockArticleDialog(
            isAdmin = isAdminUser,
            onDismiss = { showAddProductDialog = false },
            onConfirm = { name, prixAchat, price, category, marque, unit, initialStock, threshold, ref ->
                showAddProductDialog = false
                viewModel.addSingleProduct(
                    name = name,
                    priceText = price,
                    category = category,
                    famille = category,
                    marque = marque,
                    prixAchatText = prixAchat,
                    prixVenteText = price,
                    unit = unit,
                    stock = initialStock,
                    seuilAlerte = threshold,
                    reference = ref
                )
            }
        )
    }

    if (showAddExtraImportColumnDialog) {
        AlertDialog(
            onDismissRequest = { showAddExtraImportColumnDialog = false },
            title = { Text("Ajouter une colonne en plus (Articles)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choisissez le type de champ ou ajoutez une colonne libre :",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                                        text = "${field.displayName} (${field.defaultHeader})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = extraColumnHeaderInput,
                        onValueChange = { extraColumnHeaderInput = it },
                        label = { Text("Nom de la colonne dans votre fichier") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val headerName = extraColumnHeaderInput.trim().ifBlank { selectedExtraArticleField.defaultHeader }
                        articleImportColumns = articleImportColumns + ArticleColumnSpec(
                            field = selectedExtraArticleField,
                            customHeader = headerName,
                            enabled = true,
                            isExtraColumn = true
                        )
                        extraColumnHeaderInput = ""
                        showAddExtraImportColumnDialog = false
                    }
                ) {
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

    if (showPasteArticlesDialog) {
        val activeArticleHeader = articleImportColumns.filter { it.enabled }.joinToString(" ; ") { it.customHeader }
        AlertDialog(
            onDismissRequest = { showPasteArticlesDialog = false },
            title = { Text("Coller une liste d'articles (CSV / Excel)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ordre des colonnes configuré :\n$activeArticleHeader",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pasteArticlesText,
                        onValueChange = { pasteArticlesText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = {
                            Text("Disjoncteur 16A; REF-001; 613000123456; Protection; Schneider; 520; 680; 50; U; 5\nCâble 2.5mm; REF-002; 613000100025; Câblage; Elsewedy; 3800; 4500; 25; Rlx; 5")
                        },
                        minLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteArticlesText.isNotBlank()) {
                            viewModel.importProducts(pasteArticlesText, articleImportColumns)
                            pasteArticlesText = ""
                            showPasteArticlesDialog = false
                            showArticleImportPanel = false
                        }
                    }
                ) {
                    Text("Importer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteArticlesDialog = false }) {
                    Text("Annuler")
                }
            }
        )
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
                                contentDescription = "Menu latéral"
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Gestion des Stocks",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Entrées, Sorties & Alertes Seuil Bas • ${companySettings.companyName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Bouton rapide Importation d'Articles (CSV / XLS / XLSX)
                    IconButton(
                        onClick = { showArticleImportPanel = !showArticleImportPanel },
                        modifier = Modifier.testTag("stock_import_articles_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Importer des articles (CSV / XLS / XLSX)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Bouton rapide création d'article en stock
                    IconButton(
                        onClick = { showAddProductDialog = true },
                        modifier = Modifier.testTag("stock_add_product_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Nouvel article",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Bouton rapide Nouveau BL Fournisseur (Entrée marchandise)
                    IconButton(
                        onClick = { viewModel.startNewOperation(OperationType.BL_FOURNISSEUR) },
                        modifier = Modifier.testTag("stock_new_bl_fournisseur_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = "Réception BL Fournisseur",
                            tint = EntryGreen
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
                    preselectedProductForMovement = products.firstOrNull()
                    preselectedMovementType = StockMovementType.ENTREE
                    showMovementDialog = true
                },
                icon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
                text = { Text("Entrée / Sortie") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("stock_new_movement_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Onglets du module Stock
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            "État du Stock (${products.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_stock_inventory")
                )

                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            "Entrées / Sorties (${movements.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_stock_movements")
                )

                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Seuil Bas",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (lowStockProducts.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = ExitRed, contentColor = Color.White) {
                                    Text("${lowStockProducts.size}")
                                }
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (lowStockProducts.isNotEmpty()) ExitRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_stock_alerts")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Synthèse KPI des Stocks
                item {
                    StockKpiHeaderCard(
                        totalProducts = products.size,
                        totalValuation = totalStockValuation,
                        totalEntries = totalEntriesQty,
                        totalExits = totalExitsQty,
                        lowStockCount = lowStockProducts.size,
                        outOfStockCount = outOfStockCount,
                        onClickAlerts = { selectedTabIndex = 2 }
                    )
                }

                // Bannière d'alerte seuil bas si des produits sont critiques et qu'on n'est pas déjà sur l'onglet Alertes
                if (lowStockProducts.isNotEmpty() && selectedTabIndex != 2) {
                    item {
                        LowStockAlertBanner(
                            lowStockProducts = lowStockProducts,
                            onViewAlerts = { selectedTabIndex = 2 },
                            onQuickRestock = { prod ->
                                preselectedProductForMovement = prod
                                preselectedMovementType = StockMovementType.ENTREE
                                showMovementDialog = true
                            }
                        )
                    }
                }

                // Barre d'action rapide : Importer Articles (CSV / XLS / XLSX) + Nouvel Article
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showArticleImportPanel = !showArticleImportPanel },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("stock_open_article_import_panel_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showArticleImportPanel) "Masquer volet import" else "Importer Articles (CSV / XLS)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                        FilledTonalButton(
                            onClick = { showAddProductDialog = true },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("stock_quick_add_article_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nouvel Article",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Volet d'Importation Articles (CSV / XLS / XLSX) avec mêmes options de colonnes que Client
                if (showArticleImportPanel) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("stock_organized_article_import_panel"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                            text = "Volet d'Importation Articles (CSV / XLS / XLSX)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Cochez les colonnes à importer, renommez-les si besoin et organisez leur ordre (▲ / ▼) :",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { articleImportColumns = ImportUtils.defaultArticleColumns() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = "Réinitialiser l'ordre par défaut",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { showArticleImportPanel = false },
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

                                val previewOrderText = articleImportColumns
                                    .filter { it.enabled }
                                    .mapIndexed { idx, col -> "${idx + 1}. ${col.customHeader}" }
                                    .joinToString("  ;  ")

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            text = "Ordre actif des colonnes (même ordre que la création d'article) :",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = previewOrderText.ifBlank { "Aucune colonne sélectionnée" },
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
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
                                                    color = if (enabledRank != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
                                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Monter", modifier = Modifier.size(16.dp))
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
                                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre", modifier = Modifier.size(16.dp))
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
                                                            contentDescription = "Supprimer colonne",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        extraColumnHeaderInput = ""
                                        selectedExtraArticleField = ArticleImportField.COLONNE_PLUS
                                        showAddExtraImportColumnDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ajouter une colonne en plus dans les choix",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { importProductsLauncher.launch("*/*") },
                                        modifier = Modifier.weight(1.3f),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Choisir fichier CSV / XLS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }

                                    OutlinedButton(
                                        onClick = { showPasteArticlesDialog = true },
                                        modifier = Modifier.weight(0.9f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Coller", style = MaterialTheme.typography.labelSmall)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            ExportUtils.shareText(
                                                context,
                                                "Modele_Articles_Merelec.csv",
                                                ImportUtils.generateProductsTemplateCsv(articleImportColumns)
                                            )
                                        },
                                        modifier = Modifier.weight(0.9f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Modèle CSV", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                lastImportSummary?.let { summary ->
                    item {
                        val isErr = summary.errorMessage != null
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isErr) MaterialTheme.colorScheme.errorContainer else EntryGreen.copy(alpha = 0.14f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isErr) "Erreur d'importation (${summary.entityLabel})" else "Importation ${summary.entityLabel} réussie (${summary.fileFormat})",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isErr) MaterialTheme.colorScheme.onErrorContainer else EntryGreen
                                    )
                                    Text(
                                        text = summary.errorMessage ?: "${summary.totalImported} synchronisé(s) : ${summary.createdCount} créé(s), ${summary.updatedCount} mis à jour",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { viewModel.clearImportSummary() }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Fermer", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Barre de recherche commune
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stock_search_input"),
                            placeholder = {
                                Text(
                                    if (selectedTabIndex == 1) "Rechercher article, N° BL, client/fournisseur..."
                                    else "Rechercher article, référence, catégorie..."
                                )
                            },
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
                    }
                }

                when (selectedTabIndex) {
                    0 -> {
                        // Filtres État du Stock
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StockFilterMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = stockFilter == mode,
                                        onClick = { stockFilter = mode },
                                        label = { Text(mode.label) },
                                        leadingIcon = if (stockFilter == mode) {
                                            { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier.testTag("stock_filter_${mode.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        if (filteredProducts.isEmpty()) {
                            item {
                                EmptyStockSection(
                                    title = "Aucun article trouvé",
                                    subtitle = "Ajoutez un article ou modifiez vos filtres de recherche."
                                )
                            }
                        } else {
                            items(filteredProducts, key = { it.id }) { product ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                                    StockProductItemCard(
                                        product = product,
                                        onEntryClick = {
                                            preselectedProductForMovement = product
                                            preselectedMovementType = StockMovementType.ENTREE
                                            showMovementDialog = true
                                        },
                                        onExitClick = {
                                            preselectedProductForMovement = product
                                            preselectedMovementType = StockMovementType.SORTIE
                                            showMovementDialog = true
                                        },
                                        onThresholdClick = {
                                            productForThresholdDialog = product
                                        },
                                        onDeleteClick = {
                                            productToDelete = product
                                        }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // Filtres Mouvements Entrées / Sorties
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MovementFilterMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = movementFilter == mode,
                                        onClick = { movementFilter = mode },
                                        label = { Text(mode.label) },
                                        modifier = Modifier.testTag("movement_filter_${mode.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Boutons rapides création Bon de Livraison Fournisseur (Entrée) ou BL Client (Sortie)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { viewModel.startNewOperation(OperationType.BL_FOURNISSEUR) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = EntryGreen.copy(alpha = 0.14f),
                                        contentColor = EntryGreen
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stock_create_bl_fournisseur_btn")
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Nouveau BL Entrée", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }

                                FilledTonalButton(
                                    onClick = { viewModel.startNewOperation(OperationType.BL_CLIENT) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = ExitRed.copy(alpha = 0.12f),
                                        contentColor = ExitRed
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stock_create_bl_client_btn")
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Nouveau BL Sortie", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        if (filteredMovements.isEmpty()) {
                            item {
                                EmptyStockSection(
                                    title = "Aucun mouvement d'entrée ou sortie",
                                    subtitle = "Les entrées (BL Fournisseur) et sorties (BL Client / Facture) ainsi que les ajustements apparaîtront ici."
                                )
                            }
                        } else {
                            items(filteredMovements, key = { it.id }) { mov ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                    StockMovementCard(
                                        movement = mov,
                                        onOpenOperation = { opId ->
                                            viewModel.navigateTo(AppScreen.Detail(opId))
                                        },
                                        onDelete = {
                                            movementToDelete = mov
                                        }
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // Vue dédiée aux Alertes de Seuil Bas
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (lowStockProducts.isEmpty()) EntryGreen.copy(alpha = 0.1f) else ExitRed.copy(alpha = 0.08f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (lowStockProducts.isEmpty()) Icons.Default.Inventory2 else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (lowStockProducts.isEmpty()) EntryGreen else ExitRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lowStockProducts.isEmpty()) {
                                                "Aucune alerte de seuil bas"
                                            } else {
                                                "${lowStockProducts.size} article(s) en alerte de seuil bas ($outOfStockCount en rupture)"
                                            },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (lowStockProducts.isEmpty()) EntryGreen else ExitRed
                                        )
                                        Text(
                                            text = "Chaque article dont le stock actuel est inférieur ou égal au seuil configuré déclenche une alerte automatique.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        val alertList = lowStockProducts.filter {
                            val q = searchQuery.trim().lowercase(Locale.FRENCH)
                            q.isEmpty() || it.name.lowercase(Locale.FRENCH).contains(q) || it.category.lowercase(Locale.FRENCH).contains(q)
                        }

                        if (alertList.isEmpty()) {
                            item {
                                EmptyStockSection(
                                    title = "Tous vos stocks sont au-dessus du seuil d'alerte",
                                    subtitle = "Vous pouvez personnaliser le seuil d'alerte de chaque article depuis l'onglet État du Stock."
                                )
                            }
                        } else {
                            items(alertList, key = { it.id }) { prod ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                                    LowStockAlertItemCard(
                                        product = prod,
                                        onRestockClick = {
                                            preselectedProductForMovement = prod
                                            preselectedMovementType = StockMovementType.ENTREE
                                            showMovementDialog = true
                                        },
                                        onOrderBlFournisseur = {
                                            viewModel.startNewOperation(OperationType.BL_FOURNISSEUR)
                                        },
                                        onEditThreshold = {
                                            productForThresholdDialog = prod
                                        }
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

@Composable
private fun StockKpiHeaderCard(
    totalProducts: Int,
    totalValuation: Double,
    totalEntries: Double,
    totalExits: Double,
    lowStockCount: Int,
    outOfStockCount: Int,
    onClickAlerts: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("stock_kpi_header_card"),
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
                Column {
                    Text(
                        text = "Valeur Totale du Stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatDa(totalValuation),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "$totalProducts référence(s)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Entrées
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = EntryGreen.copy(alpha = 0.09f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = EntryGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Entrées", style = MaterialTheme.typography.labelSmall, color = EntryGreen)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+${CurrencyUtils.formatQty(totalEntries)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = EntryGreen
                        )
                    }
                }

                // Sorties
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = ExitRed.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExitRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sorties", style = MaterialTheme.typography.labelSmall, color = ExitRed)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "-${CurrencyUtils.formatQty(totalExits)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ExitRed
                        )
                    }
                }

                // Alertes Seuil Bas
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClickAlerts() },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lowStockCount > 0) AlertAmber.copy(alpha = 0.14f) else EntryGreen.copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (lowStockCount > 0) AlertAmber else EntryGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Seuil Bas",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (lowStockCount > 0) AlertAmber else EntryGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$lowStockCount alerte(s)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (lowStockCount > 0) ExitRed else EntryGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockAlertBanner(
    lowStockProducts: List<ProductCatalogEntity>,
    onViewAlerts: () -> Unit,
    onQuickRestock: (ProductCatalogEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("stock_low_alert_banner"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = ExitRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Alerte Stock Bas : ${lowStockProducts.size} article(s) sous le seuil",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF991B1B)
                        )
                        val previewNames = lowStockProducts.take(3).joinToString(", ") {
                            "${it.name} (${CurrencyUtils.formatQty(it.stockActuel)}/${CurrencyUtils.formatQty(it.seuilAlerte)})"
                        }
                        Text(
                            text = previewNames,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB91C1C),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { onQuickRestock(lowStockProducts.first()) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EntryGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text("+ Entrée", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    TextButton(
                        onClick = onViewAlerts,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Voir tout", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ExitRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockProductItemCard(
    product: ProductCatalogEntity,
    onEntryClick: () -> Unit,
    onExitClick: () -> Unit,
    onThresholdClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOutOfStock = product.stockActuel <= 0.0
    val isLowStock = !isOutOfStock && product.stockActuel <= product.seuilAlerte
    val statusColor = when {
        isOutOfStock -> ExitRed
        isLowStock -> AlertAmber
        else -> EntryGreen
    }
    val statusLabel = when {
        isOutOfStock -> "RUPTURE DE STOCK"
        isLowStock -> "ALERTE SEUIL BAS"
        else -> "EN STOCK"
    }

    val targetMax = (product.seuilAlerte * 3.0).coerceAtLeast(10.0)
    val progress = (product.stockActuel / targetMax).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stock_product_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (product.category.isNotBlank()) {
                            Text(
                                text = "• ${product.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (product.reference.isNotBlank() || product.defaultUnitPrice != null) {
                        val refPart = if (product.reference.isNotBlank()) "Réf: ${product.reference} • " else ""
                        val pricePart = product.defaultUnitPrice?.let { "PU: ${CurrencyUtils.formatDa(it)}" } ?: ""
                        Text(
                            text = "$refPart$pricePart",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${CurrencyUtils.formatQty(product.stockActuel)} ${product.unit}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = statusColor
                    )
                    Text(
                        text = "Seuil min: ${CurrencyUtils.formatQty(product.seuilAlerte)} ${product.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bouton + Entrée
                FilledTonalButton(
                    onClick = onEntryClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = EntryGreen.copy(alpha = 0.14f),
                        contentColor = EntryGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("stock_entry_btn_${product.id}")
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Entrée", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                // Bouton - Sortie
                FilledTonalButton(
                    onClick = onExitClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ExitRed.copy(alpha = 0.12f),
                        contentColor = ExitRed
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("stock_exit_btn_${product.id}")
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("- Sortie", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                // Bouton Seuil d'alerte
                OutlinedButton(
                    onClick = onThresholdClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("stock_threshold_btn_${product.id}")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Seuil", style = MaterialTheme.typography.labelSmall)
                }

                // Bouton Supprimer avec confirmation
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("stock_delete_product_btn_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer l'article",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StockMovementCard(
    movement: StockMovementEntity,
    onOpenOperation: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isEntry = movement.type == StockMovementType.ENTREE
    val accentColor = when (movement.type) {
        StockMovementType.ENTREE -> EntryGreen
        StockMovementType.SORTIE -> ExitRed
        StockMovementType.AJUSTEMENT -> AlertAmber
    }
    val signPrefix = when (movement.type) {
        StockMovementType.ENTREE -> "+"
        StockMovementType.SORTIE -> "-"
        StockMovementType.AJUSTEMENT -> "="
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stock_movement_card_${movement.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEntry) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = movement.productName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$signPrefix${CurrencyUtils.formatQty(movement.quantity)} ${movement.unit}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${movement.type.label} • ${DateUtils.formatDateTime(movement.timestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Stock: ${CurrencyUtils.formatQty(movement.previousStock)} → ${CurrencyUtils.formatQty(movement.newStock)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (movement.partnerName.isNotBlank() || !movement.operationId.isNullOrBlank() || movement.reason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!movement.operationId.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable { onOpenOperation(movement.operationId) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = movement.operationId,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        val infoText = buildString {
                            if (movement.partnerName.isNotBlank()) append(movement.partnerName)
                            if (movement.reason.isNotBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append(movement.reason)
                            }
                        }
                        if (infoText.isNotBlank()) {
                            Text(
                                text = infoText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer mouvement",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockAlertItemCard(
    product: ProductCatalogEntity,
    onRestockClick: () -> Unit,
    onOrderBlFournisseur: () -> Unit,
    onEditThreshold: () -> Unit
) {
    val isOutOfStock = product.stockActuel <= 0.0
    val deficit = (product.seuilAlerte - product.stockActuel).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("low_stock_alert_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isOutOfStock) ExitRed else AlertAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = if (isOutOfStock) {
                                "RUPTURE DE STOCK • Seuil minimum : ${CurrencyUtils.formatQty(product.seuilAlerte)} ${product.unit}"
                            } else {
                                "Stock bas • Manque ${CurrencyUtils.formatQty(deficit)} ${product.unit} pour atteindre le seuil (${CurrencyUtils.formatQty(product.seuilAlerte)})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOutOfStock) ExitRed else AlertAmber
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${CurrencyUtils.formatQty(product.stockActuel)} ${product.unit}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isOutOfStock) ExitRed else AlertAmber
                    )
                    Text(
                        text = "Seuil : ${CurrencyUtils.formatQty(product.seuilAlerte)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4B5563)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestockClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EntryGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("alert_restock_btn_${product.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Réapprovisionner", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = onOrderBlFournisseur,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("alert_bl_fournisseur_btn_${product.id}")
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BL Fournisseur", style = MaterialTheme.typography.labelSmall)
                }

                IconButton(
                    onClick = onEditThreshold,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNotifications,
                        contentDescription = "Modifier le seuil",
                        tint = Color(0xFF4B5563)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordStockMovementDialog(
    products: List<ProductCatalogEntity>,
    initialProduct: ProductCatalogEntity?,
    initialType: StockMovementType,
    availableBls: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (ProductCatalogEntity, StockMovementType, Double, String, String, String?) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(initialProduct ?: products.firstOrNull()) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var movementType by remember { mutableStateOf(initialType) }
    var quantityText by remember { mutableStateOf("1") }
    var partnerName by remember { mutableStateOf("") }
    var operationId by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mouvement de Stock (Entrée / Sortie)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            if (products.isEmpty()) {
                Text("Aucun article dans le catalogue. Veuillez d'abord créer un article.")
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Type de mouvement : Entrée / Sortie / Ajustement
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StockMovementType.entries.forEach { type ->
                            FilterChip(
                                selected = movementType == type,
                                onClick = { movementType = type },
                                label = {
                                    Text(
                                        when (type) {
                                            StockMovementType.ENTREE -> "+ Entrée"
                                            StockMovementType.SORTIE -> "- Sortie"
                                            StockMovementType.AJUSTEMENT -> "Inventaire"
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Sélection de l'article
                    ExposedDropdownMenuBox(
                        expanded = productDropdownExpanded,
                        onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.let { "${it.name} (Stock: ${CurrencyUtils.formatQty(it.stockActuel)} ${it.unit})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Article concerné") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = productDropdownExpanded,
                            onDismissRequest = { productDropdownExpanded = false }
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(prod.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Stock actuel : ${CurrencyUtils.formatQty(prod.stockActuel)} ${prod.unit} • Seuil : ${CurrencyUtils.formatQty(prod.seuilAlerte)}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = prod
                                        productDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = {
                            Text(
                                if (movementType == StockMovementType.AJUSTEMENT)
                                    "Nouveau stock réel (${selectedProduct?.unit ?: "U"})"
                                else
                                    "Quantité (${selectedProduct?.unit ?: "U"})"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("movement_dialog_qty_input")
                    )

                    OutlinedTextField(
                        value = operationId,
                        onValueChange = { operationId = it },
                        label = { Text("N° Bon de Livraison associé (optionnel)") },
                        placeholder = { Text(availableBls.firstOrNull() ?: "Ex: BLC-2026-001 ou BLF-2026-001") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = partnerName,
                        onValueChange = { partnerName = it },
                        label = {
                            Text(
                                if (movementType == StockMovementType.ENTREE) "Fournisseur (optionnel)"
                                else "Client / Chantier (optionnel)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motif / Observation") },
                        placeholder = { Text("Ex: Réception marchandise, sortie chantier...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (products.isNotEmpty() && selectedProduct != null) {
                Button(
                    onClick = {
                        val qty = CurrencyUtils.parseAmount(quantityText)
                        onConfirm(
                            selectedProduct!!,
                            movementType,
                            qty,
                            partnerName,
                            reason,
                            operationId.takeIf { it.isNotBlank() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (movementType == StockMovementType.SORTIE) ExitRed else EntryGreen
                    ),
                    modifier = Modifier.testTag("confirm_stock_movement_btn")
                ) {
                    Text("Valider")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun EditThresholdDialog(
    product: ProductCatalogEntity,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var thresholdText by remember(product) {
        mutableStateOf(CurrencyUtils.formatQty(product.seuilAlerte))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = AlertAmber
            )
        },
        title = {
            Text(
                text = "Seuil d'alerte bas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Définissez la quantité minimale pour « ${product.name} ». Une alerte s'affichera dès que le stock descendra à ce niveau ou en dessous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text("Seuil minimum (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("threshold_dialog_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = CurrencyUtils.parseAmount(thresholdText).coerceAtLeast(0.0)
                    onSave(parsed)
                },
                modifier = Modifier.testTag("threshold_dialog_save_btn")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun QuickAddStockArticleDialog(
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Général") }
    var marque by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("U") }
    var prixAchatText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var stockInitialText by remember { mutableStateOf("10") }
    var thresholdText by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nouvel Article en Stock",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Désignation de l'article *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("Référence") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Famille") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = marque,
                        onValueChange = { marque = it },
                        label = { Text("Marque") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isAdmin) {
                        OutlinedTextField(
                            value = prixAchatText,
                            onValueChange = { prixAchatText = it },
                            label = { Text("Prix d'achat (DA)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prix de vente (DA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockInitialText,
                        onValueChange = { stockInitialText = it },
                        label = { Text("Stock initial") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it },
                        label = { Text("Seuil alerte bas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            prixAchatText,
                            priceText,
                            category,
                            marque,
                            unit,
                            CurrencyUtils.parseAmount(stockInitialText),
                            CurrencyUtils.parseAmount(thresholdText),
                            reference
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Ajouter au Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun EmptyStockSection(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}
