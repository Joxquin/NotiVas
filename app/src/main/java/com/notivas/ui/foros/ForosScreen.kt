package com.notivas.ui.foros

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notivas.data.model.PlannerItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForosScreen(
    forums: List<PlannerItem>,
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
            // MD3 Expressive Loading Indicator
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

            if (forums.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!isRefreshing) {
                            Text(
                                text = "No hay foros pendientes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LoadingIndicator()
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(forums) { forum ->
                        ForumCard(forum)
                    }
                }
            }
        }
    }
}

@Composable
fun ForumCard(forum: PlannerItem) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = forum.contextName ?: "Sin curso",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = forum.plannable.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val dateText = forum.plannableDate?.let {
                try {
                    val date = java.time.ZonedDateTime.parse(it).withZoneSameInstant(java.time.ZoneId.of("America/Lima"))
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")
                    date.format(formatter)
                } catch (e: Exception) { it }
            } ?: "Sin fecha"

            Text(
                text = "Vence: $dateText",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
