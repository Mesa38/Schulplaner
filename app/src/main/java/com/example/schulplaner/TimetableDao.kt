package com.example.schulplaner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable ORDER BY dayOfWeek, hour")
    fun getAllEntries(): Flow<List<TimetableEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntry)

    @Query("DELETE FROM timetable WHERE dayOfWeek = :day AND hour = :hour")
    suspend fun deleteEntry(day: Int, hour: Int)
}
