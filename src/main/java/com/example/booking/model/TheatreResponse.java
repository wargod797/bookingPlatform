package com.example.booking.model;

public record TheatreResponse(
        Long id,
        String theatreName,
        String cityName
) {

    public static TheatreResponse from(Theatre theatre) {
        return new TheatreResponse(
                theatre.getId(),
                theatre.getName(),
                theatre.getCity().getName()
        );
    }
}
