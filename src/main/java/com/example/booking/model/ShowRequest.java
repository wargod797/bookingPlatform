package com.example.booking.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShowRequest {

    private Long movieId;
    private Long theatreId;
    private String showDate;
    private String showTime;
    private Double price;
}
