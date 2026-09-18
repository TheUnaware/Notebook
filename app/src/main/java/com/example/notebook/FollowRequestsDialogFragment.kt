package com.example.notebook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notebook.databinding.DialogFollowRequestsBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class FollowRequestsDialogFragment : DialogFragment() {

    private var _binding: DialogFollowRequestsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FollowRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFollowRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FollowRequestAdapter(
            onAccept = { user, position -> acceptRequest(user, position) },
            onDecline = { user, position -> declineRequest(user, position) }
        )
        binding.recyclerRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRequests.adapter = adapter

        loadRequests()
    }

    private fun loadRequests() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseUtil.database.getReference("followRequests").child(currentUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    if (!snapshot.exists()) {
                        binding.textRequestsEmpty.visibility = View.VISIBLE
                        return
                    }

                    val requesterUids = snapshot.children.mapNotNull { it.key }
                    if (requesterUids.isEmpty()) {
                        binding.textRequestsEmpty.visibility = View.VISIBLE
                        return
                    }

                    fetchRequesterProfiles(requesterUids)
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.textRequestsEmpty.visibility = View.VISIBLE
                }
            })
    }

    private fun fetchRequesterProfiles(uids: List<String>) {
        val results = mutableListOf<FollowRequestUser>()
        var remaining = uids.size

        uids.forEach { uid ->
            FirebaseUtil.database.getReference("users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val username = snapshot.child("username").getValue(String::class.java).orEmpty()
                        val displayName = snapshot.child("displayName").getValue(String::class.java).orEmpty()
                        val avatarBase64 = snapshot.child("avatarBase64").getValue(String::class.java)

                        results.add(FollowRequestUser(uid, username, displayName, avatarBase64))
                        remaining--
                        if (remaining == 0 && _binding != null) {
                            if (results.isEmpty()) {
                                binding.textRequestsEmpty.visibility = View.VISIBLE
                            } else {
                                adapter.submitList(results)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        remaining--
                        if (remaining == 0 && _binding != null && results.isNotEmpty()) {
                            adapter.submitList(results)
                        }
                    }
                })
        }
    }

    private fun acceptRequest(user: FollowRequestUser, position: Int) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val updates = mapOf(
            "/followRequests/$currentUid/${user.uid}" to null,
            "/followers/$currentUid/${user.uid}" to System.currentTimeMillis(),
            "/following/${user.uid}/$currentUid" to System.currentTimeMillis()
        )

        FirebaseUtil.database.reference.updateChildren(updates)
            .addOnSuccessListener {
                if (_binding != null) adapter.removeAt(position)
            }
    }

    private fun declineRequest(user: FollowRequestUser, position: Int) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseUtil.database.getReference("followRequests").child(currentUid).child(user.uid)
            .removeValue()
            .addOnSuccessListener {
                if (_binding != null) adapter.removeAt(position)
            }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}