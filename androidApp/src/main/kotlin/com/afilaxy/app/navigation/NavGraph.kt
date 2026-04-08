package com.afilaxy.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.afilaxy.util.FileLogger
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.afilaxy.app.ui.screens.*
import com.afilaxy.app.ui.scaffold.AfilaxyAppScaffoldSimple

import com.afilaxy.domain.repository.PreferencesRepository
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.presentation.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val protectedRoutes = setOf(
    AppRoutes.HOME, AppRoutes.CONSENT, AppRoutes.EMERGENCY, AppRoutes.PROFILE, AppRoutes.HISTORY,
    AppRoutes.SETTINGS, AppRoutes.AUTOCUIDADO, AppRoutes.PROFESSIONALS,
    AppRoutes.CRM_LOOKUP, AppRoutes.MAP, AppRoutes.MAP_PHARMACY, AppRoutes.NOTIFICATIONS, AppRoutes.HELP, AppRoutes.ABOUT,
    AppRoutes.TERMS, AppRoutes.PRIVACY, AppRoutes.PORTAL, "emergency_request", "emergency_response", AppRoutes.CHAT
)

// Rotas que usam o AfilaxyAppScaffold (tabs principais)
private val tabRoutes = setOf(
    AppRoutes.HOME, AppRoutes.MAP, AppRoutes.PROFILE, AppRoutes.PORTAL
)

