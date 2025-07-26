package me.nanova.summaryexpressive.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import me.nanova.summaryexpressive.ui.page.HistoryScreen
import me.nanova.summaryexpressive.ui.page.HomeScreen
import me.nanova.summaryexpressive.ui.page.OnboardingScreen
import me.nanova.summaryexpressive.ui.page.SettingsScreen
import me.nanova.summaryexpressive.vm.SummaryViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    initialUrl: String? = null,
    showOnboarding: Boolean
) {
    val viewModel: SummaryViewModel = hiltViewModel()
    val startDestination = if (showOnboarding) "onboarding" else "home"

    NavHost(navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(
                modifier = Modifier,
                navController = navController,
                initialUrl = initialUrl
            )
        }
        composable("onboarding") {
            OnboardingScreen(onDone = {
                viewModel.setShowOnboardingScreenValue(false)
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable(
            "settings",
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    animationSpec = tween(300, easing = EaseOut),
                    towards = AnimatedContentTransitionScope.SlideDirection.End
                )
            }
        ) {
            SettingsScreen(
                navController = navController
            )
        }
        composable(
            "history",
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    animationSpec = tween(300, easing = EaseOut),
                    towards = AnimatedContentTransitionScope.SlideDirection.End
                )
            }
        ) {
            HistoryScreen(
                navController = navController
            )
        }
    }
}