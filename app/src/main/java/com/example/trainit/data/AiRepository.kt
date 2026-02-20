package com.example.trainit.data

import com.example.trainit.data.model.*
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class AiRepository {

    private val functions = FirebaseFunctions.getInstance("europe-west1")

    /**
     * Llama a la Cloud Function y devuelve:
     * - plan parseado (AiPlan)
     * - map raw (para guardar en Firestore)
     */
    suspend fun generatePlanWithRaw(): Result<Pair<AiPlan, Map<String, Any?>>> {
        return try {
            val res = functions.getHttpsCallable("generatePlan")
                .call(emptyMap<String, Any>())
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = res.data as? Map<String, Any?> ?: return Result.failure(Exception("Respuesta inválida"))

            val plan = mapToAiPlanStatic(data)
            Result.success(plan to data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun mapToAiPlanStatic(data: Map<String, Any?>): AiPlan {
            val generatedAt = (data["generatedAt"] as? Number)?.toLong() ?: 0L

            val assessmentMap = data["assessment"] as? Map<*, *> ?: emptyMap<String, Any>()
            val assessment = Assessment(
                summary = assessmentMap["summary"] as? String ?: "",
                strengths = (assessmentMap["strengths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                improvements = (assessmentMap["improvements"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )

            val recommendations =
                (data["recommendations"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            val weeklyRaw = (data["weeklyPlan"] as? List<*>) ?: emptyList<Any>()
            val weeklyPlan = weeklyRaw.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null

                val sessionRaw = (m["session"] as? List<*>) ?: emptyList<Any>()
                val session = sessionRaw.mapNotNull { ex ->
                    val exm = ex as? Map<*, *> ?: return@mapNotNull null
                    Exercise(
                        name = exm["name"] as? String ?: "",
                        sets = (exm["sets"] as? Number)?.toInt() ?: 0,
                        reps = exm["reps"] as? String ?: ""
                    )
                }

                DayPlan(
                    day = m["day"] as? String ?: "",
                    isTrainingDay = m["isTrainingDay"] as? Boolean ?: false,
                    focus = m["focus"] as? String ?: "",
                    durationMin = (m["durationMin"] as? Number)?.toInt() ?: 0,
                    targetRpe = (m["targetRpe"] as? Number)?.toInt() ?: 0,
                    session = session,
                    notes = m["notes"] as? String ?: ""
                )
            }

            val safetyNotes =
                (data["safetyNotes"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            return AiPlan(
                generatedAt = generatedAt,
                assessment = assessment,
                recommendations = recommendations,
                weeklyPlan = weeklyPlan,
                safetyNotes = safetyNotes
            )
        }
    }
}