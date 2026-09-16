package com.example.notebook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notebook.databinding.FragmentProfileSetupBinding
import androidx.navigation.fragment.findNavController

class ProfileSetupFragment : Fragment() {

    private var _binding: FragmentProfileSetupBinding? = null
    private val binding get() = _binding!!

    private lateinit var styleAdapter: NotebookStyleAdapter
    private var selectedStyleId: String = "classic"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStylePicker()
        setupPrivacyToggle()
        setupButtons()
    }

    private fun setupStylePicker() {
        // Sample style list — swap ic_pencil for real preview icons/drawables per style later
        val styles = listOf(
            NotebookStyleOption(id = "classic", label = "Classic", previewIconRes = R.drawable.ic_pencil),
            NotebookStyleOption(id = "grid", label = "Grid", previewIconRes = R.drawable.ic_pencil),
            NotebookStyleOption(id = "kraft", label = "Kraft", previewIconRes = R.drawable.ic_pencil),
            NotebookStyleOption(id = "dotted", label = "Dotted", previewIconRes = R.drawable.ic_pencil)
        )

        styleAdapter = NotebookStyleAdapter(styles) { selected ->
            selectedStyleId = selected.id
        }

        binding.recyclerNotebookStyles.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerNotebookStyles.adapter = styleAdapter
    }

    private fun setupPrivacyToggle() {
        // Nothing needed here yet beyond reading its state on submit,
        // but this is where you'd react live to the toggle if needed:
        binding.switchPrivacy.setOnCheckedChangeListener { _, isChecked ->
            // isChecked == true means "private notebook"
        }
    }

    private fun setupButtons() {
        binding.buttonContinue.setOnClickListener {
            val displayName = binding.inputDisplayName.text?.toString()?.trim().orEmpty()
            val username = binding.inputUsername.text?.toString()?.trim().orEmpty()
            val bio = binding.inputBio.text?.toString()?.trim().orEmpty()
            val isPrivate = binding.switchPrivacy.isChecked

            if (displayName.isEmpty()) {
                binding.textError.text = "Please enter a display name"
                binding.textError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (username.isEmpty()) {
                binding.textError.text = "Please choose a username"
                binding.textError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            binding.textError.visibility = View.GONE

            findNavController().navigate(R.id.action_profileSetupFragment_to_notesFeedFragment)
            // to your backend/local store, then navigate onward.
        }

        binding.textSkip.setOnClickListener {
            findNavController().navigate(R.id.action_profileSetupFragment_to_notesFeedFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}