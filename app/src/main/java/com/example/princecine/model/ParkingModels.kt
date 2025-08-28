package com.example.princecine.model

import com.google.firebase.Timestamp

data class ParkingSlot(
    val id: String = "",
    val slotNumber: String = "",
    val vehicleType: VehicleType = VehicleType.CAR,
    val isBooked: Boolean = false,
    val bookedBy: String = "",
    val bookingId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class ParkingBooking(
    val id: String = "",
    val bookingId: String = "",
    val userId: String = "",
    val userName: String = "",
    val slotId: String = "",
    val slotNumber: String = "",
    val vehicleType: VehicleType = VehicleType.CAR,
    val vehicleNumber: String = "",
    val fee: Double = 300.0,
    val status: ParkingStatus = ParkingStatus.ACTIVE,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

enum class VehicleType(val displayName: String) {
    MOTOR_BIKE("Motor Bike"),
    THREE_WHEELER("Three Wheeler"),
    CAR("Car")
}

enum class ParkingStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
}
