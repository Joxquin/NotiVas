package com.notivas.ui.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAP_TARGET = 8
private const val TAP_RESET_WINDOW_MS = 3_000L

@Composable
fun AppInfoFooter(
    versionName: String,
    versionCode: Int,
    architecture: String,
    onNavigateToDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var firstTapTime by remember { mutableLongStateOf(0L) }

    val progress by animateFloatAsState(
        targetValue = if (tapCount > 0) tapCount / TAP_TARGET.toFloat() else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "debug_progress"
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            tapCount >= TAP_TARGET -> MaterialTheme.colorScheme.error
            tapCount >= TAP_TARGET / 2 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 300),
        label = "debug_progress_color"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                val now = System.currentTimeMillis()
                if (tapCount == 0 || now - firstTapTime > TAP_RESET_WINDOW_MS) {
                    firstTapTime = now
                    tapCount = 1
                } else {
                    tapCount++
                }
                if (tapCount >= TAP_TARGET) {
                    tapCount = 0
                    firstTapTime = 0L
                    onNavigateToDebug()
                }
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "NotiVas",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = "v$versionName ($versionCode) · $architecture",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}
