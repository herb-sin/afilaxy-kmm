package com.afilaxy.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.afilaxy.app.ui.screens.*

@Composable
fun NavGraph(startDestination: String? = null) {
    val navController = rememberNavController()
    
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
                onNavigateToAutocuidado = { navController.navigate(AppRoutes.AUTOCUIDADO) }
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
            CommunityScreen(navController = navController)
        }
        
        composable(AppRoutes.AUTOCUIDADO) {
            AutocuidadoScreen(navController = navController)
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
    }
}
