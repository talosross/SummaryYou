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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import me.nanova.summaryexpressive.ui.page.HistoryScreen
import me.nanova.summaryexpressive.ui.page.HomeScreen
import me.nanova.summaryexpressive.ui.page.OnboardingScreen
import me.nanova.summaryexpressive.ui.page.SettingsScreen
import me.nanova.summaryexpressive.vm.HistoryViewModel
import me.nanova.summaryexpressive.vm.UIViewModel

private fun slideIn(dir: SlideDirection): AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideIntoContainer(
        animationSpec = tween(300, easing = EaseIn),
        towards = dir
    )
}

private fun slideOut(dir: SlideDirection): AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutOfContainer(
        animationSpec = tween(300, easing = EaseOut),
        towards = dir
    )
}

@Composable
fun AppNavigation(
    navController: NavHostController,
) {
    val uiViewModel: UIViewModel = hiltViewModel()
    val settings by uiViewModel.settingsUiState.collectAsState()
    val startDestination = if (settings.showOnboarding) Nav.Onboarding else Nav.Home

    NavHost(navController, startDestination = startDestination.name) {
        composable(Nav.Home.name) {
            HomeScreen(
                modifier = Modifier,
                navController = navController,
                uiViewModel = uiViewModel,
                summaryViewModel = hiltViewModel()
            )
        }

        composable(Nav.Onboarding.name) {
            OnboardingScreen(
                onDone = {
                    uiViewModel.setShowOnboardingScreenValue(false)
                    navController.navigate(Nav.Home.name) {
                        popUpTo(Nav.Onboarding.name) { inclusive = true }
                    }
                },
                navController = navController,
            )
        }

        composable(
            route = "${Nav.Settings.name}?highlight={highlight}",
            enterTransition = slideIn(SlideDirection.End),
            exitTransition = slideOut(SlideDirection.Start),
            arguments = listOf(navArgument("highlight") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            SettingsScreen(
                navController = navController,
                viewModel = uiViewModel
            )
        }

        composable(
            Nav.History.name,
            enterTransition = slideIn(SlideDirection.Start),
            exitTransition = slideOut(SlideDirection.End)
        ) {
            HistoryScreen(viewModel = hiltViewModel<HistoryViewModel>())
        }
    }
}