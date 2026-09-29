/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.model.WebRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestIdentityProviderTest {
    @Test
    void preservesTheHostIdentityAndReadsOnlyTheConfiguredAttribute() {
        Map<String, ?> attributes = Map.of("tenant", "example");
        List<UserIdentity> identities = List.of(//
                UserIdentity.authenticated("api", attributes),//
                UserIdentity.consoleReadOnly("reader", attributes), //
                UserIdentity.consoleAdmin("admin", attributes));
        IdentityProvider provider = new RequestIdentityProvider("application.identity");
        for (UserIdentity identity : identities) {
            WebRequest request = mock(WebRequest.class);
            when(request.getAttribute("application.identity")).thenReturn(identity);
            assertSame(identity, provider.resolve(request));
            verify(request).getAttribute("application.identity");
            verifyNoMoreInteractions(request);
        }
    }

    @Test
    void missingOrNonIdentityAttributesGrantNoPermissions() {
        IdentityProvider provider = new RequestIdentityProvider("application.identity");
        for (Object attribute : new Object[] { null, "admin", Map.of("authenticated", true) }) {
            WebRequest request = mock(WebRequest.class);
            when(request.getAttribute("application.identity")).thenReturn(attribute);
            UserIdentity identity = provider.resolve(request);
            assertFalse(identity.authenticated());
            assertTrue(identity.operations().isEmpty());
            assertTrue(identity.attributes().isEmpty());
            verify(request).getAttribute("application.identity");
            verifyNoMoreInteractions(request);
        }
    }
}
