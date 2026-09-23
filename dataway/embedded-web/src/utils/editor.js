/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import * as monaco from 'monaco-editor/editor/editor.api.js';
// Register editor services before the first editor and lazy JSON language service are created.
import 'monaco-editor/features/register.all.js';
import 'monaco-editor/languages/definitions/sql/register.js';
import 'monaco-editor/language/json/monaco.contribution.js';
import EditorWorker from 'monaco-editor/editor/editor.worker.js?worker';
import JsonWorker from 'monaco-editor/language/json/json.worker.js?worker';

self.MonacoEnvironment = {
    getWorker(_moduleId, label) {
        return label === 'json' ? new JsonWorker() : new EditorWorker();
    },
};

monaco.languages.register({id: 'dataql'});
monaco.languages.setMonarchTokensProvider('dataql', {
    keywords: ['return', 'var', 'val', 'if', 'else', 'for', 'break', 'continue', 'import', 'as', 'hint', 'true', 'false', 'null'],
    tokenizer: {
        root: [
            [/\/\/.*$/, 'comment'],
            [/\/\*/, 'comment', '@comment'],
            [/\$\{[^}]*\}/, 'variable'],
            [/"([^"\\]|\\.)*"|'([^'\\]|\\.)*'/, 'string'],
            [/\d+(\.\d+)?/, 'number'],
            [/[a-zA-Z_$][\w$]*/, {cases: {'@keywords': 'keyword', '@default': 'identifier'}}],
            [/[{}()[\]]/, '@brackets'],
        ],
        comment: [[/[^/*]+/, 'comment'], [/\*\//, 'comment', '@pop'], [/[/*]/, 'comment']],
    },
});
monaco.languages.setLanguageConfiguration('dataql', {
    comments: {lineComment: '//', blockComment: ['/*', '*/']},
    brackets: [['{', '}'], ['[', ']'], ['(', ')']],
    autoClosingPairs: [{open: '{', close: '}'}, {open: '[', close: ']'}, {open: '(', close: ')'}, {open: '"', close: '"'}],
});

export {monaco};
