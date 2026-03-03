package com.example.trainit.data

import com.example.trainit.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getProfile(uid: String): Result<UserProfile> {
        return try {
            val snap = db.collection("users").document(uid).get().await()
            if (!snap.exists()) return Result.failure(Exception("Perfil no encontrado"))

            val profile = snap.toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Perfil inválido"))

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasCompletedOnboarding(uid: String): Boolean {
        return try {
            val snap = db.collection("users").document(uid).get().await()
            if (!snap.exists()) return false
            snap.getBoolean("onboardingCompleted") == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Guarda los datos del onboarding y marca onboardingCompleted=true.
     */
    suspend fun saveOnboarding(
        uid: String,
        gender: String,
        level: String,
        goal: String,
        daysPerWeek: Int,
        heightCm: Int,
        weightKg: Int,
        age: Int
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "gender" to gender,
                "level" to level,
                "goal" to goal,
                "daysPerWeek" to daysPerWeek,
                "heightCm" to heightCm,
                "weightKg" to weightKg,
                "age" to age,
                "onboardingCompleted" to true
            )

            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(uid: String, goal: String): Result<Unit> {
        return try {
            db.collection("users")
                .document(uid)
                .update(mapOf("goal" to goal))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun updateBasics(
        uid: String,
        heightCm: Int,
        weightKg: Int,
        age: Int,
        level: String,
        gender: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "heightCm" to heightCm,
                "weightKg" to weightKg,
                "age" to age,
                "level" to level,
                "gender" to gender
            )

            db.collection("users")
                .document(uid)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDaysPerWeek(uid: String, daysPerWeek: Int): Result<Unit> {
        return try {
            db.collection("users")
                .document(uid)
                .update(mapOf("daysPerWeek" to daysPerWeek))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}