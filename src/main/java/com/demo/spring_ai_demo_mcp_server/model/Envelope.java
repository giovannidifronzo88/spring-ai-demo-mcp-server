package com.demo.spring_ai_demo_mcp_server.model;

/**
 * Involucro delle risposte Duffel: il payload utile e' sempre sotto la chiave "data".
 */
public record Envelope<T>(T data) {
}
