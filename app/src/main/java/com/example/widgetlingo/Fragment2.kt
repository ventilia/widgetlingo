package com.example.widgetlingo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.widgetlingo.databinding.Fragment2Binding

class Fragment2 : Fragment() {

    private var _binding: Fragment2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Fragment2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // применяем шрифт itim
        val itimFont = ResourcesCompat.getFont(requireContext(), R.font.itim)
        binding.helloText.typeface = itimFont
        binding.tvRussianStudy.typeface = itimFont
        binding.tvEnglishStudy.typeface = itimFont
        binding.tvSpanishStudy.typeface = itimFont

        // выбор языка изучения
        binding.layoutRussianStudy.setOnClickListener {
            updateSettings(studyLang = "russian")
            findNavController().navigate(R.id.action_fragment2_to_fragment3)
        }

        binding.layoutEnglishStudy.setOnClickListener {
            updateSettings(studyLang = "english")
            findNavController().navigate(R.id.action_fragment2_to_fragment3)
        }

        binding.layoutSpanishStudy.setOnClickListener {
            updateSettings(studyLang = "spanish")
            findNavController().navigate(R.id.action_fragment2_to_fragment3)
        }
    }

    private fun updateSettings(studyLang: String) {
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val nativeLang = prefs.getString("native_language", "en") ?: "en"
        (activity as MainActivity).saveSettings(nativeLang, studyLang, "", "")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}