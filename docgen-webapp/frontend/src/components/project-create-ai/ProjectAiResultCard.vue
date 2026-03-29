<template>
  <WorkspaceSection
    eyebrow="结果决策"
    title="当前项目框架结果"
    description="切换查看当前最需要关注的一类结果，再继续和 AI 沟通。"
    tint
  >
    <template #actions>
      <StatusBadge :label="readyToCreate ? '框架接近可立项' : '框架仍在孵化中'" :variant="readyToCreate ? 'success' : 'warning'" small />
    </template>

    <div class="result-toolbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-chip"
        :class="{ active: activeTab === tab.key }"
        type="button"
        @click="activeTab = tab.key"
      >
        <span>{{ tab.label }}</span>
        <span v-if="typeof tab.count === 'number'" class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <article class="result-panel">
      <template v-if="activeTab === 'confirmed'">
        <h4>已形成的框架信息</h4>
        <ul class="value-list">
          <li v-if="confirmedItems.length === 0">当前还没有形成稳定的项目关键信息，建议先从项目目标、业务背景和目标用户说起。</li>
          <li v-for="item in confirmedItems" :key="item.key">
            <strong>{{ item.label }}</strong>
            <span>{{ item.value }}</span>
          </li>
        </ul>
      </template>

      <template v-else-if="activeTab === 'pending'">
        <h4>仍待确认的问题</h4>
        <ul class="value-list">
          <li v-if="pendingQuestions.length === 0">当前没有待确认问题，可以继续检查缺失项并判断这份框架是否值得先保留。</li>
          <li v-for="(question, index) in pendingQuestions" :key="`${question}-${index}`">{{ index + 1 }}. {{ question }}</li>
        </ul>
      </template>

      <template v-else-if="activeTab === 'missing'">
        <h4>还缺的关键信息</h4>
        <ul class="value-list">
          <li v-if="missingItems.length === 0">当前关键字段已经基本完整，这份项目框架已具备继续保留或正式立项的基础。</li>
          <li v-for="item in missingItems" :key="item.key">{{ item.label }}</li>
        </ul>
      </template>

      <template v-else>
        <h4>当前建议</h4>
        <p class="advice">{{ accuracyAdvice }}</p>
      </template>
    </article>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import StatusBadge from '../projects/StatusBadge.vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'
import type { StructuredFieldItem } from './types'

type ResultTabKey = 'confirmed' | 'pending' | 'missing' | 'advice'

const props = defineProps<{
  readyToCreate: boolean
  confirmedItems: StructuredFieldItem[]
  pendingQuestions: string[]
  missingItems: StructuredFieldItem[]
  accuracyAdvice: string
}>()

const activeTab = ref<ResultTabKey>('confirmed')

const tabs = computed<Array<{ key: ResultTabKey; label: string; count?: number }>>(() => [
  { key: 'confirmed', label: '已形成', count: props.confirmedItems.length },
  { key: 'pending', label: '待确认', count: props.pendingQuestions.length },
  { key: 'missing', label: '待补充', count: props.missingItems.length },
  { key: 'advice', label: '当前建议' }
])
</script>

<style scoped>
.result-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tab-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #475569;
  padding: 7px 12px;
  cursor: pointer;
}

.tab-chip.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.tab-count {
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: inherit;
  padding: 2px 8px;
  font-size: 12px;
  line-height: 1.2;
}

.result-panel {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

h4 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
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
</style>
