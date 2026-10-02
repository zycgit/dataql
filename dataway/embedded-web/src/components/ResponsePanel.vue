<!--
 Copyright 2015-2026 the original author or authors.

 Licensed under the Apache License, Version 2.0.
 See the LICENSE.txt file for the full license.
 https://www.apache.org/licenses/LICENSE-2.0
-->
<template>
  <section class="responsePanel editor-panel">
    <div class="panel-actions">
      <span v-if="response" class="response-status">{{ response.status }} · {{ response.elapsed }} ms</span>
      <el-dropdown v-if="onEditPage" trigger="click" placement="bottom-end" popper-class="result-handler-menu" @command="selectOutput">
        <el-button size="small" type="primary" plain class="result-handler-select" aria-label="Result Handler" :title="selectedOutput.label">
          <span class="result-handler-name">{{ selectedOutput.label }}</span><el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <li class="result-handler-title" role="presentation">Result Handler</li>
            <el-dropdown-item v-for="output in outputs" :key="output.handler" :command="output"
                              :aria-current="output === selectedOutput" :class="{'is-selected': output === selectedOutput}">
              <span class="result-handler-name">{{ output.label }}</span><el-icon v-if="output === selectedOutput"><Check /></el-icon>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button-group>
        <el-tooltip v-if="tab === 'structure' || !['image', 'file'].includes(view)" content="Copy to Clipboard" placement="top-end">
          <el-button size="small" round aria-label="Copy to Clipboard" @click="copy"><ActionIcon name="copy" /></el-button>
        </el-tooltip>
        <el-tooltip v-if="tab === 'structure' || view === 'json'" content="Format Result" placement="top-end">
          <el-button size="small" round aria-label="Format Result" @click="format"><ActionIcon name="format" /></el-button>
        </el-tooltip>
        <el-tooltip v-if="response?.downloadable" content="Save As Download" placement="top-end">
          <el-button size="small" round aria-label="Save As Download" @click="download"><ActionIcon name="download" /></el-button>
        </el-tooltip>
      </el-button-group>
    </div>
    <el-tabs v-model="tab" type="card" class="panel-tabs">
      <el-tab-pane name="result" label="Result">
        <div class="result-preview">
          <div v-if="response" class="result-view-toolbar">
            <span class="result-view-label">View</span>
            <el-select v-model="view" size="small" class="result-view-select" aria-label="Result view">
              <el-option v-for="item in views" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <span class="result-content-type" :title="response.contentType">{{ response.mime }}</span>
            <span class="result-size">{{ size }}</span>
          </div>
          <div class="result-view-content">
            <div v-if="view === 'table' && table" class="result-table-preview">
              <div class="result-table-summary">{{ table.source }} · {{ table.total }} rows<span v-if="table.total > table.rows.length"> · Showing first {{ table.rows.length }}</span></div>
              <el-table :data="table.rows" height="100%" border stripe size="small" empty-text="No rows" aria-label="Response table">
                <el-table-column type="index" width="54" />
                <el-table-column v-for="(column, index) in table.columns" :key="index" :label="column" min-width="120" show-overflow-tooltip>
                  <template #default="{row}">{{ cellText(row[index]) }}</template>
                </el-table-column>
              </el-table>
            </div>
            <div v-else-if="view === 'image'" class="result-image-preview">
              <img v-if="!imageFailed" :src="imageUrl" alt="Response preview" @error="imageFailed = true">
              <div v-else class="result-image-error">
                <p>This image could not be displayed.</p>
                <el-button size="small" @click="download">Download file</el-button>
              </div>
            </div>
            <div v-else-if="view === 'file'" class="result-file-preview">
              <el-icon class="result-file-icon"><Document /></el-icon>
              <div class="result-file-name">{{ response.filename }}</div>
              <div class="result-file-details">{{ response.mime }} · {{ size }}</div>
              <el-button type="primary" size="small" :icon="Download" @click="download">Download file</el-button>
            </div>
            <CodeEditor v-else v-model="display" :language="view === 'json' ? 'json' : 'plaintext'" label="Response result" />
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="onEditPage" name="structure" label="Structure" class="result-structure-pane" :disabled="!selectedOutput.structure">
        <p class="result-template-hint">Leave blank to use the handler’s default response template.</p>
        <CodeEditor :model-value="optionInfo.responseFormat || ''" language="json" label="Response structure" @update:model-value="option('responseFormat', $event)" />
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
<script setup>
import {computed, onBeforeUnmount, ref, watch} from 'vue';
import {ElMessage} from 'element-plus';
import {ArrowDown, Check, Document, Download} from '@element-plus/icons-vue';
import {responseTable} from '../utils/response.js';
import CodeEditor from './CodeEditor.vue';
import ActionIcon from './ActionIcon.vue';
const props = defineProps({
    response: {type: Object, default: null},
    optionInfo: {type: Object, default: () => ({})},
    resultHandlers: {type: Array, default: () => ['structure', 'raw']},
    onEditPage: Boolean,
});
const emit = defineEmits(['update:optionInfo']);
const tab = ref('result');
const display = ref('"empty."');
const view = ref('json');
const imageUrl = ref('');
const imageFailed = ref(false);
const table = computed(() => responseTable(props.response));
const views = computed(() => {
    const response = props.response;
    const items = [];
    if (response?.hasJson) {
        items.push({value: 'json', label: 'JSON'});
    }
    if (['json', 'text'].includes(response?.kind)) {
        items.push({value: 'text', label: 'Text'});
    }
    if (table.value) {
        items.push({value: 'table', label: 'Table'});
    }
    if (response?.kind === 'image') {
        items.push({value: 'image', label: 'Image'});
    }
    items.push({value: 'file', label: 'File'});
    return items;
});
const size = computed(() => {
    const bytes = props.response?.blob.size ?? 0;
    if (bytes < 1024) {
        return bytes + ' B';
    }
    if (bytes < 1024 * 1024) {
        return (bytes / 1024).toFixed(1) + ' KiB';
    }
    return (bytes / (1024 * 1024)).toFixed(1) + ' MiB';
});
const outputs = computed(() => {
    const labels = {structure: 'Structure', raw: 'Raw Value', csv: 'CSV', text: 'Text', verifyCode: 'VerifyCode'};
    return ['structure', 'raw', ...props.resultHandlers.filter(name => !['structure', 'raw'].includes(name))]
        .map(name => ({label: Object.hasOwn(labels, name) ? labels[name] : name, handler: name, structure: name === 'structure'}));
});
const selectedOutput = computed(() => {
    const handler = props.optionInfo.resultHandler || 'structure';
    return outputs.value.find(output => output.handler === handler) || {label: handler, handler, structure: false};
});
watch(() => selectedOutput.value.structure, enabled => {
    if (!enabled) {
        tab.value = 'result';
    }
});
watch(() => props.response, response => {
    releaseImage();
    imageFailed.value = false;
    if (response?.kind === 'image') {
        imageUrl.value = URL.createObjectURL(response.blob);
    }
    view.value = response?.kind === 'bytes' ? 'file' : response?.kind === 'image' ? 'image'
        : ['text/csv', 'application/csv'].includes(response?.mime) && table.value ? 'table' : response?.kind || 'json';
    updateDisplay();
    tab.value = 'result';
}, {immediate: true});
watch(view, updateDisplay);
function updateDisplay() {
    const response = props.response;
    display.value = view.value === 'json' && response?.hasJson ? JSON.stringify(response.data, null, 2)
        : response?.rawText ?? response?.text ?? '"empty."';
}
function releaseImage() {
    if (imageUrl.value) {
        URL.revokeObjectURL(imageUrl.value);
        imageUrl.value = '';
    }
}
onBeforeUnmount(releaseImage);
function cellText(value) {
    if (value == null) {
        return '';
    }
    return typeof value === 'object' ? JSON.stringify(value) : String(value);
}
function option(name, value) {
    const options = {...props.optionInfo, [name]: value};
    if (name === 'responseFormat' && !value.trim()) {
        delete options.responseFormat;
    }
    emit('update:optionInfo', options);
}
function selectOutput(output) {
    emit('update:optionInfo', {...props.optionInfo, resultHandler: output.handler});
}
function format() {
    try {
        if (tab.value === 'structure') {
            if (!props.optionInfo.responseFormat) {
                return;
            }
            option('responseFormat', JSON.stringify(JSON.parse(props.optionInfo.responseFormat), null, 2));
        } else {
            display.value = JSON.stringify(JSON.parse(display.value), null, 2);
        }
    } catch (error) {
        ElMessage.error('Format Error: ' + error.message);
    }
}
async function copy() {
    const text = tab.value === 'structure' ? props.optionInfo.responseFormat || ''
        : view.value === 'table' ? props.response.rawText : display.value;
    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
        } else {
            const input = document.createElement('textarea');
            input.value = text;
            input.className = 'clipboard-input';
            document.body.append(input);
            try {
                input.select();
                if (!document.execCommand('copy')) {
                    throw new Error('Select the result and copy it manually.');
                }
            } finally {
                input.remove();
            }
        }
        ElMessage.success('Copied to clipboard.');
    } catch (error) {
        ElMessage.error(error.message);
    }
}
function download() {
    const url = URL.createObjectURL(props.response.blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = props.response.filename || 'dataway-result.bin';
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
</script>
