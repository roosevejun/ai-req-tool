<template>
  <section class="panel">
    <div class="panel-head">
      <div>
        <p class="eyebrow">Quick Actions</p>
        <h3>快捷入口</h3>
        <p class="copy">从系统管理页可以直接跳转到 AI 建项目和 AI 需求整理工作台。</p>
      </div>
    </div>

    <div class="actions">
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
.panel {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  padding: 18px;
}

.panel-head h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.copy {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.primary,
.ghost {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 10px 14px;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.ghost {
  background: #f8fafc;
}
</style>
