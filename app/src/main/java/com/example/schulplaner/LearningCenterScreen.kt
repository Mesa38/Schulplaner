package com.example.schulplaner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import android.content.Intent
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningCenterScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val topicDao = remember { db.topicDao() }
    val flashcardDao = remember { db.flashcardDao() }
    val subjects by db.subjectDao().getAllSubjects().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val statsManager = remember { UserStatsManager(context) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Themen-Omat", "Karteikarten")
    
    var deleteConfirmItem by remember { mutableStateOf<Any?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenterAlignedTopAppBar(
            title = { Text("Lern-Zentrum", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> TopicOmatTab(subjects, topicDao, statsManager, scope)
            1 -> FlashcardsTab(subjects, flashcardDao, statsManager, scope) { deleteConfirmItem = it }
        }
    }

    if (deleteConfirmItem != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmItem = null },
            title = { Text("Karteikarte löschen?") },
            text = { Text("Möchtest du diese Karte permanent entfernen?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (deleteConfirmItem is Flashcard) {
                                flashcardDao.deleteFlashcard(deleteConfirmItem as Flashcard)
                            }
                            deleteConfirmItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmItem = null }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
fun TopicOmatTab(subjects: List<Subject>, topicDao: TopicDao, statsManager: UserStatsManager, scope: kotlinx.coroutines.CoroutineScope) {
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    val topics by (selectedSubject?.let { topicDao.getTopicsForSubject(it) } ?: topicDao.getAllTopics()).collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Wähle ein Fach:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (subjects.isEmpty()) {
            Text("Lege zuerst Fächer in den Einstellungen an.", color = MaterialTheme.colorScheme.error)
        } else {
            ScrollableTabRow(
                selectedTabIndex = subjects.indexOfFirst { it.name == selectedSubject }.let { if (it == -1) 0 else it },
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                subjects.forEach { sub ->
                    FilterChip(
                        selected = selectedSubject == sub.name,
                        onClick = { selectedSubject = sub.name },
                        label = { Text(sub.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedSubject == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Wähle ein Fach aus, um deine Themen zu sehen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LaunchedEffect(selectedSubject) {
                scope.launch {
                    val currentSubject = selectedSubject ?: return@launch
                    val count = topicDao.getTopicCountForSubject(currentSubject)
                    if (count == 0) {
                        val trimmedName = currentSubject.trim()
                        val pureName = trimmedName.split(" ")[0]
                        
                        val topicsToInsert = SubjectTopics.predefinedTopics[trimmedName]
                            ?: SubjectTopics.predefinedTopics[pureName]
                            ?: SubjectTopics.predefinedTopics.entries.find { it.key.startsWith(pureName) }?.value
                        
                        topicsToInsert?.forEachIndexed { idx, title ->
                            topicDao.insertTopic(Topic(subject = currentSubject, title = title, orderIndex = idx))
                        }
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(topics) { topic ->
                    TopicItem(topic, onUpdate = { newStatus ->
                        scope.launch {
                            if (topic.status == 2 && newStatus == 0) {
                                statsManager.triggerSisyphus()
                            }
                            topicDao.updateTopic(topic.copy(status = newStatus))
                            if (newStatus > 0) statsManager.addTopicXp(newStatus, topic.subject)
                            
                            // Check Tabula Rasa
                            val all = topicDao.getAllTopics().first()
                            if (all.isNotEmpty() && all.all { it.status == 2 }) {
                                statsManager.triggerTabulaRasa()
                            }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun TopicItem(topic: Topic, onUpdate: (Int) -> Unit) {
    val context = LocalContext.current
    val statusColor = when (topic.status) {
        1 -> Color(0xFFFFC107) // Gelb
        2 -> Color(0xFF4CAF50) // Grün
        else -> Color(0xFFF44336) // Rot
    }
    
    val statusText = when (topic.status) {
        1 -> "In Arbeit"
        2 -> "Gelernt"
        else -> "Offen"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(topic.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                
                val pdfList = if (topic.pdfPaths.isEmpty()) emptyList() else topic.pdfPaths.split(",").filter { it.isNotBlank() }
                
                if (pdfList.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp).clickable {
                            // Öffne die erste PDF als Shortcut
                            val file = File(pdfList.first())
                            if (file.exists()) {
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "PDF öffnen"))
                                } catch (e: Exception) {
                                    // Fehler beim Öffnen
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${pdfList.size} Zusammenfassungen bereit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onUpdate((topic.status + 1) % 3) }
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FlashcardsTab(subjects: List<Subject>, dao: FlashcardDao, statsManager: UserStatsManager, scope: kotlinx.coroutines.CoroutineScope, onDeleteRequest: (Flashcard) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf<String?>(subjects.firstOrNull()?.name) }
    var frontText by remember { mutableStateOf("") }
    var backText by remember { mutableStateOf("") }
    
    val cards by (selectedSubject?.let { dao.getFlashcardsForSubject(it) } ?: dao.getAllFlashcards()).collectAsState(initial = emptyList())
    val sortedCards = cards.sortedBy { it.nextReview }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fach wählen:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, null)
                Text("Neue Karte")
            }
        }
        
        if (subjects.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = subjects.indexOfFirst { it.name == selectedSubject }.let { if (it == -1) 0 else it },
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                subjects.forEach { sub ->
                    FilterChip(
                        selected = selectedSubject == sub.name,
                        onClick = { selectedSubject = sub.name },
                        label = { Text(sub.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (sortedCards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Keine Karten für dieses Fach vorhanden.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sortedCards) { card ->
                    FlashcardItem(
                        card = card, 
                        onRevealed = {
                            scope.launch { statsManager.addFlashcardReviewXp(card.subject) }
                        },
                        onDelete = {
                            onDeleteRequest(card)
                        },
                        onAnswer = { score ->
                            scope.launch {
                                // Spaced Repetition logic
                                val newFailedStreak = if (score <= 1) card.failedStreak + 1 else 0
                                if (newFailedStreak >= 5) {
                                     statsManager.addAmnesieCount()
                                }

                                val newEaseFactor = max(1.3f, card.easeFactor + (0.1f - (5 - score) * (0.08f + (5 - score) * 0.02f)))
                                val newInterval = if (score < 3) 0 else if (card.interval == 0) 1 else if (card.interval == 1) 6 else (card.interval * newEaseFactor).toInt()
                                val nextReview = System.currentTimeMillis() + (newInterval * 24 * 60 * 60 * 1000L)
                                
                                dao.updateFlashcard(card.copy(
                                    easeFactor = newEaseFactor,
                                    interval = newInterval,
                                    nextReview = nextReview,
                                    failedStreak = newFailedStreak
                                ))
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Karte erstellen") },
            text = {
                Column {
                    OutlinedTextField(
                        value = frontText, 
                        onValueChange = { frontText = it }, 
                        label = { Text("Vorderseite (Frage)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = backText, 
                        onValueChange = { backText = it }, 
                        label = { Text("Rückseite (Antwort)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (frontText.isNotBlank() && backText.isNotBlank() && selectedSubject != null) {
                        scope.launch {
                            val subjectName = selectedSubject!!
                            dao.insertFlashcard(Flashcard(subject = subjectName, front = frontText, back = backText))
                            
                            // Check Lost in Translation
                            val foreignLanguages = listOf("Englisch", "Französisch", "Spanisch", "Latein", "Italienisch", "Russisch")
                            if (foreignLanguages.any { subjectName.contains(it, true) }) {
                                val count = dao.getFlashcardsForSubject(subjectName).first().size
                                if (count >= 100) {
                                    statsManager.triggerLostTranslation()
                                }
                            }

                            showAddDialog = false
                            frontText = ""
                            backText = ""
                        }
                    }
                }) { Text("Hinzufügen") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
fun FlashcardItem(card: Flashcard, onRevealed: () -> Unit, onDelete: () -> Unit, onAnswer: (Int) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { 
            if (!revealed) onRevealed()
            revealed = !revealed 
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (revealed) "ANTWORT" else "FRAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(if (revealed) card.back else card.front, style = MaterialTheme.typography.bodyLarge)
            
            if (revealed) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FlashcardScoreButton("Schwer", Color(0xFFF44336)) { onAnswer(1); revealed = false }
                    FlashcardScoreButton("Ok", Color(0xFFFFC107)) { onAnswer(3); revealed = false }
                    FlashcardScoreButton("Einfach", Color(0xFF4CAF50)) { onAnswer(5); revealed = false }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardScoreButton(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
