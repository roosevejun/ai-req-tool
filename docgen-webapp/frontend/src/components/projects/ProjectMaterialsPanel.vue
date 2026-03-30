<template>
  <WorkspaceSection
    eyebrow="资料与知识"
    title="资料归档与知识状态"
    description="把链接、文本和文件沉淀到项目知识中，跟踪处理状态并查看失败原因。"
    tint
  >
    <div class="materials-grid">
      <div class="material-card">
        <div class="panel-head">
          <strong>网站链接</strong>
          <button class="ghost mini" type="button" @click="$emit('add-project-url-material')">添加</button>
        </div>
        <input v-model.trim="projectUrlDraft.title" class="input" placeholder="链接标题，可选" />
        <input v-model.trim="projectUrlDraft.sourceUri" class="input" placeholder="https://example.com" />
      </div>

      <div class="material-card">
        <div class="panel-head">
          <strong>文本资料</strong>
          <button class="ghost mini" type="button" @click="$emit('add-project-text-material')">添加</button>
        </div>
        <input v-model.trim="projectTextDraft.title" class="input" placeholder="文本标题，可选" />
        <textarea v-model="projectTextDraft.rawContent" class="input textarea" placeholder="把项目补充说明、会议纪要或业务资料直接贴进来。" />
      </div>

      <div class="material-card">
        <div class="panel-head">
          <strong>文件资料</strong>
          <span class="muted">上传后进入当前 AI 会话</span>
        </div>
        <input class="input" type="file" :disabled="projectConversationLoading || !projectConversation" @change="onProjectFileChange" />
        <input
          v-model.trim="projectFileDraft.title"
          class="input"
          :disabled="projectConversationLoading || !projectConversation"
          placeholder="文件标题，可选"
        />
        <div class="row">
          <button class="ghost mini" type="button" :disabled="projectConversationLoading || !canUploadProjectFile" @click="$emit('upload-project-file')">
            上传文件
          </button>
          <span class="muted">{{ projectSelectedFile ? projectSelectedFile.name : '尚未选择文件' }}</span>
        </div>
      </div>
    </div>

    <div v-if="projectPendingMaterials.length" class="pending-list">
      <div class="panel-head">
        <strong>待保存资料</strong>
        <button class="ghost mini" type="button" @click="$emit('clear-project-pending-materials')">清空</button>
      </div>
      <div v-for="(item, index) in projectPendingMaterials" :key="`pending-${index}`" class="pending-item">
        <div>{{ item.materialType }} / {{ item.title || '未命名资料' }}</div>
        <div class="muted">{{ item.sourceUri || projectMaterialPreview(item.rawContent) }}</div>
      </div>
      <div class="row">
        <button class="primary mini" type="button" :disabled="projectConversationLoading || !canSaveProjectMaterials" @click="$emit('save-project-materials')">
          保存资料到会话
        </button>
      </div>
    </div>

    <div v-if="projectConversation?.materials?.length" class="pending-list">
      <div class="panel-head">
        <strong>已保存资料</strong>
        <span class="muted">{{ projectConversation.materials.length }} 条</span>
      </div>

      <div class="row compact material-toolbar">
        <select
          class="input filter-select"
          :value="projectMaterialFilter"
          @change="$emit('update-project-material-filter', ($event.target as HTMLSelectElement).value as 'ALL' | 'URL' | 'TEXT' | 'FILE')"
        >
          <option value="ALL">全部</option>
          <option value="URL">网站链接</option>
          <option value="TEXT">文本资料</option>
          <option value="FILE">文件资料</option>
        </select>
        <button class="ghost mini" type="button" @click="$emit('toggle-project-materials-collapse')">
          {{ projectMaterialsCollapsed ? '展开列表' : '收起列表' }}
        </button>
        <span class="muted">{{ filteredProjectMaterials.length }} 条可见资料</span>
      </div>

      <div v-if="projectMaterialsCollapsed" class="muted collapsed-tip">资料列表已收起，你可以展开后继续查看知识状态。</div>

      <template v-else>
        <div v-for="material in filteredProjectMaterials" :key="material.id || material.title" class="pending-item">
          <div class="panel-head">
            <strong>{{ material.materialType }} / {{ material.title || '未命名资料' }}</strong>
            <button class="danger mini" type="button" :disabled="projectConversationLoading" @click="$emit('delete-project-material', material.id)">
              删除
            </button>
          </div>
          <div class="muted">{{ material.sourceUri || projectMaterialPreview(material.rawContent) }}</div>
          <div v-if="material.id && projectMaterialKnowledgeMap[material.id]?.length" class="knowledge-list">
            <div v-for="doc in projectMaterialKnowledgeMap[material.id]" :key="doc.id" class="knowledge-item">
              <StatusBadge :label="projectKnowledgeStatusText(doc.status)" variant="neutral" small />
              <StatusBadge :label="projectKnowledgeStatusText(doc.latestTaskStatus)" variant="info" small />
              <span class="muted">{{ doc.documentType }} / {{ doc.title || '未命名文档' }}</span>
              <div class="row compact">
                <button class="ghost mini" type="button" @click="$emit('open-project-knowledge-detail', doc.id)">查看详情</button>
                <button class="ghost mini" type="button" @click="$emit('open-project-knowledge-library', doc.id)">进入知识库</button>
                <button
                  v-if="doc.status === 'FAILED' || doc.latestTaskStatus === 'FAILED'"
                  class="ghost mini"
                  type="button"
                  @click="$emit('retry-project-knowledge-document', doc.id)"
                >
                  重新处理
                </button>
              </div>
              <span v-if="doc.latestTaskError" class="knowledge-error">{{ doc.latestTaskError }}</span>
            </div>
          </div>
        </div>

        <p v-if="filteredProjectMaterials.length === 0" class="muted collapsed-tip">当前筛选条件下没有资料。</p>
      </template>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import WorkspaceSection from './WorkspaceSection.vue'
