package com.demo.spring_ai_demo_mcp_server.service;

/**
 * OpenTripMap ha risposto 401/403: chiave assente, non valida o non abilitata per il piano.
 */
public class OpenTripMapAccessDeniedException extends IllegalStateException {

    private final int status;

    public OpenTripMapAccessDeniedException(int status, String openTripMapMessage, Throwable cause) {
        super("OpenTripMap ha negato l'accesso (" + status + "): " + openTripMapMessage, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
