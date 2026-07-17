import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import SplitPane from './components/SplitPane.vue';
import './assets/public.css';
import axios from 'axios';

const configIsBool = ['resultStructure', 'wrapAllParameters', 'showGitButton', 'enableCrossDomain'];
const toBoolean = (val) => {
    return val != null && val.toLowerCase() === 'true';
};

axios({
    url: 'api/global-config',
    method: 'GET',
    Accept: 'application/json',
    withCredentials: true,
    responseType: 'json',
}).then(async (response) => {
    const defaultOption = {
        resultStructure: true,
        responseFormat:
            '{\n' +
            '  "success"      : "@resultStatus",\n' +
            '  "message"      : "@resultMessage",\n' +
            '  "location"     : "@codeLocation",\n' +
            '  "code"         : "@resultCode",\n' +
            '  "lifeCycleTime": "@timeLifeCycle",\n' +
            '  "executionTime": "@timeExecution",\n' +
            '  "value"        : "@resultData"\n' +
            '}',
        wrapAllParameters: false,
        wrapParameterName: 'root',
        showGitButton: true,
        enableCrossDomain: false
    };
    if (response.data.success) {
        const configs = response.data.result;
        Object.keys(configs).forEach(function (key) {
            if (configIsBool.indexOf(key) > -1) {
                defaultOption[key] = toBoolean(configs[key]);
            } else {
                defaultOption[key] = configs[key];
            }
        });
    }
    //
    const contextPath = defaultOption['CONTEXT_PATH'];
    window.CONTEXT_PATH = contextPath === undefined ? '' : contextPath;
    window.API_BASE_URL = defaultOption['API_BASE_URL'];
    //
    const app = createApp(App);
    app.config.globalProperties.defaultOption = defaultOption;
    app.use(ElementPlus);
    Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
        app.component(key, component);
    });
    app.component('SplitPane', SplitPane);
    app.directive('clipboard', {
        beforeMount(el) {
            el.__clipboardState = {};
        },
        mounted(el, binding) {
            updateClipboardState(el, binding);
        },
        updated(el, binding) {
            updateClipboardState(el, binding);
        },
        unmounted(el, binding) {
            if (binding.arg === 'copy') {
                el.removeEventListener('click', el.__clipboardState.copyHandler);
            }
            delete el.__clipboardState;
        }
    });
    app.use(router);
    app.mount('#app');
});

function updateClipboardState(el, binding) {
    const state = el.__clipboardState;
    state[binding.arg || 'copy'] = binding.value;
    if (binding.arg !== 'copy' || state.copyHandler) {
        return;
    }
    state.copyHandler = async event => {
        const textValue = typeof state.copy === 'function' ? state.copy() : state.copy;
        const text = textValue == null ? '' : String(textValue);
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(text);
            } else {
                fallbackCopyText(text);
            }
            state.success && state.success({text, event, trigger: el});
        } catch (error) {
            state.error && state.error({error, event, trigger: el});
        }
    };
    el.addEventListener('click', state.copyHandler);
}

function fallbackCopyText(text) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', 'readonly');
    textarea.style.position = 'fixed';
    textarea.style.left = '-9999px';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
}
