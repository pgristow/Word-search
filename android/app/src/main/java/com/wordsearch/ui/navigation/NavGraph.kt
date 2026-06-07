package com.wordsearch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wordsearch.ui.auth.LoginScreen
import com.wordsearch.ui.auth.RegisterScreen
import com.wordsearch.ui.categories.CategoriesScreen
import com.wordsearch.ui.mode.ModeSelectionScreen
import com.wordsearch.ui.game.GameScreen
import com.wordsearch.ui.achievements.AchievementsScreen
import com.wordsearch.ui.leaderboard.LeaderboardScreen
import com.wordsearch.ui.challenge.DailyChallengeScreen
import com.wordsearch.ui.league.LeagueScreen
import com.wordsearch.ui.store.StoreScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Categories : Screen("categories")
    object ModeSelection : Screen("mode_selection/{categoryId}/{categoryName}") {
        fun createRoute(categoryId: String, categoryName: String) = "mode_selection/$categoryId/$categoryName"
    }
    object Game : Screen("game/{categoryId}/{gameMode}") {
        fun createRoute(categoryId: String, gameMode: String) = "game/$categoryId/$gameMode"
    }
    object Achievements : Screen("achievements")
    object Leaderboard : Screen("leaderboard")
    object League : Screen("league")
    object DailyChallenge : Screen("daily_challenge")
    object Premium : Screen("premium")
    object Store : Screen("store")
    object Settings : Screen("settings")
    object Credits : Screen("credits")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    // Shared handler for the persistent bottom navigation bar: jump to a primary
    // destination, keeping Categories as the base so the back stack doesn't grow.
    val onBottomNav: (com.wordsearch.ui.common.BottomDest) -> Unit = { dest ->
        val route = when (dest) {
            com.wordsearch.ui.common.BottomDest.DAILY -> Screen.DailyChallenge.route
            com.wordsearch.ui.common.BottomDest.CATEGORIES -> Screen.Categories.route
            com.wordsearch.ui.common.BottomDest.ACHIEVEMENTS -> Screen.Achievements.route
            com.wordsearch.ui.common.BottomDest.LEADERBOARD -> Screen.Leaderboard.route
        }
        navController.navigate(route) {
            popUpTo(Screen.Categories.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Categories.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Categories.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                onNavigateToGame = { categoryId, categoryName ->
                    navController.navigate(Screen.ModeSelection.createRoute(categoryId, categoryName))
                },
                onNavigateToAchievements = {
                    navController.navigate(Screen.Achievements.route)
                },
                onNavigateToLeaderboard = {
                    navController.navigate(Screen.Leaderboard.route)
                },
                onNavigateToDailyChallenge = {
                    navController.navigate(Screen.DailyChallenge.route)
                },
                onNavigateToStore = {
                    navController.navigate(Screen.Store.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ModeSelection.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            ModeSelectionScreen(
                categoryId = categoryId,
                categoryName = categoryName,
                onModeSelected = { gameMode ->
                    navController.navigate(Screen.Game.createRoute(categoryId, gameMode.name))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            com.wordsearch.ui.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCredits = { navController.navigate(Screen.Credits.route) }
            )
        }

        composable(Screen.Credits.route) {
            com.wordsearch.ui.credits.CreditsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("gameMode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val gameMode = backStackEntry.arguments?.getString("gameMode") ?: "CLASSIC"
            GameScreen(
                categoryId = categoryId,
                gameMode = gameMode,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGameComplete = {
                    // Main Menu: return all the way to the categories home.
                    navController.popBackStack(Screen.Categories.route, inclusive = false)
                },
                onBottomNav = onBottomNav
            )
        }

        composable(Screen.Achievements.route) {
            AchievementsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onBottomNav = onBottomNav
            )
        }

        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onBottomNav = onBottomNav
            )
        }

        composable(Screen.DailyChallenge.route) {
            DailyChallengeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onBottomNav = onBottomNav,
                onNavigateToGame = { attemptId, challengeId ->
                    navController.navigate(Screen.Game.createRoute(challengeId, "CHALLENGE"))
                }
            )
        }

        composable(Screen.League.route) {
            LeagueScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onBottomNav = onBottomNav
            )
        }

        composable(Screen.Store.route) {
            StoreScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onBottomNav = onBottomNav
            )
        }
    }
}
