/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class DefaultAuthorizationCheckTest {
    private final AuthorizationCheck check = new DefaultAuthorizationCheck();

    @ParameterizedTest
    @EnumSource(Operation.class)
    void apiIdentityOperationsAreUsedWithoutChangingTheDefaultChecker(Operation operation) {
        UserIdentity identity = UserIdentity.authenticated("operator", Map.of());
        assertEquals(operation == Operation.INVOKE || operation == Operation.DOCUMENT, this.check.check(identity, operation));
        assertFalse(this.check.check(null, operation));
        assertFalse(this.check.check(UserIdentity.anonymous(Map.of()), operation));
    }

    @Test
    void missingOperationDoesNotGrantAccess() {
        assertFalse(this.check.check(null, null));
        assertFalse(this.check.check(UserIdentity.anonymous(Map.of()), null));
        assertFalse(this.check.check(UserIdentity.authenticated("user", Map.of()), null));
        assertFalse(this.check.check(UserIdentity.consoleReadOnly("user", Map.of()), null));
        assertFalse(this.check.check(UserIdentity.consoleAdmin("user", Map.of()), null));
    }

    @Test
    void authenticationAloneDoesNotGrantConsoleAccess() {
        UserIdentity identity = UserIdentity.authenticated("admin", Map.of());
        assertTrue(identity.authenticated());
        assertTrue(this.check.check(identity, Operation.INVOKE));
        assertFalse(this.check.check(identity, Operation.LIST));
        assertFalse(this.check.check(identity, Operation.SAVE));
    }
}
