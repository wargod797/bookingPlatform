package com.example.booking.service;

import com.example.booking.config.PricingProperties;
import com.example.booking.exception.InvalidBookingRequestException;
import com.example.booking.exception.SeatUnavailableException;
import com.example.booking.model.Booking;
import com.example.booking.model.City;
import com.example.booking.model.Movie;
import com.example.booking.model.Seat;
import com.example.booking.model.Show;
import com.example.booking.model.Theatre;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.ShowRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    @Test
    void appliesBothDiscountsForEligibleAfternoonShowAndSetsTimestamp() {
        ShowRepository showRepository = mock(ShowRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingService bookingService = new BookingService(
                showRepository,
                seatRepository,
                bookingRepository,
                eligiblePricingEngine()
        );

        Show show = createShow();
        Seat seat1 = createSeat("A1", show, false);
        Seat seat2 = createSeat("A2", show, false);
        Seat seat3 = createSeat("A3", show, false);

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByShowIdAndSeatNumberIn(1L, List.of("A1", "A2", "A3")))
                .thenReturn(List.of(seat1, seat2, seat3));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.bookTickets(1L, List.of("A1", "A2", "A3"));

        assertEquals(200.0, booking.getTotalPrice());
        assertNotNull(booking.getCreatedAt());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void appliesOnlyAfternoonDiscountForNonEligibleOfferLocation() {
        ShowRepository showRepository = mock(ShowRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingService bookingService = new BookingService(
                showRepository,
                seatRepository,
                bookingRepository,
                nonEligiblePricingEngine()
        );

        Show show = createShow("Delhi", "Downtown Screens", LocalTime.of(13, 0));
        Seat seat1 = createSeat("A1", show, false);
        Seat seat2 = createSeat("A2", show, false);
        Seat seat3 = createSeat("A3", show, false);

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByShowIdAndSeatNumberIn(1L, List.of("A1", "A2", "A3")))
                .thenReturn(List.of(seat1, seat2, seat3));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.bookTickets(1L, List.of("A1", "A2", "A3"));

        assertEquals(240.0, booking.getTotalPrice());
        assertNotNull(booking.getCreatedAt());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void rejectsUnknownSeats() {
        ShowRepository showRepository = mock(ShowRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingService bookingService = new BookingService(
                showRepository,
                seatRepository,
                bookingRepository,
                eligiblePricingEngine()
        );

        Show show = createShow();
        Seat seat1 = createSeat("A1", show, false);

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByShowIdAndSeatNumberIn(1L, List.of("A1", "A2")))
                .thenReturn(List.of(seat1));

        assertThrows(
                InvalidBookingRequestException.class,
                () -> bookingService.bookTickets(1L, List.of("A1", "A2"))
        );
    }

    @Test
    void rejectsAlreadyBookedSeat() {
        ShowRepository showRepository = mock(ShowRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingService bookingService = new BookingService(
                showRepository,
                seatRepository,
                bookingRepository,
                eligiblePricingEngine()
        );

        Show show = createShow();
        Seat seat1 = createSeat("A1", show, true);

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByShowIdAndSeatNumberIn(1L, List.of("A1")))
                .thenReturn(List.of(seat1));

        assertThrows(
                SeatUnavailableException.class,
                () -> bookingService.bookTickets(1L, List.of("A1"))
        );
    }

    private PricingEngine eligiblePricingEngine() {
        PricingProperties properties = new PricingProperties();
        properties.setThirdTicketOfferCities(List.of("Mumbai"));
        properties.setThirdTicketOfferTheatres(List.of("PVR Andheri"));
        return new PricingEngine(properties);
    }

    private PricingEngine nonEligiblePricingEngine() {
        PricingProperties properties = new PricingProperties();
        properties.setThirdTicketOfferCities(List.of("Chennai"));
        properties.setThirdTicketOfferTheatres(List.of("Marina Screens"));
        return new PricingEngine(properties);
    }

    private Show createShow() {
        return createShow("Mumbai", "PVR Andheri", LocalTime.of(13, 0));
    }

    private Show createShow(String cityName, String theatreName, LocalTime showTime) {
        City city = City.builder().name(cityName).build();
        Theatre theatre = Theatre.builder().name(theatreName).city(city).build();
        Movie movie = new Movie(1L, "Interstellar", "Sci-Fi", "English");

        return Show.builder()
                .id(1L)
                .movie(movie)
                .theatre(theatre)
                .showDate(LocalDate.now())
                .showTime(showTime)
                .price(100.0)
                .build();
    }

    private Seat createSeat(String seatNumber, Show show, boolean booked) {
        return Seat.builder()
                .seatNumber(seatNumber)
                .show(show)
                .isBooked(booked)
                .build();
    }
}
