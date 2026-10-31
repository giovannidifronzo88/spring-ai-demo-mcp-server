package com.demo.spring_ai_demo_mcp_server.tool;

import com.demo.spring_ai_demo_mcp_server.model.Attraction;
import com.demo.spring_ai_demo_mcp_server.model.ItineraryPlan;
import com.demo.spring_ai_demo_mcp_server.service.ItineraryService;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItineraryTool {

    private static final String SAMPLING_UNSUPPORTED_WARNING =
            "Il client non supporta il sampling: restituite solo le attrazioni, senza itinerario";
    private static final String ITINERARY_SYSTEM_PROMPT =
            "Sei un assistente di viaggio. Componi un itinerario giorno per giorno in italiano, "
            + "usando solo le attrazioni fornite. Non inventare luoghi.";
    private static final int ITINERARY_MAX_TOKENS = 1500;

    private final ItineraryService itineraryService;

    public ItineraryTool(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @McpTool(name = "itineraryPlanner",
            description = "Recupera le attrazioni di una citta' tramite OpenTripMap e chiede al client, "
                    + "con sampling, di comporre un itinerario di piu' giorni con quelle attrazioni.")
    public ItineraryPlan planItinerary(
            @McpToolParam(description = "Nome della citta', es. Naples") String city,
            @McpToolParam(description = "Codice paese ISO a 2 lettere, es. IT") String country,
            @McpToolParam(description = "Numero di giorni dell'itinerario") int days,
            McpSyncServerExchange exchange) {
        List<Attraction> attractions = itineraryService.attractionsFor(city, country);
        if (attractions.isEmpty()) {
            return new ItineraryPlan(null, attractions, List.of("Nessuna attrazione trovata per " + city));
        }
        if (!supportsSampling(exchange)) {
            return new ItineraryPlan(null, attractions, List.of(SAMPLING_UNSUPPORTED_WARNING));
        }
        try {
            String itinerary = requestItineraryFromClient(exchange, city, days, attractions);
            return new ItineraryPlan(itinerary, attractions, List.of());
        } catch (RuntimeException exception) {
            return new ItineraryPlan(null, attractions,
                    List.of("Sampling fallito, restituite solo le attrazioni: " + exception.getMessage()));
        }
    }

    private boolean supportsSampling(McpSyncServerExchange exchange) {
        McpSchema.ClientCapabilities capabilities = exchange.getClientCapabilities();
        return capabilities != null && capabilities.sampling() != null;
    }

    private String requestItineraryFromClient(McpSyncServerExchange exchange, String city, int days,
                                              List<Attraction> attractions) {
        McpSchema.CreateMessageRequest request = McpSchema.CreateMessageRequest.builder()
                .messages(List.of(new McpSchema.SamplingMessage(
                        McpSchema.Role.USER,
                        new McpSchema.TextContent(buildItineraryPrompt(city, days, attractions)))))
                .systemPrompt(ITINERARY_SYSTEM_PROMPT)
                .maxTokens(ITINERARY_MAX_TOKENS)
                .build();
        McpSchema.CreateMessageResult result = exchange.createMessage(request);
        return textOf(result.content());
    }

    private String buildItineraryPrompt(String city, int days, List<Attraction> attractions) {
        String attractionList = attractions.stream()
                .map(this::describeAttraction)
                .collect(Collectors.joining("\n"));
        return "Crea un itinerario di " + days + " giorni a " + city + " con queste attrazioni:\n" + attractionList;
    }

    private String describeAttraction(Attraction attraction) {
        return "- " + attraction.name() + " (" + Math.round(attraction.distanceMeters()) + " m dal centro, "
                + attraction.kinds() + ")";
    }

    private String textOf(McpSchema.Content content) {
        if (content instanceof McpSchema.TextContent textContent) {
            return textContent.text();
        }
        throw new IllegalStateException("Risposta di sampling non testuale");
    }
}
