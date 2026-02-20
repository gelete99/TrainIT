package com.example.trainit.data.model

data class AiPlan(
    val generatedAt: Long = 0L,
    val assessment: Assessment = Assessment(),
    val recommendations: List<String> = emptyList(),
    val weeklyPlan: List<DayPlan> = emptyList(),
    val safetyNotes: List<String> = emptyList()
)

data class Assessment(
    val summary: String = "",
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList()
)

data class DayPlan(
    val day: String = "",
    val isTrainingDay: Boolean = false,
    val focus: String = "",
    val durationMin: Int = 0,
    val targetRpe: Int = 0,
    val session: List<Exercise> = emptyList(),
    val notes: String = ""
)

data class Exercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: String = ""
)