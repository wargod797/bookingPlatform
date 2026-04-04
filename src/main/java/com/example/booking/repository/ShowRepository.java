package com.example.booking.repository;

import com.example.booking.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("""
            select s
            from Show s
            where s.movie.id = :movieId
              and lower(s.theatre.city.name) = lower(:city)
              and s.showDate = :showDate
            """)
    List<Show> findShowsByMovieCityAndDate(
            @Param("movieId") Long movieId,
            @Param("city") String city,
            @Param("showDate") LocalDate showDate
    );
}
