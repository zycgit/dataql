<!--
 Copyright 2015-2026 the original author or authors.

 Licensed under the Apache License, Version 2.0.
 See the LICENSE.txt file for the full license.
 https://www.apache.org/licenses/LICENSE-2.0
-->
<template>
  <svg class="icon" aria-hidden="true"><use :href="icons + '#icon' + name" /></svg>
</template>
<script setup>
import icons from '../assets/icons.svg';
defineProps({name: {type: String, required: true}});
</script>
