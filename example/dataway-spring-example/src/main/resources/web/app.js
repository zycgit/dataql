/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
const baseUrl = new URL('./', document.baseURI);
const elements = Object.fromEntries([...document.querySelectorAll('[id]')].map(element => [element.id, element]));
let configuration;
let identity;
let downloadUrl;

function notify(target, message) {
    elements[target].textContent = message;
    elements[target].hidden = !message;
}

async function request(path, options = {}) {
    const response = await fetch(new URL(path, baseUrl), {credentials: 'same-origin', cache: 'no-store', ...options});
    const blob = await response.blob();
    const text = await blob.text();
    let data;
    try {
        data = JSON.parse(text);
    } catch {
        data = null;
    }
    return {response, text, data, blob};
}

function errorMessage(result, fallback) {
    return typeof result.data?.message === 'string' ? result.data.message : `${fallback}（HTTP ${result.response.status}）`;
}

function renderLoginStatus() {
    const signedIn = Boolean(identity?.authenticated);
    elements['login-status'].textContent = signedIn ? `已登录 · ${identity.identity}` : '未登录';
    elements['login-status'].dataset.tone = signedIn ? 'success' : '';
}

function renderIdentity() {
    renderLoginStatus();
    const signedIn = Boolean(identity?.authenticated);
    elements['login-form'].hidden = signedIn;
    elements['account-details'].hidden = !signedIn;
    elements['account-status'].hidden = !signedIn;
    elements['identity-name'].textContent = signedIn ? identity.identity : '';
    elements['logout-button'].disabled = !signedIn;
    if (identity?.consoleManage) {
        elements['identity-role'].textContent = '开发管理';
    } else if (identity?.consoleAccess) {
        elements['identity-role'].textContent = '控制台只读';
    } else {
        elements['identity-role'].textContent = signedIn ? 'API 访问' : '';
    }
}

async function refreshIdentity(render = renderIdentity) {
    const result = await request('session/me', {method: 'POST'});
    if (result.response.status === 401 || result.response.status === 403) {
        identity = null;
    } else if (result.response.ok) {
        identity = result.data;
    } else {
        throw new Error(errorMessage(result, '读取登录状态失败'));
    }
    render();
    return identity;
}

async function loadConfiguration() {
    const result = await request('example/config', {method: 'POST'});
    if (!result.response.ok || !result.data) {
        throw new Error(errorMessage(result, '读取入口配置失败'));
    }
    configuration = result.data;
    for (const name of ['admin', 'openapi', 'swagger']) {
        const enabled = name === 'admin' ? configuration.adminEnabled : configuration.docsEnabled;
        elements[`${name}-link`].href = configuration[name];
        elements[`${name}-link`].setAttribute('aria-disabled', String(!enabled));
        elements[`${name}-path`].textContent = enabled ? configuration[name] : '未启用';
    }
    elements['api-url'].value = `${configuration.apiPrefix}/person`;
    elements['send-button'].disabled = !configuration.apiEnabled;
    if (!configuration.apiEnabled) {
        elements['api-hint'].textContent = '当前应用未启用业务 API 入口。';
    }
}

elements['login-form'].addEventListener('submit', async event => {
    event.preventDefault();
    elements['login-button'].disabled = true;
    notify('login-notice', '');
    try {
        const body = new URLSearchParams(new FormData(elements['login-form']));
        const result = await request('session/login', {method: 'POST', body});
        if (!result.response.ok) {
            throw new Error(result.response.status === 401 ? '登录失败，请检查用户名和密码。' : errorMessage(result, '登录失败'));
        }
        await refreshIdentity();
        if (!identity?.authenticated) {
            throw new Error('登录状态未生效，请重新登录。');
        }
        notify('entry-notice', '');
    } catch (error) {
        notify('login-notice', error.message);
    } finally {
        elements['login-button'].disabled = false;
    }
});

