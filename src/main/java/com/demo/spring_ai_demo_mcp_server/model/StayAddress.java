package com.demo.spring_ai_demo_mcp_server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Indirizzo postale di un hotel restituito da Duffel Stays.
 */
public record StayAddress(@JsonProperty("line_one") String lineOne,
                          @JsonProperty("city_name") String cityName,
                          @JsonProperty("postal_code") String postalCode,
                          @JsonProperty("country_code") String countryCode) {
}
