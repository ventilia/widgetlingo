package com.example.widgetlingo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val translation: String,
    val transcription: String,
    val language: String,
    val difficulty: String
)

// В смешанной сложности выбираем рандомно из всех