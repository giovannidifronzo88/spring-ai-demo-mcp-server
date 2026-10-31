package com.demo.spring_ai_demo_mcp_server.model;

import java.util.List;

/**
 * Risultati della ricerca soggiorni (Stays).
 */
public record DuffelStaysResults(List<StayResult> results) {
}
