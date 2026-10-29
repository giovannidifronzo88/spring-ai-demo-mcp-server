package com.demo.spring_ai_demo_mcp_server.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Path;

// Fail-closed: se la chiave pubblica non e' configurata, tutte le richieste vengono rifiutate.
@Component
public class McpBearerTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final McpClientJwtVerifier jwtVerifier;

    public McpBearerTokenFilter(@Value("${app.mcp.server.public-key-path:}") String publicKeyPath) {
        this.jwtVerifier = publicKeyPath.isBlank() ? null : McpClientJwtVerifier.fromPemFile(Path.of(publicKeyPath));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (presentsValidToken(request)) {
            chain.doFilter(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean presentsValidToken(HttpServletRequest request) {
        String token = bearerTokenOf(request);
        return jwtVerifier != null && token != null && jwtVerifier.accepts(token);
    }

    private String bearerTokenOf(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
