package com.example.notebook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.appcompat.widget.PopupMenu
import com.example.notebook.databinding.FragmentProfileBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var currentName = ""
    private var currentUsername = ""
    private var currentPronouns = ""
    private var currentBio = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        binding.buttonMore.setOnClickListener { showOptionsMenu() }
    }

    private fun loadProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseUtil.database.getReference("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    currentName = snapshot.child("displayName").getValue(String::class.java).orEmpty()
                    currentUsername = snapshot.child("username").getValue(String::class.java).orEmpty()
                    currentPronouns = snapshot.child("pronouns").getValue(String::class.java).orEmpty()
                    currentBio = snapshot.child("bio").getValue(String::class.java).orEmpty()
                    val style = snapshot.child("notebookStyle").getValue(String::class.java) ?: "classic"

                    binding.textUsername.text = "@$currentUsername"
                    binding.textName.text = currentName
                    binding.textPronouns.text = currentPronouns
                    binding.textBio.text = currentBio

                    applyCoverStyle(style)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Silently ignore for now, or show a toast/snackbar later
                }
            })
    }

    private fun applyCoverStyle(styleId: String) {
        val colorRes = when (styleId) {
            "grid" -> R.color.notebook_style_grid
            "kraft" -> R.color.notebook_style_kraft
            "dotted" -> R.color.notebook_style_dotted
            else -> R.color.notebook_style_classic
        }
        binding.cardCover.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun showOptionsMenu() {
        val popup = PopupMenu(requireContext(), binding.buttonMore)
        popup.menuInflater.inflate(R.menu.profile_options_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit_profile -> {
                    openEditProfile()
                    true
                }
                R.id.action_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openEditProfile() {
        EditProfileDialogFragment(
            currentName = currentName,
            currentUsername = currentUsername,
            currentPronouns = currentPronouns,
            currentBio = currentBio,
            onProfileUpdated = { loadProfile() }
        ).show(parentFragmentManager, "EditProfileDialog")
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        findNavController().navigate(R.id.action_profile_to_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}