package com.example.booking.config;

import com.example.booking.model.Show;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@ConfigurationProperties(prefix = "booking.pricing")
public class PricingProperties {

    private List<String> thirdTicketOfferCities = new ArrayList<>();
    private List<String> thirdTicketOfferTheatres = new ArrayList<>();

    public List<String> getThirdTicketOfferCities() {
        return thirdTicketOfferCities;
    }

    public void setThirdTicketOfferCities(List<String> thirdTicketOfferCities) {
        this.thirdTicketOfferCities = thirdTicketOfferCities;
    }

    public List<String> getThirdTicketOfferTheatres() {
        return thirdTicketOfferTheatres;
    }

    public void setThirdTicketOfferTheatres(List<String> thirdTicketOfferTheatres) {
        this.thirdTicketOfferTheatres = thirdTicketOfferTheatres;
    }

    public boolean isThirdTicketOfferEligible(Show show) {
        boolean hasCityRules = !thirdTicketOfferCities.isEmpty();
        boolean hasTheatreRules = !thirdTicketOfferTheatres.isEmpty();

        if (!hasCityRules && !hasTheatreRules) {
            return false;
        }

        boolean cityMatch = matchesConfiguredValue(show.getTheatre().getCity().getName(), thirdTicketOfferCities);
        boolean theatreMatch = matchesConfiguredValue(show.getTheatre().getName(), thirdTicketOfferTheatres);

        if (hasCityRules && hasTheatreRules) {
            return cityMatch && theatreMatch;
        }
        if (hasCityRules) {
            return cityMatch;
        }
        return theatreMatch;
    }

    private boolean matchesConfiguredValue(String candidate, List<String> configuredValues) {
        String normalizedCandidate = normalize(candidate);
        return configuredValues.stream()
                .map(this::normalize)
                .anyMatch(normalizedCandidate::equals);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
