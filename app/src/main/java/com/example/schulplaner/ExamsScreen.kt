package com.example.schulplaner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ExamsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val examDao = remember { db.examDao() }
    val subjectDao = remember { db.subjectDao() }
    val statsManager = UserStatsManager(context)
    
    val allExams by examDao.getAllExams().collectAsState(initial = emptyList())
    val subjects by subjectDao.getAllSubjects().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val upcomingExams = allExams.filter { !it.isDone }
    val finishedExams = allExams.filter { it.isDone }

    var showDialog by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }
    var topicsInput by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    var deleteConfirmItem by remember { mutableStateOf<Exam?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Klausur hinzufügen")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)
        ) {
            item {
                Text(
                    "Klausurenkalender", 
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            if (upcomingExams.isNotEmpty()) {
                item {
                    ExamsTimeline(upcomingExams)
                }
            }

            if (upcomingExams.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Keine anstehenden Klausuren.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                items(upcomingExams) { exam ->
                    ExamItem(
                        exam = exam, 
                        onDelete = { deleteConfirmItem = exam },
                        onDone = {
                            scope.launch {
                                examDao.updateExam(exam.copy(isDone = true))
                                statsManager.addExamXp(exam.subject)
                            }
                        }
                    )
                }
            }

            if (finishedExams.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Geschafft 🎉", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(finishedExams) { exam ->
                    ExamItem(
                        exam = exam,
                        onDelete = { deleteConfirmItem = exam },
                        onDone = null // Bereits erledigt
                    )
                }
            }
        }

        if (deleteConfirmItem != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmItem = null },
                title = { Text("Klausur löschen?") },
                text = { Text("Möchtest du diesen Termin wirklich entfernen?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val exam = deleteConfirmItem ?: return@launch
                                val today = java.time.LocalDate.now()
                                val tomorrow = today.plusDays(1).toString()
                                if (exam.date == tomorrow) {
                                    statsManager.triggerExamGhosting()
                                }
                                examDao.deleteExam(exam.id)
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

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Klausur eintragen", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedSubject,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Fach wählen") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { expanded = true }
                            )
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                if (subjects.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Keine Fächer gefunden") },
                                        onClick = { expanded = false }
                                    )
                                }
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            selectedSubject = sub.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = dateInput,
                            onValueChange = { dateInput = it },
                            label = { Text("Datum (JJJJ-MM-TT)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("z.B. 2024-10-25") },
                            trailingIcon = { Icon(Icons.Default.Event, contentDescription = null) }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = topicsInput,
                            onValueChange = { topicsInput = it },
                            label = { Text("Themen") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedSubject.isNotBlank() && dateInput.isNotBlank()) {
                                scope.launch {
                                    examDao.insertExam(Exam(subject = selectedSubject, date = dateInput, topics = topicsInput))
                                    showDialog = false
                                    selectedSubject = ""
                                    dateInput = ""
                                    topicsInput = ""
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Speichern") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Abbrechen") }
                }
            )
        }
    }
}

@Composable
fun ExamsTimeline(exams: List<Exam>) {
    val sortedExams = exams.filter { 
        try { java.time.LocalDate.parse(it.date); true } catch (_: Exception) { false }
    }.sortedBy { it.date }

    if (sortedExams.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Stress-Level Übersicht", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    sortedExams.forEach { _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Du hast ${sortedExams.size} Klausuren geplant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun ExamItem(exam: Exam, onDelete: () -> Unit, onDone: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (exam.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        exam.subject, 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        color = if (exam.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        textDecoration = if (exam.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            exam.date, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (onDone != null && !exam.isDone) {
                    IconButton(
                        onClick = onDone,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Erledigt", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                }
            }
            if (exam.topics.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("THEMEN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    exam.topics, 
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
