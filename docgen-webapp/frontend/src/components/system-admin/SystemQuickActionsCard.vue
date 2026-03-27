<template>
  <section class="panel">
    <div class="panel-head">
      <h3>快捷入口</h3>
    </div>
    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('go-create-ai')">AI 创建项目</button>
      <button class="ghost" :disabled="loading" @click="$emit('go-ai-docgen')">进入 AI 需求整理</button>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{ loading: boolean }>()

defineEmits<{
  (event: 'go-create-ai'): void
  (event: 'go-ai-docgen'): void
}>()
</script>

<style scoped>
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 14px; margin-bottom: 14px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.panel-head h3 { margin: 0; }
.row { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.primary, .ghost { border-radius: 8px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.ghost { background: #f3f4f6; }
</style>
