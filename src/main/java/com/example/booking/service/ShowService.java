package com.example.booking.service;

import com.example.booking.exception.InvalidBookingRequestException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.Movie;
import com.example.booking.model.Seat;
import com.example.booking.model.SeatInventoryRequest;
import com.example.booking.model.Show;
import com.example.booking.model.ShowRequest;
import com.example.booking.model.Theatre;
import com.example.booking.repository.MovieRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.ShowRepository;
import com.example.booking.repository.TheatreRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;

    public ShowService(
            ShowRepository showRepository,
            MovieRepository movieRepository,
            TheatreRepository theatreRepository,
            SeatRepository seatRepository) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Show createShow(ShowRequest request) {
        if (request == null
                || request.getMovieId() == null
                || request.getTheatreId() == null
                || request.getPrice() == null
                || request.getPrice() <= 0
                || isBlank(request.getShowDate())
                || isBlank(request.getShowTime())) {
            throw new InvalidBookingRequestException(
                    "movieId, theatreId, showDate, showTime, and positive price are required"
            );
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + request.getMovieId()));

        Theatre theatre = theatreRepository.findById(request.getTheatreId())
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + request.getTheatreId()));

        Show show = Show.builder()
                .movie(movie)
                .theatre(theatre)
                .showDate(LocalDate.parse(request.getShowDate().trim()))
                .showTime(LocalTime.parse(request.getShowTime().trim()))
                .price(request.getPrice())
                .build();

        return showRepository.save(show);
    }

    @Transactional
    public List<Seat> allocateSeats(Long showId, SeatInventoryRequest request) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        List<String> normalizedSeatNumbers = normalizeSeatNumbers(
                request == null ? null : request.getSeatNumbers()
        );

        Set<String> existingSeatNumbers = seatRepository.findByShowId(showId).stream()
                .map(Seat::getSeatNumber)
                .collect(java.util.stream.Collectors.toSet());

        List<String> duplicates = normalizedSeatNumbers.stream()
                .filter(existingSeatNumbers::contains)
                .toList();

        if (!duplicates.isEmpty()) {
            throw new InvalidBookingRequestException(
                    "Seat(s) already allocated: " + String.join(", ", duplicates)
            );
        }

        List<Seat> seats = normalizedSeatNumbers.stream()
                .map(seatNumber -> Seat.builder()
                        .show(show)
                        .seatNumber(seatNumber)
                        .isBooked(false)
                        .build())
                .toList();

        return seatRepository.saveAll(seats);
    }

    private List<String> normalizeSeatNumbers(List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new InvalidBookingRequestException("At least one seat number is required");
        }

        List<String> normalized = seatNumbers.stream()
                .map(seat -> seat == null ? "" : seat.trim())
                .filter(seat -> !seat.isEmpty())
                .toList();

        Set<String> uniqueSeats = new LinkedHashSet<>(normalized);
        if (normalized.isEmpty() || uniqueSeats.size() != normalized.size()) {
            throw new InvalidBookingRequestException("Seat numbers must be unique and non-empty");
        }

        return new ArrayList<>(uniqueSeats);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
