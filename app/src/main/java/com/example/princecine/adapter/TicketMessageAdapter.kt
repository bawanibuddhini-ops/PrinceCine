package com.example.princecine.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.princecine.R
import com.example.princecine.model.TicketMessage
import com.example.princecine.model.SenderType
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.*

class TicketMessageAdapter(
    private val messages: List<TicketMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ADMIN = 2
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderType == SenderType.USER) {
            VIEW_TYPE_USER
        } else {
            VIEW_TYPE_ADMIN
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_admin, parent, false)
            AdminMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AdminMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: MaterialTextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: MaterialTextView = itemView.findViewById(R.id.tvTimestamp)
        
        fun bind(message: TicketMessage) {
            tvMessage.text = message.message
            tvTimestamp.text = message.timestamp?.toDate()?.let { date ->
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
            } ?: ""
        }
    }

    class AdminMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: MaterialTextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: MaterialTextView = itemView.findViewById(R.id.tvTimestamp)
        
        fun bind(message: TicketMessage) {
            tvMessage.text = message.message
            tvTimestamp.text = message.timestamp?.toDate()?.let { date ->
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
            } ?: ""
        }
    }
}
