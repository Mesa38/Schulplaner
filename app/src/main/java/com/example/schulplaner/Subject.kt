package com.example.schulplaner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "subjects",
    indices = [androidx.room.Index(value = ["name"], unique = true)]
)
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val type: String = "", // eA, gA, Normal
    val weightKlausur: Float = 0.5f,
    val weightMitarbeit: Float = 0.5f,
    val weightTest: Float = 0.0f,
    val weightReferat: Float = 0.0f,
    val abiType: String? = null,
    val xp: Int = 0,
    val level: Int = 1,
    val color: Int = 0xFF1A73E8.toInt(), // Default Abi-Blue
    val targetPoints: Int = 10 // Standard-Ziel: 10 Punkte (Zwei Minus)
) {
    val xpToNextLevel: Int get() = level * 100
    
    fun addXp(amount: Int): Subject {
        var newXp = xp + amount
        var newLevel = level
        while (newXp >= newLevel * 100) {
            newXp -= newLevel * 100
            newLevel++
        }
        return copy(xp = newXp, level = newLevel)
    }
}
