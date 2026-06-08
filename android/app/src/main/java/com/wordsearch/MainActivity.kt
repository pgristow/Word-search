package com.wordsearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.wordsearch.ui.auth.AuthViewModel
import com.wordsearch.ui.navigation.NavGraph
import com.wordsearch.ui.navigation.Screen
import com.wordsearch.ui.theme.WordSearchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WordSearchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WordSearchApp()
                }
            }
        }
    }
}

@Composable
fun WordSearchApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    // Determine start destination based on login status
    val startDestination = if (authViewModel.isLoggedIn()) {
        Screen.Landing.route
    } else {
        Screen.Login.route
    }

    // Auto-logout: if the server rejects our saved token, bounce to login instead of
    // getting stuck on a generic "failed to fetch" error.
    LaunchedEffect(Unit) {
        authViewModel.sessionExpired.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavGraph(
        navController = navController,
        startDestination = startDestination
    )
}
