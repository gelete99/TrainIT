package com.example.trainit.data

import com.example.trainit.data.model.Workout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class WorkoutRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun addWorkout(uid: String, workout: Workout): Result<Unit> {
        return try {
            val col = db.collection("users").document(uid).collection("workouts")
            val doc = col.document()
            val workoutWithId = workout.copy(id = doc.id)
            doc.set(workoutWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkouts(uid: String): Result<List<Workout>> {
        return try {
            val snap = db.collection("users")
                .document(uid)
                .collection("workouts")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val list = snap.documents.mapNotNull { it.toObject(Workout::class.java) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(uid: String, workoutId: String): Result<Unit> {
        return try {
            db.collection("users")
                .document(uid)
                .collection("workouts")
                .document(workoutId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
