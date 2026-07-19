package com.example.schulplaner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val lastActiveDate: String = "",
    val totalXp: Int = 0,
    val coins: Int = 0,
    val homeworkDoneCount: Int = 0,
    val focusMinutesCount: Int = 0,
    val summariesCreatedCount: Int = 0,
    val cardsReviewedCount: Int = 0,
    val achievementsJson: String = "",
    val dailyQuestsJson: String = "",
    val lastQuestDate: String = "",
    val excusesCount: Int = 0,
    val panikModusCount: Int = 0,
    val earlyStudyCount: Int = 0,
    val totalAppOpenDays: Int = 0,
    val nightEntryCount: Int = 0,
    val longFocusSessionDone: Boolean = false,
    val skipBreakCount: Int = 0,
    val earlyHomeworkCount: Int = 0,
    val totalMediaCount: Int = 0,
    val unlockedDatesJson: String = "",
    val satNightStudyDone: Boolean = false,
    val maxDailyFocusMins: Int = 0,
    val procrastinateCount: Int = 0,
    val betaClicks: Int = 0,
    val consecutiveTopGrades: Int = 0,
    val lastSchroedingerCheck: String = "",
    // NEUE FELDER
    val earlyMorningStreak: Int = 0,
    val lastMorningDate: String = "",
    val nightStudyCount: Int = 0,
    val perfectLongSessions: Int = 0,
    val perfectMonthTrigger: Boolean = false,
    val lastAbsenceCheckMonth: Int = -1
) {
    val xpToNextLevel: Int
        get() = level * 200 + (level * level * 10)

    val levelTitle: String
        get() = when (level) {
            in 1..3 -> "Schulanfänger"
            in 4..7 -> "Wissbegieriger"
            in 8..12 -> "Oberstufen-Novize"
            in 13..18 -> "Klausuren-Bezwinger"
            in 19..25 -> "Abi-Anwärter"
            in 26..35 -> "Prüfungs-Profi"
            in 36..50 -> "Wissens-Gigant"
            in 51..75 -> "Abi-Meister"
            in 76..99 -> "Oberstufen-Elite"
            else -> "Abi-Legende"
        }

    fun addXp(amount: Int): UserStats {
        var newXp = xp + amount
        var newTotalXp = totalXp + amount
        var newLevel = level
        var newCoins = coins + (amount / 5)
        
        while (newXp >= (newLevel * 200 + (newLevel * newLevel * 10))) {
            newXp -= (newLevel * 200 + (newLevel * newLevel * 10))
            newLevel++
            newCoins += 50
        }
        return copy(xp = maxOf(0, newXp), level = newLevel, totalXp = newTotalXp, coins = newCoins)
    }
}
