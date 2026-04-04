package com.example.booking.controller;

import com.example.booking.model.Seat;
import com.example.booking.model.SeatInventoryRequest;
import com.example.booking.model.Show;
import com.example.booking.model.ShowRequest;
import com.example.booking.service.ShowService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partners/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Show createShow(@RequestBody ShowRequest request) {
        return showService.createShow(request);
    }

    @PostMapping("/{showId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Seat> allocateSeats(
            @PathVariable Long showId,
            @RequestBody SeatInventoryRequest request) {
        return showService.allocateSeats(showId, request);
    }
}
