<!--
 Copyright 2015-2026 the original author or authors.

 Licensed under the Apache License, Version 2.0.
 See the LICENSE.txt file for the full license.
 https://www.apache.org/licenses/LICENSE-2.0
-->
<template>
  <div class="editor-actions">
    <el-button-group>
      <el-tooltip content="More Settings" placement="bottom-end"><el-button size="small" round aria-label="More Settings" @click="settings = true"><ActionIcon name="more" /></el-button></el-tooltip>
      <el-tooltip content="Save" placement="bottom-end"><el-button size="small" round aria-label="Save" :disabled="busy" @click="$emit('save')"><ActionIcon name="save" /></el-button></el-tooltip>
      <el-tooltip content="Execute Query" placement="bottom-end"><el-button size="small" round aria-label="Execute Query" :disabled="busy" @click="$emit('execute')"><ActionIcon name="execute" /></el-button></el-tooltip>
      <el-tooltip content="Smoke Test" placement="bottom-end"><el-button size="small" round aria-label="Smoke Test" :disabled="busy || newMode" @click="$emit('smoke')"><ActionIcon name="test" /></el-button></el-tooltip>
      <el-tooltip content="Publish" placement="bottom-end"><el-button size="small" round aria-label="Publish" :disabled="busy || newMode || !tested" @click="$emit('publish')"><ActionIcon name="release" /></el-button></el-tooltip>
    </el-button-group>
    <el-button-group class="secondary-actions">
      <el-popover placement="bottom" title="History Version" width="270" trigger="click" @show="$emit('history')">
        <template #reference><el-button size="small" round aria-label="Release History List" :disabled="busy || newMode"><ActionIcon name="history" /></el-button></template>
        <el-timeline class="release-history">
          <el-timeline-item v-for="item in history" :key="item.historyId" :type="item.status === 1 ? 'success' : 'danger'">
            <span>{{ item.time }}</span>
            <el-button size="small" :icon="Edit" circle aria-label="Restore release" @click="$emit('restore', item.historyId)" />
          </el-timeline-item>
        </el-timeline>
        <span v-if="!history.length">No release history</span>
      </el-popover>
      <el-tooltip v-if="status === 1 || status === 2" content="Disable the published Api." placement="bottom-end"><el-button size="small" round aria-label="Disable API" :disabled="busy" @click="$emit('disable')"><ActionIcon name="disable" /></el-button></el-tooltip>
      <el-tooltip v-else content="Delete Api" placement="bottom-end"><el-button size="small" round aria-label="Delete API" :disabled="busy || newMode" @click="$emit('delete')"><ActionIcon name="delete" /></el-button></el-tooltip>
    </el-button-group>
    <el-drawer v-model="settings" :with-header="false" size="70%" title="More Settings">
      <el-collapse :model-value="['parameters', 'options', 'cors']">
        <el-collapse-item title="Parameters" name="parameters">
          <div class="parameter-settings">
            <span>Wrap All Parameters</span>
            <el-switch :model-value="optionInfo.wrapAllParameters" aria-label="Wrap All Parameters" @change="option('wrapAllParameters', $event)" />
            <span>to new Parameter</span>
            <el-input :model-value="optionInfo.wrapParameterName" :disabled="!optionInfo.wrapAllParameters" size="small" aria-label="Parameter wrapper name" @update:model-value="option('wrapParameterName', $event)" />
          </div>
        </el-collapse-item>
        <el-collapse-item title="API Options" name="options">
          <p>API options override the selected handler’s defaults. Omit an option to use its default.</p>
          <el-input v-model="optionsText" type="textarea" :rows="12" aria-label="API options JSON" />
          <p v-if="optionsError" role="alert">{{ optionsError }}</p>
          <el-button size="small" type="primary" @click="applyOptions">Apply Options</el-button>
        </el-collapse-item>
        <el-collapse-item title="Cross Domain" name="cors">Cross-origin access is configured by the host web framework.</el-collapse-item>
      </el-collapse>
    </el-drawer>
  </div>
</template>
<script setup>
import {ref, watch} from 'vue';
import {Edit} from '@element-plus/icons-vue';
import ActionIcon from './ActionIcon.vue';
const props = defineProps({
    busy: Boolean, newMode: Boolean, tested: Boolean,
    status: {type: Number, default: 0},
    history: {type: Array, default: () => []},
    optionInfo: {type: Object, default: () => ({})},
});
const emit = defineEmits(['save', 'execute', 'smoke', 'publish', 'history', 'restore', 'disable', 'delete', 'update:optionInfo']);
const settings = ref(false);
const optionsText = ref('');
const optionsError = ref('');
watch([settings, () => props.optionInfo], () => {
    if (settings.value) {
        optionsText.value = JSON.stringify(props.optionInfo, null, 2);
        optionsError.value = '';
    }
}, {deep: true});
function applyOptions() {
    try {
        const options = JSON.parse(optionsText.value);
        if (!options || typeof options !== 'object' || Array.isArray(options)) {
            throw new Error('API options must be a JSON object.');
        }
        for (const [name, value] of Object.entries(options)) {
            if (value === null) {
                throw new Error(name + ' must not be null. Omit it to use the default.');
            }
        }
        emit('update:optionInfo', options);
        optionsError.value = '';
    } catch (error) {
        optionsError.value = error.message;
    }
}
function option(name, value) {
    emit('update:optionInfo', {...props.optionInfo, [name]: value});
}
</script>
