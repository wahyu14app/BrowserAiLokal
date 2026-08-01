package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme

import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val browserViewModel: BrowserViewModel = viewModel()
            NavHost(navController = navController, startDestination = "browser") {
                composable("browser") {
                    BrowserScreen(viewModel = browserViewModel, navController = navController)
                }
                composable("tools") {
                    ToolsScreen(navController = navController)
                }
                composable("ai_management") {
                    AiManagementScreen(viewModel = browserViewModel, navController = navController)
                }
                composable("profiles") {
                    ProfilesScreen(navController = navController)
                }
                composable("settings") {
                    SettingsScreen(navController = navController)
                }
                composable("instructions") {
                    InstructionsScreen(navController = navController)
                }
            }
        }
      }
    }
  }
}
