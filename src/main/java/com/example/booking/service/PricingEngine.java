package com.example.booking.service;

import com.example.booking.model.Seat;
import com.example.booking.model.Show;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
public class PricingEngine {

    public double calculatePrice(List<Seat> seats, Show show) {

        double total = seats.size() * show.getPrice();

        // Rule 1: 50% discount on 3rd ticket
        if (seats.size() >= 3) {
            total -= show.getPrice() * 0.5;
        }

        // Rule 2: Afternoon show discount (12 PM – 4 PM)
        LocalTime time = show.getShowTime();
        if (time.isAfter(LocalTime.NOON) && time.isBefore(LocalTime.of(16, 0))) {
            total *= 0.8;
        }

        return total;
    }
}