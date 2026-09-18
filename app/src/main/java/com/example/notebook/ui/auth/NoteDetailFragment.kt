package com.example.notebook

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notebook.databinding.FragmentNoteDetailBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        val noteId = arguments?.getString("noteId").orEmpty()
        if (noteId.isEmpty()) {
            findNavController().popBackStack()
            return
        }

        loadNote(noteId)
    }

    private fun loadNote(noteId: String) {
        FirebaseUtil.database.getReference("notes").child(noteId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    val note = snapshot.getValue(Note::class.java)
                    if (note == null) {
                        findNavController().popBackStack()
                        return
                    }

                    binding.textNoteDate.text = note.dateDisplay
                    binding.textNoteContent.text = note.content

                    try {
                        binding.cardNotePage.setCardBackgroundColor(Color.parseColor(note.colorHex))
                    } catch (e: IllegalArgumentException) { /* ignore */ }

                    val patternRes = when (note.template) {
                        "grid" -> R.drawable.ic_pattern_grid
                        "kraft" -> R.drawable.ic_pattern_kraft
                        "dotted" -> R.drawable.ic_pattern_dotted
                        else -> R.drawable.ic_pattern_classic
                    }
                    binding.imagePagePattern.setImageResource(patternRes)

                    if (note.anonymous) {
                        binding.containerAuthor.visibility = View.GONE
                    } else {
                        binding.containerAuthor.visibility = View.VISIBLE
                        loadAuthor(note.authorUid)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    findNavController().popBackStack()
                }
            })
    }

    private fun loadAuthor(authorUid: String) {
        FirebaseUtil.database.getReference("users").child(authorUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    val username = snapshot.child("username").getValue(String::class.java).orEmpty()
                    binding.textAuthorUsername.text = "@$username"

                    val avatarBase64 = snapshot.child("avatarBase64").getValue(String::class.java)
                    if (!avatarBase64.isNullOrEmpty()) {
                        try {
                            val bytes = Base64.decode(avatarBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            binding.imageAuthorAvatar.setImageBitmap(bitmap)
                        } catch (e: Exception) { /* keep placeholder */ }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}