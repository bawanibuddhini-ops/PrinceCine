package com.example.princecine.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.princecine.R
import com.example.princecine.adapter.MyTicketAdapter
import com.example.princecine.data.FirebaseRepository
import com.example.princecine.model.Booking
import com.example.princecine.service.AuthService
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class MyTicketsFragment : Fragment() {
    
    private lateinit var rvTickets: RecyclerView
    private lateinit var tvTicketSubtitle: MaterialTextView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var llNoTickets: View
    
    private lateinit var repository: FirebaseRepository
    private lateinit var authService: AuthService
    private lateinit var ticketAdapter: MyTicketAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_tickets, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize dependencies
        repository = FirebaseRepository()
        authService = AuthService(requireContext())
        
        // Initialize views
        rvTickets = view.findViewById(R.id.rvTickets)
        tvTicketSubtitle = view.findViewById(R.id.tvTicketSubtitle)
        progressBar = view.findViewById(R.id.progressBar)
        llNoTickets = view.findViewById(R.id.llNoTickets)
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Load tickets
        loadUserTickets()
    }
    
    private fun setupRecyclerView() {
        ticketAdapter = MyTicketAdapter(emptyList()) { booking ->
            // Handle ticket click - you can navigate to ticket details here
            Toast.makeText(requireContext(), "Ticket: ${booking.bookingId}", Toast.LENGTH_SHORT).show()
        }
        
        rvTickets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ticketAdapter
        }
    }
    
    private fun loadUserTickets() {
        lifecycleScope.launch {
            try {
                showLoadingState()
                
                val currentUser = authService.getCurrentUser()
                if (currentUser?.id == null || currentUser.id.isEmpty()) {
                    showError("User not logged in")
                    return@launch
                }
                
                Log.d("MyTicketsFragment", "Loading tickets for user: ${currentUser.id}")
                val result = repository.getUserBookings(currentUser.id)
                
                if (result.isSuccess) {
                    val bookings = result.getOrNull() ?: emptyList()
                    Log.d("MyTicketsFragment", "Loaded ${bookings.size} tickets")
                    displayTickets(bookings)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to load tickets"
                    Log.e("MyTicketsFragment", "Error loading tickets: $error")
                    showError(error)
                }
                
            } catch (e: Exception) {
                Log.e("MyTicketsFragment", "Exception loading tickets", e)
                showError("Error: ${e.message}")
            }
        }
    }
    
    private fun showLoadingState() {
        progressBar.visibility = View.VISIBLE
        rvTickets.visibility = View.GONE
        llNoTickets.visibility = View.GONE
        tvTicketSubtitle.text = "Loading tickets..."
    }
    
    private fun displayTickets(bookings: List<Booking>) {
        progressBar.visibility = View.GONE
        
        if (bookings.isEmpty()) {
            showNoTickets()
        } else {
            showTickets(bookings)
        }
    }
    
    private fun showTickets(bookings: List<Booking>) {
        rvTickets.visibility = View.VISIBLE
        llNoTickets.visibility = View.GONE
        
        val ticketCount = bookings.size
        tvTicketSubtitle.text = when (ticketCount) {
            1 -> "1 ticket purchased"
            else -> "$ticketCount tickets purchased"
        }
        
        ticketAdapter.updateBookings(bookings)
    }
    
    private fun showNoTickets() {
        rvTickets.visibility = View.GONE
        llNoTickets.visibility = View.VISIBLE
        tvTicketSubtitle.text = "No tickets found"
    }
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        rvTickets.visibility = View.GONE
        llNoTickets.visibility = View.VISIBLE
        tvTicketSubtitle.text = "Error loading tickets"
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
} 