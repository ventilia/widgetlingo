package com.example.widgetlingo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.widgetlingo.databinding.Fragment4Binding

class Fragment4 : Fragment() {

    private var _binding: Fragment4Binding? = null
    private val binding get() = _binding!!

    private var selectedDifficulty: String? = null
    private var selectedFrequency: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Fragment4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val itimFont = ResourcesCompat.getFont(requireContext(), R.font.itim)
        binding.greatText.typeface = itimFont
        binding.btnNext.typeface = itimFont

        // настройка spinner для сложности
        val difficulties = arrayOf(
            getString(R.string.beginner),
            getString(R.string.intermediate),
            getString(R.string.advanced),
            getString(R.string.mixed)
        )
        val difficultyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, difficulties)
        binding.difficultySpinner.adapter = difficultyAdapter
        binding.difficultySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedDifficulty = when (position) {
                    0 -> "beginner"
                    1 -> "intermediate"
                    2 -> "advanced"
                    3 -> "mixed"
                    else -> null
                }
                checkSelections()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedDifficulty = null
            }
        }

        // настройка spinner для частоты
        val frequencies = arrayOf(
            getString(R.string.every_hour),
            getString(R.string.every_6_hours),
            getString(R.string.every_day)
        )
        val frequencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, frequencies)
        binding.frequencySpinner.adapter = frequencyAdapter
        binding.frequencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedFrequency = when (position) {
                    0 -> "hour"
                    1 -> "6hours"
                    2 -> "day"
                    else -> null
                }
                checkSelections()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedFrequency = null
            }
        }

        // кнопка далее
        binding.btnNext.setOnClickListener {
            updateSettings()
            findNavController().navigate(R.id.action_fragment4_to_fragment5)
        }
    }

    private fun checkSelections() {
        if (selectedDifficulty != null && selectedFrequency != null) {
            binding.btnNext.visibility = View.VISIBLE
        }
    }

    private fun updateSettings() {
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val nativeLang = prefs.getString("native_language", "en") ?: "en"
        val studyLang = prefs.getString("study_language", "english") ?: "english"
        (activity as MainActivity).saveSettings(nativeLang, studyLang, selectedDifficulty ?: "mixed", selectedFrequency ?: "day")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}