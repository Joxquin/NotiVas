package com.notivas.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.notivas.ui.copilot.CopilotScreen
import com.notivas.ui.copilot.CopilotViewModel
import com.notivas.ui.dashboard.DashboardScreen
import com.notivas.ui.dashboard.DashboardViewModel
import com.notivas.ui.debug.DebugScreen
import com.notivas.ui.foros.ForosScreen
import com.notivas.ui.foros.ForosViewModel
import com.notivas.ui.notas.NotasScreen
import com.notivas.ui.notas.NotasViewModel
import com.notivas.ui.notas.SimuladorScreen
import com.notivas.ui.profile.ProfileScreen
import com.notivas.ui.profile.ProfileViewModel

@Composable
fun MainNavGraph(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    dashboardListState: LazyListState,
    notasListState: LazyListState,
    profileListState: LazyListState,
    isSimulador: Boolean,
    innerPadding: PaddingValues,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
            .then(
                if (isSimulador) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                }
            ),
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
            val selectedDateAssignments by dashboardViewModel.selectedDateAssignments.collectAsState()
            val inspectedCourse by dashboardViewModel.inspectedCourse.collectAsState()
            val inspectedCourseAssignments by dashboardViewModel.inspectedCourseAssignments.collectAsState()
            val inspectedCourseForums by dashboardViewModel.inspectedCourseForums.collectAsState()
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
            val viewModel: NotasViewModel = hiltViewModel(
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
        ) { backStackEntry ->
            val viewModel: NotasViewModel = hiltViewModel(
                remember(backStackEntry) { navController.getBackStackEntry(Screen.Notas.route) }
            )
            val uiState by viewModel.uiState.collectAsState()
            val selectedCourse = uiState.selectedCourse

            if (selectedCourse != null) {
                SimuladorScreen(
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
                onInputChange = viewModel::updateInputText,
                onSendMessage = { prompt -> viewModel.sendMessage(prompt) },
                onClearConversation = viewModel::clearConversation,
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onTriggerAtMention = viewModel::triggerAtMention,
                onSelectMentionCourse = viewModel::selectMentionCourse,
                onApplyCourseMention = viewModel::applyCourseMention,
                onApplyResourceMention = viewModel::applyResourceMention,
                onBackToCourseSelection = viewModel::backToCourseSelection,
                onDismissMentionMenu = viewModel::dismissMentionMenu,
                onOpenHistory = viewModel::openHistorySheet,
                onDismissHistory = viewModel::dismissHistorySheet,
                onStartNewChat = viewModel::startNewChat,
                onLoadSession = viewModel::loadSession,
                onRenameSession = viewModel::renameSession,
                onDeleteSession = viewModel::deleteSession
            )
        }

        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val isLoggedOut by viewModel.isLoggedOut.collectAsState()

            LaunchedEffect(isLoggedOut) {
                if (isLoggedOut) onLogout()
            }

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
                onRefreshOpenRouterBalance = viewModel::refreshOpenRouterBalance,
                onNavigateToDebug = {
                    navController.navigate(Screen.Debug.route) {
                        launchSingleTop = true
                    }
                },
                onLogout = viewModel::logout
            )
        }

        composable(Screen.Debug.route) {
            val debugViewModel: com.notivas.ui.debug.DebugViewModel = hiltViewModel()
            val assignments by debugViewModel.assignments.collectAsState()

            DebugScreen(
                versionName = "2.1.0",
                versionCode = 4,
                architecture = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
                assignments = assignments,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
