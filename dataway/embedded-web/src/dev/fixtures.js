/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */

// Each seed uses the save-api fields, plus a local id and publication flags.
export const definitions = [
    {
        id: 'mock-hello', select: 'POST', apiPath: '/mock/hello', published: true,
        comment: 'Mock greeting and request headers', codeType: 'DataQL',
        codeValue: 'return ${message};', requestBody: {message: 'Hello Dataway Mock.'},
        headerData: [{checked: true, name: 'X-Demo', value: 'console'}],
        optionInfo: {resultHandler: 'structure'},
    },
    {
        id: 'mock-users', select: 'GET', apiPath: '/mock/users', published: true,
        comment: 'Mock SQL rows', codeType: 'SQL',
        codeValue: 'select id, name from users', requestBody: {},
        optionInfo: {resultHandler: 'structure'},
    },
    {
        id: 'mock-download', select: 'GET', apiPath: '/mock/download', published: true,
        comment: 'Mock binary download', codeType: 'DataQL',
        codeValue: '// The mock responder supplies the download bytes.\nreturn null;', requestBody: {},
        optionInfo: {resultHandler: 'raw'},
    },
    {
        id: 'mock-draft', select: 'POST', apiPath: '/mock/draft', published: false,
        comment: 'Draft ready for editing', codeType: 'DataQL',
        codeValue: 'return ${message};', requestBody: {message: 'Draft example'},
        optionInfo: {resultHandler: 'structure'},
    },
    {
        id: 'mock-disabled', select: 'POST', apiPath: '/mock/disabled', published: true, enabled: false,
        comment: 'Disabled API example', codeType: 'DataQL',
        codeValue: 'return ${message};', requestBody: {message: 'Disabled example'},
        optionInfo: {resultHandler: 'structure'},
    },
    {
        id: 'mock-text', select: 'GET', apiPath: '/mock/text', published: true,
        comment: 'Plain text preview', codeType: 'DataQL', codeValue: "return 'Hello Dataway';", requestBody: {},
        optionInfo: {resultHandler: 'text'},
    },
    {
        id: 'mock-csv', select: 'GET', apiPath: '/mock/csv', published: true,
        comment: 'CSV table and download', codeType: 'SQL', codeValue: 'select id, name from users', requestBody: {},
        optionInfo: {resultHandler: 'csv'},
    },
    {
        id: 'mock-image', select: 'GET', apiPath: '/mock/image', published: true,
        comment: 'Binary image preview', codeType: 'DataQL',
        codeValue: '// The mock responder supplies an SVG image.\nreturn null;', requestBody: {},
        optionInfo: {resultHandler: 'raw'},
    },
    {
        id: 'mock-verifyCode', select: 'POST', apiPath: '/mock/verifyCode', published: true,
        comment: 'Fixed PNG verification code preview', codeType: 'DataQL', codeValue: 'return ${text};',
        requestBody: {text: 'A7K9'}, optionInfo: {resultHandler: 'verifyCode'},
    },
];

// Responses are selected by method and path. Editor scripts are never evaluated.
// A responder can return {body, status, headers}; Buffer bodies bypass JSON formatting.
export const responses = {
    'POST /mock/hello': ({parameters, request}) => ({
        body: {mock: true, message: parameters.message ?? 'Hello Dataway Mock.',
            parameters, header: request.headers['x-demo'] ?? null},
    }),
    'GET /mock/users': () => ({body: [{id: 1, name: 'Alice'}, {id: 2, name: 'Bob'}]}),
    'GET /mock/download': () => ({
        body: Buffer.from([0, 1, 255, 10]),
        headers: {'Content-Type': 'application/octet-stream', 'Content-Disposition': 'attachment; filename="mock-result.bin"'},
    }),
    'GET /mock/text': () => ({body: 'Hello Dataway\nPlain text · 你好'}),
    'POST /mock/verifyCode': () => ({body: 'A7K9'}),
    'GET /mock/csv': () => ({body: [{id: 1, name: 'Alice, "A"'}, {id: 2, name: 'Bob'}]}),
    'GET /mock/image': () => ({
        body: Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="640" height="240" viewBox="0 0 640 240">'
            + '<rect width="640" height="240" rx="12" fill="#ecf5ff"/>'
            + '<circle cx="86" cy="120" r="40" fill="#409eff"/>'
            + '<text x="86" y="135" font-size="44" font-family="sans-serif" text-anchor="middle" fill="white">D</text>'
            + '<text x="152" y="115" font-size="32" font-family="sans-serif" fill="#303133">Dataway</text>'
            + '<text x="152" y="152" font-size="18" font-family="sans-serif" fill="#606266">Image response preview</text></svg>'),
        headers: {'Content-Type': 'image/svg+xml', 'Content-Disposition': 'inline; filename="dataway-preview.svg"'},
    }),
};

export function defaultResponse({definition, parameters}) {
    return {body: {mock: true, message: 'Sample response; the script was not executed.',
        method: definition.select, path: definition.apiPath, codeType: definition.codeType, parameters}};
}
