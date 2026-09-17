package com.example.notebook

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.notebook.databinding.DialogEditProfileBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class EditProfileDialogFragment(
    private val currentName: String,
    private val currentUsername: String,
    private val currentPronouns: String,
    private val currentBio: String,
    private val onProfileUpdated: () -> Unit
) : DialogFragment() {

    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.inputEditName.setText(currentName)
        binding.inputEditUsername.setText(currentUsername)
        binding.inputEditBio.setText(currentBio)

        val pronounOptions = listOf("She/her", "He/him", "They/them", "Prefer not to say")
        binding.inputEditPronouns.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, pronounOptions)
        )
        binding.inputEditPronouns.setText(currentPronouns, false)

        binding.buttonSaveProfile.setOnClickListener {
            val newName = binding.inputEditName.text?.toString()?.trim().orEmpty()
            val newUsername = binding.inputEditUsername.text?.toString()?.trim().orEmpty()
            val newPronouns = binding.inputEditPronouns.text?.toString()?.trim().orEmpty()
            val newBio = binding.inputEditBio.text?.toString()?.trim().orEmpty()

            if (newName.isEmpty()) {
                showError("Please enter a display name")
                return@setOnClickListener
            }
            if (newUsername.isEmpty()) {
                showError("Please choose a username")
                return@setOnClickListener
            }

            binding.buttonSaveProfile.isEnabled = false

            if (newUsername.equals(currentUsername, ignoreCase = true)) {
                saveChanges(newName, newUsername, newPronouns, newBio)
            } else {
                checkUsernameAvailable(newUsername) { isAvailable ->
                    if (isAvailable) {
                        saveChanges(newName, newUsername, newPronouns, newBio)
                    } else {
                        binding.buttonSaveProfile.isEnabled = true
                        showError("That username is already taken")
                    }
                }
            }
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
                    binding.buttonSaveProfile.isEnabled = true
                    showError("Couldn't verify username: ${error.message}")
                }
            })
    }

    private fun saveChanges(name: String, username: String, pronouns: String, bio: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            binding.buttonSaveProfile.isEnabled = true
            showError("Session expired. Please log in again.")
            return
        }

        val updates = mapOf(
            "displayName" to name,
            "username" to username,
            "usernameLower" to username.lowercase(Locale.getDefault()),
            "pronouns" to pronouns,
            "bio" to bio
        )

        FirebaseUtil.database.getReference("users").child(uid)
            .updateChildren(updates)
            .addOnSuccessListener {
                onProfileUpdated()
                dismiss()
            }
            .addOnFailureListener { e ->
                binding.buttonSaveProfile.isEnabled = true
                showError("Couldn't save changes: ${e.message}")
            }
    }

    private fun showError(message: String) {
        binding.textEditError.text = message
        binding.textEditError.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}