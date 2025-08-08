package me.nanova.summaryexpressive.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
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
    showOnboarding: Boolean,
) {
    val viewModel: SummaryViewModel = hiltViewModel()
    val startDestination = if (showOnboarding) "onboarding" else "home"
    fun slideIn(dir: SlideDirection): AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideIntoContainer(
            animationSpec = tween(300, easing = EaseIn),
            towards = dir
        )
    }

    fun slideOut(dir: SlideDirection): AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutOfContainer(
            animationSpec = tween(300, easing = EaseOut),
            towards = dir
        )
    }
    NavHost(navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel
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
            enterTransition = slideIn(SlideDirection.End),
            exitTransition = slideOut(SlideDirection.Start)
        ) {
            SettingsScreen(
                navController = navController
            )
        }
        composable(
            "history",
            enterTransition = slideIn(SlideDirection.Start),
            exitTransition = slideOut(SlideDirection.End)
        ) {
            HistoryScreen()
        }
    }
}