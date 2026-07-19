package com.example.schulplaner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "absences")
data class Absence(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val subject: String,
    val reason: String = "",
    val isExcused: Boolean = false,
    val hours: Int = 1
)
