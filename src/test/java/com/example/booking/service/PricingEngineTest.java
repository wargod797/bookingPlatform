package com.example.booking.service;

import com.example.booking.config.PricingProperties;
import com.example.booking.model.City;
import com.example.booking.model.Movie;
import com.example.booking.model.Seat;
import com.example.booking.model.Show;
import com.example.booking.model.Theatre;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingEngineTest {

    @Test
    void appliesThirdTicketAndAfternoonDiscountForEligibleLocation() {
        PricingProperties properties = new PricingProperties();
        properties.setThirdTicketOfferCities(List.of("Mumbai"));
        properties.setThirdTicketOfferTheatres(List.of("PVR Andheri"));

        PricingEngine pricingEngine = new PricingEngine(properties);
        Show show = createShow("Mumbai", "PVR Andheri", LocalTime.of(13, 0), 200.0);
        List<Seat> seats = List.of(new Seat(), new Seat(), new Seat());

        double total = pricingEngine.calculatePrice(seats, show);

        assertEquals(400.0, total);
    }

    @Test
    void skipsThirdTicketDiscountForNonEligibleLocation() {
        PricingProperties properties = new PricingProperties();
        properties.setThirdTicketOfferCities(List.of("Mumbai"));
        properties.setThirdTicketOfferTheatres(List.of("PVR Andheri"));

        PricingEngine pricingEngine = new PricingEngine(properties);
        Show show = createShow("Delhi", "Downtown Screens", LocalTime.of(18, 0), 150.0);
        List<Seat> seats = List.of(new Seat(), new Seat(), new Seat());

        double total = pricingEngine.calculatePrice(seats, show);

        assertEquals(450.0, total);
    }

    private Show createShow(String cityName, String theatreName, LocalTime showTime, double price) {
        City city = City.builder().name(cityName).build();
        Theatre theatre = Theatre.builder().name(theatreName).city(city).build();
        Movie movie = new Movie(1L, "Interstellar", "Sci-Fi", "English");

        return Show.builder()
                .id(1L)
                .movie(movie)
                .theatre(theatre)
                .showDate(LocalDate.now())
                .showTime(showTime)
                .price(price)
                .build();
    }
}
