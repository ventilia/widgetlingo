package com.example.widgetlingo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.widgetlingo.databinding.Fragment1Binding

class Fragment1 : Fragment() {

    private var _binding: Fragment1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Fragment1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // применяем шрифт itim
        val itimFont = ResourcesCompat.getFont(requireContext(), R.font.itim)
        binding.welcomeText.typeface = itimFont
        binding.tvRussianNative.typeface = itimFont
        binding.tvEnglishNative.typeface = itimFont

        // получаем prefs
        val prefs = requireContext().getSharedPreferences("WidgetLingoPrefs", Context.MODE_PRIVATE)
        val nativeLang = prefs.getString("native_language", "") ?: ""

        // skip если lang set (после recreate от language choice)
        if (nativeLang.isNotEmpty()) {
            val navController = findNavController()
            // safety check: navigate только если current dest — fragment1 (фикс краша при mismatch из restored state)
            if (navController.currentDestination?.id == R.id.fragment1) {
                navController.navigate(R.id.action_fragment1_to_fragment2)
            }
            return
        }

        // выбор родного языка
        binding.layoutRussianNative.setOnClickListener {
            (activity as MainActivity).saveSettings("ru", "", "", "")
            // recreate для применения новой локали (attachbasecontext подхватит)
            activity?.recreate()
        }

        binding.layoutEnglishNative.setOnClickListener {
            (activity as MainActivity).saveSettings("en", "", "", "")
            activity?.recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}