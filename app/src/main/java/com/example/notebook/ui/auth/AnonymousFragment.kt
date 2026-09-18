package com.example.notebook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notebook.databinding.FragmentAnonymousBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class AnonymousFragment : Fragment() {

    private var _binding: FragmentAnonymousBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AnonymousNoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnonymousBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AnonymousNoteAdapter { note -> openNoteDetail(note) }
        binding.recyclerAnonymous.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAnonymous.adapter = adapter

        loadAnonymousNotes()
    }

    private fun openNoteDetail(note: Note) {
        val bundle = Bundle().apply { putString("noteId", note.id) }
        findNavController().navigate(R.id.action_search_to_noteDetail, bundle)
    }

    private fun loadAnonymousNotes() {
        FirebaseUtil.database.getReference("notes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    val anonymousNotes = snapshot.children
                        .mapNotNull { it.getValue(Note::class.java) }
                        .filter { it.anonymous }
                        .sortedByDescending { it.createdAt }
                    // authorUid is intentionally never read past this point

                    if (anonymousNotes.isEmpty()) {
                        binding.textAnonEmpty.visibility = View.VISIBLE
                    } else {
                        binding.textAnonEmpty.visibility = View.GONE
                        adapter.submitList(anonymousNotes)
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