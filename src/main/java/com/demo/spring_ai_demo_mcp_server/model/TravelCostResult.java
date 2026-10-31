package com.demo.spring_ai_demo_mcp_server.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Costo totale di un viaggio (volo + soggiorno) nella stessa valuta.
 * Se un servizio non e' disponibile per permessi mancanti, il suo importo e' null
 * e il motivo e' riportato in {@code warnings}.
 */
public record TravelCostResult(String currency,
                               BigDecimal flightTotal,
                               BigDecimal stayTotal,
                               BigDecimal grandTotal,
                               String flightOfferId,
                               String stayResultId,
                               HotelAddress stayAddress,
                               List<String> warnings) {
}
