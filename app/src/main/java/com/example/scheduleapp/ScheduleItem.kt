package it.hamy.schedule

data class ScheduleItem(
    val day: String,
    val time: String,
    val subject: String,
    val teacher: String, // Имя преподавателя
    val room: String // Кабинет
)
