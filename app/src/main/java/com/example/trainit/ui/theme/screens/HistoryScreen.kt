package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.WorkoutRepository
import com.example.trainit.data.model.Workout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen() {
    val authRepo = AuthRepository()
    val workoutRepo = WorkoutRepository()
    val scope = rememberCoroutineScope()

    var workouts by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Trigger para recargar
    var refreshKey by remember { mutableIntStateOf(0) }
    fun requestRefresh() { refreshKey++ }

    // Borrar
    var deleteTarget by remember { mutableStateOf<Workout?>(null) }
    var deleting by remember { mutableStateOf(false) }

    // Auto refresh ON_RESUME
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) requestRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Carga inicial + recargas
    LaunchedEffect(refreshKey) {
        val uid = authRepo.currentUid()
        if (uid == null) {
            error = "Sesión no válida."
            loading = false
            return@LaunchedEffect
        }

        loading = true
        error = null

        val res = workoutRepo.getWorkouts(uid)
        loading = false

        res.onSuccess { workouts = it }
            .onFailure { e -> error = e.message ?: "Error cargando historial" }
    }

    val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    // Dialog confirmación borrar
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { if (!deleting) deleteTarget = null },
            title = { Text("Borrar entrenamiento") },
            text = { Text("¿Seguro que quieres borrar este entrenamiento?") },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        val uid = authRepo.currentUid() ?: return@TextButton
                        val w = deleteTarget ?: return@TextButton

                        deleting = true
                        scope.launch {
                            val del = workoutRepo.deleteWorkout(uid, w.id)
                            deleting = false
                            deleteTarget = null

                            del.onSuccess { requestRefresh() }
                                .onFailure { e -> error = e.message ?: "Error borrando" }
                        }
                    }
                ) {
                    Text(if (deleting) "Borrando..." else "Borrar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = { deleteTarget = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
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
            Spacer(Modifier.height(8.dp))
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(w.type, style = MaterialTheme.typography.titleMedium)

                            IconButton(onClick = { deleteTarget = w }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                            }
                        }

                        Text("${w.durationMin} min · RPE ${w.rpe}/10")
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
