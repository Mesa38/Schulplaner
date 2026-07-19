package com.example.schulplaner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String = "",
    val durationMinutes: Int = 0,
    val date: String = "", // JJJJ-MM-TT
    val timestamp: Long = System.currentTimeMillis()
)