import type {
  FileDraftState,
  ProjectConversationView,
  ProjectKnowledgeDocumentListItem,
  ProjectSourceMaterial,
  TextDraftState,
  UrlDraftState
} from './types'

defineProps<{
  projectConversationLoading: boolean
  projectConversation: ProjectConversationView | null
  projectUrlDraft: UrlDraftState
  projectTextDraft: TextDraftState
  projectFileDraft: FileDraftState
  projectSelectedFile: File | null
  projectPendingMaterials: ProjectSourceMaterial[]
  projectMaterialKnowledgeMap: Record<number, ProjectKnowledgeDocumentListItem[]>
  projectMaterialFilter: 'ALL' | 'URL' | 'TEXT' | 'FILE'
  filteredProjectMaterials: ProjectSourceMaterial[]
  projectMaterialsCollapsed: boolean
  canSaveProjectMaterials: boolean
  canUploadProjectFile: boolean
  projectMaterialPreview: (value?: string) => string
  projectKnowledgeStatusText: (value?: string) => string
}>()

const emit = defineEmits<{
  (event: 'add-project-url-material'): void
  (event: 'add-project-text-material'): void
  (event: 'clear-project-pending-materials'): void
  (event: 'save-project-materials'): void
  (event: 'select-project-file', file: File | null): void
  (event: 'upload-project-file'): void
  (event: 'delete-project-material', materialId?: number): void
  (event: 'open-project-knowledge-detail', documentId: number): void
  (event: 'open-project-knowledge-library', documentId: number): void
  (event: 'retry-project-knowledge-document', documentId: number): void
  (event: 'update-project-material-filter', value: 'ALL' | 'URL' | 'TEXT' | 'FILE'): void
  (event: 'toggle-project-materials-collapse'): void
}>()

function onProjectFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  emit('select-project-file', input.files?.[0] || null)
}
</script>

<style scoped>
.materials-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.material-card,
.pending-list {
  border: 1px solid #dbe5ef;
  border-radius: 16px;
  background: #fff;
  padding: 14px;
}

.panel-head,
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.row {
  flex-wrap: wrap;
}

.compact {
  margin-top: 0;
}

.input {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 10px 12px;
  font: inherit;
  color: #0f172a;
  background: #fff;
  box-sizing: border-box;
}

.textarea {
  min-height: 108px;
  resize: vertical;
}

.muted {
  color: #475569;
  line-height: 1.65;
  font-size: 13px;
  white-space: pre-wrap;
}

.pending-item,
.knowledge-item {
  display: grid;
  gap: 8px;
}

.pending-item + .pending-item,
.knowledge-item + .knowledge-item {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #dbe2ea;
}

.knowledge-list {
  display: grid;
  gap: 8px;
}

.knowledge-error {
  color: #b91c1c;
}

.filter-select {
  min-width: 140px;
  max-width: 180px;
}

.collapsed-tip {
  margin: 0;
}

.primary,
.ghost,
.danger {
  border-radius: 999px;
  padding: 8px 14px;
  border: 1px solid transparent;
  cursor: pointer;
}

.primary {
  background: #1d4ed8;
  color: #fff;
}

.ghost {
  background: #fff;
  border-color: #cbd5e1;
  color: #334155;
}

.danger {
  background: #fff5f5;
  border-color: #fecaca;
  color: #b91c1c;
}

.mini {
  font-size: 13px;
}

@media (max-width: 1180px) {
  .materials-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
