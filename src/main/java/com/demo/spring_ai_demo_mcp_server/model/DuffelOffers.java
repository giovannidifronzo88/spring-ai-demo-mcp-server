package com.demo.spring_ai_demo_mcp_server.model;

import java.util.List;

/**
 * Offerte volo restituite da una offer request con return_offers=true.
 */
public record DuffelOffers(List<FlightOffer> offers) {
}
