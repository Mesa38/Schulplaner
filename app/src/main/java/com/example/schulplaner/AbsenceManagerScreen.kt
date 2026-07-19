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
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenceManagerScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.absenceDao() }
    val subjects by db.subjectDao().getAllSubjects().collectAsState(initial = emptyList())
    val absences by dao.getAllAbsences().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val statsManager = remember { UserStatsManager(context) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var expanded by remember { mutableStateOf(false) }
    
    var deleteConfirmItem by remember { mutableStateOf<Absence?>(null) }

    if (absences.isEmpty()) {
        LaunchedEffect(Unit) {
            val month = LocalDate.now().monthValue
            if (month == 1 || month == 2 || month == 6 || month == 7) {
                statsManager.triggerZeroAbsences()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Fehlstunden-Manager", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Fehlstunde eintragen")
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                AbsenceSummaryCard(absences)
            }

            items(absences) { absence ->
                AbsenceItem(
                    absence = absence,
                    onToggleExcused = {
                        scope.launch {
                            dao.updateAbsence(absence.copy(isExcused = !absence.isExcused))
                            if (!absence.isExcused) statsManager.addAbsenceExcuse()
                        }
                    },
                    onDelete = { deleteConfirmItem = absence }
                )
            }
            
            if (absences.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Text("Keine Fehlstunden verzeichnet. Sauber!", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Fehlstunde eintragen") },
                text = {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedSubject,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Fach") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                subjects.forEach { sub ->
                                    DropdownMenuItem(text = { Text(sub.name) }, onClick = { selectedSubject = sub.name; expanded = false })
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = hours,
                            onValueChange = { hours = it },
                            label = { Text("Anzahl Stunden") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Grund (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (selectedSubject.isNotBlank()) {
                            scope.launch {
                                dao.insertAbsence(Absence(
                                    date = date,
                                    subject = selectedSubject,
                                    hours = hours.toIntOrNull() ?: 1,
                                    reason = reason
                                ))
                                showDialog = false
                                selectedSubject = ""
                                reason = ""
                            }
                        }
                    }) { Text("Speichern") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Abbrechen") } }
            )
        }

        if (deleteConfirmItem != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmItem = null },
                title = { Text("Eintrag löschen?") },
                text = { Text("Soll diese Fehlstunde wirklich entfernt werden?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                dao.deleteAbsence(deleteConfirmItem!!)
                                deleteConfirmItem = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Löschen") }
                },
                dismissButton = { TextButton(onClick = { deleteConfirmItem = null }) { Text("Abbrechen") } }
            )
        }
    }
}

@Composable
fun AbsenceSummaryCard(absences: List<Absence>) {
    val total = absences.sumOf { it.hours }
    val excused = absences.filter { it.isExcused }.sumOf { it.hours }
    val unexcused = total - excused

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gesamtfehlstunden", style = MaterialTheme.typography.labelMedium)
                Text("$total Std", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Entschuldigt: $excused", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("Offen: $unexcused", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AbsenceItem(absence: Absence, onToggleExcused: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(absence.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${absence.date} • ${absence.hours} Std", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                if (absence.reason.isNotBlank()) {
                    Text(absence.reason, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            IconButton(onClick = onToggleExcused) {
                Icon(
                    if (absence.isExcused) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (absence.isExcused) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}
