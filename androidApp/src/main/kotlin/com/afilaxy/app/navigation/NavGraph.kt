package com.afilaxy.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.afilaxy.app.ui.screens.*
import com.afilaxy.domain.model.Evento
import com.afilaxy.domain.model.Produto
import com.google.firebase.auth.FirebaseAuth

private val protectedRoutes = setOf(
    AppRoutes.HOME, AppRoutes.EMERGENCY, AppRoutes.PROFILE, AppRoutes.HISTORY,
    AppRoutes.SETTINGS, AppRoutes.COMMUNITY, AppRoutes.AUTOCUIDADO, AppRoutes.PROFESSIONALS,
    AppRoutes.MAP, AppRoutes.NOTIFICATIONS, AppRoutes.HELP, AppRoutes.ABOUT,
    AppRoutes.TERMS, AppRoutes.PRIVACY, "emergency_request", "emergency_response", AppRoutes.CHAT
)

@Composable
fun NavGraph(startDestination: String? = null) {
    val navController = rememberNavController()
    val isAuthenticated = FirebaseAuth.getInstance().currentUser != null

    // Redirect unauthenticated deep-links to login
    LaunchedEffect(startDestination) {
        if (startDestination != null && !isAuthenticated) {
            navController.navigate(AppRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    // Dados da comunidade — centralizados aqui para reuso nas rotas de detalhe
    val produtos = listOf(
        Produto("1", "Bombinha de Asma", "Bombinha para crises de asma. Disponível sem receita em algumas farmácias.", 45.0, "Medicamentos",
            precoOriginal = "59.90", cupom = "AFILAXY10", desconto = "R$ 14,90", validadeCupom = "31/12/2025", farmacia = "Farmácias Nissei"),
        Produto("2", "Espaçador", "Espaçador para bombinha — melhora absorção do medicamento.", 25.0, "Acessórios",
            precoOriginal = "35.00", cupom = "ESPA20", desconto = "R$ 10,00", validadeCupom = "30/06/2025", farmacia = "Ultrafarma")
    )
    val eventos = listOf(
        Evento("1", "Palestra sobre Asma", "Aprenda a controlar a asma com especialistas.", "15/12/2024", "Centro Comunitário",
            organizador = "ABRA", horario = "14h"),
        Evento("2", "Grupo de Apoio", "Encontro mensal de apoio para pacientes.", "20/12/2024", "Online",
            organizador = "Crônicos do Dia-a-Dia", horario = "19h")
    )
    
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
        
        composable(AppRoutes.HOME) {
            HomeScreen(
                onNavigateToEmergency = { navController.navigate(AppRoutes.EMERGENCY) },
                onNavigateToProfile = { navController.navigate(AppRoutes.PROFILE) },
                onNavigateToHistory = { navController.navigate(AppRoutes.HISTORY) },
                onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) },
                onNavigateToCommunity = { navController.navigate(AppRoutes.COMMUNITY) },
                onNavigateToAutocuidado = { navController.navigate(AppRoutes.AUTOCUIDADO) },
                onNavigateToProfessionals = { navController.navigate(AppRoutes.PROFESSIONALS) }
            )
        }
        
        composable(AppRoutes.EMERGENCY) {
            EmergencyScreen(
                onNavigateToRequest = { emergencyId -> 
                    navController.navigate("emergency_request/$emergencyId") 
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = {},
                onNavigateToResponse = {}
            )
        }
        
        composable(
            route = "emergency_request/{emergencyId}",
            arguments = listOf(navArgument("emergencyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            EmergencyRequestScreen(
                emergencyId = emergencyId,
                navController = navController
            )
        }
        
        composable(
            route = AppRoutes.EMERGENCY_RESPONSE,
            arguments = listOf(navArgument("emergencyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            EmergencyResponseScreen(
                emergencyId = emergencyId,
                navController = navController
            )
        }
        
        composable(
            route = AppRoutes.CHAT,
            arguments = listOf(navArgument("emergencyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            ChatScreen(
                emergencyId = emergencyId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(AppRoutes.PROFILE) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(AppRoutes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
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
        
        composable(AppRoutes.COMMUNITY) {
            CommunityScreen(
                navController = navController,
                produtos = produtos,
                eventos = eventos
            )
        }

        composable(AppRoutes.AUTOCUIDADO) {
            AutocuidadoScreen(navController = navController)
        }

        composable(AppRoutes.PROFESSIONALS) {
            ProfessionalListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.MAP) {
            MapScreen(navController = navController)
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
