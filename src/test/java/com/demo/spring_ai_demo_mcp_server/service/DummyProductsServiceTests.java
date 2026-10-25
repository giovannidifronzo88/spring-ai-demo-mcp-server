package com.demo.spring_ai_demo_mcp_server.service;

import com.demo.spring_ai_demo_mcp_server.model.DummyProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DummyProductsServiceTests {

    private MockRestServiceServer server;
    private DummyProductsService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new DummyProductsService(builder);
    }

    @Test
    void returnsProductsFromResponse() {
        server.expect(requestTo("https://dummyjson.com/products"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "products": [
                            {"id": 1, "title": "Product A", "price": 10.0, "category": "beauty"},
                            {"id": 2, "title": "Product B", "price": 20.5}
                          ],
                          "total": 194,
                          "skip": 0,
                          "limit": 30
                        }
                        """, MediaType.APPLICATION_JSON));

        assertEquals(List.of(
                new DummyProduct(1L, "Product A", 10.0),
                new DummyProduct(2L, "Product B", 20.5)
        ), service.getDummyProducts());
        server.verify();
    }

    @Test
    void returnsEmptyProductsList() {
        server.expect(requestTo("https://dummyjson.com/products"))
                .andRespond(withSuccess("{\"products\": []}", MediaType.APPLICATION_JSON));

        assertEquals(List.of(), service.getDummyProducts());
        server.verify();
    }

    @Test
    void rejectsMissingProductsList() {
        server.expect(requestTo("https://dummyjson.com/products"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, service::getDummyProducts);
        server.verify();
    }

    @Test
    void rejectsEmptyResponse() {
        server.expect(requestTo("https://dummyjson.com/products"))
                .andRespond(withSuccess());

        assertThrows(IllegalStateException.class, service::getDummyProducts);
        server.verify();
    }

    @Test
    void propagatesHttpErrors() {
        server.expect(requestTo("https://dummyjson.com/products"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(HttpServerErrorException.class, service::getDummyProducts);
        server.verify();
    }
}
