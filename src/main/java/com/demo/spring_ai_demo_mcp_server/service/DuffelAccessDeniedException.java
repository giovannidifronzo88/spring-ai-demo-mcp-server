package com.demo.spring_ai_demo_mcp_server.service;

/**
 * Duffel ha risposto 403: la funzionalita' non e' abilitata per l'account.
 * Il messaggio contiene la descrizione restituita da Duffel.
 */
public class DuffelAccessDeniedException extends IllegalStateException {

    private final String endpoint;

    public DuffelAccessDeniedException(String endpoint, String duffelMessage, Throwable cause) {
        super(duffelMessage, cause);
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint;
    }
}
