package com.example.booking.service;

import com.example.booking.model.Booking;
import com.example.booking.model.Seat;
import com.example.booking.model.Show;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.ShowRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private ShowRepository showRepo;

    @Autowired
    private SeatRepository seatRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private PricingEngine pricingEngine;

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepo.findById(id);
    }

    public Booking saveBooking(Booking booking) {
        return bookingRepo.save(booking);
    }

    @Transactional
    public Booking bookTickets(Long showId, List<String> seatNumbers) {

        Show show = showRepo.findById(showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));

        List<Seat> seats = seatRepo.findByShowIdAndSeatNumberIn(showId, seatNumbers);

        // 🚨 Seat validation
        for (Seat seat : seats) {
            if (seat.isBooked()) {
                throw new RuntimeException("Seat already booked: " + seat.getSeatNumber());
            }
        }

        // Lock seats
        seats.forEach(seat -> seat.setBooked(true));

        double totalPrice = pricingEngine.calculatePrice(seats, show);

        Booking booking = new Booking();
        booking.setShow(show);
        booking.setSeats(seats);
        booking.setTotalPrice(totalPrice);

        return bookingRepo.save(booking);
    }
}
