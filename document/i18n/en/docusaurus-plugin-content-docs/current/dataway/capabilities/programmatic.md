---
title: "5.1.2 Programmatic management"
description: "Manage API drafts, versions and releases through AdminService or management HTTP endpoints."
---

## API definitions

An API contains an ID, HTTP method, path, script type, original script, description, examples, document models and execution options.

- `id` identifies the API. The method/path combination is unique and fixed after the first save.
- `sample` stores request and response examples; `schema` stores document models.
- `options` holds parameter wrapping and response settings for that API.
- The draft holds editable content; each release retains a complete definition snapshot.

The console workflow is covered in [Visual operations](management.md).

## Create a draft

Get Dataway from the framework container and call `dataway.getAdminService()`. Run the following once to initialize a draft:

```java title="Create an API draft"
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.service.admin.AdminService;

AdminService admin = dataway.getAdminService();
ApiDefinition definition = new ApiDefinition();
definition.setId("hello");
definition.setMethod("POST");
definition.setPath("/hello");
definition.setType(ApiScriptType.DATA_QL);
definition.setScript("return {\"message\": ${message}};");
definition.setDescription("Greeting API");
definition.setSample("{\"requestBody\":{\"message\":\"Hello Dataway\"}}");
definition.setSchema("{}");
definition.setOptions("{}");

ApiState saved = admin.save(definition, 0);
```

Java callers supply the API ID. SQL APIs use `ApiScriptType.SQL`, retain original SQL in script, and declare parameter names and examples in sample. See [Document generation](document.md) and [API options](development/options.md) for models and options.

Direct AdminService callers arrange authentication and management interception. Management HTTP requests pass Dataway authorization and the management interceptor chain.

## Definitions, states and history

| Method | Result |
| --- | --- |
| `list()` | Definitions with script contents omitted |
| `getDraftByApi(apiID)` | Editable definition with the original script |
| `getApiById(apiID)` | ApiState: apiID, revision, published, enabled, hasDraft |
| `getVersionById(apiID)` | Current optimistic concurrency version |
| `getHistoryByApi(apiID)` | Releases ordered oldest first |
| `getHistoryById(historyID)` | A publication snapshot |
| `getReleaseByApi(apiID)` | Current or last release, readable after disabling; null if never published |
| `getReleaseById(releaseID)` | A publication snapshot; release and history IDs identify the same record |

`published` means a release exists, `enabled` means it is callable, and `hasDraft` means the API is unpublished or its draft differs from the latest release. ApiRelease contains the release ID, number, publication time and ApiDefinition.

## Concurrent updates

Save new APIs with version `0`. For an existing API, read the version before reading and editing the draft:

```java title="Update a draft"
String apiID = "hello";
long version = admin.getVersionById(apiID);
ApiDefinition draft = admin.getDraftByApi(apiID);
draft.setDescription("Updated greeting API");
ApiState saved = admin.save(draft, version);
```

Saving, publishing and disabling update `revision`. Use the returned version for subsequent operations. Stale versions produce a DatawayException with status 409; read the latest content and merge changes before retrying.

## Publish and disable {#publish-disable}

After verifying the draft, publish with the version returned by save:

```java title="Publish the saved draft"
ApiState published = admin.publish(saved.getApiID(), saved.getRevision());
```

Call `admin.disableApi(apiID, version)` to disable an API and receive the updated ApiState. The draft and release history remain available. Call `publish` again to resume service.

To restore history, retrieve the definition with `getHistoryById(historyID).getDefinition()`, save it with the API's current version, then publish. See [Version management](../principles/index.md#lifecycle) for the relationship between drafts and releases.

## Delete an API

Call `deleteApi` with the current revision:

```java title="Delete an API and its history"
admin.deleteApi(apiID, admin.getVersionById(apiID));
```

Deletion removes the definition and all release history. To retain the definition, [disable the API](#publish-disable).

## Management HTTP endpoints

External management applications can reuse the console endpoints, defaulting to `/admin/api`:

| Method | Relative path | Purpose |
| --- | --- | --- |
| GET | `/api-list` | List APIs |
| GET | `/get-handlers` | List available result handler names |
| GET | `/api-info?id=...`, `/api-detail?id=...` | Read invocation data or editor details |
| GET | `/api-history?id=...` | List release history |
| GET | `/get-history?id=...&historyId=...` | Read a history record |
| POST | `/save-api` | Save editor contents |
| POST | `/perform`, `/smoke` | Debug editor contents or test a saved draft |
| POST | `/publish`, `/disable`, `/delete` | Publish, disable or delete |

Requests carry host credentials and the required operation permissions; updates carry `version`. Management read/write responses use `success` and `result`. Perform and Smoke return script results.

## Invoke a published API from Java

Use `ApiService` to call a published, enabled API by path or ID. See [Java invocation](development/java.md) for examples.
