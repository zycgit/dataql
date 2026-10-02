/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
export const defaultOptions = {
    resultHandler: 'structure',
    responseFormat: JSON.stringify({success: '@resultStatus', message: '@resultMessage', location: '@codeLocation',
        code: '@resultCode', lifeCycleTime: '@timeLifeCycle', executionTime: '@timeExecution', value: '@resultData'}, null, 2),
    wrapAllParameters: false,
    wrapParameterName: 'root',
};

export function newInterface() {
    return {id: '-1', version: 0, select: 'POST', apiPath: '', comment: '', status: 0, codeType: 'DataQL',
        codeValue: '// a new Query.\nreturn ${message};', requestBody: '{"message":"Hello DataQL."}',
        headerData: [], optionInfo: {...defaultOptions}};
}

export function editInterface(detail) {
    const options = {...detail.optionData};
    if (options.resultHandler == null || options.resultHandler === 'default') {
        options.resultHandler = options.resultStructure === false ? 'raw' : 'structure';
    }
    delete options.resultStructure;
    return {id: detail.id, version: detail.version, select: detail.select, apiPath: detail.path,
        comment: detail.apiComment || '', status: detail.status, codeType: detail.codeType,
        codeValue: detail.codeInfo.codeValue || '', requestBody: detail.codeInfo.requestBody || '{}',
        headerData: structuredClone(detail.codeInfo.headerData || []),
        optionInfo: {...defaultOptions, ...options}, sample: detail.sample, schema: detail.schema};
}

export function statusTag(status) {
    return [{type: 'info', title: 'Editor'}, {type: 'success', title: 'Published'},
        {type: 'warning', title: 'Changes'}, {type: 'danger', title: 'Disable'}][status] || {type: 'info', title: 'Unknown'};
}

export function methodTag(method) {
    return {POST: 'success', PUT: 'warning', PATCH: 'warning', DELETE: 'danger'}[method] || 'primary';
}

export function directories(rows) {
    const root = {label: '/', children: []};
    for (const row of rows) {
        let parent = root;
        let prefix = '';
        for (const part of row.path.split('/').filter(Boolean).slice(0, -1)) {
            prefix += '/' + part;
            let child = parent.children.find(item => item.label === prefix);
            if (!child) {
                child = {label: prefix, children: []};
                parent.children.push(child);
            }
            parent = child;
        }
    }
    return [root];
}
