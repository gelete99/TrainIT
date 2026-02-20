package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.UserRepository
import com.example.trainit.data.model.UserProfile
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    showMessage: (String) -> Unit,
    onLogout: () -> Unit
) {
    val authRepo = AuthRepository()
    val userRepo = UserRepository()
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Objetivo
    var editGoalOpen by remember { mutableStateOf(false) }
    var newGoal by remember { mutableStateOf("") }
    var savingGoal by remember { mutableStateOf(false) }

    // Datos básicos
    var editBasicsOpen by remember { mutableStateOf(false) }
    var savingBasics by remember { mutableStateOf(false) }

    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }

    val levels = listOf("principiante", "intermedio", "avanzado")
    var selectedLevel by remember { mutableStateOf(levels.first()) }

    LaunchedEffect(Unit) {
        val uid = authRepo.currentUid()
        if (uid == null) {
            loading = false
            error = "Sesión no válida."
            return@LaunchedEffect
        }

        val res = userRepo.getProfile(uid)
        loading = false

        res.onSuccess {
            profile = it
            newGoal = it.goal

            heightText = it.heightCm.takeIf { v -> v > 0 }?.toString() ?: ""
            weightText = it.weightKg.takeIf { v -> v > 0 }?.toString() ?: ""
            ageText = it.age.takeIf { v -> v > 0 }?.toString() ?: ""
            selectedLevel = it.level.ifBlank { "principiante" }
        }.onFailure {
            error = it.message ?: "Error cargando perfil"
        }
    }

    // -----------------------------
    // Dialog Cambiar objetivo
    // -----------------------------
    if (editGoalOpen) {
        AlertDialog(
            onDismissRequest = { if (!savingGoal) editGoalOpen = false },
            title = { Text("Cambiar objetivo") },
            text = {
                OutlinedTextField(
                    value = newGoal,
                    onValueChange = { newGoal = it },
                    label = { Text("Nuevo objetivo") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !savingGoal,
                    onClick = {
                        val uid = authRepo.currentUid() ?: return@TextButton
                        val clean = newGoal.trim()
                        if (clean.isBlank()) return@TextButton

                        savingGoal = true
                        scope.launch {
                            val r = userRepo.updateGoal(uid, clean)
                            savingGoal = false

                            r.onSuccess {
                                profile = profile?.copy(goal = clean)
                                editGoalOpen = false
                                showMessage("Objetivo cambiado")
                            }.onFailure { e ->
                                error = e.message ?: "Error actualizando objetivo"
                            }
                        }
                    }
                ) {
                    Text(if (savingGoal) "Guardando..." else "Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !savingGoal,
                    onClick = { editGoalOpen = false }
                ) { Text("Cancelar") }
            }
        )
    }

    // -----------------------------
    // Dialog Editar datos básicos
    // -----------------------------
    if (editBasicsOpen) {
        AlertDialog(
            onDismissRequest = { if (!savingBasics) editBasicsOpen = false },
            title = { Text("Editar datos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Altura (cm)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Peso (kg)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Edad") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Nivel", style = MaterialTheme.typography.titleMedium)

                    levels.forEach { lvl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedLevel == lvl),
                                    onClick = { selectedLevel = lvl }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedLevel == lvl),
                                onClick = { selectedLevel = lvl }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(lvl)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !savingBasics,
                    onClick = {
                        val uid = authRepo.currentUid() ?: return@TextButton

                        val h = heightText.toIntOrNull() ?: 0
                        val w = weightText.toIntOrNull() ?: 0
                        val a = ageText.toIntOrNull() ?: 0
                        val lvl = selectedLevel

                        savingBasics = true
                        scope.launch {
                            val r = userRepo.updateBasics(uid, h, w, a, lvl)
                            savingBasics = false

                            r.onSuccess {
                                profile = profile?.copy(
                                    heightCm = h,
                                    weightKg = w,
                                    age = a,
                                    level = lvl
                                )
                                editBasicsOpen = false
                            }.onFailure { e ->
                                error = e.message ?: "Error guardando datos"
                            }
                        }
                    }
                ) {
                    Text(if (savingBasics) "Guardando..." else "Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !savingBasics,
                    onClick = { editBasicsOpen = false }
                ) { Text("Cancelar") }
            }
        )
    }

    // -----------------------------
    // UI principal
    // -----------------------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineSmall)

        if (loading) {
            Text("Cargando…")
            return@Column
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        val p = profile
        if (p != null) {

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Cuenta", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Email: ${p.email.ifBlank { authRepo.currentEmail() ?: "—" }}")
                    Text("Usuario: ${p.username}")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Datos físicos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Altura: ${p.heightCm} cm")
                    Text("Peso: ${p.weightKg} kg")
                    Text("Edad: ${p.age} años")

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { editBasicsOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Editar datos")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Entrenamiento", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Nivel: ${p.level}")
                    Text("Objetivo: ${p.goal}")
                    Text("Días/semana: ${p.daysPerWeek}")

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { editGoalOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cambiar objetivo")
                    }
                }
            }
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }
    }
}