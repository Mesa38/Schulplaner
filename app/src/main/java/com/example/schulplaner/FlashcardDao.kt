package com.example.schulplaner

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE subject = :subject")
    fun getFlashcardsForSubject(subject: String): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards")
    fun getAllFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE nextReview <= :currentTime")
    fun getDueFlashcards(currentTime: Long): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: Flashcard)

    @Update
    suspend fun updateFlashcard(card: Flashcard)

    @Delete
    suspend fun deleteFlashcard(card: Flashcard)
}
