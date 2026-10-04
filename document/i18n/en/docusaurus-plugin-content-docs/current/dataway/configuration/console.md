---
title: "10.4 Console deployment"
description: "Deploy the Dataway console separately and initialize its backend API connections through a gateway."
---

The console supports embedded and separate deployments. For a separate deployment, a static server hosts the UI, a gateway forwards API requests, and `initializer.js` configures their addresses.

## Prepare static assets

Extract all files under `META-INF/dataway-ui/` from a `dataway-embedded-web` JAR matching the backend version into `/srv/www/admin/`, or build from source and copy the contents of `dist/`:

```bash title="Build the console"
cd dataway/embedded-web
npm ci
npm run build
```

## Connect frontend and backend

Enable `dataway.admin-enabled`, plus `dataway.api-enabled` for list-page API calls. See [Spring](../integration/spring.md#entry-settings), [Solon](../integration/solon.md#entry-settings) and [Hasor](../integration/hasor.md#entry-settings). This Nginx example serves the console at `http://console.example.com/admin/` and proxies the default backend routes:

```nginx title="Nginx site configuration"
server {
    listen 80;
    server_name console.example.com;
    root /srv/www;
    index index.html;
    client_max_body_size 12m;

    location = /admin {
        return 301 /admin/;
    }

    location /admin/ {
        try_files $uri $uri/ =404;
        add_header Cache-Control "no-cache";
    }

    location /management/ {
        proxy_pass http://127.0.0.1:8080/admin/api/;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
    }

    location /session/ {
        proxy_pass http://127.0.0.1:8080/session/;
    }
}
```

The console requires pages and APIs to share the same scheme, hostname and port. Include any backend context path in `proxy_pass`. Configure upload limits in both the gateway and backend; this gateway allows 12 MiB.

## Initialize the console

Set the browser-facing gateway paths in `initializer.js`:

```javascript title="initializer.js"
window.addEventListener('load', () => {
    window.DatawayUI({
        adminApi: '/management/',
        api: '/api/',
    });
});
```

- `adminApi`: Required management API address.
- `api`: Optional published API address. Omitting it disables list-page API calls; management and debugging remain available.

URLs must end in `/`, may be root-relative or page-relative, and must contain no query, fragment or credentials. Update them when gateway paths change.

## Connect application login

Users log in to the application before opening the console. Requests carry same-origin Cookies; the backend validates them and supplies an identity through `IdentityProvider`. See [Authentication and authorization](../authorization/index.md). Replace `/session/` with the application's login path.

Cookie domains must match the gateway, and paths must cover management and business APIs, such as `Path=/`. Secure Cookies require HTTPS. Local-storage tokens are not automatically added to Authorization headers; connect them through the application or gateway.

## Load initialization settings dynamically

Use an asynchronous function to fetch address settings for each environment:

```javascript title="initializer.js"
window.addEventListener('load', () => {
    window.DatawayUI(async () => {
        const response = await fetch('/ui-settings', {
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error('Unable to load console settings.');
        }
        return response.json();
    });
});
```

The application provides `/ui-settings` through the gateway, returning `{adminApi: '/management/', api: '/api/'}`. Click Retry if loading fails.

## Deploy with the backend

For the built-in console, adapters generate `initializer.js` from entry settings after restart. To replace the UI, exclude `dataway-embedded-web` and supply a custom resource JAR containing `META-INF/dataway-ui/`. Build the resource package with `./build.sh package web`.

## Local development

Run `npm run dev:mock` in `dataway/embedded-web`, then open `http://127.0.0.1:8888/admin/` for mock data. Use `npm run dev:proxy` with a real backend and configure `.env.proxy.local`:

```dotenv title=".env.proxy.local"
DATAWAY_DEV_TARGET=http://127.0.0.1:8080
DATAWAY_DEV_ADMIN_PREFIX=/admin/api
DATAWAY_DEV_API_PREFIX=/api
```

Proxy mode reuses backend credentials. Cookie paths must cover the local proxy paths.
