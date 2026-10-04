---
slug: /dataway/authorization
title: "7. Authentication and authorization"
hide_table_of_contents: true
description: "Connect application identities and control API, management and document access."
---

Authorization controls API calls, management operations and document access. The application handles login and credentials. Dataway obtains users through `IdentityProvider` and checks operation permissions through `AuthorizationCheck`.

## Request flow

1. The application validates credentials such as Cookies or JWTs. `IdentityProvider` returns a `UserIdentity`.
2. Dataway selects the request's `Operation` and calls `AuthorizationCheck.check(identity, operation)`.
3. Allowed operations proceed. Denied operations raise `DatawayException(401, "Unauthorized")`; the host framework handles the response.

The default checker uses the identity's preset permissions. Host login interceptors control access to console pages and static assets.

## Usage guide

- [Identity integration](identity.md): Choose a preset and connect application users.
- [Authorization](permissions.md): Review permissions and extend checks.
