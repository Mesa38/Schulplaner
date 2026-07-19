package com.example.schulplaner

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseSync(private val context: Context) {
    
    fun syncAll() {
        if (!FirebaseHelper.isInitialized()) return
        
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val roomDb = AppDatabase.getDatabase(context)
        val scope = CoroutineScope(Dispatchers.IO)
        val userId = auth.currentUser?.uid ?: return
        
        scope.launch {
            try {
                syncCollection(db, userId, "homework", roomDb.homeworkDao().getAllHomework().first()) { item -> 
                    roomDb.homeworkDao().insertHomework(item as Homework) 
                }
                syncCollection(db, userId, "grades", roomDb.gradeDao().getAllGrades().first()) { item -> 
                    roomDb.gradeDao().insertGrade(item as Grade) 
                }
                syncCollection(db, userId, "subjects", roomDb.subjectDao().getAllSubjects().first()) { item -> 
                    roomDb.subjectDao().insertSubject(item as Subject) 
                }
                syncCollection(db, userId, "exams", roomDb.examDao().getAllExams().first()) { item -> 
                    roomDb.examDao().insertExam(item as Exam) 
                }
                syncCollection(db, userId, "absences", roomDb.absenceDao().getAllAbsences().first()) { item -> 
                    roomDb.absenceDao().insertAbsence(item as Absence) 
                }
                syncCollection(db, userId, "timetable", roomDb.timetableDao().getAllEntries().first()) { item -> 
                    roomDb.timetableDao().insertEntry(item as TimetableEntry) 
                }
                
                // User Stats Sync
                val stats = roomDb.userStatsDao().getUserStats().first() ?: UserStats()
                val userRef = db.collection("users").document(userId).collection("stats").document("main")
                userRef.set(stats, SetOptions.merge()).await()
                
                val remoteStats = userRef.get().await().toObject(UserStats::class.java)
                if (remoteStats != null) {
                    roomDb.userStatsDao().updateStats(remoteStats)
                }

                Log.d("FirebaseSync", "Sync completed successfully")
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Sync failed", e)
            }
        }
    }

    private suspend fun <T : Any> syncCollection(
        db: FirebaseFirestore,
        userId: String,
        collectionName: String,
        localItems: List<T>,
        insertLocal: suspend (Any) -> Unit
    ) {
        val userRef = db.collection("users").document(userId).collection(collectionName)

        localItems.forEach { item ->
            try {
                val idField = item.javaClass.getDeclaredField("id")
                idField.isAccessible = true
                val idValue = idField.get(item)
                val id = idValue?.toString() ?: "unknown_${System.currentTimeMillis()}"
                userRef.document(id).set(item, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed to upload item from $collectionName", e)
            }
        }

        try {
            val snapshot = userRef.get().await()
            snapshot.documents.forEach { doc ->
                val remoteItem = when (collectionName) {
                    "homework" -> doc.toObject(Homework::class.java)
                    "grades" -> doc.toObject(Grade::class.java)
                    "subjects" -> doc.toObject(Subject::class.java)
                    "exams" -> doc.toObject(Exam::class.java)
                    "absences" -> doc.toObject(Absence::class.java)
                    "timetable" -> doc.toObject(TimetableEntry::class.java)
                    else -> null
                }
                if (remoteItem != null) {
                    insertLocal(remoteItem)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to download $collectionName", e)
        }
    }
}
