package com.example.princecine.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.princecine.R
import com.example.princecine.data.FirebaseRepository
import com.example.princecine.model.Booking
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MyTicketAdapter(
    private var bookings: List<Booking>,
    private val onTicketClick: (Booking) -> Unit = {}
) : RecyclerView.Adapter<MyTicketAdapter.TicketViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val repository = FirebaseRepository()

    class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivMoviePoster: ImageView = itemView.findViewById(R.id.ivMoviePoster)
        val tvMovieTitle: MaterialTextView = itemView.findViewById(R.id.tvMovieTitle)
        val tvHall: MaterialTextView = itemView.findViewById(R.id.tvHall)
        val tvDate: MaterialTextView = itemView.findViewById(R.id.tvDate)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tvTime)
        val tvSeats: MaterialTextView = itemView.findViewById(R.id.tvSeats)
        val tvTicketId: MaterialTextView = itemView.findViewById(R.id.tvTicketId)
        val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val booking = bookings[position]

        // Set movie title
        holder.tvMovieTitle.text = booking.movieTitle

        // Set hall (for now, we'll use a default since it's not in the booking model)
        holder.tvHall.text = "  Hall ${('A'..'C').random()}"

        // Set date
        holder.tvDate.text = "  ${booking.showDate}"

        // Set time
        holder.tvTime.text = "  ${booking.showTime}"

        // Set seats
        val seatText = if (booking.seats.size == 1) {
            "  Seat: ${booking.seats[0]}"
        } else {
            "  Seats: ${booking.seats.joinToString(", ")}"
        }
        holder.tvSeats.text = seatText

        // Set ticket ID
        holder.tvTicketId.text = "Ticket ID: ${booking.bookingId}"

        // Load movie poster from movie document using movieId
        loadMoviePoster(holder.ivMoviePoster, booking.movieId)

        // Set click listener
        holder.btnViewDetails.setOnClickListener {
            onTicketClick(booking)
        }

        // Set click listener for the whole item
        holder.itemView.setOnClickListener {
            onTicketClick(booking)
        }
    }

    private fun loadMoviePoster(imageView: ImageView, movieId: String) {
        // Set placeholder first
        imageView.setImageResource(R.drawable.ic_movie_placeholder)
        
        // Fetch movie poster from Firebase
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MyTicketAdapter", "Loading poster for movieId: $movieId")
                val movie = repository.getMovieById(movieId)
                
                if (movie != null && !movie.posterBase64.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        try {
                            val imageBytes = Base64.decode(movie.posterBase64, Base64.DEFAULT)
                            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            if (decodedImage != null) {
                                imageView.setImageBitmap(decodedImage)
                                Log.d("MyTicketAdapter", "Successfully loaded poster for movie: ${movie.title}")
                            } else {
                                Log.w("MyTicketAdapter", "Failed to decode image for movie: ${movie.title}")
                            }
                        } catch (e: Exception) {
                            Log.e("MyTicketAdapter", "Error decoding poster for movie: ${movie.title}", e)
                        }
                    }
                } else {
                    Log.w("MyTicketAdapter", "Movie not found or no poster available for movieId: $movieId")
                }
            } catch (e: Exception) {
                Log.e("MyTicketAdapter", "Exception loading poster for movieId: $movieId", e)
            }
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}
