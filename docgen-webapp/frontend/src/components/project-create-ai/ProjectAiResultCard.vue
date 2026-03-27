<template>
  <section class="card result-card">
    <div class="section-head">
      <h3>结构化整理结果</h3>
      <span class="badge" :class="{ ready: readyToCreate }">{{ readyToCreate ? '可以创建项目' : '仍需补充信息' }}</span>
    </div>
    <div class="preview-grid">
      <div class="preview-item"><strong>项目背景</strong><p>{{ structuredInfo.projectBackground || '-' }}</p></div>
      <div class="preview-item"><strong>类似产品</strong><p>{{ structuredInfo.similarProducts || '-' }}</p></div>
      <div class="preview-item"><strong>目标客户群体</strong><p>{{ structuredInfo.targetCustomerGroups || '-' }}</p></div>
      <div class="preview-item"><strong>商业价值</strong><p>{{ structuredInfo.commercialValue || '-' }}</p></div>
      <div class="preview-item"><strong>核心产品价值</strong><p>{{ structuredInfo.coreProductValue || '-' }}</p></div>
      <div class="preview-item wide"><strong>业务知识摘要</strong><p>{{ structuredInfo.businessKnowledgeSummary || '-' }}</p></div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { StructuredInfo } from './types'

defineProps<{
  readyToCreate: boolean
  structuredInfo: StructuredInfo
}>()
</script>

<style scoped>
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 14px; margin-bottom: 14px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
.preview-grid { display: grid; grid-template-columns: repeat(2, minmax(180px, 1fr)); gap: 10px; }
.preview-item { border: 1px solid #e5e7eb; border-radius: 10px; padding: 10px; background: #fafcff; }
.preview-item p { white-space: pre-wrap; margin: 0; }
.wide { grid-column: 1 / -1; }
.badge { padding: 4px 8px; border-radius: 999px; background: #f3f4f6; color: #374151; font-size: 12px; }
.badge.ready { background: #dcfce7; color: #166534; }
@media (max-width: 960px) { .preview-grid { grid-template-columns: 1fr; } }
</style>
