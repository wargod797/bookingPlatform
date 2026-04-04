package com.example.booking.controller;

import com.example.booking.service.LocationDirectoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ui/locations")
public class LocationController {

    private final LocationDirectoryService locationDirectoryService;

    public LocationController(LocationDirectoryService locationDirectoryService) {
        this.locationDirectoryService = locationDirectoryService;
    }

    @GetMapping("/countries")
    public List<String> getCountries() {
        return locationDirectoryService.getCountries();
    }

    @GetMapping("/cities")
    public List<String> getCities(@RequestParam(required = false, defaultValue = "India") String country) {
        return locationDirectoryService.getCities(country);
    }
}
