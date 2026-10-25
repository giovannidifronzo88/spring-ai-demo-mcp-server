package com.demo.spring_ai_demo_mcp_server.tool;

import com.demo.spring_ai_demo_mcp_server.model.DummyProduct;
import com.demo.spring_ai_demo_mcp_server.service.DummyProductsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DummyProductsTool {

    private final DummyProductsService dummyProductsService;

    public DummyProductsTool(DummyProductsService dummyProductsService) {
        this.dummyProductsService = dummyProductsService;
    }

    @Tool(description = "Retrieve all products")
    public List<DummyProduct> getDummyProducts() {
        return dummyProductsService.getDummyProducts().stream().toList();
    }
}
