package com.example.trainit.data

import com.example.trainit.data.model.AiPlan
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PlanRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Guarda el plan "latest" en:
     * users/{uid}/plans/latest
     *
     * Guardamos:
     * - generatedAt (Long)
     * - plan (Map) -> el JSON completo (nested maps/lists)
     */
    suspend fun saveLatestPlan(uid: String, generatedAt: Long, planMap: Map<String, Any?>): Result<Unit> {
        return try {
            val doc = mapOf(
                "generatedAt" to generatedAt,
                "plan" to planMap
            )

            db.collection("users")
                .document(uid)
                .collection("plans")
                .document("latest")
                .set(doc)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Carga el plan "latest" desde:
     * users/{uid}/plans/latest
     */
    suspend fun getLatestPlan(uid: String): Result<AiPlan?> {
        return try {
            val snap = db.collection("users")
                .document(uid)
                .collection("plans")
                .document("latest")
                .get()
                .await()

            if (!snap.exists()) return Result.success(null)

            val data = snap.data ?: return Result.success(null)

            @Suppress("UNCHECKED_CAST")
            val planMap = data["plan"] as? Map<String, Any?> ?: return Result.success(null)

            // Reutilizamos el mapper del AiRepository
            val plan = AiRepository.mapToAiPlanStatic(planMap)
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}