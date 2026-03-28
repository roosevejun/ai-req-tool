<template>
  <WorkspaceSection
    eyebrow="框架成熟度"
    title="当前项目框架提炼结果"
    description="这里帮助你判断 AI 当前整理出来的是不是一个值得保留的项目框架，以及它距离正式立项还有多远。"
    tint
  >
    <template #actions>
      <StatusBadge :label="readyToCreate ? '框架已接近立项标准' : '框架仍在孵化中'" :variant="readyToCreate ? 'success' : 'warning'" small />
    </template>

    <div class="status-grid">
      <article class="status-card status-card--success">
        <h4>已形成的框架信息</h4>
        <p class="metric">{{ confirmedItems.length }} 项</p>
        <ul class="value-list">
          <li v-for="item in confirmedItems" :key="item.key">
            <strong>{{ item.label }}</strong>
            <span>{{ item.value }}</span>
          </li>
        </ul>
      </article>

      <article class="status-card status-card--warning">
        <h4>仍待确认的问题</h4>
        <p class="metric">{{ pendingQuestions.length }} 项</p>
        <ul class="value-list">
          <li v-if="pendingQuestions.length === 0">当前没有待确认问题，可以继续检查这份框架是否值得先保留。</li>
          <li v-for="(question, index) in pendingQuestions" :key="`${question}-${index}`">{{ question }}</li>
        </ul>
      </article>
    </div>

    <div class="status-grid">
      <article class="status-card">
        <h4>还缺的关键信息</h4>
        <ul class="value-list">
          <li v-if="missingItems.length === 0">当前关键字段都已经有内容，这份项目框架已经具备继续保留或正式立项的基础。</li>
          <li v-for="item in missingItems" :key="item.key">{{ item.label }}</li>
        </ul>
      </article>

      <article class="status-card status-card--info">
        <h4>当前建议</h4>
        <p class="advice">{{ accuracyAdvice }}</p>
      </article>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import StatusBadge from '../projects/StatusBadge.vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'
import type { StructuredFieldItem } from './types'

defineProps<{
  readyToCreate: boolean
  confirmedItems: StructuredFieldItem[]
  pendingQuestions: string[]
  missingItems: StructuredFieldItem[]
  accuracyAdvice: string
}>()
</script>

<style scoped>
.status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.status-card {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.status-card--success {
  background: #f6fffb;
}

.status-card--warning {
  background: #fffdf5;
}

.status-card--info {
  background: #f8fbff;
}

h4 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

.metric {
  margin: 8px 0 0;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}

.value-list {
  display: grid;
  gap: 10px;
  margin: 12px 0 0;
  padding-left: 18px;
  color: #334155;
}

.value-list li {
  line-height: 1.7;
}

.value-list strong {
  display: block;
  color: #0f172a;
}

.advice {
  margin: 12px 0 0;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

@media (max-width: 960px) {
  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
