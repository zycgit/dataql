import { createRouter, createWebHashHistory } from 'vue-router';
import InterfaceList from '../views/InterfaceList.vue';
import InterfaceEdit from '../views/InterfaceEdit.vue';
import InterfaceNew from '../views/InterfaceNew.vue';

const routes = [
    {path: '/', name: 'root', component: InterfaceList},
    {path: '/new', name: 'new', component: InterfaceNew},
    {path: '/edit/:id', name: 'edit', component: InterfaceEdit}
];

const router = createRouter({
    history: createWebHashHistory(process.env.BASE_URL),
    routes
});

export default router;
