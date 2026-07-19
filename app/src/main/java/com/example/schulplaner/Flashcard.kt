package com.example.schulplaner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String,
    val front: String,
    val back: String,
    val level: Int = 0, // Leitner-System Box (0 bis 4)
    val nextReview: Long = System.currentTimeMillis(),
    val easeFactor: Float = 2.5f, // Für Spaced Repetition (Anki-Stil)
    val interval: Int = 0, // Tage bis zur nächsten Wiederholung
    val failedStreak: Int = 0
)
