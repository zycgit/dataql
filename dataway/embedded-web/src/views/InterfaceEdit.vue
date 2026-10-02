<!--
 Copyright 2015-2026 the original author or authors.

 Licensed under the Apache License, Version 2.0.
 See the LICENSE.txt file for the full license.
 https://www.apache.org/licenses/LICENSE-2.0
-->
<template>
  <section v-loading="busy" class="interface-edit">
    <div class="editor-header">
      <div class="route-row">
        <el-select v-model="form.select" size="small" class="method-select" aria-label="HTTP method" :disabled="!newMode">
          <el-option v-for="method in methods" :key="method" :label="method" :value="method" />
        </el-select>
        <el-tooltip :content="form.comment || 'Api description'" :disabled="showComment" placement="bottom">
          <el-input v-model="form.apiPath" size="small" class="input-with-select" placeholder="the path to access this Api" aria-label="API path" :disabled="!newMode">
            <template #append><el-button :icon="InfoFilled" aria-label="Edit description" @click="editComment" /></template>
          </el-input>
        </el-tooltip>
      </div>
      <div class="editor-toolbar">
        <el-radio-group v-model="form.codeType" size="small" @change="languageChanged">
          <el-radio value="DataQL" border>DataQL</el-radio><el-radio value="SQL" border>SQL</el-radio>
        </el-radio-group>
        <EditorActions v-model:option-info="form.optionInfo" :busy="busy" :new-mode="newMode" :status="form.status" :tested="tested && !dirty" :history="history"
                       @save="action(save)" @execute="action(execute)" @smoke="action(smoke)" @publish="action(publish)"
                       @history="loadHistory" @restore="restore" @disable="action(disable)" @delete="action(remove)" />
        <el-tag size="small" :type="statusTag(form.status).type" class="status-tag">{{ statusTag(form.status).title }}</el-tag>
        <span v-if="dirty" class="dirty-indicator">Unsaved</span>
        <el-button v-if="!newMode" link :icon="Refresh" aria-label="Reload API" title="Reload API" @click="reload" />
      </div>
    </div>
    <el-divider />
    <SplitPane class="editor-workspace">
      <template #paneL><CodeEditor v-model="form.codeValue" :language="form.codeType === 'SQL' ? 'sql' : 'dataql'" label="API script" @save="action(save)" @run="action(execute)" /></template>
      <template #paneR>
        <SplitPane split="horizontal">
          <template #paneL><RequestPanel v-model:request-body="form.requestBody" v-model:header-data="form.headerData" hide-run-btn @run="action(execute)" @save="action(save)" /></template>
          <template #paneR><ResponsePanel v-model:option-info="form.optionInfo" :response="response" :result-handlers="resultHandlers" on-edit-page /></template>
        </SplitPane>
      </template>
    </SplitPane>
    <el-dialog v-model="showComment" title="Description" width="min(520px, calc(100vw - 32px))"
               align-center append-to-body :close-on-click-modal="false" @opened="commentInput.focus()">
      <el-input ref="commentInput" v-model="commentDraft" type="textarea" :rows="5" placeholder="Describe this API."
                aria-label="API description" maxlength="255" show-word-limit />
      <template #footer>
        <el-button @click="showComment = false">Cancel</el-button>
        <el-button type="primary" @click="confirmComment">Confirm</el-button>
      </template>
    </el-dialog>
  </section>
</template>
<script setup>
import {computed, inject, onBeforeUnmount, onMounted, ref} from 'vue';
import {onBeforeRouteLeave, useRoute, useRouter} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import {InfoFilled, Refresh} from '@element-plus/icons-vue';
import SplitPane from '../components/SplitPane.vue';
import CodeEditor from '../components/CodeEditor.vue';
import RequestPanel from '../components/RequestPanel.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import EditorActions from '../components/EditorActions.vue';
import {parameters, requestHeaders} from '../utils/api.js';
import {editInterface, newInterface, statusTag} from '../utils/model.js';
const services = inject('dataway');
const route = useRoute();
const router = useRouter();
const methods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'];
const form = ref(newInterface());
const snapshot = ref(JSON.stringify(form.value));
const busy = ref(false);
const tested = ref(false);
const history = ref([]);
const resultHandlers = ref(['structure', 'raw']);
const response = ref(null);
const showComment = ref(false);
const commentDraft = ref('');
const commentInput = ref(null);
const newMode = computed(() => form.value.id === '-1');
const dirty = computed(() => JSON.stringify(form.value) !== snapshot.value);

