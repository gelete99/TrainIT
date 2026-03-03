package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.UserRepository
import com.example.trainit.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.RowScope

@Composable
fun ProfileScreen(
    showMessage: (String) -> Unit,
    onLogout: () -> Unit
) {
    val authRepo = AuthRepository()
    val userRepo = UserRepository()
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<com.example.trainit.data.model.UserProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Campos editables
    var goal by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("principiante") }
    var gender by remember { mutableStateOf("") } // ✅ NUEVO
    var daysPerWeek by remember { mutableIntStateOf(3) }
    var height by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    val levels = listOf("principiante", "intermedio", "avanzado")

    fun prettyLevel(s: String): String =
        s.trim().lowercase().replaceFirstChar { it.uppercaseChar() }

    fun prettyGender(s: String): String =
        s.trim().lowercase().replaceFirstChar { it.uppercaseChar() }

    // Carga inicial
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

        res.onSuccess { p ->
            profile = p
            goal = p.goal
            level = p.level
            gender = p.gender // ✅ NUEVO
            daysPerWeek = p.daysPerWeek
            height = if (p.heightCm > 0) p.heightCm.toString() else ""
            weightInput = if (p.weightKg > 0) p.weightKg.toString() else ""
            age = if (p.age > 0) p.age.toString() else ""
        }.onFailure { e ->
            error = e.message ?: "Error cargando perfil"
        }
    }

    fun saveProfile() {
        val uid = authRepo.currentUid() ?: run {
            error = "Sesión no válida."
            return
        }

        val h = height.toIntOrNull() ?: 0
        val w = weightInput.toIntOrNull() ?: 0
        val a = age.toIntOrNull() ?: 0

        if (h < 0 || w < 0 || a < 0) {
            showMessage("Valores no válidos")
            return
        }
        if (daysPerWeek !in 1..7) {
            showMessage("Días por semana no válido")
            return
        }
        if (gender.isBlank()) {
            showMessage("Selecciona tu sexo")
            return
        }

        saving = true
        error = null

        scope.launch {
            val rGoal = userRepo.updateGoal(uid, goal.trim())
            val rBasics = userRepo.updateBasics(
                uid = uid,
                heightCm = h,
                weightKg = w,
                age = a,
                level = level,
                gender = gender // ✅ NUEVO
            )
            val rDays = userRepo.updateDaysPerWeek(uid, daysPerWeek)

            saving = false

            if (rGoal.isSuccess && rBasics.isSuccess && rDays.isSuccess) {
                profile = profile?.copy(
                    goal = goal.trim(),
                    level = level,
                    gender = gender, // ✅ NUEVO
                    daysPerWeek = daysPerWeek,
                    heightCm = h,
                    weightKg = w,
                    age = a
                )
                showMessage("Perfil guardado")
            } else {
                val msg = (rGoal.exceptionOrNull()?.message
                    ?: rBasics.exceptionOrNull()?.message
                    ?: rDays.exceptionOrNull()?.message
                    ?: "No se pudo guardar el perfil")
                error = msg
                showMessage(msg)
            }
        }
    }

    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineSmall)

        if (loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text("Cargando…", style = MaterialTheme.typography.bodyLarge)
            }
            return@Column
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        val p = profile
        if (p == null) {
            ProCard(accent = Outline, shape = shape) {
                Column(Modifier.padding(18.dp)) {
                    Text("No disponible", style = MaterialTheme.typography.titleLarge)
                    Text("No se pudo cargar el perfil.", color = TextSecondary)
                }
            }
            return@Column
        }

        // ✅ Tu perfil (AZUL)
        ProCard(accent = BrandBlue, shape = shape) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tu perfil", style = MaterialTheme.typography.titleLarge)

                HighlightRow(label = "Usuario", value = p.username.ifBlank { "—" })
                HighlightRow(label = "Email", value = p.email.ifBlank { "—" })
                HighlightRow(label = "Sexo", value = if (gender.isBlank()) "—" else prettyGender(gender)) // ✅ NUEVO

                Spacer(Modifier.height(4.dp))

                HighlightRow(label = "Nivel", value = prettyLevel(level))
                HighlightRow(label = "Días/semana", value = "$daysPerWeek")

                HighlightRow(
                    label = "Objetivo",
                    value = goal.ifBlank { "—" },
                    maxLinesValue = 3
                )

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    HighlightMini(label = "Altura", value = if (height.isBlank()) "—" else "$height cm")
                    HighlightMini(label = "Peso", value = if (weightInput.isBlank()) "—" else "$weightInput kg")
                    HighlightMini(label = "Edad", value = if (age.isBlank()) "—" else "$age años")
                }
            }
        }

        // ✅ Editar objetivo (NARANJA)
        ProCard(accent = Warning, shape = shape) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Objetivo", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tu objetivo") },
                    placeholder = { Text("Ej: Ganar masa muscular") },
                    supportingText = { Text("Se usará para tu plan IA") }
                )
            }
        }

        // ✅ Datos físicos (NARANJA) + sexo + días por semana
        ProCard(accent = Warning, shape = shape) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Datos físicos", style = MaterialTheme.typography.titleLarge)

                // ✅ Sexo
                Text("Sexo", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("hombre", "mujer").forEach { option ->
                        val selected = gender == option
                        OutlinedButton(
                            onClick = { gender = option },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) Warning else Outline.copy(alpha = 0.8f)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selected) Warning else TextPrimary
                            )
                        ) {
                            Text(
                                prettyGender(option),
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Nivel
                Text("Nivel", style = MaterialTheme.typography.labelLarge, color = TextSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    levels.forEach { option ->
                        val selected = level == option
                        OutlinedButton(
                            onClick = { level = option },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) Warning else Outline.copy(alpha = 0.8f)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selected) Warning else TextPrimary
                            )
                        ) {
                            Text(
                                prettyLevel(option),
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // ✅ Días por semana (1–7)
                Text(
                    "Días de entrenamiento por semana",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..7).forEach { d ->
                        val selected = daysPerWeek == d
                        OutlinedButton(
                            onClick = { daysPerWeek = d },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) Warning else Outline.copy(alpha = 0.8f)
                            ),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selected) Warning else TextPrimary
                            )
                        ) {
                            Text(
                                d.toString(),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Altura (cm)") }
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Peso (kg)") }
                    )
                }

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Edad") }
                )
            }
        }

        Button(
            onClick = { if (!saving) saveProfile() },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(if (saving) "Guardando…" else "Guardar cambios")
        }

        OutlinedButton(
            onClick = {
                authRepo.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }

        Spacer(Modifier.height(24.dp))
    }
}

/* ====== UI helpers estilo “pro” ====== */

@Composable
private fun ProCard(
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun HighlightRow(
    label: String,
    value: String,
    maxLinesValue: Int = 1
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        Text(
            value,
            maxLines = maxLinesValue,
            overflow = if (maxLinesValue == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun RowScope.HighlightMini(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}