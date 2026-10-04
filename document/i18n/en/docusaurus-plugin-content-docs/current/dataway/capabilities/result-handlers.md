---
title: "5.3 Result handlers"
hide_table_of_contents: true
description: "Result handlers convert script execution results into HTTP responses. Each API selects one handler, with Structure as the default."
---

Result handlers convert script execution results into HTTP responses. Each API selects one handler, with Structure as the default.

## Guide

- [Structure](result-handlers/structure.md): Wrap results using a response template; the default handler.
- [Raw Value](result-handlers/raw.md): Output the original script value.
- [CSV](result-handlers/csv.md): Export a list of objects as CSV.
- [Text](result-handlers/text.md): Convert values to plain text.
- [VerifyCode](result-handlers/verify-code.md): Render text as a PNG verification code.
- [Custom result handlers](result-handlers/custom.md): extend output formats, status and response headers through ResultHandler.

## Select a handler {#selection}

Select a Result Handler in the console or set the API's `resultHandler` option, then save and publish. Omitted options use the application default; unknown names fail before execution. See [Visual operations](management.md#result-panel) and [Example projects](https://gitee.com/zycgit/dataql/tree/dev/example).

## Configuration precedence

Built-in defaults → constructor defaults → API options; later values take precedence. Edit API options under More Settings → API Options. An empty Structure tab retains the default response template.

All built-in handlers accept `Map<String, ?> defaults`. `prepareOptions` merges and validates settings, available through `ResultContext.getOptions()`; see [Custom result handlers](result-handlers/custom.md).
