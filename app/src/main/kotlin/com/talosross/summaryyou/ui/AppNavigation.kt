package com.talosross.summaryyou.ui

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.talosross.summaryyou.ui.page.HistoryScreen
import com.talosross.summaryyou.ui.page.HomeScreen
import com.talosross.summaryyou.ui.page.OnboardingScreen
import com.talosross.summaryyou.ui.page.SettingsScreen
import com.talosross.summaryyou.vm.AppViewModel

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
    startDestination: Nav,
    appViewModel: AppViewModel,
) {

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Nav.Home> {
            HomeScreen(
                modifier = Modifier,
                onNav = { dest -> navController.navigate(dest) },
                appViewModel = appViewModel
            )
        }

        composable<Nav.Onboarding> {
            fun handleOnboardingDone() {
                appViewModel.setIsOnboarded(true)
                navController.navigate(Nav.Home) {
                    popUpTo<Nav.Onboarding> { inclusive = true }
                }
            }

            val settings by appViewModel.settingsUiState.collectAsState()

            OnboardingScreen(
                onDone = {
                    handleOnboardingDone()
                },
                onDoneAndNavigate = { route ->
                    handleOnboardingDone()
                    navController.navigate(route)
                },
                hasProxy = settings.hasProxy
            )
        }

        composable<Nav.Settings>(
            enterTransition = slideIn(SlideDirection.End),
            exitTransition = slideOut(SlideDirection.Start),
        ) { backStackEntry ->
            val settingsRoute = backStackEntry.toRoute<Nav.Settings>()
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNav = { navController.navigate(it) },
                highlightSection = settingsRoute.highlight,
                appViewModel = appViewModel
            )
        }

        composable<Nav.History>(
            enterTransition = slideIn(SlideDirection.Start),
            exitTransition = slideOut(SlideDirection.End)
        ) {
            HistoryScreen()
        }
    }
}