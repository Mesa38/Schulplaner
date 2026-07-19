package com.example.schulplaner

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE subject = :subject ORDER BY orderIndex ASC")
    fun getTopicsForSubject(subject: String): Flow<List<Topic>>

    @Query("SELECT * FROM topics ORDER BY orderIndex ASC")
    fun getAllTopics(): Flow<List<Topic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic)

    @Update
    suspend fun updateTopic(topic: Topic)

    @Delete
    suspend fun deleteTopic(topic: Topic)

    @Query("SELECT COUNT(*) FROM topics WHERE subject = :subject")
    suspend fun getTopicCountForSubject(subject: String): Int
    
    @Query("SELECT MAX(orderIndex) FROM topics WHERE subject = :subject")
    suspend fun getMaxOrderIndex(subject: String): Int?
    
    @Transaction
    suspend fun swapOrder(topic1: Topic, topic2: Topic) {
        val index1 = topic1.orderIndex
        val index2 = topic2.orderIndex
        updateTopic(topic1.copy(orderIndex = index2))
        updateTopic(topic2.copy(orderIndex = index1))
    }
}
