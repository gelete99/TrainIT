package com.example.trainit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.trainit.nav.AppNavGraph
import com.example.trainit.nav.Routes
import com.example.trainit.ui.components.AppBottomBar
import com.example.trainit.ui.theme.TrainITTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrainITTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val mainRoutes = setOf(
        Routes.Home.route,
        Routes.Plan.route,
        Routes.History.route,
        Routes.Profile.route
    )

    val showBottomBar = currentRoute in mainRoutes

    // FAB visible solo en Home + History (puedes añadir Plan si quieres)
    val showFab = currentRoute in setOf(
        Routes.Home.route,
        Routes.History.route
        // Routes.Plan.route  // <- descomenta si también lo quieres en Plan
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) AppBottomBar(navController)
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.LogWorkout.route) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Registrar entrenamiento"
                    )
                }
            }
        }
    ) { paddingValues ->
        AppNavGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
