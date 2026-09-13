package com.notivas.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notivas.ui.copilot.CopilotScreen
import com.notivas.ui.copilot.CopilotViewModel
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

    val items = listOf(Screen.Dashboard, Screen.Notas, Screen.Copilot, Screen.Profile)

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

    val topAppBarContainerColor by animateColorAsState(
        targetValue = if (isScrolled) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(durationMillis = 250),
        label = "topAppBarContainerColor"
    )

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !isSimulador,
                enter = fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(250)) { -it },
                exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(200)) { -it }
            ) {
                val title =
                    when (currentDestination?.route) {
                        Screen.Profile.route -> "Mi Perfil"
                        Screen.Notas.route -> "Notas"
                        Screen.Copilot.route -> "Copilot AI"
                        else -> "NotiVas"
                    }
                TopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = title,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, delayMillis = 60)) +
                                        slideInVertically(
                                            animationSpec = tween(220, delayMillis = 60),
                                            initialOffsetY = { fullHeight -> fullHeight / 3 }
                                        )) togetherWith
                                        (fadeOut(animationSpec = tween(120)) +
                                                slideOutVertically(
                                                    animationSpec = tween(120),
                                                    targetOffsetY = { fullHeight -> -fullHeight / 3 }
                                                ))
                            },
                            label = "TopAppBarTitleTransition"
                        ) { targetTitle ->
                            Text(targetTitle, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = topAppBarContainerColor,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isSimulador && !isImeVisible,
                enter = fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(250)) { it },
                exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(200)) { it }
            ) {
                val outlineVariant = MaterialTheme.colorScheme.outlineVariant
                NavigationBar(
                    modifier =
                        Modifier.drawBehind {
                            drawLine(
                                color = outlineVariant.copy(alpha = 0.4f),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    items.forEach { screen ->
                        val selected =
                            currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true

                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "navItemScale_${screen.route}"
                        )

                        NavigationBarItem(
                            icon = {
                                val icon =
                                    when (screen) {
                                        Screen.Dashboard ->
                                            if (selected) Icons.Filled.Dashboard
                                            else Icons.Outlined.Dashboard

                                        Screen.Foros ->
                                            if (selected) Icons.Filled.Forum
                                            else Icons.Outlined.Forum

                                        Screen.Notas ->
                                            if (selected) Icons.Filled.Analytics
                                            else Icons.Outlined.Analytics

                                        Screen.Copilot ->
                                            if (selected) Icons.Filled.AutoAwesome
                                            else Icons.Outlined.AutoAwesome

                                        Screen.Profile ->
                                            if (selected) Icons.Filled.Person
                                            else Icons.Outlined.Person

                                        else ->
                                            if (selected) Icons.Filled.Dashboard
                                            else Icons.Outlined.Dashboard
                                    }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.graphicsLayer(
                                        scaleX = iconScale,
                                        scaleY = iconScale
                                    )
                                )
                            },
                            label = {
                                val label =
                                    when (screen) {
                                        Screen.Dashboard -> "Inicio"
                                        Screen.Foros -> "Foros"
                                        Screen.Notas -> "Notas"
                                        Screen.Copilot -> "Copilot"
                                        Screen.Profile -> "Perfil"
                                        else -> "Home"
                                    }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight =
                                        if (selected) FontWeight.SemiBold
                                        else FontWeight.Medium
                                )
                            },
                            selected = selected,
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor =
                                        MaterialTheme.colorScheme
                                            .onSecondaryContainer,
                                    selectedTextColor =
                                        MaterialTheme.colorScheme.onSurface,
                                    indicatorColor =
                                        MaterialTheme.colorScheme
                                            .secondaryContainer,
                                    unselectedIconColor =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant,
                                    unselectedTextColor =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                ),
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier =
                if (isSimulador) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                },
            enterTransition = {
                fadeIn(
                    animationSpec = tween(220, delayMillis = 60, easing = FastOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(220, delayMillis = 60, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                )
            },
            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(220, delayMillis = 60, easing = FastOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(220, delayMillis = 60, easing = FastOutSlowInEasing)
                )
            },
            popExitTransition = {
                fadeOut(
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                )
            }
        ) {
            composable(Screen.Dashboard.route) {
                val userProfile by dashboardViewModel.userProfile.collectAsState()
                val urgentAssignments by dashboardViewModel.urgentAssignments.collectAsState()
                val weeklySchedule by dashboardViewModel.weeklySchedule.collectAsState()
                val courseStats by dashboardViewModel.courseStats.collectAsState()
                val selectedDate by dashboardViewModel.selectedDate.collectAsState()
                val selectedDateAssignments by
                dashboardViewModel.selectedDateAssignments.collectAsState()
                val inspectedCourse by dashboardViewModel.inspectedCourse.collectAsState()
                val inspectedCourseAssignments by
                dashboardViewModel.inspectedCourseAssignments.collectAsState()
                val inspectedCourseForums by
                dashboardViewModel.inspectedCourseForums.collectAsState()
                val institutionName by dashboardViewModel.institutionName.collectAsState()
                val isRefreshing by dashboardViewModel.isRefreshing.collectAsState()

                DashboardScreen(
                    userProfile = userProfile,
                    urgentAssignments = urgentAssignments,
                    weeklySchedule = weeklySchedule,
                    courseStats = courseStats,
                    selectedDate = selectedDate,
                    selectedDateAssignments = selectedDateAssignments,
                    inspectedCourse = inspectedCourse,
                    inspectedCourseAssignments = inspectedCourseAssignments,
                    inspectedCourseForums = inspectedCourseForums,
                    institutionName = institutionName,
                    isRefreshing = isRefreshing,
                    lazyListState = dashboardListState,
                    onDateSelect = dashboardViewModel::selectDate,
                    onInspectCourse = dashboardViewModel::inspectCourse,
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
            composable(Screen.Notas.route) { backStackEntry ->
                val viewModel: NotasViewModel =
                    hiltViewModel(
                        remember(backStackEntry) {
                            navController.getBackStackEntry(Screen.Notas.route)
                        }
                    )
                val uiState by viewModel.uiState.collectAsState()
                val isRefreshing by viewModel.isRefreshing.collectAsState()

                NotasScreen(
                    uiState = uiState,
                    isRefreshing = isRefreshing,
                    lazyListState = notasListState,
                    onCourseSelect = viewModel::selectCourse,
                    onSimulatedScoreChange = viewModel::updateSimulatedScore,
                    onTargetGoalSelect = viewModel::selectTargetGoal,
                    onResetSimulation = viewModel::resetSimulation,
                    onRefresh = viewModel::refresh,
                    onOpenSimulator = { navController.navigate(Screen.Simulador.route) }
                )
            }
            composable(
                route = Screen.Simulador.route,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 260, delayMillis = 40, easing = FastOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -(fullWidth * 0.2f).toInt() },
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -(fullWidth * 0.2f).toInt() },
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                val viewModel: NotasViewModel =
                    hiltViewModel(
                        remember(it) { navController.getBackStackEntry(Screen.Notas.route) }
                    )
                val uiState by viewModel.uiState.collectAsState()
                val selectedCourse = uiState.selectedCourse

                if (selectedCourse != null) {
                    com.notivas.ui.notas.SimuladorScreen(
                        course = selectedCourse,
                        groups = uiState.simulationGroups,
                        availableAssignments = uiState.availableCourseAssignments,
                        simulatedFinalGrade = uiState.groupSimulatedFinalGrade,
                        totalWeight = uiState.totalConfiguredWeight,
                        onBack = { navController.popBackStack() },
                        onCreateGroup = viewModel::createGroup,
                        onDeleteGroup = viewModel::deleteGroup,
                        onAddCanvasAssignment = viewModel::addCanvasAssignmentToGroup,
                        onAddPlaceholderAssignment = viewModel::addPlaceholderAssignmentToGroup,
                        onUpdateScore = viewModel::updateSimulationItemScore,
                        onDeleteItem = viewModel::deleteSimulationItem,
                        onLinkItem = viewModel::linkSimulationItemWithCanvas
                    )
                }
            }
            composable(Screen.Copilot.route) {
                val viewModel: CopilotViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                CopilotScreen(
                    uiState = uiState,
                    onSelectCourse = viewModel::selectCourse,
                    onInputChange = viewModel::updateInputText,
                    onSendMessage = { prompt -> viewModel.sendMessage(prompt) },
                    onClearConversation = viewModel::clearConversation,
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Profile.route) {
                val viewModel: ProfileViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()
                val isLoggedOut by viewModel.isLoggedOut.collectAsState()

                LaunchedEffect(isLoggedOut) { if (isLoggedOut) onLogout() }

                ProfileScreen(
                    uiState = uiState,
                    lazyListState = profileListState,
                    onNotif24hChange = viewModel::setNotif24h,
                    onNotif3hChange = viewModel::setNotif3h,
                    onNotif30mChange = viewModel::setNotif30m,
                    onSyncIntervalChange = viewModel::setSyncInterval,
                    onBiometricLockChange = viewModel::setBiometricLock,
                    onOpenRouterApiKeyChange = viewModel::setOpenRouterApiKey,
                    onOpenRouterModelChange = viewModel::setOpenRouterModel,
                    onCopilotEnabledChange = viewModel::setCopilotEnabled,
                    onLogout = viewModel::logout
                )
            }
        }
    }
}
