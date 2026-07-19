package com.example.schulplaner

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class AbiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.abi_widget)
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val stats = db.userStatsDao().getUserStats().firstOrNull() ?: UserStats()
            val exams = db.examDao().getAllExams().firstOrNull() ?: emptyList()
            
            val nextExam = exams.filter { exam ->
                try { LocalDate.parse(exam.date) >= LocalDate.now() } catch (_: Exception) { false }
            }.sortedBy { it.date }.firstOrNull()

            views.setTextViewText(R.id.widget_level, "Level ${stats.level}: ${stats.levelTitle}")
            
            val progress = (stats.xp.toFloat() / stats.xpToNextLevel.toFloat() * 100).toInt()
            views.setProgressBar(R.id.widget_progress, 100, progress, false)
            
            val examText = nextExam?.let { exam -> "Nächste: ${exam.subject} am ${exam.date}" } ?: "Keine Klausuren geplant"
            views.setTextViewText(R.id.widget_next_exam, examText)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
