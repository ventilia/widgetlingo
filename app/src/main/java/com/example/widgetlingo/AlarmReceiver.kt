package com.example.widgetlingo

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Получен аларм, обновление слова")
        try {
            val db = AppDatabase.getDatabase(context)
            val prefs = context.getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
            val studyLang = prefs.getString("study_language", "english") ?: "english"
            val difficulty = prefs.getString("difficulty", "mixed") ?: "mixed"

            val word = runBlocking {
                if (difficulty == "mixed") {
                    db.wordDao().getRandomMixedWord(studyLang)
                } else {
                    db.wordDao().getRandomWord(studyLang, difficulty)
                }
            } ?: run {
                Log.e("AlarmReceiver", "Нет слова")
                return
            }

            val gson = Gson()
            prefs.edit().putString("current_word", gson.toJson(word)).apply()
            prefs.edit().putLong("last_update_time", System.currentTimeMillis()).apply()

            WidgetProvider.updateWidget(context)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, WidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(provider)
            for (id in appWidgetIds) {
                appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_root)
            }
            Log.d("AlarmReceiver", "Слово обновлено, виджет notified")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Ошибка в ресивере: ${e.message}")
        }
    }
}