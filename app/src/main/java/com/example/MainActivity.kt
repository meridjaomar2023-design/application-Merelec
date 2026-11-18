package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.EmployeeEntity
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EmployeeManagementScreen
import com.example.ui.screens.GoogleDriveBackupScreen
import com.example.ui.screens.IntelligentOcrScreen
import com.example.ui.screens.MainOperationsScreen
import com.example.ui.screens.OperationDetailScreen
import com.example.ui.screens.OperationFormScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootAuthGate()
                }
            }
        }
    }
}

@Composable
fun RootAuthGate() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var authenticatedEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var openEmployeeManagementOnStart by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    val employee = authenticatedEmployee
    if (employee == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            },
            onEmployeeAuthenticated = { emp ->
                currentUser = Firebase.auth.currentUser
                authenticatedEmployee = emp
            },
            onOpenEmployeeManagementDirectly = {
                openEmployeeManagementOnStart = true
            }
        )
    } else {
        val sessionKey = user?.uid ?: "emp_${employee.id}"
        val operationViewModel: OperationViewModel = viewModel(key = sessionKey)
        LaunchedEffect(employee, openEmployeeManagementOnStart) {
            operationViewModel.setAuthenticatedEmployee(employee)
            if (openEmployeeManagementOnStart) {
                openEmployeeManagementOnStart = false
                operationViewModel.navigateTo(AppScreen.EmployeeManagement)
            }
        }
        AppNavigation(
            viewModel = operationViewModel,
            onLockEmployeeSession = {
                operationViewModel.setAuthenticatedEmployee(null)
                authenticatedEmployee = null
            }
        )
    }
}

@Composable
fun AppNavigation(
    viewModel: OperationViewModel,
    onLockEmployeeSession: () -> Unit = {}
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is AppScreen.MainList -> {
                MainOperationsScreen(
                    viewModel = viewModel,
                    onLockEmployeeSession = onLockEmployeeSession
                )
            }
            is AppScreen.Form -> {
                OperationFormScreen(viewModel = viewModel)
            }
            is AppScreen.Detail -> {
                OperationDetailScreen(
                    operationId = screen.operationId,
                    viewModel = viewModel
                )
            }
            is AppScreen.ImportDirectory -> {
                com.example.ui.screens.DirectoryManagementScreen(viewModel = viewModel, initialTab = 0)
            }
            is AppScreen.Management -> {
                com.example.ui.screens.DirectoryManagementScreen(viewModel = viewModel, initialTab = screen.initialTab)
            }
            is AppScreen.PartnerDetail -> {
                com.example.ui.screens.PartnerDetailScreen(
                    partnerId = screen.partnerId,
                    viewModel = viewModel
                )
            }
            is AppScreen.BackupDrive -> {
                GoogleDriveBackupScreen(viewModel = viewModel)
            }
            is AppScreen.Dashboard -> {
                com.example.ui.screens.DashboardScreen(viewModel = viewModel)
            }
            is AppScreen.EmployeeManagement -> {
                EmployeeManagementScreen(
                    viewModel = viewModel,
                    onSwitchEmployee = onLockEmployeeSession
                )
            }
            is AppScreen.PdfStudio -> {
                com.example.ui.screens.PdfStudioScreen(
                    viewModel = viewModel,
                    initialTab = screen.initialTab
                )
            }
            is AppScreen.IntelligentOcr -> {
                IntelligentOcrScreen(
                    viewModel = viewModel,
                    initialTargetType = screen.initialTargetType
                )
            }
        }
    }
}
