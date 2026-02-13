package com.example.trainit.data.model

data class UserProfile(
    val uid: String = "",
    val level: String = "",
    val goal: String = "",
    val daysPerWeek: Int = 0,
    val hasEquipment: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
