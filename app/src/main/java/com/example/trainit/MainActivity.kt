package com.example.trainit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.rememberNavController
import com.example.trainit.nav.AppNavGraph
import com.example.trainit.ui.theme.TrainITTheme
import androidx.navigation.compose.currentBackStackEntryAsState


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

    val showBottomBar = currentRoute in setOf(
        com.example.trainit.nav.Routes.Home.route,
        com.example.trainit.nav.Routes.Plan.route,
        com.example.trainit.nav.Routes.History.route,
        com.example.trainit.nav.Routes.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                com.example.trainit.ui.components.AppBottomBar(navController)
            }
        }
    ) { paddingValues ->
        AppNavGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
