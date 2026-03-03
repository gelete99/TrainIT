package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val authRepo = AuthRepository()
    val userRepo = UserRepository()
    val scope = rememberCoroutineScope()

    val levels = listOf("principiante", "intermedio", "avanzado")
    val daysOptions = (1..7).toList()

    var gender by remember { mutableStateOf("") } // ✅ NUEVO
    var level by remember { mutableStateOf(levels.first()) }
    var goal by remember { mutableStateOf("") }
    var daysPerWeek by remember { mutableIntStateOf(3) }

    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var levelExpanded by remember { mutableStateOf(false) }
    var daysExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Onboarding", style = MaterialTheme.typography.headlineSmall)

        // ✅ Sexo (botones)
        Text("Sexo", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("hombre", "mujer").forEach { option ->
                val selected = gender == option
                OutlinedButton(
                    onClick = { gender = option; error = null },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        option.replaceFirstChar { it.uppercase() },
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Nivel (dropdown)
        ExposedDropdownMenuBox(
            expanded = levelExpanded,
            onExpandedChange = { levelExpanded = !levelExpanded }
        ) {
            OutlinedTextField(
                value = level.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Nivel") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )

            ExposedDropdownMenu(
                expanded = levelExpanded,
                onDismissRequest = { levelExpanded = false }
            ) {
                levels.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            level = opt
                            levelExpanded = false
                            error = null
                        }
                    )
                }
            }
        }

        // Objetivo libre
        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it; error = null },
            label = { Text("Objetivo (escríbelo tú)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Días/semana (dropdown 1..7)
        ExposedDropdownMenuBox(
            expanded = daysExpanded,
            onExpandedChange = { daysExpanded = !daysExpanded }
        ) {
            OutlinedTextField(
                value = daysPerWeek.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Días por semana") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = daysExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )

            ExposedDropdownMenu(
                expanded = daysExpanded,
                onDismissRequest = { daysExpanded = false }
            ) {
                daysOptions.forEach { d ->
                    DropdownMenuItem(
                        text = { Text(d.toString()) },
                        onClick = {
                            daysPerWeek = d
                            daysExpanded = false
                            error = null
                        }
                    )
                }
            }
        }

        // Altura / Peso / Edad
        OutlinedTextField(
            value = heightText,
            onValueChange = { heightText = it.filter(Char::isDigit); error = null },
            label = { Text("Altura (cm)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it.filter(Char::isDigit); error = null },
            label = { Text("Peso (kg)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ageText,
            onValueChange = { ageText = it.filter(Char::isDigit); error = null },
            label = { Text("Edad") },
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val uid = authRepo.currentUid()
                if (uid == null) {
                    error = "Sesión no válida. Vuelve a iniciar sesión."
                    return@Button
                }

                val cleanGoal = goal.trim()
                val height = heightText.toIntOrNull() ?: 0
                val weight = weightText.toIntOrNull() ?: 0
                val age = ageText.toIntOrNull() ?: 0

                if (gender.isBlank()) {
                    error = "Selecciona tu sexo."
                    return@Button
                }
                if (cleanGoal.isBlank()) {
                    error = "Introduce tu objetivo."
                    return@Button
                }
                if (height <= 0 || weight <= 0 || age <= 0) {
                    error = "Altura, peso y edad deben ser válidos."
                    return@Button
                }

                loading = true
                error = null

                scope.launch(Dispatchers.Main) {
                    val res = userRepo.saveOnboarding(
                        uid = uid,
                        gender = gender, // ✅ NUEVO
                        level = level,
                        goal = cleanGoal,
                        daysPerWeek = daysPerWeek,
                        heightCm = height,
                        weightKg = weight,
                        age = age
                    )

                    loading = false
                    res.onSuccess { onFinish() }
                        .onFailure { e -> error = e.message ?: "Error guardando onboarding" }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text("Guardar y continuar")
        }
    }
}