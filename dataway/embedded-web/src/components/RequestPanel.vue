<!--
 Copyright 2015-2026 the original author or authors.

 Licensed under the Apache License, Version 2.0.
 See the LICENSE.txt file for the full license.
 https://www.apache.org/licenses/LICENSE-2.0
-->
<template>
  <section class="requestPanel editor-panel">
    <div class="panel-actions">
      <el-button-group>
        <el-tooltip v-if="!hideRunBtn" content="Execute Query" placement="bottom-end">
          <el-button size="small" round aria-label="Execute Query" :disabled="disabled" @click="$emit('run')"><ActionIcon name="execute" /></el-button>
        </el-tooltip>
        <el-tooltip v-if="tab === 'parameters'" content="Format Parameters" placement="bottom-end">
          <el-button size="small" round aria-label="Format Parameters" @click="format"><ActionIcon name="format" /></el-button>
        </el-tooltip>
        <el-tooltip v-else content="Add Header" placement="bottom-end">
          <el-button size="small" round aria-label="Add Header" @click="updateHeaders([...headerData, {checked: true, name: '', value: ''}])"><ActionIcon name="add" /></el-button>
        </el-tooltip>
      </el-button-group>
    </div>
    <el-tabs v-model="tab" type="card" class="panel-tabs">
      <el-tab-pane name="parameters" label="Parameters">
        <CodeEditor :model-value="requestBody" language="json" label="Request parameters" @update:model-value="$emit('update:requestBody', $event)" @run="$emit('run')" @save="$emit('save')" />
      </el-tab-pane>
      <el-tab-pane name="headers" label="Headers">
        <el-table :data="headerData" height="100%" border empty-text="No Header">
          <el-table-column width="24" :resizable="false">
            <template #header><el-checkbox :model-value="allChecked" :indeterminate="someChecked && !allChecked" aria-label="Select all headers" @change="selectAll" /></template>
            <template #default="{row, $index}"><el-checkbox :model-value="row.checked" aria-label="Enable header" @change="change($index, 'checked', $event)" /></template>
          </el-table-column>
          <el-table-column label="Key" min-width="100">
            <template #default="{row, $index}"><el-input :model-value="row.name" size="small" placeholder="key of Header" aria-label="Header name" @update:model-value="change($index, 'name', $event)" /></template>
          </el-table-column>
          <el-table-column label="Value" min-width="160">
            <template #default="{row, $index}"><el-input :model-value="row.value" size="small" placeholder="value of Header" aria-label="Header value" @update:model-value="change($index, 'value', $event)" /></template>
          </el-table-column>
          <el-table-column width="38" :resizable="false">
            <template #default="{$index}"><el-button size="small" type="danger" :icon="Delete" circle aria-label="Delete header" @click="updateHeaders(headerData.filter((_, index) => index !== $index))" /></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
<script setup>
import {computed, ref} from 'vue';
import {ElMessage} from 'element-plus';
import {Delete} from '@element-plus/icons-vue';
import CodeEditor from './CodeEditor.vue';
import ActionIcon from './ActionIcon.vue';
const props = defineProps({
    requestBody: {type: String, default: '{}'},
    headerData: {type: Array, default: () => []},
    hideRunBtn: Boolean, disabled: Boolean,
});
const emit = defineEmits(['update:requestBody', 'update:headerData', 'run', 'save']);
const tab = ref('parameters');
const someChecked = computed(() => props.headerData.some(row => row.checked));
const allChecked = computed(() => props.headerData.length > 0 && props.headerData.every(row => row.checked));
function updateHeaders(rows) {
    emit('update:headerData', rows);
}
function change(index, key, value) {
    updateHeaders(props.headerData.map((row, position) => position === index ? {...row, [key]: value} : row));
}
function selectAll(checked) {
    updateHeaders(props.headerData.map(row => ({...row, checked})));
}
function format() {
    try {
        emit('update:requestBody', JSON.stringify(JSON.parse(props.requestBody), null, 2));
    } catch (error) {
        ElMessage.error('Parameters Format Error: ' + error.message);
    }
}
</script>
