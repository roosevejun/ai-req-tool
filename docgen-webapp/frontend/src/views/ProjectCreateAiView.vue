<template>
  <div class="page">
    <ProjectAiHeroPanel
      :confirmed-count="confirmedItems.length"
      :pending-count="followUpQuestions.length"
      :missing-count="missingItems.length"
      :ready-to-create="readyToCreate"
      :accuracy-advice="accuracyAdvice"
      @go-form-mode="goFormMode"
    />

    <div class="workspace-tabs">
      <div class="main-tabs" role="tablist" aria-label="AI 项目孵化工作区">
        <button
          class="main-tab"
          :class="{ active: mainTab === 'conversation' }"
          type="button"
          role="tab"
          :aria-selected="mainTab === 'conversation'"
          @click="mainTab = 'conversation'"
        >
          AI 沟通
        </button>
        <button
          class="main-tab"
          :class="{ active: mainTab === 'decision' }"
          type="button"
          role="tab"
          :aria-selected="mainTab === 'decision'"
          @click="mainTab = 'decision'"
        >
          结果决策
        </button>
      </div>
    </div>

    <div class="workspace-layout">
      <section class="main-stack">
        <div class="main-tabs" role="tablist" aria-label="AI 项目孵化工作区">
          <button
            class="main-tab"
            :class="{ active: mainTab === 'conversation' }"
            type="button"
            role="tab"
            :aria-selected="mainTab === 'conversation'"
            @click="mainTab = 'conversation'"
          >
            AI 沟通
          </button>
          <button
            class="main-tab"
            :class="{ active: mainTab === 'decision' }"
            type="button"
            role="tab"
            :aria-selected="mainTab === 'decision'"
            @click="mainTab = 'decision'"
          >
            结果决策
          </button>
        </div>

        <template v-if="mainTab === 'conversation'">
          <ProjectAiChatCard
            v-model:chat-message="chatMessage"
            :loading="loading"
            :session-id="sessionId"
            :status="status"
            :messages="messages"
            :follow-up-questions="followUpQuestions"
            :knowledge-preview-visible="knowledgePreviewVisible"
            :knowledge-preview-loading="knowledgePreviewLoading"
            :knowledge-preview="knowledgePreview"
            :knowledge-preview-query-text="knowledgePreviewQueryText"
            @send-message="sendMessage"
            @refresh-conversation="refreshConversation"
            @load-knowledge-preview="loadKnowledgePreview"
          />
        </template>

        <template v-else>
          <ProjectAiDecisionPanel
            :loading="loading"
            :session-id="sessionId"
            :ready-to-create="readyToCreate"
            :confirmed-items="confirmedItems"
            :pending-questions="followUpQuestions"
            :missing-items="missingItems"
            :accuracy-advice="accuracyAdvice"
            :create-form="createForm"
            @save-framework="saveFramework"
            @create-project="createProject"
          />
        </template>
      </section>

      <ProjectAiSidebar
        ref="sidebarRef"
        :loading="loading"
        :session-id="sessionId"
        :start-form="startForm"
        :url-draft="urlDraft"
        :text-draft="textDraft"
        :file-draft="fileDraft"
        :selected-file="selectedFile"
        :can-start-conversation="canStartConversation"
        :pending-materials="pendingMaterials"
        :saved-materials="savedMaterials"
        :material-knowledge-map="materialKnowledgeMap"
        :workspace-advice="workspaceAdvice"
        :error="error"
        :success="success"
        @add-url-material="addUrlMaterial"
        @add-text-material="addTextMaterial"
        @clear-pending-materials="clearPendingMaterials"
        @start-conversation="startConversation"
        @save-materials="saveMaterials"
        @upload-file="uploadFileMaterial"
        @file-selected="handleFileSelect"
        @open-knowledge-detail="goKnowledgeLibrary"
        @retry-knowledge-document="retryKnowledgeDocument"
      />
    </div>

    <KnowledgeDetailModal
      :visible="knowledgeDetailVisible"
      :loading="knowledgeDetailLoading"
      :reprocessing="knowledgeReprocessing"
      :detail="knowledgeDetail"
      :chunk-expanded="chunkExpanded"
      :visible-chunks="visibleChunks"
      @close="closeKnowledgeDetail"
      @reprocess="reprocessKnowledgeDetail"
      @toggle-chunks="chunkExpanded = !chunkExpanded"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import KnowledgeDetailModal from '../components/project-create-ai/KnowledgeDetailModal.vue'
