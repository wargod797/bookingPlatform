package com.example.booking.service;

import com.example.booking.config.PricingProperties;
import com.example.booking.model.Seat;
import com.example.booking.model.Show;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
public class PricingEngine {

    private final PricingProperties pricingProperties;

    public PricingEngine(PricingProperties pricingProperties) {
        this.pricingProperties = pricingProperties;
    }

    public double calculatePrice(List<Seat> seats, Show show) {
        double total = seats.size() * show.getPrice();

        if (seats.size() >= 3 && pricingProperties.isThirdTicketOfferEligible(show)) {
            total -= show.getPrice() * 0.5;
        }

        LocalTime time = show.getShowTime();
        if (!time.isBefore(LocalTime.NOON) && time.isBefore(LocalTime.of(16, 0))) {
            total *= 0.8;
        }

        return total;
    }
}
