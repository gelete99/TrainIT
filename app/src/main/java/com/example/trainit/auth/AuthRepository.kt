package com.example.trainit.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun currentUid(): String? = auth.currentUser?.uid
    fun currentEmail(): String? = auth.currentUser?.email
    fun logout() = auth.signOut()

    /**
     * Login usando USERNAME (o email si el usuario escribe un email).
     */
    suspend fun login(usernameOrEmail: String, password: String): Result<Unit> {
        return try {
            val identifier = usernameOrEmail.trim()
            val email = if (identifier.contains("@")) {
                identifier
            } else {
                val key = identifier.lowercase()
                val snap = db.collection("usernames").document(key).get().await()
                if (!snap.exists()) return Result.failure(Exception("Usuario no encontrado"))
                snap.getString("email") ?: return Result.failure(Exception("Usuario inválido"))
            }

            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registro con username + email + password
     * - crea usuario en FirebaseAuth (email/pass)
     * - crea users/{uid}
     * - crea usernames/{usernameLower}
     */
    suspend fun register(username: String, email: String, password: String): Result<Unit> {
        return try {
            val cleanUsername = username.trim()
            val cleanEmail = email.trim()

            if (cleanUsername.isBlank()) return Result.failure(Exception("El nombre de usuario es obligatorio"))
            if (cleanEmail.isBlank()) return Result.failure(Exception("El email es obligatorio"))

            val usernameKey = cleanUsername.lowercase()

            // comprobar username único
            val usernameDoc = db.collection("usernames").document(usernameKey).get().await()
            if (usernameDoc.exists()) {
                return Result.failure(Exception("Ese nombre de usuario ya está en uso"))
            }

            // crear usuario auth
            val authRes = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val uid = authRes.user?.uid ?: return Result.failure(Exception("No se pudo crear el usuario"))

            // escribir en Firestore en batch
            val batch = db.batch()

            val userRef = db.collection("users").document(uid)
            batch.set(
                userRef,
                mapOf(
                    "uid" to uid,
                    "email" to cleanEmail,
                    "username" to cleanUsername,
                    "createdAt" to System.currentTimeMillis(),
                    "onboardingCompleted" to false
                )
            )

            val usernameRef = db.collection("usernames").document(usernameKey)
            batch.set(
                usernameRef,
                mapOf(
                    "uid" to uid,
                    "email" to cleanEmail,
                    "username" to cleanUsername,
                    "createdAt" to System.currentTimeMillis()
                )
            )

            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
