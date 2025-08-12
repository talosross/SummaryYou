package me.nanova.summaryexpressive.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
) {
    val viewModel: SummaryViewModel = hiltViewModel()
    val showOnboarding by viewModel.showOnboardingScreen.collectAsState()
    val startDestination = if (showOnboarding) Nav.Onboarding else Nav.Home

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

    NavHost(navController, startDestination = startDestination.name) {
        composable(Nav.Home.name) {
            HomeScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel
            )
        }
        composable(Nav.Onboarding.name) {
            OnboardingScreen(onDone = {
                viewModel.setShowOnboardingScreenValue(false)
                navController.navigate(Nav.Home.name) {
                    popUpTo(Nav.Onboarding.name) { inclusive = true }
                }
            })
        }
        composable(
            Nav.Settings.name,
            enterTransition = slideIn(SlideDirection.End),
            exitTransition = slideOut(SlideDirection.Start)
        ) {
            SettingsScreen(
                navController = navController
            )
        }
        composable(
            Nav.History.name,
            enterTransition = slideIn(SlideDirection.Start),
            exitTransition = slideOut(SlideDirection.End)
        ) {
            HistoryScreen()
        }
    }
}