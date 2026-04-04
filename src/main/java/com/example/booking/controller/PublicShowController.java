package com.example.booking.controller;

import com.example.booking.model.SeatAvailabilityResponse;
import com.example.booking.service.ShowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shows")
public class PublicShowController {

    private final ShowService showService;

    public PublicShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping("/{showId}/seats")
    public List<SeatAvailabilityResponse> getSeatAvailability(@PathVariable Long showId) {
        return showService.getSeatAvailability(showId);
    }
}
