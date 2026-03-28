<template>
  <section class="tabs card">
    <div class="title-block">
      <h2>{{ title }}</h2>
      <p class="muted">需求 #{{ requirementId }}</p>
    </div>
    <div class="tab-actions">
      <button class="tab" :class="{ active: activeTab === 'workbench' }" @click="$emit('go-workbench')">AI 整理</button>
      <button class="tab" :class="{ active: activeTab === 'versions' }" @click="$emit('go-versions')">版本页</button>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  requirementId: number
  activeTab: 'workbench' | 'versions'
}>()

defineEmits<{
  (event: 'go-workbench'): void
  (event: 'go-versions'): void
}>()
</script>

<style scoped>
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 10px; margin-bottom: 12px; }
.tabs { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.title-block h2 { margin: 0; }
.muted { color: #6b7280; margin: 4px 0 0; }
.tab-actions { display: flex; gap: 8px; }
.tab { border: 1px solid #d1d5db; border-radius: 8px; padding: 8px 12px; background: #f3f4f6; cursor: pointer; }
.tab.active { background: #2563eb; border-color: #2563eb; color: #fff; }
@media (max-width: 980px) { .tabs { flex-direction: column; align-items: stretch; } .tab-actions { flex-wrap: wrap; } }
</style>
