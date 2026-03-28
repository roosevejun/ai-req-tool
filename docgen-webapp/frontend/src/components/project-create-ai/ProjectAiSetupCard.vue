<template>
  <WorkspaceSection
    eyebrow="辅助输入"
    title="项目起点与佐证资料"
    description="这里负责给 AI 一个起点，并补充能帮助 AI 校准理解的资料。主工作区仍以对话和框架提炼为主。"
  >
    <div class="grid two">
      <input v-model.trim="startForm.projectName" class="input" placeholder="先写下项目名称，或一个临时工作名" />
      <textarea
        v-model="startForm.description"
        class="input"
        placeholder="补充项目背景、想解决的问题、目标用户，或当前已有的业务设想。"
      />
    </div>

    <div class="materials-header">
      <div>
        <p class="section-label">知识输入</p>
        <h4>补充佐证资料</h4>
      </div>
      <StatusBadge :label="sessionId ? '会话已启动' : '等待启动会话'" :variant="sessionId ? 'success' : 'warning'" small />
    </div>

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
        <input v-model.trim="textDraft.title" class="input" placeholder="文本标题，可选" />
        <textarea
          v-model="textDraft.rawContent"
          class="input"
          placeholder="把会议纪要、需求说明、竞品分析或你的业务构想直接贴进来。"
        />
      </div>

      <div class="material-card">
        <div class="section-head">
          <strong>文件资料</strong>
        </div>
        <input ref="fileInputRef" class="input" type="file" :disabled="loading || !sessionId" @change="onFileSelect" />
        <input v-model.trim="fileDraft.title" class="input" :disabled="loading || !sessionId" placeholder="文件标题，可选" />
        <div class="row">
          <button class="ghost" type="button" :disabled="loading || !sessionId || !selectedFile" @click="$emit('upload-file')">
            上传文件
          </button>
          <span class="muted">{{ selectedFile ? selectedFile.name : '尚未选择文件' }}</span>
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
      <button class="primary" type="button" :disabled="loading || !canStartConversation" @click="$emit('start-conversation')">
        启动 AI 会话
      </button>
      <button class="ghost" type="button" :disabled="loading || !sessionId || pendingMaterials.length === 0" @click="$emit('save-materials')">
        保存资料到会话
      </button>
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
.materials-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 18px 0 10px; }
.section-label { margin: 0 0 6px; font-size: 12px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; color: #64748b; }
h4 { margin: 0; color: #0f172a; }
.materials { display: grid; gap: 12px; margin-top: 10px; }
.material-card, .pending-item { border: 1px solid #e5e7eb; border-radius: 14px; padding: 12px; background: #fafcff; }
.pending-list { margin-top: 12px; border: 1px solid #e5e7eb; border-radius: 16px; padding: 14px; background: #fff; }
.knowledge-status-list, .knowledge-status-block { display: grid; gap: 6px; }
.knowledge-status-item, .section-head, .row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.section-head { justify-content: space-between; margin-bottom: 8px; }
.muted { color: #6b7280; }
.knowledge-error { color: #b91c1c; }
.primary, .ghost, .mini { border-radius: 10px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; border-color: #2563eb; color: #fff; }
.ghost, .mini { background: #f3f4f6; }
.mini { padding: 5px 9px; font-size: 12px; }
@media (max-width: 960px) { .grid.two { grid-template-columns: 1fr; } .materials-header { flex-direction: column; align-items: flex-start; } }
</style>
