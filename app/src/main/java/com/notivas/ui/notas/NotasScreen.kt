package com.notivas.ui.notas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotasScreen(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val pullDistance = pullToRefreshState.distanceFraction

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = { } // Custom indicator follows finger progress
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // MD3 Expressive Loading Indicator (Same as Tareas/Dashboard)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (isRefreshing) 80.dp 
                        else (80.dp * pullDistance).coerceAtMost(100.dp)
                    )
                    .graphicsLayer {
                        alpha = if (isRefreshing) 1f else (pullDistance * 2).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    LoadingIndicator()
                } else if (pullDistance > 0.05f) {
                    LoadingIndicator(progress = { pullDistance.coerceIn(0f, 1f) })
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // UI requested: Column with centered LoadingIndicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Working...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
