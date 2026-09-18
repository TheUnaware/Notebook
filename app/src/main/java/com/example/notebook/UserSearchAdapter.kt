package com.example.notebook

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notebook.databinding.ItemUserSearchResultBinding

class UserSearchAdapter(
    private val onFollowClick: (UserSearchResult, Int) -> Unit,
    private val onUserClick: (UserSearchResult) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.ResultViewHolder>() {

    private val results = mutableListOf<UserSearchResult>()

    fun submitList(newResults: List<UserSearchResult>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }

    fun updateItem(position: Int, updated: UserSearchResult) {
        if (position in results.indices) {
            results[position] = updated
            notifyItemChanged(position)
        }
    }

    inner class ResultViewHolder(val binding: ItemUserSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemUserSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val user = results[position]

        holder.binding.textResultUsername.text = "@${user.username}"
        holder.binding.textResultName.text = user.displayName

        if (!user.avatarBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(user.avatarBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.binding.imageResultAvatar.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.binding.imageResultAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }
        } else {
            holder.binding.imageResultAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
        }

        holder.binding.buttonFollow.text = when (user.followState) {
            FollowState.NONE -> if (user.isPrivate) "Request" else "Follow"
            FollowState.REQUESTED -> "Requested"
            FollowState.FOLLOWING -> "Following"
        }

        holder.binding.buttonFollow.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onFollowClick(results[pos], pos)
            }
        }

        // This is the piece that was missing: nothing was listening for taps
        // on the row itself, only on the follow button.
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onUserClick(results[pos])
            }
        }
    }

    override fun getItemCount(): Int = results.size
}