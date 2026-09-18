package com.example.notebook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notebook.databinding.ItemNotebookStyleSwatchBinding

data class NotebookStyleOption(
    val id: String,
    val label: String,
    val previewIconRes: Int
)

class NotebookStyleAdapter(
    private val styles: List<NotebookStyleOption>,
    initialSelectedId: String? = null,
    private val onStyleSelected: (NotebookStyleOption) -> Unit
) : RecyclerView.Adapter<NotebookStyleAdapter.StyleViewHolder>() {

    private var selectedPosition: Int =
        styles.indexOfFirst { it.id == initialSelectedId }.let { if (it >= 0) it else 0 }

    inner class StyleViewHolder(val binding: ItemNotebookStyleSwatchBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        val binding = ItemNotebookStyleSwatchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StyleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        val style = styles[position]

        holder.binding.textSwatchLabel.text = style.label
        holder.binding.imageSwatchPreview.setImageResource(style.previewIconRes)

        val isSelected = position == selectedPosition
        holder.binding.cardSwatch.strokeColor = holder.itemView.context.getColor(
            if (isSelected) R.color.notebook_coral else R.color.notebook_line
        )
        holder.binding.cardSwatch.strokeWidth = if (isSelected) 3 else 1

        holder.itemView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previousPosition = selectedPosition
            selectedPosition = currentPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onStyleSelected(styles[currentPosition])
        }
    }

    override fun getItemCount(): Int = styles.size

    fun getSelectedStyle(): NotebookStyleOption = styles[selectedPosition]
}