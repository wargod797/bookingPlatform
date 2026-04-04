package com.example.booking.repository;

import com.example.booking.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieIdAndTheatreCityNameAndShowDate(
            Long movieId,
            String city,
            LocalDate showDate
    );
}