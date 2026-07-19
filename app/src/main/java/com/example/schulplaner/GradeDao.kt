package com.example.schulplaner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades ORDER BY semester ASC, subject ASC")
    fun getAllGrades(): Flow<List<Grade>>

    @Insert
    suspend fun insertGrade(grade: Grade)

    @Query("DELETE FROM grades WHERE id = :id")
    suspend fun deleteGrade(id: Int)
}
