/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.nacos.example.config.auth;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.spring.nacos.example.service.UserService;

/** Issues and verifies host identity tokens without storing server-side sessions. */
public final class JwtTokenService {
    public static final  String       COOKIE_NAME = "EXAMPLE_TOKEN";
    private static final String       ISSUER      = "dataway-spring-example";
    private final        JwtJsonCodec json        = new JwtJsonCodec();
    private final        SecretKey    key;
    private final        JwtParser    parser;
    private final        int          expirationSeconds;

    private final UserService users;

    public JwtTokenService(String secret, int expirationSeconds, UserService users) {
        this.users = users;
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
        if (!this.users.authenticate(username, password).authenticated()) {
            return null;
        }

        Instant now = Instant.now();
        return Jwts.builder()               //
                .json(this.json)            //
                .issuer(ISSUER)             //
                .subject(username)          //
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
            if (claims.getExpiration() == null || identityId == null || identityId.isBlank()) {
                return UserIdentity.anonymous(Map.of());
            }
            // Re-read the user table so role changes and disabled accounts take effect immediately.
            return this.users.findIdentity(identityId);
        } catch (JwtException | IllegalArgumentException invalid) {
            return UserIdentity.anonymous(Map.of());
        }
    }
}
