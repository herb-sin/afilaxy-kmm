package com.afilaxy.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.afilaxy.app.ui.screens.*
import com.afilaxy.presentation.auth.AuthViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Sealed class para rotas de navegação
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Emergency : Screen("emergency")
    data object Profile : Screen("profile")
    data object History : Screen("history")
    data object Map : Screen("map")
    data object Chat : Screen("chat/{emergencyId}") {
        fun createRoute(emergencyId: String) = "chat/$emergencyId"
    }
    data object EmergencyRequest : Screen("emergency_request/{emergencyId}") {
        fun createRoute(emergencyId: String) = "emergency_request/$emergencyId"
    }
    data object EmergencyResponse : Screen("emergency_response/{emergencyId}") {
        fun createRoute(emergencyId: String) = "emergency_response/$emergencyId"
    }
    data object HelperResponse : Screen("helper_response/{emergencyId}") {
        fun createRoute(emergencyId: String) = "helper_response/$emergencyId"
    }
}

/**
 * Grafo de navegação principal
 */
@Composable
fun LegacyNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    
    // Define startDestination based on auth state
    val startDestination = if (authState.isAuthenticated) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Tela de Login
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        // Tela de Registro
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Tela Home
        composable(Screen.Home.route) {
            HomeScreenNew(
                onNavigateToEmergency = {
                    navController.navigate(Screen.Emergency.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Tela de Emergência
        composable(Screen.Emergency.route) {
            EmergencyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { emergencyId ->
                    navController.navigate(Screen.Chat.createRoute(emergencyId))
                },
                onNavigateToResponse = { emergencyId ->
                    navController.navigate(Screen.EmergencyResponse.createRoute(emergencyId))
                },
                onNavigateToRequest = { emergencyId ->
                    navController.navigate(Screen.EmergencyRequest.createRoute(emergencyId)) {
                        popUpTo(Screen.Emergency.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Tela de Perfil
        composable(Screen.Profile.route) {
            ProfileScreenNew(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Tela de Histórico
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Tela de Chat
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("emergencyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            ChatScreen(
                emergencyId = emergencyId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Tela de Mapa
        composable(Screen.Map.route) {
            MapScreen(
                navController = navController
            )
        }
        
        // Tela de Solicitação de Emergência
        composable(
            route = Screen.EmergencyRequest.route,
            arguments = listOf(
                navArgument("emergencyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            EmergencyRequestScreen(
                navController = navController,
                emergencyId = emergencyId
            )
        }
        
        // Tela de Resposta de Emergência
        composable(
            route = Screen.EmergencyResponse.route,
            arguments = listOf(
                navArgument("emergencyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            EmergencyResponseScreen(
                navController = navController,
                emergencyId = emergencyId
            )
        }
        
        // Tela de Resposta do Helper
        composable(
            route = Screen.HelperResponse.route,
            arguments = listOf(
                navArgument("emergencyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            HelperResponseScreen(
                navController = navController,
                emergencyId = emergencyId
            )
        }
    }
}
