package com.example.schulplaner

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StudyTimerScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val statsManager = remember { UserStatsManager(context) }
    val subjects by db.subjectDao().getAllSubjects().collectAsState(initial = emptyList())
    val sessionDao = remember { db.focusSessionDao() }

    var timeLeft by remember { mutableLongStateOf(25 * 60 * 1000L) } // 25 Min default
    var isRunning by remember { mutableStateOf(false) }
    var initialTime by remember { mutableLongStateOf(25 * 60 * 1000L) }
    
    var selectedSubject by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val minutes = (timeLeft / 1000) / 60
    val seconds = (timeLeft / 1000) % 60

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1000L
            }
            if (timeLeft <= 0) {
                val focusMinutes = (initialTime / 1000 / 60).toInt()
                val subjectName = if (selectedSubject.isBlank()) "Allgemein" else selectedSubject
                statsManager.addFocusXp(focusMinutes, subjectName)
                
                // Save Session
                scope.launch {
                    sessionDao.insertSession(
                        FocusSession(
                            subject = subjectName,
                            durationMinutes = focusMinutes,
                            date = java.time.LocalDate.now().toString()
                        )
                    )
                }
            }
            isRunning = false
        }
    }

    LaunchedEffect(isRunning) {
        if (!isRunning && timeLeft > 0 && timeLeft < initialTime) {
             // Pause-Logik für "Nicht heute, Netflix!"
             scope.launch { statsManager.skipBreak() }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Fokus-Timer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("Konzentriere dich auf dein Abi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        
        Spacer(modifier = Modifier.height(48.dp))

        // Subject Selection
        Box(modifier = Modifier.fillMaxWidth(0.8f)) {
            OutlinedTextField(
                value = selectedSubject,
                onValueChange = { },
                readOnly = true,
                label = { Text("Lernfach wählen") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(onClick = { if (!isRunning) expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                },
                enabled = !isRunning
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = !isRunning) { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                DropdownMenuItem(
                    text = { Text("Allgemein") },
                    onClick = {
                        selectedSubject = "Allgemein"
                        expanded = false
                    }
                )
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
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier.height(56.dp).weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "Pause" else "Start", fontWeight = FontWeight.Bold)
            }
            
            OutlinedButton(
                onClick = { 
                    isRunning = false
                    timeLeft = initialTime
                },
                modifier = Modifier.height(56.dp).weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        if (!isRunning) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimerChip(label = "25m", duration = 25) { timeLeft = it; initialTime = it }
                TimerChip(label = "45m", duration = 45) { timeLeft = it; initialTime = it }
                TimerChip(label = "60m", duration = 60) { timeLeft = it; initialTime = it }
            }
        }
    }
}

@Composable
fun TimerChip(label: String, duration: Int, onClick: (Long) -> Unit) {
    SuggestionChip(
        onClick = { onClick(duration * 60 * 1000L) },
        label = { Text(label) }
    )
}
