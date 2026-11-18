package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.OperationType
import com.example.data.model.PeriodBucket
import com.example.data.model.PeriodType
import com.example.ui.components.charts.PeriodicBarChart
import com.example.ui.components.charts.RecoveryDonutChart
import com.example.ui.components.charts.TrendAreaChart
import com.example.ui.theme.ClientColor
import com.example.ui.theme.FournisseurColor
import com.example.ui.theme.ReglementColor
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.AnalyticsCalculator
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.ExportUtils
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.MoreVert
import java.util.Calendar

enum class TimeframeFilter(val label: String) {
    DAYS_7("7 Jours"),
    DAYS_30("30 Jours"),
    THIS_MONTH("Ce Mois"),
    THIS_YEAR("Cette Année"),
    ALL("Tout")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: OperationViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allOperations by viewModel.allOperations.collectAsStateWithLifecycle()
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val products by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val creancesSummary by viewModel.creancesSummary.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.selectBottomTab(BottomNavTab.VENTES)
    }

    var selectedTimeframe by remember { mutableStateOf(TimeframeFilter.ALL) }
    var selectedPeriodType by remember { mutableStateOf(PeriodType.MENSUEL) }
    var selectedBucket by remember { mutableStateOf<PeriodBucket?>(null) }

    // Filtrage dynamique des opérations selon le cadre temporel choisi
    val filteredOperations = remember(allOperations, selectedTimeframe) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        when (selectedTimeframe) {
            TimeframeFilter.DAYS_7 -> {
                val threshold = now - 7L * 24 * 3600 * 1000
                allOperations.filter { it.operation.dateTime >= threshold }
            }
            TimeframeFilter.DAYS_30 -> {
                val threshold = now - 30L * 24 * 3600 * 1000
                allOperations.filter { it.operation.dateTime >= threshold }
            }
            TimeframeFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val threshold = cal.timeInMillis
                allOperations.filter { it.operation.dateTime >= threshold }
            }
            TimeframeFilter.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val threshold = cal.timeInMillis
                allOperations.filter { it.operation.dateTime >= threshold }
            }
            TimeframeFilter.ALL -> allOperations
        }
    }

    // Calcul du résumé analytique dynamique
    val dashboardSummary = remember(filteredOperations, selectedPeriodType) {
        AnalyticsCalculator.computeSummary(
            operations = filteredOperations,
            periodType = selectedPeriodType,
            periodCount = if (selectedPeriodType == PeriodType.MENSUEL) 12 else 8
        )
    }

    // Top clients contribuant au Chiffre d'Affaires
    val topClients = remember(filteredOperations) {
        val clientSales = mutableMapOf<String, Double>()
        for (opWithLignes in filteredOperations) {
            val op = opWithLignes.operation
            if (op.typeOperation == OperationType.BL_CLIENT || op.typeOperation == OperationType.FACTURE) {
                val name = op.clientFournisseur.trim()
                if (name.isNotBlank()) {
                    clientSales[name] = (clientSales[name] ?: 0.0) + opWithLignes.totalTtc
                }
            } else if (op.typeOperation == OperationType.AVOIR_CLIENT) {
                val name = op.clientFournisseur.trim()
                if (name.isNotBlank()) {
                    clientSales[name] = ((clientSales[name] ?: 0.0) - opWithLignes.totalTtc).coerceAtLeast(0.0)
                }
            }
        }
        clientSales.entries.sortedByDescending { it.value }.take(5)
    }

    // Top articles vendus (par montant et quantité)
    val topArticles = remember(filteredOperations) {
        val articleStats = mutableMapOf<String, Pair<Double, Double>>() // Name -> (Quantity, TotalTtc)
        for (opWithLignes in filteredOperations) {
            val op = opWithLignes.operation
            if (op.typeOperation == OperationType.BL_CLIENT || op.typeOperation == OperationType.FACTURE) {
                for (ligne in opWithLignes.lignes) {
                    val name = ligne.produit.trim()
                    if (name.isNotBlank()) {
                        val current = articleStats[name] ?: Pair(0.0, 0.0)
                        val lineTotal = ligne.quantite * (ligne.prixUnitaire ?: 0.0)
                        articleStats[name] = Pair(current.first + ligne.quantite, current.second + lineTotal)
                    }
                }
            }
        }
        articleStats.entries.sortedByDescending { it.value.second }.take(5)
    }

    val soldeNet = dashboardSummary.totalCa - dashboardSummary.totalAchats
    val resteARecouvrer = (dashboardSummary.totalCa - dashboardSummary.totalReglements).coerceAtLeast(0.0)

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
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tableau de Bord Dynamique",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${companySettings.companyName} • Analyses & Performance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.BackupDrive) },
                        modifier = Modifier.testTag("dashboard_drive_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddToDrive,
                            contentDescription = "Drive",
                            tint = Color(0xFF0F9D58)
                        )
                    }

                    IconButton(
                        onClick = { ExportUtils.exportToExcelFile(context, allOperations) },
                        modifier = Modifier.testTag("dashboard_excel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Excel",
                            tint = Color(0xFF107C41)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.selectBottomTab(BottomNavTab.VENTES) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouvelle Vente") },
                containerColor = ClientColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("dashboard_new_sale_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filtres temporels rapides
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeframeFilter.values().forEach { filter ->
                            FilterChip(
                                selected = selectedTimeframe == filter,
                                onClick = { selectedTimeframe = filter },
                                label = { Text(filter.label) },
                                leadingIcon = if (selectedTimeframe == filter) {
                                    { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.testTag("timeframe_${filter.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sélecteur de périodicité (Mois vs Semaine)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedPeriodType == PeriodType.MENSUEL,
                            onClick = { selectedPeriodType = PeriodType.MENSUEL },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Vue Mensuelle")
                        }
                        SegmentedButton(
                            selected = selectedPeriodType == PeriodType.HEBDOMADAIRE,
                            onClick = { selectedPeriodType = PeriodType.HEBDOMADAIRE },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Vue Hebdomadaire")
                        }
                    }
                }
            }

            // Synthèse des 4 KPIs principaux (Ventes, Encaissements, Achats, Solde Net)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Chiffre d'Affaires Ventes
                        Card(
                            modifier = Modifier.weight(1f).testTag("card_kpi_ca"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ClientColor.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Chiffre d'Affaires (TTC)", style = MaterialTheme.typography.labelSmall, color = ClientColor)
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ClientColor, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyUtils.formatDa(dashboardSummary.totalCa),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ClientColor
                                )
                                dashboardSummary.caGrowthPercentage?.let { growth ->
                                    val isPos = growth >= 0
                                    Text(
                                        text = "${if (isPos) "+" else ""}${String.format("%.1f", growth)}% vs période préc.",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isPos) Color(0xFF059669) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }

                        // Total Encaissé
                        Card(
                            modifier = Modifier.weight(1f).testTag("card_kpi_reglements"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ReglementColor.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Encaissé", style = MaterialTheme.typography.labelSmall, color = ReglementColor)
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ReglementColor, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyUtils.formatDa(dashboardSummary.totalReglements),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ReglementColor
                                )
                                Text(
                                    text = "Recouvrement : ${String.format("%.1f", dashboardSummary.globalRecoveryRate)}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (dashboardSummary.globalRecoveryRate >= 75f) Color(0xFF059669) else Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Achats
                        Card(
                            modifier = Modifier.weight(1f).testTag("card_kpi_achats"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = FournisseurColor.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Achats Fournisseurs", style = MaterialTheme.typography.labelSmall, color = FournisseurColor)
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = FournisseurColor, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyUtils.formatDa(dashboardSummary.totalAchats),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = FournisseurColor
                                )
                                Text(
                                    text = "Charges marchandises",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Solde Net Commercial (CA - Achats)
                        Card(
                            modifier = Modifier.weight(1f).testTag("card_kpi_solde_net"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (soldeNet >= 0) Color(0xFF10B981).copy(alpha = 0.08f) else Color(0xFFEF4444).copy(alpha = 0.08f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Solde Net (Marge)", style = MaterialTheme.typography.labelSmall, color = if (soldeNet >= 0) Color(0xFF059669) else Color(0xFFDC2626))
                                    Icon(
                                        imageVector = if (soldeNet >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (soldeNet >= 0) Color(0xFF059669) else Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyUtils.formatDa(soldeNet),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (soldeNet >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                )
                                Text(
                                    text = if (soldeNet >= 0) "Marge positive" else "Déficit d'achats",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (soldeNet >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            // Bannière Créances en souffrance
            if (resteARecouvrer > 0.01) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { viewModel.selectBottomTab(BottomNavTab.CREANCES) }
                            .testTag("dashboard_creances_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Créances en cours : ${CurrencyUtils.formatDa(resteARecouvrer)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "${creancesSummary.countDebiteursClients} client(s) avec solde impayé",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                            TextButton(onClick = { viewModel.selectBottomTab(BottomNavTab.CREANCES) }) {
                                Text("Voir Créances", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }

            // Graphique à Barres Dynamique (Ventes vs Achats vs Règlements)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("dashboard_bar_chart_card"),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedPeriodType == PeriodType.MENSUEL) "Comparatif Mensuel (Ventes vs Règlements)" else "Comparatif Hebdomadaire",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${dashboardSummary.buckets.size} périodes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        PeriodicBarChart(
                            buckets = dashboardSummary.buckets,
                            selectedBucket = selectedBucket,
                            onBucketSelected = { selectedBucket = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Détail du Bucket sélectionné
            selectedBucket?.let { bucket ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("dashboard_selected_bucket_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Détail : ${bucket.label}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                IconButton(onClick = { selectedBucket = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ventes : ${CurrencyUtils.formatDa(bucket.caTotal)} (${bucket.countBlClient} BL/Factures)", style = MaterialTheme.typography.bodySmall)
                                Text("Règlements : ${CurrencyUtils.formatDa(bucket.reglementsTotal)}", style = MaterialTheme.typography.bodySmall.copy(color = ReglementColor, fontWeight = FontWeight.Bold))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Achats : ${CurrencyUtils.formatDa(bucket.achatsTotal)}", style = MaterialTheme.typography.bodySmall.copy(color = FournisseurColor))
                                Text("Reste dû : ${CurrencyUtils.formatDa(bucket.resteARecouvrer)}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626), fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Donut de Recouvrement
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("dashboard_donut_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Taux de Recouvrement & Trésorerie",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        RecoveryDonutChart(
                            summary = dashboardSummary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Courbe de Tendance (TrendAreaChart)
            if (dashboardSummary.buckets.size >= 2) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("dashboard_trend_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Courbe d'Évolution Temporelle (CA & Encaissé)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            TrendAreaChart(
                                buckets = dashboardSummary.buckets,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Répartition des Modes de Règlement
            if (dashboardSummary.paymentModeDistribution.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Répartition par Mode de Paiement",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val totalPayments = dashboardSummary.paymentModeDistribution.values.sum().coerceAtLeast(1.0)
                            dashboardSummary.paymentModeDistribution.entries.sortedByDescending { it.value }.forEach { (mode, amount) ->
                                val pct = (amount / totalPayments).toFloat()
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = mode, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        Text(
                                            text = "${CurrencyUtils.formatDa(amount)} (${(pct * 100).toInt()}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ReglementColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = ReglementColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top 5 Clients
            if (topClients.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = ClientColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Top Clients par Chiffre d'Affaires", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                TextButton(onClick = { viewModel.navigateTo(AppScreen.Management(0)) }) {
                                    Text("Voir Tous", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            topClients.forEachIndexed { index, entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = ClientColor.copy(alpha = 0.12f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ClientColor)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(entry.key, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                    Text(
                                        text = CurrencyUtils.formatDa(entry.value),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ClientColor)
                                    )
                                }
                                if (index < topClients.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // Top 5 Articles Vendus
            if (topArticles.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Top Articles les Plus Demandés", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                TextButton(onClick = { viewModel.navigateTo(AppScreen.Management(2)) }) {
                                    Text("Catalogue", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            topArticles.forEachIndexed { index, entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(entry.key, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                            Text("${CurrencyUtils.formatQty(entry.value.first)} unités vendues", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        text = CurrencyUtils.formatDa(entry.value.second),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                if (index < topArticles.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
