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
      <el-checkbox v-if="onEditPage" :model-value="optionInfo.resultStructure" class="structure-toggle" @change="option('resultStructure', $event)">Structure</el-checkbox>
      <el-button-group>
        <el-tooltip content="Copy to Clipboard" placement="top-end">
          <el-button size="small" round aria-label="Copy to Clipboard" @click="copy"><ActionIcon name="copy" /></el-button>
        </el-tooltip>
        <el-tooltip v-if="tab === 'structure' || !response || response.kind === 'json'" content="Format Result" placement="top-end">
          <el-button size="small" round aria-label="Format Result" @click="format"><ActionIcon name="format" /></el-button>
        </el-tooltip>
        <el-tooltip v-if="response?.kind === 'bytes'" content="Save As Download" placement="top-end">
          <el-button size="small" round aria-label="Save As Download" @click="download"><ActionIcon name="download" /></el-button>
        </el-tooltip>
      </el-button-group>
    </div>
    <el-tabs v-model="tab" type="card" class="panel-tabs">
      <el-tab-pane name="result" label="Result">
        <CodeEditor v-model="display" :language="!response || response.kind === 'json' ? 'json' : 'plaintext'" label="Response result" />
      </el-tab-pane>
      <el-tab-pane v-if="onEditPage" name="structure" label="Structure" :disabled="!optionInfo.resultStructure">
        <CodeEditor :model-value="optionInfo.responseFormat || ''" language="json" label="Response structure" @update:model-value="option('responseFormat', $event)" />
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
<script setup>
import {ref, watch} from 'vue';
import {ElMessage} from 'element-plus';
import CodeEditor from './CodeEditor.vue';
import ActionIcon from './ActionIcon.vue';
const props = defineProps({
    response: {type: Object, default: null},
    optionInfo: {type: Object, default: () => ({})},
    onEditPage: Boolean,
});
const emit = defineEmits(['update:optionInfo']);
const tab = ref('result');
const display = ref('"empty."');
watch(() => props.response, response => {
    display.value = response?.text ?? '"empty."';
    tab.value = 'result';
});
function option(name, value) {
    emit('update:optionInfo', {...props.optionInfo, [name]: value});
    if (name === 'resultStructure' && !value) {
        tab.value = 'result';
    }
}
function format() {
    try {
        if (tab.value === 'structure') {
            option('responseFormat', JSON.stringify(JSON.parse(props.optionInfo.responseFormat), null, 2));
        } else {
            display.value = JSON.stringify(JSON.parse(display.value), null, 2);
        }
    } catch (error) {
        ElMessage.error('Format Error: ' + error.message);
    }
}
async function copy() {
    const text = tab.value === 'structure' ? props.optionInfo.responseFormat : display.value;
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
