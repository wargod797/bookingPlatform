package com.example.booking.service;

import com.example.booking.model.Movie;
import com.example.booking.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepo;

    @Cacheable("movies") // caches results using chosen cache
    public Optional<Movie> getMovieById(Long id) {
        return movieRepo.findById(id);
    }

    public List<Movie> getMovies(String genre, String language) {
        if (hasText(genre) && hasText(language)) {
            return movieRepo.findByGenreIgnoreCaseAndLanguageIgnoreCase(genre.trim(), language.trim());
        }
        if (hasText(genre)) {
            return movieRepo.findByGenreIgnoreCase(genre.trim());
        }
        if (hasText(language)) {
            return movieRepo.findByLanguageIgnoreCase(language.trim());
        }
        return movieRepo.findAll();
    }

    public Movie saveMovie(Movie movie) {
        return movieRepo.save(movie);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
