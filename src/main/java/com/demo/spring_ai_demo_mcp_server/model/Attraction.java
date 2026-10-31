package com.demo.spring_ai_demo_mcp_server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Attraction(
        String name,
        @JsonProperty("dist") double distanceMeters,
        String kinds) {
}
