package com.example.princecine.ui.fragments

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.princecine.R
import com.example.princecine.adapter.ParkingSlotAdapter
import com.example.princecine.data.FirebaseRepository
import com.example.princecine.model.ParkingBooking
import com.example.princecine.model.ParkingSlot
import com.example.princecine.model.ParkingStatus
import com.example.princecine.model.VehicleType
import com.example.princecine.service.AuthService
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ParkingFragment : Fragment() {

    private lateinit var chipGroupVehicleType: ChipGroup
    private lateinit var rvLeftSide: RecyclerView
    private lateinit var rvRightSide: RecyclerView
    private lateinit var cardSelectedSlot: MaterialCardView
    private lateinit var tvSelectedSlot: MaterialTextView
    private lateinit var btnReserveParking: MaterialButton
    private lateinit var tilVehicleNumber: TextInputLayout
    private lateinit var etVehicleNumber: TextInputEditText

    private lateinit var repository: FirebaseRepository
    private lateinit var authService: AuthService
    
    private lateinit var leftSideAdapter: ParkingSlotAdapter
    private lateinit var rightSideAdapter: ParkingSlotAdapter
    
    private var selectedSlot: ParkingSlot? = null
    private var currentVehicleType = VehicleType.CAR

    companion object {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_parking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize dependencies
        repository = FirebaseRepository()
        authService = AuthService(requireContext())

        // Initialize views
        initViews(view)

        // Setup RecyclerViews
        setupRecyclerViews()

        // Setup click listeners
        setupClickListeners()

        // Load initial data
        loadParkingSlots()
    }

    private fun initViews(view: View) {
        chipGroupVehicleType = view.findViewById(R.id.chipGroupVehicleType)
        rvLeftSide = view.findViewById(R.id.rvLeftSide)
        rvRightSide = view.findViewById(R.id.rvRightSide)
        cardSelectedSlot = view.findViewById(R.id.cardSelectedSlot)
        tvSelectedSlot = view.findViewById(R.id.tvSelectedSlot)
        btnReserveParking = view.findViewById(R.id.btnReserveParking)
        tilVehicleNumber = view.findViewById(R.id.tilVehicleNumber)
        etVehicleNumber = view.findViewById(R.id.etVehicleNumber)
    }

    private fun setupRecyclerViews() {
        // Setup Left Side RecyclerView
        leftSideAdapter = ParkingSlotAdapter(emptyList()) { slot ->
            selectSlot(slot)
        }
        rvLeftSide.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = leftSideAdapter
        }

        // Setup Right Side RecyclerView
        rightSideAdapter = ParkingSlotAdapter(emptyList()) { slot ->
            selectSlot(slot)
        }
        rvRightSide.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = rightSideAdapter
        }
    }

    private fun setupClickListeners() {
        chipGroupVehicleType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentVehicleType = when (checkedIds[0]) {
                    R.id.chipMotorBike -> VehicleType.MOTOR_BIKE
                    R.id.chipThreeWheeler -> VehicleType.THREE_WHEELER
                    R.id.chipCar -> VehicleType.CAR
                    else -> VehicleType.CAR
                }
                clearSelection()
                loadParkingSlots()
            }
        }

        btnReserveParking.setOnClickListener {
            selectedSlot?.let { slot ->
                val vehicleNumber = etVehicleNumber.text?.toString()?.trim()
                if (vehicleNumber.isNullOrEmpty()) {
                    tilVehicleNumber.error = "Please enter vehicle number"
                    return@setOnClickListener
                }
                if (vehicleNumber.length < 3) {
                    tilVehicleNumber.error = "Vehicle number too short"
                    return@setOnClickListener
                }
                tilVehicleNumber.error = null
                reserveParking(slot, vehicleNumber)
            }
        }
    }

    private fun loadParkingSlots() {
        lifecycleScope.launch {
            try {
                val result = repository.getParkingSlots(currentVehicleType)
                if (result.isSuccess) {
                    val slots = result.getOrNull() ?: emptyList()
                    
                    // Separate slots by side
                    val leftSlots = slots.filter { it.slotNumber.startsWith("L-") }
                    val rightSlots = slots.filter { it.slotNumber.startsWith("R-") }
                    
                    leftSideAdapter.updateSlots(leftSlots)
                    rightSideAdapter.updateSlots(rightSlots)
                    
                    Log.d("ParkingFragment", "Loaded ${slots.size} parking slots for ${currentVehicleType.displayName}")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to load parking slots"
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ParkingFragment", "Exception loading parking slots", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectSlot(slot: ParkingSlot) {
        if (slot.isBooked) {
            Toast.makeText(requireContext(), "This slot is already booked", Toast.LENGTH_SHORT).show()
            return
        }

        // Toggle selection - if same slot clicked, deselect it
        if (selectedSlot?.id == slot.id) {
            clearSelection()
            return
        }

        selectedSlot = slot
        
        // Update adapters
        leftSideAdapter.setSelectedSlot(slot.id)
        rightSideAdapter.setSelectedSlot(slot.id)
        
        // Show selected slot info
        tvSelectedSlot.text = slot.slotNumber
        cardSelectedSlot.visibility = View.VISIBLE
        btnReserveParking.isEnabled = true
        btnReserveParking.text = "Reserve ${slot.slotNumber} - LKR 300"
    }

    private fun clearSelection() {
        selectedSlot = null
        leftSideAdapter.setSelectedSlot(null)
        rightSideAdapter.setSelectedSlot(null)
        cardSelectedSlot.visibility = View.GONE
        btnReserveParking.isEnabled = false
        btnReserveParking.text = "Reserve Parking - LKR 300"
    }

    private fun reserveParking(slot: ParkingSlot, vehicleNumber: String) {
        lifecycleScope.launch {
            try {
                val currentUser = authService.getCurrentUser()
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "Please login to reserve parking", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create booking ID
                val bookingId = "PK${System.currentTimeMillis()}${Random().nextInt(1000)}"

                val parkingBooking = ParkingBooking(
                    bookingId = bookingId,
                    userId = currentUser.id,
                    userName = currentUser.fullName ?: "Customer",
                    slotId = slot.id,
                    slotNumber = slot.slotNumber,
                    vehicleType = slot.vehicleType,
                    vehicleNumber = vehicleNumber,
                    fee = 300.0,
                    status = ParkingStatus.ACTIVE
                )

                btnReserveParking.isEnabled = false
                btnReserveParking.text = "Processing..."

                val result = repository.createParkingBooking(parkingBooking)
                
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Parking reserved successfully!", Toast.LENGTH_LONG).show()
                    
                    // Generate and download PDF
                    generateParkingTicketPDF(parkingBooking, currentUser.fullName ?: "Customer")
                    
                    // Clear form
                    etVehicleNumber.text?.clear()
                    
                    // Refresh parking slots
                    clearSelection()
                    loadParkingSlots()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to reserve parking"
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    
                    btnReserveParking.isEnabled = true
                    btnReserveParking.text = "Reserve ${slot.slotNumber} - LKR 300"
                }
            } catch (e: Exception) {
                Log.e("ParkingFragment", "Exception reserving parking", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                
                btnReserveParking.isEnabled = true
                selectedSlot?.let {
                    btnReserveParking.text = "Reserve ${it.slotNumber} - LKR 300"
                }
            }
        }
    }

    private fun generateParkingTicketPDF(booking: ParkingBooking, customerName: String) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Set white background
            val backgroundPaint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.white)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 842f, backgroundPaint)

            // Header
            val headerPaint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.red)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 120f, headerPaint)

            // Title
            val titlePaint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.white)
                textSize = 28f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("PRINCE CINEMA", 297.5f, 50f, titlePaint)
            canvas.drawText("PARKING TICKET", 297.5f, 90f, titlePaint)

            // Border
            val borderPaint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.red)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(40f, 140f, 555f, 720f, borderPaint)

            // Content
            val labelPaint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                textSize = 14f
                isFakeBoldText = true
            }

            val detailPaint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.black)
                textSize = 16f
            }

            var yPos = 180f
            val lineSpacing = 35f

            // Booking details
            canvas.drawText("Customer:", 60f, yPos, labelPaint)
            canvas.drawText(customerName, 200f, yPos, detailPaint)

            yPos += lineSpacing
            canvas.drawText("Booking ID:", 60f, yPos, labelPaint)
            canvas.drawText(booking.bookingId, 200f, yPos, detailPaint)

            yPos += lineSpacing
            canvas.drawText("Parking Slot:", 60f, yPos, labelPaint)
            canvas.drawText(booking.slotNumber, 200f, yPos, detailPaint)

            yPos += lineSpacing
            canvas.drawText("Vehicle Type:", 60f, yPos, labelPaint)
            canvas.drawText(booking.vehicleType.displayName, 200f, yPos, detailPaint)

            yPos += lineSpacing
            canvas.drawText("Vehicle Number:", 60f, yPos, labelPaint)
            canvas.drawText(booking.vehicleNumber, 200f, yPos, detailPaint)

            yPos += lineSpacing
            canvas.drawText("Fee:", 60f, yPos, labelPaint)
            canvas.drawText("LKR ${booking.fee.toInt()}", 200f, yPos, detailPaint)

            yPos += lineSpacing
            canvas.drawText("Date:", 60f, yPos, labelPaint)
            canvas.drawText(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date()), 200f, yPos, detailPaint)

            yPos += lineSpacing
            canvas.drawText("Status:", 60f, yPos, labelPaint)
            canvas.drawText("CONFIRMED", 200f, yPos, detailPaint)

            // Footer
            val footerPaint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.grey)
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Thank you for using Prince Cinema Parking!", 297.5f, 750f, footerPaint)
            canvas.drawText("Present this ticket for parking validation", 297.5f, 770f, footerPaint)

            pdfDocument.finishPage(page)

            // Save PDF to Downloads folder using modern Android approach
            val fileName = "parking_ticket_${booking.bookingId}.pdf"
            
            val file = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // For Android 10+, use MediaStore API
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                
                val uri = requireContext().contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { 
                    requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                    requireContext().contentResolver.update(uri, contentValues, null, null)
                }
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            } else {
                // For older versions, use direct file access
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                
                file.parentFile?.mkdirs()
                val fileOutputStream = FileOutputStream(file)
                pdfDocument.writeTo(fileOutputStream)
                fileOutputStream.close()
                file
            }
            
            pdfDocument.close()

            Toast.makeText(requireContext(), "Parking ticket downloaded successfully!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("ParkingFragment", "Error generating PDF", e)
            Toast.makeText(requireContext(), "Error generating ticket: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
