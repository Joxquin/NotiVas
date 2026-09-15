package com.notivas.ui.Ananau.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notivas.data.repository.AnanauSource
import com.notivas.ui.Ananau.AnanauMessageItem
import com.notivas.ui.Ananau.AnanauRole

@Composable
fun AnanauMessageBubble(
    message: AnanauMessageItem,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == AnanauRole.USER

    Column(
        modifier = modifier.fillMaxWidth(),
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
fun SourcesAccordion(sources: List<AnanauSource>) {
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

/**
 * Parses markdown text into an [AnnotatedString]
 */
fun formatMarkdown(text: String): AnnotatedString {
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

private fun AnnotatedString.Builder.appendInlineMarkdown(content: String) {
    val inlinePattern = Regex("(\\*\\*([^*]+)\\*\\*)|(`([^`]+)`)|(\\*([^*]+)\\*)")
    var lastIndex = 0

    for (match in inlinePattern.findAll(content)) {
        if (match.range.first > lastIndex) {
            append(content.substring(lastIndex, match.range.first))
        }

        when {
            match.groups[2] != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groups[2]!!.value)
                }
            }
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