import ProjectAiChatCard from '../components/project-create-ai/ProjectAiChatCard.vue'
import ProjectAiDecisionPanel from '../components/project-create-ai/ProjectAiDecisionPanel.vue'
import ProjectAiHeroPanel from '../components/project-create-ai/ProjectAiHeroPanel.vue'
import ProjectAiSidebar from '../components/project-create-ai/ProjectAiSidebar.vue'
import { latestQuestions, visibleChunks as toVisibleChunks } from '../components/project-create-ai/helpers'
import type {
  ApiResponse,
  ChatMessageItem,
  ConversationTurnResult,
  ConversationView,
  CreateFormState,
  FileDraftState,
  KnowledgeDocumentDetail,
  KnowledgeDocumentListItem,
  KnowledgePreview,
  SourceMaterial,
  StartFormState,
  StructuredFieldItem,
  StructuredInfo,
  TextDraftState,
  UploadFileMaterialResponse,
  UrlDraftState
} from '../components/project-create-ai/types'

const router = useRouter()
const route = useRoute()
const sidebarRef = ref<InstanceType<typeof ProjectAiSidebar> | null>(null)

const loading = ref(false)
const error = ref('')
const success = ref('')
const sessionId = ref<number | null>(null)
const status = ref('DRAFT')
const messages = ref<ChatMessageItem[]>([])
const followUpQuestions = ref<string[]>([])
const readyToCreate = ref(false)
const savedMaterials = ref<SourceMaterial[]>([])
const materialKnowledgeMap = ref<Record<number, KnowledgeDocumentListItem[]>>({})
const pendingMaterials = ref<SourceMaterial[]>([])
const selectedFile = ref<File | null>(null)
const chatMessage = ref('')
const knowledgeDetailVisible = ref(false)
const knowledgeDetailLoading = ref(false)
const knowledgeReprocessing = ref(false)
const knowledgeDetail = ref<KnowledgeDocumentDetail | null>(null)
const chunkExpanded = ref(false)
const knowledgePreviewLoading = ref(false)
const knowledgePreviewVisible = ref(false)
const knowledgePreview = ref<KnowledgePreview | null>(null)
const mainTab = ref<'conversation' | 'decision'>('conversation')

const structuredInfo = reactive<StructuredInfo>({
  projectName: '',
  description: '',
  projectBackground: '',
  similarProducts: '',
  targetCustomerGroups: '',
  commercialValue: '',
  coreProductValue: '',
  businessKnowledgeSummary: ''
})
const startForm = reactive<StartFormState>({ projectName: '', description: '' })
const createForm = reactive<CreateFormState>({
  projectKey: '',
  projectName: '',
  projectType: '',
  priority: '',
  visibility: 'PRIVATE',
  ownerUserId: ''
})
const urlDraft = reactive<UrlDraftState>({ title: '', sourceUri: '' })
const textDraft = reactive<TextDraftState>({ title: '', rawContent: '' })
const fileDraft = reactive<FileDraftState>({ title: '' })

