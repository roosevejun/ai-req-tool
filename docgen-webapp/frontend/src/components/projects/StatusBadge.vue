<template>
  <span class="badge" :class="[`badge--${variant}`, { 'badge--small': small }]">
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
    variant?: 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'ai'
    small?: boolean
  }>(),
  {
    label: '',
    variant: 'neutral',
    small: false
  }
)
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  border-radius: 999px;
  padding: 6px 12px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.badge--small {
  padding: 4px 10px;
}

.badge--neutral {
  background: #e2e8f0;
  color: #334155;
}

.badge--info {
  background: #dbeafe;
  color: #1d4ed8;
}

.badge--success {
  background: #dcfce7;
  color: #166534;
}

.badge--warning {
  background: #fef3c7;
  color: #a16207;
}

.badge--danger {
  background: #fee2e2;
  color: #b91c1c;
}

.badge--ai {
  background: #ccfbf1;
  color: #0f766e;
}
</style>
