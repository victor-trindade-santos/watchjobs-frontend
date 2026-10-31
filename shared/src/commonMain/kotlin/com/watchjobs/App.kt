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
import androidx.compose.runtime.collectAsState
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
import com.watchjobs.data.repository.FeedQuery
import com.watchjobs.data.repository.JobSearchQuery
import com.watchjobs.ui.application.ApplicationFormScreen
import com.watchjobs.ui.application.ApplicationListScreen
import com.watchjobs.ui.auth.DeleteAccountDialog
import com.watchjobs.ui.auth.ForgotPasswordScreen
import com.watchjobs.ui.auth.LoginScreen
import com.watchjobs.ui.auth.SignupScreen
import com.watchjobs.ui.components.MainScaffold
import com.watchjobs.ui.components.MainTab
import com.watchjobs.ui.components.UserMenuAction
import com.watchjobs.ui.jobs.JobDetailScreen
import com.watchjobs.ui.jobs.JobSearchScreen
import com.watchjobs.ui.savedjob.SavedJobFormScreen
import com.watchjobs.ui.savedjob.SavedJobListScreen
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.ui.theme.WatchJobsTheme
import com.watchjobs.viewmodel.DeleteAccountViewModel
import com.watchjobs.viewmodel.ForgotPasswordViewModel
import com.watchjobs.viewmodel.ApplicationFormViewModel
import com.watchjobs.viewmodel.ApplicationListViewModel
import com.watchjobs.viewmodel.JobDetailViewModel
import com.watchjobs.viewmodel.JobSearchViewModel
import com.watchjobs.viewmodel.LoginViewModel
import com.watchjobs.viewmodel.SavedJobFormViewModel
import com.watchjobs.viewmodel.SavedJobListViewModel
import com.watchjobs.viewmodel.SignupViewModel
import com.watchjobs.ui.board.BoardScreen
import com.watchjobs.ui.analytics.AnalyticsScreen
import com.watchjobs.viewmodel.AnalyticsViewModel
import com.watchjobs.ui.documents.DocumentsScreen
import com.watchjobs.viewmodel.DocumentsViewModel
import com.watchjobs.ui.feed.DiscoverScreen
import com.watchjobs.ui.feed.FeedScreen
import com.watchjobs.ui.notification.NotificationMenu
import com.watchjobs.ui.notification.NotificationsScreen
import com.watchjobs.model.AppNotification
import com.watchjobs.push.requestPushToken
import com.watchjobs.viewmodel.NotificationCenter
import com.watchjobs.viewmodel.NotificationCenterState
import com.watchjobs.ui.profile.LinkedInImportScreen
import com.watchjobs.ui.profile.PreferencesScreen
import com.watchjobs.ui.profile.ProfileScreen
import com.watchjobs.ui.onboarding.ProvideTour
import com.watchjobs.ui.onboarding.TourOverlay
import com.watchjobs.ui.onboarding.TourRegistry
import com.watchjobs.ui.onboarding.defaultTourSteps
import com.watchjobs.ui.profile.ProfileSetupScreen
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.viewmodel.ProfileSetupViewModel
import com.watchjobs.viewmodel.BoardViewModel
import com.watchjobs.viewmodel.DiscoverViewModel
import com.watchjobs.viewmodel.FeedViewModel
import com.watchjobs.viewmodel.LinkedInImportViewModel
import com.watchjobs.viewmodel.NotificationsViewModel
import com.watchjobs.viewmodel.PreferencesViewModel
import com.watchjobs.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay
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
    data class JobDetail(val id: Long) : Screen
    data class Discover(val id: Long? = null) : Screen
    data object Notifications : Screen
    data object Profile : Screen
    data class Preferences(val autoFill: Boolean = false) : Screen
    data object LinkedInImport : Screen
    data object Board : Screen
    data object Analytics : Screen
    data class Documents(val savedJobId: Long? = null) : Screen
    data object Starting : Screen
    data object ProfileSetup : Screen
}

// Tela inicial após o login: as vagas por match (no modo demonstração, só existe o escopo da prova).
private fun AppContainer.home(): Screen = if (demoMode) Screen.SavedJobs else Screen.JobSearch

// Depois de entrar: o assistente de perfil enquanto o backend não marcar a configuração como concluída.
private suspend fun AppContainer.entryScreen(): Screen {
    if (demoMode) return home()
    val status = runCatching { profileRepository.setupStatus() }.getOrNull()
    return if (status != null && !status.completed) Screen.ProfileSetup else home()
}