@Composable
fun NavGraph(
    startDestination: String? = null,
    emergencyViewModel: EmergencyViewModel? = null,
    pendingDestination: MutableState<String?>? = null
) {
    val navController = rememberNavController()
    val prefs: PreferencesRepository = koinInject()

    // Destino pós-autenticação: consentimento se ainda não apresentado, home caso contrário
    fun postAuthDestination() =
        if (prefs.getBoolean("consent_shown", false)) AppRoutes.HOME else AppRoutes.CONSENT

    // Se o app abre com usuário já logado mas sem consentimento gravado, redireciona
    val effectiveStart = remember(startDestination) {
        if (startDestination == AppRoutes.HOME && !prefs.getBoolean("consent_shown", false))
            AppRoutes.CONSENT
        else startDestination
    }

    // Redirect unauthenticated deep-links to login
    LaunchedEffect(startDestination) {
        if (startDestination != null && FirebaseAuth.getInstance().currentUser == null) {
            navController.navigate(AppRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    // IDs de emergencias cujo chat já foi aberto nesta sessão.
    // Impede que FCMs type=chat tardios (chegam durante ou após a transição
    // EmergencyResponseScreen → ChatScreen) empurrem uma segunda instância
    // do ChatScreen sobre a primeira, gerando ChatScreen opened 2-3x por emergência.
    // Análogo ao resolvedEmergencyIds do iOS ContentView.
    val chatNavigatedIds = remember { mutableStateOf(setOf<String>()) }

    // Registra automaticamente qualquer entidade em chat/{id},
    // qualquer que seja o caminho (FCM intent OU navegação direta pelo accept).
    DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, arguments ->
            val route = destination.route ?: return@OnDestinationChangedListener
            if (route.startsWith("chat/")) {
                val chatId = arguments?.getString("emergencyId") ?: return@OnDestinationChangedListener
                chatNavigatedIds.value = chatNavigatedIds.value + chatId
                FileLogger.log("DEBUG", "NavGraph", "chatNavigatedIds registrado emergencyId=$chatId")
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    // Navega quando onNewIntent entrega destino com app já aberto (notificação push).
    LaunchedEffect(pendingDestination?.value) {
        val dest = pendingDestination?.value ?: return@LaunchedEffect
        if (navController.currentDestination == null) return@LaunchedEffect
        val currentRoute = navController.currentDestination?.route

        // Extrai emergencyId do destino chat/
        val destChatId = if (dest.startsWith("chat/")) dest.removePrefix("chat/") else null

        // Bloqueia se já navegamos ao chat para este emergencyId — independente
        // da rota atual (pode ainda estar em emergency_response durante a transição).
        if (destChatId != null && destChatId in chatNavigatedIds.value) {
            FileLogger.log("DEBUG", "NavGraph",
                "pendingDestination ignorado — chat já aberto emergencyId=$destChatId")
            pendingDestination.value = null
            return@LaunchedEffect
        }

        val alreadyThere = when {
            dest.startsWith("chat/") && currentRoute?.startsWith("chat/") == true ->
                currentRoute == dest
            else -> currentRoute == dest
        }
        if (alreadyThere) {
            pendingDestination.value = null
            return@LaunchedEffect
        }

        // Guard: notificação FCM de emergency_request quando o chat já está ativo.
        // Sem este guard, popUpTo(HOME) eliminaria o CHAT da pilha, e o popBackStack()
        // do EmergencyRequestScreen enviaria o usuário à HOME em vez de voltar ao chat.
        val destRequestId = if (dest.startsWith("emergency_request/"))
            dest.removePrefix("emergency_request/") else null
        if (destRequestId != null && destRequestId in chatNavigatedIds.value) {
            FileLogger.log("DEBUG", "NavGraph",
                "pendingDestination: chat já aberto — redirecionando ao chat emergencyId=$destRequestId")
            // Navega para o chat existente sem popUpTo para preservar a pilha
            navController.navigate("chat/$destRequestId") { launchSingleTop = true }
            pendingDestination.value = null
            return@LaunchedEffect
        }

        // Também protege se a pilha já contém chat/ para este ID (backStack check)
        val backStack = navController.currentBackStack.value
        if (destRequestId != null && backStack.any { (it.destination.route ?: "").startsWith("chat/") }) {
            FileLogger.log("DEBUG", "NavGraph",
                "pendingDestination: backStack contém chat — redirecionando emergencyId=$destRequestId")
            navController.navigate("chat/$destRequestId") { launchSingleTop = true }
            pendingDestination.value = null
            return@LaunchedEffect
        }

        navController.navigate(dest) {
            launchSingleTop = true
            popUpTo(AppRoutes.HOME) { saveState = false }
        }
        pendingDestination.value = null
    }

    // Navega para EmergencyResponseScreen quando uma emergência chega via
    // Firestore listener em foreground (sem depender de toque na notificação push).
    // Guard 1: não navega se já estiver em emergency_response ou chat.
    // Guard 2: não navega se o emergencyId já foi para o chat nesta sessão
    //          (notificação FCM tardia de uma emergência já resolvida).
    val incomingEmergencies = emergencyViewModel?.state?.collectAsState()?.value?.incomingEmergencies ?: emptyList()
    LaunchedEffect(incomingEmergencies) {
        val incoming = incomingEmergencies.firstOrNull() ?: return@LaunchedEffect
        if (navController.currentDestination == null) return@LaunchedEffect
        val currentRoute = navController.currentDestination?.route ?: ""
        if (currentRoute.startsWith("emergency_response") || currentRoute.startsWith("chat")) return@LaunchedEffect
        // Guard: emergência já resolvida nesta sessão — notificação FCM tardia
        if (incoming.id in chatNavigatedIds.value) {
            FileLogger.log("DEBUG", "NavGraph",
                "incomingEmergency ignorado — emergência já resolvida emergencyId=${incoming.id}")
            return@LaunchedEffect
        }
        navController.navigate("emergency_response/${incoming.id}") {
            launchSingleTop = true
        }
    }


    NavHost(
        navController = navController,
        startDestination = effectiveStart ?: AppRoutes.LOGIN
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    val dest = postAuthDestination()
                    navController.navigate(dest) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(AppRoutes.REGISTER) }
            )
        }

        composable(AppRoutes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("email_verification") {
                        popUpTo(AppRoutes.REGISTER) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("email_verification") {
            EmailVerificationScreen(
                onVerified = {
                    val dest = postAuthDestination()
                    navController.navigate(dest) {
                        popUpTo("email_verification") { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Consentimento: exibido uma única vez ──────────────────────────────
        composable(AppRoutes.CONSENT) {
            ConsentScreen(
                onConsentGiven = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.CONSENT) { inclusive = true }
                    }
                }
            )
        }
        
        // Tabs principais com AfilaxyAppScaffoldSimple
        composable(AppRoutes.HOME) {
            AfilaxyAppScaffoldSimple(navController = navController) {
                HomeScreenNew(
                    onNavigateToEmergency = { navController.navigate(AppRoutes.EMERGENCY) },
                    onNavigateToHistory = { navController.navigate(AppRoutes.HISTORY) },
                    onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) },

                    onNavigateToAutocuidado = { navController.navigate(AppRoutes.AUTOCUIDADO) },
                    onNavigateToProfessionals = { navController.navigate(AppRoutes.PROFESSIONALS) },
                    onNavigateToEducation = { navController.navigate("education") },
                    onNavigateToHelp = { navController.navigate(AppRoutes.HELP) },
                    onNavigateToPharmacyMap = { navController.navigate(AppRoutes.MAP_PHARMACY) },
                    onLogout = {
                        navController.navigate(AppRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
        
        composable(AppRoutes.MAP) {
            AfilaxyAppScaffoldSimple(navController = navController) {
                MapScreen(navController = navController)
            }
        }

        composable(AppRoutes.MAP_PHARMACY) {
            MapScreen(navController = navController, pharmacyMode = true)
        }

        composable(AppRoutes.PROFILE) {
            AfilaxyAppScaffoldSimple(navController = navController) {
                ProfileScreenNew(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(AppRoutes.HISTORY) },
                    onNavigateToPrivacy = { navController.navigate(AppRoutes.PRIVACY) },
                    onNavigateToHelp = { navController.navigate(AppRoutes.HELP) }
                )
            }
        }
        
        // Tab Portal - Role-based routing
        composable(AppRoutes.PORTAL) {
            AfilaxyAppScaffoldSimple(navController = navController) {
                val profileViewModel: ProfileViewModel = koinViewModel()
                val profileState by profileViewModel.state.collectAsState()
                
                if (profileState.profile?.isHealthProfessional == true) {
                    PortalScreen()
                } else {
                    ProfessionalsScreenNew(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDetail = { id -> navController.navigate(AppRoutes.professionalDetail(id)) },
                        onNavigateToCrmLookup = { navController.navigate(AppRoutes.CRM_LOOKUP) }
                    )
                }
            }
        }
        
        composable(AppRoutes.EMERGENCY) {
            EmergencyScreen(
                onNavigateToRequest = { emergencyId ->
                    // Guard 1: chat já foi aberto para este emergencyId nesta sessão
                    if (emergencyId in chatNavigatedIds.value) {
                        FileLogger.log("DEBUG", "NavGraph",
                            "onNavigateToRequest ignorado — chat já aberto emergencyId=$emergencyId")
                        return@EmergencyScreen
                    }
                    // Guard 2: pilha de navegação já contém chat/ ou emergency_request/.
                    // Usa currentBackStack.value (lista completa) em vez de currentDestination
                    // para evitar leituras stale de snapshot Compose em contexto de coroutine.
                    val backStack = navController.currentBackStack.value
                    val alreadyInChatOrRequest = backStack.any { entry ->
                        val route = entry.destination.route ?: ""
                        route.startsWith("chat/") || route.startsWith("emergency_request/")
                    }
                    if (alreadyInChatOrRequest) {
                        val routes = backStack.mapNotNull { it.destination.route }.joinToString()
                        FileLogger.log("DEBUG", "NavGraph",
                            "onNavigateToRequest ignorado — back stack=$routes")
                        return@EmergencyScreen
                    }
                    navController.navigate("emergency_request/$emergencyId") {
                        launchSingleTop = true  // evita duplicata se já estiver no topo
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = {},
                onNavigateToResponse = {},
                viewModel = emergencyViewModel ?: koinViewModel()
            )
        }

        composable(
            route = "emergency_request/{emergencyId}",
            arguments = listOf(navArgument("emergencyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            EmergencyRequestScreen(
                emergencyId = emergencyId,
                navController = navController,
                viewModel = emergencyViewModel ?: koinViewModel(),
                alreadyInChat = { id -> id in chatNavigatedIds.value }
            )
        }
        
        composable(
            route = AppRoutes.EMERGENCY_RESPONSE,
            arguments = listOf(navArgument("emergencyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            EmergencyResponseScreen(
                emergencyId = emergencyId,
                navController = navController,
                viewModel = emergencyViewModel ?: koinViewModel()
            )
        }
        
        composable(
            route = AppRoutes.CHAT,
            arguments = listOf(navArgument("emergencyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            ChatScreen(
                emergencyId = emergencyId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateBackResolved = {
                    // Encerrado pela outra parte: limpa estado KMM e retorna à home,
                    // removendo EmergencyScreen/EmergencyRequestScreen da back stack.
                    emergencyViewModel?.onClearEmergencyState()
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.HOME) { inclusive = false }
                    }
                },
                emergencyViewModel = emergencyViewModel
            )
        }
        

        
        composable(AppRoutes.HISTORY) {
            HistoryScreenNew(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Rota de Configurações removida — log export movido para toolbar da Home
        composable(AppRoutes.TERMS) {
            TermsScreen(navController = navController)
        }
        
        composable(AppRoutes.PRIVACY) {
            PrivacyScreen(navController = navController)
        }
        
        composable(AppRoutes.ABOUT) {
            AboutScreen(navController = navController)
        }
        

        composable(AppRoutes.AUTOCUIDADO) {
            AutocuidadoScreen(navController = navController)
        }

        composable(AppRoutes.PROFESSIONALS) {
            ProfessionalsScreenNew(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(AppRoutes.professionalDetail(id)) },
                onNavigateToCrmLookup = { navController.navigate(AppRoutes.CRM_LOOKUP) }
            )
        }
        
        composable("education") {
            EducationScreenNew(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.CRM_LOOKUP) {
            CrmLookupScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = AppRoutes.PROFESSIONAL_DETAIL,
            arguments = listOf(navArgument("professionalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val professionalId = backStackEntry.arguments?.getString("professionalId") ?: ""
            ProfessionalDetailScreen(
                professionalId = professionalId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.NOTIFICATIONS) {
            NotificationsScreen(navController = navController)
        }

        composable(AppRoutes.HELP) {
            HelpScreen(navController = navController)
        }




        // Tela de Navegação (Google Maps)
        composable(
            route = AppRoutes.NAVIGATION,
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lng") { type = NavType.FloatType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 0.0
            val name = backStackEntry.arguments?.getString("name") ?: "Destino"
            NavigationScreen(
                destinationLat = lat,
                destinationLng = lng,
                destinationName = name,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
