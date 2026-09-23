<!--
 Copyright 2015-2026 the original author or authors.

 Licensed under the Apache License, Version 2.0.
 See the LICENSE.txt file for the full license.
 https://www.apache.org/licenses/LICENSE-2.0
-->
<template><div ref="container" class="code-editor" :aria-label="label" /></template>
<script setup>
import {onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {monaco} from '../utils/editor.js';

const props = defineProps({
    modelValue: {type: String, default: ''},
    language: {type: String, default: 'dataql'},
    readOnly: Boolean,
    label: {type: String, default: 'Code editor'},
});
const emit = defineEmits(['update:modelValue', 'save', 'run']);
const container = ref();
let editor;
let subscription;
let model;

onMounted(() => {
    model = monaco.editor.createModel(props.modelValue, props.language);
    editor = monaco.editor.create(container.value, {
        model, theme: 'vs', readOnly: props.readOnly,
        automaticLayout: true, minimap: {enabled: false},
        fontSize: 14, scrollBeyondLastLine: false, fixedOverflowWidgets: true,
        ariaLabel: props.label, tabSize: 4,
    });
    subscription = editor.onDidChangeModelContent(() => emit('update:modelValue', editor.getValue()));
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => emit('save'));
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => emit('run'));
});
watch(() => props.modelValue, value => {
    if (editor && value !== editor.getValue()) {
        editor.setValue(value);
    }
});
watch(() => props.language, value => {
    if (model) {
        monaco.editor.setModelLanguage(model, value);
    }
});
watch(() => props.readOnly, value => editor?.updateOptions({readOnly: value}));
onBeforeUnmount(() => {
    subscription?.dispose();
    editor?.dispose();
    model?.dispose();
});
</script>