elements['logout-button'].addEventListener('click', async () => {
    elements['logout-button'].disabled = true;
    notify('login-notice', '');
    try {
        const result = await request('session/logout', {method: 'POST'});
        if (!result.response.ok && result.response.status !== 401) {
            throw new Error(errorMessage(result, '退出失败'));
        }
        identity = null;
        renderIdentity();
        notify('entry-notice', '');
        elements.username.focus();
    } catch (error) {
        elements['logout-button'].disabled = false;
        notify('login-notice', error.message);
    }
});

elements['refresh-session'].addEventListener('click', async () => {
    elements['refresh-session'].disabled = true;
    try {
        await refreshIdentity(renderLoginStatus);
    } catch {
        elements['login-status'].textContent = '状态读取失败';
        elements['login-status'].dataset.tone = 'error';
    } finally {
        elements['refresh-session'].disabled = false;
    }
});

for (const name of ['admin', 'openapi', 'swagger']) {
    elements[`${name}-link`].addEventListener('click', async event => {
        event.preventDefault();
        notify('entry-notice', '');
        if (!configuration) {
            notify('entry-notice', '入口配置尚未加载，请刷新页面。');
            return;
        }
        const enabled = name === 'admin' ? configuration.adminEnabled : configuration.docsEnabled;
        if (!enabled) {
            notify('entry-notice', name === 'admin' ? '当前应用未启用管理控制台。' : '当前应用未启用文档入口。');
            return;
        }
        try {
            await refreshIdentity();
            if (!identity) {
                notify('entry-notice', '请先登录，再访问该入口。');
                elements.username.focus();
                return;
            }
            if (name === 'admin' && !identity.consoleAccess) {
                notify('entry-notice', `当前用户“${identity.identity}”没有管理后台访问权限，请使用 reader 或 admin 账号登录。`);
                return;
            }
            if (name !== 'admin' && !identity.documentAccess) {
                notify('entry-notice', '当前用户没有文档访问权限。');
                return;
            }
            const result = await request(configuration[name], {method: 'HEAD'});
            if (!result.response.ok) {
                const message = name === 'admin' ? '无法访问管理后台，请确认登录状态及后台访问权限。' : '无法访问文档，请确认登录状态及文档权限。';
                throw new Error(`${message}（HTTP ${result.response.status}）`);
            }
            window.location.assign(configuration[name]);
        } catch (error) {
            notify('entry-notice', error.message);
        }
    });
}

function updateBodyMode() {
    const withoutBody = ['GET', 'HEAD'].includes(elements.method.value);
    elements['json-body'].disabled = withoutBody;
    elements['body-hint'].textContent = withoutBody ? 'GET / HEAD 不发送正文，请在接口地址中填写查询参数。' : '以 application/json 发送正文。';
}

elements.method.addEventListener('change', updateBodyMode);

elements['result-example'].addEventListener('change', () => {
    const name = elements['result-example'].value;
    if (!name || !configuration) {
        return;
    }
    const parameters = name === 'verifyCode' ? {text: 'A7K9'} : name === 'people-csv' ? {} : {message: 'Hello Dataway'};
    elements['api-url'].value = `${configuration.apiPrefix}/${name}`;
    elements.method.value = 'POST';
    elements['json-body'].value = JSON.stringify(parameters, null, 2);
    updateBodyMode();
});

