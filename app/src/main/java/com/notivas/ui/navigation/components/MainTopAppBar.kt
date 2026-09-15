package com.notivas.ui.navigation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination
import com.notivas.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    currentDestination: NavDestination?,
    isVisible: Boolean,
    isScrolled: Boolean,
    modifier: Modifier = Modifier
) {
    val topAppBarContainerColor by animateColorAsState(
        targetValue = if (isScrolled) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(durationMillis = 250),
        label = "topAppBarContainerColor"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(250)) { -it },
        exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(200)) { -it },
        modifier = modifier
    ) {
        val title = when (currentDestination?.route) {
            Screen.Profile.route -> "Mi Perfil"
            Screen.Notas.route -> "Notas"
            Screen.Ananau.route -> "Ananau AI"
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = topAppBarContainerColor,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
