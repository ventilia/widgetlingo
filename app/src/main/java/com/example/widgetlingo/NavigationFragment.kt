package com.example.widgetlingo

import android.app.AlertDialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.example.widgetlingo.databinding.FragmentNavigationBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NavigationFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация значений из prefs
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val currentNative = prefs.getString("native_language", "en") ?: "en"
        val currentStudy = prefs.getString("study_language", "english") ?: "english"
        val currentDifficulty = prefs.getString("difficulty", "mixed") ?: "mixed"
        val currentFrequency = prefs.getString("frequency", "day") ?: "day"

        binding.nativeLanguageText.text = when (currentNative) {
            "ru" -> getString(R.string.russian)
            else -> getString(R.string.english)
        }
        binding.studyLanguageText.text = when (currentStudy) {
            "russian" -> getString(R.string.russian)
            "spanish" -> getString(R.string.spanish)
            else -> getString(R.string.english)
        }
        binding.difficultyText.text = when (currentDifficulty) {
            "beginner" -> getString(R.string.beginner)
            "intermediate" -> getString(R.string.intermediate)
            "advanced" -> getString(R.string.advanced)
            else -> getString(R.string.mixed)
        }
        binding.frequencyText.text = when (currentFrequency) {
            "hour" -> getString(R.string.every_hour)
            "6hours" -> getString(R.string.every_6_hours)
            else -> getString(R.string.every_day)
        }
        updateWidgetBlockText()

        // Клик по блокам
        binding.nativeLanguageBlock.setOnClickListener {
            showNativeLanguageChoice()
        }
        binding.studyLanguageBlock.setOnClickListener {
            showStudyLanguageChoice()
        }
        binding.difficultyBlock.setOnClickListener {
            showDifficultyDialog()
        }
        binding.frequencyBlock.setOnClickListener {
            showFrequencyDialog()
        }
        binding.widgetBlock.setOnClickListener {
            if (!isWidgetAdded()) {
                addWidget()
            } else {
                AlertDialog.Builder(requireContext())
                    .setMessage("Виджет уже добавлен")
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
            updateWidgetBlockText()
        }
        binding.githubBlock.setOnClickListener {
            // Открыть GitHub
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ventilia/widgetlingo"))
            startActivity(intent)
        }
        binding.creatorsBlock.setOnClickListener {
            showCreatorsDialog()
        }

        // Применяем шрифт Itim к текстам
        val itimFont = ResourcesCompat.getFont(requireContext(), R.font.itim)
        binding.nativeLanguageText.typeface = itimFont
        binding.studyLanguageText.typeface = itimFont
        binding.difficultyText.typeface = itimFont
        binding.frequencyText.typeface = itimFont
        binding.widgetText.typeface = itimFont
        binding.infoTitle.typeface = itimFont
    }

    private fun showNativeLanguageChoice() {
        val options = arrayOf(getString(R.string.russian), getString(R.string.english))
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val current = prefs.getString("native_language", "en") ?: "en"
        val checked = if (current == "ru") 0 else 1

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.native_language)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val newNative = if (which == 0) "ru" else "en"
                prefs.edit().putString("native_language", newNative).apply()
                binding.nativeLanguageText.text = options[which]
                activity?.recreate()  // Для применения локали
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showStudyLanguageChoice() {
        val options = arrayOf(getString(R.string.russian), getString(R.string.english), getString(R.string.spanish))
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val current = prefs.getString("study_language", "english") ?: "english"
        val checked = when (current) {
            "russian" -> 0
            "english" -> 1
            "spanish" -> 2
            else -> 1
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.study_language)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val newStudy = when (which) {
                    0 -> "russian"
                    1 -> "english"
                    2 -> "spanish"
                    else -> "english"
                }
                prefs.edit().putString("study_language", newStudy).apply()
                binding.studyLanguageText.text = options[which]
                WidgetProvider.updateWidget(requireContext())
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDifficultyDialog() {
        val options = arrayOf(getString(R.string.beginner), getString(R.string.intermediate), getString(R.string.advanced), getString(R.string.mixed))
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val current = prefs.getString("difficulty", "mixed") ?: "mixed"
        val checked = when (current) {
            "beginner" -> 0
            "intermediate" -> 1
            "advanced" -> 2
            else -> 3
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.difficulty)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val newDifficulty = when (which) {
                    0 -> "beginner"
                    1 -> "intermediate"
                    2 -> "advanced"
                    else -> "mixed"
                }
                prefs.edit().putString("difficulty", newDifficulty).apply()
                binding.difficultyText.text = options[which]
                WidgetProvider.updateWidget(requireContext())
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showFrequencyDialog() {
        val options = arrayOf(getString(R.string.every_hour), getString(R.string.every_6_hours), getString(R.string.every_day))
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val current = prefs.getString("frequency", "day") ?: "day"
        val checked = when (current) {
            "hour" -> 0
            "6hours" -> 1
            else -> 2
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.frequency)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val newFrequency = when (which) {
                    0 -> "hour"
                    1 -> "6hours"
                    else -> "day"
                }
                prefs.edit().putString("frequency", newFrequency).apply()
                binding.frequencyText.text = options[which]
                (activity as? MainActivity)?.scheduleWordUpdate()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun isWidgetAdded(): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(requireContext())
        val provider = ComponentName(requireContext(), WidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(provider)
        return appWidgetIds.isNotEmpty()
    }

    private fun addWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(requireContext())
        val provider = ComponentName(requireContext(), WidgetProvider::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
            val pinnedWidgetCallbackIntent = Intent(requireContext(), MainActivity::class.java)
            pinnedWidgetCallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val successCallback = PendingIntent.getActivity(
                requireContext(),
                0,
                pinnedWidgetCallbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            appWidgetManager.requestPinAppWidget(provider, null, successCallback)
        } else {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.add_widget_manually)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    private fun updateWidgetBlockText() {
        binding.widgetText.text = if (isWidgetAdded()) {
            "Виджет добавлен"
        } else {
            getString(R.string.add_widget)
        }
    }

    private fun showCreatorsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.creators_title)
            .setMessage(R.string.creators_description)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}