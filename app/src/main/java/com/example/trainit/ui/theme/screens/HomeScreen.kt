package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.UserRepository
import com.example.trainit.data.WorkoutRepository
import com.example.trainit.data.model.UserProfile
import com.example.trainit.data.model.Workout
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Cargando…", style = MaterialTheme.typography.bodyLarge)
            }
            return@Column
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        // Objetivo
        profile?.let { p ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Tu objetivo", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = p.goal.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Nivel: ${p.level} · ${p.daysPerWeek} días/semana",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Altura/Peso/Edad: ${p.heightCm} cm · ${p.weightKg} kg · ${p.age} años",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Estado vacío
        if (workouts.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Aún no hay entrenamientos", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Pulsa el botón “+” para registrar tu primer entreno.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        // Stats 2x2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Entrenos",
                value = totalWorkouts.toString(),
                subtitle = "Totales"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Minutos",
                value = totalMinutes.toString(),
                subtitle = "Totales"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Semana",
                value = weekCount.toString(),
                subtitle = "Entrenos"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Semana",
                value = weekMinutes.toString(),
                subtitle = "Minutos"
            )
        }

        // Último entreno
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Último entreno", style = MaterialTheme.typography.titleLarge)

                if (lastWorkout == null) {
                    Text("Aún no has registrado entrenamientos.")
                } else {
                    Text(lastWorkout.type, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${lastWorkout.durationMin} min · RPE ${lastWorkout.rpe}/10 · ${daysAgoLabel(lastWorkout.date)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        df.format(Date(lastWorkout.date)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (lastWorkout.notes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(lastWorkout.notes)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
