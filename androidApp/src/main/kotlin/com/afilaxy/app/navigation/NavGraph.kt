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

import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.presentation.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel

private val protectedRoutes = setOf(
    AppRoutes.HOME, AppRoutes.EMERGENCY, AppRoutes.PROFILE, AppRoutes.HISTORY,
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
        startDestination = startDestination ?: AppRoutes.LOGIN
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { 
                    navController.navigate(AppRoutes.HOME) { 
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
                    navController.navigate(AppRoutes.HOME) { 
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
                    onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) }
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
                    // Guard duplo: não abre emergency_request se o chat já foi aberto para este ID.
                    // O LaunchedEffect do EmergencyScreen pode re-disparar quando o KMM emite um
                    // state update ao receber dados do helper (currentEmergency actualizado com
                    // helperId), mudando isRequester ou emergencyId e re-executando o effect.
                    // wasNavigatedToRequest() deveria bloquear, mas pode falhar se o ViewModel
                    // foi re-criado pelo Koin após o EmergencyScreen voltar ao topo da backstack.
                    if (emergencyId in chatNavigatedIds.value) {
                        FileLogger.log("DEBUG", "NavGraph",
                            "onNavigateToRequest ignorado — chat já aberto emergencyId=$emergencyId")
                        return@EmergencyScreen
                    }
                    navController.navigate("emergency_request/$emergencyId")
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
                viewModel = emergencyViewModel ?: koinViewModel()
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
        
        composable(AppRoutes.SETTINGS) {
            SettingsScreenNew(navController = navController)
        }
        
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

        // Detalhe de Evento
        composable(
            route = AppRoutes.EVENTO_DETAIL,
            arguments = listOf(navArgument("eventoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
            val evento = eventos.find { it.id == eventoId }
            if (evento != null) {
                EventoDetailScreen(
                    evento = evento,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        // Detalhe de Produto
        composable(
            route = AppRoutes.PRODUTO_DETAIL,
            arguments = listOf(navArgument("produtoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val produtoId = backStackEntry.arguments?.getString("produtoId") ?: ""
            val produto = produtos.find { it.id == produtoId }
            if (produto != null) {
                ProdutoDetailScreen(
                    produto = produto,
                    onBackClick = { navController.popBackStack() }
                )
            }
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
