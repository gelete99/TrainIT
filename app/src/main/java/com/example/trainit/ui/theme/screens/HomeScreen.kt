package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.WorkoutRepository
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
    val workoutRepo = WorkoutRepository()

    var workouts by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun startOfWeekMillis(): Long {
        // Semana que empieza Lunes
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val monday = today.with(DayOfWeek.MONDAY)
        return monday.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun load() {
        val uid = authRepo.currentUid()
        if (uid == null) {
            loading = false
            error = "Sesión no válida. Vuelve a iniciar sesión."
            return
        }
        loading = true
        error = null
        // LaunchedEffect hace la llamada async abajo (usamos un “trigger” simple)
    }

    LaunchedEffect(Unit) {
        val uid = authRepo.currentUid()
        if (uid == null) {
            loading = false
            error = "Sesión no válida. Vuelve a iniciar sesión."
            return@LaunchedEffect
        }

        loading = true
        error = null

        val result = workoutRepo.getWorkouts(uid)
        loading = false

        result.onSuccess { workouts = it }
            .onFailure { e -> error = e.message ?: "Error cargando datos" }
    }

    val df = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Home", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (loading) {
            Text("Cargando…")
            return@Column
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { load() }) { Text("Reintentar") }
            return@Column
        }

        val totalWorkouts = workouts.size
        val totalMinutes = workouts.sumOf { it.durationMin }
        val weekStart = startOfWeekMillis()
        val weekWorkouts = workouts.filter { it.date >= weekStart }
        val weekMinutes = weekWorkouts.sumOf { it.durationMin }
        val lastWorkout = workouts.firstOrNull()

        // Cards de stats
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

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Último entreno", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (lastWorkout == null) {
                    Text("Aún no has registrado entrenamientos.")
                } else {
                    Text(lastWorkout.type)
                    Text("${lastWorkout.durationMin} min")
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
