package com.example.trainit.data.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val gender: String = "", 
    val level: String = "principiante",
    val goal: String = "",
    val daysPerWeek: Int = 3,
    val heightCm: Int = 0,
    val weightKg: Int = 0,
    val age: Int = 0,
    val onboardingCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)