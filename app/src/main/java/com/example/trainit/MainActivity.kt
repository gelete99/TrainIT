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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.trainit.nav.AppNavGraph
import com.example.trainit.nav.Routes
import com.example.trainit.ui.components.AppBottomBar
import com.example.trainit.ui.theme.TrainITTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.delay


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

    val showFab = currentRoute in setOf(
        Routes.Home.route,
        Routes.History.route
    )

    // ✅ Snackbar global (compatible)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            // Lanza el snackbar en una corrutina separada (porque showSnackbar suspende)
            launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Indefinite
                )
            }

            // Cierra tras 1 segundo
            delay(1200)
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { if (showBottomBar) AppBottomBar(navController) },
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
            modifier = Modifier.padding(paddingValues),
            showMessage = showMessage
        )
    }
}
