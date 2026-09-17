package com.example.notebook.ui.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notebook.NotebookStyleAdapter
import com.example.notebook.NotebookStyleOption
import com.example.notebook.R
import com.example.notebook.databinding.FragmentProfileSetupBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
        setupPronounsDropdown()
        setupDobPicker()
        setupButtons()
    }

    private fun setupStylePicker() {
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
        binding.switchPrivacy.setOnCheckedChangeListener { _, isChecked ->
            // isChecked == true means "private notebook"
        }
    }

    private fun setupPronounsDropdown() {
        val options = listOf("She/her", "He/him", "They/them", "Prefer not to say")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        binding.inputPronouns.setAdapter(adapter)
    }

    private fun setupDobPicker() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val openPicker = {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    binding.inputDob.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR) - 18,
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.inputDob.setOnClickListener { openPicker() }
        binding.layoutDob.setEndIconOnClickListener { openPicker() }
    }

    private fun setupButtons() {
        binding.buttonContinue.setOnClickListener {
            val displayName = binding.inputDisplayName.text?.toString()?.trim().orEmpty()
            val username = binding.inputUsername.text?.toString()?.trim().orEmpty()
            val dob = binding.inputDob.text?.toString()?.trim().orEmpty()
            val pronouns = binding.inputPronouns.text?.toString()?.trim().orEmpty()
            val location = binding.inputLocation.text?.toString()?.trim().orEmpty()
            val bio = binding.inputBio.text?.toString()?.trim().orEmpty()
            val isPrivate = binding.switchPrivacy.isChecked

            if (displayName.isEmpty()) {
                showError("Please enter a display name")
                return@setOnClickListener
            }
            if (username.isEmpty()) {
                showError("Please choose a username")
                return@setOnClickListener
            }
            if (dob.isEmpty()) {
                showError("Please add your date of birth")
                return@setOnClickListener
            }

            binding.textError.visibility = View.GONE
            binding.buttonContinue.isEnabled = false

            checkUsernameAvailable(username) { isAvailable ->
                if (!isAvailable) {
                    binding.buttonContinue.isEnabled = true
                    showError("That username is already taken")
                    return@checkUsernameAvailable
                }

                saveProfile(
                    displayName = displayName,
                    username = username,
                    dob = dob,
                    pronouns = pronouns,
                    location = location,
                    bio = bio,
                    isPrivate = isPrivate
                )
            }
        }

        binding.textSkip.setOnClickListener {
            findNavController().navigate(R.id.action_profileSetupFragment_to_notesFeedFragment)
        }
    }

    private fun checkUsernameAvailable(username: String, onResult: (Boolean) -> Unit) {
        val usernameLower = username.lowercase(Locale.getDefault())

        FirebaseUtil.database.getReference("users")
            .orderByChild("usernameLower")
            .equalTo(usernameLower)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onResult(!snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.buttonContinue.isEnabled = true
                    showError("Couldn't verify username: ${error.message}")
                }
            })
    }

    private fun saveProfile(
        displayName: String,
        username: String,
        dob: String,
        pronouns: String,
        location: String,
        bio: String,
        isPrivate: Boolean
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            binding.buttonContinue.isEnabled = true
            showError("Session expired. Please log in again.")
            return
        }

        val profileUpdates = mapOf(
            "displayName" to displayName,
            "username" to username,
            "usernameLower" to username.lowercase(Locale.getDefault()),
            "dob" to dob,
            "pronouns" to pronouns,
            "location" to location,
            "bio" to bio,
            "isPrivate" to isPrivate,
            "notebookStyle" to selectedStyleId,
            "profileComplete" to true
        )

        FirebaseUtil.database.getReference("users").child(uid)
            .updateChildren(profileUpdates)
            .addOnSuccessListener {
                binding.buttonContinue.isEnabled = true
                findNavController().navigate(R.id.action_profileSetupFragment_to_notesFeedFragment)
            }
            .addOnFailureListener { e ->
                binding.buttonContinue.isEnabled = true
                showError("Couldn't save profile: ${e.message}")
            }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}