package com.example.schulplaner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dayOfWeek: Int = 1, // 1 for Monday, 5 for Friday
    val hour: Int = 1,      // 1 to 10
    val subject: String = ""
)