elements['api-form'].addEventListener('submit', async event => {
    event.preventDefault();
    elements['send-button'].disabled = true;
    elements['send-button'].textContent = '发送中…';
    notify('api-notice', '');
    try {
        const url = new URL(elements['api-url'].value.trim(), baseUrl);
        if (!['http:', 'https:'].includes(url.protocol)) {
            throw new Error('请输入 HTTP 或 HTTPS 接口地址。');
        }
        const options = {method: elements.method.value};
        const text = elements['json-body'].value.trim();
        if (!['GET', 'HEAD'].includes(options.method) && text) {
            try {
                JSON.parse(text);
            } catch {
                throw new Error('JSON Body 格式不正确，请修改后再发送。');
            }
            options.headers = {'Content-Type': 'application/json; charset=UTF-8'};
            options.body = text;
        }
        const started = performance.now();
        const result = await request(url, options);
        const elapsed = Math.round(performance.now() - started);
        elements['response-status'].textContent = `HTTP ${result.response.status} · ${elapsed} ms`;
        elements['response-status'].dataset.tone = result.response.ok ? 'success' : 'error';
        elements['response-body'].textContent = result.data !== null ? JSON.stringify(result.data, null, 2) : result.text || '无响应正文';
        elements['response-headers'].textContent = [...result.response.headers].map(([key, value]) => `${key}: ${value}`).join('\n');
        showMedia(result);
        if (result.response.status === 401 || result.response.status === 403) {
            notify('api-notice', '请求被拒绝，请确认已登录且当前账号拥有该接口的访问权限。');
            await refreshIdentity();
        } else if (result.response.status === 404) {
            notify('api-notice', '接口不存在，请检查调用地址，并确认接口已发布。');
        }
    } catch (error) {
        notify('api-notice', error.message);
    } finally {
        elements['send-button'].disabled = !configuration?.apiEnabled;
        elements['send-button'].textContent = '发送请求';
    }
});

async function initialize() {
    updateBodyMode();
    try {
        await loadConfiguration();
    } catch (error) {
        notify('entry-notice', error.message);
    }
    try {
        await refreshIdentity();
    } catch (error) {
        elements['login-status'].textContent = '状态读取失败';
        elements['login-status'].dataset.tone = 'error';
        notify('login-notice', error.message);
    }
}

initialize();

elements['upload-form'].addEventListener('submit', async event => {
    event.preventDefault();
    notify('api-notice', '');
    try {
        const body = new FormData(elements['upload-form']);
        const path = event.submitter?.value === 'download' ? 'upload-download' : 'upload';
        const result = await request(`${configuration.apiPrefix}/${path}`, {method: 'POST', body});
        elements['response-status'].textContent = `HTTP ${result.response.status}`;
        elements['response-body'].textContent = result.data ? JSON.stringify(result.data, null, 2) : result.text;
        elements['response-headers'].textContent = [...result.response.headers].map(([key, value]) => `${key}: ${value}`).join('\n');
        showMedia(result);
    } catch (error) {
        notify('api-notice', error.message);
    }
});

function showMedia(result) {
    const link = elements['response-download'];
    const preview = elements['response-image'];
    preview.hidden = true;
    preview.removeAttribute('src');
    elements['response-body'].hidden = false;
    if (downloadUrl) {
        URL.revokeObjectURL(downloadUrl);
        downloadUrl = null;
    }
    const disposition = result.response.headers.get('content-disposition') || '';
    const type = result.response.headers.get('content-type') || '';
    const image = result.response.ok && /^image\//i.test(type);
    link.hidden = !result.response.ok || (!image && !/attachment/i.test(disposition) && !/application\/octet-stream/i.test(type));
    if (link.hidden) {
        return;
    }
    let filename = /filename="?([^";]+)"?/i.exec(disposition)?.[1] || (type.startsWith('image/png') ? 'dataway-result.png' : 'dataway-result.bin');
    const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1];
    if (encoded) {
        try {
            filename = decodeURIComponent(encoded);
        } catch {
            // Retain the fallback filename.
        }
    }
    downloadUrl = URL.createObjectURL(result.blob);
    if (image) {
        preview.src = downloadUrl;
        preview.hidden = false;
        elements['response-body'].hidden = true;
    }
    link.href = downloadUrl;
    link.download = filename;
    link.textContent = '保存响应文件：' + filename;
}
window.addEventListener('pagehide', () => {
    if (downloadUrl) {
        URL.revokeObjectURL(downloadUrl);
    }
});