const canStartConversation = computed(
  () => !!(startForm.projectName.trim() || startForm.description.trim() || pendingMaterials.value.length > 0)
)
const knowledgePreviewQueryText = computed(
  () => knowledgePreview.value?.query || chatMessage.value.trim() || structuredInfo.projectName || startForm.projectName.trim()
)
const visibleChunks = computed(() => toVisibleChunks(knowledgeDetail.value, chunkExpanded.value))
const confirmedItems = computed<StructuredFieldItem[]>(() => {
  const candidates: StructuredFieldItem[] = [
    { key: 'projectName', label: '项目名称', value: structuredInfo.projectName || startForm.projectName.trim() },
    { key: 'description', label: '项目描述', value: structuredInfo.description || startForm.description.trim() },
    { key: 'projectBackground', label: '项目背景', value: structuredInfo.projectBackground || '' },
    { key: 'similarProducts', label: '类似产品', value: structuredInfo.similarProducts || '' },
    { key: 'targetCustomerGroups', label: '目标客户群体', value: structuredInfo.targetCustomerGroups || '' },
    { key: 'commercialValue', label: '商业价值', value: structuredInfo.commercialValue || '' },
    { key: 'coreProductValue', label: '核心产品价值', value: structuredInfo.coreProductValue || '' },
    { key: 'businessKnowledgeSummary', label: '业务知识摘要', value: structuredInfo.businessKnowledgeSummary || '' }
  ]
  return candidates.filter((item) => !!item.value?.trim())
})
const missingItems = computed<StructuredFieldItem[]>(() => {
  const candidates: StructuredFieldItem[] = [
    { key: 'projectBackground', label: '项目背景', value: structuredInfo.projectBackground || '' },
    { key: 'targetCustomerGroups', label: '目标客户群体', value: structuredInfo.targetCustomerGroups || '' },
    { key: 'commercialValue', label: '商业价值', value: structuredInfo.commercialValue || '' },
    { key: 'coreProductValue', label: '核心产品价值', value: structuredInfo.coreProductValue || '' }
  ]
  return candidates.filter((item) => !item.value.trim())
})
const accuracyAdvice = computed(() => {
  if (!sessionId.value) {
    return '先让 AI 听懂你的项目想法。建议输入项目名称和一段背景描述，再让第一句话自动启动会话。'
  }
  if (followUpQuestions.value.length > 0) {
    return `当前还有 ${followUpQuestions.value.length} 个关键问题待确认。优先围绕这些问题补充事实，能最快提升框架成熟度。`
  }
  if (missingItems.value.length > 0) {
    return `AI 已经提炼出主体框架，但 ${missingItems.value.map((item) => item.label).join('、')} 仍不够清晰。建议继续补充业务事实，而不是只给结论。`
  }
  if (!readyToCreate.value) {
    return '当前框架已经比较完整，建议先判断这份框架是否值得保留，再决定继续孵化还是正式立项。'
  }
  return '当前关键项目信息已经基本闭环。你可以先保留这份项目框架，也可以在确认无误后正式立项。'
})
const workspaceAdvice = computed(() => {
  if (!sessionId.value) {
    if (!canStartConversation.value) return '先写下项目名称或补充一段背景描述，再让第一句话启动 AI 项目孵化。'
    if (pendingMaterials.value.length > 0) return '当前已有待保存资料。建议先说出项目想法，再让资料参与 AI 校准。'
    return '直接开始和 AI 沟通，让项目想法进入可持续澄清和孵化流程。'
  }
  if (savedMaterials.value.length === 0) {
    return '会话已启动。建议继续补充网站、文本或文件资料，让 AI 有更完整的上下文参与框架整理。'
  }
  if (!readyToCreate.value) {
    return '当前资料和会话已经建立，建议继续回答追问、补充信息，并让 AI 把结果沉淀成可保留的项目框架。'
  }
  return '当前已经形成一份较成熟的项目框架。你可以先保留它，也可以在确认准确后正式立项。'
})

function resetFileDraft() {
  selectedFile.value = null
  fileDraft.title = ''
  sidebarRef.value?.resetFileInput()
}

function handleFileSelect(file: File | null) {
  selectedFile.value = file
}

function applyStructuredInfo(next: StructuredInfo) {
  structuredInfo.projectName = next.projectName || ''
  structuredInfo.description = next.description || ''
  structuredInfo.projectBackground = next.projectBackground || ''
  structuredInfo.similarProducts = next.similarProducts || ''
  structuredInfo.targetCustomerGroups = next.targetCustomerGroups || ''
  structuredInfo.commercialValue = next.commercialValue || ''
  structuredInfo.coreProductValue = next.coreProductValue || ''
  structuredInfo.businessKnowledgeSummary = next.businessKnowledgeSummary || ''
  if (!createForm.projectName && structuredInfo.projectName) {
    createForm.projectName = structuredInfo.projectName
  }
}

async function refreshKnowledgeStatuses() {
  const targets = savedMaterials.value.filter((item) => typeof item.id === 'number')
  if (targets.length === 0) {
    materialKnowledgeMap.value = {}
    return
  }
  const entries = await Promise.all(
    targets.map(async (item) => {
      const res = await axios.get<ApiResponse<KnowledgeDocumentListItem[]>>(`/api/knowledge-documents/source-materials/${item.id}`)
      return [item.id as number, res.data.data || []] as const
    })
  )
  materialKnowledgeMap.value = Object.fromEntries(entries)
}

async function openKnowledgeDetail(documentId: number) {
  knowledgeDetailVisible.value = true
  knowledgeDetailLoading.value = true
  knowledgeDetail.value = null
  try {
    const res = await axios.get<ApiResponse<KnowledgeDocumentDetail>>(`/api/knowledge-documents/${documentId}`)
    knowledgeDetail.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载知识文档详情失败。'
  } finally {
    knowledgeDetailLoading.value = false
  }
}

function goKnowledgeLibrary(documentId: number) {
  void router.push({
    path: '/knowledge',
    query: {
      documentId: String(documentId)
    }
  })
}

function goFormMode() {
  void router.push('/projects/create/form')
}

