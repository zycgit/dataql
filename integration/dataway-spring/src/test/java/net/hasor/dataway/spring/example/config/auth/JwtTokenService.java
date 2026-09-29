/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.example.config.auth;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import net.hasor.dataway.authorization.UserIdentity;

/** Issues and verifies host identity tokens without storing server-side sessions. */
public final class JwtTokenService {
    public static final  String       COOKIE_NAME = "EXAMPLE_TOKEN";
    private static final String       ISSUER      = "dataway-spring-example";
    private final        JwtJsonCodec json        = new JwtJsonCodec();
    private final        SecretKey    key;
    private final        JwtParser    parser;
    private final        int          expirationSeconds;

    public JwtTokenService(String secret, int expirationSeconds) {
        this.key = secret.isBlank() ? Jwts.SIG.HS256.key().build() : Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        this.expirationSeconds = expirationSeconds;
        if (this.expirationSeconds <= 0) {
            throw new IllegalArgumentException("example.jwt.expiration-seconds must be positive");
        }

        this.parser = Jwts.parser()     //
                .json(this.json)        //
                .verifyWith(this.key)   //
                .requireIssuer(ISSUER)  //
                .sig().clear().add(Jwts.SIG.HS256).and().build();
    }

    public String login(String username, String password) {
        if (!"example-password".equals(password) || username == null || !Set.of("api", "reader", "admin").contains(username)) {
            return null;
        }

        Instant now = Instant.now();
        return Jwts.builder()               //
                .json(this.json)            //
                .issuer(ISSUER)             //
                .subject(username)          //
                .claim("role", username)    //
                .claim("attributes", Map.of("tenant", "example"))//
                .issuedAt(Date.from(now))   //
                .expiration(Date.from(now.plusSeconds(this.expirationSeconds)))//
                .signWith(this.key, Jwts.SIG.HS256).compact();
    }

    public UserIdentity identity(String token) {
        if (token == null || token.isBlank()) {
            return UserIdentity.anonymous(Map.of());
        }

        try {
            Claims claims = this.parser.parseSignedClaims(token).getPayload();
            String identityId = claims.getSubject();
            String role = claims.get("role", String.class);
            Map<String, ?> attributes = claims.get("attributes", Map.class);
            if (claims.getExpiration() == null || identityId == null || identityId.isBlank() || role == null || attributes == null) {
                return UserIdentity.anonymous(Map.of());
            }

            return switch (role) {
                case "api" -> UserIdentity.authenticated(identityId, attributes);
                case "reader" -> UserIdentity.consoleReadOnly(identityId, attributes);
                case "admin" -> UserIdentity.consoleAdmin(identityId, attributes);
                default -> UserIdentity.anonymous(Map.of());
            };
        } catch (JwtException | IllegalArgumentException invalid) {
            return UserIdentity.anonymous(Map.of());
        }
    }
}