@Composable
fun App(container: AppContainer = remember { AppContainer() }) {
    WatchJobsTheme {
        var screen by remember {
            mutableStateOf<Screen>(if (container.authRepository.isLoggedIn()) Screen.Starting else Screen.Login())
        }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val center = remember { NotificationCenter(container.notificationRepository, scope) }
        val centerState by center.state.collectAsState()
        val tour = remember { TourRegistry() }
        var tourVisible by remember { mutableStateOf(false) }
        var tourChecked by remember { mutableStateOf(false) }
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

        // 403 PROFILE_SETUP_REQUIRED em qualquer rota: volta para o assistente de perfil.
        LaunchedEffect(Unit) {
            container.api.profileSetupRequired.collect {
                if (screen != Screen.ProfileSetup) navigate(Screen.ProfileSetup, "Conclua a configuração do perfil para continuar.")
            }
        }

        // Sininho: consulta os avisos não lidos a cada minuto enquanto o usuário está logado.
        val loggedIn = screen !is Screen.Login && screen !is Screen.Signup && screen !is Screen.ForgotPassword
        val refreshUnread: () -> Unit = { center.refresh() }
        LaunchedEffect(loggedIn) {
            if (!loggedIn || container.demoMode) {
                center.clear()
                if (!loggedIn) {
                    tourChecked = false
                    tourVisible = false
                }
                return@LaunchedEffect
            }
            // Android com Firebase configurado: registra o aparelho para receber push.
            requestPushToken { token ->
                container.pushToken = token
                scope.launch { runCatching { container.notificationRepository.registerDevice(token) } }
            }
            while (true) {
                center.refreshCount()
                delay(60_000)
            }
        }

        // Tour guiado (F06): aparece sozinho uma vez, depois do assistente de perfil, sobre a página inicial.
        val inApp = loggedIn && screen != Screen.Starting && screen != Screen.ProfileSetup
        val startTour: () -> Unit = {
            if (screen != container.home()) navigate(container.home(), null)
            tourVisible = true
        }
        LaunchedEffect(inApp) {
            if (!inApp || container.demoMode || tourChecked) return@LaunchedEffect
            tourChecked = true
            val status = runCatching { container.profileRepository.onboarding() }.getOrNull()
            if (status != null && !status.completed) startTour()
        }

        Box(Modifier.fillMaxSize().background(WatchJobsColors.Background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            ProvideTour(tour) {
                Column(Modifier.fillMaxSize()) {
                    if (container.demoMode) DemoBanner()
                    ScreenHost(screen) { current ->
                        AppScreen(current, container, navigate, scope, centerState.unread, refreshUnread, center, centerState, startTour)
                    }
                }
            }
            if (tourVisible && inApp) {
                TourOverlay(tour, defaultTourSteps) { _ ->
                    tourVisible = false
                    scope.launch { runCatching { container.profileRepository.completeOnboarding() } }
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
    scope: kotlinx.coroutines.CoroutineScope,
    unread: Long,
    refreshUnread: () -> Unit,
    center: NotificationCenter,
    centerState: NotificationCenterState,
    startTour: () -> Unit
) {
    var showDeleteAccount by remember { mutableStateOf(false) }
    val demo = container.demoMode
    val applicationsHome: Screen = Screen.Board
    // Abre o contexto do aviso: busca concluída, vagas novas, candidatura ou vaga salva.
    val openNotification: (AppNotification) -> Unit = { notification ->
        center.markRead(notification.id)
        val searchId = notification.searchRequestId
        val savedJobId = notification.savedJobId
        val target = when {
            notification.type == "SEARCH_COMPLETED" && searchId != null -> Screen.Discover(searchId)
            notification.type == "SAVED_JOB_ALERT" && savedJobId != null -> Screen.SavedJobForm(savedJobId)
            notification.type == "INTERVIEW_REMINDER" || notification.type == "STALE_APPLICATION" -> Screen.Board
            else -> Screen.JobSearch
        }
        navigate(target, null)
    }
    val logout: () -> Unit = {
        scope.launch {
            container.pushToken?.let { token -> runCatching { container.notificationRepository.unregisterDevice(token) } }
            container.pushToken = null
            container.authRepository.logout()
            container.jobSearchQuery = JobSearchQuery()
            container.feedQuery = FeedQuery()
            navigate(Screen.Login(), "Você saiu da conta.")
        }
    }
    when (current) {
        is Screen.Login -> LoginScreen(
            viewModel = viewModel { LoginViewModel(container.authRepository, current.email) },
            onLoggedIn = { scope.launch { navigate(container.entryScreen(), null) } },
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
            onSignedUp = { email, loggedIn ->
                if (loggedIn) {
                    scope.launch { navigate(container.entryScreen(), "Conta criada. Bem-vindo ao WatchJobs!") }
                } else {
                    navigate(Screen.Login(email), "Conta criada. Entre com a sua senha.")
                }
            },
            onBackToLogin = { navigate(Screen.Login(), null) }
        )
        Screen.Starting -> {
            LaunchedEffect(Unit) { navigate(container.entryScreen(), null) }
            LoadingBox(Modifier.fillMaxSize())
        }
        Screen.ProfileSetup -> ProfileSetupScreen(
            viewModel = viewModel { ProfileSetupViewModel(container.profileRepository) },
            profileViewModel = viewModel { ProfileViewModel(container.profileRepository, container.catalogRepository) },
            importViewModel = viewModel { LinkedInImportViewModel(container.profileRepository) },
            preferencesViewModel = viewModel { PreferencesViewModel(container.profileRepository, container.catalogRepository, autoFill = true) },
            onCompleted = { navigate(container.home(), "Perfil configurado. Tudo pronto!") },
            onLogout = logout,
            showMessage = { message -> scope.launch { navigate(Screen.ProfileSetup, message) } }
        )
        else -> MainScaffold(
            initials = container.authRepository.currentUser()?.initials ?: "?",
            currentTab = when (current) {
                is Screen.Applications, is Screen.ApplicationForm, is Screen.Board -> MainTab.APPLICATIONS
                is Screen.JobSearch, is Screen.JobDetail, is Screen.Discover, is Screen.Notifications -> MainTab.JOB_SEARCH
                is Screen.Profile, is Screen.Preferences, is Screen.LinkedInImport, is Screen.Documents -> MainTab.PROFILE
                is Screen.Analytics -> MainTab.ANALYTICS
                else -> MainTab.SAVED_JOBS
            },
            onTabSelected = { tab ->
                val target = when (tab) {
                    MainTab.JOB_SEARCH -> Screen.JobSearch
                    MainTab.SAVED_JOBS -> Screen.SavedJobs
                    MainTab.APPLICATIONS -> applicationsHome
                    MainTab.ANALYTICS -> Screen.Analytics
                    MainTab.PROFILE -> Screen.Profile
                }
                navigate(target, null)
            },
            tabs = if (demo) listOf(MainTab.JOB_SEARCH, MainTab.SAVED_JOBS, MainTab.APPLICATIONS) else MainTab.entries,
            unreadCount = if (demo) null else unread,
            onToggleTheme = { container.themePreference.toggle() },
            onNotifications = { center.refresh() },
            notifications = { close ->
                NotificationMenu(
                    state = centerState,
                    onOpen = { notification -> close(); openNotification(notification) },
                    onMarkAllRead = center::markAllRead,
                    onSeeMore = { close(); navigate(Screen.Notifications, null) }
                )
            },
            // No modo demonstração não há perfil: a exclusão da conta fica no menu do usuário.
            userMenu = if (demo) listOf(UserMenuAction.DELETE_ACCOUNT, UserMenuAction.LOGOUT) else UserMenuAction.standard,
            onUserMenu = { action ->
                when (action) {
                    UserMenuAction.PROFILE -> navigate(Screen.Profile, null)
                    UserMenuAction.PREFERENCES -> navigate(Screen.Preferences(), null)
                    UserMenuAction.LINKEDIN_IMPORT -> navigate(Screen.LinkedInImport, null)
                    UserMenuAction.DOCUMENTS -> navigate(Screen.Documents(), null)
                    UserMenuAction.TOUR -> startTour()
                    UserMenuAction.DELETE_ACCOUNT -> { showDeleteAccount = true }
                    UserMenuAction.LOGOUT -> logout()
                }
            }
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
                    onCancel = { navigate(Screen.SavedJobs, null) },
                    onDocuments = if (demo) null else ({ current.id?.let { navigate(Screen.Documents(it), null) } })
                )
                Screen.Applications -> ApplicationListScreen(
                    viewModel = viewModel { ApplicationListViewModel(container.applicationRepository, container.savedJobRepository) },
                    onNew = { navigate(Screen.ApplicationForm(), null) },
                    onEdit = { id -> navigate(Screen.ApplicationForm(id), null) },
                    onOpenSavedJob = { id -> navigate(Screen.SavedJobForm(id), null) },
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
                Screen.JobSearch -> if (demo) {
                    JobSearchScreen(
                        viewModel = viewModel {
                            JobSearchViewModel(container.jobSearchRepository, container.jobSearchQuery) { container.jobSearchQuery = it }
                        },
                        onView = { id -> navigate(Screen.JobDetail(id), null) },
                        showMessage = { message -> navigate(Screen.JobSearch, message) }
                    )
                } else {
                    FeedScreen(
                        viewModel = viewModel {
                            FeedViewModel(container.feedRepository, container.jobSearchRepository, container.feedQuery) { container.feedQuery = it }
                        },
                        onView = { id -> navigate(Screen.JobDetail(id), null) },
                        onDiscover = { navigate(Screen.Discover(), null) },
                        onPreferences = { navigate(Screen.Preferences(), null) },
                        showMessage = { message -> navigate(Screen.JobSearch, message) }
                    )
                }
                is Screen.JobDetail -> JobDetailScreen(
                    viewModel = viewModel {
                        JobDetailViewModel(current.id, container.jobSearchRepository, if (demo) null else container.feedRepository)
                    },
                    onBack = { navigate(Screen.JobSearch, null) },
                    onOpenSaved = { savedId -> navigate(savedId?.let { Screen.SavedJobForm(it) } ?: Screen.SavedJobs, null) },
                    showMessage = { message -> navigate(current, message) },
                    onInteracted = { message -> navigate(Screen.JobSearch, message) }
                )
                is Screen.Discover -> DiscoverScreen(
                    viewModel = viewModel { DiscoverViewModel(container.feedRepository, container.profileRepository, current.id) },
                    onView = { id -> navigate(Screen.JobDetail(id), null) },
                    onBack = { navigate(Screen.JobSearch, null) }
                )
                Screen.Notifications -> NotificationsScreen(
                    viewModel = viewModel { NotificationsViewModel(container.notificationRepository) },
                    onOpen = openNotification,
                    onCountChanged = refreshUnread,
                    showMessage = { message -> navigate(Screen.Notifications, message) }
                )
                Screen.Profile -> ProfileScreen(
                    viewModel = viewModel { ProfileViewModel(container.profileRepository, container.catalogRepository) },
                    onPreferences = { navigate(Screen.Preferences(), null) },
                    onImport = { navigate(Screen.LinkedInImport, null) },
                    onSearchJobs = { navigate(Screen.JobSearch, null) },
                    onDeleteAccount = { showDeleteAccount = true },
                    showMessage = { message -> navigate(Screen.Profile, message) }
                )
                is Screen.Preferences -> PreferencesScreen(
                    viewModel = viewModel { PreferencesViewModel(container.profileRepository, container.catalogRepository, current.autoFill) },
                    onBack = { navigate(Screen.Profile, null) },
                    onSaved = { message -> navigate(Screen.JobSearch, message) },
                    showMessage = { message -> navigate(current, message) }
                )
                Screen.LinkedInImport -> LinkedInImportScreen(
                    viewModel = viewModel { LinkedInImportViewModel(container.profileRepository) },
                    onBack = { navigate(Screen.Profile, null) },
                    onApplied = { message, reviewPreferences ->
                        navigate(if (reviewPreferences) Screen.Preferences(autoFill = true) else Screen.Profile, message)
                    }
                )
                Screen.Board -> BoardScreen(
                    viewModel = viewModel { BoardViewModel(container.boardRepository, container.boardViewPreference) },
                    onOpenSavedJob = { id -> navigate(Screen.SavedJobForm(id), null) },
                    onNewSavedJob = { navigate(Screen.SavedJobForm(), null) },
                    onOpenList = { navigate(Screen.Applications, null) },
                    showMessage = { message -> navigate(Screen.Board, message) }
                )
                Screen.Analytics -> AnalyticsScreen(
                    viewModel = viewModel { AnalyticsViewModel(container.analyticsRepository) },
                    onOpenBoard = { navigate(Screen.Board, null) },
                    onOpenSavedJob = { id -> navigate(Screen.SavedJobForm(id), null) }
                )
                is Screen.Documents -> DocumentsScreen(
                    viewModel = viewModel { DocumentsViewModel(container.documentRepository, container.savedJobRepository, current.savedJobId) },
                    onOpenProfile = { navigate(Screen.Profile, null) },
                    showMessage = { message -> navigate(current, message) }
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
            color = WatchJobsColors.OnAccent
        )
    }
}
