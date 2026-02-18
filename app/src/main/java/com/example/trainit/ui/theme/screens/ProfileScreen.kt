package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    var editGoalOpen by remember { mutableStateOf(false) }
    var newGoal by remember { mutableStateOf("") }
    var savingGoal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = authRepo.currentUid()
        if (uid == null) {
            loading = false
            error = "Sesión no válida."
            return@LaunchedEffect
        }

        loading = true
        error = null

        val res = userRepo.getProfile(uid)
        loading = false

        res.onSuccess {
            profile = it
            newGoal = it.goal
        }.onFailure {
            error = it.message ?: "Error cargando perfil"
        }
    }

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
                        if (clean.isBlank()) {
                            showMessage("El objetivo no puede estar vacío")
                            return@TextButton
                        }

                        savingGoal = true
                        scope.launch {
                            val r = userRepo.updateGoal(uid, clean)
                            savingGoal = false

                            r.onSuccess {
                                profile = profile?.copy(goal = clean)
                                editGoalOpen = false
                                showMessage("Objetivo actualizado")
                            }.onFailure { e ->
                                val msg = e.message ?: "Error actualizando objetivo"
                                error = msg
                                showMessage(msg)
                            }
                        }
                    }
                ) { Text(if (savingGoal) "Guardando..." else "Guardar") }
            },
            dismissButton = {
                TextButton(
                    enabled = !savingGoal,
                    onClick = { editGoalOpen = false }
                ) { Text("Cancelar") }
            }
        )
    }

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

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

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
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Entrenamiento", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Nivel: ${p.level}")
                    Text("Objetivo: ${p.goal}")
                    Text("Días/semana: ${p.daysPerWeek}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { editGoalOpen = true }) {
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
