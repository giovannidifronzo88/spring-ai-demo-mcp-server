package com.demo.spring_ai_demo_mcp_server.service;

import com.demo.spring_ai_demo_mcp_server.model.FlightOffer;
import com.demo.spring_ai_demo_mcp_server.model.FlightSearch;
import com.demo.spring_ai_demo_mcp_server.model.HotelAddress;
import com.demo.spring_ai_demo_mcp_server.model.StayAccommodation;
import com.demo.spring_ai_demo_mcp_server.model.StayAddress;
import com.demo.spring_ai_demo_mcp_server.model.StayLocation;
import com.demo.spring_ai_demo_mcp_server.model.StayResult;
import com.demo.spring_ai_demo_mcp_server.model.StaySearch;
import com.demo.spring_ai_demo_mcp_server.model.TravelCostResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelCostServiceTest {

    private static final FlightSearch FLIGHT = new FlightSearch(
            "MXP", "BCN", LocalDate.of(2026, 7, 1), null, 1);
    private static final StaySearch STAY = new StaySearch(
            41.38, 2.17, 5, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4), 1);

    @Test
    void sumsCheapestFlightAndCheapestStayInSameCurrency() {
        TravelCostService service = serviceWith(
                List.of(
                        new FlightOffer("off_1", new BigDecimal("120.00"), "EUR"),
                        new FlightOffer("off_2", new BigDecimal("95.50"), "EUR")),
                List.of(
                        new StayResult("stay_1", new BigDecimal("300.00"), "EUR", null),
                        new StayResult("stay_2", new BigDecimal("250.25"), "EUR", null)));

        TravelCostResult result = service.calculateTotal(FLIGHT, STAY);

        assertEquals("EUR", result.currency());
        assertEquals(new BigDecimal("95.50"), result.flightTotal());
        assertEquals(new BigDecimal("250.25"), result.stayTotal());
        assertEquals(new BigDecimal("345.75"), result.grandTotal());
        assertEquals("off_2", result.flightOfferId());
        assertEquals("stay_2", result.stayResultId());
        assertNull(result.stayAddress());
    }

    @Test
    void returnsNameAndAddressOfCheapestStay() {
        StayAddress address = new StayAddress("Carrer de Balmes 1", "Barcelona", "08007", "ES");
        StayResult cheapest = new StayResult("stay_2", new BigDecimal("250.25"), "EUR",
                new StayAccommodation("Hotel Test", new StayLocation(address)));
        TravelCostService service = serviceWith(
                List.of(new FlightOffer("off_1", new BigDecimal("120.00"), "EUR")),
                List.of(new StayResult("stay_1", new BigDecimal("300.00"), "EUR", null), cheapest));

        TravelCostResult result = service.calculateTotal(FLIGHT, STAY);

        assertEquals(new HotelAddress("Hotel Test", address), result.stayAddress());
    }

    @Test
    void keepsFlightTotalWhenStaysAreDenied() {
        TravelCostService service = serviceWithSuppliers(
                () -> List.of(new FlightOffer("off_1", new BigDecimal("120.00"), "EUR")),
                TravelCostServiceTest::staysDenied);

        TravelCostResult result = service.calculateTotal(FLIGHT, STAY);

        assertEquals("EUR", result.currency());
        assertEquals(new BigDecimal("120.00"), result.flightTotal());
        assertNull(result.stayTotal());
        assertEquals(new BigDecimal("120.00"), result.grandTotal());
        assertNull(result.stayResultId());
        assertNull(result.stayAddress());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("hotel"));
        assertTrue(result.warnings().get(0).contains("/stays/search"));
    }

    @Test
    void keepsStayTotalWhenFlightsAreDenied() {
        TravelCostService service = serviceWithSuppliers(
                TravelCostServiceTest::flightsDenied,
                () -> List.of(new StayResult("stay_1", new BigDecimal("300.00"), "EUR", null)));

        TravelCostResult result = service.calculateTotal(FLIGHT, STAY);

        assertEquals("EUR", result.currency());
        assertNull(result.flightTotal());
        assertEquals(new BigDecimal("300.00"), result.stayTotal());
        assertEquals(new BigDecimal("300.00"), result.grandTotal());
        assertNull(result.flightOfferId());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("voli"));
        assertTrue(result.warnings().get(0).contains("/air/offer_requests"));
    }

    @Test
    void failsWhenBothServicesAreDenied() {
        TravelCostService service = serviceWithSuppliers(
                TravelCostServiceTest::flightsDenied,
                TravelCostServiceTest::staysDenied);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.calculateTotal(FLIGHT, STAY));

        assertTrue(exception.getMessage().contains("/air/offer_requests"));
        assertTrue(exception.getMessage().contains("/stays/search"));
    }

    @Test
    void failsWhenNoFlightOffers() {
        TravelCostService service = serviceWith(
                List.of(),
                List.of(new StayResult("stay_1", new BigDecimal("300.00"), "EUR", null)));

        assertThrows(IllegalStateException.class, () -> service.calculateTotal(FLIGHT, STAY));
    }

    @Test
    void failsWhenNoStays() {
        TravelCostService service = serviceWith(
                List.of(new FlightOffer("off_1", new BigDecimal("120.00"), "EUR")),
                List.of());

        assertThrows(IllegalStateException.class, () -> service.calculateTotal(FLIGHT, STAY));
    }

    @Test
    void failsWhenCurrenciesDiffer() {
        TravelCostService service = serviceWith(
                List.of(new FlightOffer("off_1", new BigDecimal("120.00"), "EUR")),
                List.of(new StayResult("stay_1", new BigDecimal("300.00"), "USD", null)));

        assertThrows(IllegalStateException.class, () -> service.calculateTotal(FLIGHT, STAY));
    }

    private TravelCostService serviceWith(List<FlightOffer> offers, List<StayResult> stays) {
        return serviceWithSuppliers(() -> offers, () -> stays);
    }

    private static List<FlightOffer> flightsDenied() {
        throw new DuffelAccessDeniedException("/air/offer_requests", "Feature not enabled", null);
    }

    private static List<StayResult> staysDenied() {
        throw new DuffelAccessDeniedException("/stays/search", "Feature not enabled", null);
    }

    private TravelCostService serviceWithSuppliers(Supplier<List<FlightOffer>> offers,
                                                   Supplier<List<StayResult>> stays) {
        DuffelApiClient fakeClient = new DuffelApiClient(RestClient.builder(), "test-token", "http://localhost") {
            @Override
            public List<FlightOffer> searchFlightOffers(FlightSearch search) {
                return offers.get();
            }

            @Override
            public List<StayResult> searchStays(StaySearch search) {
                return stays.get();
            }
        };
        return new TravelCostService(fakeClient);
    }
}
