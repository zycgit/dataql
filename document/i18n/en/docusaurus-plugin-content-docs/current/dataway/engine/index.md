---
slug: /dataway/engine
title: "9. Engine extensions"
hide_table_of_contents: true
---

Dataway connects application functions, external fragments and host resources through `DatawayConfig`. It builds execution with `HostConfiguration → QueryManager → QueryBuilder → Query`. Each guide covers implementation, registration and script usage.

## Usage guide

- [Custom functions](functions.md): Expose individual application functions.
- [Function libraries](libraries.md): Group functions under a namespace.
- [Application imports](imports.md): Reuse application objects and dependencies through imports.
- [Fragment processors](fragments.md): Interpret and execute custom external blocks.
- [Finder](finder.md): Configure object lookup and resource/class loading.
- [Custom scopes](scope.md): Supply default parameters and application environments.
- [Engine and query configuration](customizers.md): Configure the engine and queries, and supply application objects to extensions.
- [SQL interceptors](sql-interceptors.md): Observe or control JDBC execution.
- [SQL fragments](sql-macros.md): Register named SQL fragments shared by queries.
- [SQL rules](sql-rules.md): Add application-specific dynamic SQL rules.
- [SQL type handlers](sql-types.md): Customize JDBC parameter writing and result reading.
- [SQL dialects](sql-dialects.md): Customize SQL generation for page and count queries.
