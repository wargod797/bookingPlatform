package com.example.booking.repository;

import com.example.booking.model.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByNameIgnoreCase(String name);

    java.util.List<City> findAllByOrderByNameAsc();
}
