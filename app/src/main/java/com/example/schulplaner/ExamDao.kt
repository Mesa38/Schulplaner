package com.example.schulplaner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date ASC")
    fun getAllExams(): Flow<List<Exam>>

    @Insert
    suspend fun insertExam(exam: Exam)

    @androidx.room.Update
    suspend fun updateExam(exam: Exam)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExam(id: Int)
}
