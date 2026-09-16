package com.example.notebook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notebook.databinding.ItemNotebookStyleSwatchBinding

/**
 * One notebook style option shown in the picker — a name plus an icon/drawable
 * used as its preview.
 */
data class NotebookStyleOption(
    val id: String,
    val label: String,
    val previewIconRes: Int
)

/**
 * Adapter for the horizontal "Pick a page style" RecyclerView on the
 * profile setup screen. Renders one item_notebook_style_swatch.xml card
 * per style, highlights the selected one, and reports taps back via
 * onStyleSelected.
 */
class NotebookStyleAdapter(
    private val styles: List<NotebookStyleOption>,
    private val onStyleSelected: (NotebookStyleOption) -> Unit
) : RecyclerView.Adapter<NotebookStyleAdapter.StyleViewHolder>() {

    private var selectedPosition = 0

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

    /** Which style is currently highlighted, if you need it from outside. */
    fun getSelectedStyle(): NotebookStyleOption = styles[selectedPosition]
}