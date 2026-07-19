package com.example.schulplaner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatisticsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val subjects by db.subjectDao().getAllSubjects().collectAsState(initial = emptyList())
    val grades by db.gradeDao().getAllGrades().collectAsState(initial = emptyList())
    val focusSessions by db.focusSessionDao().getAllSessions().collectAsState(initial = emptyList())
    val topics by db.topicDao().getAllTopics().collectAsState(initial = emptyList())

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Deine Statistiken",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "Dein Weg zum Abi in Zahlen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val totalMinutes = focusSessions.sumOf { it.durationMinutes }
            StatSummaryCard(
                title = "Lernzeit",
                value = "${totalMinutes / 60}h ${totalMinutes % 60}m",
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
            
            val avgPoints = if (grades.isNotEmpty()) grades.map { it.points }.average() else 0.0
            StatSummaryCard(
                title = "Schnitt",
                value = "${"%.2f".format(avgPoints)} Pkt",
                icon = Icons.Default.AutoGraph,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val summaryCount = topics.count { it.pdfPaths.isNotBlank() }
            StatSummaryCard(
                title = "Summaries",
                value = "$summaryCount Files",
                icon = Icons.Default.HistoryEdu,
                modifier = Modifier.weight(1f)
            )
            
            val flashcardCount = db.flashcardDao().getAllFlashcards().collectAsState(initial = emptyList()).value.size
            StatSummaryCard(
                title = "Flashcards",
                value = "$flashcardCount Cards",
                icon = Icons.Default.Style,
                modifier = Modifier.weight(1f)
            )
        }

        val bestSubject = subjects.map { sub ->
            sub.name to grades.filter { it.subject == sub.name }.map { it.points }.average()
        }.filter { !it.second.isNaN() }.maxByOrNull { it.second }

        if (bestSubject != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Dein bestes Fach", style = MaterialTheme.typography.labelMedium)
                        Text("${bestSubject.first} (${"%.1f".format(bestSubject.second)} Pkt)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Learning Time Chart
        LearningTimeChart(subjects, focusSessions)

        Spacer(modifier = Modifier.height(24.dp))

        // Grade Development
        GradeDevelopmentChart(grades)

        Spacer(modifier = Modifier.height(24.dp))

        // Topic Progress
        TopicProgressSection(subjects, topics)

        Spacer(modifier = Modifier.height(24.dp))

        // Heatmap
        LearningHeatmap(focusSessions)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun LearningHeatmap(sessions: List<FocusSession>) {
    val sessionsByDate = sessions.groupBy { it.date }
    val today = java.time.LocalDate.now()
    val weeksToShow = 12
    val daysToShow = weeksToShow * 7
    val startDate = today.minusDays(daysToShow.toLong() - 1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lern-Aktivität", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val boxWidth = maxWidth
                val spacing = 4.dp
                val itemSize = (boxWidth - (spacing * (weeksToShow - 1))) / weeksToShow

                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    for (week in 0 until weeksToShow) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                            for (dayOfWeek in 0 until 7) {
                                val currentDate = startDate.plusDays((week * 7 + dayOfWeek).toLong())
                                val sessionCount = sessionsByDate[currentDate.toString()]?.size ?: 0
                                val intensity = (sessionCount.toFloat() / 3f).coerceIn(0f, 1f)
                                
                                val color = if (sessionCount == 0) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f + (intensity * 0.7f))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(itemSize)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text("Aktivität der letzten $weeksToShow Wochen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun StatSummaryCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LearningTimeChart(subjects: List<Subject>, sessions: List<FocusSession>) {
    val timePerSubject = sessions.groupBy { it.subject }
        .mapValues { it.value.sumOf { session -> session.durationMinutes } }
        .toList()
        .sortedByDescending { it.second }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lernzeit nach Fach (Minuten)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (timePerSubject.isEmpty()) {
                Text("Noch keine Fokus-Sitzungen aufgezeichnet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            } else {
                val maxTime = timePerSubject.maxOf { it.second }.toFloat()
                timePerSubject.forEach { (subject, minutes) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(subject, style = MaterialTheme.typography.labelLarge)
                            Text("${minutes}m", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(minutes / maxTime)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeDevelopmentChart(grades: List<Grade>) {
    val semesterAvg = (1..4).map { sem ->
        val semGrades = grades.filter { it.semester == sem }
        sem to if (semGrades.isNotEmpty()) semGrades.map { it.points }.average() else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoGraph, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Noten-Entwicklung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                semesterAvg.forEach { (sem, avg) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (avg != null) {
                            Text("${"%.1f".format(avg)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height((avg * 8).dp) // Scale points to height
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Q$sem", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun TopicProgressSection(subjects: List<Subject>, topics: List<Topic>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HistoryEdu, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abi-Readiness (Themen)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            subjects.forEach { subject ->
                val subjectTopics = topics.filter { it.subject == subject.name }
                if (subjectTopics.isNotEmpty()) {
                    val learnedCount = subjectTopics.count { it.status == 2 }
                    val progress = learnedCount.toFloat() / subjectTopics.size.toFloat()
                    
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(subject.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}
