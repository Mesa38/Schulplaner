package com.example.schulplaner

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AbsenceDao {
    @Query("SELECT * FROM absences ORDER BY date DESC")
    fun getAllAbsences(): Flow<List<Absence>>

    @Insert
    suspend fun insertAbsence(absence: Absence)

    @Update
    suspend fun updateAbsence(absence: Absence)

    @Delete
    suspend fun deleteAbsence(absence: Absence)

    @Query("SELECT COUNT(*) FROM absences WHERE isExcused = 1")
    suspend fun getExcusedCount(): Int
}
