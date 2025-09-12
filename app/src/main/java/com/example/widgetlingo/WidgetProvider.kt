package com.example.widgetlingo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.google.gson.Gson

class WidgetProvider : AppWidgetProvider() {

    // Метод вызывается при каждом обновлении виджета
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateWidget(context)
    }

    // Основной метод обновления: загружаем слово и устанавливаем в views
    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val wordJson = prefs.getString("current_word", null)
        if (wordJson == null) {
            Log.e("WidgetProvider", "Нет слова в prefs")
            return
        }

        val gson = Gson()
        val wordEntity: WordEntity? = gson.fromJson(wordJson, WordEntity::class.java)
        if (wordEntity == null) {
            Log.e("WidgetProvider", "Ошибка парсинга слова")
            return
        }

        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        views.setTextViewText(R.id.tv_word, wordEntity.word)
        views.setTextViewText(R.id.tv_translation, wordEntity.translation)
        views.setTextViewText(R.id.tv_transcription, wordEntity.transcription)

        // Проверка размера для видимости транскрипции
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        if (minWidth < 150 || minHeight < 150) {
            views.setViewVisibility(R.id.tv_transcription, View.GONE)
        } else {
            views.setViewVisibility(R.id.tv_transcription, View.VISIBLE)
        }

        // Клик на весь виджет открывает приложение
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        // Статический метод для обновления всех виджетов из других частей приложения
        fun updateWidget(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisAppWidget = ComponentName(context, WidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)

                if (appWidgetIds.isNotEmpty()) {
                    val provider = WidgetProvider()
                    provider.onUpdate(context, appWidgetManager, appWidgetIds)
                }
            } catch (e: Exception) {
                Log.e("WidgetProvider", "Ошибка обновления виджета: ${e.message}")
            }
        }
    }
}