package com.demo.spring_ai_demo_mcp_server.model;

import java.time.LocalDate;

/**
 * Parametri di ricerca soggiorni: l'area e' un cerchio di raggio radiusKm attorno alle coordinate.
 */
public record StaySearch(double latitude,
                         double longitude,
                         double radiusKm,
                         LocalDate checkIn,
                         LocalDate checkOut,
                         int guests) {
}
