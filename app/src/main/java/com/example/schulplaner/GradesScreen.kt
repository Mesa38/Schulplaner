package com.example.schulplaner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import kotlinx.coroutines.launch

@Composable
fun GradesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.gradeDao() }
    val subjectDao = remember { db.subjectDao() }
    val grades by dao.getAllGrades().collectAsState(initial = emptyList())
    val subjects by subjectDao.getAllSubjects().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val statsManager = remember { UserStatsManager(context) }

    var showDialog by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var points by remember { mutableStateOf("0") }
    var type by remember { mutableStateOf("Klausur") }
    var semester by remember { mutableIntStateOf(1) }
    
    var deleteConfirmItem by remember { mutableStateOf<Grade?>(null) }

    val gradeTypes = listOf("Klausur", "Test", "Mitarbeit", "Referat")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Grade")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Deine Noten", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "0 bis 15 Punkte System", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Summary (Average)
            if (grades.isNotEmpty()) {
                item {
                    var totalPoints = 0.0
                    var totalWeight = 0.0

                    subjects.forEach { sub ->
                        val subjectGrades = grades.filter { it.subject == sub.name }
                        if (subjectGrades.isNotEmpty()) {
                            val klausuren = subjectGrades.filter { it.type == "Klausur" }
                            val mitarbeit = subjectGrades.filter { it.type == "Mitarbeit" }
                            
                            val avgK = if (klausuren.isNotEmpty()) klausuren.map { it.points }.average() else 0.0
                            val avgM = if (mitarbeit.isNotEmpty()) mitarbeit.map { it.points }.average() else 0.0
                            
                            val subjectAvg = (avgK * sub.weightKlausur) + (avgM * sub.weightMitarbeit)
                            totalPoints += subjectAvg
                            totalWeight += 1.0
                        }
                    }
                    
                    val finalAvg = if (totalWeight > 0) totalPoints / totalWeight else 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gesamtschnitt (gewichtet)", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${"%.2f".format(finalAvg)} Pkt", 
                                style = MaterialTheme.typography.displayMedium, 
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            (1..4).forEach { sem ->
                val semesterGrades = grades.filter { it.semester == sem }
                if (semesterGrades.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Semester $sem (Q$sem)", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(semesterGrades) { grade ->
                        GradeItem(grade) {
                            deleteConfirmItem = grade
                        }
                    }
                }
            }
            
            if (grades.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        Text("Noch keine Noten eingetragen.", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        if (deleteConfirmItem != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmItem = null },
                title = { Text("Note löschen?") },
                text = { Text("Möchtest du diese Note wirklich entfernen?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                dao.deleteGrade(deleteConfirmItem!!.id)
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
                title = { Text("Note hinzufügen") },
                text = {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = subject,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Fach wählen") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Text("▼")
                                    }
                                }
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (subjects.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Fächer in Einstellungen anlegen") },
                                        onClick = { expanded = false }
                                    )
                                }
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text("${sub.name} (${sub.type.split(" ")[0]})") },
                                        onClick = {
                                            subject = sub.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Punkte: $points")
                        Slider(
                            value = points.toFloat(),
                            onValueChange = { points = it.toInt().toString() },
                            valueRange = 0f..15f,
                            steps = 14
                        )

                        Text("Semester: $semester")
                        Row {
                            (1..4).forEach { s ->
                                FilterChip(
                                    selected = semester == s,
                                    onClick = { semester = s },
                                    label = { Text(s.toString()) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }

                        Text("Typ:")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            gradeTypes.take(2).forEach { t ->
                                FilterChip(
                                    selected = type == t,
                                    onClick = { type = t },
                                    label = { Text(t) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (subject.isNotBlank()) {
                            scope.launch {
                                val pointsInt = points.toInt()
                                dao.insertGrade(
                                    Grade(
                                        subject = subject, 
                                        points = pointsInt, 
                                        type = type, 
                                        semester = semester,
                                        date = java.time.LocalDate.now().toString()
                                    )
                                )
                                statsManager.addGradeXp(pointsInt, subject)
                                showDialog = false
                                subject = ""
                            }
                        }
                    }) { Text("Speichern") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Abbrechen") }
                }
            )
        }
    }
}

@Composable
fun GradeItem(grade: Grade, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (grade.points >= 5) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.errorContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${grade.points}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (grade.points >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(grade.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${grade.type} • Q${grade.semester}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
