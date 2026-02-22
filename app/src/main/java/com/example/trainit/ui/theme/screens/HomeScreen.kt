package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.UserRepository
import com.example.trainit.data.WorkoutRepository
import com.example.trainit.data.model.UserProfile
import com.example.trainit.data.model.Workout
import com.example.trainit.ui.theme.*
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun HomeScreen() {
    val authRepo = AuthRepository()
    val userRepo = UserRepository()
    val workoutRepo = WorkoutRepository()

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var workouts by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // ✅ Trigger de refresh
    var refreshKey by remember { mutableIntStateOf(0) }
    fun requestRefresh() { refreshKey++ }

    // ✅ Auto refresh ON_RESUME
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) requestRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun startOfWeekMillis(): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val monday = today.with(DayOfWeek.MONDAY)
        return monday.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    // ✅ Carga inicial + recargas
    LaunchedEffect(refreshKey) {
        val uid = authRepo.currentUid()
        if (uid == null) {
            loading = false
            error = "Sesión no válida."
            return@LaunchedEffect
        }

        loading = true
        error = null

        val p = userRepo.getProfile(uid)
        val w = workoutRepo.getWorkouts(uid)

        loading = false

        p.onSuccess { profile = it }.onFailure { error = it.message ?: "Error cargando perfil" }
        w.onSuccess { workouts = it }.onFailure { error = it.message ?: "Error cargando entrenos" }
    }

    val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val totalWorkouts = workouts.size
    val totalMinutes = workouts.sumOf { it.durationMin }

    val weekStart = startOfWeekMillis()
    val weekWorkouts = workouts.filter { it.date >= weekStart }
    val weekCount = weekWorkouts.size
    val weekMinutes = weekWorkouts.sumOf { it.durationMin }

    val lastWorkout = workouts.firstOrNull()

    fun daysAgoLabel(epochMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val date = Date(epochMillis).toInstant().atZone(zone).toLocalDate()
        val days = (today.toEpochDay() - date.toEpochDay()).toInt().absoluteValue
        return when (days) {
            0 -> "Hoy"
            1 -> "Ayer"
            else -> "Hace $days días"
        }
    }

    val shape = RoundedCornerShape(18.dp)

    // 🎨 Colores por sección
    val objectiveAccent = BrandBlue
    val statsAccent = Warning
    val lastWorkoutAccent = Success

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val username = profile?.username?.takeIf { it.isNotBlank() }
        Text(
            text = if (username != null) "Hola, $username" else "Home",
            style = MaterialTheme.typography.headlineMedium
        )

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

        // ✅ Tu objetivo (AZUL) — Card pro
        profile?.let { p ->
            ProCard(
                accent = objectiveAccent,
                shape = shape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tu objetivo", style = MaterialTheme.typography.titleLarge)

                    Text(
                        text = p.goal.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        "Nivel: ${p.level} · ${p.daysPerWeek} días/semana",
                        color = TextSecondary
                    )
                    Text(
                        "Altura/Peso/Edad: ${p.heightCm} cm · ${p.weightKg} kg · ${p.age} años",
                        color = TextSecondary
                    )
                }
            }
        }

        // Estado vacío
        if (workouts.isEmpty()) {
            ProCard(
                accent = Outline,
                shape = shape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Aún no hay entrenamientos", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Pulsa el botón “+” para registrar tu primer entreno.",
                        color = TextSecondary
                    )
                }
            }
            return@Column
        }

        // ✅ Stats 2x2 (NARANJA) — Cards pro con presencia
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeStatCard(
                accent = statsAccent,
                title = "Entrenos",
                value = totalWorkouts.toString(),
                subtitle = "Totales",
                shape = shape
            )
            HomeStatCard(
                accent = statsAccent,
                title = "Minutos",
                value = totalMinutes.toString(),
                subtitle = "Totales",
                shape = shape
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeStatCard(
                accent = statsAccent,
                title = "Semana",
                value = weekCount.toString(),
                subtitle = "Entrenos",
                shape = shape
            )
            HomeStatCard(
                accent = statsAccent,
                title = "Semana",
                value = weekMinutes.toString(),
                subtitle = "Minutos",
                shape = shape
            )
        }

        // ✅ Último entreno (VERDE) — Card pro
        ProCard(
            accent = lastWorkoutAccent,
            shape = shape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Último entreno", style = MaterialTheme.typography.titleLarge)

                if (lastWorkout == null) {
                    Text("Aún no has registrado entrenamientos.")
                } else {
                    Text(
                        lastWorkout.type,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        "${lastWorkout.durationMin} min · RPE ${lastWorkout.rpe}/10 · ${daysAgoLabel(lastWorkout.date)}",
                        color = TextSecondary
                    )

                    Text(
                        df.format(Date(lastWorkout.date)),
                        color = TextSecondary
                    )

                    if (lastWorkout.notes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            lastWorkout.notes,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/* ====== Cards pro (mismo estilo que History/Profile) ====== */

@Composable
private fun ProCard(
    accent: Color,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun RowScope.HomeStatCard(
    accent: Color,
    title: String,
    value: String,
    subtitle: String,
    shape: RoundedCornerShape
) {
    Card(
        modifier = Modifier.weight(1f),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent
            )
            Text(
                subtitle,
                color = TextSecondary
            )
        }
    }
}