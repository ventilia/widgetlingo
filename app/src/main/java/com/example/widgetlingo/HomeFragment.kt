package com.example.widgetlingo

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.widgetlingo.databinding.FragmentHomeBinding
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUpdateWord.setOnClickListener {
            updateWord()
        }

        loadCurrentWord()
    }

    private fun loadCurrentWord() {
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val wordJson = prefs.getString("current_word", null)

        val word: WordEntity? = if (wordJson != null) {
            gson.fromJson(wordJson, WordEntity::class.java)
        } else {
            null
        }

        if (word != null) {
            updateUIWithWord(word)
        } else {
            generateAndSaveNewWord()
        }

        startTimer()
    }

    private fun updateWord() {
        generateAndSaveNewWord()
    }

    private fun generateAndSaveNewWord() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
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

                withContext(Dispatchers.Main) {
                    updateUIWithWord(word)
                }

                WidgetProvider.updateWidget(requireContext())
                val appWidgetManager = AppWidgetManager.getInstance(requireContext())
                val provider = ComponentName(requireContext(), WidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(provider)
                for (id in appWidgetIds) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_root)
                }
            } else {
                Log.e("HomeFragment", "No word available in DB")
            }
        }
    }

    private fun updateUIWithWord(word: WordEntity) {
        binding.tvCurrentWord.text = word.word
        binding.tvCurrentTranslation.text = word.translation
        binding.tvCurrentTranscription.text = word.transcription  // Всегда показываем, если слово есть
    }

    private fun startTimer() {
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val frequency = prefs.getString("frequency", "day") ?: "day"
        val intervalMillis = when (frequency) {
            "hour" -> TimeUnit.HOURS.toMillis(1)
            "6hours" -> TimeUnit.HOURS.toMillis(6)
            "day" -> TimeUnit.HOURS.toMillis(24)
            else -> TimeUnit.HOURS.toMillis(24)
        }

        timerRunnable = object : Runnable {
            override fun run() {
                val lastUpdate = prefs.getLong("last_update_time", 0)
                val timePassed = System.currentTimeMillis() - lastUpdate
                val timeLeft = intervalMillis - timePassed

                if (timeLeft > 0) {
                    val hours = TimeUnit.MILLISECONDS.toHours(timeLeft)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
                    binding.tvNextUpdate.text = "Обновится через: ${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                    handler.postDelayed(this, 1000)
                } else {
                    updateWord()
                }
            }
        }
        handler.post(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        loadCurrentWord()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)  // Избегаем утечек
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}