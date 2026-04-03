package com.example.booking.service;

import com.example.booking.model.Movie;
import com.example.booking.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepo;

    @Cacheable("movies") // caches results using chosen cache
    public Optional<Movie> getMovieById(Long id) {
        return movieRepo.findById(id);
    }

    public Movie saveMovie(Movie movie) {
        return movieRepo.save(movie);
    }
}
