package com.example.booking.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class LocationDirectoryService {

    private static final String COUNTRY_DIRECTORY_URL = "https://countriesnow.space/api/v0.1/countries";
    private static final Map<String, List<String>> FALLBACK_DIRECTORY = createFallbackDirectory();

    private final RestClient restClient;

    public LocationDirectoryService() {
        this.restClient = RestClient.builder().build();
    }

    @Cacheable(value = "locationDirectory", key = "'all'")
    public LinkedHashMap<String, List<String>> getCountryDirectory() {
        try {
            CountriesNowResponse response = restClient.get()
                    .uri(COUNTRY_DIRECTORY_URL)
                    .retrieve()
                    .body(CountriesNowResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                return new LinkedHashMap<>(FALLBACK_DIRECTORY);
            }

            TreeMap<String, List<String>> sortedDirectory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (CountryDirectoryItem item : response.data()) {
                if (item == null || item.country() == null || item.country().isBlank() || item.cities() == null || item.cities().isEmpty()) {
                    continue;
                }

                List<String> cleanedCities = item.cities().stream()
                        .filter(city -> city != null && !city.isBlank())
                        .map(String::trim)
                        .distinct()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();

                if (!cleanedCities.isEmpty()) {
                    sortedDirectory.put(item.country().trim(), cleanedCities);
                }
            }

            if (sortedDirectory.isEmpty()) {
                return new LinkedHashMap<>(FALLBACK_DIRECTORY);
            }

            return new LinkedHashMap<>(sortedDirectory);
        } catch (Exception ex) {
            return new LinkedHashMap<>(FALLBACK_DIRECTORY);
        }
    }

    @Cacheable(value = "locationCountries", key = "'all'")
    public List<String> getCountries() {
        return new ArrayList<>(getCountryDirectory().keySet());
    }

    @Cacheable(value = "locationCities", key = "#country == null || #country.isBlank() ? 'india' : #country.trim().toLowerCase()")
    public List<String> getCities(String country) {
        String requestedCountry = normalize(country);
        if (requestedCountry.isEmpty()) {
            return getCountryDirectory().getOrDefault("India", List.of());
        }

        Optional<Map.Entry<String, List<String>>> match = getCountryDirectory().entrySet().stream()
                .filter(entry -> normalize(entry.getKey()).equals(requestedCountry))
                .findFirst();

        return match.map(Map.Entry::getValue)
                .orElseGet(() -> getCountryDirectory().getOrDefault("India", List.of()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, List<String>> createFallbackDirectory() {
        LinkedHashMap<String, List<String>> fallback = new LinkedHashMap<>();
        fallback.put("India", List.of("Bengaluru", "Chennai", "Delhi", "Hyderabad", "Kolkata", "Mumbai", "Pune"));
        fallback.put("United Arab Emirates", List.of("Abu Dhabi", "Ajman", "Dubai", "Sharjah"));
        fallback.put("United Kingdom", List.of("Birmingham", "Edinburgh", "Leeds", "London", "Manchester"));
        fallback.put("United States", List.of("Chicago", "Los Angeles", "New York", "San Francisco", "Seattle"));
        return fallback;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CountriesNowResponse(List<CountryDirectoryItem> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CountryDirectoryItem(String country, List<String> cities) {
    }
}
