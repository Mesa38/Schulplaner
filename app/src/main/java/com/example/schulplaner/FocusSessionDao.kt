package com.example.schulplaner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insertSession(session: FocusSession)

    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE subject = :subjectName")
    fun getSessionsForSubject(subjectName: String): Flow<List<FocusSession>>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions")
    fun getTotalFocusMinutes(): Flow<Int?>
}
