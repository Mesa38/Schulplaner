package com.example.schulplaner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class TimeSlot(val hour: Int, val start: String, val end: String)

val timeSlots = listOf(
    TimeSlot(1, "08:00", "08:45"),
    TimeSlot(2, "08:50", "09:35"),
    TimeSlot(3, "09:55", "10:40"),
    TimeSlot(4, "10:45", "11:30"),
    TimeSlot(5, "11:45", "12:30"),
    TimeSlot(6, "12:35", "13:20"),
    TimeSlot(7, "13:25", "14:10"),
    TimeSlot(8, "14:15", "15:00"),
    TimeSlot(9, "15:05", "15:50"),
    TimeSlot(10, "15:55", "16:40"),
)

val days = listOf("Mo", "Di", "Mi", "Do", "Fr")

@Composable
fun TimetableScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.timetableDao() }
    val subjectDao = remember { db.subjectDao() }
    val entries by dao.getAllEntries().collectAsState(initial = emptyList())
    val subjects by subjectDao.getAllSubjects().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableIntStateOf(1) }
    var selectedHour by remember { mutableIntStateOf(1) }
    var subjectInput by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    var deleteConfirmItem by remember { mutableStateOf<TimetableEntry?>(null) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Fach für ${days[selectedDay - 1]}, $selectedHour. Std eintragen") },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = subjectInput,
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
                        DropdownMenuItem(
                            text = { Text("--- LEEREN ---") },
                            onClick = {
                                subjectInput = ""
                                expanded = false
                            }
                        )
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text("${sub.name} (${sub.type.split(" ")[0]})") },
                                onClick = {
                                    subjectInput = sub.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (subjectInput.isBlank()) {
                        val entry = entries.find { it.dayOfWeek == selectedDay && it.hour == selectedHour }
                        if (entry != null) {
                            deleteConfirmItem = entry
                        }
                        showDialog = false
                    } else {
                        scope.launch {
                            val now = java.time.LocalTime.now()
                            if (entries.isEmpty() && now.isBefore(java.time.LocalTime.of(7, 30))) {
                                UserStatsManager(context).triggerEarlyBird()
                            }
                            dao.insertEntry(
                                TimetableEntry(
                                    dayOfWeek = selectedDay,
                                    hour = selectedHour,
                                    subject = subjectInput
                                )
                            )
                            showDialog = false
                            subjectInput = ""
                        }
                    }
                }) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    if (deleteConfirmItem != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmItem = null },
            title = { Text("Eintrag löschen?") },
            text = { Text("Möchtest du dieses Fach wirklich aus dem Plan entfernen?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            dao.deleteEntry(deleteConfirmItem!!.dayOfWeek, deleteConfirmItem!!.hour)
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

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Stundenplan", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Grid-like structure using LazyRow and LazyColumn
        Row(modifier = Modifier.fillMaxWidth()) {
            // Empty space for the time column header
            Box(modifier = Modifier.width(60.dp))
            
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(timeSlots) { slot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .border(0.5.dp, Color.LightGray),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time column
                    Column(
                        modifier = Modifier.width(60.dp).padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = slot.start, fontSize = 10.sp)
                        Text(text = "${slot.hour}.", fontWeight = FontWeight.Bold)
                        Text(text = slot.end, fontSize = 10.sp)
                    }

                    // Days columns
                    for (dayIndex in 1..5) {
                        val entry = entries.find { it.dayOfWeek == dayIndex && it.hour == slot.hour }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(0.5.dp, Color.LightGray)
                                .background(if (entry != null) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    selectedDay = dayIndex
                                    selectedHour = slot.hour
                                    subjectInput = entry?.subject ?: ""
                                    showDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (entry != null) {
                                Text(
                                    text = entry.subject,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
