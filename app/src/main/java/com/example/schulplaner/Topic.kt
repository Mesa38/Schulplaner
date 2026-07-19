package com.example.schulplaner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    indices = [androidx.room.Index(value = ["subject", "title"], unique = true)]
)
data class Topic(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String,
    val title: String,
    val status: Int = 0, // 0: Offen, 1: Zusammenfassung, 2: Gelernt
    val pdfPaths: String = "", // Komma-separierte Liste von Pfaden
    val orderIndex: Int = 0
)