async function restoreConversation(targetSessionId: number) {
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<ConversationView>>(`/api/projects/ai/conversations/${targetSessionId}`)
    const data = res.data.data
    sessionId.value = data.sessionId
    status.value = data.status || 'ACTIVE'
    readyToCreate.value = data.readyToCreate
    messages.value = data.messages || []
    savedMaterials.value = data.materials || []
    followUpQuestions.value = latestQuestions(data.messages || [])
    applyStructuredInfo(data.structuredInfo)
    await refreshKnowledgeStatuses()
    success.value = '已恢复当前项目框架，你可以继续孵化或决定是否立项。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '恢复项目框架失败。'
  } finally {
    loading.value = false
  }
}

async function refreshConversation() {
  if (!sessionId.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<ConversationView>>(`/api/projects/ai/conversations/${sessionId.value}`)
    const data = res.data.data
    status.value = data.status || 'ACTIVE'
    readyToCreate.value = data.readyToCreate
    messages.value = data.messages || []
    savedMaterials.value = data.materials || []
    followUpQuestions.value = latestQuestions(data.messages || [])
    applyStructuredInfo(data.structuredInfo)
    await refreshKnowledgeStatuses()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '刷新会话失败。'
  } finally {
    loading.value = false
  }
}

function addUrlMaterial() {
  if (!urlDraft.sourceUri.trim()) return
  pendingMaterials.value = [
    ...pendingMaterials.value,
    {
      materialType: 'URL',
      title: urlDraft.title.trim() || '',
      sourceUri: urlDraft.sourceUri.trim()
    }
  ]
  urlDraft.title = ''
  urlDraft.sourceUri = ''
}

function addTextMaterial() {
  if (!textDraft.rawContent.trim()) return
  pendingMaterials.value = [
    ...pendingMaterials.value,
    {
      materialType: 'TEXT',
      title: textDraft.title.trim() || '',
      rawContent: textDraft.rawContent.trim()
    }
  ]
  textDraft.title = ''
  textDraft.rawContent = ''
}

function clearPendingMaterials() {
  pendingMaterials.value = []
}

async function retryKnowledgeDocument(documentId: number) {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/knowledge-documents/${documentId}/reprocess`)
    await refreshKnowledgeStatuses()
    success.value = '已重新提交知识文档处理任务。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '重新处理知识文档失败。'
  } finally {
    loading.value = false
  }
}

function closeKnowledgeDetail() {
  knowledgeDetailVisible.value = false
  knowledgeDetail.value = null
  chunkExpanded.value = false
}

async function reprocessKnowledgeDetail() {
  const documentId = knowledgeDetail.value?.document?.id
  if (!documentId) return
  knowledgeReprocessing.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/knowledge-documents/${documentId}/reprocess`)
    await openKnowledgeDetail(documentId)
    await refreshKnowledgeStatuses()
    success.value = '已重新提交知识文档处理任务。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '重新处理知识文档失败。'
  } finally {
    knowledgeReprocessing.value = false
  }
}

async function loadKnowledgePreview() {
  if (!sessionId.value) return
  knowledgePreviewVisible.value = true
  knowledgePreviewLoading.value = true
  error.value = ''
  try {
    const res = await axios.post<ApiResponse<KnowledgePreview>>(`/api/projects/ai/conversations/${sessionId.value}/knowledge-preview`, {
      query: knowledgePreviewQueryText.value
    })
    knowledgePreview.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载检索上下文失败。'
  } finally {
    knowledgePreviewLoading.value = false
  }
}

async function startConversation() {
  if (!canStartConversation.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await axios.post<ApiResponse<ConversationView>>('/api/projects/ai/conversations', {
      projectName: startForm.projectName || null,
      description: startForm.description || null,
      initialMessage: null,
      materials: pendingMaterials.value
    })
    const data = res.data.data
    sessionId.value = data.sessionId
    status.value = data.status || 'ACTIVE'
    readyToCreate.value = data.readyToCreate
    messages.value = data.messages || []
    savedMaterials.value = data.materials || []
    followUpQuestions.value = latestQuestions(data.messages || [])
    pendingMaterials.value = []
    applyStructuredInfo(data.structuredInfo)
    await refreshKnowledgeStatuses()
    await router.replace({ path: '/projects/create-ai', query: { sessionId: String(data.sessionId) } })
    success.value = 'AI 会话已启动，你可以继续补充想法、资料和关键事实。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '启动 AI 会话失败。'
  } finally {
    loading.value = false
  }
}

