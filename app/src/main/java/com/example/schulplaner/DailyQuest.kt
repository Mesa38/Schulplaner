package com.example.schulplaner

data class DailyQuest(
    val id: String,
    val title: String,
    val xpReward: Int,
    val coinReward: Int,
    val target: Int,
    val type: QuestType
)

enum class QuestType {
    STUDY_MINUTES,
    REVIZE_CARDS,
    ADD_GRADE,
    COMPLETE_HOMEWORK,
    CREATE_SUMMARY
}

object QuestPool {
    val potentialQuests = listOf(
        DailyQuest("study_15", "Lerne 15 Minuten", 50, 20, 15, QuestType.STUDY_MINUTES),
        DailyQuest("study_30", "Lerne 30 Minuten", 100, 40, 30, QuestType.STUDY_MINUTES),
        DailyQuest("cards_5", "Wiederhole 5 Karten", 40, 15, 5, QuestType.REVIZE_CARDS),
        DailyQuest("cards_10", "Wiederhole 10 Karten", 80, 30, 10, QuestType.REVIZE_CARDS),
        DailyQuest("grade_1", "Trage eine Note ein", 30, 10, 1, QuestType.ADD_GRADE),
        DailyQuest("hw_1", "Erledige eine Hausaufgabe", 50, 20, 1, QuestType.COMPLETE_HOMEWORK),
        DailyQuest("summary_1", "Erstelle eine Zusammenfassung", 60, 25, 1, QuestType.CREATE_SUMMARY)
    )
}
