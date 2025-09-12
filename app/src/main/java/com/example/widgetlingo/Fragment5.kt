package com.example.widgetlingo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.widgetlingo.databinding.Fragment5Binding

class Fragment5 : Fragment() {

    private var _binding: Fragment5Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Fragment5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val itimFont = ResourcesCompat.getFont(requireContext(), R.font.itim)
        binding.allReadyText.typeface = itimFont
        binding.btnCreateWidget.typeface = itimFont


        binding.btnCreateWidget.setOnClickListener {
            (activity as MainActivity).finishOnboarding()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}