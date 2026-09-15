package com.notivas.ui.Ananau

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.notivas.data.model.Course
import com.notivas.ui.Ananau.components.*

@Composable
fun AnanauScreen(
    uiState: AnanauUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onClearConversation: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onTriggerAtMention: () -> Unit,
    onSelectMentionCourse: (Course) -> Unit,
    onApplyCourseMention: (Course) -> Unit,
    onApplyResourceMention: (Course, String) -> Unit,
    onBackToCourseSelection: () -> Unit,
    onDismissMentionMenu: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onDismissHistory: () -> Unit = {},
    onStartNewChat: () -> Unit = {},
    onLoadSession: (String) -> Unit = {},
    onRenameSession: (String, String) -> Unit = { _, _ -> },
    onDeleteSession: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages or loading state change
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Modal Bottom Sheet for History
    if (uiState.showHistorySheet) {
        AnanauHistoryBottomSheet(
            savedSessions = uiState.savedSessions,
            currentSessionId = uiState.currentSessionId,
            courses = uiState.courses,
            onSelectSession = onLoadSession,
            onRenameSession = onRenameSession,
            onDeleteSession = onDeleteSession,
            onNewChat = onStartNewChat,
            onDismiss = onDismissHistory
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Top action bar: History, Balance, and Chat Actions
        AnanauChatHeader(
            savedSessionsCount = uiState.savedSessions.size,
            sessionTokens = uiState.sessionTokens,
            openRouterBalance = uiState.openRouterBalance,
            hasMessages = uiState.messages.isNotEmpty(),
            onOpenHistory = onOpenHistory,
            onStartNewChat = onStartNewChat,
            onClearConversation = onClearConversation
        )

        // Setup Warning Banner (if API Key missing or Ananau disabled)
        if (!uiState.hasApiKey || !uiState.isAnanauEnabled) {
            MissingApiKeyCard(
                isAnanauDisabled = !uiState.isAnanauEnabled,
                onNavigateToProfile = onNavigateToProfile
            )
        }

        // Messages Flow & Contextual @ Mention Overlay Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Main Message Flow or Empty State
            AnanauMessageList(
                messages = uiState.messages,
                isLoading = uiState.isLoading,
                listState = listState,
                coursesCount = uiState.courses.size,
                currentModel = uiState.currentModel,
                onSuggestionClick = { onSendMessage(it) },
                modifier = Modifier.fillMaxSize()
            )

            // Scrim overlay when @ mention popup is open
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.showMentionMenu,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismissMentionMenu
                        )
                )
            }

            // Contextual @ Mention Popup
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.showMentionMenu,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AnanauMentionPopup(
                    uiState = uiState,
                    onSelectCourse = onSelectMentionCourse,
                    onApplyCourse = onApplyCourseMention,
                    onApplyResource = onApplyResourceMention,
                    onBack = onBackToCourseSelection,
                    onDismiss = onDismissMentionMenu
                )
            }
        }

        // Docked input bar with '@' trigger button
        AnanauInputBar(
            inputText = uiState.inputText,
            isLoading = uiState.isLoading,
            enabled = uiState.hasApiKey && uiState.isAnanauEnabled,
            onInputChange = onInputChange,
            onSend = { onSendMessage(null) },
            onTriggerMention = onTriggerAtMention
        )
    }
}
