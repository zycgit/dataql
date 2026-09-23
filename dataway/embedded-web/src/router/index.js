/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import {createRouter, createWebHashHistory} from 'vue-router';
import InterfaceList from '../views/InterfaceList.vue';
import InterfaceEdit from '../views/InterfaceEdit.vue';
import InterfaceNew from '../views/InterfaceNew.vue';

export default createRouter({
    history: createWebHashHistory(),
    routes: [
        {path: '/', name: 'root', component: InterfaceList},
        {path: '/new', name: 'new', component: InterfaceNew},
        {path: '/edit/:id', name: 'edit', component: InterfaceEdit},
        {path: '/:pathMatch(.*)*', redirect: '/'},
    ],
});
