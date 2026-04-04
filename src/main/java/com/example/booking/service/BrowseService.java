package com.example.booking.service;

import com.example.booking.model.Show;
import com.example.booking.repository.ShowRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BrowseService {

    private final ShowRepository showRepository;

    public BrowseService(ShowRepository showRepository) {
        this.showRepository = showRepository;
    }

    public List<Show> getShows(Long movieId, String city, LocalDate date) {
        return showRepository.findShowsByMovieCityAndDate(
                movieId, city, date);
    }
}
