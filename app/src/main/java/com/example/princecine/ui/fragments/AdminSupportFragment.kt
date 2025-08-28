package com.example.princecine.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.example.princecine.R
import com.example.princecine.adapter.SupportTicketAdapter
import com.example.princecine.adapter.TicketMessageAdapter
import com.example.princecine.data.FirebaseRepository
import com.example.princecine.model.SupportTicket
import com.example.princecine.model.TicketStatus
import com.example.princecine.model.TicketMessage
import com.example.princecine.model.SenderType
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminSupportFragment : Fragment() {
    
    private lateinit var chipAll: Chip
    private lateinit var chipPending: Chip
    private lateinit var chipResolved: Chip
    private lateinit var rvTickets: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var ticketAdapter: SupportTicketAdapter
    
    private val allTickets = mutableListOf<SupportTicket>()
    private lateinit var repository: FirebaseRepository
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_support, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        repository = FirebaseRepository()
        
        initializeViews(view)
        setupRecyclerView()
        setupCapsuleClickListeners()
        loadTicketsFromFirebase()
        updateUI()
    }
    
    private fun initializeViews(view: View) {
        chipAll = view.findViewById(R.id.chipAll)
        chipPending = view.findViewById(R.id.chipPending)
        chipResolved = view.findViewById(R.id.chipResolved)
        rvTickets = view.findViewById(R.id.rvTickets)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        
        // Hide FAB for admin users
        val fabNewTicket = view.findViewById<View>(R.id.fabNewTicket)
        fabNewTicket.visibility = View.GONE
    }
    
    private fun setupRecyclerView() {
        ticketAdapter = SupportTicketAdapter(
            tickets = allTickets,
            onTicketClick = { ticket ->
                // Handle ticket click - show ticket details for admin
                showTicketDetailsDialog(ticket)
            },
            isAdmin = true,
            onSolveClick = { ticket ->
                showSolveTicketDialog(ticket)
            }
        )
        
        rvTickets.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ticketAdapter
        }
    }
    
    private fun setupCapsuleClickListeners() {
        chipAll.setOnClickListener { selectCapsule(chipAll, null) }
        chipPending.setOnClickListener { selectCapsule(chipPending, TicketStatus.PENDING) }
        chipResolved.setOnClickListener { selectCapsule(chipResolved, TicketStatus.RESOLVED) }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload tickets when fragment becomes visible again
        loadTicketsFromFirebase()
    }
    
    private fun selectCapsule(selectedChip: Chip, status: TicketStatus?) {
        // Reset all capsules to unselected state
        val allChips = listOf(chipAll, chipPending, chipResolved)
        
        allChips.forEach { chip ->
            chip.setChipBackgroundColorResource(R.color.white)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        }
        
        // Set selected capsule to selected state
        selectedChip.setChipBackgroundColorResource(R.color.red)
        selectedChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        
        // Filter tickets based on selected status
        ticketAdapter.filterTickets(status)
        updateUI()
    }
    
    private fun loadTicketsFromFirebase() {
        lifecycleScope.launch {
            try {
                val result = repository.getAllSupportTickets()
                result.onSuccess { tickets ->
                    allTickets.clear()
                    allTickets.addAll(tickets)
                    ticketAdapter.updateTickets(allTickets)
                    updateUI()
                    
                    if (tickets.isEmpty()) {
                        Toast.makeText(requireContext(), "No support tickets available.", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Failed to load tickets: ${error.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading tickets: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateUI() {
        if (ticketAdapter.itemCount == 0) {
            rvTickets.visibility = View.GONE
            llEmptyState.visibility = View.VISIBLE
        } else {
            rvTickets.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
        }
    }

    private fun showTicketDetailsDialog(ticket: SupportTicket) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ticket_messages, null)
        
        val tvTicketTitle = dialogView.findViewById<MaterialTextView>(R.id.tvTicketTitle)
        val tvTicketStatus = dialogView.findViewById<MaterialTextView>(R.id.tvTicketStatus)
        val rvMessages = dialogView.findViewById<RecyclerView>(R.id.rvMessages)
        val tilNewMessage = dialogView.findViewById<TextInputLayout>(R.id.tilNewMessage)
        val etNewMessage = dialogView.findViewById<TextInputEditText>(R.id.etNewMessage)
        val btnSendMessage = dialogView.findViewById<MaterialButton>(R.id.btnSendMessage)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnClose)
        val btnResolve = dialogView.findViewById<MaterialButton>(R.id.btnResolve)

        // Set ticket info
        tvTicketTitle.text = ticket.title
        tvTicketStatus.text = "Status: ${ticket.status.name}"

        // Setup messages RecyclerView
        val messageAdapter = com.example.princecine.adapter.TicketMessageAdapter(ticket.messages)
        rvMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messageAdapter
        }

        // Show resolve button only for pending tickets
        if (ticket.status == TicketStatus.PENDING || ticket.status == TicketStatus.IN_PROGRESS) {
            btnResolve.visibility = View.VISIBLE
        } else {
            btnResolve.visibility = View.GONE
            tilNewMessage.visibility = View.GONE
            btnSendMessage.visibility = View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnSendMessage.setOnClickListener {
            val messageText = etNewMessage.text?.toString()?.trim()
            if (messageText.isNullOrEmpty()) {
                tilNewMessage.error = "Please enter a message"
                return@setOnClickListener
            }
            tilNewMessage.error = null
            
            // Send admin message
            sendAdminMessage(ticket, messageText) { updatedTicket ->
                dialog.dismiss()
                showTicketDetailsDialog(updatedTicket) // Refresh dialog
            }
        }

        btnResolve.setOnClickListener {
            dialog.dismiss()
            showSolveTicketDialog(ticket)
        }

        dialog.show()
    }

    private fun sendAdminMessage(ticket: SupportTicket, message: String, onSuccess: (SupportTicket) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = repository.addAdminReplyToTicket(
                    ticketId = ticket.id,
                    adminMessage = message,
                    adminName = "Admin", // TODO: Get actual admin name
                    adminId = "admin_id" // TODO: Get actual admin ID
                )
                
                result.onSuccess {
                    // Refresh the data from Firebase to ensure consistency
                    loadTicketsFromFirebase()
                    
                    // Create updated ticket locally for immediate UI callback
                    val newMessage = com.example.princecine.model.TicketMessage(
                        id = "msg_${System.currentTimeMillis()}",
                        message = message,
                        senderType = com.example.princecine.model.SenderType.ADMIN,
                        senderName = "Admin",
                        senderId = "admin_id",
                        timestamp = com.google.firebase.Timestamp.now(),
                        isRead = false
                    )
                    
                    val updatedMessages = ticket.messages + newMessage
                    val updatedTicket = ticket.copy(
                        messages = updatedMessages,
                        status = TicketStatus.IN_PROGRESS,
                        updatedAt = com.google.firebase.Timestamp.now()
                    )
                    
                    onSuccess(updatedTicket)
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Error sending message: ${error.message}", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error sending message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSolveTicketDialog(ticket: SupportTicket) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_solve_ticket, null)
        
        val tvTicketTitle = dialogView.findViewById<MaterialTextView>(R.id.tvTicketTitle)
        val tvTicketDescription = dialogView.findViewById<MaterialTextView>(R.id.tvTicketDescription)
        val tilReply = dialogView.findViewById<TextInputLayout>(R.id.tilReply)
        val etReply = dialogView.findViewById<TextInputEditText>(R.id.etReply)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btnSubmit)

        // Set ticket details (read-only)
        tvTicketTitle.text = ticket.title
        tvTicketDescription.text = ticket.description

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            if (validateReply(tilReply, etReply)) {
                val reply = etReply.text.toString().trim()
                resolveTicket(ticket, reply)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validateReply(tilReply: TextInputLayout, etReply: TextInputEditText): Boolean {
        if (etReply.text.isNullOrBlank()) {
            tilReply.error = "Reply is required"
            return false
        }
        tilReply.error = null
        return true
    }

    private fun resolveTicket(ticket: SupportTicket, reply: String) {
        lifecycleScope.launch {
            try {
                val repository = FirebaseRepository()
                val result = repository.addAdminReplyToTicket(
                    ticketId = ticket.id,
                    adminMessage = reply,
                    adminName = "Admin", // You can get actual admin name from AuthService
                    adminId = "admin_id" // You can get actual admin ID from AuthService
                )
                
                result.onSuccess {
                    // Update ticket status to RESOLVED
                    val statusResult = repository.updateTicketStatus(ticket.id, TicketStatus.RESOLVED)
                    statusResult.onSuccess {
                        // Update local data and refresh the list
                        ticket.status = TicketStatus.RESOLVED
                        loadTicketsFromFirebase() // Refresh the data from Firebase
                        Toast.makeText(requireContext(), "Ticket resolved successfully!", Toast.LENGTH_SHORT).show()
                    }.onFailure { error ->
                        Toast.makeText(requireContext(), "Failed to update ticket status: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Failed to send reply: ${error.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error resolving ticket: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

