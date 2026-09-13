package com.notivas.ui.copilot

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notivas.data.repository.CopilotSource

@Composable
fun CopilotScreen(
    uiState: CopilotUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onClearConversation: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onTriggerAtMention: () -> Unit,
    onSelectMentionCourse: (com.notivas.data.model.Course) -> Unit,
    onApplyCourseMention: (com.notivas.data.model.Course) -> Unit,
    onApplyResourceMention: (com.notivas.data.model.Course, String) -> Unit,
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
        CopilotHistoryBottomSheet(
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
        // Top action bar: History & New Chat buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Historial button
            FilledTonalButton(
                onClick = onOpenHistory,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Historial",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (uiState.savedSessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${uiState.savedSessions.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Center: Session tokens and/or balance pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (uiState.sessionTokens > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${uiState.sessionTokens} tks",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.openRouterBalance?.let { balance ->
                    val balanceText = when {
                        balance.isFreeTier == true -> "Free Tier"
                        balance.remainingCredits != null -> {
                            val rem = balance.remainingCredits
                            if (rem < 0.01) {
                                String.format(java.util.Locale.US, "$%.4f", rem)
                            } else {
                                String.format(java.util.Locale.US, "$%.2f", rem)
                            }
                        }
                        else -> null
                    }
                    if (balanceText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = balanceText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Right side buttons: Nuevo chat / Limpiar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onStartNewChat,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddComment,
                        contentDescription = "Nuevo chat",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (uiState.messages.isNotEmpty()) {
                    IconButton(
                        onClick = onClearConversation,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Limpiar chat actual",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Setup Warning Banner (if API Key missing or copilot disabled)
        if (!uiState.hasApiKey || !uiState.isCopilotEnabled) {
            MissingApiKeyCard(
                isCopilotDisabled = !uiState.isCopilotEnabled,
                onNavigateToProfile = onNavigateToProfile
            )
        }

        // Main Message Flow or Empty State Suggestions
        if (uiState.messages.isEmpty()) {
            CopilotEmptyState(
                coursesCount = uiState.courses.size,
                currentModel = uiState.currentModel,
                onSuggestionClick = { onSendMessage(it) },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    CopilotMessageBubble(message = message)
                }

                if (uiState.isLoading) {
                    item(key = "loading_bubble") {
                        CopilotLoadingBubble()
                    }
                }
            }
        }

        // Contextual @ Mention Popup (floating directly above the input bar)
        AnimatedVisibility(
            visible = uiState.showMentionMenu,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            CopilotMentionPopup(
                uiState = uiState,
                onSelectCourse = onSelectMentionCourse,
                onApplyCourse = onApplyCourseMention,
                onApplyResource = onApplyResourceMention,
                onBack = onBackToCourseSelection,
                onDismiss = onDismissMentionMenu
            )
        }

        // Docked input bar with '@' trigger button
        CopilotInputBar(
            inputText = uiState.inputText,
            isLoading = uiState.isLoading,
            enabled = uiState.hasApiKey && uiState.isCopilotEnabled,
            onInputChange = onInputChange,
            onSend = { onSendMessage(null) },
            onTriggerMention = onTriggerAtMention
        )
    }
}

@Composable
private fun MissingApiKeyCard(
    isCopilotDisabled: Boolean,
    onNavigateToProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCopilotDisabled) "Copilot Desactivado" else "OpenRouter Key Requerida",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = if (isCopilotDisabled)
                        "Activa NotiVas Copilot en tus ajustes de Perfil."
                    else
                        "Ingresa tu token de OpenRouter en Perfil para activar consultas contextuales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onNavigateToProfile,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Ajustes", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private data class SuggestionItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
)

@Composable
private fun CopilotEmptyState(
    coursesCount: Int,
    currentModel: String,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val modelLabel = remember(currentModel) {
        when {
            currentModel.contains("gemini") -> "Gemini 2.5 Flash"
            currentModel.contains("claude") -> "Claude 3.5 Haiku"
            currentModel.contains("gpt-4o") -> "GPT-4o Mini"
            else -> currentModel.split("/").lastOrNull() ?: currentModel
        }
    }

    val suggestions = remember {
        listOf(
            SuggestionItem(
                icon = Icons.Default.DateRange,
                title = "¿Qué tareas tengo pendientes de entrega esta semana?"
            ),
            SuggestionItem(
                icon = Icons.AutoMirrored.Filled.Assignment,
                title = "¿Cuáles son las consignas y rúbricas de mi próxima tarea?"
            ),
            SuggestionItem(
                icon = Icons.Default.Calculate,
                title = "Crea un grupo de 'Laboratorios' con 30% en el simulador"
            ),
            SuggestionItem(
                icon = Icons.Default.Grade,
                title = "¿Cómo están mis notas y calificaciones en todos mis cursos?"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NotiVas Copilot",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "Modelo: $modelLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "$coursesCount cursos sincronizados",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            text = "Asistente universitario conectado en vivo con tus tareas, rúbricas de Canvas LMS y tu simulador de notas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Sugerencias rápidas",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        suggestions.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSuggestionClick(item.title) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun CopilotMessageBubble(message: CopilotMessageItem) {
    val isUser = message.role == CopilotRole.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .wrapContentWidth(if (isUser) Alignment.End else Alignment.Start),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .padding(top = 4.dp, end = 8.dp)
                        .size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Surface(
                shape = if (isUser) {
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 4.dp
                    )
                } else {
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 18.dp
                    )
                },
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                border = if (!isUser) {
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                } else null,
                modifier = Modifier.wrapContentSize()
            ) {
                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current

                Column(modifier = Modifier.padding(14.dp)) {
                    SelectionContainer {
                        Text(
                            text = remember(message.text) { formatMarkdown(message.text) },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp
                            ),
                            color = if (isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    // Action feedback badge (e.g. Simulation group created)
                    if (!message.actionFeedback.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = message.actionFeedback,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Consulted sources accordion
                    if (message.sources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SourcesAccordion(sources = message.sources)
                    }

                    // Quick copy button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(message.text))
                                Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    tint = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Copiar",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcesAccordion(sources: List<CopilotSource>) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Source,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Fuentes consultadas (${sources.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sources.forEach { source ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = source.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = source.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopilotLoadingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 4.dp,
                bottomEnd = 18.dp
            ),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Consultando Canvas LMS y analizando...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CopilotInputBar(
    inputText: String,
    isLoading: Boolean,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onTriggerMention: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep internal TextFieldValue synchronized with incoming inputText while placing cursor at the end
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(inputText, selection = TextRange(inputText.length)))
    }

    LaunchedEffect(inputText) {
        if (textFieldValue.text != inputText) {
            textFieldValue = TextFieldValue(
                text = inputText,
                selection = TextRange(inputText.length)
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    // Quick '@' mention button inside the input capsule
                    IconButton(
                        onClick = {
                            onTriggerMention()
                            // Immediately position cursor after newly appended '@'
                            val newLen = if (inputText.isEmpty() || inputText.endsWith(" ")) {
                                inputText.length + 1
                            } else {
                                inputText.length + 2
                            }
                            textFieldValue = textFieldValue.copy(selection = TextRange(newLen))
                        },
                        enabled = enabled && !isLoading,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "@",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    TextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            onInputChange(newValue.text)
                        },
                        placeholder = {
                            Text(
                                text = if (enabled)
                                    "Escribe '@' para elegir curso o recurso..."
                                else
                                    "Configura tu API Key en Perfil para consultar...",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        singleLine = false,
                        maxLines = 4,
                        enabled = enabled && !isLoading
                    )
                }
            }

            FloatingActionButton(
                onClick = onSend,
                shape = CircleShape,
                containerColor = if (inputText.isNotBlank() && enabled && !isLoading) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (inputText.isNotBlank() && enabled && !isLoading) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Parses markdown text into an [AnnotatedString] supporting:
 * - Bullet lists (`* ` or `- `) converted to clean bullet dots (`• `)
 * - Markdown headers (`#`, `##`, `###`) rendered in bold
 * - Bold text (`**text**`) rendered with [FontWeight.Bold]
 * - Inline code (`` `code` ``) rendered with [FontFamily.Monospace]
 * - Italic text (`*text*`) rendered with [FontStyle.Italic]
 */
private fun formatMarkdown(text: String): AnnotatedString {
    val lines = text.split("\n")
    return buildAnnotatedString {
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            if (line.trim().startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true
                    codeBlockLines.clear()
                } else {
                    inCodeBlock = false
                    // Render accumulated code block
                    val codeContent = codeBlockLines.joinToString("\n")
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            background = Color(0x33888888)
                        )
                    ) {
                        append("  $codeContent  \n")
                    }
                }
                return@forEachIndexed
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                return@forEachIndexed
            }

            var formattedLine = line
            var isHeader = false

            // Headers
            if (formattedLine.startsWith("### ")) {
                formattedLine = formattedLine.removePrefix("### ")
                isHeader = true
            } else if (formattedLine.startsWith("## ")) {
                formattedLine = formattedLine.removePrefix("## ")
                isHeader = true
            } else if (formattedLine.startsWith("# ")) {
                formattedLine = formattedLine.removePrefix("# ")
                isHeader = true
            }

            // Bullet points (* or - followed by whitespace)
            val bulletRegex = Regex("^(\\s*)([*-])\\s+(.*)$")
            val bulletMatch = bulletRegex.find(formattedLine)
            val leadingIndent: String
            val lineContent: String
            if (bulletMatch != null) {
                leadingIndent = bulletMatch.groupValues[1]
                lineContent = bulletMatch.groupValues[3]
                append("$leadingIndent• ")
            } else {
                lineContent = formattedLine
            }

            if (isHeader) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInlineMarkdown(lineContent)
                }
            } else {
                appendInlineMarkdown(lineContent)
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }

        // Handle unclosed code block if any
        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            val codeContent = codeBlockLines.joinToString("\n")
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    background = Color(0x33888888)
                )
            ) {
                append("  $codeContent  ")
            }
        }
    }
}

/**
 * Parses inline formatting: bold (**...**), inline code (`...`), and italic (*...*).
 */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineMarkdown(content: String) {
    // Regex matching bold (**...**), code (`...`), or italic (*...*)
    val inlinePattern = Regex("(\\*\\*([^*]+)\\*\\*)|(`([^`]+)`)|(\\*([^*]+)\\*)")
    var lastIndex = 0

    for (match in inlinePattern.findAll(content)) {
        // Append text before match
        if (match.range.first > lastIndex) {
            append(content.substring(lastIndex, match.range.first))
        }

        when {
            // Bold: **text**
            match.groups[2] != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groups[2]!!.value)
                }
            }
            // Code: `code`
            match.groups[4] != null -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x22888888)
                    )
                ) {
                    append(" ${match.groups[4]!!.value} ")
                }
            }
            // Italic: *text*
            match.groups[6] != null -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groups[6]!!.value)
                }
            }
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < content.length) {
        append(content.substring(lastIndex))
    }
}

