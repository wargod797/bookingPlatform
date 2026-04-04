package com.example.booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PageController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("highlights", List.of(
                new Highlight("Spring Frontend", "Rendered with Thymeleaf on top of the existing Spring Boot backend."),
                new Highlight("Admin + Customer Roles", "The homepage now separates platform management from customer booking journeys."),
                new Highlight("Live API Console", "Each form calls the real endpoints and streams the response into the UI.")
        ));

        model.addAttribute("journeySteps", List.of(
                new JourneyStep("01", "Onboard theatre partners", "Seed theatre and city data before publishing shows."),
                new JourneyStep("02", "Create movies and showtimes", "Configure movie catalog entries and attach them to theatres."),
                new JourneyStep("03", "Allocate seat inventory", "Register seat numbers so the booking flow has something to reserve."),
                new JourneyStep("04", "Browse and book", "Search by movie, city, and date, then complete the ticket purchase.")
        ));

        model.addAttribute("scenarioCards", List.of(
                new ScenarioCard(
                        "mumbai",
                        "Offer Location",
                        "Mumbai Matinee",
                        "PVR Andheri at 13:00 applies both discounts for a 200.0 total on three seats.",
                        "/images/interstellar-poster.svg"
                ),
                new ScenarioCard(
                        "delhi",
                        "Afternoon Only",
                        "Delhi Matinee",
                        "Downtown Screens at 13:00 skips the location offer and lands at 240.0.",
                        "/images/inception-poster.svg"
                )
        ));

        model.addAttribute("endpointGroups", List.of(
                new EndpointGroup(
                        "Partner APIs",
                        "Setup endpoints used to prepare theatres, shows, and seat inventory.",
                        List.of(
                                new EndpointItem("POST", "/partners/theatres", "Onboard a theatre in a city."),
                                new EndpointItem("GET", "/partners/theatres/cities", "List onboarded cities for admin dropdowns."),
                                new EndpointItem("GET", "/partners/theatres?city=...", "List onboarded theatres for the selected city."),
                                new EndpointItem("POST", "/partners/shows", "Create a show for a movie and theatre."),
                                new EndpointItem("POST", "/partners/shows/{showId}/seats", "Allocate seat numbers for a show.")
                        )
                ),
                new EndpointGroup(
                        "Customer APIs",
                        "Read and write endpoints that power the browsing and booking journey.",
                        List.of(
                                new EndpointItem("GET", "/browse/shows", "Browse by movie, city, and date."),
                                new EndpointItem("GET", "/shows/{showId}/seats", "Load seat availability for the selected show."),
                                new EndpointItem("POST", "/bookings", "Book seats for a selected show."),
                                new EndpointItem("GET", "/bookings/{id}", "Fetch a booking by id.")
                        )
                ),
                new EndpointGroup(
                        "Directory APIs",
                        "Catalog and location endpoints that feed the customer discovery flow.",
                        List.of(
                                new EndpointItem("POST", "/movies", "Create a movie entry."),
                                new EndpointItem("GET", "/movies", "List movies with optional filters."),
                                new EndpointItem("GET", "/movies/{id}", "Fetch a movie and exercise the cache path."),
                                new EndpointItem("GET", "/ui/locations/countries", "Load countries for the customer location form."),
                                new EndpointItem("GET", "/ui/locations/cities?country=...", "Load cities from the online-backed directory.")
                        )
                )
        ));

        return "index";
    }

    public record Highlight(String title, String description) {
    }

    public record JourneyStep(String number, String title, String description) {
    }

    public record ScenarioCard(
            String presetName,
            String eyebrow,
            String title,
            String description,
            String imagePath) {
    }

    public record EndpointGroup(String title, String description, List<EndpointItem> items) {
    }

    public record EndpointItem(String method, String path, String description) {
    }
}
