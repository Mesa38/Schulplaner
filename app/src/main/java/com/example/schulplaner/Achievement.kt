package com.example.schulplaner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val descriptionLong: String,
    val icon: ImageVector,
    val condition: (UserStats, List<Subject>, List<Grade>, List<FocusSession>) -> Boolean
)

object AchievementManager {
    val allAchievements = listOf(
        // IMAGE 1
        Achievement(
            "weekend_warrior",
            "Wochenend-Krieger",
            "Lerne an einem Samstagabend nach 20:00 Uhr.",
            "Während andere feiern, formst du deine Zukunft.",
            Icons.Default.Weekend
        ) { stats, _, _, _ -> stats.satNightStudyDone },
        
        Achievement(
            "marathon_runner",
            "Marathonläufer",
            "Sammle an einem einzigen Tag über 4 Stunden reine Lernzeit.",
            "Dein Gehirn ist jetzt offiziell ein Hochleistungssportler.",
            Icons.AutoMirrored.Filled.DirectionsRun
        ) { stats, _, _, _ -> stats.maxDailyFocusMins >= 240 },

        Achievement(
            "multitasking_master",
            "Multitasking-Meister",
            "Erfasse Lernzeiten für vier verschiedene Fächer an einem Tag.",
            "Vom Gedicht zur Kurvendiskussion in Rekordzeit.",
            Icons.Default.Layers
        ) { _, _, _, sessions -> 
            val today = java.time.LocalDate.now().toString()
            sessions.filter { it.date == today }.map { it.subject }.distinct().size >= 4
        },

        Achievement(
            "procrastination_pro",
            "Prokrastinations-Profi",
            "Verschiebe das Fälligkeitsdatum einer Aufgabe dreimal nach hinten.",
            "Morgen ist ja auch noch ein Tag. Oder übermorgen.",
            Icons.Default.Schedule
        ) { stats, _, _, _ -> stats.procrastinateCount >= 3 },

        Achievement(
            "early_bird_legacy",
            "Der frühe Vogel",
            "Trage deinen ersten Stundenplan am Morgen des ersten Schultags ein.",
            "Maximale Motivation am Tag 1.",
            Icons.Default.WbTwilight
        ) { stats, _, _, _ -> stats.earlyStudyCount >= 1 },

        Achievement(
            "sisyphus",
            "Sisyphus",
            "Setze ein Thema, das auf 'Erledigt' stand, zurück auf 'Offen'.",
            "Du dachtest, du hättest es verstanden. Du lagst falsch.",
            Icons.Default.History
        ) { stats, _, _, _ -> stats.achievementsJson.contains("sisyphus_trigger") },

        Achievement(
            "amnesia",
            "Amnesie",
            "Klicke bei derselben Karteikarte fünfmal hintereinander auf 'Schwer'.",
            "Dieses eine verdammte Wort will einfach nicht in den Kopf.",
            Icons.Default.QuestionMark
        ) { stats, _, _, _ -> stats.achievementsJson.contains("amnesia_trigger") },

        // IMAGE 2
        Achievement(
            "convinced_nerd",
            "Streber aus Überzeugung",
            "Trage drei Noten mit 13 Punkten oder mehr in Folge ein.",
            "Ist die Luft da oben auf dem Olymp eigentlich dünn?",
            Icons.Default.AutoGraph
        ) { stats, _, _, _ -> stats.consecutiveTopGrades >= 3 },

        Achievement(
            "lifebuoy",
            "Rettungsring",
            "Erzile in einer mündlichen Note mind. 5 Pkt mehr als in der Klausur.",
            "Reden konntest du eben schon immer besser als Schreiben.",
            Icons.Default.Support
        ) { _, _, grades, _ -> 
            val subjects = grades.map { it.subject }.distinct()
            subjects.any { sub ->
                val subGrades = grades.filter { it.subject == sub }
                val klausur = subGrades.find { it.type == "Klausur" }?.points
                val mitarbeit = subGrades.find { it.type == "Mitarbeit" }?.points
                if (klausur != null && mitarbeit != null) mitarbeit >= (klausur + 5) else false
            }
        },

        Achievement(
            "understatement",
            "Tiefstapler",
            "Setze dein Wunsch-Abitur im Notenrechner auf 4,0.",
            "Hauptsache durch, der Rest ist Kosmetik.",
            Icons.Default.VerticalAlignBottom
        ) { stats, _, _, _ -> stats.achievementsJson.contains("understatement_trigger") },

        Achievement(
            "einstein_complex",
            "Einstein-Komplex",
            "Setze dein Wunsch-Abitur im Notenrechner auf exakt 1,0.",
            "Die Elite-Unis rollen schon den roten Teppich aus.",
            Icons.Default.Lightbulb
        ) { stats, _, _, _ -> stats.achievementsJson.contains("einstein_trigger") },

        Achievement(
            "four_wins",
            "Vier gewinnt",
            "Trage in einer Klausur exakt 5 Punkte ein.",
            "Maximaler Ertrag bei minimalem Aufwand.",
            Icons.Default.DoneAll
        ) { _, _, grades, _ -> grades.any { it.type == "Klausur" && it.points == 5 } },

        Achievement(
            "schroedingers_exam",
            "Schrödingers Klausur",
            "Ein Klausurtermin ist vergangen, aber 3 Wochen später fehlt die Note.",
            "Solange die Note nicht da ist, ist sie gleichzeitig gut und schlecht.",
            Icons.Default.SdCardAlert
        ) { stats, _, _, _ -> stats.achievementsJson.contains("schroedinger_trigger") },

        // IMAGE 3
        Achievement(
            "sunday_depression",
            "Sonntags-Depression",
            "Öffne die App an einem Sonntagabend zwischen 18:00 und 20:00 Uhr.",
            "Der Moment, in dem dir einfällt, was du alles machen wolltest.",
            Icons.Default.Cloud
        ) { stats, _, _, _ -> stats.achievementsJson.contains("sunday_depression_trigger") },

        Achievement(
            "printer_insolvency",
            "Druckerpatronen-Insolvenz",
            "Lade dein 50. Dokument oder Foto in den Media-Hub hoch.",
            "Der Kopierer im Lehrerzimmer raucht wegen dir.",
            Icons.Default.Print
        ) { stats, _, _, _ -> stats.totalMediaCount >= 50 },

        Achievement(
            "alchemist",
            "Der Alchemist",
            "Lerne Biologie, Chemie und Physik an einem einzigen Tag.",
            "Du bist kurz davor, den Stein der Weisen zu erschaffen.",
            Icons.Default.Science
        ) { _, _, _, sessions -> 
            val today = java.time.LocalDate.now().toString()
            val todaySubs = sessions.filter { it.date == today }.map { it.subject.lowercase() }
            todaySubs.contains("biologie") && todaySubs.contains("chemie") && todaySubs.contains("physik")
        },

        Achievement(
            "philosopher",
            "Philosoph",
            "Verbringe insgesamt über 15 Stunden Lernzeit im Fach Philosophie.",
            "Ich denke, also bin ich... gestresst.",
            Icons.Default.SelfImprovement
        ) { _, _, _, sessions -> 
            sessions.filter { it.subject.contains("Philo", true) || it.subject.contains("Ethik", true) || it.subject.contains("Reli", true) }
                .sumOf { it.durationMinutes } >= 900
        },

        Achievement(
            "lost_in_translation",
            "Lost in Translation",
            "Erstelle über 100 Karteikarten in einer einzigen Fremdsprache.",
            "Du träumst mittlerweile vermutlich auf Spanisch.",
            Icons.Default.Translate
        ) { stats, _, _, _ -> stats.achievementsJson.contains("lost_translation_trigger") },

        Achievement(
            "exam_ghosting",
            "Klausuren-Ghosting",
            "Lösche eine Klausur aus dem Kalender, weil du am Tag davor das Datum entfernst.",
            "Was nicht im Kalender steht, existiert auch nicht. (Gute Besserung!)",
            Icons.Default.VisibilityOff
        ) { stats, _, _, _ -> stats.achievementsJson.contains("exam_ghosting_trigger") },

        Achievement(
            "ehrenrunde_safe",
            "Ehrenrunde abgewendet",
            "Das System berechnet, dass du trotz eines Unterkurses dein Abi noch sicher bestehst.",
            "Der Puls fährt langsam wieder auf Normalniveau herunter.",
            Icons.Default.Shield
        ) { stats, _, _, _ -> stats.achievementsJson.contains("ehrenrunde_safe_trigger") },

        Achievement(
            "zero_absences",
            "Der Unsichtbare",
            "Erreiche am Ende eines Halbjahres exakt 0 Fehlstunden.",
            "Du warst selbst dann da, wenn die Lehrkraft krank war.",
            Icons.Default.RecordVoiceOver
        ) { stats, _, _, _ -> stats.achievementsJson.contains("zero_absences_trigger") },

        // IMAGE 4
        Achievement(
            "rainbow_timetable",
            "Regenbogen-Stundenplan",
            "Weise in den Einstellungen jedem Fach eine andere Farbe zu.",
            "Alles muss seine Ordnung haben. Ästhetik ist wichtig.",
            Icons.Default.Palette
        ) { _, subjects, _, _ -> 
            val colors = subjects.map { it.color }.distinct()
            subjects.size >= 5 && colors.size == subjects.size
        },

        Achievement(
            "archivist",
            "Der Archivar",
            "Speichere insgesamt über 100 Einträge in der App ab.",
            "Du bist quasi das wandelnde Staatsarchiv deiner Schule.",
            Icons.Default.Inventory
        ) { stats, _, _, _ -> stats.totalMediaCount + stats.homeworkDoneCount >= 100 },

        Achievement(
            "tabula_rasa",
            "Tabula Rasa",
            "Markiere den gesamten Abi-Stoff deiner P-Fächer als 'Erledigt'.",
            "Das Werk ist vollbracht. Du bist bereit für die Arena.",
            Icons.Default.CleaningServices
        ) { stats, _, _, _ -> stats.achievementsJson.contains("tabula_rasa_trigger") },

        Achievement(
            "beta_tester",
            "Beta-Tester",
            "Tippe 10-mal schnell auf das App-Logo (Easter Egg).",
            "Du hast das geheime Entwickler-Menü gefunden. Respekt!",
            Icons.Default.BugReport
        ) { stats, _, _, _ -> stats.betaClicks >= 10 }
    )
}
