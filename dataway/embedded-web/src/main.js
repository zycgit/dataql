/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import {createApp} from 'vue';
import ElementPlus from 'element-plus';
import en from 'element-plus/es/locale/lang/en';
import 'element-plus/dist/index.css';
import './assets/public.css';
import App from './App.vue';
import router from './router/index.js';

createApp(App).use(ElementPlus, {locale: en}).use(router).mount('#app');
