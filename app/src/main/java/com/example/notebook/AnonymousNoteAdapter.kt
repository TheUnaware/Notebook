package com.example.notebook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notebook.databinding.ItemAnonymousNoteCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnonymousNoteAdapter(
    private val onNoteClick: (Note) -> Unit
) : ListAdapter<Note, AnonymousNoteAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnonymousNoteCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAnonymousNoteCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.textCardContent.text = note.content

            val dateText = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(note.createdAt))
            binding.textCardDate.text = dateText

            val patternRes = when (note.template) {
                "grid" -> R.drawable.ic_pattern_grid
                "kraft" -> R.drawable.ic_pattern_kraft
                "dotted" -> R.drawable.ic_pattern_dotted
                else -> R.drawable.ic_pattern_classic
            }
            binding.imageCardPattern.setImageResource(patternRes)

            binding.root.setOnClickListener { onNoteClick(note) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }
}