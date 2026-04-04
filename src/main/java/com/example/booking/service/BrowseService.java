package com.example.booking.service;

import com.example.booking.model.Show;
import com.example.booking.repository.ShowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BrowseService {

    @Autowired
    private ShowRepository showRepository;

    public List<Show> getShows(Long movieId, String city, LocalDate date) {
        return showRepository.findByMovieIdAndTheatreCityNameAndShowDate(
                movieId, city, date);
    }
}