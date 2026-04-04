package com.example.booking.repository;

import com.example.booking.model.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

    @Query("""
            select t
            from Theatre t
            where (:cityName is null or lower(t.city.name) = lower(:cityName))
            order by t.city.name asc, t.name asc
            """)
    List<Theatre> findByOptionalCityOrdered(@Param("cityName") String cityName);
}
