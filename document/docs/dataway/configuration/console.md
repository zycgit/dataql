---
title: "10.4 控制台部署"
description: "独立部署 Dataway 控制台，通过网关连接后端 API 并初始化访问地址。"
---

控制台支持随应用部署或独立部署。独立部署时，由静态服务器提供页面，网关转发 API 请求，`initializer.js` 配置访问地址。

## 准备页面资源

将与后端版本匹配的 `dataway-embedded-web` JAR 中 `META-INF/dataway-ui/` 的全部文件解压到 `/srv/www/admin/`，或从源码构建后复制 `dist/` 内容：

```bash title="构建控制台"
cd dataway/embedded-web
npm ci
npm run build
```

## 连接前后端

后端启用 `dataway.admin-enabled`，列表页调用 API 时还需启用 `dataway.api-enabled`，配置见 [Spring](../integration/spring.md#入口配置)、[Solon](../integration/solon.md#入口配置)、[Hasor](../integration/hasor.md#入口配置)。以下通过 Nginx 提供同源访问，页面地址为 `http://console.example.com/admin/`，后端使用默认路由：

```nginx title="Nginx 站点配置"
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

控制台要求页面与 API 同源（协议、域名、端口一致）。后端有 context path 时，将其写入 `proxy_pass`；上传大小限制需同时配置网关和后端，本例网关限制为 12 MiB。

## 初始化控制台

在 `initializer.js` 中填写浏览器可访问的网关路径：

```javascript title="initializer.js"
window.addEventListener('load', () => {
    window.DatawayUI({
        adminApi: '/management/',
        api: '/api/',
    });
});
```

- `adminApi`：必填，管理 API 地址。
- `api`：可选，已发布 API 地址；省略后列表页无法调用 API，管理和调试仍可使用。

地址以 `/` 结尾，支持根相对路径和页面相对路径，不含查询参数、片段或凭据。网关路径变化后需同步修改。

## 接入应用登录

用户先登录应用，再打开控制台。请求自动携带同源 Cookie，由后端校验并通过 `IdentityProvider` 提供身份，见[身份鉴权](../authorization/index.md)。示例中的 `/session/` 按应用登录路径替换。

Cookie 域名应匹配网关，路径覆盖管理和业务 API（如 `Path=/`）；Secure Cookie 需使用 HTTPS。本地存储中的令牌不会自动添加到 Authorization 头，需由应用或网关衔接。

## 动态获取初始化参数

多环境部署可通过异步函数获取地址配置：

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

`/ui-settings` 由应用提供并通过网关转发，返回 `{adminApi: '/management/', api: '/api/'}`。获取失败时可点击 Retry 重试。

## 随后端部署

内置控制台由整合模块根据入口配置生成 `initializer.js`，重启后生效。替换界面时，排除 `dataway-embedded-web`，引入包含 `META-INF/dataway-ui/` 的自定义资源 JAR。资源包可通过 `./build.sh package web` 单独构建。

## 本地调试

在 `dataway/embedded-web` 中运行 `npm run dev:mock` 使用模拟数据，访问 `http://127.0.0.1:8888/admin/`。连接真实后端时配置以下文件，再运行 `npm run dev:proxy`：

```dotenv title=".env.proxy.local"
DATAWAY_DEV_TARGET=http://127.0.0.1:8080
DATAWAY_DEV_ADMIN_PREFIX=/admin/api
DATAWAY_DEV_API_PREFIX=/api
```

代理模式复用后端登录凭据，Cookie 的路径需覆盖本地代理路径。
