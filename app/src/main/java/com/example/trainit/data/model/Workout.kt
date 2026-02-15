package com.example.trainit.data.model

data class Workout(
    val id: String = "",
    val date: Long = System.currentTimeMillis(),  // millis
    val type: String = "",                        // "Push", "Full body", etc.
    val durationMin: Int = 0,
    val notes: String = ""
)
