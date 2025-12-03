package com.app.whatsinside2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pantryapp.ScannerScreen

sealed class Screen(val route: String){
    object Home : Screen("home_screen")
    object Scanner : Screen("scanner_screen")
    object Details : Screen("details_screen/{barcode}"){
        fun createRoute(barcode: String) = "details_screen/$barcode"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsInside2() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mein Vorratsschrank") })
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(route = Screen.Home.route){
                HomeScreen(navController = navController)
            }

            composable(route = Screen.Scanner.route) {
                ScannerScreen(navController = navController)
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("barcode") { type = NavType.StringType })
            ) { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode")

                if(barcode != null) {
                    DetailsScreen(navController = navController, barcode = barcode)
                }
            }
        }
    }
}