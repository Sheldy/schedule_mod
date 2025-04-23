package it.hamy.schedule.model

data class LessonResult(
    val subject: String,
    val teacher: String,
    val type: String,
    val totalHours: Int,
    val actualHours: Int,
    val remainingHours: Int,
    val endDate: String,
    val percent: Int
)