/**
 * Interactive Contextual @ Mention Popup.
 * Provides two cascade steps:
 * 1. COURSES: List of enrolled courses matching query.
 * 2. COURSE_RESOURCES: Direct access to assignments, forums, and full course query.
 */
@Composable
private fun CopilotMentionPopup(
    uiState: CopilotUiState,
    onSelectCourse: (com.notivas.data.model.Course) -> Unit,
    onApplyCourse: (com.notivas.data.model.Course) -> Unit,
    onApplyResource: (com.notivas.data.model.Course, String) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .heightIn(max = 280.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.mentionStep == MentionStep.COURSE_RESOURCES && uiState.activeMentionCourse != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a cursos",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.activeMentionCourse.name,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mencionar Curso o Recurso",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Content based on step
            if (uiState.mentionStep == MentionStep.COURSES) {
                val filteredCourses = remember(uiState.courses, uiState.mentionQuery) {
                    if (uiState.mentionQuery.isBlank()) {
                        uiState.courses
                    } else {
                        uiState.courses.filter {
                            it.name.contains(uiState.mentionQuery, ignoreCase = true) ||
                                    (it.courseCode?.contains(uiState.mentionQuery, ignoreCase = true) == true)
                        }
                    }
                }

                if (filteredCourses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron cursos para '${uiState.mentionQuery}'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredCourses, key = { it.id }) { course ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCourse(course) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!course.courseCode.isNullOrBlank()) {
                                        Text(
                                            text = course.courseCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Ver recursos",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // STEP 2: Course Resources (Assignments, Forums, Entire Course)
                val course = uiState.activeMentionCourse
                if (course != null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        // Option 1: Mention entire course
                        item(key = "entire_course") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onApplyCourse(course) },
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Consultar todo el curso",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Pregunta general sobre ${course.courseCode ?: course.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }

                        // Section: Assignments
                        if (uiState.courseAssignments.isNotEmpty()) {
                            item(key = "header_assignments") {
                                Text(
                                    text = "TAREAS Y LABORATORIOS (${uiState.courseAssignments.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp)
                                )
                            }

                            items(uiState.courseAssignments, key = { "assign_${it.id}" }) { assignment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onApplyResource(course, assignment.name) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = null,
                                        tint = if (assignment.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = assignment.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (assignment.pointsPossible != null || assignment.score != null) {
                                            Text(
                                                text = if (assignment.score != null)
                                                    "Nota: ${assignment.score}/${assignment.pointsPossible ?: 20} pts"
                                                else
                                                    "Puntos: ${assignment.pointsPossible} pts",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section: Forums and Discussions
                        val courseForums = uiState.coursePlannerItems.filter {
                            it.plannableType == "discussion_topic" ||
                                    it.plannable.title.contains("FORO", ignoreCase = true) ||
                                    it.plannable.title.contains("DEBATE", ignoreCase = true)
                        }

                        if (courseForums.isNotEmpty()) {
                            item(key = "header_forums") {
                                Text(
                                    text = "FOROS Y DEBATES (${courseForums.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp)
                                )
                            }

                            items(courseForums, key = { "forum_${it.plannableId}" }) { forum ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onApplyResource(course, forum.plannable.title) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = forum.plannable.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Section: Modules and Teacher Resources
                        if (uiState.courseModules.isNotEmpty()) {
                            item(key = "header_modules") {
                                Text(
                                    text = "MÓDULOS Y RECURSOS DEL DOCENTE (${uiState.courseModules.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp)
                                )
                            }

                            uiState.courseModules.forEach { module ->
                                val moduleItems = module.items ?: emptyList()
                                item(key = "mod_header_${module.id}") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onApplyResource(course, "Módulo: ${module.name}") }
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = module.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                items(moduleItems, key = { "item_${module.id}_${it.id}" }) { item ->
                                    val itemIcon = when (item.type.lowercase()) {
                                        "file" -> Icons.Default.AttachFile
                                        "page" -> Icons.Default.Description
                                        "externalurl" -> Icons.Default.Link
                                        "discussion" -> Icons.Default.Forum
                                        "assignment" -> Icons.AutoMirrored.Filled.Assignment
                                        else -> Icons.Default.Description
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onApplyResource(course, "${module.name}: ${item.title}") }
                                            .padding(start = 32.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = itemIcon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.type} • ${module.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopilotHistoryBottomSheet(
    savedSessions: List<com.notivas.data.model.CopilotSession>,
    currentSessionId: String?,
    courses: List<com.notivas.data.model.Course>,
    onSelectSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onDismiss: () -> Unit
) {
    var sessionToRename by remember { mutableStateOf<com.notivas.data.model.CopilotSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<com.notivas.data.model.CopilotSession?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Historial de Conversaciones",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onNewChat()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nuevo",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            if (savedSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No tienes conversaciones guardadas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Cada vez que chatees con Copilot se guardará aquí automáticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(savedSessions, key = { it.id }) { session ->
                        val isCurrent = session.id == currentSessionId
                        val course = courses.find { it.id == session.courseId }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCurrent)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onSelectSession(session.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isCurrent) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (course != null) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = course.courseCode ?: course.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        val dateFormat = remember {
                                            java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                                        }
                                        Text(
                                            text = dateFormat.format(java.util.Date(session.updatedAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )

                                        if (session.totalTokens > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                                            ) {
                                                Text(
                                                    text = "${session.totalTokens} tks",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Actions: Rename and Delete
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            sessionToRename = session
                                            renameInputText = session.title
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Renombrar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            sessionToDelete = session
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    sessionToRename?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = {
                Text(
                    text = "Renombrar Conversación",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("Título del chat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInputText.isNotBlank()) {
                            onRenameSession(session.id, renameInputText.trim())
                        }
                        sessionToRename = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "¿Eliminar este chat?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Se eliminará permanentemente la conversación '${session.title}' de tu dispositivo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSession(session.id)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}


