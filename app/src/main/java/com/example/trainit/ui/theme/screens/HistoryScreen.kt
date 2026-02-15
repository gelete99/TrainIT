package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.WorkoutRepository
import com.example.trainit.data.model.Workout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen() {
    val authRepo = AuthRepository()
    val workoutRepo = WorkoutRepository()

    var workouts by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    fun load() {
        val uid = authRepo.currentUid()
        if (uid == null) {
            error = "Sesión no válida. Vuelve a iniciar sesión."
            loading = false
            return
        }

        loading = true
        error = null

        // LaunchedEffect no acepta funciones suspend aquí directamente, así que usamos LaunchedEffect + suspends dentro abajo.
    }

    LaunchedEffect(Unit) {
        val uid = authRepo.currentUid()
        if (uid == null) {
            error = "Sesión no válida. Vuelve a iniciar sesión."
            loading = false
            return@LaunchedEffect
        }

        loading = true
        error = null

        val result = workoutRepo.getWorkouts(uid)
        loading = false

        result.onSuccess { workouts = it }
            .onFailure { e -> error = e.message ?: "Error cargando historial" }
    }

    val df = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Historial", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (loading) {
            Text("Cargando…")
            return@Column
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    // recarga rápida:
                    // (forzamos recomposición cambiando estado)
                    loading = true
                    error = null
                    workouts = emptyList()
                }
            ) {
                Text("Reintentar")
            }
            return@Column
        }

        if (workouts.isEmpty()) {
            Text("Aún no hay entrenamientos guardados.")
            Spacer(Modifier.height(8.dp))
            Text("Ve a “Log Workout” y registra tu primer entreno.")
            return@Column
        }

        // Stats básicas (MVP)
        val total = workouts.size
        val totalMin = workouts.sumOf { it.durationMin }

        Text("Entrenos: $total · Minutos totales: $totalMin")
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(workouts) { w ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(w.type, style = MaterialTheme.typography.titleMedium)
                        Text("${w.durationMin} min")
                        Text(df.format(Date(w.date)))
                        if (w.notes.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(w.notes)
                        }
                    }
                }
            }
        }
    }
}
