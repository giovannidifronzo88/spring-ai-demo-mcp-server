package com.demo.spring_ai_demo_mcp_server.configuration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;

public class McpClientJwtVerifier {

    static final String EXPECTED_SUBJECT = "spring-ai-basic-demo";
    static final String AUDIENCE = "spring-ai-demo-mcp-server";
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

    private final ECDSAVerifier signatureVerifier;

    private McpClientJwtVerifier(ECDSAVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    public static McpClientJwtVerifier fromPemFile(Path publicKeyPath) {
        try {
            ECKey publicKey = JWK.parseFromPEMEncodedObjects(Files.readString(publicKeyPath)).toECKey();
            return new McpClientJwtVerifier(new ECDSAVerifier(publicKey));
        } catch (IOException | JOSEException e) {
            throw new IllegalStateException("Chiave pubblica MCP non leggibile: " + publicKeyPath, e);
        }
    }

    public boolean accepts(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            return usesEs256(jwt) && jwt.verify(signatureVerifier) && hasValidClaims(jwt.getJWTClaimsSet(), Instant.now());
        } catch (ParseException | JOSEException e) {
            return false;
        }
    }

    private boolean usesEs256(SignedJWT jwt) {
        return JWSAlgorithm.ES256.equals(jwt.getHeader().getAlgorithm());
    }

    private boolean hasValidClaims(JWTClaimsSet claims, Instant now) {
        return isExpectedSubject(claims)
                && isExpectedAudience(claims)
                && isNotExpired(claims, now)
                && isNotIssuedInTheFuture(claims, now);
    }

    private boolean isExpectedSubject(JWTClaimsSet claims) {
        return EXPECTED_SUBJECT.equals(claims.getSubject());
    }

    private boolean isExpectedAudience(JWTClaimsSet claims) {
        return claims.getAudience() != null && claims.getAudience().contains(AUDIENCE);
    }

    private boolean isNotExpired(JWTClaimsSet claims, Instant now) {
        return claims.getExpirationTime() != null
                && claims.getExpirationTime().toInstant().plus(CLOCK_SKEW).isAfter(now);
    }

    private boolean isNotIssuedInTheFuture(JWTClaimsSet claims, Instant now) {
        return claims.getIssueTime() != null
                && !claims.getIssueTime().toInstant().minus(CLOCK_SKEW).isAfter(now);
    }
}
