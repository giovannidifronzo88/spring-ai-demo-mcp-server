package com.demo.spring_ai_demo_mcp_server.service;

import com.demo.spring_ai_demo_mcp_server.model.Attraction;
import com.demo.spring_ai_demo_mcp_server.model.GeoPlace;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenTripMapApiClientTest {

    private static final String BASE_URL = "https://api.opentripmap.com";
    private static final String KEY = "test-key";

    @Test
    void findPlaceReturnsCoordinatesOfCity() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/0.1/en/places/geoname?name=Naples&country=IT&apikey=" + KEY))
                .andRespond(withSuccess("{\"name\":\"Naples\",\"lat\":40.85,\"lon\":14.27,\"country\":\"IT\"}",
                        MediaType.APPLICATION_JSON));

        GeoPlace place = clientWith(builder).findPlace("Naples", "IT");

        assertEquals(40.85, place.lat());
        assertEquals(14.27, place.lon());
        server.verify();
    }

    @Test
    void findPlaceFailsWhenCityIsUnknown() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/0.1/en/places/geoname?name=Nowhere&country=IT&apikey=" + KEY))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalArgumentException.class, () -> clientWith(builder).findPlace("Nowhere", "IT"));
    }

    @Test
    void attractionsAroundMapsDistanceFromResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/0.1/en/places/radius?radius=10000&lon=14.27&lat=40.85&limit=20"
                        + "&format=json&apikey=" + KEY))
                .andRespond(withSuccess("[{\"xid\":\"1\",\"name\":\"Castel Nuovo\",\"dist\":512.7,\"kinds\":\"castles\"}]",
                        MediaType.APPLICATION_JSON));

        List<Attraction> attractions = clientWith(builder).attractionsAround(40.85, 14.27, 10_000, 20);

        assertEquals(1, attractions.size());
        assertEquals("Castel Nuovo", attractions.getFirst().name());
        assertEquals(512.7, attractions.getFirst().distanceMeters());
    }

    @Test
    void forbiddenResponseThrowsAccessDenied() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/0.1/en/places/geoname?name=Naples&country=IT&apikey=" + KEY))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("{\"error\":\"invalid key\"}"));

        assertThrows(OpenTripMapAccessDeniedException.class,
                () -> clientWith(builder).findPlace("Naples", "IT"));
    }

    @Test
    void missingApiKeyFailsBeforeCallingService() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenTripMapApiClient clientWithoutKey = new OpenTripMapApiClient(builder, "", BASE_URL);

        assertThrows(IllegalStateException.class, () -> clientWithoutKey.findPlace("Naples", "IT"));
        server.verify();
    }

    private OpenTripMapApiClient clientWith(RestClient.Builder builder) {
        return new OpenTripMapApiClient(builder, KEY, BASE_URL);
    }
}
