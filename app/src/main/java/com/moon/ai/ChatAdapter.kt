package com.moon.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_MODEL = 1

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
                oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).role == Role.USER) TYPE_USER else TYPE_MODEL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserViewHolder(inflater.inflate(R.layout.item_message_user, parent, false))
        } else {
            ModelViewHolder(inflater.inflate(R.layout.item_message_model, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is UserViewHolder) holder.bind(message) else (holder as ModelViewHolder).bind(message)
    }

    private class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.messageText)
        fun bind(message: ChatMessage) {
            textView.text = message.text
        }
    }

    private class ModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.messageText)
        fun bind(message: ChatMessage) {
            textView.text = if (message.streaming && message.text.isEmpty()) {
                itemView.context.getString(R.string.thinking)
            } else {
                message.text
            }
        }
    }
}
