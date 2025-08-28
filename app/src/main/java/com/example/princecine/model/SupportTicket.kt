package com.example.princecine.model

import com.google.firebase.Timestamp

data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val category: SupportCategory = SupportCategory.GENERAL,
    var status: TicketStatus = TicketStatus.PENDING,
    val priority: Priority = Priority.MEDIUM,
    val ticketId: String = "", // Human-readable ticket ID like ST001234
    val attachmentUrls: List<String> = emptyList(),
    val adminNotes: String = "",
    val resolution: String = "",
    val messages: List<TicketMessage> = emptyList(), // New: Message thread
    val dateRaised: Timestamp? = null, // renamed from createdAt for compatibility
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

enum class SupportCategory {
    BOOKING,
    PAYMENT,
    TECHNICAL,
    ACCOUNT,
    GENERAL
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
