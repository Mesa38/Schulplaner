package com.example.schulplaner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "homework")
data class Homework(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String = "",
    val title: String = "",
    val dueDate: String = "",
    val done: Boolean = false,
    val imagePath: String? = null,
    val description: String = "",
    val postponeCount: Int = 0
)
