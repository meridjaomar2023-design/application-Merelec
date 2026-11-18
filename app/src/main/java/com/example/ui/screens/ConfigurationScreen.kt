package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CompanySettings
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.OperationViewModel
import java.io.File

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.MoreVert
import com.example.ui.viewmodel.BottomNavTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    viewModel: OperationViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenFullCsvExport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val logoVersion by viewModel.logoVersion.collectAsStateWithLifecycle()
    val stampVersion by viewModel.stampVersion.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.selectBottomTab(BottomNavTab.VENTES)
    }

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

    val stampBitmap = remember(stampVersion, logoVersion, currentSettings) {
        com.example.util.PdfGenerator.loadOrCreateWetStampBitmap(context, currentSettings)?.asImageBitmap()
    }
    val hasCustomStamp = remember(stampVersion, logoVersion) {
        File(context.filesDir, "custom_stamp.png").exists()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveCustomLogo(context, it)
        }
    }

    val stampPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveCustomStampPng(context, it)
        }
    }

    // Editable states initialized with currentSettings
    var companyName by remember(currentSettings) { mutableStateOf(currentSettings.companyName) }
    var activitySlogan by remember(currentSettings) { mutableStateOf(currentSettings.activitySlogan) }
    var address by remember(currentSettings) { mutableStateOf(currentSettings.address) }
    var phone by remember(currentSettings) { mutableStateOf(currentSettings.phone) }
    var phoneSecondary by remember(currentSettings) { mutableStateOf(currentSettings.phoneSecondary) }
    var email by remember(currentSettings) { mutableStateOf(currentSettings.email) }
    var website by remember(currentSettings) { mutableStateOf(currentSettings.website) }

    var nif by remember(currentSettings) { mutableStateOf(currentSettings.nif) }
    var nis by remember(currentSettings) { mutableStateOf(currentSettings.nis) }
    var rc by remember(currentSettings) { mutableStateOf(currentSettings.rc) }
    var articleImposition by remember(currentSettings) { mutableStateOf(currentSettings.articleImposition) }
    var nin by remember(currentSettings) { mutableStateOf(currentSettings.nin) }

    var bankName by remember(currentSettings) { mutableStateOf(currentSettings.bankName) }
    var bankRib by remember(currentSettings) { mutableStateOf(currentSettings.bankRib) }

    var tvaEnabledByDefault by remember(currentSettings) { mutableStateOf(currentSettings.tvaEnabledByDefault) }
    var defaultTvaRate by remember(currentSettings) { mutableDoubleStateOf(currentSettings.defaultTvaRate) }
    var defaultTvaRateText by remember(currentSettings) { mutableStateOf(if (currentSettings.defaultTvaRate % 1.0 == 0.0) currentSettings.defaultTvaRate.toInt().toString() else currentSettings.defaultTvaRate.toString()) }
    var reducedTvaRateText by remember(currentSettings) { mutableStateOf(if (currentSettings.reducedTvaRate % 1.0 == 0.0) currentSettings.reducedTvaRate.toInt().toString() else currentSettings.reducedTvaRate.toString()) }

    var currency by remember(currentSettings) { mutableStateOf(currentSettings.currency) }
    var defaultStockAlertText by remember(currentSettings) { mutableStateOf(if (currentSettings.defaultStockAlertThreshold % 1.0 == 0.0) currentSettings.defaultStockAlertThreshold.toInt().toString() else currentSettings.defaultStockAlertThreshold.toString()) }

    var prefixFacture by remember(currentSettings) { mutableStateOf(currentSettings.prefixFacture) }
    var prefixBonCommande by remember(currentSettings) { mutableStateOf(currentSettings.prefixBonCommande) }
    var prefixBonLivraison by remember(currentSettings) { mutableStateOf(currentSettings.prefixBonLivraison) }
    var prefixBonFournisseur by remember(currentSettings) { mutableStateOf(currentSettings.prefixBonFournisseur) }
    var prefixDevis by remember(currentSettings) { mutableStateOf(currentSettings.prefixDevis) }
    var prefixAvoir by remember(currentSettings) { mutableStateOf(currentSettings.prefixAvoir) }
    var prefixReglement by remember(currentSettings) { mutableStateOf(currentSettings.prefixReglement) }

    val allOpsForWeb by viewModel.allOperations.collectAsStateWithLifecycle()
    val clientsForWeb by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliersForWeb by viewModel.suppliersList.collectAsStateWithLifecycle()
    val allPartnersForWeb = remember(clientsForWeb, suppliersForWeb) { clientsForWeb + suppliersForWeb }
    val allProductsForWeb by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val allEmployeesForWeb by viewModel.allEmployees.collectAsStateWithLifecycle()

    val saveWebHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val htmlBytes = com.example.util.ExportUtils.buildStandaloneWebPcHtmlBytes(
                    context = context,
                    settings = currentSettings,
                    operations = allOpsForWeb,
                    partners = allPartnersForWeb,
                    products = allProductsForWeb,
                    employees = allEmployeesForWeb
                )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(htmlBytes)
                }
                viewModel.showMessage("Version Web PC Windows (Merelec_Web_PC_Windows.html) enregistrée avec succès !")
            } catch (e: Exception) {
                viewModel.showMessage("Erreur lors de l'enregistrement : ${e.localizedMessage}")
            }
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Configuration & Entreprise",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Coordonnées, TVA & Paramètres système",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val parsedDefaultTva = defaultTvaRateText.toDoubleOrNull() ?: 19.0
                            val parsedReducedTva = reducedTvaRateText.toDoubleOrNull() ?: 9.0
                            val parsedStockAlert = defaultStockAlertText.toDoubleOrNull() ?: 5.0

                            val updated = currentSettings.copy(
                                companyName = companyName.trim().ifEmpty { "GESTION MERÉLEC" },
                                activitySlogan = activitySlogan.trim(),
                                address = address.trim(),
                                phone = phone.trim(),
                                phoneSecondary = phoneSecondary.trim(),
                                email = email.trim(),
                                website = website.trim(),
                                nif = nif.trim(),
                                nis = nis.trim(),
                                rc = rc.trim(),
                                articleImposition = articleImposition.trim(),
                                nin = nin.trim(),
                                bankName = bankName.trim(),
                                bankRib = bankRib.trim(),
                                tvaEnabledByDefault = tvaEnabledByDefault,
                                defaultTvaRate = parsedDefaultTva,
                                reducedTvaRate = parsedReducedTva,
                                currency = currency.trim().ifEmpty { "DA" },
                                defaultStockAlertThreshold = parsedStockAlert,
                                prefixFacture = prefixFacture.trim().ifEmpty { "FAC-" },
                                prefixBonCommande = prefixBonCommande.trim().ifEmpty { "BC-" },
                                prefixBonLivraison = prefixBonLivraison.trim().ifEmpty { "BL-" },
                                prefixBonFournisseur = prefixBonFournisseur.trim().ifEmpty { "BF-" },
                                prefixDevis = prefixDevis.trim().ifEmpty { "DEV-" },
                                prefixAvoir = prefixAvoir.trim().ifEmpty { "AVR-" },
                                prefixReglement = prefixReglement.trim().ifEmpty { "REG-" }
                            )
                            viewModel.updateCompanySettings(updated)
                        },
                        modifier = Modifier.testTag("config_save_appbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Enregistrer",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Web PC Windows: Lien direct + Fichier autonome Merelec_Web_PC_Windows.html
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_download_index_html_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Version Web pour PC Windows (Chrome / Edge)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Accédez à vos Bons, Factures, Créances, Stock & Scanner OCR depuis votre PC Windows",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🌐 Lien Web direct pour votre PC Windows :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = com.example.util.ExportUtils.WEB_PC_DIRECT_URL,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        clipboard?.setPrimaryClip(
                                            android.content.ClipData.newPlainText(
                                                "Lien Web PC Merélec",
                                                com.example.util.ExportUtils.WEB_PC_DIRECT_URL
                                            )
                                        )
                                        viewModel.showMessage("Lien Web PC Windows copié dans le presse-papiers !")
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("config_copy_web_pc_link_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("📋 Copier le lien Web PC Windows", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                com.example.util.ExportUtils.shareStandaloneWebPcHtml(
                                    context = context,
                                    settings = currentSettings,
                                    operations = allOpsForWeb,
                                    partners = allPartnersForWeb,
                                    products = allProductsForWeb,
                                    employees = allEmployeesForWeb
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("config_share_web_pc_html_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Envoyer vers mon PC Windows (WhatsApp / Email / Drive)")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        FilledTonalButton(
                            onClick = { saveWebHtmlLauncher.launch("Merelec_Web_PC_Windows.html") },
                            modifier = Modifier.fillMaxWidth().testTag("config_download_index_html_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Télécharger Merelec_Web_PC_Windows.html")
                        }
                    }
                }
            }

            // Section Synchronisation Temps Réel Firebase (Cloud Firestore)
            item {
                val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsStateWithLifecycle()
                val isSyncingFirebase by viewModel.isSyncingFirebase.collectAsStateWithLifecycle()
                val lastFirebaseSyncTime by viewModel.lastFirebaseSyncTime.collectAsStateWithLifecycle()

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_firebase_realtime_sync_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFECFDF5)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Synchronisation Temps Réel Firebase",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = if (isFirebaseConnected) {
                                        "🟢 Écouteurs temps réel actifs (Bons, Factures, Clients, Fournisseurs, Articles, Créances)" +
                                            (lastFirebaseSyncTime?.let { " • Dernière sync : $it" } ?: "")
                                    } else {
                                        "⚠️ Connectez-vous avec votre compte Google pour activer la synchronisation Cloud en temps réel"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF047857)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.syncWithFirebaseNow() },
                            enabled = !isSyncingFirebase,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            modifier = Modifier.fillMaxWidth().testTag("config_sync_firebase_now_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isSyncingFirebase) {
                                    "Synchronisation bidirectionnelle en cours..."
                                } else {
                                    "Synchroniser toutes les données en Temps Réel (Firebase)"
                                }
                            )
                        }
                    }
                }
            }

            // Section 0: Gestion des Utilisateurs, Emails, Mots de passe & Droits d'accès
            item {
                val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
                val allEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_users_permissions_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Gestion des Utilisateurs & Mots de passe",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Connecté : ${currentEmployee.name} (${currentEmployee.role.label}) • ${allEmployees.size} compte(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Attribuez à chaque employé son adresse email, son mot de passe personnel et ses droits de modification ou suppression sur les Bons, Factures et Créances.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.EmployeeManagement) },
                            modifier = Modifier.fillMaxWidth().testTag("config_open_users_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gérer les Utilisateurs, Emails, Mots de passe & Droits")
                        }
                    }
                }
            }

            // Section 0bis: Studio de Création & Réorganisation PDF (Devis, Bon, Facture, Créance, Catalogue)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_pdf_studio_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Studio de Création & Réorganisation PDF",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Devis • Bon • Facture • Créance • Catalogue Article",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Ajoutez votre logo (gauche, centré ou droite), réorganisez l'ordre de vos coordonnées et des colonnes de chaque document, et générez vos PDF avec aperçu A4 en direct.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.PdfStudio(0)) },
                            modifier = Modifier.fillMaxWidth().testTag("config_open_pdf_studio_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ouvrir le Studio de Création & Réorganisation PDF")
                        }
                    }
                }
            }

            // Section 1: Logo & Identité Visuelle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_logo_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Logo & Identité Visuelle",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (customBitmap != null) {
                                    Image(
                                        bitmap = customBitmap,
                                        contentDescription = "Logo entreprise",
                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.logo_merelec),
                                        contentDescription = "Logo officiel",
                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("config_change_logo_btn")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Changer le logo", style = MaterialTheme.typography.labelSmall)
                                }

                                if (customBitmap != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.resetToDefaultLogo(context) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("config_reset_logo_btn")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Logo officiel", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Cachet Humide HD en format PNG (sans signature stylo) avec option d'agrandissement
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cachet Humide HD (Format PNG) — Taille : ${currentSettings.pdfStampScale}%",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Apposé automatiquement sur vos Bons (BL, BC, BR), Règlements, Factures et Devis",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = currentSettings.pdfShowStampAndSignature,
                                onCheckedChange = { enabled ->
                                    viewModel.updateCompanySettings(
                                        currentSettings.copy(
                                            pdfShowStampAndSignature = enabled,
                                            pdfShowSignatures = if (enabled) true else currentSettings.pdfShowSignatures
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("config_stamp_signature_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
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

                                Text(
                                    text = "Agrandir le Cachet Humide sur les Bons, Devis & Factures : ${currentSettings.pdfStampScale}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        100 to "100%",
                                        130 to "130%",
                                        160 to "160%",
                                        200 to "200%",
                                        225 to "225%",
                                        250 to "250%"
                                    ).forEach { (scaleVal, label) ->
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.updateCompanySettings(currentSettings.copy(pdfStampScale = scaleVal))
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (currentSettings.pdfStampScale == scaleVal)
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            ),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
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
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).testTag("config_pick_stamp_png_btn")
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Importer Cachet PNG HD", style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (hasCustomStamp) {
                                        TextButton(
                                            onClick = { viewModel.resetStampToDefaultPng(context) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Cachet par défaut", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Coordonnées de l'entreprise
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_coordinates_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Coordonnées de l'Entreprise",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = "Ces informations apparaîtront sur l'en-tête de vos Factures, BL, Bons de Commande et Relevés PDF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Nom ou Raison Sociale *") },
                            modifier = Modifier.fillMaxWidth().testTag("config_input_company_name"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = activitySlogan,
                            onValueChange = { activitySlogan = it },
                            label = { Text("Activité / Slogan commercial") },
                            modifier = Modifier.fillMaxWidth().testTag("config_input_activity"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Adresse du siège / Dépôt") },
                            modifier = Modifier.fillMaxWidth().testTag("config_input_address"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Tél. principal *") },
                                modifier = Modifier.weight(1f).testTag("config_input_phone"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) }
                            )

                            OutlinedTextField(
                                value = phoneSecondary,
                                onValueChange = { phoneSecondary = it },
                                label = { Text("Tél. mobile") },
                                modifier = Modifier.weight(1f).testTag("config_input_phone_sec"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email professionnel") },
                                modifier = Modifier.weight(1f).testTag("config_input_email"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                            )

                            OutlinedTextField(
                                value = website,
                                onValueChange = { website = it },
                                label = { Text("Site Web") },
                                modifier = Modifier.weight(1f).testTag("config_input_website"),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            // Section 3: Identifiants Légaux & Fiscaux (Algérie)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_legal_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Identifiants Fiscaux & Bancaires",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = rc,
                                onValueChange = { rc = it },
                                label = { Text("RC N°") },
                                modifier = Modifier.weight(1f).testTag("config_input_rc"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = articleImposition,
                                onValueChange = { articleImposition = it },
                                label = { Text("N° article") },
                                modifier = Modifier.weight(1f).testTag("config_input_ai"),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nif,
                                onValueChange = { nif = it },
                                label = { Text("NIF N°") },
                                modifier = Modifier.weight(1f).testTag("config_input_nif"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = nis,
                                onValueChange = { nis = it },
                                label = { Text("NIS N°") },
                                modifier = Modifier.weight(1f).testTag("config_input_nis"),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = nin,
                            onValueChange = { nin = it },
                            label = { Text("NIN N° (Numéro d'Identification National)") },
                            placeholder = { Text("Ex: 109801600012345001") },
                            modifier = Modifier.fillMaxWidth().testTag("config_input_nin"),
                            singleLine = true
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Coordonnées Bancaires",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Banque") },
                            placeholder = { Text("Ex: BNA Agence Alger, CPA, CCP...") },
                            modifier = Modifier.fillMaxWidth().testTag("config_input_bank_name"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = bankRib,
                            onValueChange = { bankRib = it },
                            label = { Text("RIB") },
                            placeholder = { Text("Ex: 001 00234 1234567890 12") },
                            modifier = Modifier.fillMaxWidth().testTag("config_input_bank_rib"),
                            singleLine = true
                        )
                    }
                }
            }

            // Section 4: Gestion de la TVA
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_tva_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                Column {
                                    Text(
                                        text = "Gestion de la TVA",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Activation automatique & taux applicables",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Activer TVA par défaut sur les factures",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Cochera automatiquement la TVA lors de la création d'une nouvelle facture",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = tvaEnabledByDefault,
                                onCheckedChange = { tvaEnabledByDefault = it },
                                modifier = Modifier.testTag("config_tva_default_switch")
                            )
                        }

                        Text(
                            text = "Taux de TVA standard par défaut :",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(19.0 to "19% (Standard DZ)", 9.0 to "9% (Taux réduit)", 0.0 to "0% (Exonéré)").forEach { (rate, label) ->
                                val isSelected = defaultTvaRateText.toDoubleOrNull() == rate
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        defaultTvaRateText = if (rate % 1.0 == 0.0) rate.toInt().toString() else rate.toString()
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("config_tva_rate_chip_${rate.toInt()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = defaultTvaRateText,
                                onValueChange = { defaultTvaRateText = it },
                                label = { Text("Taux standard (%)") },
                                modifier = Modifier.weight(1f).testTag("config_input_tva_standard"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                trailingIcon = { Text("%", modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold) }
                            )

                            OutlinedTextField(
                                value = reducedTvaRateText,
                                onValueChange = { reducedTvaRateText = it },
                                label = { Text("Taux réduit (%)") },
                                modifier = Modifier.weight(1f).testTag("config_input_tva_reduced"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                trailingIcon = { Text("%", modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            // Section 5: Autres Paramètres & Préfixes
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_other_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Autres Paramètres & Préférences",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = currency,
                                onValueChange = { currency = it },
                                label = { Text("Devise d'affichage") },
                                placeholder = { Text("DA") },
                                modifier = Modifier.weight(1f).testTag("config_input_currency"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = defaultStockAlertText,
                                onValueChange = { defaultStockAlertText = it },
                                label = { Text("Seuil alerte stock par défaut") },
                                modifier = Modifier.weight(1f).testTag("config_input_stock_alert"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Préfixes de numérotation automatique :",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = prefixFacture,
                                onValueChange = { prefixFacture = it },
                                label = { Text("Factures") },
                                modifier = Modifier.weight(1f).testTag("config_prefix_fac"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = prefixBonCommande,
                                onValueChange = { prefixBonCommande = it },
                                label = { Text("Bons Commande") },
                                modifier = Modifier.weight(1f).testTag("config_prefix_bc"),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = prefixBonLivraison,
                                onValueChange = { prefixBonLivraison = it },
                                label = { Text("BL Client") },
                                modifier = Modifier.weight(1f).testTag("config_prefix_bl"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = prefixBonFournisseur,
                                onValueChange = { prefixBonFournisseur = it },
                                label = { Text("Bon Fournisseur") },
                                modifier = Modifier.weight(1f).testTag("config_prefix_bf"),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = prefixDevis,
                                onValueChange = { prefixDevis = it },
                                label = { Text("Devis") },
                                modifier = Modifier.weight(1f).testTag("config_prefix_dev"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = prefixAvoir,
                                onValueChange = { prefixAvoir = it },
                                label = { Text("Avoirs") },
                                modifier = Modifier.weight(1f).testTag("config_prefix_avr"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = prefixReglement,
                                onValueChange = { prefixReglement = it },
                                label = { Text("Règlement") },
                                modifier = Modifier.weight(1f).testTag("config_prefix_reg"),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Section 6: Sauvegardes & Outils Système
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("config_system_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sauvegardes & Exportations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.BackupDrive) },
                                modifier = Modifier.weight(1f).testTag("config_open_drive_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddToDrive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Google Drive", style = MaterialTheme.typography.labelSmall)
                            }

                            FilledTonalButton(
                                onClick = { viewModel.navigateTo(AppScreen.Management(0)) },
                                modifier = Modifier.weight(1f).testTag("config_open_dir_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Répertoire", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.ImportDirectory) },
                            modifier = Modifier.fillMaxWidth().testTag("config_open_import_db_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Importer Base de Données : Articles, Clients, Fourn. & Créances (CSV / Excel)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (onOpenFullCsvExport != null) {
                            Button(
                                onClick = onOpenFullCsvExport,
                                modifier = Modifier.fillMaxWidth().testTag("config_export_full_room_csv_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Exporter Toute la Base Room (CSV Externe)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
                        val isAdminUser = currentEmployee.role == com.example.data.model.EmployeeRole.ADMIN ||
                            currentEmployee.email.equals(com.example.data.model.EmployeeEntity.PRIMARY_ADMIN_EMAIL, ignoreCase = true)
                        var showConfigResetDbDialog by remember { mutableStateOf(false) }

                        if (isAdminUser) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Button(
                                onClick = { showConfigResetDbDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("config_admin_reset_db_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Remise à zéro de la Base de Données (Administrateur)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        if (showConfigResetDbDialog && isAdminUser) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { showConfigResetDbDialog = false },
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Remise à zéro de la Base de Données",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Action réservée à l'administrateur (${currentEmployee.email}). Voulez-vous remettre la base de données à zéro ?",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.resetDatabaseForAdmin(onlyDirectory = false)
                                                showConfigResetDbDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth().testTag("config_confirm_full_reset_db_btn")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Remettre TOUTE la base à zéro")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.resetDatabaseForAdmin(onlyDirectory = true)
                                                showConfigResetDbDialog = false
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("config_confirm_dir_reset_db_btn")
                                        ) {
                                            Text("Vider uniquement Articles & Clients/Fourn.")
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    androidx.compose.material3.TextButton(onClick = { showConfigResetDbDialog = false }) {
                                        Text("Annuler")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bouton Principal d'enregistrement
            item {
                Button(
                    onClick = {
                        val parsedDefaultTva = defaultTvaRateText.toDoubleOrNull() ?: 19.0
                        val parsedReducedTva = reducedTvaRateText.toDoubleOrNull() ?: 9.0
                        val parsedStockAlert = defaultStockAlertText.toDoubleOrNull() ?: 5.0

                        val updated = currentSettings.copy(
                            companyName = companyName.trim().ifEmpty { "GESTION MERÉLEC" },
                            activitySlogan = activitySlogan.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            phoneSecondary = phoneSecondary.trim(),
                            email = email.trim(),
                            website = website.trim(),
                            nif = nif.trim(),
                            nis = nis.trim(),
                            rc = rc.trim(),
                            articleImposition = articleImposition.trim(),
                            nin = nin.trim(),
                            bankName = bankName.trim(),
                            bankRib = bankRib.trim(),
                            tvaEnabledByDefault = tvaEnabledByDefault,
                            defaultTvaRate = parsedDefaultTva,
                            reducedTvaRate = parsedReducedTva,
                            currency = currency.trim().ifEmpty { "DA" },
                            defaultStockAlertThreshold = parsedStockAlert,
                            prefixFacture = prefixFacture.trim().ifEmpty { "FAC-" },
                            prefixBonCommande = prefixBonCommande.trim().ifEmpty { "BC-" },
                            prefixBonLivraison = prefixBonLivraison.trim().ifEmpty { "BL-" },
                            prefixBonFournisseur = prefixBonFournisseur.trim().ifEmpty { "BF-" },
                            prefixDevis = prefixDevis.trim().ifEmpty { "DEV-" },
                            prefixAvoir = prefixAvoir.trim().ifEmpty { "AVR-" },
                            prefixReglement = prefixReglement.trim().ifEmpty { "REG-" }
                        )
                        viewModel.updateCompanySettings(updated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("config_save_bottom_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enregistrer la Configuration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
