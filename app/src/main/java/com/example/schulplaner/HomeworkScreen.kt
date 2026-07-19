package com.example.schulplaner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.homeworkDao() }
    val subjectDao = remember { db.subjectDao() }

    val homeworkList by dao.getAllHomework().collectAsState(initial = emptyList())
    val subjects by subjectDao.getAllSubjects().collectAsState(initial = emptyList())

    var subject by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDateText by remember { mutableStateOf("Datum wählen") }

    var deleteConfirmItem by remember { mutableStateOf<Homework?>(null) }

    val scope = rememberCoroutineScope()
    val statsManager = remember { UserStatsManager(context) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) capturedImagePath = null
    }

    Scaffold { padding ->
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
                    "Hausaufgaben", 
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            item {
                // Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Neue Aufgabe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Titel") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = subject,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Fach wählen") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            subject = sub.name
                                            expanded = false
                                        }
                                    )
                                }
                                if (subjects.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Keine Fächer gefunden") },
                                        onClick = { expanded = false }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Modern Date Picker Trigger
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedDateText,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Fällig am") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                                    }
                                }
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                        }
                        
                        if (selectedDateText != "Datum wählen") {
                            val selectedDate = LocalDate.parse(selectedDateText)
                            val diff = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), selectedDate)
                            if (diff in 0..2) {
                                Text(
                                    text = if (diff == 0L) "⚠️ Heute fällig!" else "⚠️ Fällig in $diff Tagen!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Zusatzinformationen (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    val file = File(context.filesDir, "hw_${System.currentTimeMillis()}.jpg")
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    capturedImagePath = file.absolutePath
                                    launcher.launch(uri)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Anhang (Foto)")
                            }
                            
                            if (capturedImagePath != null) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (subject.isNotBlank() && title.isNotBlank() && selectedDateText != "Datum wählen") {
                                    scope.launch {
                                        dao.insertHomework(Homework(
                                            subject = subject, 
                                            title = title, 
                                            dueDate = selectedDateText, 
                                            imagePath = capturedImagePath,
                                            description = description
                                        ))
                                        
                                        // Track for procrastination if date was changed multiple times?
                                        // Here we just count insertions for the goal
                                        val dueDate = LocalDate.parse(selectedDateText)
                                        val daysBefore = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
                                        
                                        val subName = subject
                                        subject = ""
                                        title = ""
                                        selectedDateText = "Datum wählen"
                                        description = ""
                                        capturedImagePath = null
                                        statsManager.addHomeworkXp(subName, daysBefore)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Aufgabe speichern", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(homeworkList) { hw ->
                HomeworkItem(
                    homework = hw, 
                    onDone = {
                        scope.launch {
                            dao.deleteHomework(hw.id)
                            statsManager.addHomeworkXp(hw.subject)
                        }
                    }, 
                    onDelete = {
                        deleteConfirmItem = hw
                    },
                    onPostpone = {
                        scope.launch {
                            val newDate = LocalDate.parse(hw.dueDate).plusDays(1).toString()
                            val updatedHw = hw.copy(dueDate = newDate, postponeCount = hw.postponeCount + 1)
                            dao.insertHomework(updatedHw)
                            if (updatedHw.postponeCount >= 3) {
                                statsManager.addProcrastination()
                            }
                        }
                    }
                )
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            selectedDateText = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (deleteConfirmItem != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmItem = null },
                title = { Text("Hausaufgabe löschen?") },
                text = { Text("Möchtest du diese Aufgabe wirklich permanent entfernen?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                dao.deleteHomework(deleteConfirmItem!!.id)
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
}

@Composable
fun HomeworkItem(homework: Homework, onDone: () -> Unit, onDelete: () -> Unit, onPostpone: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(homework.subject, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(homework.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (homework.description.isNotBlank()) {
                        Text(homework.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("Fällig: ${homework.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (homework.postponeCount > 0) {
                            Text(" (${homework.postponeCount}x verschoben)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                IconButton(onClick = onPostpone) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = "Verschieben", tint = MaterialTheme.colorScheme.secondary)
                }

                if (homework.imagePath != null) {
                    Icon(
                        Icons.Default.Image, 
                        contentDescription = "Foto vorhanden",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                IconButton(onClick = onDone) {
                    Icon(Icons.Default.Check, contentDescription = "Erledigt", tint = Color(0xFF4CAF50))
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }
}
