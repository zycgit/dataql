<!--
 Copyright 2015-2026 the original author or authors.

 Licensed under the Apache License, Version 2.0.
 See the LICENSE.txt file for the full license.
 https://www.apache.org/licenses/LICENSE-2.0
-->
<template>
  <el-splitter :layout="split === 'horizontal' ? 'vertical' : 'horizontal'" class="split-pane">
    <el-splitter-panel :size="size" :min="minPercent + '%'" @update:size="size = $event">
      <slot name="paneL" />
    </el-splitter-panel>
    <el-splitter-panel :min="minPercent + '%'"><slot name="paneR" /></el-splitter-panel>
  </el-splitter>
</template>
<script setup>
import {ref} from 'vue';
const props = defineProps({
    split: {type: String, default: 'vertical'},
    minPercent: {type: Number, default: 10},
    defaultPercent: {type: Number, default: 50},
});
const size = ref(props.defaultPercent + '%');
</script>
