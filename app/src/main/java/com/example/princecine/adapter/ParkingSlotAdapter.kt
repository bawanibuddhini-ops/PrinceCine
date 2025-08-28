package com.example.princecine.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.princecine.R
import com.example.princecine.model.ParkingSlot
import com.example.princecine.model.VehicleType
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView

class ParkingSlotAdapter(
    private var parkingSlots: List<ParkingSlot>,
    private val onSlotClick: (ParkingSlot) -> Unit
) : RecyclerView.Adapter<ParkingSlotAdapter.SlotViewHolder>() {

    private var selectedSlotId: String? = null

    class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardSlot: MaterialCardView = itemView.findViewById(R.id.cardParkingSlot)
        val ivVehicleIcon: ImageView = itemView.findViewById(R.id.ivVehicleIcon)
        val tvSlotNumber: MaterialTextView = itemView.findViewById(R.id.tvSlotNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = parkingSlots[position]
        
        holder.tvSlotNumber.text = slot.slotNumber
        
        // Set vehicle icon based on type
        val iconRes = when (slot.vehicleType) {
            VehicleType.MOTOR_BIKE -> R.drawable.ic_motorcycle
            VehicleType.THREE_WHEELER -> R.drawable.ic_three_wheeler
            VehicleType.CAR -> R.drawable.ic_car
        }
        holder.ivVehicleIcon.setImageResource(iconRes)
        
        // Set slot appearance based on status
        val context = holder.itemView.context
        
        when {
            slot.id == selectedSlotId -> {
                // Selected slot - green background
                holder.cardSlot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.green))
                holder.cardSlot.strokeColor = ContextCompat.getColor(context, R.color.green)
                holder.cardSlot.strokeWidth = 4
                holder.tvSlotNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.ivVehicleIcon.clearColorFilter()
                holder.ivVehicleIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))
                holder.cardSlot.isEnabled = true
            }
            slot.isBooked -> {
                // Booked slot - red background
                holder.cardSlot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.red))
                holder.cardSlot.strokeColor = ContextCompat.getColor(context, R.color.red)
                holder.cardSlot.strokeWidth = 2
                holder.tvSlotNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.ivVehicleIcon.clearColorFilter()
                holder.ivVehicleIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))
                holder.cardSlot.isEnabled = false
            }
            else -> {
                // Available slot - white background with black border
                holder.cardSlot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                holder.cardSlot.strokeColor = ContextCompat.getColor(context, R.color.black)
                holder.cardSlot.strokeWidth = 2
                holder.tvSlotNumber.setTextColor(ContextCompat.getColor(context, R.color.black))
                holder.ivVehicleIcon.clearColorFilter() // Clear any previous color filter
                holder.ivVehicleIcon.setColorFilter(ContextCompat.getColor(context, R.color.black))
                holder.cardSlot.isEnabled = true
            }
        }
        
        // Set click listener
        holder.cardSlot.setOnClickListener {
            if (!slot.isBooked) {
                onSlotClick(slot)
            }
        }
    }

    override fun getItemCount(): Int = parkingSlots.size

    fun updateSlots(newSlots: List<ParkingSlot>) {
        parkingSlots = newSlots
        notifyDataSetChanged()
    }
    
    fun setSelectedSlot(slotId: String?) {
        val previousSelected = selectedSlotId
        selectedSlotId = slotId
        
        // Update only the affected items
        parkingSlots.forEachIndexed { index, slot ->
            if (slot.id == previousSelected || slot.id == selectedSlotId) {
                notifyItemChanged(index)
            }
        }
    }
}
