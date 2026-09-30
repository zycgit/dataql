/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.example.service;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.sql.DataSource;
import net.hasor.dataway.authorization.UserIdentity;

/** Loads credentials and current permissions from the application's user table. */
public class UserService {
    private final DataSource source;

    public UserService(DataSource source) {
        this.source = source;
    }

    public UserIdentity authenticate(String username, String password) {
        if (username == null || password == null) {
            return UserIdentity.anonymous(Map.of());
        }
        try (var connection = this.source.getConnection();
             var query = connection.prepareStatement("SELECT * FROM example_users WHERE username = ? AND enabled = TRUE")) {
            query.setString(1, username);
            try (var row = query.executeQuery()) {
                if (row.next() && this.matches(password, row.getString("password_hash"), row.getString("password_salt"))) {
                    return this.identity(row);
                }
                return UserIdentity.anonymous(Map.of());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot authenticate the application user", e);
        }
    }

    private boolean matches(String password, String hash, String salt) {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 210000, 256);
        try {
            byte[] actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(Base64.getDecoder().decode(hash), actual);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot verify the password", e);
        } finally {
            spec.clearPassword();
        }
    }

    public UserIdentity findIdentity(String username) {
        try (var connection = this.source.getConnection();
             var query = connection.prepareStatement("SELECT * FROM example_users WHERE username = ? AND enabled = TRUE")) {
            query.setString(1, username);
            try (var row = query.executeQuery()) {
                return row.next() ? this.identity(row) : UserIdentity.anonymous(Map.of());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load the application user", e);
        }
    }

    private UserIdentity identity(ResultSet row) throws SQLException {
        String username = row.getString("username");
        Map<String, Object> attributes = Map.of("tenant", row.getString("tenant"));
        return switch (row.getString("role")) {
            case "api" -> UserIdentity.authenticated(username, attributes);
            case "reader" -> UserIdentity.consoleReadOnly(username, attributes);
            case "admin" -> UserIdentity.consoleAdmin(username, attributes);
            default -> UserIdentity.anonymous(Map.of());
        };
    }
}
