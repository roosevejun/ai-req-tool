<template>
  <WorkspaceSection
    eyebrow="AI 协同"
    title="AI 协同优化"
    description="在当前项目上下文里继续对话、补资料、看检索结果，并把 AI 结果回填到项目表单。"
    tint
  >
    <template #actions>
      <button class="primary mini" :disabled="loading || !projectEditForm.projectName" @click="$emit('save-edit')">保存修改</button>
      <button class="ghost mini" :disabled="loading" @click="$emit('cancel-edit')">取消编辑</button>
    </template>

    <div class="meta-grid compact-grid">
      <div><strong>ID:</strong> {{ project.id }}</div>
      <div><strong>项目 Key:</strong> {{ project.projectKey }}</div>
    </div>

    <div class="form-grid">
      <input v-model.trim="projectEditForm.projectName" class="input" placeholder="项目名称" />
      <select v-model="projectEditForm.projectType" class="input">
        <option value="">请选择项目类型</option>
        <option value="PRODUCT">产品型</option>
        <option value="PLATFORM">平台型</option>
        <option value="OPS">运营型</option>
        <option value="INTEGRATION">集成型</option>
      </select>
      <select v-model="projectEditForm.priority" class="input">
        <option value="P0">P0</option>
        <option value="P1">P1</option>
        <option value="P2">P2</option>
        <option value="P3">P3</option>
      </select>
      <select v-model="projectEditForm.visibility" class="input">
        <option value="PRIVATE">私有</option>
        <option value="ORG">组织内</option>
      </select>
      <select v-model="projectEditForm.status" class="input">
        <option value="ACTIVE">进行中</option>
        <option value="ARCHIVED">已归档</option>
        <option value="PAUSED">已暂停</option>
      </select>
      <select v-model="projectEditForm.ownerUserId" class="input">
        <option value="">请选择负责人</option>
        <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">
          {{ u.displayName || u.username }} ({{ u.username }})
        </option>
      </select>
      <input v-model="projectEditForm.startDate" type="date" class="input" />
      <input v-model="projectEditForm.targetDate" type="date" class="input" />
      <input v-model.trim="projectEditForm.tags" class="input" placeholder="标签，多个用逗号分隔" />
    </div>

    <textarea v-model="projectEditForm.description" class="input textarea" placeholder="项目描述" />

    <div class="panel-grid">
      <section class="panel">
        <div class="panel-head">
          <h4>项目信息补充</h4>
          <button class="danger mini" :disabled="loading" @click="$emit('delete-project')">删除项目</button>
        </div>
        <div class="field-stack">
          <textarea v-model="projectEditForm.projectBackground" class="input textarea" placeholder="项目背景" />
          <textarea v-model="projectEditForm.similarProducts" class="input textarea" placeholder="类似产品或竞品信息" />
          <textarea v-model="projectEditForm.targetCustomerGroups" class="input textarea" placeholder="目标客户群体" />
          <textarea v-model="projectEditForm.commercialValue" class="input textarea" placeholder="商业价值" />
          <textarea v-model="projectEditForm.coreProductValue" class="input textarea" placeholder="核心产品价值" />
        </div>
      </section>

      <section class="panel conversation-panel">
        <div class="panel-head">
          <h4>AI 对话</h4>
          <div class="row compact">
            <button class="ghost mini" :disabled="projectConversationLoading" @click="$emit('refresh-project-ai')">
              {{ projectConversation ? '刷新会话' : '启动 AI' }}
            </button>
            <button
              class="ghost mini"
              :disabled="projectConversationLoading || !projectConversation"
              @click="$emit('apply-project-ai')"
            >
              应用 AI 结果
            </button>
          </div>
        </div>

        <p class="summary">你可以继续补充业务信息、贴资料、上传文件，再让 AI 帮你优化项目信息并沉淀知识。</p>

        <div v-if="projectConversation" class="ai-status">
          <strong>会话状态</strong>
          <StatusBadge :label="projectConversation.status || '-'" variant="ai" small />
          <StatusBadge v-if="projectConversation.readyToCreate" label="结果已可应用" variant="success" small />
        </div>

        <div v-if="projectConversation?.assistantSummary" class="ai-assistant">
          <strong>AI 最新摘要</strong> {{ projectConversation.assistantSummary }}
        </div>

        <div v-if="projectConversation?.messages?.length" class="ai-history">
          <div v-for="message in projectConversation.messages.slice(-6)" :key="message.id" class="ai-message" :class="message.role">
            <div class="ai-message-role">{{ message.role === 'assistant' ? 'AI' : '你' }}</div>
            <div class="ai-message-content">{{ message.content }}</div>
          </div>
        </div>

        <textarea
          :value="projectConversationInput"
          class="input textarea"
          placeholder="继续告诉 AI 你想补充什么，例如市场背景、客户痛点、差异化能力等。"
          @input="$emit('update-project-ai-input', ($event.target as HTMLTextAreaElement).value)"
        />

        <div class="row">
          <button
            class="primary mini"
            :disabled="projectConversationLoading || !projectConversation || !canSendProjectConversation"
            @click="$emit('send-project-ai')"
          >
            发送给 AI
          </button>
          <button
            class="ghost mini"
            :disabled="projectConversationLoading || !projectConversation"
            @click="$emit('load-project-knowledge-preview')"
          >
            查看检索上下文
          </button>
        </div>

        <div v-if="projectKnowledgePreviewVisible" class="knowledge-preview">
          <div class="panel-head">
            <h4>检索上下文</h4>
            <span class="muted" v-if="projectKnowledgePreviewQueryText">检索词：{{ projectKnowledgePreviewQueryText }}</span>
          </div>
          <div v-if="projectKnowledgePreviewLoading" class="muted">正在加载检索上下文...</div>
          <div v-else-if="projectKnowledgePreview">
            <div class="preview-item">
              <strong>命中片段</strong>
              <p v-if="projectKnowledgePreview.items.length === 0">当前没有命中可用知识片段。</p>
              <p v-for="item in projectKnowledgePreview.items" :key="`${item.documentId}-${item.chunkId}`">
                文档 #{{ item.documentId }} / Chunk #{{ item.chunkNo }} / 分数
                {{ typeof item.score === 'number' ? item.score.toFixed(3) : '-' }}<br />
                {{ item.chunkText }}
              </p>
            </div>
            <div class="preview-item">
              <strong>拼接后的上下文</strong>
              <p>{{ projectKnowledgePreview.contextText || '-' }}</p>
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="panel material-panel">
      <div class="panel-head">
        <h4>资料与知识状态</h4>
        <span class="muted">继续补充链接、文本或文件，AI 会基于这些内容更新知识文档。</span>
      </div>

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
          </div>
          <input class="input" type="file" :disabled="projectConversationLoading || !projectConversation" @change="onProjectFileChange" />
          <input
            v-model.trim="projectFileDraft.title"
            class="input"
            :disabled="projectConversationLoading || !projectConversation"
            placeholder="文件标题，可选"
          />
          <div class="row">
            <button
              class="ghost mini"
              type="button"
              :disabled="projectConversationLoading || !canUploadProjectFile"
              @click="$emit('upload-project-file')"
            >
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
          <button
            class="primary mini"
            type="button"
            :disabled="projectConversationLoading || !canSaveProjectMaterials"
            @click="$emit('save-project-materials')"
          >
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
              <button
                class="danger mini"
                type="button"
                :disabled="projectConversationLoading"
                @click="$emit('delete-project-material', material.id)"
              >
                删除
              </button>
            </div>
            <div class="muted">{{ material.sourceUri || projectMaterialPreview(material.rawContent) }}</div>
            <div v-if="material.id && projectMaterialKnowledgeMap[material.id]?.length" class="knowledge-list">
              <div v-for="doc in projectMaterialKnowledgeMap[material.id]" :key="doc.id" class="knowledge-item">
                <StatusBadge :label="projectKnowledgeStatusText(doc.status)" variant="neutral" small />
                <StatusBadge :label="projectKnowledgeStatusText(doc.latestTaskStatus)" variant="info" small />
                <span class="muted">{{ doc.documentType }} / {{ doc.title || '未命名文档' }}</span>
                <button class="ghost mini" type="button" @click="$emit('open-project-knowledge-detail', doc.id)">进入知识库</button>
                <button
                  v-if="doc.status === 'FAILED' || doc.latestTaskStatus === 'FAILED'"
                  class="ghost mini"
                  type="button"
                  @click="$emit('retry-project-knowledge-document', doc.id)"
                >
                  重新处理
                </button>
                <span v-if="doc.latestTaskError" class="knowledge-error">{{ doc.latestTaskError }}</span>
              </div>
            </div>
          </div>

          <p v-if="filteredProjectMaterials.length === 0" class="muted collapsed-tip">当前筛选条件下没有资料。</p>
        </template>

        <p class="muted">如需查看资源、分块和任务历史，可以直接进入独立知识库页。</p>
      </div>
    </section>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import WorkspaceSection from './WorkspaceSection.vue'
