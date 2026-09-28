/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UserIdentityTest {
    @ParameterizedTest
    @CsvSource({//
            "INVOKE,  true,  true",//
            "DOCUMENT,true,  true",//
            "LIST,    false, true",//
            "READ,    false, true",//
            "HISTORY, false, true",//
            "SAVE,    false, false",//
            "PUBLISH, false, false",//
            "DISABLE, false, false",//
            "DELETE,  false, false",//
            "DEBUG,   false, false"//
    })
    void presetIdentitiesHaveTheirDeclaredOperations(Operation operation, boolean apiAccess, boolean readOnly) {
        assertFalse(UserIdentity.anonymous(Map.of("tenant", "example")).checkOperation(operation));
        assertEquals(apiAccess, UserIdentity.authenticated("caller", Map.of()).checkOperation(operation));
        assertEquals(readOnly, UserIdentity.consoleReadOnly("reader", Map.of()).checkOperation(operation));
        assertTrue(UserIdentity.consoleAdmin("developer", Map.of()).checkOperation(operation));
    }

    @Test
    void defaultIdentitiesRetainTheirAuthenticationAndAttributeContracts() {
        UserIdentity anonymous = UserIdentity.anonymous(Map.of());
        assertNull(anonymous.identityId());
        assertFalse(anonymous.authenticated());
        assertTrue(anonymous.attributes().isEmpty());
        assertTrue(anonymous.operations().isEmpty());
        assertFalse(anonymous.checkOperation(null));
        for (UserIdentity identity : new UserIdentity[] { UserIdentity.authenticated("user", Map.of()), UserIdentity.consoleReadOnly("user", Map.of()), UserIdentity.consoleAdmin("user", Map.of()) }) {
            assertEquals("user", identity.identityId());
            assertTrue(identity.authenticated());
            assertTrue(identity.attributes().isEmpty());
            assertFalse(identity.checkOperation(null));
        }
        UserIdentity identity = UserIdentity.authenticated("user", Map.of("tenant", "example"));
        assertEquals("example", identity.attributes().get("tenant"));
        assertEquals(Set.of(Operation.INVOKE, Operation.DOCUMENT), identity.operations());
    }

    @Test
    void presetIdentitiesCopyAttributesAndExposeOnlyReadAccess() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("tenant", "initial");
        UserIdentity[] identities = {//
                UserIdentity.anonymous(attributes),//
                UserIdentity.authenticated("caller", attributes),//
                UserIdentity.consoleReadOnly("reader", attributes),//
                UserIdentity.consoleAdmin("developer", attributes)//
        };
        attributes.put("tenant", "changed");
        attributes.put("added", "outside");
        for (UserIdentity identity : identities) {
            assertEquals(Map.of("tenant", "initial"), identity.attributes());
            assertThrows(UnsupportedOperationException.class, () -> identity.attributes().put("added", null));
            assertThrows(UnsupportedOperationException.class, () -> identity.attributes().remove("tenant"));
            assertThrows(UnsupportedOperationException.class, () -> identity.attributes().clear());
            assertThrows(UnsupportedOperationException.class, () -> identity.attributes().entrySet().iterator().next().setValue(null));
        }
    }

    @Test
    void anonymousIdentitiesKeepTheirOwnAttributeSnapshots() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("tenant", "first");
        UserIdentity first = UserIdentity.anonymous(attributes);
        attributes.put("tenant", "second");
        UserIdentity second = UserIdentity.anonymous(attributes);
        attributes.clear();
        assertEquals(Map.of("tenant", "first"), first.attributes());
        assertEquals(Map.of("tenant", "second"), second.attributes());
        assertTrue(UserIdentity.anonymous(Map.of()).attributes().isEmpty());
    }

    @Test
    void presetPermissionsAndAttributesRemainReadOnly() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("tenant", "initial");
        UserIdentity identity = UserIdentity.consoleReadOnly("reader", attributes);
        attributes.put("tenant", "changed");
        assertTrue(identity.checkOperation(Operation.LIST));
        assertFalse(identity.checkOperation(Operation.DELETE));
        assertEquals("initial", identity.attributes().get("tenant"));
        assertThrows(UnsupportedOperationException.class, () -> identity.attributes().put("added", null));
        assertThrows(UnsupportedOperationException.class, () -> identity.attributes().remove("tenant"));
        assertThrows(UnsupportedOperationException.class, () -> identity.attributes().clear());
        assertThrows(UnsupportedOperationException.class, () -> identity.attributes().entrySet().iterator().next().setValue(null));
        assertThrows(UnsupportedOperationException.class, () -> identity.operations().add(Operation.DELETE));
        assertThrows(UnsupportedOperationException.class, () -> UserIdentity.consoleAdmin("user", Map.of()).operations().clear());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t" })
    void authenticatedIdentitiesRequireAnId(String id) {
        assertThrows(IllegalArgumentException.class, () -> UserIdentity.authenticated(id, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> UserIdentity.consoleReadOnly(id, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> UserIdentity.consoleAdmin(id, Map.of()));
    }
}
