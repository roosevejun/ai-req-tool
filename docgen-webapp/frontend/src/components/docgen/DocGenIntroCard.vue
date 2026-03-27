<template>
  <section class="card">
    <h2>1. 输入业务背景</h2>
    <p class="hint">先描述业务场景、目标用户、核心流程和你的初步想法，AI 会先澄清问题，再逐步整理成 PRD。</p>

    <details class="details">
      <summary>如果你已经有上一版 PRD，可以贴在这里作为修订基线</summary>
      <p class="hint">AI 会在已有 PRD 的基础上做增量整理，便于后续比较版本变化。</p>
      <textarea
        v-model="previousPrdMarkdown"
        class="textarea"
        placeholder="粘贴上一版 PRD Markdown，可选"
      />
    </details>

    <textarea
      v-model="businessDescription"
      class="textarea"
      placeholder="例如：我们要做一个面向企业客户的 AI 需求管理工具，支持项目创建、需求澄清、PRD 生成与版本管理。"
    />

    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('create-job')">
        {{ loading ? '启动中...' : '启动 AI 整理' }}
      </button>
      <button class="ghost" :disabled="loading" @click="$emit('reset')">重置</button>
    </div>
  </section>
</template>

<script setup lang="ts">
const businessDescription = defineModel<string>('businessDescription', { required: true })
const previousPrdMarkdown = defineModel<string>('previousPrdMarkdown', { required: true })

defineProps<{
  loading: boolean
}>()

defineEmits<{
  (event: 'create-job'): void
  (event: 'reset'): void
}>()
</script>

<style scoped>
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 16px; margin-bottom: 14px; }
.hint { color: #6b7280; margin-top: -6px; margin-bottom: 10px; }
.details { margin-bottom: 12px; }
.textarea { width: 100%; min-height: 140px; resize: vertical; padding: 10px; box-sizing: border-box; border-radius: 8px; border: 1px solid #e5e7eb; font-size: 14px; line-height: 1.4; }
.row { display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap; }
button { border-radius: 8px; padding: 10px 14px; border: 1px solid transparent; cursor: pointer; font-size: 14px; }
.primary { background: #2563eb; color: #fff; }
.ghost { background: #f3f4f6; color: #111827; border-color: #e5e7eb; }
</style>
