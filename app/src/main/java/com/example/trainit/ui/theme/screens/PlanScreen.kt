package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.AiRepository
import com.example.trainit.data.PlanRepository
import com.example.trainit.data.WorkoutRepository
import com.example.trainit.data.model.AiPlan
import com.example.trainit.data.model.DayPlan
import com.example.trainit.data.model.Workout
import com.example.trainit.ui.theme.BrandBlue
import com.example.trainit.ui.theme.BrandBlueDark
import com.example.trainit.ui.theme.Surface
import com.example.trainit.ui.theme.TextSecondary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
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
    val workoutRepo = remember { WorkoutRepository() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<PlanUiState>(PlanUiState.Loading) }
    var generating by remember { mutableStateOf(false) }

    // ✅ Persistencia UI de “añadido”
    val completedDays = remember { mutableStateMapOf<String, Boolean>() }
    val completingDays = remember { mutableStateMapOf<String, Boolean>() }

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

    fun completionKey(planGeneratedAt: Long, day: String): String =
        "${planGeneratedAt}_${day.trim()}"

    fun startOfWeek(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    fun dayNameToDayOfWeek(day: String): DayOfWeek? {
        return when (day.trim().lowercase(Locale.getDefault())) {
            "lunes" -> DayOfWeek.MONDAY
            "martes" -> DayOfWeek.TUESDAY
            "miércoles", "miercoles" -> DayOfWeek.WEDNESDAY
            "jueves" -> DayOfWeek.THURSDAY
            "viernes" -> DayOfWeek.FRIDAY
            "sábado", "sabado" -> DayOfWeek.SATURDAY
            "domingo" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    fun dateMillisForDayName(dayName: String): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val monday = startOfWeek(today)

        val dow = dayNameToDayOfWeek(dayName) ?: DayOfWeek.MONDAY
        val offset = (dow.value - DayOfWeek.MONDAY.value).toLong()
        val dayDate = monday.plusDays(offset)

        // Hora actual para que se vea “real” en historial
        val now = java.time.LocalTime.now(zone)
        return dayDate.atTime(now).atZone(zone).toInstant().toEpochMilli()
    }

    fun buildWorkoutFromDay(dayPlan: DayPlan): Workout {
        val notesExercises = if (dayPlan.session.isNotEmpty()) {
            dayPlan.session.joinToString(separator = "\n") { ex ->
                "- ${ex.name} — ${ex.sets}x ${ex.reps}"
            }
        } else ""

        val mergedNotes = buildString {
            if (dayPlan.notes.isNotBlank()) append(dayPlan.notes.trim())
            if (notesExercises.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("Sesión:\n")
                append(notesExercises)
            }
        }

        return Workout(
            id = "",
            date = dateMillisForDayName(dayPlan.day),
            type = dayPlan.focus.ifBlank { "Entreno" },
            durationMin = dayPlan.durationMin,
            rpe = dayPlan.targetRpe,
            notes = mergedNotes
        )
    }

    suspend fun loadCompletions(uid: String, planGeneratedAt: Long) {
        try {
            val snaps = db.collection("users")
                .document(uid)
                .collection("planCompletions")
                .whereEqualTo("planGeneratedAt", planGeneratedAt)
                .get()
                .await()

            snaps.documents.forEach { doc ->
                completedDays[doc.id] = true
            }
        } catch (_: Exception) {
            // silencioso
        }
    }

    fun markDayAsAdded(dayPlan: DayPlan, planGeneratedAt: Long) {
        val uid = authRepo.currentUid() ?: run {
            state = PlanUiState.Error("Sesión no válida.")
            return
        }

        if (!dayPlan.isTrainingDay) return

        val key = completionKey(planGeneratedAt, dayPlan.day)
        if (completedDays[key] == true) return
        if (completingDays[key] == true) return

        completingDays[key] = true

        scope.launch {
            try {
                val docRef = db.collection("users")
                    .document(uid)
                    .collection("planCompletions")
                    .document(key)

                // ✅ si ya existe, ya está persistido
                val existing = docRef.get().await()
                if (existing.exists()) {
                    completedDays[key] = true
                    return@launch
                }

                // 1) guardar workout
                val workout = buildWorkoutFromDay(dayPlan)
                val res = workoutRepo.addWorkout(uid, workout)

                res.onSuccess {
                    // 2) guardar completion persistente
                    val data = mapOf(
                        "planGeneratedAt" to planGeneratedAt,
                        "day" to dayPlan.day,
                        "completedAt" to System.currentTimeMillis()
                    )
                    docRef.set(data).await()

                    completedDays[key] = true
                }.onFailure { e ->
                    state = PlanUiState.Error(e.message ?: "No se pudo añadir al historial")
                }
            } catch (e: Exception) {
                state = PlanUiState.Error(e.message ?: "Error guardando en historial")
            } finally {
                completingDays[key] = false
            }
        }
    }

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

            completedDays.clear()
            completingDays.clear()

            // ✅ cargar persistencia “añadido”
            if (plan != null) {
                scope.launch { loadCompletions(uid, plan.generatedAt) }
            }
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
                    completedDays.clear()
                    completingDays.clear()

                    // ✅ tras generar, cargar completions (debería estar vacío)
                    scope.launch { loadCompletions(uid, plan.generatedAt) }

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

    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
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
                    Text(if (generating) "Generando…" else "Generar plan")
                }

                if (plan.generatedAt > 0) {
                    Text(
                        "Última generación: ${df.format(Date(plan.generatedAt))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ProPlanCard(accent = BrandBlue, shape = shape) {
                    Column(
                        modifier = Modifier.padding(18.dp),
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

                if (plan.recommendations.isNotEmpty()) {
                    ProPlanCard(accent = BrandBlue, shape = shape) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Recomendaciones", style = MaterialTheme.typography.titleLarge)
                            BulletList(plan.recommendations)
                        }
                    }
                }

                ProPlanCard(accent = BrandBlue, shape = shape) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Plan de la semana", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "7 días (entreno/descanso según tu perfil)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                plan.weeklyPlan.forEach { dayPlan ->
                    val key = completionKey(plan.generatedAt, dayPlan.day)
                    DayPlanCardPro(
                        dayPlan = dayPlan,
                        shape = shape,
                        isCompleting = completingDays[key] == true,
                        isCompleted = completedDays[key] == true,
                        onAddToHistory = { markDayAsAdded(dayPlan, plan.generatedAt) }
                    )
                }

                if (plan.safetyNotes.isNotEmpty()) {
                    ProPlanCard(accent = BrandBlue, shape = shape) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Notas de seguridad", style = MaterialTheme.typography.titleLarge)
                            BulletList(plan.safetyNotes)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/* ====== Helpers ====== */

@Composable
private fun ProPlanCard(
    accent: Color,
    shape: RoundedCornerShape,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
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
private fun DayPlanCardPro(
    dayPlan: DayPlan,
    shape: RoundedCornerShape,
    isCompleting: Boolean,
    isCompleted: Boolean,
    onAddToHistory: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, BrandBlueDark.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(BrandBlueDark)
            )

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            dayPlan.day,
                            style = MaterialTheme.typography.titleLarge,
                            color = BrandBlue
                        )

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

                    Column(horizontalAlignment = Alignment.End) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    if (dayPlan.isTrainingDay) "ENTRENO" else "DESCANSO",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )

                        Spacer(Modifier.height(6.dp))

                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "Ocultar" else "Ver")
                        }
                    }
                }

                // ✅ Botón discreto “Añadir al historial”
                if (dayPlan.isTrainingDay) {
                    OutlinedButton(
                        onClick = onAddToHistory,
                        enabled = !isCompleting && !isCompleted,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue)
                    ) {
                        if (isCompleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            when {
                                isCompleted -> "Añadido al historial ✓"
                                isCompleting -> "Añadiendo…"
                                else -> "Añadir al historial"
                            }
                        )
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
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}