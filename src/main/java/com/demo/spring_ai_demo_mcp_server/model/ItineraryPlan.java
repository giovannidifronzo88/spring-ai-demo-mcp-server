package com.demo.spring_ai_demo_mcp_server.model;

import java.util.List;

public record ItineraryPlan(String itinerary, List<Attraction> attractions, List<String> warnings) {
}
