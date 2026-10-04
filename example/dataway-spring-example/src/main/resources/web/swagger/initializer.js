/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
async function initializeSwagger() {
    const response = await fetch('../example/config', {method: 'POST', credentials: 'same-origin'});
    const configuration = await response.json();
    const specification = new URLSearchParams(window.location.search).get('spec');
    SwaggerUIBundle({
        url: specification === 'swagger2' ? configuration.swagger : configuration.openapi,
        dom_id: '#swagger-ui',
        deepLinking: true,
        withCredentials: true,
        validatorUrl: null,
        presets: [SwaggerUIBundle.presets.apis]
    });
}
initializeSwagger();