async function saveMaterials() {
  if (!sessionId.value || pendingMaterials.value.length === 0) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/projects/ai/conversations/${sessionId.value}/materials`, {
      materials: pendingMaterials.value
    })
    pendingMaterials.value = []
    await refreshConversation()
    success.value = '资料已保存到当前会话。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '保存资料失败。'
  } finally {
    loading.value = false
  }
}

async function saveFramework() {
  if (!sessionId.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await refreshConversation()
    await router.replace({ path: '/projects/create-ai', query: { sessionId: String(sessionId.value) } })
    success.value = `当前项目框架已保留。后续可以通过此页面继续孵化，当前会话编号 #${sessionId.value}。`
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '保留当前项目框架失败。'
  } finally {
    loading.value = false
  }
}

async function uploadFileMaterial() {
  if (!sessionId.value || !selectedFile.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    if (fileDraft.title.trim()) {
      formData.append('title', fileDraft.title.trim())
    }
    const res = await axios.post<ApiResponse<UploadFileMaterialResponse>>(
      `/api/projects/ai/conversations/${sessionId.value}/materials/upload`,
      formData
    )
    const uploadedMaterial = (res.data.data as any)?.material
    if (uploadedMaterial) {
      savedMaterials.value = [...savedMaterials.value, uploadedMaterial]
    }
    resetFileDraft()
    await refreshConversation()
    success.value = '文件资料已上传。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '上传文件资料失败。'
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  const message = chatMessage.value.trim()
  if (!message) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    if (!sessionId.value) {
      const res = await axios.post<ApiResponse<ConversationView>>('/api/projects/ai/conversations', {
        projectName: startForm.projectName || null,
        description: startForm.description || null,
        initialMessage: message,
        materials: pendingMaterials.value
      })
      const data = res.data.data
      sessionId.value = data.sessionId
      status.value = data.status || 'ACTIVE'
      readyToCreate.value = data.readyToCreate
      messages.value = data.messages || []
      savedMaterials.value = data.materials || []
      followUpQuestions.value = latestQuestions(data.messages || [])
      pendingMaterials.value = []
      applyStructuredInfo(data.structuredInfo)
      chatMessage.value = ''
      await refreshKnowledgeStatuses()
      await router.replace({ path: '/projects/create-ai', query: { sessionId: String(data.sessionId) } })
      success.value = 'AI 已根据你的第一句话启动项目孵化。'
      return
    }

    const res = await axios.post<ApiResponse<ConversationTurnResult>>(`/api/projects/ai/conversations/${sessionId.value}/chat`, {
      message
    })
    const data: any = res.data.data
    status.value = data.status || status.value
    readyToCreate.value = data.readyToCreate
    messages.value = data.messages || []
    followUpQuestions.value = latestQuestions(data.messages || [])
    applyStructuredInfo(data.structuredInfo)
    chatMessage.value = ''
    success.value = 'AI 已完成这一轮更新。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '发送消息失败。'
  } finally {
    loading.value = false
  }
}

async function createProject() {
  if (!sessionId.value || !readyToCreate.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await axios.post<ApiResponse<number>>(`/api/projects/ai/conversations/${sessionId.value}/create-project`, {
      projectKey: createForm.projectKey || null,
      projectName: createForm.projectName || null,
      projectType: createForm.projectType || null,
      priority: createForm.priority || null,
      visibility: createForm.visibility || null,
      ownerUserId: createForm.ownerUserId ? Number(createForm.ownerUserId) : null
    })
    const projectId = res.data.data
    success.value = '项目已创建，正在跳转到项目工作台。'
    await router.push(`/projects?projectId=${projectId}`)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建项目失败。'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const querySessionId = Number(route.query.sessionId || 0)
  if (querySessionId > 0) {
    await restoreConversation(querySessionId)
  }
})
</script>

<style scoped>
.page {
  max-width: 1440px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.workspace-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(340px, 0.95fr);
  gap: 14px;
  margin-top: 12px;
}

.workspace-tabs {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-top: 14px;
}

.main-stack {
  display: grid;
  gap: 0;
  align-content: start;
}

.main-stack > .main-tabs {
  display: none;
}

.main-tabs {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border: 1px solid #dbe2ea;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  width: fit-content;
}

.main-tab {
  border: 0;
  border-radius: 999px;
  padding: 10px 16px;
  background: transparent;
  color: #475569;
  font-weight: 600;
  cursor: pointer;
}

.main-tab.active {
  background: #eff6ff;
  color: #1d4ed8;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.16);
}

@media (max-width: 1024px) {
  .workspace-layout {
    grid-template-columns: 1fr;
  }
}
</style>
