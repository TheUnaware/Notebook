package com.example.notebook

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notebook.databinding.FragmentProfileBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import androidx.recyclerview.widget.GridLayoutManager

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var currentName = ""
    private var currentUsername = ""
    private var currentPronouns = ""
    private var currentBio = ""
    private var currentStyle = "classic"
    private var currentCoverColor: String? = null
    private var currentAvatarBase64: String? = null
    private var isProfilePrivate = false
    private lateinit var noteGridAdapter: NoteGridAdapter

    // Which profile is being shown, and whether it's the logged-in user's own.
    private var viewedUid: String = ""
    private var isOwnProfile = true
    private var followState: FollowState = FollowState.NONE

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

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val argUid = arguments?.getString("uid")
        viewedUid = argUid ?: currentUid
        isOwnProfile = viewedUid == currentUid || argUid == null

        // Own profile: show the three-dot menu. Someone else's: show Follow instead.
        binding.buttonMore.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
        binding.buttonFollow.visibility = if (isOwnProfile) View.GONE else View.VISIBLE

        loadProfile()

        binding.buttonMore.setOnClickListener { showOptionsMenu() }
        binding.buttonFollow.setOnClickListener { onFollowClicked() }
        noteGridAdapter = NoteGridAdapter { note -> openNoteDetail(note) }
        binding.recyclerNotes.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerNotes.adapter = noteGridAdapter
    }

    private fun loadProfile() {
        if (viewedUid.isEmpty()) return

        FirebaseUtil.database.getReference("users").child(viewedUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    currentName = snapshot.child("displayName").getValue(String::class.java).orEmpty()
                    currentUsername = snapshot.child("username").getValue(String::class.java).orEmpty()
                    currentPronouns = snapshot.child("pronouns").getValue(String::class.java).orEmpty()
                    currentBio = snapshot.child("bio").getValue(String::class.java).orEmpty()
                    currentStyle = snapshot.child("notebookStyle").getValue(String::class.java) ?: "classic"
                    currentCoverColor = snapshot.child("coverColor").getValue(String::class.java)
                    currentAvatarBase64 = snapshot.child("avatarBase64").getValue(String::class.java)
                    isProfilePrivate = snapshot.child("isPrivate").getValue(Boolean::class.java) ?: false

                    binding.textUsername.text = "@$currentUsername"
                    binding.textName.text = currentName
                    binding.textPronouns.text = currentPronouns
                    binding.textBio.text = currentBio

                    if (!currentAvatarBase64.isNullOrEmpty()) {
                        setAvatarFromBase64(currentAvatarBase64!!)
                    }

                    applyCoverStyle(currentStyle, currentCoverColor)

                    if (isOwnProfile) {
                        // You can always see your own notes, private or not.
                        showNotesSection(locked = false)
                    } else {
                        resolveFollowState()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Silently ignore for now, or show a toast/snackbar later
                }
            })
    }

    private fun resolveFollowState() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseUtil.database.getReference("following").child(currentUid).child(viewedUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(followingSnapshot: DataSnapshot) {
                    if (_binding == null) return

                    if (followingSnapshot.exists()) {
                        followState = FollowState.FOLLOWING
                        updateFollowButton()
                        showNotesSection(locked = false)
                        return
                    }

                    FirebaseUtil.database.getReference("followRequests").child(viewedUid).child(currentUid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(requestSnapshot: DataSnapshot) {
                                if (_binding == null) return
                                followState =
                                    if (requestSnapshot.exists()) FollowState.REQUESTED else FollowState.NONE
                                updateFollowButton()
                                // Private + not following yet -> lock the notes section.
                                showNotesSection(locked = isProfilePrivate)
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateFollowButton() {
        if (_binding == null) return
        binding.buttonFollow.text = when (followState) {
            FollowState.NONE -> if (isProfilePrivate) "Request" else "Follow"
            FollowState.REQUESTED -> "Requested"
            FollowState.FOLLOWING -> "Following"
        }
    }

    private fun onFollowClicked() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        when (followState) {
            FollowState.NONE -> {
                if (isProfilePrivate) {
                    FirebaseUtil.database.getReference("followRequests").child(viewedUid).child(currentUid)
                        .setValue(System.currentTimeMillis())
                        .addOnSuccessListener {
                            followState = FollowState.REQUESTED
                            updateFollowButton()
                        }
                } else {
                    val updates = mapOf(
                        "/following/$currentUid/$viewedUid" to System.currentTimeMillis(),
                        "/followers/$viewedUid/$currentUid" to System.currentTimeMillis()
                    )
                    FirebaseUtil.database.reference.updateChildren(updates)
                        .addOnSuccessListener {
                            followState = FollowState.FOLLOWING
                            updateFollowButton()
                            showNotesSection(locked = false)
                        }
                }
            }

            FollowState.REQUESTED -> {
                FirebaseUtil.database.getReference("followRequests").child(viewedUid).child(currentUid)
                    .removeValue()
                    .addOnSuccessListener {
                        followState = FollowState.NONE
                        updateFollowButton()
                    }
            }

            FollowState.FOLLOWING -> {
                val updates = mapOf(
                    "/following/$currentUid/$viewedUid" to null,
                    "/followers/$viewedUid/$currentUid" to null
                )
                FirebaseUtil.database.reference.updateChildren(updates)
                    .addOnSuccessListener {
                        followState = FollowState.NONE
                        updateFollowButton()
                        showNotesSection(locked = isProfilePrivate)
                    }
            }
        }
    }

    private fun showNotesSection(locked: Boolean) {
        if (_binding == null) return

        if (locked) {
            binding.textNotesPlaceholder.text = "This profile is private"
            binding.textNotesPlaceholder.visibility = View.VISIBLE
            binding.recyclerNotes.visibility = View.GONE
            return
        }

        loadNotes()
    }

    private fun loadNotes() {
        if (viewedUid.isEmpty() || _binding == null) return

        FirebaseUtil.database.getReference("userNotes").child(viewedUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    val notes = snapshot.children.mapNotNull { it.getValue(Note::class.java) }
                        .sortedByDescending { it.createdAt }

                    if (notes.isEmpty()) {
                        binding.textNotesPlaceholder.text = "No notes uploaded by user"
                        binding.textNotesPlaceholder.visibility = View.VISIBLE
                        binding.recyclerNotes.visibility = View.GONE
                    } else {
                        binding.textNotesPlaceholder.visibility = View.GONE
                        binding.recyclerNotes.visibility = View.VISIBLE
                        noteGridAdapter.submitList(notes)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (_binding == null) return
                    binding.textNotesPlaceholder.text = "Couldn't load notes"
                    binding.textNotesPlaceholder.visibility = View.VISIBLE
                    binding.recyclerNotes.visibility = View.GONE
                }
            })
    }

    private fun openNoteDetail(note: Note) {
        val bundle = Bundle().apply {
            putString("noteId", note.id)
            putString("authorUid", note.authorUid)
        }
        findNavController().navigate(R.id.action_profile_to_noteDetail, bundle)
    }

    private fun setAvatarFromBase64(base64: String) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.imageAvatar.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // Leave placeholder if decoding fails
        }
    }

    private fun applyCoverStyle(styleId: String, coverColor: String?) {
        val colorInt = if (!coverColor.isNullOrEmpty()) {
            try {
                Color.parseColor(coverColor)
            } catch (e: IllegalArgumentException) {
                defaultColorForStyle(styleId)
            }
        } else {
            defaultColorForStyle(styleId)
        }
        binding.cardCover.setCardBackgroundColor(colorInt)

        val patternRes = when (styleId) {
            "grid" -> R.drawable.ic_pattern_grid
            "kraft" -> R.drawable.ic_pattern_kraft
            "dotted" -> R.drawable.ic_pattern_dotted
            else -> R.drawable.ic_pattern_classic
        }
        binding.imageCoverPattern.setImageResource(patternRes)
    }

    private fun defaultColorForStyle(styleId: String): Int {
        val colorRes = when (styleId) {
            "grid" -> R.color.notebook_style_grid
            "kraft" -> R.color.notebook_style_kraft
            "dotted" -> R.color.notebook_style_dotted
            else -> R.color.notebook_style_classic
        }
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    // Everything below is unchanged, and only ever runs for isOwnProfile == true
    // since button_more is hidden for other people's profiles.

    private fun showOptionsMenu() {
        val popup = PopupMenu(requireContext(), binding.buttonMore)
        popup.menuInflater.inflate(R.menu.profile_options_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit_profile -> {
                    openEditProfile()
                    true
                }
                R.id.action_follow_requests -> {
                    FollowRequestsDialogFragment().show(parentFragmentManager, "FollowRequestsDialog")
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
            currentStyle = currentStyle,
            currentCoverColor = currentCoverColor,
            currentAvatarBase64 = currentAvatarBase64,
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