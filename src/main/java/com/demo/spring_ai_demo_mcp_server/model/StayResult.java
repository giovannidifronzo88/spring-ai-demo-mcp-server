package com.demo.spring_ai_demo_mcp_server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Un hotel con la tariffa piu' economica trovata per il soggiorno richiesto.
 */
public record StayResult(String id,
                         @JsonProperty("cheapest_rate_total_amount") BigDecimal cheapestRateTotalAmount,
                         @JsonProperty("cheapest_rate_currency") String cheapestRateCurrency,
                         StayAccommodation accommodation) {
}
