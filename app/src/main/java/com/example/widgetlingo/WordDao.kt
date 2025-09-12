package com.example.widgetlingo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WordDao {
    @Insert
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT * FROM words WHERE language = :lang AND difficulty = :diff ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWord(lang: String, diff: String): WordEntity?

    @Query("SELECT * FROM words WHERE language = :lang ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomMixedWord(lang: String): WordEntity?
}