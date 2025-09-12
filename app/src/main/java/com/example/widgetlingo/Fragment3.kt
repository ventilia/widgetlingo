package com.example.widgetlingo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.widgetlingo.databinding.Fragment3Binding

class Fragment3 : Fragment() {

    private var _binding: Fragment3Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Fragment3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // применяем шрифт itim
        val itimFont = ResourcesCompat.getFont(requireContext(), R.font.itim)
        binding.learningText.typeface = itimFont
        binding.btnOk.typeface = itimFont

        // устанавливаем текст с языком изучения
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val studyLang = prefs.getString("study_language", "english") ?: "english"
        binding.learningText.text = getString(R.string.learning_description, getStringForLanguage(studyLang))

        // кнопка ok
        binding.btnOk.setOnClickListener {
            findNavController().navigate(R.id.action_fragment3_to_fragment4)
        }
    }

    private fun getStringForLanguage(lang: String): String {
        return when (lang) {
            "russian" -> getString(R.string.russian)
            "english" -> getString(R.string.english)
            "spanish" -> getString(R.string.spanish)
            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}