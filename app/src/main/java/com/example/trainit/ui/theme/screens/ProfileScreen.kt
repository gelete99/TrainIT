package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trainit.auth.AuthRepository

@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    val repo = AuthRepository()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            repo.logout()
            onLogout()
        }) {
            Text("Cerrar sesión")
        }
    }
}
