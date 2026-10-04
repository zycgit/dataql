---
title: "5.1.1 Visual operations"
description: "Use Dataway UI to find, edit and debug APIs and manage their states and release history."
---

import ConsoleGuideImage from '@site/src/components/ConsoleGuideImage';

Dataway UI provides list and editor pages for finding APIs, writing scripts, configuring parameters, debugging, publishing and restoring history.

## API list

Click Interface in the top navigation. The page has three areas: A API list, B request parameters and C response results. Drag the dividers to resize them.

<ConsoleGuideImage page="list" alt="API list page: A points to the API list, B to request parameters, and C to response results" />

### A: API list

- Select: click a row or its checkbox to load the API's parameter examples and headers into area B.
- Search: enter a path or description keyword in search Api to filter the list.
- Directory: click the grid icon to the left of search to filter by a path directory.
- Refresh: click the refresh icon to the right of search to reload the list.
- Edit: click the edit icon at the end of a row to open its editor.

Each row shows the HTTP method, state, path and description. See [API states](#api-status) below for state labels.

### B: Request parameters

- Parameters: enter JSON parameters. The eraser icon at the top right formats the JSON.
- Headers: add, edit or delete request headers. Checked headers are sent with the call.
- Execute Query: click the green play button at the top right to call the selected published API.

Parameter changes on the list page apply to the current call. Edit and save parameter examples on the editor page to retain them. APIs in Changes state continue to execute their published content.

### C: Response results

Result displays the published API response. See [D: Results](#result-panel) for preview, copy, format and download controls.

## Create and edit

Click New in the top navigation to create an API, or open the editor from a list row. Area A is the toolbar, B the script editor, C the test parameters, and D the result.

<ConsoleGuideImage page="editor" alt="API editor: A points to the toolbar, B to the script, C to test parameters, and D to the result" />

### A: Toolbar

Set the HTTP method and API path on the left. Paths start with `/`, such as `/hello`. The method and path are fixed after the first save.

Click the information icon beside the path to enter a description in the multiline Description dialog. Click Confirm or Cancel, then choose the DataQL or SQL script type.

The buttons on the right, from left to right, are:

- More Settings (ellipsis): configure parameter wrapping and the wrapper parameter name. See [API options](development/options.md#parameter-wrapping).
- Save (disk): save the script, description, parameter examples and execution options.
- Execute Query (green play): run the current editor contents and display the result in area D.
- Smoke Test (flask): test the saved draft. Save current edits first.
- Publish (up arrow): publish after a successful smoke test. Subsequent API calls use this release.
- Release History List (clock): view releases and load a selected record into the editor.
- Disable API / Delete API: disable a published API, or delete an unpublished or disabled API.

The API state appears beside the buttons. Unsaved indicates edits pending save. Existing APIs also provide a Reload API button to load the latest draft.

### B: Script editor

Write DataQL or SQL with syntax highlighting, search and replace. Click Execute Query to debug the current contents. See [Script support](development/script.md) for examples.

### C: Test parameters

Enter examples in Parameters and request headers in Headers. Controls work as on the list page. Save retains these values with the API.

### D: Results {#result-panel}

Result displays the debug response, with the HTTP status and request duration at the top right. The Interface list uses the same preview options.

<ConsoleGuideImage page="result-handlers" alt="Result handlers: 1 points to the Result Handler dropdown button; 2 points to the expanded handler list" />

Click the Result Handler dropdown at 1 and select one output mode from the list at 2. Structure is selected by default, with its template editable on the Structure tab. Selecting Raw Value, CSV, Text, VerifyCode or another handler disables template editing and preserves the existing template. The list includes application-registered handlers. Settings are saved with the API and apply to public calls after publication. See [Result handlers](result-handlers.md).

Result selects a preview from the response Content-Type:

- JSON: `application/json` and types with a `+json` suffix. Arrays also support Table, including top-level arrays and arrays in `value`, `data` or `result`.
- Table: `text/csv` and `application/csv`, with support for quoted cells, commas and line breaks.
- Text: `text/*`, `application/xml`, `application/*+xml`, `application/javascript` and `application/x-www-form-urlencoded`. HTML, XML and JavaScript are displayed as source text.
- Image: `image/*`, including SVG, for formats supported by the browser.
- File: other types or responses without Content-Type, with the filename, type, size and a download action.

Changing View affects only the current display. Tables preview up to 200 rows; text, JSON and downloads retain the full content. Text uses the response charset, with UTF-8 as the default. Invalid JSON is displayed as text, and images that cannot be displayed offer a download.

- Copy icon: copy the current result.
- Eraser icon: format a JSON result.
- Download icon or Download file in the File view: save the original content using the response filename.

## API states {#api-status}

- Editor: the draft has not been published.
- Published: the API is published and its draft matches the active release.
- Changes: the API is published with draft changes awaiting publication.
- Disable: the API is disabled; its draft and history remain available.

Save updates the draft. Use Save → Smoke Test → Publish to make it available to callers. Test again after editing or reloading. See [Version management](../principles/index.md#lifecycle) for the relationship between drafts and releases.

## History, disabling and deletion

### Restore history

<ConsoleGuideImage page="history" alt="Restore history: 1 points to the clock button in the toolbar; 2 points to the restore button beside a release" />

1. Click the clock button at 1 (Release History List) to open History Version.
2. Choose a release and click its restore button at 2 to load that release into the editor.
3. Review the script and parameters, then use Save → Smoke Test → Publish to make the restored contents active.

Loading a release that differs from the draft shows Unsaved. Until you publish again, callers continue to use the currently active release.

### Disable an API

<ConsoleGuideImage page="disable" alt="Disable an API: 1 points to Disable API; 2 points to the confirmation button in the dialog" />

1. Open an API in Published or Changes state and click Disable API at 1.
2. Click OK at 2 to disable it, or Cancel to leave it active.

The state changes to Disable and public calls stop. Its draft and history remain available. Use Smoke Test → Publish to resume service.

### Delete an API

<ConsoleGuideImage page="delete" alt="Delete an API: 1 points to Delete API; 2 points to the confirmation button in the dialog" />

1. Open a saved unpublished API, or disable a published API first, then click the trash button at 1 (Delete API).
2. Click OK at 2 to delete it, or Cancel to keep it.

Deletion removes the API and all release history; leaving or reloading prompts about unsaved changes, and revision conflicts require keeping local edits before reloading and merging.

See [Programmatic management](programmatic.md) for Java and management HTTP operations.
