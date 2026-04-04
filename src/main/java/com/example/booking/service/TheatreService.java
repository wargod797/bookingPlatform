package com.example.booking.service;

import com.example.booking.exception.InvalidBookingRequestException;
import com.example.booking.model.City;
import com.example.booking.model.Theatre;
import com.example.booking.model.TheatreOnboardingRequest;
import com.example.booking.model.TheatreResponse;
import com.example.booking.repository.CityRepository;
import com.example.booking.repository.TheatreRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final CityRepository cityRepository;

    public TheatreService(TheatreRepository theatreRepository, CityRepository cityRepository) {
        this.theatreRepository = theatreRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional
    public Theatre onboardTheatre(TheatreOnboardingRequest request) {
        if (request == null || isBlank(request.getTheatreName()) || isBlank(request.getCityName())) {
            throw new InvalidBookingRequestException("Theatre name and city name are required");
        }

        String theatreName = request.getTheatreName().trim();
        String cityName = request.getCityName().trim();

        if (theatreRepository.existsByNameAndCityNameIgnoreCase(theatreName, cityName)) {
            throw new InvalidBookingRequestException(
                    "Theatre is already onboarded for city: " + cityName
            );
        }

        City city = cityRepository.findByNameIgnoreCase(cityName)
                .orElseGet(() -> cityRepository.save(City.builder().name(cityName).build()));

        Theatre theatre = Theatre.builder()
                .name(theatreName)
                .city(city)
                .build();

        return theatreRepository.save(theatre);
    }

    public List<String> getPartnerCities() {
        return cityRepository.findAllByOrderByNameAsc().stream()
                .map(City::getName)
                .toList();
    }

    public List<TheatreResponse> getTheatres(String cityName) {
        String normalizedCity = isBlank(cityName) ? null : cityName.trim();
        return theatreRepository.findByOptionalCityOrdered(normalizedCity).stream()
                .map(TheatreResponse::from)
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
