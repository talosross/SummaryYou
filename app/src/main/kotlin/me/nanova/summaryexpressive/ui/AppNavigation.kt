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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    startDestination: Nav,
    uiViewModel: UIViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = startDestination.name) {
        composable(Nav.Home.name) {
            HomeScreen(
                modifier = Modifier,
                onNav = { navController.navigate(it.name) }
            )
        }

        composable(Nav.Onboarding.name) {
            fun handleOnboardingDone() {
                uiViewModel.setIsOnboarded(true)
                navController.navigate(Nav.Home.name) {
                    popUpTo(Nav.Onboarding.name) { inclusive = true }
                }
            }

            OnboardingScreen(
                onDone = {
                    handleOnboardingDone()
                },
                onDoneAndNavigate = {
                    handleOnboardingDone()
                    navController.navigate(it)
                }
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
        ) { backStackEntry ->
            val highlightSection = backStackEntry.arguments?.getString("highlight")
            SettingsScreen(
                onBack = { navController.navigateUp() },
                onNav = { navController.navigate(it.name) },
                highlightSection = highlightSection
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