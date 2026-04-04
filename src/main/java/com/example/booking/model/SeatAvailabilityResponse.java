package com.example.booking.model;

public record SeatAvailabilityResponse(
        Long id,
        String seatNumber,
        boolean booked
) {
    public static SeatAvailabilityResponse from(Seat seat) {
        return new SeatAvailabilityResponse(
                seat.getId(),
                seat.getSeatNumber(),
                seat.isBooked()
        );
    }
}