import type {
  FileDraftState,
  ProjectConversationView,
  ProjectEditFormState,
  ProjectItem,
  ProjectKnowledgeDocumentListItem,
  ProjectKnowledgePreview,
  ProjectSourceMaterial,
  TextDraftState,
  UrlDraftState,
  UserOption
} from './types'

defineProps<{
  loading: boolean
  project: ProjectItem
  projectEditForm: ProjectEditFormState
  projectConversationLoading: boolean
  projectConversation: ProjectConversationView | null
  projectConversationInput: string
  canSendProjectConversation: boolean
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
  projectKnowledgePreviewVisible: boolean
  projectKnowledgePreviewLoading: boolean
  projectKnowledgePreview: ProjectKnowledgePreview | null
  projectKnowledgePreviewQueryText: string
  userOptions: UserOption[]
}>()

const emit = defineEmits<{
  (event: 'cancel-edit'): void
  (event: 'save-edit'): void
  (event: 'delete-project'): void
  (event: 'refresh-project-ai'): void
  (event: 'send-project-ai'): void
  (event: 'apply-project-ai'): void
  (event: 'update-project-ai-input', value: string): void
  (event: 'add-project-url-material'): void
  (event: 'add-project-text-material'): void
  (event: 'clear-project-pending-materials'): void
  (event: 'save-project-materials'): void
  (event: 'select-project-file', file: File | null): void
  (event: 'upload-project-file'): void
  (event: 'delete-project-material', materialId?: number): void
  (event: 'open-project-knowledge-detail', documentId: number): void
  (event: 'retry-project-knowledge-document', documentId: number): void
  (event: 'load-project-knowledge-preview'): void
  (event: 'update-project-material-filter', value: 'ALL' | 'URL' | 'TEXT' | 'FILE'): void
  (event: 'toggle-project-materials-collapse'): void
}>()

function onProjectFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  emit('select-project-file', input.files?.[0] || null)
}
</script>

<style scoped>
.meta-grid,
.form-grid,
.panel-grid,
.materials-grid {
  display: grid;
  gap: 12px;
}
.meta-grid,
.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.compact-grid {
  font-size: 14px;
  color: #475569;
}
.panel-grid {
  grid-template-columns: 1.05fr 1.15fr;
  align-items: start;
}
.materials-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.panel,
.material-card,
.pending-list,
.knowledge-preview {
  border: 1px solid #dbe5ef;
  border-radius: 16px;
  background: #fff;
  padding: 14px;
}
.conversation-panel {
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.06) 0%, #ffffff 100%);
}
.material-panel {
  background: #f8fbff;
}
.panel-head,
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.row { flex-wrap: wrap; }
.compact { margin-top: 0; }
.field-stack { display: grid; gap: 10px; }
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
.textarea { min-height: 108px; resize: vertical; }
.summary,
.muted,
.preview-item p,
.ai-message-content,
.ai-assistant {
  color: #475569;
  line-height: 1.65;
  white-space: pre-wrap;
}
.muted { font-size: 13px; }
.ai-status,
.ai-assistant {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  border-radius: 14px;
  padding: 12px 14px;
}
.ai-status { background: #eff6ff; color: #1e3a8a; }
.ai-history { display: grid; gap: 10px; }
.ai-message {
  border: 1px solid #dbe5ef;
  border-radius: 14px;
  padding: 12px 14px;
  background: #fff;
}
.ai-message.assistant { background: #f8fbff; }
.ai-message-role {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  color: #0f766e;
  letter-spacing: 0.08em;
}
.pending-item,
.knowledge-item { display: grid; gap: 8px; }
.pending-item + .pending-item,
.knowledge-item + .knowledge-item {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #dbe2ea;
}
.knowledge-list { display: grid; gap: 8px; }
.knowledge-error { color: #b91c1c; }
.filter-select { min-width: 140px; max-width: 180px; }
.collapsed-tip { margin: 0; }
.preview-item + .preview-item {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #dbe2ea;
}
.primary,
.ghost,
.danger {
  border-radius: 999px;
  padding: 8px 14px;
  border: 1px solid transparent;
  cursor: pointer;
}
.primary { background: #1d4ed8; color: #fff; }
.ghost { background: #fff; border-color: #cbd5e1; color: #334155; }
.danger { background: #fff5f5; border-color: #fecaca; color: #b91c1c; }
.mini { font-size: 13px; }
@media (max-width: 1180px) {
  .panel-grid,
  .materials-grid { grid-template-columns: 1fr; }
}
@media (max-width: 860px) {
  .meta-grid,
  .form-grid { grid-template-columns: 1fr; }
  .panel-head { align-items: flex-start; flex-direction: column; }
}
</style>
