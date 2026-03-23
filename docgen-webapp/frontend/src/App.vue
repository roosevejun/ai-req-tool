<template>
  <div class="container">
    <h1>DocGen Agent 需求文档生成</h1>
    <DocGenPage />
  </div>
</template>

<script setup lang="ts">
import DocGenPage from './components/DocGenPage.vue'
</script>

<style scoped>
.container {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, "PingFang SC", "Hiragino Sans GB",
    "Microsoft YaHei", sans-serif;
}

h1 {
  margin-bottom: 16px;
}
</style>

