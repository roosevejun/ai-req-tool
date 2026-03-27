<template>
  <section class="card">
    <h3>创建 AI 项目会话</h3>
    <div class="grid two">
      <input v-model.trim="startForm.projectName" class="input" placeholder="输入项目名称，例如 AI 巡检平台" />
      <textarea v-model="startForm.description" class="input" placeholder="描述项目背景、目标用户、核心流程，以及你当前已知的信息" />
    </div>

    <h3>补充资料</h3>
    <div class="materials">
      <div class="material-card">
        <div class="section-head">
          <strong>网站链接</strong>
          <button class="mini" type="button" @click="$emit('add-url-material')">添加</button>
        </div>
        <input v-model.trim="urlDraft.title" class="input" placeholder="链接标题，可选" />
        <input v-model.trim="urlDraft.sourceUri" class="input" placeholder="https://example.com" />
      </div>

      <div class="material-card">
        <div class="section-head">
          <strong>文本资料</strong>
          <button class="mini" type="button" @click="$emit('add-text-material')">添加</button>
        </div>
        <input v-model.trim="textDraft.title" class="input" placeholder="资料标题，可选" />
        <textarea v-model="textDraft.rawContent" class="input" placeholder="粘贴文档内容、会议纪要、业务说明或其他文字资料" />
      </div>

      <div class="material-card">
        <div class="section-head">
          <strong>文件资料</strong>
        </div>
        <input ref="fileInputRef" class="input" type="file" :disabled="loading || !sessionId" @change="onFileSelect" />
        <input v-model.trim="fileDraft.title" class="input" :disabled="loading || !sessionId" placeholder="文件标题，可选" />
        <div class="row">
          <button class="ghost" type="button" :disabled="loading || !sessionId || !selectedFile" @click="$emit('upload-file')">
            上传文件资料
          </button>
          <span class="muted">{{ selectedFile ? selectedFile.name : '请先启动会话后再上传文件' }}</span>
        </div>
      </div>
    </div>

    <div v-if="pendingMaterials.length > 0" class="pending-list">
      <div class="section-head">
        <strong>待保存资料</strong>
        <button class="ghost mini" type="button" @click="$emit('clear-pending-materials')">清空</button>
      </div>
      <div v-for="(item, idx) in pendingMaterials" :key="`pending-${idx}`" class="pending-item">
        <div>{{ item.materialType }} / {{ item.title || '未命名资料' }}</div>
        <div class="muted">{{ item.sourceUri || previewText(item.rawContent) }}</div>
      </div>
    </div>

    <div v-if="savedMaterials.length > 0" class="pending-list">
      <div class="section-head">
        <strong>已保存资料</strong>
        <span class="muted">{{ savedMaterials.length }} 条</span>
      </div>
      <div v-for="(item, idx) in savedMaterials" :key="`saved-${idx}`" class="pending-item">
        <div>{{ item.materialType }} / {{ item.title || '未命名资料' }}</div>
        <div class="muted">{{ item.sourceUri || previewText(item.rawContent) }}</div>
        <div v-if="materialKnowledgeItems(item).length > 0" class="knowledge-status-list">
          <div v-for="doc in materialKnowledgeItems(item)" :key="doc.id" class="knowledge-status-block">
            <div class="knowledge-status-item">
              <span class="status-badge" :class="doc.status?.toLowerCase()">{{ knowledgeStatusText(doc.status) }}</span>
              <span class="muted">{{ doc.documentType }} / {{ doc.title || '知识文档' }}</span>
              <button class="ghost mini" type="button" @click="$emit('open-knowledge-detail', doc.id)">查看详情</button>
            </div>
            <div v-if="doc.latestTaskError" class="knowledge-error">错误：{{ doc.latestTaskError }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="row">
      <button class="primary" type="button" :disabled="loading || !canStartConversation" @click="$emit('start-conversation')">
        启动 AI 会话
      </button>
      <button class="ghost" type="button" :disabled="loading || !sessionId || pendingMaterials.length === 0" @click="$emit('save-materials')">
        保存待上传资料
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { knowledgeStatusText, previewText } from './helpers'
import type {
  FileDraftState,
  KnowledgeDocumentListItem,
  SourceMaterial,
  StartFormState,
  TextDraftState,
  UrlDraftState
} from './types'

const fileInputRef = ref<HTMLInputElement | null>(null)

const props = defineProps<{
  loading: boolean
  sessionId: number | null
  startForm: StartFormState
  urlDraft: UrlDraftState
  textDraft: TextDraftState
  fileDraft: FileDraftState
  selectedFile: File | null
  canStartConversation: boolean
  pendingMaterials: SourceMaterial[]
  savedMaterials: SourceMaterial[]
  materialKnowledgeMap: Record<number, KnowledgeDocumentListItem[]>
}>()

const emit = defineEmits<{
  (event: 'add-url-material'): void
  (event: 'add-text-material'): void
  (event: 'clear-pending-materials'): void
  (event: 'start-conversation'): void
  (event: 'save-materials'): void
  (event: 'upload-file'): void
  (event: 'file-selected', file: File | null): void
  (event: 'open-knowledge-detail', documentId: number): void
}>()

function onFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  emit('file-selected', input.files?.[0] || null)
}

function materialKnowledgeItems(material: SourceMaterial) {
  return material.id ? props.materialKnowledgeMap[material.id] || [] : []
}

defineExpose({
  resetFileInput() {
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  }
})
</script>

<style scoped>
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 14px; margin-bottom: 14px; }
.grid { display: grid; gap: 10px; }
.grid.two { grid-template-columns: 1fr 1.3fr; }
.input { width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 8px; padding: 9px 10px; background: #fff; }
textarea.input { min-height: 92px; resize: vertical; }
.materials { display: grid; gap: 12px; margin-top: 10px; }
.material-card, .pending-item { border: 1px solid #e5e7eb; border-radius: 10px; padding: 10px; background: #fafcff; }
.pending-list { margin-top: 12px; }
.knowledge-status-list, .knowledge-status-block { display: grid; gap: 6px; }
.knowledge-status-item, .section-head, .row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.section-head { justify-content: space-between; margin-bottom: 8px; }
.muted { color: #6b7280; }
.knowledge-error { color: #b91c1c; }
.primary, .ghost, .mini { border-radius: 8px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; border-color: #2563eb; color: #fff; }
.ghost, .mini { background: #f3f4f6; }
.mini { padding: 5px 9px; font-size: 12px; }
.status-badge { padding: 2px 8px; border-radius: 999px; background: #e5e7eb; color: #374151; font-size: 12px; }
.status-badge.ready, .status-badge.success { background: #dcfce7; color: #166534; }
.status-badge.failed { background: #fee2e2; color: #b91c1c; }
.status-badge.pending, .status-badge.processing, .status-badge.running { background: #dbeafe; color: #1d4ed8; }
@media (max-width: 960px) { .grid.two { grid-template-columns: 1fr; } }
</style>
