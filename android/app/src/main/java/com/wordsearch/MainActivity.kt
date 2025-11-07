package com.wordsearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
        Screen.Categories.route
    } else {
        Screen.Login.route
    }

    NavGraph(
        navController = navController,
        startDestination = startDestination
    )
}
