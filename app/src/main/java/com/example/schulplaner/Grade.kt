package com.example.schulplaner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grades")
data class Grade(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String = "",
    val points: Int = 0,
    val type: String = "", // Klausur, Test, Mitarbeit
    val semester: Int = 1, // 1, 2, 3, 4
    val date: String = ""
)
