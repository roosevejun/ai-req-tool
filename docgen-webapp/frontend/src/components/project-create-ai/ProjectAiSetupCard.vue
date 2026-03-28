<template>
  <WorkspaceSection
    eyebrow="Project Kickoff"
    title="?? AI ????"
    description="??????????????? AI ????????????????"
  >
    <div class="grid two">
      <input v-model.trim="startForm.projectName" class="input" placeholder="????????? AI ????" />
      <textarea
        v-model="startForm.description"
        class="input"
        placeholder="???????????????????????????"
      />
    </div>

    <div class="materials-header">
      <div>
        <p class="section-label">Knowledge Inputs</p>
        <h4>????</h4>
      </div>
      <StatusBadge :label="sessionId ? '?????' : '??????'" :variant="sessionId ? 'success' : 'warning'" small />
    </div>

    <div class="materials">
      <div class="material-card">
        <div class="section-head">
          <strong>????</strong>
          <button class="mini" type="button" @click="$emit('add-url-material')">??</button>
        </div>
        <input v-model.trim="urlDraft.title" class="input" placeholder="???????" />
        <input v-model.trim="urlDraft.sourceUri" class="input" placeholder="https://example.com" />
      </div>

      <div class="material-card">
        <div class="section-head">
          <strong>????</strong>
          <button class="mini" type="button" @click="$emit('add-text-material')">??</button>
        </div>
        <input v-model.trim="textDraft.title" class="input" placeholder="???????" />
        <textarea
          v-model="textDraft.rawContent"
          class="input"
          placeholder="???????????????????????"
        />
      </div>

      <div class="material-card">
        <div class="section-head">
          <strong>????</strong>
        </div>
        <input ref="fileInputRef" class="input" type="file" :disabled="loading || !sessionId" @change="onFileSelect" />
        <input v-model.trim="fileDraft.title" class="input" :disabled="loading || !sessionId" placeholder="???????" />
        <div class="row">
          <button class="ghost" type="button" :disabled="loading || !sessionId || !selectedFile" @click="$emit('upload-file')">
            ??????
          </button>
          <span class="muted">{{ selectedFile ? selectedFile.name : '????????????' }}</span>
        </div>
      </div>
    </div>

    <div v-if="pendingMaterials.length > 0" class="pending-list">
      <div class="section-head">
        <strong>?????</strong>
        <button class="ghost mini" type="button" @click="$emit('clear-pending-materials')">??</button>
      </div>
      <div v-for="(item, idx) in pendingMaterials" :key="`pending-${idx}`" class="pending-item">
        <div>{{ item.materialType }} / {{ item.title || '?????' }}</div>
        <div class="muted">{{ item.sourceUri || previewText(item.rawContent) }}</div>
      </div>
    </div>

    <div v-if="savedMaterials.length > 0" class="pending-list">
      <div class="section-head">
        <strong>?????</strong>
        <span class="muted">{{ savedMaterials.length }} ?</span>
      </div>
      <div v-for="(item, idx) in savedMaterials" :key="`saved-${idx}`" class="pending-item">
        <div>{{ item.materialType }} / {{ item.title || '?????' }}</div>
        <div class="muted">{{ item.sourceUri || previewText(item.rawContent) }}</div>
        <div v-if="materialKnowledgeItems(item).length > 0" class="knowledge-status-list">
          <div v-for="doc in materialKnowledgeItems(item)" :key="doc.id" class="knowledge-status-block">
            <div class="knowledge-status-item">
              <StatusBadge :label="knowledgeStatusText(doc.status)" small />
              <span class="muted">{{ doc.documentType }} / {{ doc.title || '????' }}</span>
              <button
                v-if="doc.status === 'FAILED' || doc.latestTaskStatus === 'FAILED'"
                class="ghost mini"
                type="button"
                @click="$emit('retry-knowledge-document', doc.id)"
              >
                ????
              </button>
              <button class="ghost mini" type="button" @click="$emit('open-knowledge-detail', doc.id)">进入知识库</button>
            </div>
            <div v-if="doc.latestTaskError" class="knowledge-error">???{{ doc.latestTaskError }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="row">
      <button class="primary" type="button" :disabled="loading || !canStartConversation" @click="$emit('start-conversation')">
        ?? AI ??
      </button>
      <button class="ghost" type="button" :disabled="loading || !sessionId || pendingMaterials.length === 0" @click="$emit('save-materials')">
        ???????
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
