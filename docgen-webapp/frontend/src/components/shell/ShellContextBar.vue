<template>
  <div class="context-bar">
    <div class="breadcrumbs">
      <button
        v-for="(item, idx) in breadcrumbs"
        :key="`${item.label}-${idx}`"
        class="crumb"
        :class="{ current: idx === breadcrumbs.length - 1, clickable: !!item.to }"
        @click="item.to && $emit('navigate', item.to)"
      >
        {{ item.label }}
      </button>
    </div>

    <div class="context-actions">
      <button v-if="backTarget" class="ghost" @click="$emit('navigate', backTarget)">返回上一层</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Crumb } from './types'

defineProps<{
  breadcrumbs: Crumb[]
  backTarget: string
}>()

defineEmits<{
  (event: 'navigate', to: string): void
}>()
</script>

<style scoped>
.context-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 18px 0;
}

.breadcrumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.crumb,
.ghost {
  border: 1px solid #d4dce6;
  background: #fff;
  border-radius: 999px;
  padding: 8px 12px;
  cursor: pointer;
}

.crumb {
  color: #475569;
}

.crumb.current {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
  font-weight: 700;
}

.crumb.clickable:hover,
.ghost:hover {
  border-color: #94a3b8;
}

@media (max-width: 980px) {
  .context-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
