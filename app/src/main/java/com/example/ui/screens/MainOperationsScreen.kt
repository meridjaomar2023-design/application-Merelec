package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallet
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.material.icons.filled.DocumentScanner
import com.example.R
import com.example.data.model.OperationType
import com.example.data.model.permissionCategory
import com.example.ui.components.InvoiceOcrScannerDialog
import com.example.ui.components.OperationCard
import com.example.ui.theme.ClientColor
import com.example.ui.theme.FournisseurColor
import com.example.ui.theme.ReglementColor
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.CurrencyUtils
import com.example.util.ExportUtils
import com.example.util.PdfGenerator
import java.io.File

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainOperationsScreen(
    viewModel: OperationViewModel,
    onLockEmployeeSession: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentBottomTab by viewModel.currentBottomTab.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val logoVersion by viewModel.logoVersion.collectAsStateWithLifecycle()
    val customLogoFile = remember(logoVersion) { File(context.filesDir, "custom_logo.png") }
    val customBitmap = remember(logoVersion, customLogoFile.lastModified()) {
        if (customLogoFile.exists()) {
            try {
                BitmapFactory.decodeFile(customLogoFile.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val openDrawer: () -> Unit = {
        coroutineScope.launch {
            drawerState.open()
        }
    }
    var showFullRoomCsvExportDialog by remember { mutableStateOf(false) }
    var showCompanyCoordinatesDialog by remember { mutableStateOf(false) }

    if (showCompanyCoordinatesDialog) {
        CompanyCoordinatesQuickDialog(
            currentSettings = companySettings,
            onSave = { updated ->
                viewModel.updateCompanySettings(updated)
                showCompanyCoordinatesDialog = false
            },
            onOpenFullConfig = {
                showCompanyCoordinatesDialog = false
                viewModel.selectBottomTab(BottomNavTab.CONFIGURATION)
            },
            onDismiss = { showCompanyCoordinatesDialog = false }
        )
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("left_navigation_drawer_sheet")
            ) {
                // En-tête du menu latéral gauche avec option directe de paramétrage des coordonnées de l'entreprise en haut
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showCompanyCoordinatesDialog = true
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customBitmap != null) {
                                Image(
                                    bitmap = customBitmap,
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_merelec),
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = companySettings.companyName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = companySettings.address.ifBlank { "Touchez pour paramétrer vos coordonnées" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showCompanyCoordinatesDialog = true
                            },
                            modifier = Modifier.size(34.dp).testTag("drawer_top_company_coordinates_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Paramétrer les coordonnées de mon entreprise",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showCompanyCoordinatesDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_header_company_settings_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Coordonnées de mon entreprise",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option tout en haut du volet latéral gauche : Paramétrage des coordonnées de mon entreprise
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Coordonnées de mon entreprise",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = {
                        Column {
                            Text(
                                "Coordonnées de mon entreprise",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Nom, adresse, tél, RC, NIF, NIS, NIN sur les bons",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        showCompanyCoordinatesDialog = true
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_company_coordinates_top")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "RÉPERTOIRE CLIENTS & FOURNISSEURS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )

                // Option Client déplacée dans le volet latéral gauche en haut
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Clients",
                            tint = ClientColor
                        )
                    },
                    label = {
                        Column {
                            Text("Clients", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Répertoire clients, bons & import/export",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = currentBottomTab == BottomNavTab.CLIENTS,
                    onClick = {
                        viewModel.selectBottomTab(BottomNavTab.CLIENTS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_clients")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option Fournisseur ajoutée dans le volet latéral gauche en haut
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Fournisseurs",
                            tint = FournisseurColor
                        )
                    },
                    label = {
                        Column {
                            Text("Fournisseurs", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Répertoire fournisseurs, réceptions & import/export",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = currentBottomTab == BottomNavTab.FOURNISSEURS,
                    onClick = {
                        viewModel.selectBottomTab(BottomNavTab.FOURNISSEURS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_fournisseurs")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option Articles & Importation CSV/XLS dans le volet latéral gauche
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Articles & Importation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = {
                        Column {
                            Text("Articles & Importation", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Catalogue articles & import CSV / XLS / XLSX",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = currentBottomTab == BottomNavTab.ARTICLES,
                    onClick = {
                        viewModel.selectBottomTab(BottomNavTab.ARTICLES)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_articles_import")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ADMINISTRATION & ANALYSES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )

                // Option 1 : Dashboard
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = {
                        Column {
                            Text("Dashboard", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Tableau de bord & statistiques",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = currentBottomTab == BottomNavTab.DASHBOARD,
                    onClick = {
                        viewModel.selectBottomTab(BottomNavTab.DASHBOARD)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_dashboard")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option 2 : Gestion des Stocks (Entrées, Sorties & Alertes Seuil Bas)
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Gestion des Stocks"
                        )
                    },
                    label = {
                        Column {
                            Text("Gestion des Stocks", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (lowStockProducts.isNotEmpty()) {
                                    "Entrées / Sorties • ${lowStockProducts.size} alerte(s) seuil bas"
                                } else {
                                    "Entrées, sorties & seuils d'alerte"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (lowStockProducts.isNotEmpty()) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    badge = {
                        if (lowStockProducts.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = "${lowStockProducts.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    selected = currentBottomTab == BottomNavTab.STOCK,
                    onClick = {
                        viewModel.selectBottomTab(BottomNavTab.STOCK)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_stock")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option 3 : Configuration
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuration"
                        )
                    },
                    label = {
                        Column {
                            Text("Configuration", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Entreprise, TVA & paramètres",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = currentBottomTab == BottomNavTab.CONFIGURATION,
                    onClick = {
                        viewModel.selectBottomTab(BottomNavTab.CONFIGURATION)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_configuration")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option 4 : Employés & Droits d'accès (Bon, Facture, Créance)
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Employés & Droits d'accès",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = {
                        Column {
                            Text("Employés & Droits d'accès", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Droits Modif/Suppr Bon, Facture, Créance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.EmployeeManagement)
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_employees")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option 5 : Studio de Création & Réorganisation PDF (Devis, Bon, Facture, Créance, Catalogue)
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Studio & Réorganisation PDF",
                            tint = Color(0xFFD97706)
                        )
                    },
                    label = {
                        Column {
                            Text("Studio & Réorganisation PDF", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Logo, Coordonnées, Devis, Bon, Facture, Créance, Catalogue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.PdfStudio(0))
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_pdf_studio")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option 6 : Module OCR Intelligent – Gestion Commerciale (Scan IA, PDF Multipages, Vérification 2 Volets & Transformation Client)
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "OCR Intelligent",
                            tint = Color(0xFF4338CA)
                        )
                    },
                    label = {
                        Column {
                            Text("OCR Intelligent (IA)", fontWeight = FontWeight.Bold)
                            Text(
                                "Scan multipages, PDF, vérification 2 volets & majoration",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.IntelligentOcr(OperationType.BL_CLIENT))
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_intelligent_ocr")
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "VERSION WEB PC WINDOWS & SAUVEGARDE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )

                val allOpsDrawer by viewModel.allOperations.collectAsStateWithLifecycle()
                val clientsDrawer by viewModel.clientsList.collectAsStateWithLifecycle()
                val suppliersDrawer by viewModel.suppliersList.collectAsStateWithLifecycle()
                val allPartnersDrawer = remember(clientsDrawer, suppliersDrawer) { clientsDrawer + suppliersDrawer }
                val allProductsDrawer by viewModel.catalogProductsList.collectAsStateWithLifecycle()
                val allEmployeesDrawer by viewModel.allEmployees.collectAsStateWithLifecycle()

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Version Web PC Windows",
                            tint = Color(0xFF0284C7)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                "Version Web pour PC Windows",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Lien direct Chrome/Edge & fichier Merelec_Web_PC_Windows.html",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        ExportUtils.shareStandaloneWebPcHtml(
                            context = context,
                            settings = companySettings,
                            operations = allOpsDrawer,
                            partners = allPartnersDrawer,
                            products = allProductsDrawer,
                            employees = allEmployeesDrawer
                        )
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_web_pc_windows")
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Exporter Base de Données Room (CSV)",
                            tint = Color(0xFF2E7D32)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                "Exporter Base Room (CSV)",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Sauvegarde externe intégrale (6 tables)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        showFullRoomCsvExportDialog = true
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_export_full_csv")
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AddToDrive,
                            contentDescription = "Sauvegarde Google Drive",
                            tint = Color(0xFF0F9D58)
                        )
                    },
                    label = { Text("Sauvegarde Google Drive") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.BackupDrive)
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_drive")
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SYNCHRONISATION CLOUD MULTI-POSTES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF059669),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )

                val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsStateWithLifecycle()
                val isSyncingFirebase by viewModel.isSyncingFirebase.collectAsStateWithLifecycle()
                val lastFirebaseSyncTime by viewModel.lastFirebaseSyncTime.collectAsStateWithLifecycle()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF059669).copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🟢 ${currentEmployee.name} (${currentEmployee.role.label})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "Email : ${currentEmployee.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isFirebaseConnected) {
                                "☁️ Firebase Temps Réel actif" + (lastFirebaseSyncTime?.let { " • Sync: $it" } ?: "")
                            } else {
                                "⚠️ Mode hors-ligne (Connectez-vous pour synchroniser)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isFirebaseConnected) Color(0xFF059669) else Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.syncWithFirebaseNow() },
                            enabled = !isSyncingFirebase,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drawer_sync_firebase_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncingFirebase) "Synchronisation en cours..." else "Synchroniser Temps Réel Firebase",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SwitchAccount,
                            contentDescription = "Changer d'employé"
                        )
                    },
                    label = {
                        Column {
                            Text("Changer d'employé", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Verrouiller et revenir à l'écran Email/Mot de passe",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onLockEmployeeSession()
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_switch_employee")
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Changer de compte Google / Déconnexion"
                        )
                    },
                    label = {
                        Column {
                            Text("Déconnexion Google Cloud", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Changer de compte multi-postes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        signOutFromFirebase(
                            context = context,
                            onSignOutComplete = {},
                            scope = coroutineScope
                        )
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_sign_out")
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentBottomTab == BottomNavTab.VENTES,
                        onClick = { viewModel.selectBottomTab(BottomNavTab.VENTES) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Vente"
                            )
                        },
                        label = { Text("Vente") },
                        modifier = Modifier.testTag("nav_tab_vente")
                    )

                    NavigationBarItem(
                        selected = currentBottomTab == BottomNavTab.ACHATS,
                        onClick = { viewModel.selectBottomTab(BottomNavTab.ACHATS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Achat"
                            )
                        },
                        label = { Text("Achat") },
                        modifier = Modifier.testTag("nav_tab_achat")
                    )

                    NavigationBarItem(
                        selected = currentBottomTab == BottomNavTab.CREANCES,
                        onClick = { viewModel.selectBottomTab(BottomNavTab.CREANCES) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Créances"
                            )
                        },
                        label = { Text("Créances") },
                        modifier = Modifier.testTag("nav_tab_creances")
                    )

                    NavigationBarItem(
                        selected = currentBottomTab == BottomNavTab.ARTICLES,
                        onClick = { viewModel.selectBottomTab(BottomNavTab.ARTICLES) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "Articles"
                            )
                        },
                        label = { Text("Articles") },
                        modifier = Modifier.testTag("nav_tab_articles")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentBottomTab) {
                    BottomNavTab.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onOpenDrawer = openDrawer
                        )
                    }
                    BottomNavTab.VENTES -> {
                        SalesTabView(
                            viewModel = viewModel,
                            onOpenDrawer = openDrawer
                        )
                    }
                    BottomNavTab.ACHATS -> {
                        PurchasesTabView(
                            viewModel = viewModel,
                            onOpenDrawer = openDrawer
                        )
                    }
                    BottomNavTab.CLIENTS -> {
                        androidx.compose.runtime.key("tab_clients_directory") {
                            DirectoryManagementScreen(
                                viewModel = viewModel,
                                initialTab = 0,
                                showBackButton = false,
                                onOpenDrawer = openDrawer
                            )
                        }
                    }
                    BottomNavTab.FOURNISSEURS -> {
                        androidx.compose.runtime.key("tab_suppliers_directory") {
                            DirectoryManagementScreen(
                                viewModel = viewModel,
                                initialTab = 1,
                                showBackButton = false,
                                onOpenDrawer = openDrawer
                            )
                        }
                    }
                    BottomNavTab.CREANCES -> {
                        CreancesTabView(
                            viewModel = viewModel,
                            onOpenDrawer = openDrawer
                        )
                    }
                    BottomNavTab.ARTICLES -> {
                        androidx.compose.runtime.key("tab_articles_directory") {
                            DirectoryManagementScreen(
                                viewModel = viewModel,
                                initialTab = 2,
                                showBackButton = false,
                                onOpenDrawer = openDrawer
                            )
                        }
                    }
                    BottomNavTab.STOCK -> {
                        StockManagementScreen(
                            viewModel = viewModel,
                            onOpenDrawer = openDrawer
                        )
                    }
                    BottomNavTab.CONFIGURATION -> {
                        ConfigurationScreen(
                            viewModel = viewModel,
                            onOpenDrawer = openDrawer,
                            onOpenFullCsvExport = { showFullRoomCsvExportDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showFullRoomCsvExportDialog) {
        ExportFullRoomDatabaseCsvDialog(
            viewModel = viewModel,
            onDismiss = { showFullRoomCsvExportDialog = false }
        )
    }
}

/**
 * Vue principale de l'onglet VENTE :
 * Chiffre d'Affaires, Factures, Bons de Livraison, Bons de Commande, Règlements clients & Répertoire Clients
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesTabView(
    viewModel: OperationViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val salesOperations by viewModel.salesOperations.collectAsStateWithLifecycle()
    val allOperations by viewModel.allOperations.collectAsStateWithLifecycle()
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    val catalogProducts by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val kpi by viewModel.salesKpi.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilterType.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()

    val logoVersion by viewModel.logoVersion.collectAsStateWithLifecycle()
    val customLogoFile = remember(logoVersion) { File(context.filesDir, "custom_logo.png") }
    val customBitmap = remember(logoVersion, customLogoFile.lastModified()) {
        if (customLogoFile.exists()) {
            try {
                BitmapFactory.decodeFile(customLogoFile.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var showNewSaleDialog by remember { mutableStateOf(false) }
    var showOcrScannerDialog by remember { mutableStateOf(false) }
    var blToConvertToFacture by remember { mutableStateOf<com.example.data.model.OperationWithLignes?>(null) }
    var devisToConvertToBl by remember { mutableStateOf<com.example.data.model.OperationWithLignes?>(null) }
    var opToDelete by remember { mutableStateOf<com.example.data.model.OperationWithLignes?>(null) }
    var showBatchConvertBlsDialog by remember { mutableStateOf(false) }
    var showBatchConvertDevisToBlDialog by remember { mutableStateOf(false) }
    var pdfDialogPair by remember { mutableStateOf<Pair<com.example.data.model.OperationWithLignes, File>?>(null) }

    if (showOcrScannerDialog) {
        val knownPartnersList = remember(clients, suppliers) {
            (clients + suppliers).distinctBy { it.name.lowercase() }
        }
        InvoiceOcrScannerDialog(
            initialTargetType = OperationType.BL_CLIENT,
            knownPartners = knownPartnersList,
            catalogProducts = catalogProducts,
            ownCompanyName = companySettings.companyName,
            defaultMarginCoefficient = companySettings.defaultMarginCoefficient,
            allowAppendToCurrentDraft = false,
            onOpenIntelligentOcr = {
                showOcrScannerDialog = false
                viewModel.navigateTo(AppScreen.IntelligentOcr(OperationType.BL_CLIENT))
            },
            onDismiss = { showOcrScannerDialog = false },
            onImportResult = { targetType, partnerName, invNum, lines, tva, rate, margin, append, saveNow ->
                showOcrScannerDialog = false
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

    if (opToDelete != null) {
        val target = opToDelete!!
        AlertDialog(
            onDismissRequest = { opToDelete = null },
            title = { Text("Supprimer ${target.operation.typeOperation.label} ?") },
            text = { Text("Voulez-vous vraiment supprimer définitivement « ${target.operation.id} » (${target.operation.clientFournisseur}) ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOperation(target.operation.id)
                        opToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { opToDelete = null }) {
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

    if (showBatchConvertBlsDialog) {
        val availableBls = allOperations.filter { it.operation.typeOperation == OperationType.BL_CLIENT }
        SelectAndConvertBlsDialog(
            availableBls = availableBls,
            viewModel = viewModel,
            onDismiss = { showBatchConvertBlsDialog = false }
        )
    }

    if (showBatchConvertDevisToBlDialog) {
        val availableDevis = allOperations.filter { it.operation.typeOperation == OperationType.DEVIS_CLIENT }
        SelectAndConvertDevisToBlDialog(
            availableDevis = availableDevis,
            viewModel = viewModel,
            onDismiss = { showBatchConvertDevisToBlDialog = false }
        )
    }

    if (showExportDialog) {
        ExportOperationsDialog(
            allOperations = salesOperations.ifEmpty { allOperations },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showNewSaleDialog) {
        NewSaleChoiceDialog(
            onDismiss = { showNewSaleDialog = false },
            onSelect = { type ->
                showNewSaleDialog = false
                viewModel.startNewOperation(type)
            },
            onConvertBlToFacture = {
                showNewSaleDialog = false
                showBatchConvertBlsDialog = true
            },
            onConvertDevisToBl = {
                showNewSaleDialog = false
                showBatchConvertDevisToBlDialog = true
            },
            onScanInvoiceOcr = {
                showNewSaleDialog = false
                showOcrScannerDialog = true
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
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
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFF009FE3).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customBitmap != null) {
                                Image(
                                    bitmap = customBitmap,
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize().padding(2.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_merelec),
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize().padding(2.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showOcrScannerDialog = true },
                        modifier = Modifier.testTag("sales_ocr_scan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scanner Facture en Image (OCR)",
                            tint = Color(0xFF4338CA)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.syncWithFirebaseNow() },
                        modifier = Modifier.testTag("sales_sync_firebase_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Synchroniser en temps réel avec Firebase",
                            tint = Color(0xFF0284C7)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.BackupDrive) },
                        modifier = Modifier.testTag("sales_drive_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddToDrive,
                            contentDescription = "Drive",
                            tint = Color(0xFF0F9D58)
                        )
                    }

                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("sales_pdf_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Exporter Bons de Livraison & Factures en PDF",
                            tint = Color(0xFF16A34A)
                        )
                    }

                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("sales_excel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Excel",
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
                onClick = { showNewSaleDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouvelle Vente") },
                containerColor = ClientColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("sales_new_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Synthèse Financière Ventes
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("sales_kpi_card"),
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
                                text = "Synthèse Commerciale Clients",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${kpi.countVentes} pièce(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // CA Ventes TTC
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = ClientColor.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Total Ventes (TTC)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ClientColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyUtils.formatDa(kpi.totalVentesTtc),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = ClientColor
                                    )
                                }
                            }

                            // Encaissé
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = ReglementColor.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Total Encaissé",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ReglementColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyUtils.formatDa(kpi.totalEncaisse),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = ReglementColor
                                    )
                                }
                            }

                            // Créances restantes
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (kpi.creancesRestantes > 0) Color(0xFFEF4444).copy(alpha = 0.08f) else Color(0xFF10B981).copy(alpha = 0.08f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Créances Dues",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (kpi.creancesRestantes > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyUtils.formatDa(kpi.creancesRestantes),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (kpi.creancesRestantes > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Répertoire Clients Card Hub
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.navigateTo(AppScreen.Management(0)) }
                        .testTag("sales_clients_hub_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ClientColor.copy(alpha = 0.06f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ClientColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ClientColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Répertoire Clients",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${clients.size} client(s) enregistrés",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.navigateTo(AppScreen.Management(0)) }) {
                            Text("Voir Clients", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Recherche
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier.fillMaxWidth().testTag("sales_search_input"),
                        placeholder = { Text("Rechercher client, facture, BL, article...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
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

            // Filtres rapides Ventes
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { viewModel.selectedFilterType.value = null },
                        label = { Text("Toutes les Ventes (${salesOperations.size})") },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("sales_filter_all")
                    )

                    listOf(
                        OperationType.DEVIS_CLIENT to "Devis",
                        OperationType.FACTURE to "Factures",
                        OperationType.BL_CLIENT to "Bons de Livraison",
                        OperationType.AVOIR_CLIENT to "Avoirs Client",
                        OperationType.BON_COMMANDE to "Bons de Commande",
                        OperationType.REGLEMENT to "Règlements"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { viewModel.selectedFilterType.value = if (selectedFilter == type) null else type },
                            label = { Text(label) },
                            modifier = Modifier.testTag("sales_filter_${type.name.lowercase()}")
                        )
                    }
                }
            }

            // Liste des opérations Ventes
            if (salesOperations.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (query.isNotEmpty() || selectedFilter != null) "Aucun résultat trouvé" else "Aucune opération de vente enregistrée",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Touchez « Nouvelle Vente » pour créer une facture ou un BL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(
                    items = salesOperations,
                    key = { it.operation.id }
                ) { item ->
                    val canEditItem = currentEmployee.canModifyOperation(item.operation.typeOperation)
                    val canDeleteItem = currentEmployee.canDeleteOperation(item.operation.typeOperation)
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        OperationCard(
                            item = item,
                            onClick = { viewModel.navigateTo(AppScreen.Detail(item.operation.id)) },
                            onConvertToFacture = if (item.operation.typeOperation == OperationType.BL_CLIENT) {
                                {
                                    if (currentEmployee.canModifyFacture || currentEmployee.canModifyBon) {
                                        blToConvertToFacture = item
                                    } else {
                                        viewModel.showMessage(
                                            currentEmployee.permissionDeniedMessage(
                                                action = "convertir en facture",
                                                category = item.operation.typeOperation.permissionCategory()
                                            )
                                        )
                                    }
                                }
                            } else null,
                            onConvertToBl = if (item.operation.typeOperation == OperationType.DEVIS_CLIENT) {
                                {
                                    if (currentEmployee.canModifyBon) {
                                        devisToConvertToBl = item
                                    } else {
                                        viewModel.showMessage(
                                            currentEmployee.permissionDeniedMessage(
                                                action = "convertir ce devis en BL",
                                                category = item.operation.typeOperation.permissionCategory()
                                            )
                                        )
                                    }
                                }
                            } else null,
                            onGeneratePdf = {
                                val matchedClient = clients.find {
                                    it.name.trim().equals(item.operation.clientFournisseur.trim(), ignoreCase = true)
                                }
                                val file = PdfGenerator.generatePdf(context, item, companySettings, matchedClient)
                                pdfDialogPair = item to file
                            },
                            onEdit = {
                                viewModel.editOperation(item)
                            },
                            onDelete = {
                                if (canDeleteItem) {
                                    opToDelete = item
                                } else {
                                    viewModel.showMessage(
                                        currentEmployee.permissionDeniedMessage(
                                            action = "supprimer",
                                            category = item.operation.typeOperation.permissionCategory()
                                        )
                                    )
                                }
                            },
                            canEdit = canEditItem,
                            canDelete = canDeleteItem
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vue principale de l'onglet ACHAT :
 * Dépenses, Bons Fournisseurs, Factures d'Achat, Règlements fournisseurs & Répertoire Fournisseurs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchasesTabView(
    viewModel: OperationViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val purchaseOperations by viewModel.purchaseOperations.collectAsStateWithLifecycle()
    val allOperations by viewModel.allOperations.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val catalogProducts by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val kpi by viewModel.purchasesKpi.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilterType.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()

    val logoVersion by viewModel.logoVersion.collectAsStateWithLifecycle()
    val customLogoFile = remember(logoVersion) { File(context.filesDir, "custom_logo.png") }
    val customBitmap = remember(logoVersion, customLogoFile.lastModified()) {
        if (customLogoFile.exists()) {
            try {
                BitmapFactory.decodeFile(customLogoFile.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var showNewPurchaseDialog by remember { mutableStateOf(false) }
    var showOcrScannerDialog by remember { mutableStateOf(false) }
    var purchaseOpToDelete by remember { mutableStateOf<com.example.data.model.OperationWithLignes?>(null) }
    var devisFournisseurToConvertToBl by remember { mutableStateOf<com.example.data.model.OperationWithLignes?>(null) }
    var showBatchConvertSupplierDevisDialog by remember { mutableStateOf(false) }
    var pdfDialogPair by remember { mutableStateOf<Pair<com.example.data.model.OperationWithLignes, File>?>(null) }

    if (showOcrScannerDialog) {
        val knownPartnersList = remember(clients, suppliers) {
            (suppliers + clients).distinctBy { it.name.lowercase() }
        }
        InvoiceOcrScannerDialog(
            initialTargetType = OperationType.FACTURE_FOURNISSEUR,
            knownPartners = knownPartnersList,
            catalogProducts = catalogProducts,
            ownCompanyName = companySettings.companyName,
            defaultMarginCoefficient = companySettings.defaultMarginCoefficient,
            allowAppendToCurrentDraft = false,
            onOpenIntelligentOcr = {
                showOcrScannerDialog = false
                viewModel.navigateTo(AppScreen.IntelligentOcr(OperationType.FACTURE_FOURNISSEUR))
            },
            onDismiss = { showOcrScannerDialog = false },
            onImportResult = { targetType, partnerName, invNum, lines, tva, rate, margin, append, saveNow ->
                showOcrScannerDialog = false
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

    if (purchaseOpToDelete != null) {
        val target = purchaseOpToDelete!!
        AlertDialog(
            onDismissRequest = { purchaseOpToDelete = null },
            title = { Text("Supprimer ${target.operation.typeOperation.label} ?") },
            text = { Text("Voulez-vous vraiment supprimer définitivement « ${target.operation.id} » (${target.operation.clientFournisseur}) ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOperation(target.operation.id)
                        purchaseOpToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { purchaseOpToDelete = null }) {
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

    if (devisFournisseurToConvertToBl != null) {
        ConvertDevisToBlDialog(
            source = devisFournisseurToConvertToBl!!,
            viewModel = viewModel,
            onDismiss = { devisFournisseurToConvertToBl = null }
        )
    }

    if (showBatchConvertSupplierDevisDialog) {
        val availableSupplierDevis = allOperations.filter { it.operation.typeOperation == OperationType.DEVIS_FOURNISSEUR }
        SelectAndConvertDevisToBlDialog(
            availableDevis = availableSupplierDevis,
            viewModel = viewModel,
            onDismiss = { showBatchConvertSupplierDevisDialog = false }
        )
    }

    if (showExportDialog) {
        ExportOperationsDialog(
            allOperations = purchaseOperations.ifEmpty { allOperations },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showNewPurchaseDialog) {
        NewPurchaseChoiceDialog(
            onDismiss = { showNewPurchaseDialog = false },
            onSelect = { type ->
                showNewPurchaseDialog = false
                viewModel.startNewOperation(type)
            },
            onConvertDevisToBl = {
                showNewPurchaseDialog = false
                showBatchConvertSupplierDevisDialog = true
            },
            onScanInvoiceOcr = {
                showNewPurchaseDialog = false
                showOcrScannerDialog = true
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
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
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, FournisseurColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customBitmap != null) {
                                Image(
                                    bitmap = customBitmap,
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize().padding(2.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_merelec),
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize().padding(2.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showOcrScannerDialog = true },
                        modifier = Modifier.testTag("purchases_ocr_scan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scanner Facture en Image (OCR)",
                            tint = Color(0xFF4338CA)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.syncWithFirebaseNow() },
                        modifier = Modifier.testTag("purchases_sync_firebase_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Synchroniser en temps réel avec Firebase",
                            tint = Color(0xFF0284C7)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.BackupDrive) },
                        modifier = Modifier.testTag("purchases_drive_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddToDrive,
                            contentDescription = "Drive",
                            tint = Color(0xFF0F9D58)
                        )
                    }

                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("purchases_pdf_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Exporter Bons en PDF",
                            tint = Color(0xFF16A34A)
                        )
                    }

                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("purchases_excel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Excel",
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
                onClick = { showNewPurchaseDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouvel Achat") },
                containerColor = FournisseurColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("purchases_new_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Synthèse Financière Achats
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("purchases_kpi_card"),
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
                                text = "Synthèse Fournisseurs & Achats",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${kpi.countAchats} bon(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = FournisseurColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Achats TTC
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = FournisseurColor.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Total Achats (TTC)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FournisseurColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyUtils.formatDa(kpi.totalAchatsTtc),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = FournisseurColor
                                    )
                                }
                            }

                            // Payé Fournisseurs
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = ReglementColor.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Total Réglé",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ReglementColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyUtils.formatDa(kpi.totalPaye),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = ReglementColor
                                    )
                                }
                            }

                            // Dettes restantes
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (kpi.dettesRestantes > 0) Color(0xFFEF4444).copy(alpha = 0.08f) else Color(0xFF10B981).copy(alpha = 0.08f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Dettes Dues",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (kpi.dettesRestantes > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyUtils.formatDa(kpi.dettesRestantes),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (kpi.dettesRestantes > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Répertoire Fournisseurs Card Hub
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.navigateTo(AppScreen.Management(1)) }
                        .testTag("purchases_suppliers_hub_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FournisseurColor.copy(alpha = 0.06f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(FournisseurColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = FournisseurColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Répertoire Fournisseurs",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${suppliers.size} fournisseur(s) enregistrés",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.navigateTo(AppScreen.Management(1)) }) {
                            Text("Voir Fournisseurs", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Recherche
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier.fillMaxWidth().testTag("purchases_search_input"),
                        placeholder = { Text("Rechercher fournisseur, bon, article...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
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

            // Filtres rapides Achats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { viewModel.selectedFilterType.value = null },
                        label = { Text("Tous les Achats (${purchaseOperations.size})") },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("purchases_filter_all")
                    )

                    listOf(
                        OperationType.FACTURE_FOURNISSEUR to "Factures Fournisseur",
                        OperationType.DEVIS_FOURNISSEUR to "Devis Fournisseur",
                        OperationType.BL_FOURNISSEUR to "Bons Fournisseurs",
                        OperationType.AVOIR_FOURNISSEUR to "Avoirs Fournisseur",
                        OperationType.REGLEMENT to "Règlements"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { viewModel.selectedFilterType.value = if (selectedFilter == type) null else type },
                            label = { Text(label) },
                            modifier = Modifier.testTag("purchases_filter_${type.name.lowercase()}")
                        )
                    }
                }
            }

            // Liste des opérations Achats
            if (purchaseOperations.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (query.isNotEmpty() || selectedFilter != null) "Aucun résultat trouvé" else "Aucun achat ou bon fournisseur enregistré",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Touchez « Nouvel Achat » pour enregistrer une réception fournisseur",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(
                    items = purchaseOperations,
                    key = { it.operation.id }
                ) { item ->
                    val canEditItem = currentEmployee.canModifyOperation(item.operation.typeOperation)
                    val canDeleteItem = currentEmployee.canDeleteOperation(item.operation.typeOperation)
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        OperationCard(
                            item = item,
                            onClick = { viewModel.navigateTo(AppScreen.Detail(item.operation.id)) },
                            onConvertToBl = if (item.operation.typeOperation == OperationType.DEVIS_FOURNISSEUR) {
                                {
                                    if (currentEmployee.canModifyBon) {
                                        devisFournisseurToConvertToBl = item
                                    } else {
                                        viewModel.showMessage(
                                            currentEmployee.permissionDeniedMessage(
                                                action = "convertir ce devis en bon fournisseur",
                                                category = item.operation.typeOperation.permissionCategory()
                                            )
                                        )
                                    }
                                }
                            } else null,
                            onGeneratePdf = {
                                val matchedSupplier = suppliers.find {
                                    it.name.trim().equals(item.operation.clientFournisseur.trim(), ignoreCase = true)
                                }
                                val file = PdfGenerator.generatePdf(context, item, companySettings, matchedSupplier)
                                pdfDialogPair = item to file
                            },
                            onEdit = {
                                viewModel.editOperation(item)
                            },
                            onDelete = {
                                if (canDeleteItem) {
                                    purchaseOpToDelete = item
                                } else {
                                    viewModel.showMessage(
                                        currentEmployee.permissionDeniedMessage(
                                            action = "supprimer",
                                            category = item.operation.typeOperation.permissionCategory()
                                        )
                                    )
                                }
                            },
                            canEdit = canEditItem,
                            canDelete = canDeleteItem
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialogue de choix pour une Nouvelle Vente
 */
@Composable
private fun NewSaleChoiceDialog(
    onDismiss: () -> Unit,
    onSelect: (OperationType) -> Unit,
    onConvertBlToFacture: () -> Unit = {},
    onConvertDevisToBl: () -> Unit = {},
    onScanInvoiceOcr: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Vente") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choisissez le type de document à créer :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onScanInvoiceOcr() }.testTag("choice_scan_invoice_ocr_sale"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Scanner Facture en Image (OCR)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF312E81))
                            Text("Extraire automatiquement les articles depuis une photo ou image de facture", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4338CA))
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onConvertDevisToBl() }.testTag("choice_convert_devis_to_bl"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Convertir Devis en Bon de Livraison (BL)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0369A1))
                            Text("Transformer un ou plusieurs Devis existants en BL", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0284C7))
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onConvertBlToFacture() }.testTag("choice_convert_bl_to_facture"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Convertir Bon de Livraison en Facture", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF065F46))
                            Text("Transformer un ou plusieurs BL existants en Facture", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857))
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.DEVIS_CLIENT) }.testTag("choice_devis_client"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Devis Client", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Proposition commerciale ou devis estimatif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.FACTURE) }.testTag("choice_facture"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Facture Client", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Avec TVA 19%, ventilation HT & TTC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.BL_CLIENT) }.testTag("choice_bl_client"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = ClientColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Bon de Livraison (BL)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Livraison standard client avec articles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.AVOIR_CLIENT) }.testTag("choice_avoir_client"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFC2410C), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Avoir Client", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Facture d'avoir, retour marchandise ou remise", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.BON_COMMANDE) }.testTag("choice_bc"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Bon de Commande", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Commande client avant livraison", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.REGLEMENT) }.testTag("choice_reglement_client"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wallet, contentDescription = null, tint = ReglementColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Règlement / Paiement Reçu", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Encaisser espèces, chèque ou virement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/**
 * Dialogue de choix pour un Nouvel Achat
 */
@Composable
private fun NewPurchaseChoiceDialog(
    onDismiss: () -> Unit,
    onSelect: (OperationType) -> Unit,
    onConvertDevisToBl: () -> Unit = {},
    onScanInvoiceOcr: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel Achat") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choisissez le type d'opération d'achat :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onScanInvoiceOcr() }.testTag("choice_scan_invoice_ocr_purchase"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Scanner Facture en Image (OCR)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF312E81))
                            Text("Reconnaissance OCR d'une facture fournisseur en photo ou image", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4338CA))
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onConvertDevisToBl() }.testTag("choice_convert_supplier_devis_to_bl"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Convertir Devis Fournisseur en Bon (BL)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0369A1))
                            Text("Transformer un ou plusieurs Devis fournisseurs en BL", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0284C7))
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.DEVIS_FOURNISSEUR) }.testTag("choice_devis_fournisseur"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF00838F), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Devis Fournisseur", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Demande de prix ou devis reçu d'un fournisseur", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.BL_FOURNISSEUR) }.testTag("choice_bl_fournisseur"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = FournisseurColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Bon Fournisseur / Réception", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Entrée de stock et enregistrement achat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.FACTURE_FOURNISSEUR) }.testTag("choice_facture_fournisseur"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Facture Fournisseur", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Facture d'achat fournisseur convertible en Facture ou BL Client", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.AVOIR_FOURNISSEUR) }.testTag("choice_avoir_fournisseur"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Avoir Fournisseur", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Bon d'avoir reçu suite à retour ou rabais", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(OperationType.REGLEMENT) }.testTag("choice_reglement_fournisseur"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wallet, contentDescription = null, tint = ReglementColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Règlement Versé au Fournisseur", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Payer dette fournisseur (espèces, virement)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/**
 * Dialogue d'exportation PDF (Bons de Livraison & Factures) et Excel / CSV
 */
@Composable
private fun ExportOperationsDialog(
    allOperations: List<com.example.data.model.OperationWithLignes>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val blAndFactures = remember(allOperations) {
        allOperations.filter {
            it.operation.typeOperation == OperationType.BL_CLIENT ||
                it.operation.typeOperation == OperationType.FACTURE ||
                it.operation.typeOperation == OperationType.FACTURE_FOURNISSEUR ||
                it.operation.typeOperation == OperationType.DEVIS_CLIENT ||
                it.operation.typeOperation == OperationType.BON_COMMANDE ||
                it.operation.typeOperation == OperationType.BL_FOURNISSEUR
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD97706).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Impression A4 des Bons, PDF & Excel",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val bonsList = remember(allOperations) {
                    allOperations.filter {
                        it.operation.typeOperation == OperationType.BL_CLIENT ||
                            it.operation.typeOperation == OperationType.BON_COMMANDE ||
                            it.operation.typeOperation == OperationType.BL_FOURNISSEUR ||
                            it.operation.typeOperation == OperationType.REGLEMENT
                    }.ifEmpty { blAndFactures.ifEmpty { allOperations } }
                }

                // Section 0 : Impression A4 des Bons Complets (1 Bon = 1 Page A4 avec Logo HD & Cachet Humide)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Impression A4 des Bons (${bonsList.size} bon(s))",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF075985)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Chaque Bon (BL, BC, BR, Règlement) est généré sur sa propre page A4 complète avec Logo HD, articles, montant en lettres et Cachet Humide.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0369A1)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onDismiss()
                                val pdfFile = PdfGenerator.generateFullA4BonsBatchPdf(
                                    context = context,
                                    operations = bonsList
                                )
                                PdfGenerator.printPdfA4(
                                    context = context,
                                    file = pdfFile,
                                    jobName = "Bons_A4_Merelec_${bonsList.size}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("export_print_bons_a4_btn")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Imprimer les Bons en Format A4")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                val pdfFile = PdfGenerator.generateFullA4BonsBatchPdf(
                                    context = context,
                                    operations = bonsList
                                )
                                PdfGenerator.openPdf(context, pdfFile)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("export_open_bons_a4_btn")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Visualiser / Partager les Bons A4 complets (.pdf)")
                        }
                    }
                }

                // Section 1 : Exportation PDF du Journal récapitulatif (Bons de Livraison & Factures)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Journal Récapitulatif PDF (Bons & Factures)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Générez et partagez un tableau récapitulatif A4 des ${blAndFactures.size.coerceAtLeast(allOperations.size)} document(s) affichés.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onDismiss()
                                val targetList = blAndFactures.ifEmpty { allOperations }
                                val pdfFile = PdfGenerator.generateOperationsBatchPdf(
                                    context = context,
                                    operations = targetList,
                                    reportTitle = "JOURNAL DES BONS DE LIVRAISON & FACTURES"
                                )
                                PdfGenerator.sharePdf(
                                    context = context,
                                    file = pdfFile,
                                    subject = "Journal Bons de Livraison & Factures - Merélec"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("export_pdf_batch_share_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partager Journal PDF (BL & Factures)")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                val targetList = blAndFactures.ifEmpty { allOperations }
                                val pdfFile = PdfGenerator.generateOperationsBatchPdf(
                                    context = context,
                                    operations = targetList,
                                    reportTitle = "JOURNAL DES BONS DE LIVRAISON & FACTURES"
                                )
                                PdfGenerator.printPdfA4(context, pdfFile, "Journal_Bons_Factures_A4")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("export_pdf_batch_open_btn")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Imprimer le Journal en A4")
                        }
                    }
                }

                // Section 2 : Exportation Excel / CSV
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Format Tableur Excel / CSV (${allOperations.size} opération(s))",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                ExportUtils.exportToExcelFile(context, allOperations)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("export_excel_share_btn")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ouvrir / Partager (.xls)")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                val csv = ExportUtils.generateOperationsCsv(allOperations)
                                ExportUtils.shareText(context, "Gestion_Merelec_Export.csv", csv)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("export_csv_btn")
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partager en CSV")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
fun ExportFullRoomDatabaseCsvDialog(
    viewModel: OperationViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allOperations by viewModel.allOperations.collectAsStateWithLifecycle()
    val clientsList by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliersList by viewModel.suppliersList.collectAsStateWithLifecycle()
    val catalogProducts by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val articlesWithComp by viewModel.articlesEclatesWithComposition.collectAsStateWithLifecycle()
    val stockMovements by viewModel.allStockMovements.collectAsStateWithLifecycle()

    val allPartners = remember(clientsList, suppliersList) {
        (clientsList + suppliersList).distinctBy { it.id }
    }
    val allCompositions = remember(articlesWithComp) {
        articlesWithComp.flatMap { it.composantes }
    }
    val totalLinesCount = remember(allOperations) {
        allOperations.sumOf { it.lignes.size }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = ExportUtils.writeFullRoomDatabaseCsvToUri(
                context = context,
                targetUri = uri,
                operations = allOperations,
                partners = allPartners,
                products = catalogProducts,
                compositions = allCompositions,
                stockMovements = stockMovements
            )
            if (saved) {
                viewModel.showMessage("Base de données Room exportée avec succès en CSV !")
                onDismiss()
            } else {
                viewModel.showMessage("Erreur lors de l'enregistrement du fichier CSV.")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.TableChart,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Sauvegarde Externe Room (CSV)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Exportez l'intégralité des 6 tables de la base de données Room au format CSV (UTF-8 compatible Excel & Google Sheets) :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Contenu de la base Room à exporter :",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• operations_terrain : ${allOperations.size} documents (BL, Factures, Règlements)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• lignes_operation : $totalLinesCount lignes d'articles facturés/livrés",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• partners_directory : ${allPartners.size} partenaires (${clientsList.size} clients, ${suppliersList.size} fournisseurs)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• products_catalog : ${catalogProducts.size} articles & stocks",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• articles_composition : ${allCompositions.size} sous-articles (nomenclature BOM)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• stock_movements : ${stockMovements.size} mouvements d'entrées/sorties",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 1. Enregistrer directement sur le stockage externe / appareil (SAF CreateDocument)
                Button(
                    onClick = {
                        saveCsvLauncher.launch(ExportUtils.defaultFullRoomBackupFileName())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_save_full_room_csv_device"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enregistrer le CSV sur l'appareil",
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2. Partager le fichier CSV intégral unifié
                FilledTonalButton(
                    onClick = {
                        ExportUtils.exportAndShareFullRoomDatabaseCsv(
                            context = context,
                            operations = allOperations,
                            partners = allPartners,
                            products = catalogProducts,
                            compositions = allCompositions,
                            stockMovements = stockMovements
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_share_full_room_csv_unified"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Partager le CSV Intégral (Drive, Email, WhatsApp)")
                }

                // 3. Partager les 6 tables en 6 fichiers CSV distincts
                OutlinedButton(
                    onClick = {
                        ExportUtils.exportAndShareRoomTablesSeparateCsvs(
                            context = context,
                            operations = allOperations,
                            partners = allPartners,
                            products = catalogProducts,
                            compositions = allCompositions,
                            stockMovements = stockMovements
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_share_full_room_csv_separate_tables"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporter les 6 Tables en 6 CSV séparés")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_full_room_csv_dialog")
            ) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun CompanyCoordinatesQuickDialog(
    currentSettings: com.example.data.model.CompanySettings,
    onSave: (com.example.data.model.CompanySettings) -> Unit,
    onOpenFullConfig: () -> Unit,
    onDismiss: () -> Unit
) {
    var companyName by remember(currentSettings) { mutableStateOf(currentSettings.companyName) }
    var activitySlogan by remember(currentSettings) { mutableStateOf(currentSettings.activitySlogan) }
    var address by remember(currentSettings) { mutableStateOf(currentSettings.address) }
    var phone by remember(currentSettings) { mutableStateOf(currentSettings.phone) }
    var phoneSecondary by remember(currentSettings) { mutableStateOf(currentSettings.phoneSecondary) }
    var email by remember(currentSettings) { mutableStateOf(currentSettings.email) }
    var rc by remember(currentSettings) { mutableStateOf(currentSettings.rc) }
    var nif by remember(currentSettings) { mutableStateOf(currentSettings.nif) }
    var nis by remember(currentSettings) { mutableStateOf(currentSettings.nis) }
    var nin by remember(currentSettings) { mutableStateOf(currentSettings.nin) }
    var articleImposition by remember(currentSettings) { mutableStateOf(currentSettings.articleImposition) }
    var bankRib by remember(currentSettings) { mutableStateOf(currentSettings.bankRib) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("company_coordinates_quick_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Coordonnées de mon Entreprise",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Affichées en haut et dans le cachet des bons PDF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Nom de l'entreprise *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_company_name_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = activitySlogan,
                        onValueChange = { activitySlogan = it },
                        label = { Text("Activité / Slogan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_company_slogan_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adresse complète") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_company_address_input")
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Téléphone 1") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("quick_company_phone_input")
                        )
                        OutlinedTextField(
                            value = phoneSecondary,
                            onValueChange = { phoneSecondary = it },
                            label = { Text("Téléphone 2") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rc,
                            onValueChange = { rc = it },
                            label = { Text("N° RC") },
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
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nif,
                            onValueChange = { nif = it },
                            label = { Text("NIF") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = nis,
                            onValueChange = { nis = it },
                            label = { Text("NIS") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = nin,
                        onValueChange = { nin = it },
                        label = { Text("N° NIN (Numéro d'Identification National)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_company_nin_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = bankRib,
                        onValueChange = { bankRib = it },
                        label = { Text("RIB Bancaire") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onOpenFullConfig,
                        modifier = Modifier.fillMaxWidth().testTag("open_full_config_from_dialog_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Logo, TVA & Configuration complète")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentSettings.copy(
                            companyName = companyName.trim().ifEmpty { "GESTION MERÉLEC" },
                            activitySlogan = activitySlogan.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            phoneSecondary = phoneSecondary.trim(),
                            email = email.trim(),
                            rc = rc.trim(),
                            nif = nif.trim(),
                            nis = nis.trim(),
                            nin = nin.trim(),
                            articleImposition = articleImposition.trim(),
                            bankRib = bankRib.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_quick_company_coordinates_btn")
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



