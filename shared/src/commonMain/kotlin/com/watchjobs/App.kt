package com.watchjobs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchjobs.data.remote.FakeBackendTransport
import com.watchjobs.ui.application.ApplicationFormScreen
import com.watchjobs.ui.application.ApplicationListScreen
import com.watchjobs.ui.auth.DeleteAccountDialog
import com.watchjobs.ui.auth.ForgotPasswordScreen
import com.watchjobs.ui.auth.LoginScreen
import com.watchjobs.ui.auth.SignupScreen
import com.watchjobs.ui.components.MainScaffold
import com.watchjobs.ui.components.MainTab
import com.watchjobs.ui.jobs.JobSearchScreen
import com.watchjobs.ui.savedjob.SavedJobFormScreen
import com.watchjobs.ui.savedjob.SavedJobListScreen
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.ui.theme.WatchJobsTheme
import com.watchjobs.viewmodel.DeleteAccountViewModel
import com.watchjobs.viewmodel.ForgotPasswordViewModel
import com.watchjobs.viewmodel.ApplicationFormViewModel
import com.watchjobs.viewmodel.ApplicationListViewModel
import com.watchjobs.viewmodel.JobSearchViewModel
import com.watchjobs.viewmodel.LoginViewModel
import com.watchjobs.viewmodel.SavedJobFormViewModel
import com.watchjobs.viewmodel.SavedJobListViewModel
import com.watchjobs.viewmodel.SignupViewModel
import kotlinx.coroutines.launch

// Navegação por estado simples: a tela atual fica numa variável e um "when" decide o que mostrar.
sealed interface Screen {
    data class Login(val email: String = "") : Screen
    data class ForgotPassword(val email: String = "") : Screen
    data object Signup : Screen
    data object SavedJobs : Screen
    data class SavedJobForm(val id: Long? = null) : Screen
    data object Applications : Screen
    data class ApplicationForm(val id: Int? = null, val savedJobId: Long? = null) : Screen
    data object JobSearch : Screen
}

@Composable
fun App(container: AppContainer = remember { AppContainer() }) {
    WatchJobsTheme {
        var screen by remember {
            mutableStateOf<Screen>(if (container.authRepository.isLoggedIn()) Screen.SavedJobs else Screen.Login())
        }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val navigate: (Screen, String?) -> Unit = { target, message ->
            screen = target
            if (message != null) {
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }
            }
        }

        // Um 401 numa rota protegida apaga o token; aqui o app volta para o login.
        LaunchedEffect(Unit) {
            container.api.sessionExpired.collect { navigate(Screen.Login(), "Sua sessão expirou. Entre novamente.") }
        }

        Box(Modifier.fillMaxSize().background(WatchJobsColors.Background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(Modifier.fillMaxSize()) {
                if (container.demoMode) DemoBanner()
                ScreenHost(screen) { current ->
                    AppScreen(current, container, navigate, scope)
                }
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(20.dp)) { data ->
                Snackbar(data, shape = RectangleShape, containerColor = WatchJobsColors.Ink, contentColor = WatchJobsColors.White)
            }
        }
    }
}

