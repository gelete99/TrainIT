package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.WorkoutRepository
import com.example.trainit.data.model.Workout
import com.example.trainit.ui.theme.Success
import com.example.trainit.ui.theme.Surface
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    showMessage: (String) -> Unit
) {
    val authRepo = AuthRepository()
    val workoutRepo = WorkoutRepository()
    val scope = rememberCoroutineScope()

    var workouts by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    var refreshKey by remember { mutableIntStateOf(0) }
    fun requestRefresh() { refreshKey++ }

    var deleteTarget by remember { mutableStateOf<Workout?>(null) }
    var deleting by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) requestRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    // Dialog borrar
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

                            del.onSuccess {
                                showMessage("Entrenamiento eliminado")
                                requestRefresh()
                            }.onFailure { e ->
                                val msg = e.message ?: "Error borrando"
                                error = msg
                                showMessage(msg)
                            }
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
        Spacer(Modifier.height(16.dp))

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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(workouts) { w ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Success.copy(alpha = 0.50f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // 🔹 Título (ancho completo)
                        Text(
                            w.type,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 🔹 Métricas
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            MetricItem("Duración", "${w.durationMin} min")
                            MetricItem("RPE", "${w.rpe}/10")
                        }

                        // 🔹 Fecha + papelera alineada a la derecha
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                df.format(Date(w.date)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                enabled = !deleting,
                                onClick = { deleteTarget = w }
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Borrar"
                                )
                            }
                        }

                        // 🔹 Divider
                        if (w.notes.isNotBlank()) {
                            HorizontalDivider()
                            Text(
                                w.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = Success
        )
    }
}