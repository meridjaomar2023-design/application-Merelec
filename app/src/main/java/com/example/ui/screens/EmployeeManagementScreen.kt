package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeRole
import com.example.ui.viewmodel.OperationViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EmployeeManagementScreen(
    viewModel: OperationViewModel,
    onSwitchEmployee: () -> Unit = {}
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val rawEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val employees = remember(rawEmployees) {
        rawEmployees.distinctBy { emp ->
            EmployeeEntity.normalizeEmail(emp.email).ifBlank { emp.id }
        }
    }
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showAddThreeUsersDialog by remember { mutableStateOf(false) }
    var employeeToDelete by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showChangeOwnPasswordDialog by remember { mutableStateOf(false) }

    val canManage = currentEmployee.canManageEmployees || currentEmployee.role == EmployeeRole.ADMIN

    // Si un employé non-administrateur ouvre cet écran, il doit saisir le mot de passe d'un Administrateur
    var adminUnlockEmail by remember { mutableStateOf("") }
    var adminUnlockPassword by remember { mutableStateOf("") }
    var adminUnlockPasswordVisible by remember { mutableStateOf(false) }
    var adminUnlockError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    fun openCreateUserDialog() {
        isCreatingNew = true
        editingEmployee = EmployeeEntity(
            id = "EMP-${UUID.randomUUID().toString().take(8).uppercase()}",
            name = "",
            email = "",
            password = "",
            role = EmployeeRole.VENDEUR,
            isActive = true,
            canModifyBon = true,
            canDeleteBon = false,
            canModifyFacture = false,
            canDeleteFacture = false,
            canModifyCreance = false,
            canDeleteCreance = false,
            canManageEmployees = false
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Utilisateurs & Droits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Emails, mots de passe & accès",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("back_from_employees_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSwitchEmployee,
                        modifier = Modifier.testTag("switch_employee_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwitchAccount,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Se déconnecter")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (canManage) {
                ExtendedFloatingActionButton(
                    onClick = { openCreateUserDialog() },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Attribuer un nouvel utilisateur") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_employee_fab")
                )
            }
        }
    ) { innerPadding ->
        if (!canManage) {
            // Écran de verrouillage : un simple vendeur/caissier ne peut PAS voir ni modifier les utilisateurs
            // ni basculer en mode Administrateur sans saisir l'email et le mot de passe Administrateur !
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Accès Réservé à l'Administrateur",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Vous êtes connecté en tant que « ${currentEmployee.name} » (${currentEmployee.role.label}). Seul un Administrateur peut créer des utilisateurs, attribuer leurs emails/mots de passe ou modifier leurs droits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Authentification Administrateur requise :",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = adminUnlockEmail,
                            onValueChange = {
                                adminUnlockEmail = it
                                adminUnlockError = null
                            },
                            label = { Text("Email Administrateur") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = adminUnlockPassword,
                            onValueChange = {
                                adminUnlockPassword = it
                                adminUnlockError = null
                            },
                            label = { Text("Mot de passe Administrateur") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { adminUnlockPasswordVisible = !adminUnlockPasswordVisible }) {
                                    Icon(
                                        imageVector = if (adminUnlockPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (adminUnlockPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (adminUnlockError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = adminUnlockError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val matchedAdmin = employees.find {
                                    it.isActive &&
                                        (it.role == EmployeeRole.ADMIN || it.canManageEmployees) &&
                                        EmployeeEntity.normalizeEmail(it.email) == EmployeeEntity.normalizeEmail(adminUnlockEmail) &&
                                        it.password == adminUnlockPassword.trim()
                                }
                                if (matchedAdmin != null) {
                                    viewModel.setAuthenticatedEmployee(matchedAdmin)
                                    viewModel.showMessage("Mode Administrateur déverrouillé (${matchedAdmin.name}).")
                                } else {
                                    adminUnlockError = "Email ou mot de passe Administrateur incorrect."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Déverrouiller la Gestion des Utilisateurs")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Carte de session de l'Administrateur connecté
                item {
                    ActiveEmployeeBannerCard(
                        employee = currentEmployee,
                        onChangeOwnPassword = {
                            showChangeOwnPasswordDialog = true
                        },
                        onEditOwnCredentials = {
                            isCreatingNew = false
                            editingEmployee = currentEmployee
                        },
                        onSwitchEmployee = onSwitchEmployee
                    )
                }

                // 2. Carte d'action rapide pour créer 1 ou 3 utilisateurs et leur attribuer email & mot de passe
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Attribuer des comptes utilisateurs",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ajoutez 1 utilisateur ou configurez 3 utilisateurs en même temps avec leur email, mot de passe personnel et droits (Bon, Facture, Créance).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddThreeUsersDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .testTag("add_three_employees_button")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ajouter 3 Utilisateurs")
                                }
                                OutlinedButton(
                                    onClick = { openCreateUserDialog() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_single_employee_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+1 Utilisateur")
                                }
                            }
                        }
                    }
                }

                // 3. En-tête de la liste des comptes utilisateurs synchronisés dans Firestore
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Utilisateurs & Identifiants (${employees.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(employees, key = { it.id }) { emp ->
                    EmployeeAccountCard(
                        employee = emp,
                        isCurrentSession = emp.id == currentEmployee.id || emp.email.equals(currentEmployee.email, ignoreCase = true),
                        onEdit = {
                            isCreatingNew = false
                            editingEmployee = emp
                        },
                        onDelete = {
                            employeeToDelete = emp
                        },
                        onToggleWritePermission = { allowWrite ->
                            viewModel.saveEmployeeAccount(emp.withAllWritePermissions(allowWrite))
                        },
                        onToggleDeletePermission = { allowDelete ->
                            viewModel.saveEmployeeAccount(emp.withAllDeletePermissions(allowDelete))
                        }
                    )
                }
            }
        }
    }

    // Dialog d'ajout / modification d'un employé (Email, Mot de passe & Droits Bon/Facture/Créance)
    editingEmployee?.let { draftEmp ->
        EmployeeEditorDialog(
            initialEmployee = draftEmp,
            isNew = isCreatingNew,
            onDismiss = { editingEmployee = null },
            onSave = { updated ->
                viewModel.saveEmployeeAccount(updated)
                editingEmployee = null
            }
        )
    }

    // Dialog de création groupée de 3 utilisateurs en même temps
    if (showAddThreeUsersDialog) {
        BatchAddThreeEmployeesDialog(
            onDismiss = { showAddThreeUsersDialog = false },
            onSaveThree = { list ->
                viewModel.saveMultipleEmployeeAccounts(list)
                showAddThreeUsersDialog = false
            }
        )
    }

    // Dialog de confirmation de suppression d'employé
    employeeToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Supprimer l'utilisateur ?") },
            text = {
                Text("Voulez-vous vraiment révoquer et supprimer le compte de « ${target.name} » (${target.email}) ? Cet utilisateur ne pourra plus se connecter à l'application.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployeeAccount(target)
                        employeeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialog dédié pour changer le mot de passe de l'Administrateur une fois connecté à l'intérieur
    if (showChangeOwnPasswordDialog) {
        ChangeAdminPasswordDialog(
            admin = currentEmployee,
            onDismiss = { showChangeOwnPasswordDialog = false },
            onSaveNewPassword = { newEmail, newPassword ->
                val updated = currentEmployee.copy(
                    email = newEmail.trim().lowercase(),
                    password = newPassword.trim()
                )
                viewModel.saveEmployeeAccount(updated)
                viewModel.showMessage("Votre mot de passe Administrateur a été mis à jour avec succès !")
                showChangeOwnPasswordDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveEmployeeBannerCard(
    employee: EmployeeEntity,
    onChangeOwnPassword: () -> Unit,
    onEditOwnCredentials: () -> Unit,
    onSwitchEmployee: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (employee.role == EmployeeRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin : ${employee.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Email : ${employee.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                OutlinedButton(
                    onClick = onSwitchEmployee,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verrouiller", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onChangeOwnPassword,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("change_admin_password_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Changer mon mot de passe", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = onEditOwnCredentials,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Profil", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ChangeAdminPasswordDialog(
    admin: EmployeeEntity,
    onDismiss: () -> Unit,
    onSaveNewPassword: (newEmail: String, newPassword: String) -> Unit
) {
    var emailInput by remember(admin) { mutableStateOf(admin.email) }
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Changer le mot de passe Administrateur",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Vous êtes connecté en tant qu'Administrateur (${admin.name}). Saisissez votre mot de passe actuel puis votre nouveau mot de passe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        errorMsg = null
                    },
                    label = { Text("Email Administrateur") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentPasswordInput,
                    onValueChange = {
                        currentPasswordInput = it
                        errorMsg = null
                    },
                    label = { Text("Mot de passe actuel") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPasswords = !showPasswords }) {
                            Icon(
                                imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("current_admin_password_input")
                )

                OutlinedTextField(
                    value = newPasswordInput,
                    onValueChange = {
                        newPasswordInput = it
                        errorMsg = null
                    },
                    label = { Text("Nouveau mot de passe") },
                    placeholder = { Text("Minimum 4 caractères") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_admin_password_input")
                )

                OutlinedTextField(
                    value = confirmPasswordInput,
                    onValueChange = {
                        confirmPasswordInput = it
                        errorMsg = null
                    },
                    label = { Text("Confirmer le nouveau mot de passe") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_admin_password_input")
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (emailInput.isBlank() || !emailInput.contains("@")) {
                        errorMsg = "Veuillez saisir une adresse email valide."
                        return@Button
                    }
                    if (currentPasswordInput.trim() != admin.password) {
                        errorMsg = "Le mot de passe actuel saisi est incorrect."
                        return@Button
                    }
                    if (newPasswordInput.trim().length < 4) {
                        errorMsg = "Le nouveau mot de passe doit contenir au moins 4 caractères."
                        return@Button
                    }
                    if (newPasswordInput.trim() != confirmPasswordInput.trim()) {
                        errorMsg = "La confirmation du nouveau mot de passe ne correspond pas."
                        return@Button
                    }
                    onSaveNewPassword(emailInput, newPasswordInput)
                },
                modifier = Modifier.testTag("save_new_admin_password_button")
            ) {
                Text("Enregistrer le nouveau mot de passe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeAccountCard(
    employee: EmployeeEntity,
    isCurrentSession: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleWritePermission: (Boolean) -> Unit = {},
    onToggleDeletePermission: (Boolean) -> Unit = {}
) {
    var showPassword by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employee_card_${employee.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isCurrentSession) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (employee.isActive) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (!employee.isActive) Icons.Default.Block
                            else if (employee.role == EmployeeRole.ADMIN) Icons.Default.AdminPanelSettings
                            else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (employee.isActive) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = employee.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Rôle : ${employee.role.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isCurrentSession) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "Session Actuelle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!employee.isActive) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "Suspendu",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_employee_${employee.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier identifiants et droits",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (!isCurrentSession) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("delete_employee_${employee.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer l'employé",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Encadré affichant :
            // 1) Email sur une seule ligne
            // 2) Mot de passe au-dessous
            // 3) Changer d'email / MDP au-dessous
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Ligne 1 : Email sur une seule ligne
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Email : ${employee.email}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Ligne 2 : Mot de passe au-dessous (sur une seule ligne avec bouton œil)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showPassword) "Mot de passe : ${employee.password}" else "Mot de passe : ••••••••",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Masquer" else "Voir le mot de passe",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Ligne 3 : Changer d'email / MDP au-dessous
                    FilledTonalButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Changer d'email / MDP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Contrôle rapide des Autorisations d'Écriture et de Suppression
            Text(
                text = "Gestion rapide des Autorisations :",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (employee.hasAnyWritePermission) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    border = BorderStroke(
                        1.dp,
                        if (employee.hasAnyWritePermission) Color(0xFF2E7D32).copy(alpha = 0.4f) else Color(0xFFC62828).copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Écriture",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (employee.hasAnyWritePermission) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            )
                            Text(
                                text = if (employee.hasAllWritePermissions) "Autorisée"
                                else if (employee.hasAnyWritePermission) "Partielle"
                                else "Lecture seule",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (employee.hasAnyWritePermission) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        Switch(
                            checked = employee.hasAnyWritePermission,
                            onCheckedChange = { onToggleWritePermission(it) },
                            modifier = Modifier.testTag("toggle_write_${employee.id}")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (employee.hasAnyDeletePermission) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    border = BorderStroke(
                        1.dp,
                        if (employee.hasAnyDeletePermission) Color(0xFF2E7D32).copy(alpha = 0.4f) else Color(0xFFC62828).copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Suppr.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (employee.hasAnyDeletePermission) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            )
                            Text(
                                text = if (employee.hasAllDeletePermissions) "Autorisée"
                                else if (employee.hasAnyDeletePermission) "Partielle"
                                else "Interdite",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (employee.hasAnyDeletePermission) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        Switch(
                            checked = employee.hasAnyDeletePermission,
                            onCheckedChange = { onToggleDeletePermission(it) },
                            modifier = Modifier.testTag("toggle_delete_${employee.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PermissionStatusBadge("Écriture Bon", employee.canModifyBon)
                PermissionStatusBadge("Suppr. Bon", employee.canDeleteBon)
                PermissionStatusBadge("Écriture Facture", employee.canModifyFacture)
                PermissionStatusBadge("Suppr. Facture", employee.canDeleteFacture)
                PermissionStatusBadge("Écriture Créance", employee.canModifyCreance)
                PermissionStatusBadge("Suppr. Créance", employee.canDeleteCreance)
            }
        }
    }
}

@Composable
fun PermissionStatusBadge(
    label: String,
    allowed: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (allowed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        border = BorderStroke(
            0.8.dp,
            if (allowed) Color(0xFF2E7D32).copy(alpha = 0.4f) else Color(0xFFC62828).copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (allowed) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = if (allowed) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (allowed) Color(0xFF1B5E20) else Color(0xFFB71C1C)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeEditorDialog(
    initialEmployee: EmployeeEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (EmployeeEntity) -> Unit
) {
    var draft by remember(initialEmployee) { mutableStateOf(initialEmployee) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isNew) "Créer un utilisateur & ses accès" else "Modifier Email, Mot de passe & Droits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = {
                        draft = draft.copy(name = it)
                        errorText = null
                    },
                    label = { Text("Nom complet de l'utilisateur") },
                    placeholder = { Text("ex: Karim Benali") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = draft.email,
                    onValueChange = {
                        draft = draft.copy(email = it)
                        errorText = null
                    },
                    label = { Text("Email de connexion attribué") },
                    placeholder = { Text("ex: karim@merelec.dz") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = draft.password,
                    onValueChange = {
                        draft = draft.copy(password = it)
                        errorText = null
                    },
                    label = { Text("Mot de passe attribué") },
                    placeholder = { Text("Définir son mot de passe") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Profil de rôle (pré-remplit les droits) :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EmployeeRole.entries.forEach { roleOption ->
                        FilterChip(
                            selected = draft.role == roleOption,
                            onClick = {
                                draft = EmployeeEntity.applyRolePreset(draft, roleOption)
                            },
                            label = { Text(roleOption.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider()

                PermissionToggleRow(
                    title = "Autoriser l'accès à l'application",
                    subtitle = "Si désactivé, cet email ne pourra plus se connecter",
                    checked = draft.isActive,
                    onCheckedChange = { draft = draft.copy(isActive = it) }
                )

                HorizontalDivider()

                Text(
                    text = "Autorisations Globales (Écriture & Suppression)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PermissionToggleRow(
                    title = "Autorisation d'Écriture & Modification (Tous modules)",
                    subtitle = "Autoriser l'écriture et la modification des Bons, Factures, Créances, Clients et Articles",
                    checked = draft.hasAllWritePermissions,
                    onCheckedChange = { draft = draft.withAllWritePermissions(it) }
                )
                PermissionToggleRow(
                    title = "Autorisation de Suppression (Tous modules)",
                    subtitle = "Autoriser la suppression des Bons, Factures, Créances, Clients et Articles",
                    checked = draft.hasAllDeletePermissions,
                    onCheckedChange = { draft = draft.withAllDeletePermissions(it) }
                )

                HorizontalDivider()

                Text(
                    text = "1. Détail Droits sur les Bons (BL, Commandes, Devis)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PermissionToggleRow(
                    title = "Droit d'Écriture / Modification de Bon",
                    subtitle = "Créer et modifier les Bons de livraison, Bons de commande et Devis",
                    checked = draft.canModifyBon,
                    onCheckedChange = { draft = draft.copy(canModifyBon = it) }
                )
                PermissionToggleRow(
                    title = "Droit de Suppression de Bon",
                    subtitle = "Supprimer définitivement un Bon de livraison, BC ou Devis",
                    checked = draft.canDeleteBon,
                    onCheckedChange = { draft = draft.copy(canDeleteBon = it) }
                )

                HorizontalDivider()

                Text(
                    text = "2. Détail Droits sur les Factures & Avoirs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PermissionToggleRow(
                    title = "Droit d'Écriture / Modification de Facture",
                    subtitle = "Modifier les Factures, Avoirs et convertir des BL en Facture",
                    checked = draft.canModifyFacture,
                    onCheckedChange = { draft = draft.copy(canModifyFacture = it) }
                )
                PermissionToggleRow(
                    title = "Droit de Suppression de Facture",
                    subtitle = "Supprimer définitivement une Facture ou un Avoir",
                    checked = draft.canDeleteFacture,
                    onCheckedChange = { draft = draft.copy(canDeleteFacture = it) }
                )

                HorizontalDivider()

                Text(
                    text = "3. Détail Droits sur les Créances & Règlements",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PermissionToggleRow(
                    title = "Droit d'Écriture / Modification de Créance",
                    subtitle = "Modifier le solde client/fournisseur, ajuster une créance ou un règlement",
                    checked = draft.canModifyCreance,
                    onCheckedChange = { draft = draft.copy(canModifyCreance = it) }
                )
                PermissionToggleRow(
                    title = "Droit de Suppression de Créance",
                    subtitle = "Supprimer un règlement ou apurer/remettre à zéro une créance",
                    checked = draft.canDeleteCreance,
                    onCheckedChange = { draft = draft.copy(canDeleteCreance = it) }
                )

                HorizontalDivider()

                PermissionToggleRow(
                    title = "Droit d'Administration des Utilisateurs",
                    subtitle = "Créer des utilisateurs et gérer leurs autorisations d'écriture et de suppression",
                    checked = draft.canManageEmployees,
                    onCheckedChange = { draft = draft.copy(canManageEmployees = it) }
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedEmail = EmployeeEntity.normalizeEmail(draft.email)
                    if (draft.name.isBlank()) {
                        errorText = "Veuillez saisir le nom de l'utilisateur."
                        return@Button
                    }
                    if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
                        errorText = "Veuillez saisir une adresse email valide."
                        return@Button
                    }
                    if (draft.password.length < 3) {
                        errorText = "Le mot de passe doit contenir au moins 3 caractères."
                        return@Button
                    }
                    onSave(draft.copy(email = normalizedEmail))
                }
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
private fun PermissionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchAddThreeEmployeesDialog(
    onDismiss: () -> Unit,
    onSaveThree: (List<EmployeeEntity>) -> Unit
) {
    var user1 by remember {
        mutableStateOf(
            EmployeeEntity.defaultVendeur().copy(
                id = "EMP-${UUID.randomUUID().toString().take(8).uppercase()}",
                name = "Utilisateur 1 (Vendeur)",
                email = "utilisateur1@merelec.dz",
                password = "user123"
            )
        )
    }
    var user2 by remember {
        mutableStateOf(
            EmployeeEntity.defaultVendeur2().copy(
                id = "EMP-${UUID.randomUUID().toString().take(8).uppercase()}",
                name = "Utilisateur 2 (Vendeur)",
                email = "utilisateur2@merelec.dz",
                password = "user234"
            )
        )
    }
    var user3 by remember {
        mutableStateOf(
            EmployeeEntity.defaultCaissier().copy(
                id = "EMP-${UUID.randomUUID().toString().take(8).uppercase()}",
                name = "Utilisateur 3 (Caissier)",
                email = "utilisateur3@merelec.dz",
                password = "user345"
            )
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val drafts = listOf(user1, user2, user3)
    fun updateDraft(index: Int, updated: EmployeeEntity) {
        errorMsg = null
        when (index) {
            0 -> user1 = updated
            1 -> user2 = updated
            2 -> user3 = updated
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("batch_add_three_users_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Ajouter 3 Utilisateurs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personnalisez le nom, l'email, le mot de passe et le rôle de chacun des 3 utilisateurs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                drafts.forEachIndexed { idx, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Compte Utilisateur #${idx + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { updateDraft(idx, item.copy(name = it)) },
                                label = { Text("Nom de l'utilisateur #${idx + 1}") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = item.email,
                                    onValueChange = { updateDraft(idx, item.copy(email = it)) },
                                    label = { Text("Email #${idx + 1}") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedTextField(
                                    value = item.password,
                                    onValueChange = { updateDraft(idx, item.copy(password = it)) },
                                    label = { Text("Mot de passe") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                EmployeeRole.entries.filter { it != EmployeeRole.ADMIN }.forEach { roleOpt ->
                                    FilterChip(
                                        selected = item.role == roleOpt,
                                        onClick = {
                                            updateDraft(idx, EmployeeEntity.applyRolePreset(item, roleOpt))
                                        },
                                        label = { Text(roleOpt.label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prepared = drafts.mapIndexed { idx, emp ->
                        val normEmail = EmployeeEntity.normalizeEmail(emp.email)
                        if (emp.name.isBlank()) {
                            errorMsg = "Veuillez saisir le nom de l'utilisateur #${idx + 1}."
                            return@Button
                        }
                        if (normEmail.isBlank() || !normEmail.contains("@")) {
                            errorMsg = "Veuillez saisir un email valide pour l'utilisateur #${idx + 1}."
                            return@Button
                        }
                        if (emp.password.trim().length < 3) {
                            errorMsg = "Le mot de passe de l'utilisateur #${idx + 1} doit contenir au moins 3 caractères."
                            return@Button
                        }
                        emp.copy(
                            name = emp.name.trim(),
                            email = normEmail,
                            password = emp.password.trim()
                        )
                    }
                    onSaveThree(prepared)
                },
                modifier = Modifier.testTag("confirm_save_three_users_button")
            ) {
                Text("Enregistrer les 3 Utilisateurs")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
