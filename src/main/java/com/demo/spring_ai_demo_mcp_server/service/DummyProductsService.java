package com.demo.spring_ai_demo_mcp_server.service;

import com.demo.spring_ai_demo_mcp_server.model.DummyProduct;
import com.demo.spring_ai_demo_mcp_server.model.ProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class DummyProductsService {

    public final RestClient restClient;

    public DummyProductsService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public List<DummyProduct> getDummyProducts() {
        ProductResponse response = restClient.get()
                .uri("https://dummyjson.com/products")
                .retrieve()
                .body(ProductResponse.class);

        if (response == null || response.products() == null) {
            throw new IllegalStateException("DummyJSON response is missing the products list");
        }

        return response.products();
    }
}
