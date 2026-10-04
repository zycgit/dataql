---
slug: /dataway/configuration
title: "10. Configuration"
hide_table_of_contents: true
description: "Complete DatawayConfig options and core extensions."
---

`DatawayConfig` holds Dataway's core configuration: metadata storage, identity and authorization, result handlers, upload caching, API documents and the execution engine. Complete configuration methods, defaults and constraints are listed in [DatawayConfig](core.md).

Host configuration files control entry switches and paths. Their formats and complete options are covered in [Spring](../integration/spring.md#entry-settings), [Solon](../integration/solon.md#entry-settings) and [Hasor](../integration/hasor.md#entry-settings).

## Usage guide

- [DatawayConfig](core.md): Configure core services and application defaults.
- [Management interceptors](admin-interceptors.md): Audit, validate and control transactions around management operations.
- [API interceptors](api-interceptors.md): Validate parameters and process script execution results.
- [Console deployment](console.md): Deploy the UI separately and connect backend APIs, initialization and login through a gateway.
