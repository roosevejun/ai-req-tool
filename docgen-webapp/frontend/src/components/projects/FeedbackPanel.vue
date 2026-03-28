<template>
  <section v-if="message" class="feedback-panel" :class="`feedback-panel--${tone}`">
    <p class="feedback-title">{{ title }}</p>
    <p class="feedback-message">{{ message }}</p>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    message?: string
    tone?: 'info' | 'success' | 'danger' | 'warning'
  }>(),
  {
    message: '',
    tone: 'info'
  }
)
</script>

<style scoped>
.feedback-panel {
  border-radius: 16px;
  padding: 14px;
  border: 1px solid #dbe2ea;
  background: #fff;
}

.feedback-panel--info {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.feedback-panel--success {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.feedback-panel--danger {
  border-color: #fecaca;
  background: #fff5f5;
}

.feedback-panel--warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.feedback-title {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.feedback-message {
  margin: 0;
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