function editComment() {
    commentDraft.value = form.value.comment;
    showComment.value = true;
}
function confirmComment() {
    form.value.comment = commentDraft.value;
    showComment.value = false;
}
async function action(operation) {
    if (busy.value) {
        return;
    }
    busy.value = true;
    try {
        await operation();
    } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
            ElMessage.error(error.status === 409 ? error.message + ' Your edits were kept. Reload before retrying.' : error.message);
        }
    } finally {
        busy.value = false;
    }
}
function apply(detail) {
    form.value = editInterface(detail);
    snapshot.value = JSON.stringify(form.value);
    tested.value = false;
}
async function load(id) {
    const detail = (await services.client.value.management('api-detail', {id})).result;
    apply(detail);
}
async function reload() {
    if (dirty.value && !window.confirm('Discard unsaved changes and reload?')) {
        return;
    }
    await action(() => load(form.value.id));
}
function validate() {
    if (!form.value.apiPath.startsWith('/')) {
        throw new Error('API path must start with /.');
    }
    parameters(form.value.requestBody, form.value.select);
    if (!form.value.codeValue.trim()) {
        throw new Error('Script cannot be empty.');
    }
    if (form.value.optionInfo.resultHandler === 'structure' && Object.hasOwn(form.value.optionInfo, 'responseFormat')) {
        JSON.parse(form.value.optionInfo.responseFormat);
    }
}
async function save() {
    validate();
    const result = await services.client.value.management('save-api', {method: 'POST', id: form.value.id, body: form.value});
    const wasNew = newMode.value;
    await load(result.result);
    ElMessage.success('Save successfully.');
    if (wasNew) {
        await router.replace('/edit/' + encodeURIComponent(result.result));
    }
}
async function execute() {
    validate();
    response.value = await services.client.value.execute('perform', form.value.id,
        {...form.value, requestBody: parameters(form.value.requestBody, form.value.select)}, requestHeaders(form.value.headerData));
}
async function smoke() {
    if (dirty.value) {
        throw new Error('Save your changes before running Smoke Test.');
    }
    const result = await services.client.value.execute('smoke', form.value.id,
        {id: form.value.id, version: form.value.version, requestBody: parameters(form.value.requestBody, form.value.select)}, requestHeaders(form.value.headerData));
    response.value = result;
    tested.value = result.ok && result.data?.success !== false;
    if (tested.value) {
        ElMessage.success('Smoke test passed.');
    }
}
async function publish() {
    if (!tested.value || dirty.value) {
        throw new Error('Save and run Smoke Test before publishing.');
    }
    await services.client.value.management('publish', {method: 'POST', id: form.value.id, body: {id: form.value.id, version: form.value.version}});
    await load(form.value.id);
    ElMessage.success('Published successfully.');
}
async function loadHistory() {
    await action(async () => {
        history.value = (await services.client.value.management('api-history', {id: form.value.id})).result;
    });
}
async function restore(historyId) {
    if (dirty.value && !window.confirm('Replace unsaved changes with this release?')) {
        return;
    }
    await action(async () => {
        const detail = (await services.client.value.management('get-history', {id: form.value.id, query: {historyId}})).result;
        const restored = editInterface({...detail, id: form.value.id, version: form.value.version,
            path: form.value.apiPath, select: form.value.select, status: form.value.status});
        form.value = restored;
        tested.value = false;
        ElMessage.success('Release loaded into the editor. Save to update the draft.');
    });
}
async function disable() {
    await ElMessageBox.confirm('Disable the published API?', 'Disable', {type: 'warning'});
    if (dirty.value && !window.confirm('Discard unsaved changes?')) {
        return;
    }
    await services.client.value.management('disable', {method: 'POST', id: form.value.id, body: {id: form.value.id, version: form.value.version}});
    await load(form.value.id);
    ElMessage.success('API disabled.');
}
async function remove() {
    await ElMessageBox.confirm('Delete this API and its release history?', 'Delete', {type: 'warning'});
    await services.client.value.management('delete', {method: 'POST', id: form.value.id, body: {id: form.value.id, version: form.value.version}});
    snapshot.value = JSON.stringify(form.value);
    await router.push('/');
}
function languageChanged(type) {
    tested.value = false;
    if (newMode.value && ['// a new Query.\nreturn ${message};', '-- a new Query.\nselect #{message};'].includes(form.value.codeValue)) {
        form.value.codeValue = type === 'SQL' ? '-- a new Query.\nselect #{message};' : '// a new Query.\nreturn ${message};';
    }
}
function beforeUnload(event) {
    if (dirty.value) {
        event.preventDefault();
        event.returnValue = '';
    }
}
onBeforeRouteLeave(() => !dirty.value || window.confirm('Discard unsaved changes?'));
onMounted(() => {
    window.addEventListener('beforeunload', beforeUnload);
    action(async () => {
        resultHandlers.value = (await services.client.value.management('get-handlers')).result;
        if (route.params.id) {
            await load(route.params.id);
        }
    });
});
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload));
</script>
