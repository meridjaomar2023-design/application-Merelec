package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.BackupContent
import com.example.util.BackupUtils
import com.example.util.DateUtils
import com.example.util.DriveAuthOutcome
import com.example.util.GoogleDriveService
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveBackupScreen(
    viewModel: OperationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val operations by viewModel.allOperations.collectAsStateWithLifecycle()
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    val products by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val lastBackupDate by viewModel.lastBackupDate.collectAsStateWithLifecycle()
    val driveAccessToken by viewModel.driveAccessToken.collectAsStateWithLifecycle()
    val driveBackups by viewModel.driveBackups.collectAsStateWithLifecycle()
    val isDriveBusy by viewModel.isDriveBusy.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val isAdminUser = currentEmployee.isAdmin

    var pendingRestoreContent by remember { mutableStateOf<BackupContent?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingUploadAfterAuth by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateBack()
    }

    // Google Identity OAuth2 Consent Resolution Launcher
    val driveAuthIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val tokenResult = GoogleDriveService.extractAccessTokenFromIntent(context, result.data)
            tokenResult.onSuccess { token ->
                viewModel.setDriveAccessToken(token)
                if (pendingUploadAfterAuth) {
                    pendingUploadAfterAuth = false
                    viewModel.uploadBackupToGoogleDrive(token)
                } else {
                    viewModel.showMessage("Compte Google Drive connecté avec succès !")
                }
            }.onFailure { err ->
                pendingUploadAfterAuth = false
                viewModel.showMessage("Erreur d'autorisation Google Drive : ${err.localizedMessage}")
            }
        } else {
            pendingUploadAfterAuth = false
            viewModel.showMessage("Autorisation Google Drive annulée.")
        }
    }

    fun triggerDriveAuth(uploadImmediately: Boolean) {
        coroutineScope.launch {
            pendingUploadAfterAuth = uploadImmediately
            when (val outcome = GoogleDriveService.authorizeDriveAccess(context)) {
                is DriveAuthOutcome.Authorized -> {
                    viewModel.setDriveAccessToken(outcome.accessToken)
                    if (uploadImmediately) {
                        pendingUploadAfterAuth = false
                        viewModel.uploadBackupToGoogleDrive(outcome.accessToken)
                    } else {
                        viewModel.showMessage("Connecté à Google Drive !")
                    }
                }
                is DriveAuthOutcome.NeedsResolution -> {
                    val intentSenderRequest = IntentSenderRequest.Builder(outcome.pendingIntent).build()
                    driveAuthIntentLauncher.launch(intentSenderRequest)
                }
                is DriveAuthOutcome.Error -> {
                    pendingUploadAfterAuth = false
                    viewModel.showMessage(outcome.message)
                }
            }
        }
    }

    // Launcher 1: Create Document directly in Google Drive / Storage Access Framework
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val json = viewModel.generateBackupJson()
                context.contentResolver.openOutputStream(it)?.use { os ->
                    BackupUtils.writeBackupToStream(os, json)
                }
                viewModel.recordBackupSuccess()
                viewModel.showMessage("Sauvegarde enregistrée avec succès dans Google Drive !")
            } catch (e: Exception) {
                viewModel.showMessage("Erreur lors de la sauvegarde : ${e.localizedMessage}")
            }
        }
    }

    // Launcher 2: Open Document from Google Drive for restoration
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val jsonText = BackupUtils.readBackupFromUri(context, it)
                val parsed = BackupUtils.parseBackupJson(jsonText)
                if (parsed != null) {
                    pendingRestoreContent = parsed
                    showRestoreDialog = true
                } else {
                    viewModel.showMessage("Le fichier sélectionné n'est pas une sauvegarde Gestion Merelec valide.")
                }
            } catch (e: Exception) {
                viewModel.showMessage("Impossible de lire la sauvegarde : ${e.localizedMessage}")
            }
        }
    }

    // Dialogue de confirmation de restauration
    if (showRestoreDialog && pendingRestoreContent != null) {
        val backup = pendingRestoreContent!!
        AlertDialog(
            onDismissRequest = {
                showRestoreDialog = false
                pendingRestoreContent = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Confirmer la Restauration",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Voulez-vous restaurer les données de cette sauvegarde Google Drive ?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Date de la sauvegarde : ${backup.dateFormatted}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "• ${backup.operationsCount} bon(s) et règlement(s)", style = MaterialTheme.typography.bodySmall)
                            Text(text = "• ${backup.clientsCount} client(s) & ${backup.suppliersCount} fournisseur(s)", style = MaterialTheme.typography.bodySmall)
                            Text(text = "• ${backup.productsCount} article(s) au catalogue", style = MaterialTheme.typography.bodySmall)
                            if (backup.compositions.isNotEmpty()) {
                                Text(text = "• ${backup.compositions.size} composant(s) d'articles éclatés", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Text(
                        text = "Note : Les données restaurées seront fusionnées avec votre base existante.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDatabase(backup) {
                            showRestoreDialog = false
                            pendingRestoreContent = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_btn")
                ) {
                    Text("Restaurer les données")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        pendingRestoreContent = null
                    }
                ) {
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
                            text = "Sauvegarde Google Drive",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Synchronisation Cloud & Restauration de la base",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("drive_backup_back_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (driveAccessToken != null) {
                        IconButton(
                            onClick = { viewModel.refreshDriveBackups() },
                            enabled = !isDriveBusy,
                            modifier = Modifier.testTag("refresh_drive_backups_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualiser les sauvegardes Google Drive",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card: Status & Google Drive Integration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F9D58).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDriveBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = Color(0xFF0F9D58),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF0F9D58),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Google Drive Cloud Backup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (driveAccessToken != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Connecté",
                                    tint = Color(0xFF0F9D58),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lastBackupDate != null)
                                    "Dernière sauvegarde : $lastBackupDate"
                                else
                                    "Aucune sauvegarde effectuée pour l'instant",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Database Current Metrics
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Contenu de la base de données à sauvegarder :",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bons de livraison & Règlements", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("${operations.size}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clients & Fournisseurs", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("${clients.size + suppliers.size} (${clients.size} clients / ${suppliers.size} fourn.)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Catalogue des Articles", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("${products.size} article(s)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // Section Cloud Direct API (Google Drive API v3 with drive.file OAuth scope)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFF0F9D58)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Synchronisation Directe Google Drive",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sauvegardez et restaurez directement vos données en un clic sur votre espace Google Drive sécurisé (autorisation OAuth drive.file).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (driveAccessToken != null) {
                                viewModel.uploadBackupToGoogleDrive(driveAccessToken)
                            } else {
                                triggerDriveAuth(uploadImmediately = true)
                            }
                        },
                        enabled = !isDriveBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cloud_sync_drive_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58))
                    ) {
                        Icon(imageVector = Icons.Default.AddToDrive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isDriveBusy) "Synchronisation en cours..."
                            else "Sauvegarder en 1 clic sur Google Drive"
                        )
                    }

                    if (driveAccessToken == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { triggerDriveAuth(uploadImmediately = false) },
                            enabled = !isDriveBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connect_drive_account_button")
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Afficher mes sauvegardes Google Drive")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sauvegardes sur votre Google Drive (${driveBackups.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { viewModel.refreshDriveBackups() },
                                enabled = !isDriveBusy,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Actualiser", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (driveBackups.isEmpty() && !isDriveBusy) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Aucune sauvegarde trouvée sur votre compte Google Drive. Cliquez sur « Sauvegarder en 1 clic » ci-dessus pour créer votre première sauvegarde Cloud.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                driveBackups.forEach { file ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = Color(0xFF0F9D58),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = file.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val sizeKb = String.format(Locale.FRENCH, "%.1f Ko", file.sizeBytes / 1024.0)
                                                Text(
                                                    text = "${file.createdTimeFormatted} • $sizeKb",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.downloadBackupFromGoogleDrive(file.id) { parsed ->
                                                        pendingRestoreContent = parsed
                                                        showRestoreDialog = true
                                                    }
                                                },
                                                enabled = !isDriveBusy,
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("restore_cloud_item_${file.id}")
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Restaurer", style = MaterialTheme.typography.labelSmall)
                                            }
                                            if (isAdminUser) {
                                                IconButton(
                                                    onClick = { viewModel.deleteBackupFromGoogleDrive(file.id) },
                                                    enabled = !isDriveBusy,
                                                    modifier = Modifier.testTag("delete_cloud_item_${file.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Supprimer cette sauvegarde",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.testTag("locked_delete_cloud_item_${file.id}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Security,
                                                            contentDescription = "Suppression réservée à l'administrateur",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "Admin",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!isAdminUser) {
                                    Text(
                                        text = "🔒 La suppression de sauvegarde est désactivée pour les comptes non-administrateurs (${currentEmployee.name}).",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: Sauvegarde via Sélecteur de Dossier / Application Google Drive
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Sauvegarder un fichier dans un dossier",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enregistre un fichier de sauvegarde complet dans le dossier Google Drive ou local de votre choix.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bouton 1 : Choisir dossier dans Google Drive
                    Button(
                        onClick = {
                            val defaultName = "Gestion_Merelec_Backup_${DateUtils.formatForExport(System.currentTimeMillis())}.json"
                            createDocumentLauncher.launch(defaultName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_to_drive_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enregistrer dans Google Drive")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bouton 2 : Envoyer via l'app Google Drive
                    FilledTonalButton(
                        onClick = {
                            val json = viewModel.generateBackupJson()
                            BackupUtils.shareBackupToDriveApp(context, json)
                            viewModel.recordBackupSuccess()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_backup_drive_app_button")
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Envoyer vers l'application Google Drive")
                    }
                }
            }

            // Section 2: Restauration depuis un fichier
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Restaurer depuis un fichier",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sélectionnez un fichier de sauvegarde (.json) depuis votre explorateur Google Drive ou votre appareil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_from_drive_button")
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restaurer depuis Google Drive")
                    }
                }
            }

            // Security & Privacy notice
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vos sauvegardes sont privées et stockées directement sur votre compte Google Drive personnel (scope sécurisé drive.file).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
