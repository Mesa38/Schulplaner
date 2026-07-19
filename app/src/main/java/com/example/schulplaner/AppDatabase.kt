package com.example.schulplaner

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Homework::class, TimetableEntry::class, Grade::class, Subject::class, Exam::class, UserStats::class, Topic::class, Flashcard::class, FocusSession::class, Absence::class], version = 500, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun homeworkDao(): HomeworkDao
    abstract fun timetableDao(): TimetableDao
    abstract fun gradeDao(): GradeDao
    abstract fun subjectDao(): SubjectDao
    abstract fun examDao(): ExamDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun topicDao(): TopicDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun absenceDao(): AbsenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abi_planer_final_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}