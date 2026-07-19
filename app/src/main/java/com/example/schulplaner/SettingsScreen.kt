package com.example.schulplaner

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.subjectDao() }
    val subjects by dao.getAllSubjects().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val statsManager = remember { UserStatsManager(context) }

    var subjectName by remember { mutableStateOf("") }
    var selectedAbiType by remember { mutableStateOf<String?>(null) }
    
    val abiTypes = listOf("Kein Prüfungsfach", "P1 (eA)", "P2 (eA)", "P3 (eA)", "P4 (gA)", "P5 (gA)")

    var renameSubjectItem by remember { mutableStateOf<Subject?>(null) }
    var renameSubjectTitle by remember { mutableStateOf("") }
    var deleteConfirmItem by remember { mutableStateOf<Subject?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // NEU: Account Section
        Text("Account & Synchronisation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Eingeloggt als:", style = MaterialTheme.typography.labelSmall)
                        Text(FirebaseHelper.getAuth()?.currentUser?.email ?: "Nicht angemeldet", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (FirebaseHelper.isInitialized()) {
                        Button(
                            onClick = { FirebaseHelper.getAuth()?.signOut() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Abmelden")
                        }
                    }
                }
                
                if (FirebaseHelper.isInitialized() && FirebaseHelper.getAuth()?.currentUser != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { FirebaseSync(context).syncAll() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Sync, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jetzt synchronisieren")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { 
                        scope.launch { statsManager.addBetaClick() }
                    }
            ) {
                // Das Logo-Platzhalter für Easter Egg
                Icon(Icons.Default.Settings, null, modifier = Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Setup & Prüfungsfächer", 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Neues Fach / Kurs hinzufügen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Name (z.B. Deutsch, Mathe)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Status im Abitur (Niedersachsen):", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                abiTypes.chunked(2).forEach { rowTypes ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowTypes.forEach { type ->
                            val isSelected = (selectedAbiType == null && type == "Kein Prüfungsfach") || 
                                           (selectedAbiType != null && type.startsWith(selectedAbiType!!))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).clickable {
                                    selectedAbiType = if (type == "Kein Prüfungsfach") null else type.split(" ")[0]
                                }
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { 
                                        selectedAbiType = if (type == "Kein Prüfungsfach") null else type.split(" ")[0]
                                    }
                                )
                                Text(
                                    text = type, 
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        if (subjectName.isNotBlank()) {
                            scope.launch {
                                val typeLabel = when(selectedAbiType) {
                                    "P1", "P2", "P3" -> "eA"
                                    "P4", "P5" -> "gA"
                                    else -> "Normal"
                                }
                                try {
                                    dao.insertSubject(Subject(
                                        name = subjectName, 
                                        type = typeLabel,
                                        abiType = selectedAbiType
                                    ))
                                    subjectName = ""
                                    selectedAbiType = null
                                } catch (e: Exception) {
                                    // Handle Unique Constraint if needed
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fach speichern", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (renameSubjectItem != null) {
            AlertDialog(
                onDismissRequest = { renameSubjectItem = null },
                title = { Text("Fach umbenennen") },
                text = {
                    OutlinedTextField(
                        value = renameSubjectTitle,
                        onValueChange = { renameSubjectTitle = it },
                        label = { Text("Neuer Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (renameSubjectTitle.isNotBlank()) {
                            scope.launch {
                                dao.insertSubject(renameSubjectItem!!.copy(name = renameSubjectTitle))
                                renameSubjectItem = null
                            }
                        }
                    }) { Text("Speichern") }
                },
                dismissButton = {
                    TextButton(onClick = { renameSubjectItem = null }) { Text("Abbrechen") }
                }
            )
        }

        if (deleteConfirmItem != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmItem = null },
                title = { Text("Fach löschen?") },
                text = { Text("Möchtest du dieses Fach inklusive aller Noten wirklich entfernen?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                dao.deleteSubject(deleteConfirmItem!!)
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
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Notifications Section
        Text("Benachrichtigungen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                var remindersEnabled by remember { mutableStateOf(true) }
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { _ -> }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hausaufgaben-Erinnerungen", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = remindersEnabled, 
                        onCheckedChange = { 
                            remindersEnabled = it 
                            if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                var reminderTime by remember { mutableStateOf("16:00") }
                Text("Uhrzeit: $reminderTime Uhr", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = 16f, 
                    onValueChange = { }, 
                    valueRange = 0f..23f,
                    steps = 23
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Daten & Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val exportData = "AbiPlaner Export - Fächer: ${subjects.size}, Noten: ${db.gradeDao().getAllGrades().first().size}"
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, exportData)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Daten exportieren / teilen")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Deine Fächer & Gewichtung", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Fehlstunden-Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Trage hier deine Entschuldigungen ein.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            statsManager.addAbsenceExcuse()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Neue Entschuldigung abgeben")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        subjects.forEach { subject ->
            var weightK by remember { mutableStateOf(subject.weightKlausur.toString()) }
            var weightM by remember { mutableStateOf(subject.weightMitarbeit.toString()) }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(subject.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text("${subject.type} ${subject.abiType ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { 
                            renameSubjectItem = subject
                            renameSubjectTitle = subject.name
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Umbenennen", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { deleteConfirmItem = subject }) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedTextField(
                            value = weightK,
                            onValueChange = { weightK = it },
                            label = { Text("Klausur %") },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = weightM,
                            onValueChange = { weightM = it },
                            label = { Text("Mitarbeit %") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Farbe wählen:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colors = listOf(0xFF1A73E8, 0xFFE53935, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA, 0xFF00ACC1, 0xFFFDD835, 0xFFD81B60)
                        colors.forEach { colorVal ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(androidx.compose.ui.graphics.Color(colorVal))
                                    .clickable {
                                        scope.launch {
                                            dao.insertSubject(subject.copy(color = colorVal.toInt()))
                                        }
                                    }
                                    .border(
                                        width = if (subject.color == colorVal.toInt()) 2.dp else 0.dp,
                                        color = if (subject.color == colorVal.toInt()) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            val wK = weightK.toFloatOrNull() ?: 0.5f
                            val wM = weightM.toFloatOrNull() ?: 0.5f
                            scope.launch {
                                dao.insertSubject(subject.copy(weightKlausur = wK, weightMitarbeit = wM))
                            }
                        },
                        modifier = Modifier.align(Alignment.End).padding(top = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
