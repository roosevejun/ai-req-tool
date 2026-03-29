<template>
  <WorkspaceSection
    eyebrow="辅助输入"
    title="补充资料"
    description="补充链接、原文或文件，帮助 AI 校准理解。"
  >
    <div class="materials-topbar">
      <div>
        <p class="topbar-title">选择一种方式补充资料</p>
        <p class="topbar-note">项目想法请继续在主工作区和 AI 沟通，这里只补充不便口头表达的信息。</p>
      </div>
      <StatusBadge :label="sessionId ? '已关联当前会话' : '首条消息发送时一并提交'" :variant="sessionId ? 'success' : 'warning'" small />
    </div>

    <div class="material-mode-switch">
      <button class="mode-chip" :class="{ active: activeMode === 'url' }" type="button" @click="activeMode = 'url'">网站链接</button>
      <button class="mode-chip" :class="{ active: activeMode === 'text' }" type="button" @click="activeMode = 'text'">原文资料</button>
      <button class="mode-chip" :class="{ active: activeMode === 'file' }" type="button" @click="activeMode = 'file'">文件资料</button>
    </div>

    <div class="materials">
      <div v-if="activeMode === 'url'" class="material-card">
        <div class="section-head">
          <strong>网站链接</strong>
          <button class="mini" type="button" @click="$emit('add-url-material')">添加</button>
        </div>
        <input v-model.trim="urlDraft.title" class="input" placeholder="链接标题，可选" />
        <input v-model.trim="urlDraft.sourceUri" class="input" placeholder="https://example.com" />
      </div>

      <div v-else-if="activeMode === 'text'" class="material-card">
        <div class="section-head">
          <strong>补充原文资料</strong>
          <button class="mini" type="button" @click="$emit('add-text-material')">添加</button>
        </div>
        <input v-model.trim="textDraft.title" class="input" placeholder="资料标题，可选" />
        <textarea
          v-model="textDraft.rawContent"
          class="input"
          placeholder="可粘贴会议纪要、调研摘要、客户原话、竞品资料等较长原文。"
        />
      </div>

      <div v-else class="material-card">
        <div class="section-head">
          <strong>文件资料</strong>
        </div>
        <input ref="fileInputRef" class="input" type="file" :disabled="loading || !sessionId" @change="onFileSelect" />
        <input v-model.trim="fileDraft.title" class="input" :disabled="loading || !sessionId" placeholder="文件标题，可选" />
        <div class="row">
          <button class="ghost" type="button" :disabled="loading || !sessionId || !selectedFile" @click="$emit('upload-file')">
            上传文件
          </button>
          <span class="muted">{{ sessionId ? (selectedFile ? selectedFile.name : '尚未选择文件') : '请先在主工作区发送第一条消息后再上传文件' }}</span>
        </div>
      </div>
    </div>

    <div class="materials-summary">
      <div class="summary-card">
        <span class="summary-label">待提交资料</span>
        <strong>{{ pendingMaterials.length }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">已保存资料</span>
        <strong>{{ savedMaterials.length }}</strong>
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
              <StatusBadge :label="knowledgeStatusText(doc.status)" small />
              <span class="muted">{{ doc.documentType }} / {{ doc.title || '未命名文档' }}</span>
              <button
                v-if="doc.status === 'FAILED' || doc.latestTaskStatus === 'FAILED'"
                class="ghost mini"
                type="button"
                @click="$emit('retry-knowledge-document', doc.id)"
              >
                重新处理
              </button>
              <button class="ghost mini" type="button" @click="$emit('open-knowledge-detail', doc.id)">进入知识库</button>
            </div>
            <div v-if="doc.latestTaskError" class="knowledge-error">错误：{{ doc.latestTaskError }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="row">
      <button class="ghost" type="button" :disabled="loading || !sessionId || pendingMaterials.length === 0" @click="$emit('save-materials')">
        保存资料到会话
      </button>
      <span v-if="!sessionId" class="muted">首次发送消息时，待提交资料会自动跟随会话一起创建。</span>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import StatusBadge from '../projects/StatusBadge.vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'
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
const activeMode = ref<'url' | 'text' | 'file'>('url')

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
  (event: 'retry-knowledge-document', documentId: number): void
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
.grid { display: grid; gap: 10px; }
.grid.two { grid-template-columns: 1fr 1.3fr; }
.input { width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 10px; padding: 9px 10px; background: #fff; }
textarea.input { min-height: 92px; resize: vertical; }
.materials-topbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin: 12px 0 10px; }
.topbar-title { margin: 0; color: #0f172a; font-size: 15px; font-weight: 700; }
.topbar-note { margin: 4px 0 0; color: #64748b; font-size: 13px; line-height: 1.6; }
.material-mode-switch { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.mode-chip { border-radius: 999px; border: 1px solid #d1d5db; background: #fff; color: #475569; padding: 7px 11px; cursor: pointer; }
.mode-chip.active { border-color: #2563eb; background: #eff6ff; color: #1d4ed8; }
.materials { display: grid; gap: 12px; margin-top: 10px; }
.material-card, .pending-item { border: 1px solid #e5e7eb; border-radius: 14px; padding: 12px; background: #fafcff; }
.pending-list { margin-top: 12px; border: 1px solid #e5e7eb; border-radius: 16px; padding: 14px; background: #fff; }
.materials-summary { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-top: 12px; }
.summary-card { border: 1px solid #e5e7eb; border-radius: 14px; background: #fff; padding: 10px 12px; display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.summary-label { color: #64748b; font-size: 13px; }
.knowledge-status-list, .knowledge-status-block { display: grid; gap: 6px; }
.knowledge-status-item, .section-head, .row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.section-head { justify-content: space-between; margin-bottom: 8px; }
.muted { color: #6b7280; }
.knowledge-error { color: #b91c1c; }
.primary, .ghost, .mini { border-radius: 10px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; border-color: #2563eb; color: #fff; }
.ghost, .mini { background: #f3f4f6; }
.mini { padding: 5px 9px; font-size: 12px; }
@media (max-width: 960px) { .grid.two { grid-template-columns: 1fr; } .materials-topbar { flex-direction: column; align-items: flex-start; } .materials-summary { grid-template-columns: 1fr; } }
</style>
