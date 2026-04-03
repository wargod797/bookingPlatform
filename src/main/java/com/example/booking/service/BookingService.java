package com.example.booking.service;

import com.example.booking.model.Booking;
import com.example.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepo;

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepo.findById(id);
    }

    public Booking saveBooking(Booking booking) {
        return bookingRepo.save(booking);
    }
}
