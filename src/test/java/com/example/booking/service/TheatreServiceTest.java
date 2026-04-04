package com.example.booking.service;

import com.example.booking.exception.InvalidBookingRequestException;
import com.example.booking.model.City;
import com.example.booking.model.Theatre;
import com.example.booking.model.TheatreOnboardingRequest;
import com.example.booking.repository.CityRepository;
import com.example.booking.repository.TheatreRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TheatreServiceTest {

    @Test
    void createsCityWhenOnboardingNewTheatre() {
        TheatreRepository theatreRepository = mock(TheatreRepository.class);
        CityRepository cityRepository = mock(CityRepository.class);
        TheatreService theatreService = new TheatreService(theatreRepository, cityRepository);

        when(theatreRepository.existsByNameAndCityNameIgnoreCase("PVR Andheri", "Mumbai"))
                .thenReturn(false);
        when(cityRepository.findByNameIgnoreCase("Mumbai")).thenReturn(Optional.empty());
        when(cityRepository.save(any(City.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(theatreRepository.save(any(Theatre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Theatre theatre = theatreService.onboardTheatre(new TheatreOnboardingRequest("PVR Andheri", "Mumbai"));

        assertEquals("PVR Andheri", theatre.getName());
        assertEquals("Mumbai", theatre.getCity().getName());
    }

    @Test
    void rejectsDuplicateTheatreInSameCity() {
        TheatreRepository theatreRepository = mock(TheatreRepository.class);
        CityRepository cityRepository = mock(CityRepository.class);
        TheatreService theatreService = new TheatreService(theatreRepository, cityRepository);

        when(theatreRepository.existsByNameAndCityNameIgnoreCase("PVR Andheri", "Mumbai"))
                .thenReturn(true);

        assertThrows(
                InvalidBookingRequestException.class,
                () -> theatreService.onboardTheatre(new TheatreOnboardingRequest("PVR Andheri", "Mumbai"))
        );
    }
}
