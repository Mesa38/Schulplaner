package com.example.schulplaner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    
    val homeworkCount by db.homeworkDao().getAllHomework().collectAsState(initial = emptyList())
    val exams by db.examDao().getAllExams().collectAsState(initial = emptyList())
    val grades by db.gradeDao().getAllGrades().collectAsState(initial = emptyList())
    val subjects by db.subjectDao().getAllSubjects().collectAsState(initial = emptyList())
    val topics by db.topicDao().getAllTopics().collectAsState(initial = emptyList())
    val userStats by db.userStatsDao().getUserStats().collectAsState(initial = UserStats())

    val stats = userStats ?: UserStats()
    var previousLevel by remember { mutableIntStateOf(stats.level) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Nur beim ersten Start einmalig prüfen, ob Fächer da sind
    LaunchedEffect(Unit) {
        val currentSubjects = db.subjectDao().getAllSubjects().firstOrNull() ?: emptyList()
        if (currentSubjects.isEmpty()) {
            db.subjectDao().insertSubject(Subject(name = "Chemie LK", type = "eA", abiType = "P1"))
            db.subjectDao().insertSubject(Subject(name = "Mathe LK", type = "eA", abiType = "P2"))
            db.subjectDao().insertSubject(Subject(name = "Physik LK", type = "eA", abiType = "P3"))
            db.subjectDao().insertSubject(Subject(name = "Englisch PK", type = "gA", abiType = "P4"))
            db.subjectDao().insertSubject(Subject(name = "Politik-Wirtschaft PK", type = "gA", abiType = "P5"))
        }
        UserStatsManager(context).onAppOpen()
    }

    LaunchedEffect(stats.level) {
        if (stats.level > previousLevel) {
            snackbarHostState.showSnackbar("🎉 LEVEL UP! Du bist jetzt Level ${stats.level}: ${stats.levelTitle}")
            previousLevel = stats.level
        }
    }

    val nextExam = exams.filter { 
        try { LocalDate.parse(it.date) >= LocalDate.now() } catch (_: Exception) { false }
    }.firstOrNull()

    val daysToExam = nextExam?.let {
        try {
            val examDate = LocalDate.parse(it.date)
            ChronoUnit.DAYS.between(LocalDate.now(), examDate)
        } catch (_: Exception) { null }
    }
    
    // Durchschnittsberechnung
    var totalPoints = 0.0
    var totalWeight = 0.0
    subjects.forEach { subject ->
        val subjectGrades = grades.filter { it.subject == subject.name }
        if (subjectGrades.isNotEmpty()) {
            val klausuren = subjectGrades.filter { it.type == "Klausur" }
            val mitarbeit = subjectGrades.filter { it.type == "Mitarbeit" }
            val avgK = if (klausuren.isNotEmpty()) klausuren.map { it.points }.average() else 0.0
            val avgM = if (mitarbeit.isNotEmpty()) mitarbeit.map { it.points }.average() else 0.0
            val subjectAvg = (avgK * subject.weightKlausur) + (avgM * subject.weightMitarbeit)
            totalPoints += subjectAvg
            totalWeight += 1.0
        }
    }
    val avgPoints = if (totalWeight > 0) totalPoints / totalWeight else 0.0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            HeaderSection(daysToExam)

            Spacer(modifier = Modifier.height(16.dp))
            
            CurrentLessonCard(db)

            Spacer(modifier = Modifier.height(16.dp))
            
            MotivationQuote()

            Spacer(modifier = Modifier.height(16.dp))

            // NEU: QUICK ALERTS
            QuickAlerts(homeworkCount, exams, subjects, grades)

            Spacer(modifier = Modifier.height(24.dp))

            LernEmpfehlung(nextExam, topics)

            Spacer(modifier = Modifier.height(24.dp))

            LevelCard(stats)

            Spacer(modifier = Modifier.height(24.dp))
            
            SubjectLevelRow(subjects)

            Spacer(modifier = Modifier.height(20.dp))

            QuestCard(stats)

            Spacer(modifier = Modifier.height(24.dp))

            AchievementRow(stats) { navController.navigate("achievements") }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Dein Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            // Fixed height Grid logic to allow the whole page to scroll
            val gridItems = listOf(
                DashboardCardData("Fokus-Timer", "Konzentriert lernen", Icons.Default.Timer, Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1976D2))), "timer"),
                DashboardCardData("Hausaufgaben", "${homeworkCount.size} offen", Icons.Default.Home, Brush.verticalGradient(listOf(Color(0xFF81C784), Color(0xFF388E3C))), "homework"),
                DashboardCardData("Nächste Klausur", nextExam?.let { "${it.subject}\n${it.date}" } ?: "Keine anstehend", Icons.AutoMirrored.Filled.Assignment, Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFF57C00))), "exams"),
                DashboardCardData("Notenschnitt", "${"%.2f".format(avgPoints)} Pkt", Icons.Default.Star, Brush.verticalGradient(listOf(Color(0xFFBA68C8), Color(0xFF7B1FA2))), "grades"),
                DashboardCardData("Statistiken", "Erfolg messen", Icons.Default.AutoGraph, Brush.verticalGradient(listOf(Color(0xFF26A69A), Color(0xFF00695C))), "statistics"),
                DashboardCardData("Abi-Rechner", "Punkte planen", Icons.Default.Calculate, Brush.verticalGradient(listOf(Color(0xFFE57373), Color(0xFFD32F2F))), "abi_calc"),
                DashboardCardData("Media Hub", "PDF Zusammenfassungen", Icons.Default.CloudQueue, Brush.verticalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))), "media_hub"),
                DashboardCardData("Einstellungen", "Setup", Icons.Default.Settings, Brush.verticalGradient(listOf(Color(0xFF90A4AE), Color(0xFF455A64))), "settings")
            )

            gridItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            DashboardCard(
                                title = item.title,
                                info = item.info,
                                icon = item.icon,
                                gradient = item.gradient,
                                onClick = { navController.navigate(item.route) }
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            DashboardCard(
                title = "Fehlstunden",
                info = "Entschuldigungen & Tracking",
                icon = Icons.Default.AssignmentLate,
                gradient = Brush.verticalGradient(listOf(Color(0xFFE57373), Color(0xFFC62828))),
                onClick = { navController.navigate("absences") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // NEU: Lern-Zentrum Highlight
            DashboardCard(
                title = "Lern-Zentrum",
                info = "Karteikarten & Themen-Omat",
                icon = Icons.Default.AutoStories,
                gradient = Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))),
                onClick = { navController.navigate("learning_center") }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

data class DashboardCardData(
    val title: String,
    val info: String,
    val icon: ImageVector,
    val gradient: Brush,
    val route: String
)

@Composable
fun CurrentLessonCard(db: AppDatabase) {
    val entries by db.timetableDao().getAllEntries().collectAsState(initial = emptyList())
    val now = java.time.LocalTime.now()
    val dayIndex = java.time.LocalDate.now().dayOfWeek.value
    
    // Find current or next lesson
    val currentSlot = timeSlots.find { slot ->
        val start = java.time.LocalTime.parse(slot.start)
        val end = java.time.LocalTime.parse(slot.end)
        now.isAfter(start) && now.isBefore(end)
    }
    
    val currentEntry = entries.find { it.dayOfWeek == dayIndex && it.hour == currentSlot?.hour }

    if (currentEntry != null && currentSlot != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${currentSlot.hour}", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("JETZT GERADE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                    Text(currentEntry.subject, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                    Text("${currentSlot.start} - ${currentSlot.end}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
fun QuickAlerts(homework: List<Homework>, exams: List<Exam>, subjects: List<Subject>, grades: List<Grade>) {
    val alerts = mutableListOf<Pair<String, ImageVector>>()
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1).toString()
    
    if (homework.any { it.dueDate == tomorrow }) {
        alerts.add("Morgen Hausaufgaben fällig!" to Icons.AutoMirrored.Filled.Assignment)
    }
    if (exams.any { it.date == tomorrow }) {
        alerts.add("KLAUSUR MORGEN!" to Icons.Default.PriorityHigh)
    }
    
    // Check if any subject has a bad average
    subjects.forEach { sub ->
        val sg = grades.filter { it.subject == sub.name }
        if (sg.isNotEmpty()) {
            val avg = sg.map { it.points }.average()
            if (avg < 5.0) {
                alerts.add("Unterkurs-Gefahr in ${sub.name}!" to Icons.Default.Warning)
            }
        }
    }

    if (alerts.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Wichtige Hinweise", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                alerts.forEach { alert ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(alert.second, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(alert.first, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}

@Composable
fun LernEmpfehlung(nextExam: Exam?, topics: List<Topic>) {
    if (nextExam == null) return

    val examTopics = topics.filter { it.subject == nextExam.subject }
    val openTopics = examTopics.filter { it.status < 2 }
    
    if (openTopics.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.onTertiary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Lern-Empfehlung", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                Text(
                    "Bereite dich auf ${nextExam.subject} vor! Wiederhole heute: \"${openTopics.random().title}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MotivationQuote() {
    val quotes = listOf(
        "Der Weg zum Abi ist kein Sprint, sondern ein Marathon. 🏃",
        "15 Punkte sind kein Traum, sondern ein Ziel. 🎯",
        "Wissen ist Macht, aber Durchhalten ist der Schlüssel. 🔑",
        "Heute lernen für das Ich von morgen. 🚀",
        "Jede Karteikarte bringt dich näher ans Zeugnis. 🎓"
    )
    val quote = remember { quotes.random() }
    
    Text(
        text = quote,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun HeaderSection(daysToExam: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hallo! 👋",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Bereit für den Abi-Erfolg?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (daysToExam != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$daysToExam",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "TAGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun LevelCard(stats: UserStats) {
    val progress by animateFloatAsState(
        targetValue = stats.xp.toFloat() / stats.xpToNextLevel.toFloat(),
        label = "XP Progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Level ${stats.level}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stats.levelTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    if (stats.streak > 0) {
                        Surface(
                            color = Color(0xFFFF9800),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, "", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${stats.streak}d", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.MonetizationOn, "", tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${stats.coins}", color = Color(0xFFFFA000), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Gesamt: ${stats.totalXp} XP • ${stats.xp} / ${stats.xpToNextLevel} XP bis Level ${stats.level + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun AchievementRow(stats: UserStats, onNavigate: () -> Unit) {
    val unlockedIds = stats.achievementsJson.split(",").filter { it.isNotBlank() }
    
    Column(modifier = Modifier.clickable { onNavigate() }) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Erfolge", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AchievementManager.allAchievements.take(5).forEach { achievement ->
                val isUnlocked = unlockedIds.contains(achievement.id)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isUnlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        achievement.icon, 
                        contentDescription = achievement.title,
                        tint = if (isUnlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectLevelRow(subjects: List<Subject>) {
    Column {
        Text("Fächer-Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            subjects.forEach { subject ->
                Card(
                    modifier = Modifier.width(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(subject.name.take(3).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                        Text("Lvl ${subject.level}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { subject.xp.toFloat() / subject.xpToNextLevel.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestCard(stats: UserStats) {
    val activeQuestIds = stats.dailyQuestsJson.split(",").filter { it.isNotBlank() }
    val activeQuests = activeQuestIds.mapNotNull { id -> QuestPool.potentialQuests.find { it.id == id } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TaskAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Daily Quests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (activeQuests.isEmpty()) {
                Text("Keine Quests verfügbar. Starte die App morgen neu!", style = MaterialTheme.typography.bodySmall)
            } else {
                activeQuests.forEach { quest ->
                    val progress = when(quest.type) {
                        QuestType.STUDY_MINUTES -> stats.focusMinutesCount
                        QuestType.COMPLETE_HOMEWORK -> stats.homeworkDoneCount
                        QuestType.REVIZE_CARDS -> stats.cardsReviewedCount
                        else -> 0
                    }
                    val isDone = progress >= quest.target
                    QuestItem(quest.title, isDone)
                }
            }
        }
    }
}

@Composable
fun QuestItem(text: String, isDone: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isDone) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
        )
    }
}

@Composable
fun DashboardCard(
    title: String,
    info: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .background(gradient, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column {
                Text(
                    text = title, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = info, 
                    fontSize = 11.sp, 
                    lineHeight = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2
                )
            }
        }
    }
}
