<template>
  <div v-if="visible" class="overlay" @click.self="$emit('close')">
    <div class="panel">
      <div class="section-head">
        <h3>知识文档详情</h3>
        <button class="ghost mini" type="button" @click="$emit('close')">关闭</button>
      </div>

      <div v-if="loading" class="empty-state">正在加载详情...</div>
      <div v-else-if="detail">
        <div class="meta-grid">
          <div><strong>ID:</strong> {{ detail.document.id }}</div>
          <div><strong>类型:</strong> {{ detail.document.documentType }}</div>
          <div><strong>状态:</strong> {{ detail.document.status }}</div>
        </div>

        <div class="block">
          <strong>标题</strong>
          <p>{{ detail.document.title || '-' }}</p>
        </div>

        <div class="block">
          <strong>摘要</strong>
          <p>{{ detail.document.summary || '-' }}</p>
        </div>

        <div class="block">
          <strong>资源列表</strong>
          <p v-if="detail.assets.length === 0">暂无资源</p>
          <ul v-else class="simple-list">
            <li v-for="asset in detail.assets" :key="asset.id">{{ asset.assetRole }} / {{ asset.storageKey }}</li>
          </ul>
        </div>

        <div class="block">
          <div class="section-head">
            <strong>分块预览</strong>
            <button class="ghost mini" type="button" @click="$emit('toggle-chunks')">
              {{ chunkExpanded ? '收起' : '展开全部' }}
            </button>
          </div>
          <p v-if="visibleChunks.length === 0">暂无分块</p>
          <div v-for="chunk in visibleChunks" :key="chunk.id" class="chunk-card">
            <div class="muted">Chunk #{{ chunk.chunkNo }}</div>
            <p>{{ chunk.chunkText }}</p>
          </div>
        </div>

        <div class="block">
          <strong>任务历史</strong>
          <p v-if="detail.tasks.length === 0">暂无任务记录</p>
          <ul v-else class="simple-list">
            <li v-for="task in detail.tasks" :key="task.id">
              {{ task.taskType }} / {{ task.status }}<span v-if="task.lastError"> / {{ task.lastError }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ProjectKnowledgeDocumentChunk, ProjectKnowledgeDocumentDetail } from './types'

defineProps<{
  visible: boolean
  loading: boolean
  detail: ProjectKnowledgeDocumentDetail | null
  chunkExpanded: boolean
  visibleChunks: ProjectKnowledgeDocumentChunk[]
}>()

defineEmits<{
  (event: 'close'): void
  (event: 'toggle-chunks'): void
}>()
</script>

<style scoped>
.overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: flex; align-items: center; justify-content: center; padding: 20px; z-index: 60; }
.panel { width: min(920px, 100%); max-height: 85vh; overflow: auto; background: #fff; border-radius: 14px; padding: 16px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
.meta-grid { display: grid; grid-template-columns: repeat(3, minmax(120px, 1fr)); gap: 10px; }
.block { margin-top: 12px; }
.block p { white-space: pre-wrap; }
.chunk-card { border: 1px solid #e5e7eb; border-radius: 10px; padding: 10px; background: #fafcff; margin-top: 8px; }
.simple-list { margin: 8px 0 0; padding-left: 18px; }
.muted, .empty-state { color: #6b7280; }
.ghost, .mini { border-radius: 8px; border: 1px solid #d1d5db; padding: 5px 9px; cursor: pointer; background: #f3f4f6; font-size: 12px; }
@media (max-width: 960px) { .meta-grid { grid-template-columns: 1fr; } }
</style>
