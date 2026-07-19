package com.example.schulplaner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val homeworkList = db.homeworkDao().getAllHomework().first()
        val exams = db.examDao().getAllExams().first()
        val today = java.time.LocalDate.now()
        val tomorrow = today.plusDays(1).toString()

        if (homeworkList.isNotEmpty()) {
            showNotification("Hausaufgaben fällig!", "Du hast noch ${homeworkList.size} offene Aufgaben.", 1)
        }

        val examTomorrow = exams.find { it.date == tomorrow }
        if (examTomorrow != null) {
            showNotification("🔥 Klausur morgen!", "Morgen schreibst du ${examTomorrow.subject}. Viel Erfolg!", 2)
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String, id: Int) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Schulplaner-Erinnerungen", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }
}
