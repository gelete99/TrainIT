package com.example.trainit.data

import com.example.trainit.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun hasProfile(uid: String): Boolean {
        val doc = db.collection("users").document(uid).get().await()
        return doc.exists()
    }

    suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        return try {
            db.collection("users")
                .document(profile.uid)
                .set(profile)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
