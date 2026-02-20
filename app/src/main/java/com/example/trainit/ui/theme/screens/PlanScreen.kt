package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.AiRepository
import com.example.trainit.data.PlanRepository
import com.example.trainit.data.model.AiPlan
import com.example.trainit.data.model.DayPlan
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface PlanUiState {
    data object Loading : PlanUiState
    data object Empty : PlanUiState
    data class Success(val plan: AiPlan) : PlanUiState
    data class Error(val message: String) : PlanUiState
}

@Composable
fun PlanScreen() {
    val authRepo = AuthRepository()
    val aiRepo = remember { AiRepository() }
    val planRepo = remember { PlanRepository() }
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<PlanUiState>(PlanUiState.Loading) }
    var generating by remember { mutableStateOf(false) }

    // ✅ Scroll state
    val scrollState = rememberScrollState()

    // Refresh on resume
    var refreshKey by remember { mutableIntStateOf(0) }
    fun requestRefresh() { refreshKey++ }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) requestRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    // Cargar plan guardado
    LaunchedEffect(refreshKey) {
        val uid = authRepo.currentUid()
        if (uid == null) {
            state = PlanUiState.Error("Sesión no válida.")
            return@LaunchedEffect
        }

        state = PlanUiState.Loading
        val res = planRepo.getLatestPlan(uid)
        res.onSuccess { plan ->
            state = if (plan == null) PlanUiState.Empty else PlanUiState.Success(plan)
        }.onFailure { e ->
            state = PlanUiState.Error(e.message ?: "Error cargando plan")
        }
    }

    fun generateAndSave() {
        val uid = authRepo.currentUid() ?: run {
            state = PlanUiState.Error("Sesión no válida.")
            return
        }

        generating = true
        scope.launch {
            val res = aiRepo.generatePlanWithRaw()
            res.onSuccess { (plan, rawMap) ->
                val save = planRepo.saveLatestPlan(
                    uid = uid,
                    generatedAt = plan.generatedAt,
                    planMap = rawMap
                )

                save.onSuccess {
                    state = PlanUiState.Success(plan)
                    // ✅ al generar, subimos arriba del todo
                    scrollState.animateScrollTo(0)
                }.onFailure { e ->
                    state = PlanUiState.Error(e.message ?: "No se pudo guardar el plan")
                }
            }.onFailure { e ->
                state = PlanUiState.Error(e.message ?: "No se pudo generar el plan")
            }

            generating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // ✅ Scroll vertical
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Plan semanal", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Basado en tu perfil y tus últimos entrenos",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (val s = state) {
            PlanUiState.Loading -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Cargando…", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Buscando tu último plan guardado.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            PlanUiState.Empty -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Aún no hay plan guardado", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Genera tu plan semanal con IA. Se guardará automáticamente.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { if (!generating) generateAndSave() },
                            enabled = !generating,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (generating) "Generando…" else "Generar plan")
                        }
                    }
                }
            }

            is PlanUiState.Error -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No disponible", style = MaterialTheme.typography.titleLarge)
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = { if (!generating) generateAndSave() },
                            enabled = !generating,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (generating) "Generando…" else "Reintentar")
                        }
                    }
                }
            }

            is PlanUiState.Success -> {
                val plan = s.plan

                Button(
                    onClick = { if (!generating) generateAndSave() },
                    enabled = !generating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (generating) "Generando…" else "Regenerar y guardar")
                }

                if (plan.generatedAt > 0) {
                    Text(
                        "Última generación: ${df.format(Date(plan.generatedAt))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Valoración
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Valoración", style = MaterialTheme.typography.titleLarge)
                        if (plan.assessment.summary.isNotBlank()) Text(plan.assessment.summary)

                        if (plan.assessment.strengths.isNotEmpty()) {
                            Text("Puntos fuertes", style = MaterialTheme.typography.titleMedium)
                            BulletList(plan.assessment.strengths)
                        }

                        if (plan.assessment.improvements.isNotEmpty()) {
                            Text("A mejorar", style = MaterialTheme.typography.titleMedium)
                            BulletList(plan.assessment.improvements)
                        }
                    }
                }

                // Recomendaciones
                if (plan.recommendations.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Recomendaciones", style = MaterialTheme.typography.titleLarge)
                            BulletList(plan.recommendations)
                        }
                    }
                }

                // Plan semanal
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Plan de la semana", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "7 días (entreno/descanso según tu perfil)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                plan.weeklyPlan.forEach { DayPlanCard(it) }

                // Seguridad
                if (plan.safetyNotes.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Notas de seguridad", style = MaterialTheme.typography.titleLarge)
                            BulletList(plan.safetyNotes)
                        }
                    }
                }
            }
        }

        // ✅ espacio final para que no quede pegado abajo
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { it ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(it)
            }
        }
    }
}

@Composable
private fun DayPlanCard(dayPlan: DayPlan) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(dayPlan.day, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (dayPlan.isTrainingDay) dayPlan.focus else "Descanso / Movilidad",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dayPlan.isTrainingDay) {
                        Text(
                            "${dayPlan.durationMin} min · RPE ${dayPlan.targetRpe}/10",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Ocultar" else "Ver")
                }
            }

            if (expanded) {
                if (dayPlan.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(dayPlan.notes)
                }

                if (dayPlan.session.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Sesión", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayPlan.session.forEach { ex ->
                            Text(
                                "• ${ex.name} — ${ex.sets}x ${ex.reps}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}