package com.demo.spring_ai_demo_mcp_server.tool;

import com.demo.spring_ai_demo_mcp_server.model.Attraction;
import com.demo.spring_ai_demo_mcp_server.model.ItineraryPlan;
import com.demo.spring_ai_demo_mcp_server.service.ItineraryService;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItineraryToolTest {

    private static final List<Attraction> ATTRACTIONS = List.of(
            new Attraction("Castel Nuovo", 512.7, "castles"),
            new Attraction("Napoli Sotterranea", 830.2, "historic"));

    @Mock
    private ItineraryService itineraryService;

    @Mock
    private McpSyncServerExchange exchange;

    @Test
    void returnsAttractionsWithWarningWhenClientHasNoSampling() {
        when(itineraryService.attractionsFor("Naples", "IT")).thenReturn(ATTRACTIONS);
        when(exchange.getClientCapabilities()).thenReturn(McpSchema.ClientCapabilities.builder().build());

        ItineraryPlan plan = new ItineraryTool(itineraryService).planItinerary("Naples", "IT", 2, exchange);

        assertNull(plan.itinerary());
        assertEquals(ATTRACTIONS, plan.attractions());
        assertEquals(1, plan.warnings().size());
    }

    @Test
    void returnsItineraryFromSamplingWhenClientSupportsIt() {
        when(itineraryService.attractionsFor("Naples", "IT")).thenReturn(ATTRACTIONS);
        when(exchange.getClientCapabilities()).thenReturn(samplingCapabilities());
        when(exchange.createMessage(any(McpSchema.CreateMessageRequest.class)))
                .thenReturn(new McpSchema.CreateMessageResult(
                        McpSchema.Role.ASSISTANT,
                        new McpSchema.TextContent("Giorno 1: Castel Nuovo"),
                        "test-model",
                        McpSchema.CreateMessageResult.StopReason.END_TURN));

        ItineraryPlan plan = new ItineraryTool(itineraryService).planItinerary("Naples", "IT", 2, exchange);

        assertEquals("Giorno 1: Castel Nuovo", plan.itinerary());
        assertTrue(plan.warnings().isEmpty());
    }

    @Test
    void keepsAttractionsWhenSamplingFails() {
        when(itineraryService.attractionsFor("Naples", "IT")).thenReturn(ATTRACTIONS);
        when(exchange.getClientCapabilities()).thenReturn(samplingCapabilities());
        when(exchange.createMessage(any(McpSchema.CreateMessageRequest.class)))
                .thenThrow(new IllegalStateException("client disconnected"));

        ItineraryPlan plan = new ItineraryTool(itineraryService).planItinerary("Naples", "IT", 2, exchange);

        assertNull(plan.itinerary());
        assertEquals(ATTRACTIONS, plan.attractions());
        assertFalse(plan.warnings().isEmpty());
    }

    @Test
    void warnsWhenNoAttractionsAreFound() {
        when(itineraryService.attractionsFor("Nowhere", "IT")).thenReturn(List.of());

        ItineraryPlan plan = new ItineraryTool(itineraryService).planItinerary("Nowhere", "IT", 2, exchange);

        assertNull(plan.itinerary());
        assertTrue(plan.attractions().isEmpty());
        assertEquals(1, plan.warnings().size());
    }

    private McpSchema.ClientCapabilities samplingCapabilities() {
        return McpSchema.ClientCapabilities.builder().sampling().build();
    }
}
