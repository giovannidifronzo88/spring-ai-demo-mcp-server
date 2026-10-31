package com.demo.spring_ai_demo_mcp_server.model;

/**
 * Nome e indirizzo dell'hotel scelto per il calcolo del costo totale.
 */
public record HotelAddress(String hotelName, StayAddress address) {
}
