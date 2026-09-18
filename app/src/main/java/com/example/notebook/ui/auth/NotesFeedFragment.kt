package com.example.notebook.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notebook.DailyNote
import com.example.notebook.FeedNote
import com.example.notebook.Note
import com.example.notebook.NoteFeedAdapter
import com.example.notebook.R
import com.example.notebook.StoryAdapter
import com.example.notebook.StoryAuthor
import com.example.notebook.databinding.FragmentNotesFeedBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class NotesFeedFragment : Fragment() {

    private var _binding: FragmentNotesFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var feedAdapter: NoteFeedAdapter
    private lateinit var storyAdapter: StoryAdapter

    private var currentStoryAuthors: List<StoryAuthor> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedAdapter = NoteFeedAdapter { note -> openNoteDetail(note) }
        binding.recyclerHomeFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHomeFeed.adapter = feedAdapter

        storyAdapter = StoryAdapter { position -> openStoryViewer(position) }
        binding.recyclerStories.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerStories.adapter = storyAdapter

        loadFollowingFeed()
    }

    private fun openNoteDetail(note: Note) {
        val bundle = Bundle().apply { putString("noteId", note.id) }
        findNavController().navigate(R.id.action_notesFeed_to_noteDetail, bundle)
    }

    private fun openStoryViewer(startIndex: Int) {
        val uidsInOrder = currentStoryAuthors.map { it.uid }.toTypedArray()
        val bundle = Bundle().apply {
            putStringArray("authorUids", uidsInOrder)
            putInt("startIndex", startIndex)
        }
        findNavController().navigate(R.id.action_notesFeed_to_storyViewer, bundle)
    }

    // --- Following-only main feed ---

    private fun loadFollowingFeed() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseUtil.database.getReference("following").child(currentUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    val followingUids = snapshot.children.mapNotNull { it.key }.toSet()

                    if (followingUids.isEmpty()) {
                        binding.textFeedEmpty.visibility = View.VISIBLE
                        loadDailyNotes(emptySet())
                        return
                    }

                    loadNotesForUids(followingUids)
                    loadDailyNotes(followingUids)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadNotesForUids(followingUids: Set<String>) {
        FirebaseUtil.database.getReference("notes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    val notes = snapshot.children
                        .mapNotNull { it.getValue(Note::class.java) }
                        .filter { !it.anonymous && it.authorUid in followingUids }
                        .sortedByDescending { it.createdAt }

                    if (notes.isEmpty()) {
                        binding.textFeedEmpty.visibility = View.VISIBLE
                        return
                    }

                    resolveAuthorsForNotes(notes)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun resolveAuthorsForNotes(notes: List<Note>) {
        val authorUids = notes.map { it.authorUid }.distinct()
        val usernameMap = mutableMapOf<String, String>()
        val avatarMap = mutableMapOf<String, String?>()
        var remaining = authorUids.size

        authorUids.forEach { uid ->
            FirebaseUtil.database.getReference("users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        usernameMap[uid] = userSnapshot.child("username").getValue(String::class.java).orEmpty()
                        avatarMap[uid] = userSnapshot.child("avatarBase64").getValue(String::class.java)

                        remaining--
                        if (remaining == 0 && _binding != null) {
                            val feedNotes = notes.map {
                                FeedNote(
                                    note = it,
                                    authorUsername = usernameMap[it.authorUid].orEmpty(),
                                    authorAvatarBase64 = avatarMap[it.authorUid]
                                )
                            }
                            binding.textFeedEmpty.visibility = View.GONE
                            feedAdapter.submitList(feedNotes)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        remaining--
                        if (remaining == 0 && _binding != null) {
                            val feedNotes = notes.map {
                                FeedNote(
                                    note = it,
                                    authorUsername = usernameMap[it.authorUid].orEmpty(),
                                    authorAvatarBase64 = avatarMap[it.authorUid]
                                )
                            }
                            feedAdapter.submitList(feedNotes)
                        }
                    }
                })
        }
    }

    // --- Stories row: active (non-expired) daily notes from followed users ---

    private fun loadDailyNotes(followingUids: Set<String>) {
        if (followingUids.isEmpty()) {
            storyAdapter.submitList(emptyList())
            return
        }

        FirebaseUtil.database.getReference("dailyNotes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    val now = System.currentTimeMillis()
                    val activeDaily = snapshot.children
                        .mapNotNull { it.getValue(DailyNote::class.java) }
                        .filter { it.authorUid in followingUids && it.expiresAt > now }

                    if (activeDaily.isEmpty()) {
                        storyAdapter.submitList(emptyList())
                        return
                    }

                    val grouped = activeDaily.groupBy { it.authorUid }
                    resolveStoryAuthors(grouped)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun resolveStoryAuthors(grouped: Map<String, List<DailyNote>>) {
        val authorUids = grouped.keys.toList()
        var remaining = authorUids.size
        val results = mutableListOf<StoryAuthor>()

        authorUids.forEach { uid ->
            FirebaseUtil.database.getReference("users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        val username = userSnapshot.child("username").getValue(String::class.java).orEmpty()
                        val avatar = userSnapshot.child("avatarBase64").getValue(String::class.java)
                        val notesForAuthor = grouped[uid].orEmpty().sortedBy { it.createdAt }

                        results.add(
                            StoryAuthor(
                                uid = uid,
                                username = username,
                                avatarBase64 = avatar,
                                dailyNotes = notesForAuthor
                            )
                        )

                        remaining--
                        if (remaining == 0 && _binding != null) {
                            currentStoryAuthors = results.sortedByDescending { it.dailyNotes.maxOf { n -> n.createdAt } }
                            storyAdapter.submitList(currentStoryAuthors)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        remaining--
                        if (remaining == 0 && _binding != null) {
                            currentStoryAuthors = results
                            storyAdapter.submitList(currentStoryAuthors)
                        }
                    }
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}