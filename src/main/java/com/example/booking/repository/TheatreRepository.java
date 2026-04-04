package com.example.booking.repository;

import com.example.booking.model.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    @Query("""
            select count(t) > 0
            from Theatre t
            where lower(t.name) = lower(:name)
              and lower(t.city.name) = lower(:cityName)
            """)
    boolean existsByNameAndCityNameIgnoreCase(
            @Param("name") String name,
            @Param("cityName") String cityName
    );
}
