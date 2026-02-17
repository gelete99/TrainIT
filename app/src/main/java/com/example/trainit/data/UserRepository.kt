package com.example.trainit.data

import com.example.trainit.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun hasCompletedOnboarding(uid: String): Boolean {
        val doc = db.collection("users").document(uid).get().await()
        val completed = doc.getBoolean("onboardingCompleted") ?: false
        return doc.exists() && completed
    }

    suspend fun getProfile(uid: String): Result<UserProfile> {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) return Result.failure(Exception("Perfil no encontrado"))

            val profile = doc.toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Perfil inválido"))

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveOnboarding(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("users")
                .document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(uid: String, newGoal: String): Result<Unit> {
        return try {
            db.collection("users").document(uid)
                .update("goal", newGoal.trim())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
