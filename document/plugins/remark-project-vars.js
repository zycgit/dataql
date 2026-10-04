/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
function replaceVariables(value, variables) {
    return value.replace(/@project\.([A-Za-z][A-Za-z0-9_]*)@/g, (token, name) => {
        if (!Object.hasOwn(variables, name) || typeof variables[name] !== 'string') {
            throw new Error(`Unknown or invalid project variable: ${token}`);
        }
        return variables[name];
    });
}

module.exports = function remarkProjectVars(variables) {
    return function transform(tree) {
        function visit(node) {
            if (['text', 'inlineCode', 'code'].includes(node.type)) {
                node.value = replaceVariables(node.value, variables);
            }
            if (['link', 'image', 'definition'].includes(node.type)) {
                node.url = replaceVariables(node.url, variables);
                if (node.title) {
                    node.title = replaceVariables(node.title, variables);
                }
            }
            node.children?.forEach(visit);
        }
        visit(tree);
    };
};

module.exports.replaceVariables = replaceVariables;
