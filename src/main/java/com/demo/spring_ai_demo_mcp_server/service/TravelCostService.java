package com.demo.spring_ai_demo_mcp_server.service;

import com.demo.spring_ai_demo_mcp_server.model.FlightOffer;
import com.demo.spring_ai_demo_mcp_server.model.FlightSearch;
import com.demo.spring_ai_demo_mcp_server.model.HotelAddress;
import com.demo.spring_ai_demo_mcp_server.model.StayAccommodation;
import com.demo.spring_ai_demo_mcp_server.model.StayResult;
import com.demo.spring_ai_demo_mcp_server.model.StaySearch;
import com.demo.spring_ai_demo_mcp_server.model.TravelCostResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TravelCostService {

    private final DuffelApiClient duffelApiClient;

    public TravelCostService(DuffelApiClient duffelApiClient) {
        this.duffelApiClient = duffelApiClient;
    }

    public TravelCostResult calculateTotal(FlightSearch flightSearch, StaySearch staySearch) {
        List<String> warnings = new ArrayList<>();
        FlightOffer flightOffer = cheapestFlightOffer(flightSearch, warnings);
        StayResult stayResult = cheapestStay(staySearch, warnings);
        if (flightOffer == null && stayResult == null) {
            throw new IllegalStateException(String.join(" ", warnings));
        }

        BigDecimal flightTotal = flightAmountOf(flightOffer);
        BigDecimal stayTotal = stayAmountOf(stayResult);
        TravelCostResult result = new TravelCostResult(
                commonCurrency(flightOffer, stayResult),
                flightTotal,
                stayTotal,
                sumOfAvailable(flightTotal, stayTotal),
                flightOffer == null ? null : flightOffer.id(),
                stayResult == null ? null : stayResult.id(),
                stayResult == null ? null : hotelAddressOf(stayResult),
                warnings);
        return result;
    }

    private FlightOffer cheapestFlightOffer(FlightSearch flightSearch, List<String> warnings) {
        try {
            return cheapestFlightOffer(duffelApiClient.searchFlightOffers(flightSearch));
        } catch (DuffelAccessDeniedException exception) {
            warnings.add(accessWarning("voli", exception));
            return null;
        }
    }

    private StayResult cheapestStay(StaySearch staySearch, List<String> warnings) {
        try {
            return cheapestStay(duffelApiClient.searchStays(staySearch));
        } catch (DuffelAccessDeniedException exception) {
            warnings.add(accessWarning("hotel", exception));
            return null;
        }
    }

    private String accessWarning(String serviceName, DuffelAccessDeniedException exception) {
        return "Permessi mancanti per i " + serviceName + " (" + exception.endpoint() + "): "
                + exception.getMessage() + ". Il totale non include questo servizio.";
    }

    private BigDecimal flightAmountOf(FlightOffer flightOffer) {
        return flightOffer == null ? null : flightOffer.totalAmount();
    }

    private BigDecimal stayAmountOf(StayResult stayResult) {
        return stayResult == null ? null : stayResult.cheapestRateTotalAmount();
    }

    private BigDecimal sumOfAvailable(BigDecimal flightTotal, BigDecimal stayTotal) {
        BigDecimal total = BigDecimal.ZERO;
        if (flightTotal != null) {
            total = total.add(flightTotal);
        }
        if (stayTotal != null) {
            total = total.add(stayTotal);
        }
        return total;
    }

    private HotelAddress hotelAddressOf(StayResult stayResult) {
        StayAccommodation accommodation = stayResult.accommodation();
        if (accommodation == null || accommodation.location() == null) {
            return null;
        }
        HotelAddress hotelAddress = new HotelAddress(accommodation.name(), accommodation.location().address());
        return hotelAddress;
    }

    private FlightOffer cheapestFlightOffer(List<FlightOffer> offers) {
        return offers.stream()
                .min(Comparator.comparing(FlightOffer::totalAmount))
                .orElseThrow(() -> new IllegalStateException("Nessuna offerta volo disponibile per la ricerca"));
    }

    private StayResult cheapestStay(List<StayResult> stays) {
        return stays.stream()
                .min(Comparator.comparing(StayResult::cheapestRateTotalAmount))
                .orElseThrow(() -> new IllegalStateException("Nessun soggiorno disponibile per la ricerca"));
    }

    // Il totale si puo' calcolare solo se volo e hotel usano la stessa valuta: non c'e' conversione.
    // Se uno dei due manca (permessi negati) la valuta e' quella dell'altro.
    private String commonCurrency(FlightOffer flightOffer, StayResult stayResult) {
        if (flightOffer == null) {
            return stayResult.cheapestRateCurrency();
        }
        if (stayResult == null) {
            return flightOffer.totalCurrency();
        }
        if (!flightOffer.totalCurrency().equals(stayResult.cheapestRateCurrency())) {
            throw new IllegalStateException("Valute diverse: volo " + flightOffer.totalCurrency()
                    + ", soggiorno " + stayResult.cheapestRateCurrency());
        }
        return flightOffer.totalCurrency();
    }
}
