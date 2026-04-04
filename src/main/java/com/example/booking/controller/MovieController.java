package com.example.booking.controller;

import com.example.booking.model.Movie;
import com.example.booking.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/movies")
@Tag(name = "Movie API", description = "Operations related to movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @Operation(summary = "List movies", description = "Fetch movies with optional genre and language filters")
    @GetMapping
    public List<Movie> getMovies(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String language) {
        return movieService.getMovies(genre, language);
    }

    @Operation(summary = "Get movie by ID", description = "Fetch a movie using its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movie found"),
            @ApiResponse(responseCode = "404", description = "Movie not found")
    })

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovie(@PathVariable Long id) {
        Optional<Movie> movie = movieService.getMovieById(id);
        return movie.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Add a new movie", description = "Create a new movie entry")
    @ApiResponse(responseCode = "200", description = "Movie created successfully")
    @PostMapping
    public Movie addMovie(
            @RequestBody Movie movie) {
        return movieService.saveMovie(movie);
    }
}
