---
title: "1. Introduction"
description: "Dataway embeds API development into Java applications: configure, debug and publish endpoints backed by application databases and services."
---

**Dataway is an embedded API development framework built on DataQL.** Add its JARs to write, debug and publish HTTP endpoints through the console.

DataQL handles queries, transformations and service aggregation. Dataway manages and publishes those scripts as APIs, reducing repetitive data-access and Controller code.

## Development workflow

The application provides data sources, services and identities. Developers edit, test and publish scripts in the console; callers access published APIs over HTTP. Draft changes take effect after the API is published again.

![Dataway connects application data to frontend APIs](/img/dataway/application-overview.png)

## Use cases

| Scenario | Approach |
| --- | --- |
| Read data | Query reports, dashboards, lists and detail pages; select fields, calculate values and shape the output |
| Write data | Accept form or JSON parameters and invoke SQL or business services, with validation and transactions configured by the application |
| Aggregate data | Combine results from several databases or services into the structure needed by a single API call |

## Embedded architecture

A shared core and adapters integrate with Spring, Solon and Hasor. The host supplies data sources, services and authentication.

| Component | Purpose |
| --- | --- |
| `dataway-embedded` | API management, script execution, authorization and four HTTP handlers |
| `dataway-embedded-web` | Console resource JAR, included by default and replaceable |
| `dataway-spring`, `dataway-solon`, `dataway-hasor` | Host configuration and MVC integration |
| `dataway-meta-jdbc` | Store API definitions, drafts and release history in a database |
| `dataway-meta-nacos` | Store API definitions, drafts and release history in Nacos |
| `dataql-sqlproc` | Optional SQL execution for application data |

Four HTTP entries share core services. Metadata and business data sources are configured independently; see [request flow](../principles/index.md#request-flow).

## Start here

Follow [Quick start](quickstart.md) to publish and call an API with Spring Boot and JDBC. The separate [Spring Boot + Nacos example](../integration/spring.md#nacos-example) includes a test entry point that starts local Nacos automatically.

1. Integrate [Spring](../integration/spring.md), [Solon](../integration/solon.md) or [Hasor](../integration/hasor.md).
2. Prepare [metadata storage](../metadata/index.md) and [request identities](../authorization/identity.md).
3. [Write the script](../capabilities/development/script.md), then save and publish it through [Visual operations](../capabilities/management.md#api-status).
4. Enable [Swagger / OpenAPI documents](../capabilities/document.md) when needed.

This guide uses `@project.docsVersion@` with JDK 17. Examples use Spring Boot 4.1, Solon 4.1 and Hasor 5.3.
