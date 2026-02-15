package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    val user = remember { FirebaseAuth.getInstance().currentUser }

    val email = user?.email ?: "—"
    val uid = user?.uid ?: "—"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Cuenta", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Email: $email")
                Spacer(Modifier.height(6.dp))
                Text("UID: $uid", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Estado", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("• Login: OK (Firebase Auth)")
                Text("• Perfil: OK (Firestore users/{uid})")
                Text("• Workouts: OK (Firestore users/{uid}/workouts)")
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }

        Text(
            "Nota: más adelante puedes añadir edición de perfil (nivel, objetivo, días/semana, equipo).",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
