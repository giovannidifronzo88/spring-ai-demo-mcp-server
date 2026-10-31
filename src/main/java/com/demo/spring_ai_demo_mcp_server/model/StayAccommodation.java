package com.demo.spring_ai_demo_mcp_server.model;

/**
 * Struttura alberghiera restituita da Duffel Stays: nome e posizione.
 */
public record StayAccommodation(String name, StayLocation location) {
}
