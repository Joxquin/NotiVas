package com.notivas.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Assessment
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
import com.notivas.ui.foros.ForosScreen
import com.notivas.ui.foros.ForosViewModel
import com.notivas.ui.notas.NotasScreen
import com.notivas.ui.notas.NotasViewModel
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
        Screen.Foros,
        Screen.Notas,
        Screen.Profile
    )

    Scaffold(
        topBar = {
            val title = when (currentDestination?.route) {
                Screen.Profile.route -> "Mi Perfil"
                Screen.Foros.route -> "Foros"
                Screen.Notas.route -> "Notas"
                else -> "NotiVas"
            }
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
                            val icon = when(screen) {
                                Screen.Dashboard -> Icons.AutoMirrored.Filled.Assignment
                                Screen.Foros -> Icons.Default.Forum
                                Screen.Notas -> Icons.Default.Assessment
                                Screen.Profile -> Icons.Default.Person
                                else -> Icons.Default.Home
                            }
                            Icon(icon, contentDescription = null) 
                        },
                        label = { 
                            val label = when(screen) {
                                Screen.Dashboard -> "Tareas"
                                Screen.Foros -> "Foros"
                                Screen.Notas -> "Notas"
                                Screen.Profile -> "Yo"
                                else -> "Home"
                            }
                            Text(label) 
                        },
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
                val plannerTasks by dashboardViewModel.plannerTasks.collectAsState()
                val courses by dashboardViewModel.courses.collectAsState()
                val selectedCourseId by dashboardViewModel.selectedCourseId.collectAsState()
                val selectedSemester by dashboardViewModel.selectedSemester.collectAsState()
                val availableSemesters by dashboardViewModel.availableSemesters.collectAsState()
                val showUndatedTasks by dashboardViewModel.showUndatedTasks.collectAsState()
                val isRefreshing by dashboardViewModel.isRefreshing.collectAsState()

                DashboardScreen(
                    assignments = assignments,
                    plannerTasks = plannerTasks,
                    courses = courses,
                    selectedCourseId = selectedCourseId,
                    selectedSemester = selectedSemester,
                    availableSemesters = availableSemesters,
                    showUndatedTasks = showUndatedTasks,
                    isRefreshing = isRefreshing,
                    onCourseSelect = dashboardViewModel::selectCourse,
                    onSemesterSelect = dashboardViewModel::selectSemester,
                    onToggleShowUndated = dashboardViewModel::toggleShowUndatedTasks,
                    onRefresh = dashboardViewModel::refresh
                )
            }
            composable(Screen.Foros.route) {
                val viewModel: ForosViewModel = hiltViewModel()
                val forums by viewModel.forums.collectAsState()
                val isRefreshing by viewModel.isRefreshing.collectAsState()

                ForosScreen(
                    forums = forums,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh
                )
            }
            composable(Screen.Notas.route) {
                val viewModel: NotasViewModel = hiltViewModel()
                val isRefreshing by viewModel.isRefreshing.collectAsState()

                NotasScreen(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh
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