@Composable
private fun AppScreen(
    current: Screen,
    container: AppContainer,
    navigate: (Screen, String?) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var showDeleteAccount by remember { mutableStateOf(false) }
    when (current) {
        is Screen.Login -> LoginScreen(
            viewModel = viewModel { LoginViewModel(container.authRepository, current.email) },
            onLoggedIn = { navigate(Screen.SavedJobs, null) },
            onCreateAccount = { navigate(Screen.Signup, null) },
            onForgotPassword = { navigate(Screen.ForgotPassword(current.email), null) }
        )
        is Screen.ForgotPassword -> ForgotPasswordScreen(
            viewModel = viewModel { ForgotPasswordViewModel(container.authRepository, current.email) },
            onDone = { email -> navigate(Screen.Login(email), "Senha redefinida. Entre com a nova senha.") },
            onBackToLogin = { navigate(Screen.Login(current.email), null) }
        )
        Screen.Signup -> SignupScreen(
            viewModel = viewModel { SignupViewModel(container.authRepository) },
            onSignedUp = { email -> navigate(Screen.Login(email), "Conta criada. Entre com a sua senha.") },
            onBackToLogin = { navigate(Screen.Login(), null) }
        )
        else -> MainScaffold(
            initials = container.authRepository.currentUser()?.initials ?: "?",
            currentTab = when (current) {
                is Screen.Applications, is Screen.ApplicationForm -> MainTab.APPLICATIONS
                is Screen.JobSearch -> MainTab.JOB_SEARCH
                else -> MainTab.SAVED_JOBS
            },
            onTabSelected = { tab ->
                val target = when (tab) {
                    MainTab.JOB_SEARCH -> Screen.JobSearch
                    MainTab.SAVED_JOBS -> Screen.SavedJobs
                    MainTab.APPLICATIONS -> Screen.Applications
                }
                navigate(target, null)
            },
            onLogout = {
                scope.launch {
                    container.authRepository.logout()
                    navigate(Screen.Login(), "Você saiu da conta.")
                }
            },
            onDeleteAccount = { showDeleteAccount = true }
        ) {
            val backToJobs: (String) -> Unit = { message -> navigate(Screen.SavedJobs, message) }
            val backToApplications: (String) -> Unit = { message -> navigate(Screen.Applications, message) }
            when (current) {
                Screen.SavedJobs -> SavedJobListScreen(
                    viewModel = viewModel { SavedJobListViewModel(container.savedJobRepository, container.applicationRepository) },
                    onNew = { navigate(Screen.SavedJobForm(), null) },
                    onEdit = { id -> navigate(Screen.SavedJobForm(id), null) },
                    onApply = { id -> navigate(Screen.ApplicationForm(savedJobId = id), null) },
                    showMessage = { message -> navigate(Screen.SavedJobs, message) }
                )
                is Screen.SavedJobForm -> SavedJobFormScreen(
                    viewModel = viewModel { SavedJobFormViewModel(current.id, container.savedJobRepository, container.applicationRepository) },
                    onSaved = backToJobs,
                    onCancel = { navigate(Screen.SavedJobs, null) }
                )
                Screen.Applications -> ApplicationListScreen(
                    viewModel = viewModel { ApplicationListViewModel(container.applicationRepository) },
                    onNew = { navigate(Screen.ApplicationForm(), null) },
                    onEdit = { id -> navigate(Screen.ApplicationForm(id), null) },
                    showMessage = { message -> navigate(Screen.Applications, message) }
                )
                is Screen.ApplicationForm -> ApplicationFormScreen(
                    viewModel = viewModel {
                        ApplicationFormViewModel(current.id, current.savedJobId, container.applicationRepository, container.savedJobRepository)
                    },
                    onSaved = backToApplications,
                    onCancel = { navigate(Screen.Applications, null) },
                    onNewSavedJob = { navigate(Screen.SavedJobForm(), null) }
                )
                Screen.JobSearch -> JobSearchScreen(
                    viewModel = viewModel { JobSearchViewModel(container.jobSearchRepository) },
                    showMessage = { message -> navigate(Screen.JobSearch, message) }
                )
                else -> Unit
            }
        }
    }
    if (showDeleteAccount) {
        DeleteAccountDialog(
            viewModel = viewModel { DeleteAccountViewModel(container.authRepository) },
            onDeleted = {
                showDeleteAccount = false
                navigate(Screen.Login(), "Conta excluída.")
            },
            onDismiss = { showDeleteAccount = false }
        )
    }
}

// Cada tela ganha o seu próprio ViewModelStore, que é limpo ao sair dela (os ViewModels são descartados).
@Composable
private fun ScreenHost(screen: Screen, content: @Composable (Screen) -> Unit) {
    val owner = remember(screen) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        key(screen) { content(screen) }
    }
}

// Faixa do modo demonstração: deixa claro que os dados são simulados e mostra o acesso pronto.
@Composable
private fun DemoBanner() {
    Box(
        Modifier.fillMaxWidth().background(WatchJobsColors.Red).padding(horizontal = 16.dp, vertical = 6.dp).testTag("demoBanner"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "MODO DEMONSTRAÇÃO · sem backend, dados em memória · entre com ${FakeBackendTransport.DEMO_EMAIL} / ${FakeBackendTransport.DEMO_PASSWORD}",
            style = MaterialTheme.typography.labelMedium,
            color = WatchJobsColors.White
        )
    }
}
