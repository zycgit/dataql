<!--
 Copyright 2015-2026 the original author or authors.

 Licensed under the Apache License, Version 2.0.
 See the LICENSE.txt file for the full license.
 https://www.apache.org/licenses/LICENSE-2.0
-->
<template>
  <SplitPane class="interface-list" :min-percent="30">
    <template #paneL>
      <el-table v-loading="loading" :data="filtered" height="100%" border stripe highlight-current-row empty-text="No Api" @row-click="select">
        <el-table-column width="24" :resizable="false">
          <template #header><el-button link :icon="Menu" aria-label="Directory" @click="showDirectory = !showDirectory" /></template>
          <template #default="{row}"><el-checkbox :model-value="selected?.id === row.id" aria-label="Choose to Test" @change="select(row)" /></template>
        </el-table-column>
        <el-table-column :show-overflow-tooltip="true" :resizable="false">
          <template #header><el-input v-model="search" size="small" placeholder="search Api" aria-label="search Api" clearable /></template>
          <template #default="{row}">
            <el-tag size="small" effect="dark" :type="methodTag(row.select)" class="method-tag">{{ row.select }}</el-tag>
            <el-tag size="small" :type="statusTag(row.status).type" class="status-tag">{{ statusTag(row.status).title }}</el-tag>
            <span class="api-path">{{ row.path }}</span><span class="api-comment">[{{ row.comment }}]</span>
          </template>
        </el-table-column>
        <el-table-column width="24" :resizable="false">
          <template #header><el-button link :icon="Refresh" aria-label="reload Api List" @click="reload" /></template>
          <template #default="{row}"><router-link :to="'/edit/' + encodeURIComponent(row.id)" aria-label="Edit API" @click.stop><el-icon><Edit /></el-icon></router-link></template>
        </el-table-column>
      </el-table>
      <el-tree v-if="showDirectory" class="directory-list" :data="directories(rows)" node-key="label" default-expand-all @node-click="chooseDirectory" />
    </template>
    <template #paneR>
      <SplitPane split="horizontal" :min-percent="30">
        <template #paneL><RequestPanel v-model:request-body="requestBody" v-model:header-data="headers" :disabled="running || !selected || ![1, 2].includes(selected.status)" @run="run" /></template>
        <template #paneR><ResponsePanel :response="response" /></template>
      </SplitPane>
    </template>
  </SplitPane>
</template>
<script setup>
import {computed, inject, onMounted, ref} from 'vue';
import {ElMessage} from 'element-plus';
import {Menu, Refresh, Edit} from '@element-plus/icons-vue';
import SplitPane from '../components/SplitPane.vue';
import RequestPanel from '../components/RequestPanel.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import {directories, methodTag, statusTag} from '../utils/model.js';
const services = inject('dataway');
const rows = ref([]);
const selected = ref();
const search = ref('');
const showDirectory = ref(false);
const loading = ref(false);
const running = ref(false);
const requestBody = ref('{}');
const headers = ref([]);
const response = ref(null);
let selection = 0;
const filtered = computed(() => rows.value.filter(row => (row.path + ' ' + row.comment).toLowerCase().includes(search.value.toLowerCase())));
function chooseDirectory(node) {
    search.value = node.label;
    showDirectory.value = false;
}
async function reload() {
    loading.value = true;
    try {
        rows.value = (await services.client.value.management('api-list')).result;
        const row = rows.value.find(item => item.id === selected.value?.id) || rows.value[0];
        if (row) {
            await select(row);
        } else {
            selection++;
            selected.value = undefined;
            requestBody.value = '{}';
            headers.value = [];
            response.value = null;
        }
    } catch (error) {
        ElMessage.error(error.message);
    } finally {
        loading.value = false;
    }
}
async function select(row) {
    const ticket = ++selection;
    selected.value = row;
    try {
        const detail = (await services.client.value.management('api-info', {id: row.id})).result;
        if (ticket !== selection) {
            return;
        }
        selected.value = detail;
        requestBody.value = detail.requestBody || '{}';
        headers.value = detail.headerData || [];
        response.value = null;
    } catch (error) {
        if (ticket === selection) {
            selected.value = undefined;
            ElMessage.error(error.message);
        }
    }
}
async function run() {
    if (running.value || !selected.value || ![1, 2].includes(selected.value.status)) {
        return;
    }
    running.value = true;
    const ticket = selection;
    try {
        const result = await services.client.value.invoke(selected.value, requestBody.value, headers.value);
        if (ticket === selection) {
            response.value = result;
        }
    } catch (error) {
        ElMessage.error(error.message);
    } finally {
        running.value = false;
    }
}
onMounted(reload);
</script>
