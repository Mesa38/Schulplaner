package com.example.schulplaner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RocketLaunch
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
import kotlinx.coroutines.launch

@Composable
fun AbiCalcScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val subjects by db.subjectDao().getAllSubjects().collectAsState(initial = emptyList())
    val grades by db.gradeDao().getAllGrades().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val statsManager = remember { UserStatsManager(context) }

    var abiPointsP1 by remember { mutableIntStateOf(0) }
    var abiPointsP2 by remember { mutableIntStateOf(0) }
    var abiPointsP3 by remember { mutableIntStateOf(0) }
    var abiPointsP4 by remember { mutableIntStateOf(0) }
    var abiPointsP5 by remember { mutableIntStateOf(0) }

    var targetGrade by remember { mutableStateOf("1,9") }
    var showWhatIf by remember { mutableStateOf(false) }

    // Block 1 nach niedersächsischen Regeln (mind. 32, max. 36 Schulhalbjahresergebnisse)
    // Bei G9 werden üblicherweise genau 32 oder bis zu 36 Ergebnisse eingebracht und auf 40 normiert.
    val block1Details = remember(subjects, grades) {
        var totalPoints = 0.0
        var totalCoursesCount = 0
        val warnings = mutableListOf<String>()
        
        // Prüfe Kern- und Pflichtfächer (Deutsch, Mathe, Fremdsprachen)
        val hasDeutsch = subjects.any { it.name.contains("Deutsch", ignoreCase = true) }
        val hasMathe = subjects.any { it.name.contains("Mathe", ignoreCase = true) }
        
        if (!hasDeutsch) warnings.add("⚠️ Deutsch fehlt in deiner Fächerliste (Pflicht: 4 Halbjahre).")
        if (!hasMathe) warnings.add("⚠️ Mathematik fehlt in deiner Fächerliste (Pflicht: 4 Halbjahre).")

        subjects.forEach { subject ->
            val subjectGrades = grades.filter { it.subject == subject.name }
            (1..4).forEach { sem ->
                val semGrades = subjectGrades.filter { it.semester == sem }
                if (semGrades.isNotEmpty()) {
                    val klausuren = semGrades.filter { it.type == "Klausur" }
                    val mitarbeit = semGrades.filter { it.type == "Mitarbeit" }
                    val avgK = if (klausuren.isNotEmpty()) klausuren.map { it.points }.average() else 0.0
                    val avgM = if (mitarbeit.isNotEmpty()) mitarbeit.map { it.points }.average() else 0.0
                    val semAvg = (avgK * subject.weightKlausur) + (avgM * subject.weightMitarbeit)
                    
                    totalPoints += semAvg
                    totalCoursesCount++
                }
            }
        }
        
        val pointsNormalized = if (totalCoursesCount > 0) (totalPoints / totalCoursesCount) * 40 else 0.0
        Triple(pointsNormalized.toInt(), totalCoursesCount, warnings)
    }

    val block1PointsExtrapolated = block1Details.first
    val currentCoursesCount = block1Details.second
    val systemWarnings = block1Details.third

    // Block 2 (Prüfungen, 4-fache Wertung in Niedersachsen für 5 Fächer = 20-fache Einzelwertung)
    val block2Points = (abiPointsP1 + abiPointsP2 + abiPointsP3 + abiPointsP4 + abiPointsP5) * 4

    val totalPoints = block1PointsExtrapolated + block2Points
    val finalGrade = pointsToGrade(totalPoints)

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    scope.launch { statsManager.triggerPanikModus() }
                },
                icon = { Icon(Icons.Default.PriorityHigh, null) },
                text = { Text("Panik-Modus") },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)
        ) {
            item {
                Text("Niedersachsen Abi-Rechner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Exakt angepasst an das G9-System", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Voraussichtliche Abiturnote", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(finalGrade, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text("Gesamtpunkte: $totalPoints / 900", style = MaterialTheme.typography.bodyMedium)
                        
                        LinearProgressIndicator(
                            progress = { totalPoints.toFloat() / 900f },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                }
                
                if (systemWarnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Einbringungs-Check Niedersachsen:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            systemWarnings.forEach { warning ->
                                Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Was-wäre-wenn Sektion
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Was-wäre-wenn? (Ziel-Modus)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Wunsch-NC:", modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = targetGrade,
                                onValueChange = { targetGrade = it },
                                modifier = Modifier.width(100.dp),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                placeholder = { Text("1,9") }
                            )
                        }
                        
                        Button(
                            onClick = { 
                                showWhatIf = true 
                                val targetPoints = gradeToMinPoints(targetGrade)
                                val cleanedTarget = targetGrade.replace(".", ",").trim()
                                
                                if (cleanedTarget == "4,0") scope.launch { statsManager.triggerTiefstapler() }
                                if (cleanedTarget == "1,0") scope.launch { statsManager.triggerEinsteinKomplex() }

                                if (totalPoints == targetPoints && targetPoints == 300) { 
                                     scope.launch { statsManager.triggerPunktlandung() }
                                }
                                if (pointsToGrade(totalPoints) == "1,5" || pointsToGrade(totalPoints) == "1,4") {
                                     scope.launch { statsManager.triggerUeberflieger() }
                                }

                                // Check Ehrenrunde
                                val allPoints = mutableListOf<Double>()
                                subjects.forEach { sub ->
                                    (1..4).forEach { sem ->
                                        val sg = grades.filter { it.subject == sub.name && it.semester == sem }
                                        if (sg.isNotEmpty()) {
                                            val avgK = sg.filter { it.type == "Klausur" }.map { it.points }.average().let { if (it.isNaN()) 0.0 else it }
                                            val avgM = sg.filter { it.type == "Mitarbeit" }.map { it.points }.average().let { if (it.isNaN()) 0.0 else it }
                                            allPoints.add((avgK * sub.weightKlausur) + (avgM * sub.weightMitarbeit))
                                        }
                                    }
                                }
                                if (allPoints.any { it < 5.0 } && totalPoints >= 300) {
                                    scope.launch { statsManager.triggerEhrenrundeSafe() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Strategie berechnen")
                        }
                        
                        if (showWhatIf) {
                            val targetPoints = gradeToMinPoints(targetGrade)
                            if (targetPoints > 0) {
                                val neededPoints = targetPoints - totalPoints
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (neededPoints <= 0) {
                                    Text("🎉 Du hast dein Ziel bereits erreicht!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                } else {
                                    val remainingCourses = 36 - currentCoursesCount
                                    val remainingParts = if (remainingCourses > 0) remainingCourses + 20 else 20
                                    val avgNeeded = (neededPoints.toDouble() / remainingParts) + (totalPoints.toDouble() / 60)
                                    
                                    val resultPoints = Math.ceil(avgNeeded).toInt().coerceIn(0, 15)
                                    
                                    Text("Um den Schnitt von $targetGrade zu erreichen, brauchst du in allen restlichen Leistungen im Schnitt mindestens:", style = MaterialTheme.typography.bodySmall)
                                    Text("$resultPoints Punkte", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    
                                    if (resultPoints > 15) {
                                        Text("⚠️ Hinweis: Dieses Ziel ist mathematisch aktuell nicht mehr erreichbar.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // NC-Optimierer (Klammern)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("NC-Optimierer (Klammern)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Die App hat automatisch deine besten 36 Schulhalbjahresergebnisse ausgewählt, um deinen NC zu maximieren.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        val allSemesterGrades = mutableListOf<Double>()
                        subjects.forEach { sub ->
                            (1..4).forEach { sem ->
                                val sg = grades.filter { it.subject == sub.name && it.semester == sem }
                                if (sg.isNotEmpty()) {
                                    val avgK = sg.filter { it.type == "Klausur" }.map { it.points }.average().let { if (it.isNaN()) 0.0 else it }
                                    val avgM = sg.filter { it.type == "Mitarbeit" }.map { it.points }.average().let { if (it.isNaN()) 0.0 else it }
                                    allSemesterGrades.add((avgK * sub.weightKlausur) + (avgM * sub.weightMitarbeit))
                                }
                            }
                        }
                        
                        if (allSemesterGrades.size > 36) {
                            val sorted = allSemesterGrades.sortedDescending()
                            val optimizedAvg = sorted.take(36).average()
                            val standardAvg = allSemesterGrades.average()
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Vorteil durch Optimierung: +${"%.2f".format((optimizedAvg - standardAvg) * 40)} Punkte",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Block 2: Abiturprüfungen (P1 - P5)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Erreichte oder erwartete Punkte (4-fache Wertung)", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item { AbiPointInput("P1 (LK)", abiPointsP1) { abiPointsP1 = it } }
            item { AbiPointInput("P2 (LK)", abiPointsP2) { abiPointsP2 = it } }
            item { AbiPointInput("P3 (LK/GK)", abiPointsP3) { abiPointsP3 = it } }
            item { AbiPointInput("P4 (GK)", abiPointsP4) { abiPointsP4 = it } }
            item { AbiPointInput("P5 (GK/Präsentation)", abiPointsP5) { abiPointsP5 = it } }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Block 1: Qualifikationsphase", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Eingetragene Kurse: $currentCoursesCount / 36\n" +
                            "Aktueller Stand in Block 1: $block1PointsExtrapolated Punkte (hochgerechnet).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

fun gradeToMinPoints(grade: String): Int {
    return when (grade.replace(".", ",")) {
        "1,0" -> 823
        "1,1" -> 805
        "1,2" -> 787
        "1,3" -> 769
        "1,4" -> 751
        "1,5" -> 733
        "1,6" -> 715
        "1,7" -> 697
        "1,8" -> 679
        "1,9" -> 661
        "2,0" -> 643
        "2,1" -> 625
        "2,2" -> 607
        "2,3" -> 589
        "2,4" -> 571
        "2,5" -> 553
        "2,6" -> 535
        "2,7" -> 517
        "2,8" -> 499
        "2,9" -> 481
        "3,0" -> 463
        "3,1" -> 445
        "3,2" -> 427
        "3,3" -> 409
        "3,4" -> 391
        "3,5" -> 373
        "3,6" -> 355
        "3,7" -> 337
        "3,8" -> 319
        "3,9" -> 301
        "4,0" -> 300
        else -> 0
    }
}

@Composable
fun AbiPointInput(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = if (value == 0) "" else value.toString(),
            onValueChange = { 
                val points = it.toIntOrNull() ?: 0
                if (points in 0..15) onValueChange(points)
            },
            modifier = Modifier.width(80.dp),
            singleLine = true
        )
    }
}

fun pointsToGrade(points: Int): String {
    return when {
        points >= 823 -> "1,0"
        points >= 805 -> "1,1"
        points >= 787 -> "1,2"
        points >= 769 -> "1,3"
        points >= 751 -> "1,4"
        points >= 733 -> "1,5"
        points >= 715 -> "1,6"
        points >= 697 -> "1,7"
        points >= 679 -> "1,8"
        points >= 661 -> "1,9"
        points >= 643 -> "2,0"
        points >= 625 -> "2,1"
        points >= 607 -> "2,2"
        points >= 589 -> "2,3"
        points >= 571 -> "2,4"
        points >= 553 -> "2,5"
        points >= 535 -> "2,6"
        points >= 517 -> "2,7"
        points >= 499 -> "2,8"
        points >= 481 -> "2,9"
        points >= 463 -> "3,0"
        points >= 445 -> "3,1"
        points >= 427 -> "3,2"
        points >= 409 -> "3,3"
        points >= 391 -> "3,4"
        points >= 373 -> "3,5"
        points >= 355 -> "3,6"
        points >= 337 -> "3,7"
        points >= 319 -> "3,8"
        points >= 301 -> "3,9"
        points >= 300 -> "4,0"
        else -> "n.b."
    }
}
