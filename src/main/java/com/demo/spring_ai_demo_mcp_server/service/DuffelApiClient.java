package com.demo.spring_ai_demo_mcp_server.service;

import com.demo.spring_ai_demo_mcp_server.model.Envelope;
import com.demo.spring_ai_demo_mcp_server.model.DuffelOffers;
import com.demo.spring_ai_demo_mcp_server.model.DuffelStaysResults;
import com.demo.spring_ai_demo_mcp_server.model.FlightOffer;
import com.demo.spring_ai_demo_mcp_server.model.FlightSearch;
import com.demo.spring_ai_demo_mcp_server.model.StaySearch;
import com.demo.spring_ai_demo_mcp_server.model.StayResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class DuffelApiClient {

    private static final String DUFFEL_VERSION = "v2";

    private final RestClient restClient;
    private final String token;

    public DuffelApiClient(RestClient.Builder restClientBuilder,
                           @Value("${duffel.api.token:}") String token,
                           @Value("${duffel.api.base-url:https://api.duffel.com}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.token = token;
    }

    public List<FlightOffer> searchFlightOffers(FlightSearch search) {
        Envelope<DuffelOffers> envelope = post(
                "/air/offer_requests?return_offers=true",
                flightRequestBody(search),
                new ParameterizedTypeReference<>() {});
        return envelope.data() == null || envelope.data().offers() == null ? new ArrayList<>() : envelope.data().offers();

    }

    public List<StayResult> searchStays(StaySearch search) {
        Envelope<DuffelStaysResults> envelope = post(
                "/stays/search",
                stayRequestBody(search),
                new ParameterizedTypeReference<>() {});
        return envelope.data() == null || envelope.data().results() == null ? new ArrayList<>() : envelope.data().results();
    }

    private <T> Envelope<T> post(String uri, Map<String, Object> body,
                                 ParameterizedTypeReference<Envelope<T>> responseType) {
        Envelope<T> envelope;
        String token = requireToken();
        try {
            envelope = restClient.post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Duffel-Version", DUFFEL_VERSION)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException exception) {
            throw duffelHttpError(uri, exception);
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Impossibile contattare Duffel per " + uri
                    + " (timeout o rete): " + exception.getMessage(), exception);
        }
        if (envelope == null) {
            throw new IllegalStateException("Risposta Duffel vuota per " + uri);
        }
        return envelope;
    }

    private IllegalStateException duffelHttpError(String uri, RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 403) {
            return new DuffelAccessDeniedException(uri, exception.getResponseBodyAsString(), exception);
        }
        return new IllegalStateException("Duffel ha risposto con errore " + exception.getStatusCode().value()
                + " per " + uri + ": " + exception.getResponseBodyAsString(), exception);
    }

    private String requireToken() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Token Duffel non configurato: imposta la variabile DUFFEL_API_TOKEN");
        }
        return token;
    }


    private Map<String, Object> flightRequestBody(FlightSearch search) {
        List<Map<String, Object>> slices = new ArrayList<>();
        slices.add(slice(search.origin(), search.destination(), search.departureDate()));
        if (search.returnDate() != null) {
            slices.add(slice(search.destination(), search.origin(), search.returnDate()));
        }
        return Map.of("data", Map.of(
                "slices", slices,
                "passengers", adultPassengers(search.adults()),
                "cabin_class", "economy"));
    }

    private Map<String, Object> slice(String origin, String destination, LocalDate departureDate) {
        return Map.of(
                "origin", origin,
                "destination", destination,
                "departure_date", departureDate.toString());
    }

    private Map<String, Object> stayRequestBody(StaySearch search) {
        Map<String, Object> coordinates = Map.of(
                "latitude", search.latitude(),
                "longitude", search.longitude());
        Map<String, Object> location = Map.of(
                "radius", search.radiusKm(),
                "geographic_coordinates", coordinates);
        return Map.of("data", Map.of(
                "check_in_date", search.checkIn().toString(),
                "check_out_date", search.checkOut().toString(),
                "rooms", 1,
                "guests", adultPassengers(search.guests()),
                "location", location));
    }

    private List<Map<String, Object>> adultPassengers(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> Map.<String, Object>of("type", "adult"))
                .toList();
    }
}
