package com.notivas.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notivas.ui.dashboard.DashboardScreen
import com.notivas.ui.dashboard.DashboardViewModel
import com.notivas.ui.profile.ProfileScreen
import com.notivas.ui.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // We get the viewmodel here to use it in the TopAppBar action
    val dashboardViewModel: DashboardViewModel = hiltViewModel()

    val items = listOf(
        Screen.Dashboard,
        Screen.Profile
    )

    Scaffold(
        topBar = {
            val title = if (currentDestination?.route == Screen.Profile.route) "Mi Perfil" else "NotiVas"
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                if (screen == Screen.Dashboard) Icons.Default.Home else Icons.Default.Person,
                                contentDescription = null
                            ) 
                        },
                        label = { Text(if (screen == Screen.Dashboard) "Home" else "Yo") },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val assignments by dashboardViewModel.assignments.collectAsState()
                val courses by dashboardViewModel.courses.collectAsState()
                val selectedCourseId by dashboardViewModel.selectedCourseId.collectAsState()
                val isRefreshing by dashboardViewModel.isRefreshing.collectAsState()

                DashboardScreen(
                    assignments = assignments,
                    courses = courses,
                    selectedCourseId = selectedCourseId,
                    isRefreshing = isRefreshing,
                    onCourseSelect = dashboardViewModel::selectCourse,
                    onRefresh = dashboardViewModel::refresh
                )
            }
            composable(Screen.Profile.route) {
                val viewModel: ProfileViewModel = hiltViewModel()
                val profile by viewModel.profile.collectAsState()
                val isLoggedOut by viewModel.isLoggedOut.collectAsState()

                LaunchedEffect(isLoggedOut) {
                    if (isLoggedOut) onLogout()
                }

                ProfileScreen(
                    profile = profile,
                    onLogout = viewModel::logout
                )
            }
        }
    }
}
