package com.demo.spring_ai_demo_mcp_server.model;

import java.util.List;

public record DummyProduct(
        Long id,
        String title,
        String description,
        String category,
        String brand,
        Double price,
        Double discountPercentage,
        Double rating,
        Integer stock,
        String availabilityStatus,
        List<String> tags
) {
}
