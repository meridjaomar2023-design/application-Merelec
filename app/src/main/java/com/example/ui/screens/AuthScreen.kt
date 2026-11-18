package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeRole
import com.example.data.repository.OperationRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Configuration Google Sign-In manquante : default_web_client_id introuvable")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = try {
                credentialManager.getCredential(activityContext, primaryRequest)
            } catch (firstErr: Exception) {
                if (firstErr is GetCredentialCancellationException) throw firstErr
                val fallbackOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                credentialManager.getCredential(activityContext, fallbackRequest)
            }
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Type d'identifiant Google inattendu")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.w("Auth", "Google Sign-In unavailable on this device: ${e.message}")
            val rawMsg = e.localizedMessage ?: e.message ?: ""
            val friendlyMsg = if (
                rawMsg.contains("no provider dependencies found", ignoreCase = true) ||
                rawMsg.contains("GetCredentialProviderConfigurationException", ignoreCase = true) ||
                rawMsg.contains("provider", ignoreCase = true)
            ) {
                "Connexion directe active : votre base de données fonctionne sur cet appareil sans dépendre de Google Play Services. Connectez-vous avec votre email et mot de passe ci-dessous."
            } else {
                rawMsg.ifBlank { "Synchronisation Google Cloud indisponible sur cet appareil." }
            }
            onAuthError(friendlyMsg)
        }
    }
}

