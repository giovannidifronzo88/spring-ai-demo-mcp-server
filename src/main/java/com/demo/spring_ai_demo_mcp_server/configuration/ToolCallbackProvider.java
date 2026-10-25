package com.demo.spring_ai_demo_mcp_server.configuration;

import com.demo.spring_ai_demo_mcp_server.tool.DummyProductsTool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallbackProvider {

    @Bean
    MethodToolCallbackProvider tools(DummyProductsTool tool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tool)
                .build();
    }
}
