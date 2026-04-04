package com.example.booking.service;

import com.example.booking.model.Show;
import com.example.booking.repository.ShowRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrowseServiceTest {

    @Test
    void delegatesBrowseQueryByMovieCityAndDate() {
        ShowRepository repository = mock(ShowRepository.class);
        BrowseService browseService = new BrowseService(repository);

        LocalDate date = LocalDate.of(2026, 4, 4);
        List<Show> expectedShows = List.of(Show.builder().id(1L).build());

        when(repository.findShowsByMovieCityAndDate(1L, "Mumbai", date))
                .thenReturn(expectedShows);

        List<Show> actualShows = browseService.getShows(1L, "Mumbai", date);

        assertSame(expectedShows, actualShows);
    }
}
