package com.demo.spring_ai_demo_mcp_server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record FlightOffer(String id,
                          @JsonProperty("total_amount") BigDecimal totalAmount,
                          @JsonProperty("total_currency") String totalCurrency) {
}
