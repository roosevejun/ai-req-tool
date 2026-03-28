<template>
  <section class="card">
    <div class="section-head">
      <h3>项目详情</h3>
      <div class="row compact">
        <button v-if="!editingProject" class="ghost mini" @click="$emit('start-edit')">编辑项目</button>
        <button v-if="!editingProject" class="danger mini" :disabled="loading" @click="$emit('delete-project')">删除项目</button>
        <template v-else>
          <button class="primary mini" :disabled="loading || !projectEditForm.projectName" @click="$emit('save-edit')">
            保存修改
          </button>
          <button class="ghost mini" :disabled="loading" @click="$emit('cancel-edit')">取消</button>
        </template>
      </div>
    </div>

    <template v-if="!editingProject">
      <div class="meta-grid">
        <div><strong>ID:</strong> {{ project.id }}</div>
        <div><strong>项目 Key:</strong> {{ project.projectKey }}</div>
        <div><strong>项目名称:</strong> {{ project.projectName }}</div>
        <div><strong>项目类型:</strong> {{ projectTypeLabel(project.projectType) }}</div>
        <div><strong>优先级:</strong> {{ project.priority || '-' }}</div>
        <div><strong>可见性:</strong> {{ visibilityLabel(project.visibility) }}</div>
        <div><strong>计划开始:</strong> {{ project.startDate || '-' }}</div>
        <div><strong>计划结束:</strong> {{ project.targetDate || '-' }}</div>
        <div><strong>状态:</strong> {{ projectStatusLabel(project.status) }}</div>
        <div><strong>标签:</strong> {{ project.tags || '-' }}</div>
      </div>
      <p class="summary">{{ project.description || '暂无项目描述' }}</p>
      <div class="detail-section">
        <h4>项目背景</h4>
        <p class="summary">{{ project.projectBackground || '暂无项目背景' }}</p>
      </div>
      <div class="detail-section">
        <h4>类似产品</h4>
        <p class="summary">{{ project.similarProducts || '暂无类似产品信息' }}</p>
      </div>
      <div class="detail-section">
        <h4>目标客户群体</h4>
        <p class="summary">{{ project.targetCustomerGroups || '暂无目标客户群体信息' }}</p>
      </div>
      <div class="detail-section">
        <h4>商业价值</h4>
        <p class="summary">{{ project.commercialValue || '暂无商业价值描述' }}</p>
      </div>
      <div class="detail-section">
        <h4>核心产品价值</h4>
        <p class="summary">{{ project.coreProductValue || '暂无核心产品价值描述' }}</p>
      </div>
    </template>

    <template v-else>
      <div class="meta-grid compact-grid">
        <div><strong>ID:</strong> {{ project.id }}</div>
        <div><strong>项目 Key:</strong> {{ project.projectKey }}</div>
      </div>

      <div class="form-grid">
        <input v-model.trim="projectEditForm.projectName" class="input" placeholder="项目名称" />
        <select v-model="projectEditForm.projectType" class="input">
          <option value="">项目类型，可选</option>
          <option value="PRODUCT">产品型</option>
          <option value="PLATFORM">平台型</option>
          <option value="OPS">运维型</option>
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
          <option value="">负责人，保持不变</option>
          <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">
            {{ u.displayName || u.username }} ({{ u.username }})
          </option>
        </select>
        <input v-model="projectEditForm.startDate" type="date" class="input" />
        <input v-model="projectEditForm.targetDate" type="date" class="input" />
        <input v-model.trim="projectEditForm.tags" class="input" placeholder="标签，逗号分隔" />
      </div>

      <textarea v-model="projectEditForm.description" class="input" placeholder="项目描述，可选" />

      <div class="detail-section">
        <h4>产品信息维护</h4>
        <textarea v-model="projectEditForm.projectBackground" class="input" placeholder="项目背景，可选" />
        <textarea v-model="projectEditForm.similarProducts" class="input" placeholder="类似产品 / 参考产品，可选" />
        <textarea v-model="projectEditForm.targetCustomerGroups" class="input" placeholder="目标客户群体，可选" />
        <textarea v-model="projectEditForm.commercialValue" class="input" placeholder="商业价值，可选" />
        <textarea v-model="projectEditForm.coreProductValue" class="input" placeholder="核心产品价值，可选" />
      </div>

      <div class="detail-section ai-edit-section">
        <div class="section-head">
          <h4>AI 协同优化</h4>
          <div class="row compact">
            <button class="ghost mini" :disabled="projectConversationLoading" @click="$emit('refresh-project-ai')">
              {{ projectConversation ? '恢复会话' : '启动 AI' }}
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

        <p class="summary">
          这里会带上当前项目内容和历史会话继续优化。你可以继续提问，也可以继续补充网站、文本和文件资料。
        </p>

        <div v-if="projectConversation" class="ai-status">
          <strong>当前状态:</strong> {{ projectConversation.status || '-' }}
          <span v-if="projectConversation.readyToCreate" class="ai-ready">信息已经比较完整</span>
        </div>

        <div v-if="projectConversation?.assistantSummary" class="ai-assistant">
          <strong>AI 最近建议:</strong> {{ projectConversation.assistantSummary }}
        </div>

        <div class="materials-grid">
          <div class="material-card">
            <div class="section-head">
              <strong>网站链接</strong>
              <button class="ghost mini" type="button" @click="$emit('add-project-url-material')">添加</button>
            </div>
            <input v-model.trim="projectUrlDraft.title" class="input" placeholder="链接标题，可选" />
            <input v-model.trim="projectUrlDraft.sourceUri" class="input" placeholder="https://example.com" />
          </div>

          <div class="material-card">
            <div class="section-head">
              <strong>文本资料</strong>
              <button class="ghost mini" type="button" @click="$emit('add-project-text-material')">添加</button>
            </div>
            <input v-model.trim="projectTextDraft.title" class="input" placeholder="资料标题，可选" />
            <textarea v-model="projectTextDraft.rawContent" class="input" placeholder="粘贴会议纪要、业务说明、用户反馈等文本资料" />
          </div>

          <div class="material-card">
            <div class="section-head">
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
                上传文件资料
              </button>
              <span class="muted">{{ projectSelectedFile ? projectSelectedFile.name : '先启动会话后再上传文件' }}</span>
            </div>
          </div>
        </div>

        <div v-if="projectPendingMaterials.length" class="pending-list">
          <div class="section-head">
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
          <div class="section-head">
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
              {{ projectMaterialsCollapsed ? '展开资料' : '收起资料' }}
            </button>
            <span class="muted">{{ filteredProjectMaterials.length }} 条符合当前筛选</span>
          </div>

          <div v-if="projectMaterialsCollapsed" class="muted collapsed-tip">资料列表已收起，可按类型筛选后再展开查看。</div>

          <template v-else>
            <div v-for="material in filteredProjectMaterials" :key="material.id || material.title" class="pending-item">
              <div class="section-head">
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
                  <span class="status-badge">{{ projectKnowledgeStatusText(doc.status) }}</span>
                  <span class="status-badge task">{{ projectKnowledgeStatusText(doc.latestTaskStatus) }}</span>
                  <span class="muted">{{ doc.documentType }} / {{ doc.title || '知识文档' }}</span>
                  <button class="ghost mini" type="button" @click="$emit('open-project-knowledge-detail', doc.id)">查看详情</button>
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

            <p v-if="filteredProjectMaterials.length === 0" class="muted collapsed-tip">当前筛选条件下还没有资料。</p>
          </template>

          <p class="muted">如需替换资料，先删除旧资料，再添加新的链接、文本或文件即可。</p>
        </div>

        <div v-if="projectConversation?.messages?.length" class="ai-history">
          <div v-for="message in projectConversation.messages.slice(-6)" :key="message.id" class="ai-message" :class="message.role">
            <div class="ai-message-role">{{ message.role === 'assistant' ? 'AI' : '你' }}</div>
            <div class="ai-message-content">{{ message.content }}</div>
          </div>
        </div>

        <textarea
          :value="projectConversationInput"
          class="input"
          placeholder="继续补充信息，或让 AI 帮你优化当前项目描述"
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
          <div class="section-head">
            <h4>本次检索上下文</h4>
            <span class="muted" v-if="projectKnowledgePreviewQueryText">检索词：{{ projectKnowledgePreviewQueryText }}</span>
          </div>
          <div v-if="projectKnowledgePreviewLoading" class="muted">正在加载检索上下文...</div>
          <div v-else-if="projectKnowledgePreview">
            <div class="preview-item">
              <strong>命中的片段</strong>
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
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
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
  editingProject: boolean
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
  projectTypeLabel: (value?: string) => string
  visibilityLabel: (value?: string) => string
  projectStatusLabel: (value?: string) => string
}>()

