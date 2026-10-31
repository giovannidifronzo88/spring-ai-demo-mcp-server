package com.demo.spring_ai_demo_mcp_server.tool;

import com.demo.spring_ai_demo_mcp_server.model.FlightSearch;
import com.demo.spring_ai_demo_mcp_server.model.StaySearch;
import com.demo.spring_ai_demo_mcp_server.model.TravelCostResult;
import com.demo.spring_ai_demo_mcp_server.service.TravelCostService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class DuffelTravelTool {

    private final TravelCostService travelCostService;

    public DuffelTravelTool(TravelCostService travelCostService) {
        this.travelCostService = travelCostService;
    }

    @Tool(name = "travelCost",
            description = "Calcola il costo totale di un viaggio: la tariffa aerea piu' economica (economy) "
            + "piu' la camera d'hotel piu' economica, nella stessa valuta, usando le offerte Duffel.")
    public TravelCostResult getTravelCost(
            @ToolParam(description = "Parametri del volo: aeroporti IATA a 3 lettere, date, passeggeri adulti") FlightSearch flight,
            @ToolParam(description = "Parametri dell'hotel: coordinate, raggio in km, date di check-in e check-out, ospiti") StaySearch stay) {
        return travelCostService.calculateTotal(flight, stay);
    }
}
