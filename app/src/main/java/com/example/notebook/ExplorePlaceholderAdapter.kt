package com.example.notebook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notebook.databinding.ItemNotePlaceholderBinding

class ExplorePlaceholderAdapter(private val count: Int = 12) :
    RecyclerView.Adapter<ExplorePlaceholderAdapter.PlaceholderViewHolder>() {

    inner class PlaceholderViewHolder(binding: ItemNotePlaceholderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceholderViewHolder {
        val binding = ItemNotePlaceholderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlaceholderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceholderViewHolder, position: Int) {
        // Nothing to bind yet — swap this out once notes exist
    }

    override fun getItemCount(): Int = count
}