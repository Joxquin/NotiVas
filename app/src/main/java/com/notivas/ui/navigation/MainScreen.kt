package com.notivas.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notivas.ui.dashboard.DashboardViewModel
import com.notivas.ui.navigation.components.MainBottomNavBar
import com.notivas.ui.navigation.components.MainTopAppBar

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val items = remember { listOf(Screen.Dashboard, Screen.Notas, Screen.Copilot, Screen.Profile) }

    val isSimulador = currentDestination?.route == Screen.Simulador.route
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    val dashboardListState = rememberLazyListState()
    val notasListState = rememberLazyListState()
    val profileListState = rememberLazyListState()

    val activeListState = when (currentDestination?.route) {
        Screen.Dashboard.route -> dashboardListState
        Screen.Notas.route -> notasListState
        Screen.Profile.route -> profileListState
        else -> null
    }

    val isScrolled by remember(activeListState) {
        derivedStateOf {
            activeListState?.let {
                it.firstVisibleItemIndex > 0 || it.firstVisibleItemScrollOffset > 0
            } ?: false
        }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                currentDestination = currentDestination,
                isVisible = !isSimulador,
                isScrolled = isScrolled
            )
        },
        bottomBar = {
            MainBottomNavBar(
                currentDestination = currentDestination,
                isVisible = !isSimulador && !isImeVisible,
                items = items,
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            dashboardViewModel = dashboardViewModel,
            dashboardListState = dashboardListState,
            notasListState = notasListState,
            profileListState = profileListState,
            isSimulador = isSimulador,
            innerPadding = innerPadding,
            onLogout = onLogout
        )
    }
}
