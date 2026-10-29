package com.demo.spring_ai_demo_mcp_server.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class McpClientJwtVerifierTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsTokenSignedWithTheMatchingPrivateKey() throws Exception {
        KeyPair clientKeys = generateEcKeyPair();
        McpClientJwtVerifier verifier = verifierFor(clientKeys);

        assertThat(verifier.accepts(signedToken(clientKeys, claimsExpiringIn(300)))).isTrue();
    }

    @Test
    void rejectsTokenSignedWithAnotherPrivateKey() throws Exception {
        McpClientJwtVerifier verifier = verifierFor(generateEcKeyPair());
        KeyPair attackerKeys = generateEcKeyPair();

        assertThat(verifier.accepts(signedToken(attackerKeys, claimsExpiringIn(300)))).isFalse();
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        KeyPair clientKeys = generateEcKeyPair();
        McpClientJwtVerifier verifier = verifierFor(clientKeys);

        assertThat(verifier.accepts(signedToken(clientKeys, claimsExpiringIn(-300)))).isFalse();
    }

    @Test
    void rejectsTokenWithWrongAudience() throws Exception {
        KeyPair clientKeys = generateEcKeyPair();
        McpClientJwtVerifier verifier = verifierFor(clientKeys);
        JWTClaimsSet claims = new JWTClaimsSet.Builder(claimsExpiringIn(300)).audience("altro-server").build();

        assertThat(verifier.accepts(signedToken(clientKeys, claims))).isFalse();
    }

    @Test
    void rejectsTokenWithWrongSubject() throws Exception {
        KeyPair clientKeys = generateEcKeyPair();
        McpClientJwtVerifier verifier = verifierFor(clientKeys);
        JWTClaimsSet claims = new JWTClaimsSet.Builder(claimsExpiringIn(300)).subject("altro-client").build();

        assertThat(verifier.accepts(signedToken(clientKeys, claims))).isFalse();
    }

    @Test
    void rejectsUnsignedToken() throws Exception {
        McpClientJwtVerifier verifier = verifierFor(generateEcKeyPair());
        String unsignedToken = new PlainJWT(claimsExpiringIn(300)).serialize();

        assertThat(verifier.accepts(unsignedToken)).isFalse();
    }

    @Test
    void rejectsMalformedToken() throws Exception {
        McpClientJwtVerifier verifier = verifierFor(generateEcKeyPair());

        assertThat(verifier.accepts("non-un-jwt")).isFalse();
    }

    private McpClientJwtVerifier verifierFor(KeyPair keys) throws Exception {
        Path publicKeyFile = tempDir.resolve("mcp-client-public.pem");
        Files.writeString(publicKeyFile, toPem("PUBLIC KEY", keys.getPublic().getEncoded()));
        return McpClientJwtVerifier.fromPemFile(publicKeyFile);
    }

    private String signedToken(KeyPair keys, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).build(), claims);
        jwt.sign(new ECDSASigner((ECPrivateKey) keys.getPrivate()));
        return jwt.serialize();
    }

    private JWTClaimsSet claimsExpiringIn(long seconds) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .subject(McpClientJwtVerifier.EXPECTED_SUBJECT)
                .audience(McpClientJwtVerifier.AUDIENCE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(seconds)))
                .jwtID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static String toPem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }
}
