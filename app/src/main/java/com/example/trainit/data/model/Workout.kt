package com.example.trainit.data.model

data class Workout(
    val id: String = "",
    val date: Long = System.currentTimeMillis(),
    val type: String = "",
    val durationMin: Int = 0,
    val rpe: Int = 0,      // 0-10
    val notes: String = ""
)
