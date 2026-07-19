package com.example.schulplaner

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaHubScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val subjectDao = remember { db.subjectDao() }
    val topicDao = remember { db.topicDao() }
    val subjects by subjectDao.getAllSubjects().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedSubject by remember { mutableStateOf<String?>(null) }
    val topics by (selectedSubject?.let { topicDao.getTopicsForSubject(it) } ?: topicDao.getAllTopics()).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    val filteredTopics = topics.filter { it.title.contains(searchQuery, true) }

    var showAddTopicDialog by remember { mutableStateOf(false) }
    var newTopicTitle by remember { mutableStateOf("") }
    
    var deleteConfirmItem by remember { mutableStateOf<Any?>(null) }
    var renameTopicItem by remember { mutableStateOf<Topic?>(null) }
    var renameTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Media Hub", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedSubject != null) {
                FloatingActionButton(onClick = { showAddTopicDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Thema hinzufügen")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Subject Selector
            if (subjects.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = subjects.indexOfFirst { it.name == selectedSubject }.let { if (it == -1) 0 else it },
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    subjects.forEach { sub ->
                        FilterChip(
                            selected = selectedSubject == sub.name,
                            onClick = { selectedSubject = sub.name },
                            label = { Text(sub.name) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // NEU: Suchleiste für Cyber-Spion
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it 
                    if (it.length > 3) {
                         scope.launch { UserStatsManager(context).triggerCyberSpy() }
                    }
                },
                placeholder = { Text("Themen durchsuchen (z.B. Chemie)...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedSubject == null && searchQuery.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Wähle ein Fach, um Zusammenfassungen zu verwalten.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(filteredTopics, key = { _, t -> t.id }) { index, topic ->
                        MediaTopicItem(
                            topic = topic, 
                            onUpdate = { updated -> scope.launch { topicDao.updateTopic(updated) } },
                            onDelete = { deleteConfirmItem = topic },
                            onRename = { 
                                renameTopicItem = topic
                                renameTitle = topic.title
                            },
                            onMoveUp = if (index > 0) { {
                                scope.launch {
                                    topicDao.swapOrder(topic, topics[index - 1])
                                }
                            } } else null,
                            onMoveDown = if (index < topics.size - 1) { {
                                scope.launch {
                                    topicDao.swapOrder(topic, topics[index + 1])
                                }
                            } } else null,
                            onDeletePdf = { pdfIdx ->
                                deleteConfirmItem = topic to pdfIdx
                            }
                        )
                    }
                }
            }
        }

        if (showAddTopicDialog) {
            AlertDialog(
                onDismissRequest = { showAddTopicDialog = false },
                title = { Text("Neues Thema") },
                text = {
                    OutlinedTextField(
                        value = newTopicTitle,
                        onValueChange = { newTopicTitle = it },
                        label = { Text("Titel des Themas") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newTopicTitle.isNotBlank() && selectedSubject != null) {
                            scope.launch {
                                val nextIdx = (topicDao.getMaxOrderIndex(selectedSubject!!) ?: -1) + 1
                                topicDao.insertTopic(Topic(subject = selectedSubject!!, title = newTopicTitle, orderIndex = nextIdx))
                                newTopicTitle = ""
                                showAddTopicDialog = false
                            }
                        }
                    }) { Text("Hinzufügen") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTopicDialog = false }) { Text("Abbrechen") }
                }
            )
        }
        
        if (renameTopicItem != null) {
            AlertDialog(
                onDismissRequest = { renameTopicItem = null },
                title = { Text("Thema umbenennen") },
                text = {
                    OutlinedTextField(
                        value = renameTitle,
                        onValueChange = { renameTitle = it },
                        label = { Text("Neuer Titel") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (renameTitle.isNotBlank()) {
                            scope.launch {
                                topicDao.updateTopic(renameTopicItem!!.copy(title = renameTitle))
                                renameTopicItem = null
                            }
                        }
                    }) { Text("Speichern") }
                },
                dismissButton = {
                    TextButton(onClick = { renameTopicItem = null }) { Text("Abbrechen") }
                }
            )
        }

        if (deleteConfirmItem != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmItem = null },
                title = { Text("Wirklich löschen?") },
                text = { Text("Möchtest du diesen Eintrag permanent entfernen?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val item = deleteConfirmItem
                                if (item is Topic) {
                                    topicDao.deleteTopic(item)
                                } else if (item is Pair<*, *>) {
                                    val targetTopic = item.first as Topic
                                    val pdfIdx = item.second as Int
                                    val currentPdfList = targetTopic.pdfPaths.split(",").filter { it.isNotBlank() }.toMutableList()
                                    if (pdfIdx in currentPdfList.indices) {
                                        currentPdfList.removeAt(pdfIdx)
                                        topicDao.updateTopic(targetTopic.copy(pdfPaths = currentPdfList.joinToString(",")))
                                    }
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
}

@Composable
fun MediaTopicItem(
    topic: Topic, 
    onUpdate: (Topic) -> Unit, 
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDeletePdf: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPdfList by remember { mutableStateOf(false) }
    
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "summary_${topic.id}_${System.currentTimeMillis()}.pdf"
            val file = File(context.filesDir, fileName)
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                val currentPaths = if (topic.pdfPaths.isEmpty()) emptyList() else topic.pdfPaths.split(",").filter { it.isNotBlank() }
                val newPaths = (currentPaths + file.absolutePath).joinToString(",")
                onUpdate(topic.copy(pdfPaths = newPaths))
                
                // Achievement Bücherwurm & Messie
                val statsManager = UserStatsManager(context)
                scope.launch { 
                    statsManager.updateMediaCount(1, topic.subject)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Reihenfolge: ${topic.orderIndex}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = "Umbenennen", modifier = Modifier.size(18.dp))
                }
                
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Hoch", modifier = Modifier.size(18.dp))
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Runter", modifier = Modifier.size(18.dp))
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
            
            val pdfList = if (topic.pdfPaths.isEmpty()) emptyList() else topic.pdfPaths.split(",").filter { it.isNotBlank() }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("${pdfList.size} PDFs", style = MaterialTheme.typography.bodySmall)
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(onClick = { pdfLauncher.launch("application/pdf") }) {
                    Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hinzufügen", fontSize = 12.sp)
                }
                
                if (pdfList.isNotEmpty()) {
                    TextButton(onClick = { showPdfList = !showPdfList }) {
                        Text(if (showPdfList) "Verbergen" else "Anzeigen")
                    }
                }
            }
            
            if (showPdfList && pdfList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                pdfList.forEachIndexed { idx, path ->
                    val file = File(path)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Zusammenfassung ${idx + 1}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        
                        IconButton(onClick = {
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
                        }) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        IconButton(onClick = {
                            onDeletePdf(idx)
                        }) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
