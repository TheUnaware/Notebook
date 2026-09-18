package com.example.notebook

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notebook.databinding.FragmentSearchBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var resultsAdapter: UserSearchAdapter
    private lateinit var exploreAdapter: ExplorePlaceholderAdapter

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resultsAdapter = UserSearchAdapter(
            onFollowClick = { user, position -> onFollowClicked(user, position) },
            onUserClick = { user -> openProfile(user) }
        )
        binding.recyclerUserResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUserResults.adapter = resultsAdapter

        exploreAdapter = ExplorePlaceholderAdapter()
        binding.recyclerExploreGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerExploreGrid.adapter = exploreAdapter

        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()

                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                if (query.isEmpty()) {
                    showExploreView()
                    return
                }

                val runnable = Runnable { performSearch(query) }
                searchRunnable = runnable
                searchHandler.postDelayed(runnable, 350) // debounce
            }
        })
    }

    private fun openProfile(user: UserSearchResult) {
        val bundle = Bundle().apply { putString("uid", user.uid) }
        findNavController().navigate(R.id.action_search_to_profile, bundle)
    }

    private fun showExploreView() {
        binding.recyclerExploreGrid.visibility = View.VISIBLE
        binding.recyclerUserResults.visibility = View.GONE
        binding.textNoResults.visibility = View.GONE
    }

    private fun showResultsView() {
        binding.recyclerExploreGrid.visibility = View.GONE
        binding.recyclerUserResults.visibility = View.VISIBLE
    }

    private fun performSearch(query: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val queryLower = query.lowercase(Locale.getDefault())

        FirebaseUtil.database.getReference("users")
            .orderByChild("usernameLower")
            .startAt(queryLower)
            .endAt(queryLower + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    val matches = mutableListOf<UserSearchResult>()
                    for (child in snapshot.children) {
                        val uid = child.child("uid").getValue(String::class.java) ?: continue
                        if (uid == currentUid) continue // don't show yourself

                        val username = child.child("username").getValue(String::class.java).orEmpty()
                        val displayName = child.child("displayName").getValue(String::class.java).orEmpty()
                        val avatarBase64 = child.child("avatarBase64").getValue(String::class.java)
                        val isPrivate = child.child("isPrivate").getValue(Boolean::class.java) ?: false

                        matches.add(
                            UserSearchResult(
                                uid = uid,
                                username = username,
                                displayName = displayName,
                                avatarBase64 = avatarBase64,
                                isPrivate = isPrivate,
                                followState = FollowState.NONE
                            )
                        )
                    }

                    if (matches.isEmpty()) {
                        showResultsView()
                        resultsAdapter.submitList(emptyList())
                        binding.textNoResults.visibility = View.VISIBLE
                        return
                    }

                    binding.textNoResults.visibility = View.GONE
                    showResultsView()
                    resolveFollowStates(currentUid, matches)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Silently ignore for now, or show a toast/snackbar later
                }
            })
    }

    private fun resolveFollowStates(currentUid: String, matches: MutableList<UserSearchResult>) {
        var remaining = matches.size
        if (remaining == 0) {
            resultsAdapter.submitList(matches)
            return
        }

        matches.forEachIndexed { index, user ->
            FirebaseUtil.database.getReference("following").child(currentUid).child(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(followingSnapshot: DataSnapshot) {
                        if (followingSnapshot.exists()) {
                            matches[index] = user.copy(followState = FollowState.FOLLOWING)
                            remaining--
                            if (remaining == 0 && _binding != null) resultsAdapter.submitList(matches)
                            return
                        }

                        FirebaseUtil.database.getReference("followRequests").child(user.uid).child(currentUid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(requestSnapshot: DataSnapshot) {
                                    matches[index] = user.copy(
                                        followState = if (requestSnapshot.exists()) FollowState.REQUESTED else FollowState.NONE
                                    )
                                    remaining--
                                    if (remaining == 0 && _binding != null) resultsAdapter.submitList(matches)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    remaining--
                                    if (remaining == 0 && _binding != null) resultsAdapter.submitList(matches)
                                }
                            })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        remaining--
                        if (remaining == 0 && _binding != null) resultsAdapter.submitList(matches)
                    }
                })
        }
    }

    private fun onFollowClicked(user: UserSearchResult, position: Int) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        when (user.followState) {
            FollowState.NONE -> {
                if (user.isPrivate) {
                    FirebaseUtil.database.getReference("followRequests").child(user.uid).child(currentUid)
                        .setValue(System.currentTimeMillis())
                        .addOnSuccessListener {
                            resultsAdapter.updateItem(position, user.copy(followState = FollowState.REQUESTED))
                        }
                } else {
                    val updates = mapOf(
                        "/following/$currentUid/${user.uid}" to System.currentTimeMillis(),
                        "/followers/${user.uid}/$currentUid" to System.currentTimeMillis()
                    )
                    FirebaseUtil.database.reference.updateChildren(updates)
                        .addOnSuccessListener {
                            resultsAdapter.updateItem(position, user.copy(followState = FollowState.FOLLOWING))
                        }
                }
            }

            FollowState.REQUESTED -> {
                FirebaseUtil.database.getReference("followRequests").child(user.uid).child(currentUid)
                    .removeValue()
                    .addOnSuccessListener {
                        resultsAdapter.updateItem(position, user.copy(followState = FollowState.NONE))
                    }
            }

            FollowState.FOLLOWING -> {
                val updates = mapOf(
                    "/following/$currentUid/${user.uid}" to null,
                    "/followers/${user.uid}/$currentUid" to null
                )
                FirebaseUtil.database.reference.updateChildren(updates)
                    .addOnSuccessListener {
                        resultsAdapter.updateItem(position, user.copy(followState = FollowState.NONE))
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }
}