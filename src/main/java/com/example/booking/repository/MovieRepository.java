package com.example.booking.repository;

import com.example.booking.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByGenreIgnoreCase(String genre);

    List<Movie> findByLanguageIgnoreCase(String language);

    List<Movie> findByGenreIgnoreCaseAndLanguageIgnoreCase(String genre, String language);
}
