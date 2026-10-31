package com.demo.spring_ai_demo_mcp_server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoPlace(String name, Double lat, Double lon) {
}
