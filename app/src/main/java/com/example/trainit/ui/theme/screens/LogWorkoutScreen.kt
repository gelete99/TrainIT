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
import com.example.trainit.data.WorkoutRepository
import com.example.trainit.data.model.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LogWorkoutScreen(
    onSaved: (() -> Unit)? = null // opcional: si luego quieres volver a History/Home
) {
    val authRepo = AuthRepository()
    val workoutRepo = WorkoutRepository()

    var type by remember { mutableStateOf("") }
    var durationMin by remember { mutableIntStateOf(45) }
    var notes by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Registrar entrenamiento", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = type,
            onValueChange = { type = it; error = null; success = null },
            label = { Text("Tipo (ej: Push, Full body, Cardio)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = durationMin.toString(),
            onValueChange = { v ->
                val parsed = v.toIntOrNull()
                if (parsed != null) durationMin = parsed.coerceIn(1, 300)
                error = null
                success = null
            },
            label = { Text("Duración (min)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it; error = null; success = null },
            label = { Text("Notas (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(12.dp))

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        success?.let { Text(it) }

        Spacer(Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            onClick = {
                val uid = authRepo.currentUid()
                if (uid == null) {
                    error = "Sesión no válida. Vuelve a iniciar sesión."
                    return@Button
                }

                val cleanType = type.trim()
                if (cleanType.isBlank()) {
                    error = "Escribe un tipo de entrenamiento."
                    return@Button
                }

                loading = true
                error = null
                success = null

                val workout = Workout(
                    type = cleanType,
                    durationMin = durationMin,
                    notes = notes.trim()
                )

                CoroutineScope(Dispatchers.Main).launch {
                    val result = workoutRepo.addWorkout(uid, workout)
                    loading = false

                    result.onSuccess {
                        success = "Entrenamiento guardado ✅"
                        type = ""
                        notes = ""
                        durationMin = 45
                        onSaved?.invoke()
                    }.onFailure { e ->
                        error = e.message ?: "Error guardando entrenamiento"
                    }
                }
            }
        ) {
            if (loading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
            }
            Text("Guardar")
        }
    }
}
