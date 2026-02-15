package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlanScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Plan", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Tu plan semanal", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aquí verás tu plan generado en base a tu perfil y tu historial de entrenos."
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Estado", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("• Perfil: OK (Onboarding)")
                Text("• Historial: OK (Workouts en Firestore)")
                Text("• IA: pendiente (Cloud Function + OpenAI)")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Próximo paso", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cuando activemos IA, se generará un plan en JSON y lo renderizaremos en tarjetas por día."
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Botón preparado para el futuro (por ahora desactivado)
        Button(
            onClick = { /* IA después */ },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generar plan con IA (próximamente)")
        }

        Text(
            "Tip: para la demo, esta pantalla ya explica el flujo. Luego solo activamos el botón y renderizamos el JSON.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
