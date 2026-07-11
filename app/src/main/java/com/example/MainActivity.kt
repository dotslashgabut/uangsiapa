package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddEditScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AddEditViewModel
import com.example.ui.viewmodel.AddEditViewModelFactory
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory
import com.example.ui.viewmodel.ReportViewModel
import com.example.ui.viewmodel.ReportViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }
            
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneyTrackerAppUI(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}

@Composable
fun MoneyTrackerAppUI(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as MoneyTrackerApp
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(app.repository))
            HomeScreen(
                viewModel = mainViewModel,
                isDark = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onNavigateToAddEdit = { id ->
                    if (id == null) {
                        navController.navigate("add_edit/-1")
                    } else {
                        navController.navigate("add_edit/$id")
                    }
                },
                onNavigateToReport = {
                    navController.navigate("report")
                }
            )
        }
        
        composable(
            route = "add_edit/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("transactionId") ?: -1
            val viewId = if (id == -1) null else id
            val addEditViewModel: AddEditViewModel = viewModel(factory = AddEditViewModelFactory(app.repository))
            
            AddEditScreen(
                viewModel = addEditViewModel,
                transactionId = viewId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("report") {
            val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(app.repository))
            ReportScreen(
                viewModel = reportViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
