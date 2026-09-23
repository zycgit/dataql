/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
export function backendProxy(env) {
    const target = new URL(env.DATAWAY_DEV_TARGET || 'http://localhost:8080');
    if (!['http:', 'https:'].includes(target.protocol) || target.username || target.password
        || target.pathname !== '/' || target.search || target.hash) {
        throw new Error('DATAWAY_DEV_TARGET must be an HTTP(S) origin; put context paths in the prefix settings.');
    }
    return {
        '^/dataway/api(?:/|\\?|$)': endpoint(target.origin, '/dataway/api', env.DATAWAY_DEV_ADMIN_PREFIX || '/dataway/api'),
        '^/api(?:/|\\?|$)': endpoint(target.origin, '/api', env.DATAWAY_DEV_API_PREFIX || '/api'),
    };
}

function endpoint(target, localPrefix, backendPrefix) {
    if (!backendPrefix.startsWith('/') || /[?#\\\s]/.test(backendPrefix) || backendPrefix.includes('//')
        || backendPrefix.split('/').some(part => part === '.' || part === '..')) {
        throw new Error('Dataway backend prefixes must be absolute paths, without queries or fragments.');
    }
    const prefix = backendPrefix.replace(/\/+$/, '');
    return {
        target,
        changeOrigin: true,
        // Let the browser send backend session cookies through the local development origin.
        cookieDomainRewrite: '',
        cookiePathRewrite: '/',
        rewrite: url => {
            const rewritten = prefix + url.slice(localPrefix.length);
            return rewritten.startsWith('/') ? rewritten : '/' + rewritten;
        },
    };
}
