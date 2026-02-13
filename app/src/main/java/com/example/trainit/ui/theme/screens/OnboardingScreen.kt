package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.UserRepository
import com.example.trainit.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val authRepo = AuthRepository()
    val userRepo = UserRepository()

    var level by remember { mutableStateOf("beginner") }
    var goal by remember { mutableStateOf("muscle") }
    var daysPerWeek by remember { mutableIntStateOf(3) }
    var hasEquipment by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Configura tu perfil", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = level,
            onValueChange = { level = it; error = null },
            label = { Text("Nivel (beginner/intermediate/advanced)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it; error = null },
            label = { Text("Objetivo (muscle/fat_loss/strength)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = daysPerWeek.toString(),
            onValueChange = { v ->
                daysPerWeek = v.toIntOrNull()?.coerceIn(1, 7) ?: daysPerWeek
                error = null
            },
            label = { Text("Días por semana (1-7)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text("Tengo equipo")
        Switch(
            checked = hasEquipment,
            onCheckedChange = { hasEquipment = it; error = null }
        )

        Spacer(Modifier.height(12.dp))

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val uid = authRepo.currentUid()
                if (uid == null) {
                    error = "Sesión no válida. Vuelve a iniciar sesión."
                    return@Button
                }

                loading = true
                error = null

                val profile = UserProfile(
                    uid = uid,
                    level = level.trim(),
                    goal = goal.trim(),
                    daysPerWeek = daysPerWeek,
                    hasEquipment = hasEquipment
                )

                CoroutineScope(Dispatchers.Main).launch {
                    val result = userRepo.saveProfile(profile)
                    loading = false

                    result.onSuccess { onFinish() }
                        .onFailure { e -> error = e.message ?: "Error guardando perfil" }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
            }
            Text("Guardar y continuar")
        }
    }
}
