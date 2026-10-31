package com.demo.spring_ai_demo_mcp_server.service;

import com.demo.spring_ai_demo_mcp_server.model.Attraction;
import com.demo.spring_ai_demo_mcp_server.model.GeoPlace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
public class OpenTripMapApiClient {

    private final RestClient restClient;
    private final String apiKey;

    public OpenTripMapApiClient(RestClient.Builder restClientBuilder,
                                @Value("${open.trip.map.api.key:}") String apiKey,
                                @Value("${opentripmap.api.base-url:https://api.opentripmap.com}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public GeoPlace findPlace(String city, String country) {
        GeoPlace place = get(
                "/0.1/en/places/geoname?name={name}&country={country}&apikey={apikey}",
                new ParameterizedTypeReference<GeoPlace>() {},
                city, country);
        if (place == null || place.lat() == null || place.lon() == null) {
            throw new IllegalArgumentException("Luogo non trovato su OpenTripMap: " + city + " (" + country + ")");
        }
        return place;
    }

    public List<Attraction> attractionsAround(double lat, double lon, int radiusMeters, int limit) {
        List<Attraction> attractions = get(
                "/0.1/en/places/radius?radius={radius}&lon={lon}&lat={lat}&limit={limit}&format=json&apikey={apikey}",
                new ParameterizedTypeReference<List<Attraction>>() {},
                radiusMeters, lon, lat, limit);
        return attractions == null ? List.of() : attractions;
    }

    private <T> T get(String uriTemplate, ParameterizedTypeReference<T> responseType, Object... variables) {
        requireApiKey();
        Object[] allVariables = withApiKey(variables);
        try {
            return restClient.get()
                    .uri(uriTemplate, allVariables)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException exception) {
            throw openTripMapHttpError(exception);
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Impossibile contattare OpenTripMap (timeout o rete): "
                    + exception.getMessage(), exception);
        }
    }

    private Object[] withApiKey(Object[] variables) {
        Object[] allVariables = new Object[variables.length + 1];
        System.arraycopy(variables, 0, allVariables, 0, variables.length);
        allVariables[variables.length] = apiKey;
        return allVariables;
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Chiave OpenTripMap non configurata: imposta la variabile OPEN_TRIP_MAP_API_KEY");
        }
    }

    private RuntimeException openTripMapHttpError(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401 || status == 403) {
            return new OpenTripMapAccessDeniedException(status, exception.getResponseBodyAsString(), exception);
        }
        return new IllegalStateException("OpenTripMap ha risposto con errore " + status + ": "
                + exception.getResponseBodyAsString(), exception);
    }
}
