package com.example.booking.controller;

import com.example.booking.model.Show;
import com.example.booking.service.BrowseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/browse")
public class BrowseController {

    @Autowired
    private BrowseService browseService;

    @GetMapping("/shows")
    public List<Show> getShows(
            @RequestParam Long movieId,
            @RequestParam String city,
            @RequestParam String date) {

        return browseService.getShows(
                movieId,
                city,
                LocalDate.parse(date)
        );
    }
}
