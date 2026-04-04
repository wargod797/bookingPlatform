package com.example.booking.service;

import com.example.booking.exception.InvalidBookingRequestException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.exception.SeatUnavailableException;
import com.example.booking.model.Booking;
import com.example.booking.model.Seat;
import com.example.booking.model.Show;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.ShowRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final ShowRepository showRepo;
    private final SeatRepository seatRepo;
    private final BookingRepository bookingRepo;
    private final PricingEngine pricingEngine;

    public BookingService(
            ShowRepository showRepo,
            SeatRepository seatRepo,
            BookingRepository bookingRepo,
            PricingEngine pricingEngine) {
        this.showRepo = showRepo;
        this.seatRepo = seatRepo;
        this.bookingRepo = bookingRepo;
        this.pricingEngine = pricingEngine;
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepo.findById(id);
    }

    @Transactional
    public Booking bookTickets(Long showId, List<String> seatNumbers) {
        List<String> requestedSeats = normalizeSeatNumbers(seatNumbers);

        Show show = showRepo.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        List<Seat> seats = seatRepo.findByShowIdAndSeatNumberIn(showId, requestedSeats);
        validateSeatSelection(requestedSeats, seats);

        for (Seat seat : seats) {
            if (seat.isBooked()) {
                throw new SeatUnavailableException("Seat already booked: " + seat.getSeatNumber());
            }
        }

        seats.forEach(seat -> seat.setBooked(true));

        Booking booking = new Booking();
        booking.setShow(show);
        booking.setSeats(orderSeatsByRequest(requestedSeats, seats));
        booking.setTotalPrice(pricingEngine.calculatePrice(seats, show));
        booking.setCreatedAt(LocalDateTime.now());

        return bookingRepo.save(booking);
    }

    private List<String> normalizeSeatNumbers(List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new InvalidBookingRequestException("At least one seat must be selected");
        }

        List<String> normalizedSeats = seatNumbers.stream()
                .map(seat -> seat == null ? "" : seat.trim())
                .filter(seat -> !seat.isEmpty())
                .collect(Collectors.toList());

        Set<String> uniqueSeats = new LinkedHashSet<>(normalizedSeats);
        if (normalizedSeats.isEmpty() || uniqueSeats.size() != seatNumbers.size()) {
            throw new InvalidBookingRequestException("Seat numbers must be unique and non-empty");
        }

        return new ArrayList<>(uniqueSeats);
    }

    private void validateSeatSelection(List<String> requestedSeats, List<Seat> seats) {
        if (seats.size() != requestedSeats.size()) {
            Set<String> foundSeatNumbers = seats.stream()
                    .map(Seat::getSeatNumber)
                    .collect(Collectors.toSet());

            List<String> missingSeats = requestedSeats.stream()
                    .filter(seat -> !foundSeatNumbers.contains(seat))
                    .toList();

            throw new InvalidBookingRequestException(
                    "Unknown seat(s): " + String.join(", ", missingSeats)
            );
        }
    }

    private List<Seat> orderSeatsByRequest(List<String> requestedSeats, List<Seat> seats) {
        return requestedSeats.stream()
                .map(seatNumber -> seats.stream()
                        .filter(seat -> seat.getSeatNumber().equals(seatNumber))
                        .findFirst()
                        .orElseThrow())
                .toList();
    }
}
