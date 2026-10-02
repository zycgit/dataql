<!--
 Copyright 2015-2026 the original author or authors.

 Licensed under the Apache License, Version 2.0.
 See the LICENSE.txt file for the full license.
 https://www.apache.org/licenses/LICENSE-2.0
-->
<template>
  <el-container class="application">
    <el-header height="60px">
      <el-menu mode="horizontal" :ellipsis="false">
        <el-menu-item index="1"><router-link to="/"><el-icon><Notebook /></el-icon>Interface</router-link></el-menu-item>
        <el-menu-item index="2"><router-link to="/new"><el-icon><Plus /></el-icon>New</router-link></el-menu-item>
        <el-menu-item index="3"><a href="https://www.dataql.net/web/dataql/what_is_dataql.html" target="_blank" rel="noopener noreferrer"><el-icon><Warning /></el-icon>What is DataQL?</a></el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <div v-if="startupError" class="startup-error">
        <el-alert :title="startupError" type="error" :closable="false" show-icon />
        <el-button @click="start">Retry</el-button>
      </div>
      <router-view v-if="client" :key="route.path" />
      <div v-else-if="!startupError" v-loading="true" class="startup-loading" />
    </el-main>
  </el-container>
</template>
<script setup>
import {onMounted, provide, ref, shallowRef} from 'vue';
import {useRoute} from 'vue-router';
import {Notebook, Plus, Warning} from '@element-plus/icons-vue';
import {DatawayClient} from './utils/api.js';
const props = defineProps({options: {type: [Object, Function], required: true}});
const route = useRoute();
const client = shallowRef();
const startupError = ref('');
provide('dataway', {client});
async function start() {
    startupError.value = '';
    try {
        const config = typeof props.options === 'function' ? await props.options() : props.options;
        client.value = new DatawayClient(config);
    } catch (error) {
        startupError.value = error.message;
    }
}
onMounted(start);
</script>
