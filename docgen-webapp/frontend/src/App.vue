<template>
  <RouterView v-if="isLoginPage" />
  <AppShell v-else />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppShell from './components/AppShell.vue'

const route = useRoute()
const isLoginPage = computed(() => route.path === '/login')
</script>

<style>
html,
body,
#app {
  min-height: 100%;
}

body {
  margin: 0;
  overflow-x: hidden;
  color: #0f172a;
  background: #f8fafc;
  font-family: "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}
</style>

