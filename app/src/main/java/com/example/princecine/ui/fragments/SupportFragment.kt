package com.example.princecine.ui.fragments

import android.os.Bundle
import android.util.Log
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
import com.example.princecine.service.AuthService
import com.example.princecine.model.SupportTicket
import com.example.princecine.model.TicketStatus
import com.example.princecine.model.TicketMessage
import com.example.princecine.model.SenderType
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.*
import android.text.TextWatcher
import android.text.Editable
import kotlinx.coroutines.launch

class SupportFragment : Fragment() {
    
    private lateinit var chipAll: Chip
    private lateinit var chipPending: Chip
    private lateinit var chipResolved: Chip
    private lateinit var rvTickets: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var fabNewTicket: FloatingActionButton
    private lateinit var btnRefresh: MaterialButton
    private lateinit var ticketAdapter: SupportTicketAdapter
    
    private val allTickets = mutableListOf<SupportTicket>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_support, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        setupCapsuleClickListeners()
        setupFabClickListener()
        setupRefreshButton()
        loadUserTickets()
    }
    
        override fun onResume() {
        super.onResume()
        // Reload tickets when fragment becomes visible again
        loadUserTickets()
    }
    
    private fun loadUserTickets() {
        Log.d("SupportFragment", "loadUserTickets() called")
        
        val currentUser = AuthService(requireContext()).getCurrentUser()
        if (currentUser == null) {
            Log.w("SupportFragment", "No current user found")
            Toast.makeText(context, "Please login to view support tickets", Toast.LENGTH_LONG).show()
            updateUI()
            return
        }
        
        Log.d("SupportFragment", "Current user found: ${currentUser.email}, ID: ${currentUser.id}")
        
        // Debug: Check if user ID is valid
        if (currentUser.id.isEmpty() || currentUser.id.isBlank()) {
            Log.e("SupportFragment", "User ID is empty or blank!")
            Toast.makeText(context, "Invalid user session. Please logout and login again.", Toast.LENGTH_LONG).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                Log.d("SupportFragment", "Loading support tickets for user: ${currentUser.id}")
                
                // Check network connectivity
                val repository = FirebaseRepository()
                
                // Add timeout and retry logic
                val result = repository.getUserTickets(currentUser.id)
                
                result.onSuccess { tickets ->
                    Log.d("SupportFragment", "Successfully loaded ${tickets.size} support tickets")
                    if (tickets.isEmpty()) {
                        Log.d("SupportFragment", "No tickets found for user")
                        Toast.makeText(context, "No support tickets found. Click the + button to create one!", Toast.LENGTH_SHORT).show()
                    }
                    allTickets.clear()
                    allTickets.addAll(tickets)
                    ticketAdapter.updateTickets(tickets)
                    updateUI()
                }.onFailure { error ->
                    Log.e("SupportFragment", "Failed to load support tickets: ${error.message}", error)
                    
                    // Show retry option for users
                    when {
                        error.message?.contains("network", ignoreCase = true) == true -> {
                            Toast.makeText(context, "Network error. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                        }
                        error.message?.contains("permission", ignoreCase = true) == true -> {
                            Toast.makeText(context, "Permission denied. Please contact support or try logging out and back in.", Toast.LENGTH_LONG).show()
                        }
                        error.message?.contains("unavailable", ignoreCase = true) == true -> {
                            Toast.makeText(context, "Service temporarily unavailable. Please try again in a moment.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(context, "Failed to load support tickets: ${error.message}. Pull down to refresh.", Toast.LENGTH_LONG).show()
                        }
                    }
                    updateUI()
                }
            } catch (e: Exception) {
                Log.e("SupportFragment", "Error loading support tickets", e)
                Toast.makeText(context, "Error loading support tickets", Toast.LENGTH_SHORT).show()
                updateUI()
            }
        }
        updateUI()
    }
    
    private fun initializeViews(view: View) {
        chipAll = view.findViewById(R.id.chipAll)
        chipPending = view.findViewById(R.id.chipPending)
        chipResolved = view.findViewById(R.id.chipResolved)
        rvTickets = view.findViewById(R.id.rvTickets)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        fabNewTicket = view.findViewById(R.id.fabNewTicket)
        btnRefresh = view.findViewById(R.id.btnRefresh)
    }
    
    private fun setupRecyclerView() {
        ticketAdapter = SupportTicketAdapter(
            tickets = allTickets,
            onTicketClick = { ticket ->
                // Open ticket details with message thread
                showTicketDetailsDialog(ticket)
            },
            isAdmin = false,
            onSolveClick = null
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
    
    private fun setupFabClickListener() {
        fabNewTicket.setOnClickListener {
            showNewInquiryDialog()
        }
    }
    
    private fun setupRefreshButton() {
        btnRefresh.setOnClickListener {
            Log.d("SupportFragment", "Manual refresh triggered")
            Toast.makeText(context, "Refreshing tickets...", Toast.LENGTH_SHORT).show()
            loadUserTickets()
        }
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
    
    private fun updateUI() {
        if (ticketAdapter.itemCount == 0) {
            rvTickets.visibility = View.GONE
            llEmptyState.visibility = View.VISIBLE
        } else {
            rvTickets.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
        }
    }

    private fun showNewInquiryDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_new_inquiry, null)
        
        val tilInquiryTitle = dialogView.findViewById<TextInputLayout>(R.id.tilInquiryTitle)
        val tilDescription = dialogView.findViewById<TextInputLayout>(R.id.tilDescription)
        val etInquiryTitle = dialogView.findViewById<TextInputEditText>(R.id.etInquiryTitle)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btnSubmit)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set up button click listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            if (validateInputs(tilInquiryTitle, tilDescription, etInquiryTitle, etDescription)) {
                submitInquiry(etInquiryTitle.text.toString(), etDescription.text.toString())
                dialog.dismiss()
            }
        }

        // Set up text change listeners to clear errors when user types
        etInquiryTitle.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                tilInquiryTitle.error = null
            }
        })

        etDescription.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                tilDescription.error = null
            }
            })

        dialog.show()
    }

    private fun validateInputs(
        tilInquiryTitle: TextInputLayout,
        tilDescription: TextInputLayout,
        etInquiryTitle: TextInputEditText,
        etDescription: TextInputEditText
    ): Boolean {
        var isValid = true

        // Validate inquiry title
        if (etInquiryTitle.text.isNullOrBlank()) {
            tilInquiryTitle.error = "Inquiry title is required"
            isValid = false
        }

        // Validate description
        if (etDescription.text.isNullOrBlank()) {
            tilDescription.error = "Description is required"
            isValid = false
        }

        return isValid
    }

    private fun submitInquiry(title: String, description: String) {
        lifecycleScope.launch {
            try {
                val currentUser = AuthService(requireContext()).getCurrentUser()
                if (currentUser == null) {
                    Toast.makeText(context, "Please login to submit inquiry", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Generate new ticket ID
                val newTicketId = generateTicketId()
                
                // Create initial message from user
                val initialMessage = TicketMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    message = description,
                    senderType = SenderType.USER,
                    senderName = currentUser.fullName ?: "User",
                    senderId = currentUser.id,
                    timestamp = com.google.firebase.Timestamp.now(),
                    isRead = true // User's own message is always read
                )
                
                // Create new support ticket
                val newTicket = SupportTicket(
                    userId = currentUser.id,
                    userEmail = currentUser.email,
                    userName = currentUser.fullName ?: "User",
                    title = title,
                    description = description,
                    status = TicketStatus.PENDING,
                    dateRaised = com.google.firebase.Timestamp.now(),
                    ticketId = newTicketId,
                    messages = listOf(initialMessage)
                )

                // Save to Firebase
                val repository = FirebaseRepository()
                val result = repository.createSupportTicket(newTicket)
                
                result.onSuccess { documentId ->
                    // Refresh the data from Firebase to ensure consistency
                    loadUserTickets()
                    
                    Toast.makeText(context, "Inquiry submitted successfully!", Toast.LENGTH_LONG).show()
                }.onFailure { error ->
                    Toast.makeText(context, "Error submitting inquiry: ${error.message}", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "Error submitting inquiry: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generateTicketId(): String {
        val timestamp = System.currentTimeMillis()
        val random = Random().nextInt(1000)
        return "ST${timestamp}${random}"
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date())
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

        // Set ticket info
        tvTicketTitle.text = ticket.title
        tvTicketStatus.text = "Status: ${ticket.status.name}"

        // Setup messages RecyclerView
        val messageAdapter = TicketMessageAdapter(ticket.messages)
        rvMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messageAdapter
        }

        // Hide message input if ticket is resolved/closed
        if (ticket.status == TicketStatus.RESOLVED || ticket.status == TicketStatus.CLOSED) {
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
            
            // Send message and refresh the dialog
            sendUserMessage(ticket, messageText) { updatedTicket ->
                dialog.dismiss()
                showTicketDetailsDialog(updatedTicket) // Refresh dialog with updated ticket
            }
        }

        dialog.show()
    }

    private fun sendUserMessage(ticket: SupportTicket, message: String, onSuccess: (SupportTicket) -> Unit) {
        lifecycleScope.launch {
            try {
                val currentUser = AuthService(requireContext()).getCurrentUser()
                if (currentUser == null) {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val newMessage = TicketMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    message = message,
                    senderType = SenderType.USER,
                    senderName = currentUser.fullName ?: "User",
                    senderId = currentUser.id,
                    timestamp = com.google.firebase.Timestamp.now(),
                    isRead = true
                )

                val repository = FirebaseRepository()
                val result = repository.addMessageToTicket(ticket.id, newMessage)
                
                result.onSuccess {
                    // Refresh the data from Firebase to ensure consistency
                    loadUserTickets()
                    
                    // Update local ticket for immediate UI callback
                    val updatedMessages = ticket.messages + newMessage
                    val updatedTicket = ticket.copy(
                        messages = updatedMessages,
                        updatedAt = com.google.firebase.Timestamp.now()
                    )
                    
                    onSuccess(updatedTicket)
                }.onFailure { error ->
                    Toast.makeText(context, "Error sending message: ${error.message}", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "Error sending message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
