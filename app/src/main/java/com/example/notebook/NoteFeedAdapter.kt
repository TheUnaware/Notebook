package com.example.notebook

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notebook.databinding.ItemNoteFeedCardBinding

data class FeedNote(
    val note: Note,
    val authorUsername: String,
    val authorAvatarBase64: String?
)

class NoteFeedAdapter(
    private val onNoteClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteFeedAdapter.FeedViewHolder>() {

    private val items = mutableListOf<FeedNote>()

    fun submitList(newItems: List<FeedNote>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class FeedViewHolder(val binding: ItemNoteFeedCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemNoteFeedCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = items[position]
        val note = item.note

        if (note.anonymous) {
            holder.binding.textCardUsername.text = "Anonymous"
            holder.binding.imageCardAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
        } else {
            holder.binding.textCardUsername.text = "@${item.authorUsername}"
            if (!item.authorAvatarBase64.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(item.authorAvatarBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.binding.imageCardAvatar.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    holder.binding.imageCardAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
                }
            } else {
                holder.binding.imageCardAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }
        }

        holder.binding.textCardDate.text = note.dateDisplay
        holder.binding.textCardContent.text = note.content

        try {
            holder.binding.root.setCardBackgroundColor(Color.parseColor(note.colorHex))
        } catch (e: IllegalArgumentException) { /* keep default */ }

        val patternRes = when (note.template) {
            "grid" -> R.drawable.ic_pattern_grid
            "kraft" -> R.drawable.ic_pattern_kraft
            "dotted" -> R.drawable.ic_pattern_dotted
            else -> R.drawable.ic_pattern_classic
        }
        holder.binding.imageCardPattern.setImageResource(patternRes)

        holder.itemView.setOnClickListener { onNoteClick(note) }
    }

    override fun getItemCount(): Int = items.size
}