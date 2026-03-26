package com.afilaxy.app.ui.scaffold

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.afilaxy.app.navigation.AppRoutes

/**
 * Scaffold principal do app com navegação bottom bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfilaxyAppScaffoldSimple(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(text = tab.label)
                        }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        content()
    }
}

/**
 * Definição dos 4 tabs principais do app
 */
private enum class NavigationTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(
        route = AppRoutes.HOME,
        label = "Home",
        icon = Icons.Default.Home
    ),
    MAP(
        route = AppRoutes.MAP,
        label = "Mapa",
        icon = Icons.Default.LocationOn
    ),
    PROFILE(
        route = AppRoutes.PROFILE,
        label = "Perfil",
        icon = Icons.Default.Person
    ),
    PORTAL(
        route = AppRoutes.PORTAL,
        label = "Portal",
        icon = Icons.Default.MedicalServices
    )
}