const emit = defineEmits<{
  (event: 'start-edit'): void
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
.card {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}

.section-head,
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.row {
  margin-top: 10px;
  flex-wrap: wrap;
}

.compact {
  margin-top: 0;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 10px;
  font-size: 14px;
}

.compact-grid {
  margin-bottom: 10px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 10px;
}

.materials-grid {
  display: grid;
  gap: 12px;
  margin-top: 10px;
}

.material-card,
.pending-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
  background: #fafcff;
}

.pending-list {
  margin-top: 12px;
}

.material-toolbar {
  justify-content: flex-start;
  align-items: center;
}

.knowledge-list {
  display: grid;
  gap: 6px;
  margin-top: 8px;
}

.knowledge-item {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.knowledge-preview {
  margin-top: 12px;
}

.preview-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
  background: #fafcff;
  margin-top: 8px;
}

.preview-item p {
  white-space: pre-wrap;
  margin: 6px 0 0;
}

.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
  margin-top: 8px;
  background: #fff;
}

.filter-select {
  width: auto;
  min-width: 120px;
  margin-top: 0;
}

textarea.input {
  min-height: 70px;
  resize: vertical;
}

.summary,
.muted {
  font-size: 13px;
  color: #4b5563;
  white-space: pre-wrap;
}

.detail-section {
  margin-top: 12px;
}

.detail-section h4 {
  margin: 0;
  font-size: 14px;
}

.collapsed-tip {
  margin-top: 8px;
}

.ai-edit-section {
  border-top: 1px dashed #dbe2ea;
  padding-top: 12px;
}

.ai-status {
  margin-top: 8px;
  font-size: 13px;
  color: #374151;
}

.ai-ready {
  margin-left: 8px;
  color: #166534;
}

.ai-assistant {
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #eef4ff;
  color: #1e3a8a;
  font-size: 13px;
}

.ai-history {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.ai-message {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
  background: #fafafa;
}

.ai-message.assistant {
  background: #f8fbff;
}

.ai-message.user {
  background: #fff;
}

.ai-message-role {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.ai-message-content {
  white-space: pre-wrap;
  font-size: 13px;
  color: #111827;
}

.knowledge-error {
  color: #b91c1c;
  font-size: 12px;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 999px;
  background: #e5e7eb;
  color: #374151;
  font-size: 12px;
}

.status-badge.task {
  background: #dbeafe;
  color: #1d4ed8;
}

.primary,
.ghost,
.danger,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.ghost,
.danger,
.mini {
  background: #f3f4f6;
}

.danger {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.mini {
  padding: 5px 9px;
  font-size: 12px;
}

@media (max-width: 980px) {
  .meta-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
