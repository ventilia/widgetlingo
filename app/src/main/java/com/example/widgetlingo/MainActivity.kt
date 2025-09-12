package com.example.widgetlingo

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.widgetlingo.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun attachBaseContext(newBase: Context?) {
        // Применение локали на основе prefs
        val prefs = newBase?.getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val lang = prefs?.getString("native_language", "en") ?: "en"
        val locale = if (lang == "ru") Locale("ru", "RU") else Locale(lang)
        val context = newBase?.let {
            val config = it.resources.configuration
            config.setLocale(locale)
            it.createConfigurationContext(config)
        } ?: newBase
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        navigationView = binding.navView

        val prefs = getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)

        // Усиленный сброс онбординга: если флаг completed true, но ключевых настроек нет (повреждённые данные после uninstall), очищаем prefs полностью
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        if (onboardingCompleted && (!prefs.contains("study_language") || !prefs.contains("native_language"))) {
            prefs.edit().clear().apply()
            Log.d("MainActivity", "Обнаружено несоответствие в prefs, полный сброс для исправления онбординга")
        }

        // Повторная проверка после возможного сброса
        val finalOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as? NavHostFragment
            ?: return

        navController = navHostFragment.navController

        val navGraph = navController.navInflater.inflate(R.navigation.main_nav_graph)

        if (!finalOnboardingCompleted) {
            navGraph.setStartDestination(R.id.fragment1)
            binding.toolbar.visibility = View.GONE
        } else {
            navGraph.setStartDestination(R.id.homeFragment)
            binding.toolbar.visibility = View.VISIBLE
        }

        navController.graph = navGraph

        // Настройка тулбара
        val itimFont = ResourcesCompat.getFont(this, R.font.itim)
        binding.titleText.typeface = itimFont

        // Кнопка меню для drawer
        binding.menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Настройка блоков в NavigationView
        setupNavigationView()

        if (finalOnboardingCompleted) {
            if (navController.currentDestination?.id != R.id.homeFragment) {
                val options = NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .setLaunchSingleTop(true)
                    .build()
                navController.navigate(R.id.homeFragment, null, options)
            }

            requestIgnoreBatteryOptimizations()

            if (!prefs.contains("current_word")) {
                generateAndSaveNewWord()
            }

            if (!isWidgetAdded()) {
                addWidget()
            }
        }

        scheduleWordUpdate()
    }

    // Настройка блоков настроек в drawer
    private fun setupNavigationView() {
        val customNavView = layoutInflater.inflate(R.layout.fragment_navigation, navigationView, false)
        navigationView.addView(customNavView)

        val nativeBlock = customNavView.findViewById<LinearLayout>(R.id.native_language_block)
        val nativeText = customNavView.findViewById<TextView>(R.id.native_language_text)
        val studyBlock = customNavView.findViewById<LinearLayout>(R.id.study_language_block)
        val studyText = customNavView.findViewById<TextView>(R.id.study_language_text)
        val difficultyBlock = customNavView.findViewById<LinearLayout>(R.id.difficulty_block)
        val difficultyText = customNavView.findViewById<TextView>(R.id.difficulty_text)
        val frequencyBlock = customNavView.findViewById<LinearLayout>(R.id.frequency_block)
        val frequencyText = customNavView.findViewById<TextView>(R.id.frequency_text)
        val widgetBlock = customNavView.findViewById<LinearLayout>(R.id.widget_block)

        val githubBlock = customNavView.findViewById<LinearLayout>(R.id.github_block)
        val creatorsBlock = customNavView.findViewById<LinearLayout>(R.id.creators_block)
        val infoTitle = customNavView.findViewById<TextView>(R.id.info_title)

        val prefs = getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val currentNative = prefs.getString("native_language", "en") ?: "en"
        val currentStudy = prefs.getString("study_language", "english") ?: "english"
        val currentDifficulty = prefs.getString("difficulty", "mixed") ?: "mixed"
        val currentFrequency = prefs.getString("frequency", "day") ?: "day"

        nativeText.text = when (currentNative) {
            "ru" -> getString(R.string.russian)
            else -> getString(R.string.english)
        }
        studyText.text = when (currentStudy) {
            "russian" -> getString(R.string.russian)
            "spanish" -> getString(R.string.spanish)
            else -> getString(R.string.english)
        }
        difficultyText.text = when (currentDifficulty) {
            "beginner" -> getString(R.string.beginner)
            "intermediate" -> getString(R.string.intermediate)
            "advanced" -> getString(R.string.advanced)
            else -> getString(R.string.mixed)
        }
        frequencyText.text = when (currentFrequency) {
            "hour" -> getString(R.string.every_hour)
            "6hours" -> getString(R.string.every_6_hours)
            else -> getString(R.string.every_day)
        }


        val itimFont = ResourcesCompat.getFont(this, R.font.itim)
        nativeText.typeface = itimFont
        studyText.typeface = itimFont
        difficultyText.typeface = itimFont
        frequencyText.typeface = itimFont

        infoTitle.typeface = itimFont

        nativeBlock.setOnClickListener {
            showNativeLanguageChoice(nativeText)
        }
        studyBlock.setOnClickListener {
            showStudyLanguageChoice(studyText)
        }
        difficultyBlock.setOnClickListener {
            showDifficultyDialog(difficultyText)
        }
        frequencyBlock.setOnClickListener {
            showFrequencyDialog(frequencyText)
        }
        widgetBlock.setOnClickListener {
            if (!isWidgetAdded()) {
                addWidget()
            } else {
                AlertDialog.Builder(this)
                    .setMessage("Виджет уже добавлен")
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }

        }
        githubBlock.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ventilia/widgetlingo"))
            startActivity(intent)
        }
        creatorsBlock.setOnClickListener {
            showCreatorsDialog()
        }
    }

    // Диалог выбора родного языка
    private fun showNativeLanguageChoice(textView: TextView) {
        val options = arrayOf(getString(R.string.russian), getString(R.string.english))
        val prefs = getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val current = prefs.getString("native_language", "en") ?: "en"
        val checked = if (current == "ru") 0 else 1

        AlertDialog.Builder(this)
            .setTitle(R.string.native_language)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val newNative = if (which == 0) "ru" else "en"
                prefs.edit().putString("native_language", newNative).apply()
                textView.text = options[which]
                recreate()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // Диалог выбора языка изучения
    private fun showStudyLanguageChoice(textView: TextView) {
        val options = arrayOf(getString(R.string.russian), getString(R.string.english), getString(R.string.spanish))
        val prefs = getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val current = prefs.getString("study_language", "english") ?: "english"
        val checked = when (current) {
            "russian" -> 0
            "english" -> 1
            "spanish" -> 2
            else -> 1
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.study_language)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val newStudy = when (which) {
                    0 -> "russian"
                    1 -> "english"
                    2 -> "spanish"
                    else -> "english"
                }
                prefs.edit().putString("study_language", newStudy).apply()
                textView.text = options[which]
                WidgetProvider.updateWidget(this)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // Диалог выбора сложности
    private fun showDifficultyDialog(textView: TextView) {
        val options = arrayOf(getString(R.string.beginner), getString(R.string.intermediate), getString(R.string.advanced), getString(R.string.mixed))
        val prefs = getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val current = prefs.getString("difficulty", "mixed") ?: "mixed"
        val checked = when (current) {
            "beginner" -> 0
            "intermediate" -> 1
            "advanced" -> 2
            else -> 3
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.difficulty)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val newDifficulty = when (which) {
                    0 -> "beginner"
                    1 -> "intermediate"
                    2 -> "advanced"
                    else -> "mixed"
                }
                prefs.edit().putString("difficulty", newDifficulty).apply()
                textView.text = options[which]
                WidgetProvider.updateWidget(this)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // Диалог выбора частоты
    private fun showFrequencyDialog(textView: TextView) {
        val options = arrayOf(getString(R.string.every_hour), getString(R.string.every_6_hours), getString(R.string.every_day))
        val prefs = getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val current = prefs.getString("frequency", "day") ?: "day"
        val checked = when (current) {
            "hour" -> 0
            "6hours" -> 1
            else -> 2
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.frequency)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val newFrequency = when (which) {
                    0 -> "hour"
                    1 -> "6hours"
                    else -> "day"
                }
                prefs.edit().putString("frequency", newFrequency).apply()
                textView.text = options[which]
                scheduleWordUpdate()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // Обновление текста для блока виджета
    private fun updateWidgetText(textView: TextView) {
        textView.text = if (isWidgetAdded()) {
            "Виджет добавлен"
        } else {
            getString(R.string.add_widget)
        }
    }

    // Диалог о создателях
    private fun showCreatorsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.creators_title)
            .setMessage(R.string.creators_description)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    // Сохранение настроек (для онбординга или других фрагментов)
    fun saveSettings(nativeLang: String = "", studyLang: String = "", difficulty: String = "", frequency: String = "") {
        val prefs = getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (nativeLang.isNotEmpty()) putString("native_language", nativeLang)
            if (studyLang.isNotEmpty()) putString("study_language", studyLang)
            if (difficulty.isNotEmpty()) putString("difficulty", difficulty)
            if (frequency.isNotEmpty()) putString("frequency", frequency)
            apply()
        }
    }

    // Завершение онбординга
    fun finishOnboarding() {
        val prefs = getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        recreate()
    }

    // Планирование обновления слова через AlarmManager
    fun scheduleWordUpdate() {
        val prefs = getSharedPreferences("WidgetLingoPrefs", MODE_PRIVATE)
        val frequency = prefs.getString("frequency", "day") ?: "day"
        val interval = when (frequency) {
            "hour" -> AlarmManager.INTERVAL_HOUR
            "6hours" -> 6 * AlarmManager.INTERVAL_HOUR
            "day" -> AlarmManager.INTERVAL_DAY
            else -> AlarmManager.INTERVAL_DAY
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val triggerTime = System.currentTimeMillis() + interval

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    interval,
                    pendingIntent
                )
            }
            Log.d("MainActivity", "Alarm scheduled successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error scheduling alarm: ${e.message}")
        }
    }

    // Запрос на игнор оптимизации батареи
    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val prefs = getSharedPreferences("WidgetLingoPrefs", MODE_PRIVATE)
            if (!prefs.getBoolean("battery_optimization_requested", false)) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                    prefs.edit().putBoolean("battery_optimization_requested", true).apply()
                }
            }
        }
    }

    // Проверка наличия виджета
    private fun isWidgetAdded(): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, WidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(provider)
        return appWidgetIds.isNotEmpty()
    }

    // Добавление виджета
    private fun addWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, WidgetProvider::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
            val pinnedWidgetCallbackIntent = Intent(this, MainActivity::class.java)
            pinnedWidgetCallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val successCallback = PendingIntent.getActivity(
                this,
                0,
                pinnedWidgetCallbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            appWidgetManager.requestPinAppWidget(provider, null, successCallback)
        } else {
            AlertDialog.Builder(this)
                .setMessage(R.string.add_widget_manually)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    // Генерация нового слова асинхронно
    private fun generateAndSaveNewWord() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val prefs = getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
            val studyLang = prefs.getString("study_language", "english") ?: "english"
            val difficulty = prefs.getString("difficulty", "mixed") ?: "mixed"

            val word = if (difficulty == "mixed") {
                db.wordDao().getRandomMixedWord(studyLang)
            } else {
                db.wordDao().getRandomWord(studyLang, difficulty)
            }

            if (word != null) {
                val gson = Gson()
                prefs.edit().putString("current_word", gson.toJson(word)).apply()
                prefs.edit().putLong("last_update_time", System.currentTimeMillis()).apply()
                WidgetProvider.updateWidget(this@MainActivity)
            } else {
                Log.e("MainActivity", "Нет слов в базе данных")
            }
        }
    }

    // Обработка кнопки назад для закрытия drawer
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}