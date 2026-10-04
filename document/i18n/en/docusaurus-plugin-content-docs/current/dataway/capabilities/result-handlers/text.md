---
title: "5.3.4 Text"
description: "Text converts script values into plain text for textual content and simple status responses."
---

## Introduction

Text converts script values into plain text for textual content and simple status responses.

## Purpose

Responses use `text/plain; charset=UTF-8` and `String.valueOf(value)`. Objects use their own string representation, such as `{name=Ada}`; null produces `null`.

Script failures produce a Structure failure response. Use Raw Value for binary models and input streams.

## Usage

Call `POST /result-text` with `{"message":"Hello Dataway"}`:

```javascript
return ${message};
```

The response body is `Hello Dataway`.

## Configuration

Select Text in the console or set the API option below, then save and publish:

```json title="API options"
{"resultHandler": "text"}
```
