package com.notivas.ui.navigation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.notivas.ui.navigation.Screen

@Composable
fun MainBottomNavBar(
    currentDestination: NavDestination?,
    isVisible: Boolean,
    items: List<Screen>,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(250)) { it },
        exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(200)) { it },
        modifier = modifier
    ) {
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        NavigationBar(
            modifier = Modifier.drawBehind {
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
                val selected = currentDestination?.hierarchy?.any {
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
                        val icon = when (screen) {
                            Screen.Dashboard ->
                                if (selected) Icons.Filled.Dashboard
                                else Icons.Outlined.Dashboard

                            Screen.Foros ->
                                if (selected) Icons.Filled.Forum
                                else Icons.Outlined.Forum

                            Screen.Notas ->
                                if (selected) Icons.Filled.Analytics
                                else Icons.Outlined.Analytics

                            Screen.Ananau ->
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
                        val label = when (screen) {
                            Screen.Dashboard -> "Inicio"
                            Screen.Foros -> "Foros"
                            Screen.Notas -> "Notas"
                            Screen.Ananau -> "Ananau"
                            Screen.Profile -> "Perfil"
                            else -> "Home"
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    selected = selected,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = { onNavigateToRoute(screen.route) }
                )
            }
        }
    }
}
