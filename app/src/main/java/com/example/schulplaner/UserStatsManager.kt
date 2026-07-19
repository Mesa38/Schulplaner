package com.example.schulplaner

import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class UserStatsManager(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.userStatsDao()

    private fun updateWidget() {
        val intent = Intent(context, AbiWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, AbiWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    suspend fun onAppOpen() {
        try {
            val stats = dao.getUserStats().firstOrNull() ?: UserStats()
            val today = LocalDate.now()
            val todayStr = today.toString()
            
            var updated = stats
            
            // Perfect Month Check
            if (stats.lastAbsenceCheckMonth != today.monthValue) {
                val lastMonth = today.minusMonths(1)
                val absences = db.absenceDao().getAllAbsences().firstOrNull() ?: emptyList()
                val hadAbsenceLastMonth = absences.any { 
                    try {
                        val d = LocalDate.parse(it.date)
                        d.monthValue == lastMonth.monthValue && d.year == lastMonth.year
                    } catch(e: Exception) { false }
                }
                if (!hadAbsenceLastMonth && stats.lastAbsenceCheckMonth != -1) {
                    updated = updated.copy(perfectMonthTrigger = true)
                }
                updated = updated.copy(lastAbsenceCheckMonth = today.monthValue)
            }

            if (stats.lastActiveDate != todayStr) {
                val nowTime = LocalTime.now()
                val isEarlyMorning = nowTime.isBefore(LocalTime.of(7, 0))
                
                updated = updated.copy(
                    totalAppOpenDays = stats.totalAppOpenDays + 1,
                    lastActiveDate = todayStr,
                    earlyMorningStreak = if (isEarlyMorning) stats.earlyMorningStreak + 1 else 0
                )
                updated = checkDailyQuests(updated)
            }

            val now = LocalTime.now()
            if (now.hour >= 1 && now.hour <= 4) {
                updated = updated.copy(nightStudyCount = updated.nightStudyCount + 1)
            }
            if (today.dayOfWeek == DayOfWeek.SUNDAY && now.hour in 18..19) {
                updated = updated.copy(achievementsJson = updated.achievementsJson + ",sunday_depression_trigger")
            }

            // Schrödinger Check
            val lastCheck = try { LocalDate.parse(stats.lastSchroedingerCheck) } catch(e: Exception) { LocalDate.MIN }
            if (lastCheck != today) {
                val exams = db.examDao().getAllExams().firstOrNull() ?: emptyList()
                val grades = db.gradeDao().getAllGrades().firstOrNull() ?: emptyList()
                val threeWeeksAgo = today.minusWeeks(3).toString()
                
                val schroedingerFound = exams.any { exam ->
                    exam.date < threeWeeksAgo && grades.none { it.subject == exam.subject }
                }
                
                updated = updated.copy(
                    lastSchroedingerCheck = todayStr,
                    achievementsJson = if (schroedingerFound) updated.achievementsJson + ",schroedinger_trigger" else updated.achievementsJson
                )
            }

            dao.updateStats(updated)
            checkAchievements(updated)
            updateWidget()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkDailyQuests(stats: UserStats): UserStats {
        val today = LocalDate.now().toString()
        if (stats.lastQuestDate != today) {
            val newQuests = QuestPool.potentialQuests.shuffled().take(3).map { it.id }.joinToString(",")
            return stats.copy(
                lastQuestDate = today,
                dailyQuestsJson = newQuests,
                homeworkDoneCount = 0,
                focusMinutesCount = 0,
                cardsReviewedCount = 0
            )
        }
        return stats
    }

    suspend fun addHomeworkXp(subjectName: String, daysBefore: Long = 0) {
        val stats = dao.getUserStats().firstOrNull() ?: UserStats()
        val statsNewDay = checkNewDay(stats)
        
        updateSubjectXp(subjectName, 25)

        var updated = statsNewDay.addXp(25)
            .copy(homeworkDoneCount = statsNewDay.homeworkDoneCount + 1)
        
        if (daysBefore >= 2) {
            updated = updated.copy(earlyHomeworkCount = updated.earlyHomeworkCount + 1)
        }

        val now = LocalTime.now()
        if (now.hour in 1..4) {
            updated = updated.copy(nightEntryCount = updated.nightEntryCount + 1)
        }

        checkAchievements(updated)
    }

    suspend fun addFocusXp(minutes: Int, subjectName: String) {
        val stats = dao.getUserStats().firstOrNull() ?: UserStats()
        val statsNewDay = checkNewDay(stats)

        updateSubjectXp(subjectName, minutes * 3)

        val now = LocalTime.now()
        val today = LocalDate.now()
        val isEarly = now.hour < 6 || (now.hour == 6 && now.minute <= 45)
        val isSatNight = today.dayOfWeek == DayOfWeek.SATURDAY && now.hour >= 20
        
        var updated = statsNewDay.addXp(minutes * 3)
            .copy(
                focusMinutesCount = statsNewDay.focusMinutesCount + minutes,
                earlyStudyCount = if (isEarly) statsNewDay.earlyStudyCount + 1 else statsNewDay.earlyStudyCount,
                satNightStudyDone = statsNewDay.satNightStudyDone || isSatNight
            )
        
        // Track max daily focus
        val sessionsToday = db.focusSessionDao().getAllSessions().firstOrNull()?.filter { it.date == today.toString() } ?: emptyList()
        val totalToday = sessionsToday.sumOf { it.durationMinutes } + minutes
        if (totalToday > updated.maxDailyFocusMins) {
            updated = updated.copy(maxDailyFocusMins = totalToday)
        }

        if (minutes >= 120) {
            updated = updated.copy(longFocusSessionDone = true)
        }

        if (subjectName.contains("Deutsch", true)) {
            val sessions = db.focusSessionDao().getSessionsForSubject(subjectName).firstOrNull() ?: emptyList()
            val totalDeutsch = sessions.sumOf { it.durationMinutes } + minutes
            if (totalDeutsch >= 600) {
                updated = updated.copy(achievementsJson = updated.achievementsJson + ",goethe_proud_trigger")
            }
        }

        if (now.hour >= 22 && minutes >= 45) {
            val exams = db.examDao().getAllExams().firstOrNull() ?: emptyList()
            val tomorrow = today.plusDays(1).toString()
            if (exams.any { it.date == tomorrow }) {
                updated = updated.copy(achievementsJson = updated.achievementsJson + ",late_study_trigger")
            }
        }

        checkAchievements(updated)
    }

    suspend fun addGradeXp(points: Int, subjectName: String) {
        val stats = dao.getUserStats().firstOrNull() ?: UserStats()
        val statsNewDay = checkNewDay(stats)

        val amount = when {
            points >= 13 -> 120
            points >= 10 -> 80
            else -> 20
        }
        updateSubjectXp(subjectName, amount)
        
        var updated = statsNewDay.addXp(amount)
        if (points >= 13) {
            updated = updated.copy(consecutiveTopGrades = updated.consecutiveTopGrades + 1)
        } else {
            updated = updated.copy(consecutiveTopGrades = 0)
        }

        checkAchievements(updated)
    }

    suspend fun triggerPanikModus() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(panikModusCount = stats.panikModusCount + 1)
        checkAchievements(updated)
    }

    suspend fun triggerPunktlandung() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",punktlandung_trigger")
        checkAchievements(updated)
    }

    suspend fun triggerUeberflieger() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",ueberflieger_trigger")
        checkAchievements(updated)
    }

    suspend fun addTopicXp(status: Int, subjectName: String) {
        val currentStats = dao.getUserStats().first() ?: UserStats()
        val amount = if (status == 2) 100 else 50
        
        updateSubjectXp(subjectName, amount)
        
        var updated = currentStats.addXp(amount)
        if (status == 1) {
            updated = updated.copy(summariesCreatedCount = updated.summariesCreatedCount + 1)
        }
        
        if (status == 2) {
            val topics = db.topicDao().getTopicsForSubject(subjectName).first()
            if (topics.isNotEmpty() && topics.all { it.status == 2 }) {
                updated = updated.copy(achievementsJson = updated.achievementsJson + ",gallic_village_trigger")
            }
        }
        
        checkAchievements(updated)
    }

    suspend fun addFlashcardReviewXp(subjectName: String) {
        val stats = dao.getUserStats().first() ?: UserStats()
        updateSubjectXp(subjectName, 10)
        val updated = stats.addXp(10).copy(cardsReviewedCount = stats.cardsReviewedCount + 1)
        checkAchievements(updated)
    }

    suspend fun addAbsenceExcuse() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(excusesCount = stats.excusesCount + 1)
        checkAchievements(updated)
    }

    suspend fun skipBreak() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(skipBreakCount = stats.skipBreakCount + 1)
        checkAchievements(updated)
    }

    suspend fun updateMediaCount(countChange: Int, subjectName: String) {
        val stats = dao.getUserStats().firstOrNull() ?: UserStats()
        val allTopics = db.topicDao().getTopicsForSubject(subjectName).firstOrNull() ?: emptyList()
        val subjectMediaCount = allTopics.sumOf { it.pdfPaths.split(",").filter { p -> p.isNotBlank() }.size }
        
        var achievements = stats.achievementsJson
        if (subjectMediaCount >= 30 && !achievements.contains("spickzettel_messie_trigger")) {
             achievements += ",spickzettel_messie_trigger"
        }
        
        val updated = stats.copy(totalMediaCount = stats.totalMediaCount + countChange, achievementsJson = achievements)
        checkAchievements(updated)
    }

    suspend fun triggerCyberSpy() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",cyber_spy_trigger")
        checkAchievements(updated)
    }

    suspend fun addBetaClick() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(betaClicks = stats.betaClicks + 1)
        checkAchievements(updated)
    }

    suspend fun addProcrastination() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(procrastinateCount = stats.procrastinateCount + 1)
        checkAchievements(updated)
    }

    suspend fun triggerSisyphus() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",sisyphus_trigger")
        checkAchievements(updated)
    }

    suspend fun triggerTabulaRasa() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",tabula_rasa_trigger")
        checkAchievements(updated)
    }

    suspend fun addAmnesieCount() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",amnesie_trigger")
        checkAchievements(updated)
    }

    suspend fun triggerZeroAbsences() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",zero_absences_trigger")
        checkAchievements(updated)
    }

    suspend fun triggerEhrenrundeSafe() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",ehrenrunde_safe_trigger")
        checkAchievements(updated)
    }

    suspend fun triggerEinsteinKomplex() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",einstein_komplex_trigger")
        checkAchievements(updated)
    }

    suspend fun triggerTiefstapler() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",tiefstapler_trigger")
        checkAchievements(updated)
    }

    suspend fun triggerLostTranslation() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",lost_translation_trigger")
        checkAchievements(updated)
    }

    suspend fun triggerExamGhosting() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(achievementsJson = stats.achievementsJson + ",exam_ghosting_trigger")
        checkAchievements(updated)
    }

    suspend fun addExamXp(subjectName: String) {
        val stats = dao.getUserStats().firstOrNull() ?: UserStats()
        val statsNewDay = checkNewDay(stats)
        updateSubjectXp(subjectName, 150)
        val updated = statsNewDay.addXp(150)
        checkAchievements(updated)
    }

    suspend fun triggerEarlyBird() {
        val stats = dao.getUserStats().first() ?: UserStats()
        val updated = stats.copy(earlyStudyCount = stats.earlyStudyCount + 1)
        checkAchievements(updated)
    }

    private suspend fun updateSubjectXp(name: String, amount: Int) {
        val subjectList = db.subjectDao().getAllSubjects().firstOrNull() ?: emptyList()
        val subject = subjectList.find { it.name == name }
        subject?.let {
            db.subjectDao().insertSubject(it.addXp(amount))
        }
    }

    private suspend fun checkAchievements(stats: UserStats) {
        val unlockedIds = stats.achievementsJson.split(",").filter { it.isNotBlank() }.toMutableSet()
        val unlockedDates = stats.unlockedDatesJson.split(",").filter { it.isNotBlank() }.toMutableList()
        val subjects = db.subjectDao().getAllSubjects().firstOrNull() ?: emptyList()
        val grades = db.gradeDao().getAllGrades().firstOrNull() ?: emptyList()
        val sessions = db.focusSessionDao().getAllSessions().firstOrNull() ?: emptyList()
        
        var newlyUnlocked = false
        val today = LocalDate.now().toString()
        
        AchievementManager.allAchievements.forEach { achievement ->
            if (!unlockedIds.contains(achievement.id) && achievement.condition(stats, subjects, grades, sessions)) {
                unlockedIds.add(achievement.id)
                unlockedDates.add("${achievement.id}:$today")
                newlyUnlocked = true
                
                // Show notification for achievement
                showAchievementNotification(achievement.title, achievement.description)
            }
        }
        
        val finalStats = if (newlyUnlocked) {
            stats.copy(
                achievementsJson = unlockedIds.joinToString(","),
                unlockedDatesJson = unlockedDates.joinToString(","),
                coins = stats.coins + 150
            )
        } else stats
        
        dao.updateStats(finalStats)
        updateWidget()
    }

    private fun showAchievementNotification(title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "achievements"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Erfolge", android.app.NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("🏆 Erfolg freigeschaltet!")
            .setContentText("$title: $message")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun checkNewDay(stats: UserStats): UserStats {
        val today = LocalDate.now()
        val todayStr = today.toString()
        if (stats.lastActiveDate != todayStr) {
            val yesterday = today.minusDays(1).toString()
            val newStreak = if (stats.lastActiveDate == yesterday) stats.streak + 1 else 1
            
            // Check for "Last Man Standing" trigger (Summer Holidays approx start)
            var achievements = stats.achievementsJson
            if (today.monthValue == 7 && today.dayOfMonth == 1) {
                achievements += ",last_man_standing_trigger"
            }

            return stats.copy(streak = newStreak, lastActiveDate = todayStr, achievementsJson = achievements)
        }
        return stats
    }
}
