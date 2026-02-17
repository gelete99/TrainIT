package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.WorkoutRepository
import com.example.trainit.data.model.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkoutScreen(
    onSaved: (() -> Unit)? = null
) {
    val authRepo = AuthRepository()
    val workoutRepo = WorkoutRepository()
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf("") }
    var durationMinText by remember { mutableStateOf("45") }
    var notes by remember { mutableStateOf("") }

    val rpeOptions = (0..10).toList()
    var rpe by remember { mutableIntStateOf(5) }
    var rpeExpanded by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Registrar entrenamiento", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = type,
            onValueChange = { type = it; error = null },
            label = { Text("Tipo (ej: Push, Cardio, Full body)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = durationMinText,
            onValueChange = { durationMinText = it.filter(Char::isDigit); error = null },
            label = { Text("Duración (min)") },
            modifier = Modifier.fillMaxWidth()
        )

        // RPE (dropdown 0..10)
        ExposedDropdownMenuBox(
            expanded = rpeExpanded,
            onExpandedChange = { rpeExpanded = !rpeExpanded }
        ) {
            OutlinedTextField(
                value = rpe.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Esfuerzo percibido (RPE 0-10)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rpeExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )

            ExposedDropdownMenu(
                expanded = rpeExpanded,
                onDismissRequest = { rpeExpanded = false }
            ) {
                rpeOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.toString()) },
                        onClick = {
                            rpe = opt
                            rpeExpanded = false
                            error = null
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it; error = null },
            label = { Text("Notas (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val uid = authRepo.currentUid()
                if (uid == null) {
                    error = "Sesión no válida."
                    return@Button
                }

                val cleanType = type.trim()
                val duration = durationMinText.toIntOrNull() ?: 0

                if (cleanType.isBlank()) {
                    error = "Escribe un tipo de entrenamiento."
                    return@Button
                }
                if (duration <= 0) {
                    error = "Duración inválida."
                    return@Button
                }

                loading = true
                error = null

                val workout = Workout(
                    type = cleanType,
                    durationMin = duration,
                    rpe = rpe,
                    notes = notes.trim()
                )

                scope.launch(Dispatchers.Main) {
                    val res = workoutRepo.addWorkout(uid, workout)
                    loading = false
                    res.onSuccess { onSaved?.invoke() }
                        .onFailure { e -> error = e.message ?: "Error guardando entreno" }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text("Guardar")
        }
    }
}
