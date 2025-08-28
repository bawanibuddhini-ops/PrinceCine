package com.example.princecine.model

import com.google.firebase.Timestamp

data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    var status: TicketStatus = TicketStatus.PENDING,
    val ticketId: String = "", // Human-readable ticket ID like ST001234
    val messages: List<TicketMessage> = emptyList(), // Message thread
    val dateRaised: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null
)

data class TicketMessage(
    val id: String = "",
    val message: String = "",
    val senderType: SenderType = SenderType.USER, // USER or ADMIN
    val senderName: String = "",
    val senderId: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false
)

enum class SenderType {
    USER,
    ADMIN
}

enum class TicketStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
