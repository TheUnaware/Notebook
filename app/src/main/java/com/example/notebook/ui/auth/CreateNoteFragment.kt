package com.example.notebook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notebook.databinding.FragmentCreateNoteBinding

class CreateNoteFragment : Fragment() {

    private var _binding: FragmentCreateNoteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardBasic.setOnClickListener { navigateToEditor(isAnonymous = false, isDaily = false) }
        binding.cardDaily.setOnClickListener { navigateToEditor(isAnonymous = false, isDaily = true) }
        binding.cardAnonymous.setOnClickListener { navigateToEditor(isAnonymous = true, isDaily = false) }
    }

    private fun navigateToEditor(isAnonymous: Boolean, isDaily: Boolean) {
        val bundle = Bundle().apply {
            putBoolean("isAnonymous", isAnonymous)
            putBoolean("isDaily", isDaily)
        }
        findNavController().navigate(R.id.action_createNote_to_editor, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}