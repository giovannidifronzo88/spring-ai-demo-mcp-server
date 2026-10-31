package com.demo.spring_ai_demo_mcp_server.service;

import com.demo.spring_ai_demo_mcp_server.model.Attraction;
import com.demo.spring_ai_demo_mcp_server.model.GeoPlace;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItineraryService {

    private static final int SEARCH_RADIUS_METERS = 10_000;
    private static final int MAX_ATTRACTIONS = 20;

    private final OpenTripMapApiClient openTripMapApiClient;

    public ItineraryService(OpenTripMapApiClient openTripMapApiClient) {
        this.openTripMapApiClient = openTripMapApiClient;
    }

    public List<Attraction> attractionsFor(String city, String country) {
        GeoPlace place = openTripMapApiClient.findPlace(city, country);
        return openTripMapApiClient.attractionsAround(place.lat(), place.lon(), SEARCH_RADIUS_METERS, MAX_ATTRACTIONS);
    }
}
