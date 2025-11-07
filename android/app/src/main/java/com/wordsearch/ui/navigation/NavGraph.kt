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
import com.wordsearch.ui.game.GameScreen
import com.wordsearch.ui.achievements.AchievementsScreen
import com.wordsearch.ui.leaderboard.LeaderboardScreen
import com.wordsearch.ui.challenge.DailyChallengeScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Categories : Screen("categories")
    object Game : Screen("game/{categoryId}") {
        fun createRoute(categoryId: String) = "game/$categoryId"
    }
    object Achievements : Screen("achievements")
    object Leaderboard : Screen("leaderboard")
    object DailyChallenge : Screen("daily_challenge")
    object Premium : Screen("premium")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
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
                onNavigateToGame = { categoryId ->
                    navController.navigate(Screen.Game.createRoute(categoryId))
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
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            GameScreen(
                categoryId = categoryId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGameComplete = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Achievements.route) {
            AchievementsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.DailyChallenge.route) {
            DailyChallengeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGame = { attemptId, challengeId ->
                    navController.navigate(Screen.Game.createRoute(challengeId))
                }
            )
        }
    }
}
