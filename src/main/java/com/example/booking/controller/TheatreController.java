package com.example.booking.controller;

import com.example.booking.model.Theatre;
import com.example.booking.model.TheatreOnboardingRequest;
import com.example.booking.model.TheatreResponse;
import com.example.booking.service.TheatreService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partners/theatres")
public class TheatreController {

    private final TheatreService theatreService;

    public TheatreController(TheatreService theatreService) {
        this.theatreService = theatreService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TheatreResponse onboardTheatre(@RequestBody TheatreOnboardingRequest request) {
        Theatre theatre = theatreService.onboardTheatre(request);
        return TheatreResponse.from(theatre);
    }

    @GetMapping
    public List<TheatreResponse> getTheatres(@RequestParam(required = false) String city) {
        return theatreService.getTheatres(city);
    }

    @GetMapping("/cities")
    public List<String> getPartnerCities() {
        return theatreService.getPartnerCities();
    }
}
