package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

@Composable
fun HomeScreen() {
    val authRepo = AuthRepository()
    val userRepo = UserRepository()
    val workoutRepo = WorkoutRepository()

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var workouts by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun startOfWeekMillis(): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val monday = today.with(DayOfWeek.MONDAY)
        return monday.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    LaunchedEffect(Unit) {
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

        p.onSuccess { profile = it }.onFailure { error = it.message }
        w.onSuccess { workouts = it }.onFailure { error = it.message }
    }

    val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Home", style = MaterialTheme.typography.headlineSmall)

        if (loading) {
            Text("Cargando…")
            return@Column
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        // ✅ Tarjeta "Tu objetivo" (punto 3 asumido)
        profile?.let { p ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Tu objetivo", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Objetivo: ${p.goal}")
                    Text("Nivel: ${p.level}")
                    Text("Días/semana: ${p.daysPerWeek}")
                    Text("Altura/Peso/Edad: ${p.heightCm}cm · ${p.weightKg}kg · ${p.age} años")
                }
            }
        }

        // Stats
        val totalWorkouts = workouts.size
        val totalMinutes = workouts.sumOf { it.durationMin }
        val weekStart = startOfWeekMillis()
        val weekWorkouts = workouts.filter { it.date >= weekStart }
        val weekMinutes = weekWorkouts.sumOf { it.durationMin }
        val lastWorkout = workouts.firstOrNull()

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Resumen", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Entrenos totales: $totalWorkouts")
                Text("Minutos totales: $totalMinutes")
                Spacer(Modifier.height(8.dp))
                Text("Esta semana: ${weekWorkouts.size} entrenos · $weekMinutes min")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Último entreno", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (lastWorkout == null) {
                    Text("Aún no has registrado entrenamientos.")
                } else {
                    Text(lastWorkout.type)
                    Text("${lastWorkout.durationMin} min · RPE ${lastWorkout.rpe}/10")
                    Text(df.format(Date(lastWorkout.date)))
                    if (lastWorkout.notes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(lastWorkout.notes)
                    }
                }
            }
        }
    }
}
