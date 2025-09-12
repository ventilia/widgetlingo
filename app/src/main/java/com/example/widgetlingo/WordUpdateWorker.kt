package com.example.widgetlingo

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

class WordUpdateWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        try {

            val db = AppDatabase.getDatabase(applicationContext)
            val prefs = applicationContext.getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
            val studyLang = prefs.getString("study_language", "english") ?: "english"
            val difficulty = prefs.getString("difficulty", "mixed") ?: "mixed"

            val word = runBlocking {
                if (difficulty == "mixed") {
                    db.wordDao().getRandomMixedWord(studyLang)
                } else {
                    db.wordDao().getRandomWord(studyLang, difficulty)
                }
            } ?: return Result.failure()


            val gson = Gson()
            prefs.edit().putString("current_word", gson.toJson(word)).apply()
            prefs.edit().putLong("last_update_time", System.currentTimeMillis()).apply()


            WidgetProvider.updateWidget(applicationContext)
            return Result.success()
        } catch (e: Exception) {

            return Result.failure()
        }
    }
}