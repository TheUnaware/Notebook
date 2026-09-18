package com.example.notebook

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notebook.databinding.ItemFollowRequestBinding

data class FollowRequestUser(
    val uid: String,
    val username: String,
    val displayName: String,
    val avatarBase64: String?
)

class FollowRequestAdapter(
    private val onAccept: (FollowRequestUser, Int) -> Unit,
    private val onDecline: (FollowRequestUser, Int) -> Unit
) : RecyclerView.Adapter<FollowRequestAdapter.RequestViewHolder>() {

    private val requests = mutableListOf<FollowRequestUser>()

    fun submitList(newRequests: List<FollowRequestUser>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    fun removeAt(position: Int) {
        if (position in requests.indices) {
            requests.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class RequestViewHolder(val binding: ItemFollowRequestBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemFollowRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val user = requests[position]

        holder.binding.textRequestUsername.text = "@${user.username}"
        holder.binding.textRequestName.text = user.displayName

        if (!user.avatarBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(user.avatarBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.binding.imageRequestAvatar.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.binding.imageRequestAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }
        } else {
            holder.binding.imageRequestAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
        }

        holder.binding.buttonAccept.setOnClickListener {
            onAccept(user, holder.bindingAdapterPosition)
        }
        holder.binding.buttonDecline.setOnClickListener {
            onDecline(user, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = requests.size
}