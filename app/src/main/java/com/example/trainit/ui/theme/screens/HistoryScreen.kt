package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    // Trigger para recargar
    var refreshKey by remember { mutableIntStateOf(0) }

    fun requestRefresh() {
        refreshKey++
    }

    // Auto-refresh al volver a la pantalla (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                requestRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Carga inicial + recargas por refreshKey
    LaunchedEffect(refreshKey) {
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
            Text("Vuelve a abrir esta pantalla para reintentar.")
            return@Column
        }

        if (workouts.isEmpty()) {
            Text("Aún no hay entrenamientos guardados.")
            Spacer(Modifier.height(8.dp))
            Text("Pulsa el “+” para registrar tu primer entreno.")
            return@Column
        }

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
