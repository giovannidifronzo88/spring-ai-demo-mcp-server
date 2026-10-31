package com.demo.spring_ai_demo_mcp_server.model;

import java.time.LocalDate;

/**
 * Parametri di ricerca voli. Gli aeroporti sono codici IATA a 3 lettere.
 * Se returnDate e' null la ricerca e' solo andata.
 */
public record FlightSearch(String origin,
                           String destination,
                           LocalDate departureDate,
                           LocalDate returnDate,
                           int adults) {
}
