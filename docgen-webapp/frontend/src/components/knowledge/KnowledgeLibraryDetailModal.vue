<template>
  <div v-if="visible" class="overlay" @click.self="$emit('close')">
    <div class="panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Knowledge Detail</p>
          <h3>知识文档详情</h3>
        </div>
        <div class="actions">
          <button class="ghost mini" type="button" :disabled="loading || reprocessing || !detail" @click="$emit('reprocess')">
            {{ reprocessing ? '重新处理中...' : '重新处理' }}
          </button>
          <button class="ghost mini" type="button" @click="$emit('close')">关闭</button>
        </div>
      </div>

      <div v-if="loading" class="empty-state">正在加载知识文档详情...</div>
      <div v-else-if="detail" class="detail-body">
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
          <p v-if="detail.assets.length === 0">暂无资源。</p>
          <ul v-else class="simple-list">
            <li v-for="asset in detail.assets" :key="asset.id" class="asset-item">
              <div>
                {{ asset.assetRole }} / {{ asset.storageKey }}
                <span v-if="asset.mimeType" class="muted"> / {{ asset.mimeType }}</span>
              </div>
              <div class="asset-actions">
                <a class="ghost mini link-button" :href="assetPreviewUrl(asset.id)" target="_blank" rel="noreferrer">预览</a>
                <a class="ghost mini link-button" :href="assetDownloadUrl(asset.id)" target="_blank" rel="noreferrer">下载</a>
              </div>
            </li>
          </ul>
        </div>

        <div class="block">
          <div class="section-line">
            <strong>分块预览</strong>
            <button class="ghost mini" type="button" @click="$emit('toggle-chunks')">
              {{ chunkExpanded ? '收起' : '展开全部' }}
            </button>
          </div>
          <p v-if="visibleChunks.length === 0">暂无分块。</p>
          <div v-for="chunk in visibleChunks" :key="chunk.id" class="chunk-card">
            <div class="muted">Chunk #{{ chunk.chunkNo }}</div>
            <p>{{ chunk.chunkText }}</p>
          </div>
        </div>

        <div class="block">
          <strong>任务历史</strong>
          <p v-if="detail.tasks.length === 0">暂无任务记录。</p>
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
import type { KnowledgeDocumentChunk, KnowledgeDocumentDetail } from '../project-create-ai/types'

defineProps<{
  visible: boolean
  loading: boolean
  reprocessing: boolean
  detail: KnowledgeDocumentDetail | null
  chunkExpanded: boolean
  visibleChunks: KnowledgeDocumentChunk[]
}>()

defineEmits<{
  (event: 'close'): void
  (event: 'toggle-chunks'): void
  (event: 'reprocess'): void
}>()

function assetPreviewUrl(assetId: number) {
  return `/api/knowledge-documents/assets/${assetId}/preview`
}

function assetDownloadUrl(assetId: number) {
  return `/api/knowledge-documents/assets/${assetId}/download`
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 60;
}
.panel {
  width: min(920px, 100%);
  max-height: 85vh;
  overflow: auto;
  background: #fff;
  border-radius: 20px;
  padding: 18px;
}
.panel-head,
.section-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}
h3 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}
.actions {
  display: flex;
  gap: 8px;
}
.detail-body {
  margin-top: 16px;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 10px;
}
.block {
  margin-top: 14px;
}
.block p {
  white-space: pre-wrap;
}
.chunk-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 10px;
  background: #fafcff;
  margin-top: 8px;
}
.simple-list {
  margin: 8px 0 0;
  padding-left: 18px;
}
.asset-item {
  margin-bottom: 8px;
}
.asset-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
  flex-wrap: wrap;
}
.link-button {
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}
.muted,
.empty-state {
  color: #6b7280;
}
.ghost,
.mini {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 6px 10px;
  cursor: pointer;
  background: #f3f4f6;
  font-size: 12px;
}
@media (max-width: 960px) {
  .meta-grid {
    grid-template-columns: 1fr;
  }
  .panel-head {
    flex-direction: column;
  }
}
</style>