fun signOutFromFirebase(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Failed to clear credential state", e)
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit = {},
    onEmployeeAuthenticated: (EmployeeEntity) -> Unit = {},
    onOpenEmployeeManagementDirectly: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val repository = remember(context) { OperationRepository(context) }
    val rawEmployees by repository.allEmployees.collectAsState(initial = emptyList())
    val activeEmployees = remember(rawEmployees) {
        val list = if (rawEmployees.isNotEmpty()) rawEmployees else listOf(
            EmployeeEntity.defaultAdmin(),
            EmployeeEntity.defaultMerelecBejaia(),
            EmployeeEntity.defaultVendeur(),
            EmployeeEntity.defaultVendeur2(),
            EmployeeEntity.defaultCaissier()
        )
        list.filter { it.isActive }.distinctBy { EmployeeEntity.normalizeEmail(it.email) }
    }

    // Onglet actif : 0 = Connexion Utilisateur (gauche), 1 = Onglet latéral droit (Espace Propriétaire / Administrateur)
    var selectedTab by remember { mutableIntStateOf(0) }
    // Tiroir / Volet latéral droit ("onglet latéral droit")
    var isRightAdminDrawerOpen by remember { mutableStateOf(false) }

    // Champs de connexion Utilisateur (toujours vides au départ)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var cloudUserEmail by remember { mutableStateOf(Firebase.auth.currentUser?.email) }

    // Boîte de dialogue de connexion Administrateur (Email + Mot de passe obligatoires avant d'entrer)
    var showAdminLoginDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.seedInitialSampleDataIfEmpty()
            } catch (_: Exception) {
            }
        }
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    cloudUserEmail = Firebase.auth.currentUser?.email
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            repository.seedInitialSampleDataIfEmpty()
                        } catch (_: Exception) {
                        }
                    }
                },
                onUnauthenticated = {},
                scope = scope
            )
        } else {
            cloudUserEmail = Firebase.auth.currentUser?.email
        }
    }

    fun performEmployeeEmailPasswordLogin() {
        val cleanEmail = EmployeeEntity.normalizeEmail(email)
        val cleanPass = password.trim()
        if (cleanEmail.isEmpty() || cleanPass.isEmpty()) {
            errorMessage = "Veuillez saisir votre adresse email et votre mot de passe."
            return
        }
        errorMessage = null
        infoMessage = null
        isLoading = true

        scope.launch {
            try {
                cloudUserEmail = Firebase.auth.currentUser?.email
                val authRes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.authenticateEmployee(cleanEmail, cleanPass)
                }
                isLoading = false
                authRes.onSuccess { emp ->
                    onEmployeeAuthenticated(emp)
                    onAuthSuccess()
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Identifiants employé refusés."
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.localizedMessage ?: "Erreur lors de la vérification des accès."
            }
        }
    }

    // Connexion Administrateur obligatoire avec Email et Mot de passe avant d'accéder à la gestion des utilisateurs
    fun performAdminLoginForUserManagement(
        adminEmailInput: String,
        adminPasswordInput: String,
        onResultError: (String) -> Unit
    ) {
        val cleanEmail = EmployeeEntity.normalizeEmail(adminEmailInput)
        val cleanPass = adminPasswordInput.trim()
        if (cleanEmail.isEmpty() || cleanPass.isEmpty()) {
            onResultError("Veuillez saisir l'email et le mot de passe Administrateur.")
            return
        }
        isLoading = true

        scope.launch {
            try {
                cloudUserEmail = Firebase.auth.currentUser?.email
                val authRes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.authenticateEmployee(cleanEmail, cleanPass)
                }
                isLoading = false
                authRes.onSuccess { emp ->
                    if (emp.role == EmployeeRole.ADMIN || emp.canManageEmployees) {
                        showAdminLoginDialog = false
                        isRightAdminDrawerOpen = false
                        onEmployeeAuthenticated(emp)
                        onAuthSuccess()
                        onOpenEmployeeManagementDirectly()
                    } else {
                        onResultError("Accès refusé : le compte « ${emp.name} » (${emp.role.label}) n'est pas Administrateur.")
                    }
                }.onFailure { err ->
                    onResultError(err.localizedMessage ?: "Email ou mot de passe Administrateur incorrect.")
                }
            } catch (e: Exception) {
                isLoading = false
                onResultError(e.localizedMessage ?: "Erreur lors de la connexion Administrateur.")
            }
        }
    }

    if (showAdminLoginDialog) {
        AdminLoginRequiredDialog(
            isLoading = isLoading,
            isDefaultAdminActive = !repository.isAdminCustomized(),
            onDismiss = {
                if (!isLoading) {
                    showAdminLoginDialog = false
                }
            },
            onAuthenticateAdmin = { adminEmail, adminPassword, setDialogError ->
                performAdminLoginForUserManagement(adminEmail, adminPassword, setDialogError)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // Contenu central de la page de connexion
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Barre d'onglets en haut : Gauche = Connexion Utilisateur | Droite = Espace Propriétaire / Administrateur
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "Connexion Utilisateur",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_user_login")
                        )

                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "Espace Administrateur",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_right_admin_space")
                        )
                    }

                    if (selectedTab == 0) {
                        // ONGLET GAUCHE : CONNEXION UTILISATEUR (sans Espace Propriétaire/Administrateur en bas)
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
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Connexion Sécurisée",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Connexion Utilisateur",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Saisissez l'adresse email et le mot de passe qui vous ont été attribués par l'administrateur.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sélection rapide des comptes autorisés (remplit uniquement l'email, mot de passe requis)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Comptes configurés (${activeEmployees.size}) — cliquez pour remplir l'email :",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    activeEmployees.chunked(2).forEach { rowAccounts ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowAccounts.forEach { acc ->
                                                val normAccEmail = EmployeeEntity.normalizeEmail(acc.email)
                                                val isSelected = EmployeeEntity.normalizeEmail(email) == normAccEmail
                                                val tag = when (normAccEmail) {
                                                    EmployeeEntity.PRIMARY_ADMIN_EMAIL -> "quick_select_meridjaomar"
                                                    EmployeeEntity.SECONDARY_USER_EMAIL -> "quick_select_merelec_bejaia"
                                                    else -> "quick_select_${acc.id}"
                                                }
                                                Surface(
                                                    onClick = {
                                                        email = normAccEmail
                                                        errorMessage = null
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.surface
                                                    },
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag(tag)
                                                ) {
                                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                                        Text(
                                                            text = acc.name,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = normAccEmail,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                            if (rowAccounts.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                label = { Text("Email de l'utilisateur") },
                                placeholder = { Text("ex: meridjaomar2023@gmail.com ou merelec.bejaia@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                label = { Text("Mot de passe") },
                                placeholder = { Text("Saisissez votre mot de passe secret") },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe"
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errorMessage!!,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            if (infoMessage != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = infoMessage!!,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { performEmployeeEmailPasswordLogin() },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("employee_email_login_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Vérification des identifiants...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Se connecter",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        // ONGLET DROIT : ESPACE PROPRIÉTAIRE / ADMINISTRATEUR
                        OwnerAdminSectionContent(
                            isLoading = isLoading,
                            cloudUserEmail = cloudUserEmail,
                            onDisconnectCloud = {
                                signOutFromFirebase(
                                    context = context,
                                    onSignOutComplete = {
                                        cloudUserEmail = null
                                        infoMessage = "Compte Cloud déconnecté de ce poste."
                                    },
                                    scope = scope
                                )
                            },
                            onConnectCloud = {
                                errorMessage = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        val gEmail = Firebase.auth.currentUser?.email
                                        cloudUserEmail = gEmail
                                        infoMessage = "Synchronisation Google Cloud activée (${gEmail ?: "Actif"}) !"
                                        scope.launch {
                                            try {
                                                repository.seedInitialSampleDataIfEmpty()
                                            } catch (_: Exception) {
                                            }
                                        }
                                    },
                                    onAuthError = { msg ->
                                        infoMessage = msg
                                    },
                                    scope = scope
                                )
                            },
                            onConfigureAdminClick = {
                                showAdminLoginDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Onglet latéral droit flottant (Poignée sur le bord droit de l'écran)
        if (!isRightAdminDrawerOpen) {
            Surface(
                onClick = { isRightAdminDrawerOpen = true },
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(vertical = 16.dp)
                    .testTag("right_lateral_admin_tab_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Ouvrir l'onglet latéral droit Administrateur",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Admin",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Volet / Onglet latéral droit coulissant ("Espace Propriétaire / Administrateur")
        AnimatedVisibility(
            visible = isRightAdminDrawerOpen,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Fond semi-transparent cliquable pour fermer le volet droit
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { isRightAdminDrawerOpen = false }
                )

                // Feuille latérale droite
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .widthIn(min = 290.dp, max = 340.dp)
                        .testTag("right_lateral_admin_drawer"),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Onglet Administrateur",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { isRightAdminDrawerOpen = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer l'onglet latéral droit"
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        OwnerAdminSectionContent(
                            isLoading = isLoading,
                            cloudUserEmail = cloudUserEmail,
                            onDisconnectCloud = {
                                signOutFromFirebase(
                                    context = context,
                                    onSignOutComplete = {
                                        cloudUserEmail = null
                                        infoMessage = "Compte Cloud déconnecté de ce poste."
                                    },
                                    scope = scope
                                )
                            },
                            onConnectCloud = {
                                errorMessage = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        val gEmail = Firebase.auth.currentUser?.email
                                        cloudUserEmail = gEmail
                                        infoMessage = "Synchronisation Google Cloud activée (${gEmail ?: "Actif"}) !"
                                        scope.launch {
                                            try {
                                                repository.seedInitialSampleDataIfEmpty()
                                            } catch (_: Exception) {
                                            }
                                        }
                                    },
                                    onAuthError = { msg ->
                                        infoMessage = msg
                                    },
                                    scope = scope
                                )
                            },
                            onConfigureAdminClick = {
                                showAdminLoginDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerAdminSectionContent(
    isLoading: Boolean,
    cloudUserEmail: String?,
    onDisconnectCloud: () -> Unit,
    onConnectCloud: () -> Unit,
    onConfigureAdminClick: () -> Unit
) {
    val isCloudLinked = cloudUserEmail != null || Firebase.auth.currentUser != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Espace Propriétaire / Administrateur",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cet espace est strictement réservé à l'Administrateur. Vous devez d'abord vous connecter avec votre email et votre mot de passe Administrateur pour pouvoir gérer les utilisateurs ou changer votre mot de passe.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Statut de la Base de Données & Synchronisation Cloud Firestore (réservé à l'Espace Administrateur)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE8F5E9),
            border = BorderStroke(
                1.dp,
                Color(0xFF2E7D32).copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_cloud_firestore_status_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isCloudLinked) {
                                "Base Cloud Firestore connectée (${cloudUserEmail ?: "Actif"})"
                            } else {
                                "Base de données prête & connectée"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = if (isCloudLinked) {
                                "Synchronisation multi-postes active"
                            } else {
                                "Stockage local + synchronisation Firestore automatique"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                if (isCloudLinked) {
                    TextButton(onClick = onDisconnectCloud) {
                        Text("Changer Cloud", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    TextButton(onClick = onConnectCloud) {
                        Text("Lier Cloud", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onConfigureAdminClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("owner_admin_setup_button"),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Configurer l'Administrateur & Gérer les Utilisateurs",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Boîte de dialogue exigeant que l'administrateur se connecte D'ABORD avec son Email et son Mot de passe
 * lorsqu'il clique sur « Configurer l'Administrateur & Gérer les Utilisateurs ».
 * Une fois connecté à l'intérieur, il pourra changer son mot de passe et gérer les comptes utilisateurs.
 */
@Composable
private fun AdminLoginRequiredDialog(
    isLoading: Boolean,
    isDefaultAdminActive: Boolean = false,
    onDismiss: () -> Unit,
    onAuthenticateAdmin: (email: String, password: String, onError: (String) -> Unit) -> Unit
) {
    var adminEmail by remember { mutableStateOf(EmployeeEntity.PRIMARY_ADMIN_EMAIL) }
    var adminPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Connexion Administrateur Requise",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Veuillez d'abord vous connecter avec votre adresse email et votre mot de passe Administrateur. Une fois à l'intérieur, vous pourrez changer votre mot de passe et gérer les utilisateurs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isDefaultAdminActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Compte Administrateur configuré : ${EmployeeEntity.PRIMARY_ADMIN_EMAIL} (mot de passe initial : admin123).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = adminEmail,
                    onValueChange = {
                        adminEmail = it
                        dialogError = null
                    },
                    label = { Text("Email Administrateur") },
                    placeholder = { Text("Saisissez l'email administrateur") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_dialog_email_input")
                )

                OutlinedTextField(
                    value = adminPassword,
                    onValueChange = {
                        adminPassword = it
                        dialogError = null
                    },
                    label = { Text("Mot de passe Administrateur") },
                    placeholder = { Text("Saisissez le mot de passe actuel") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_dialog_password_input")
                )

                if (dialogError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = dialogError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAuthenticateAdmin(adminEmail, adminPassword) { err ->
                        dialogError = err
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.testTag("admin_dialog_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vérification...")
                } else {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Se connecter")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Annuler")
            }
        }
    )
}
