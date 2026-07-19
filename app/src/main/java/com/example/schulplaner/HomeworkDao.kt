package com.example.schulplaner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeworkDao {

    @Insert
    suspend fun insertHomework(homework: Homework)

    @Query("SELECT * FROM homework ORDER BY dueDate ASC")
    fun getAllHomework(): Flow<List<Homework>>

    @Query("DELETE FROM homework WHERE id = :id")
    suspend fun deleteHomework(id: Int)
}