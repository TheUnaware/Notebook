package com.example.notebook

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notebook.databinding.ItemNoteGridCellBinding

class NoteGridAdapter(
    private val onNoteClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteGridAdapter.NoteViewHolder>() {

    private val notes = mutableListOf<Note>()

    fun submitList(newNotes: List<Note>) {
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }

    inner class NoteViewHolder(val binding: ItemNoteGridCellBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteGridCellBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        holder.binding.textCellPreview.text = note.content
        holder.binding.textCellDate.text = note.dateDisplay

        try {
            holder.binding.root.setCardBackgroundColor(Color.parseColor(note.colorHex))
        } catch (e: IllegalArgumentException) { /* keep default */ }

        val patternRes = when (note.template) {
            "grid" -> R.drawable.ic_pattern_grid
            "kraft" -> R.drawable.ic_pattern_kraft
            "dotted" -> R.drawable.ic_pattern_dotted
            else -> R.drawable.ic_pattern_classic
        }
        holder.binding.imageCellPattern.setImageResource(patternRes)

        holder.itemView.setOnClickListener { onNoteClick(note) }
    }

    override fun getItemCount(): Int = notes.size
}