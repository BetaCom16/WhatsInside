package com.app.whatsinside2

import android.R.attr.icon
import android.R.attr.label
import android.net.http.SslCertificate.saveState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.whatsinside2.ScannerScreen

sealed class Screen(val route: String){
    object Home : Screen("home_screen")
    object Scanner : Screen("scanner_screen")
    object Statistics : Screen("statistics_screen")

    // Wenn ID vorhanden, wird das Produkt bearbeitet. Ansonsten wird neu gescannt
    object Details : Screen("details_screen/{barcode}?id={id}"){
        fun createRoute(barcode: String, id: Int? = null): String {
            return if (id != null) {
                "details_screen/$barcode?id=$id"
            } else{
                "details_screen/$barcode"
            }
        }
    }
}

// Für die Einträge in der NavBar
data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsInside2() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Die Einträge in der NavBar unten am Bildschirm
    val bottomNavItems = listOf(
        BottomNavItem("Vorrat", Screen.Home.route, Icons.Default.Home),
        BottomNavItem("Neu", Screen.Scanner.route, Icons.Default.AddCircle),
        BottomNavItem("Statistik", Screen.Statistics.route, Icons.Default.BarChart)
    )

    // Zeigt die NavBar nur auf den Hauptseiten an - nicht beim Scannen oder in den Produktdetails
    val showBottomBar = currentDestination?.route in listOf(Screen.Home.route, Screen.Statistics.route)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mein Vorratsschrank") }) },
        bottomBar = {
            if(showBottomBar){
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        // Prüft, ob der der Tab gerade aktiv ist
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if(item.route == Screen.Scanner.route) {
                                    navController.navigate(item.route)
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        icon = { Icon(item.icon, contentDescription = item.name) },
                        label = { Text(item.name)}
                        )
                    }
                }
            }
        }

    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // HomeScreen
            composable(route = Screen.Home.route){
                HomeScreen(navController = navController)
            }

            // ScannerScreen
            composable(route = Screen.Scanner.route) {
                ScannerScreen(navController = navController)
            }

            // Screen für Statistik und Rezepte
            composable(route = Screen.Statistics.route){
                StatisticsScreen(navController = navController)
            }

            // Screen für Produktdetails
            composable(
                route = Screen.Details.route,
                arguments = listOf(
                    navArgument("barcode") { type = NavType.StringType },
                    navArgument("id"){
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode")
                val id = backStackEntry.arguments?.getInt("id") ?: -1

                if(barcode != null) {
                    DetailsScreen(navController = navController, barcode = barcode, productId = id)
                }
            }
        }
    }
}
