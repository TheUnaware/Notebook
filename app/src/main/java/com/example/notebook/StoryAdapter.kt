package com.example.notebook

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notebook.databinding.ItemStoryAvatarBinding

data class StoryAuthor(
    val uid: String,
    val username: String,
    val avatarBase64: String?,
    val dailyNotes: List<DailyNote>
)

class StoryAdapter(
    private val onAuthorClick: (position: Int) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private val items = mutableListOf<StoryAuthor>()

    fun submitList(newItems: List<StoryAuthor>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class StoryViewHolder(val binding: ItemStoryAvatarBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryAvatarBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textStoryUsername.text = item.username

        if (!item.avatarBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(item.avatarBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.binding.imageStoryAvatar.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.binding.imageStoryAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }
        } else {
            holder.binding.imageStoryAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
        }

        holder.itemView.setOnClickListener { onAuthorClick(position) }
    }

    override fun getItemCount(): Int = items.size
}