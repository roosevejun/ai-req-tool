<template>
  <div class="page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">AI 项目工作台</p>
        <h1>AI 创建项目</h1>
        <p class="hero-copy">以对话为主，把你的项目想法持续提炼成准确的项目信息。资料和知识输入只作为 AI 校准理解的辅助依据。</p>
      </div>
      <div class="hero-badges">
        <button class="hero-switch" type="button" @click="goFormMode">切换到传统创建</button>
        <StatusBadge :label="sessionId ? `会话 #${sessionId}` : '未启动会话'" :variant="sessionId ? 'success' : 'warning'" />
        <StatusBadge :label="readyToCreate ? '信息已基本准确' : '仍需继续校准'" :variant="readyToCreate ? 'success' : 'ai'" />
        <StatusBadge :label="`${confirmedItems.length} 项已提炼`" variant="info" />
      </div>
    </section>

    <div class="layout">
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

      <ProjectAiResultCard
        :ready-to-create="readyToCreate"
        :confirmed-items="confirmedItems"
        :pending-questions="followUpQuestions"
        :missing-items="missingItems"
        :accuracy-advice="accuracyAdvice"
      />
    </div>

    <div class="lower-layout">
      <ProjectAiSetupCard
        ref="setupCardRef"
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

      <section class="side-stack">
        <ProjectAiCreateCard
          :loading="loading"
          :session-id="sessionId"
          :ready-to-create="readyToCreate"
          :create-form="createForm"
          @create-project="createProject"
        />

        <FeedbackPanel title="下一步建议" :message="workspaceAdvice" tone="warning" />
        <FeedbackPanel title="处理提示" :message="error" tone="danger" />
        <FeedbackPanel title="最新进展" :message="success" tone="success" />
      </section>
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
import { computed, reactive, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import StatusBadge from '../components/projects/StatusBadge.vue'
import KnowledgeDetailModal from '../components/project-create-ai/KnowledgeDetailModal.vue'
import ProjectAiChatCard from '../components/project-create-ai/ProjectAiChatCard.vue'
import ProjectAiCreateCard from '../components/project-create-ai/ProjectAiCreateCard.vue'
import ProjectAiResultCard from '../components/project-create-ai/ProjectAiResultCard.vue'
import ProjectAiSetupCard from '../components/project-create-ai/ProjectAiSetupCard.vue'
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
const setupCardRef = ref<InstanceType<typeof ProjectAiSetupCard> | null>(null)

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

const canStartConversation = computed(() => !!(startForm.projectName.trim() || startForm.description.trim() || pendingMaterials.value.length > 0))
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
    return '先让 AI 听懂你的项目想法。建议输入项目名称和一段背景描述，再启动会话。'
  }
  if (followUpQuestions.value.length > 0) {
    return `当前还有 ${followUpQuestions.value.length} 个关键问题待确认。优先回答这些问题，能最快提升项目信息准确性。`
  }
  if (missingItems.value.length > 0) {
    return `AI 已经提炼出主体信息，但 ${missingItems.value.map((item) => item.label).join('、')} 仍不够明确。建议继续补充业务事实，而不是只给结论。`
  }
  if (!readyToCreate.value) {
    return '当前信息已经比较完整，建议再检查项目背景、目标客户和核心价值是否表达准确，确认后再创建项目。'
  }
  return '当前关键项目信息已基本形成闭环，可以检查项目 Key、负责人和可见性后正式创建项目。'
})
const workspaceAdvice = computed(() => {
  if (!sessionId.value) {
    if (!canStartConversation.value) return '先写下项目名称或补充一段背景描述，再启动 AI 会话。'
    if (pendingMaterials.value.length > 0) return '当前已有待保存资料，建议启动会话后先把资料沉淀进去，再开始和 AI 对话。'
    return '先启动 AI 会话，让项目想法进入可持续澄清和结构化整理流程。'
  }
  if (savedMaterials.value.length === 0) {
    return '当前会话已启动，建议继续补充网址、文本或文件资料，让 AI 有足够上下文参与项目梳理。'
  }
  if (!readyToCreate.value) {
    return '当前资料和会话已经建立，建议继续追问、补充信息并让 AI 把结果沉淀成结构化项目内容。'
  }
  return '当前已经具备创建条件，建议检查结构化结果和创建表单后，正式创建项目。'
})

function resetFileDraft() {
  selectedFile.value = null
  fileDraft.title = ''
  setupCardRef.value?.resetFileInput()
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

async function retryKnowledgeDocument(documentId: number) {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/knowledge-documents/${documentId}/reprocess`)
    await refreshKnowledgeStatuses()
    if (knowledgeDetail.value?.document.id === documentId) {
      await openKnowledgeDetail(documentId)
    }
    success.value = '知识文档已重新处理。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '重新处理知识文档失败。'
  } finally {
    loading.value = false
  }
}

async function reprocessKnowledgeDetail() {
  if (!knowledgeDetail.value) {
    return
  }
  knowledgeReprocessing.value = true
  error.value = ''
  success.value = ''
  try {
    const documentId = knowledgeDetail.value.document.id
    const res = await axios.post<ApiResponse<KnowledgeDocumentDetail>>(`/api/knowledge-documents/${documentId}/reprocess`)
    knowledgeDetail.value = res.data.data
    await refreshKnowledgeStatuses()
    success.value = '知识文档已重新处理。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '重新处理知识文档失败。'
  } finally {
    knowledgeReprocessing.value = false
  }
}

function closeKnowledgeDetail() {
  knowledgeDetailVisible.value = false
  knowledgeDetail.value = null
  chunkExpanded.value = false
  knowledgeReprocessing.value = false
}

async function loadKnowledgePreview() {
  if (!sessionId.value) return
  knowledgePreviewLoading.value = true
  knowledgePreviewVisible.value = true
  try {
    const query = chatMessage.value.trim()
    const res = await axios.get<ApiResponse<KnowledgePreview>>(`/api/projects/ai/conversations/${sessionId.value}/knowledge-preview`, {
      params: query ? { query } : undefined
    })
    knowledgePreview.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载检索上下文失败。'
  } finally {
    knowledgePreviewLoading.value = false
  }
}

function addUrlMaterial() {
  if (!urlDraft.sourceUri.trim()) {
    error.value = '请先填写网站链接。'
    return
  }
  pendingMaterials.value.push({
    materialType: 'URL',
    title: urlDraft.title.trim() || undefined,
    sourceUri: urlDraft.sourceUri.trim()
  })
  urlDraft.title = ''
  urlDraft.sourceUri = ''
  error.value = ''
}

function addTextMaterial() {
  if (!textDraft.rawContent.trim()) {
    error.value = '请先填写文本资料内容。'
    return
  }
  pendingMaterials.value.push({
    materialType: 'TEXT',
    title: textDraft.title.trim() || undefined,
    rawContent: textDraft.rawContent.trim()
  })
  textDraft.title = ''
  textDraft.rawContent = ''
  error.value = ''
}

function clearPendingMaterials() {
  pendingMaterials.value = []
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

async function startConversation() {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await axios.post<ApiResponse<ConversationView>>('/api/projects/ai/conversations', {
      projectName: startForm.projectName,
      description: startForm.description,
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
    success.value = 'AI 会话已启动。'
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
    const res = await axios.post<ApiResponse<UploadFileMaterialResponse>>(`/api/projects/ai/conversations/${sessionId.value}/materials/upload`, formData)
    const uploadedMaterial = res.data.data?.material
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
  if (!sessionId.value || !chatMessage.value.trim()) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await axios.post<ApiResponse<ConversationTurnResult>>(`/api/projects/ai/conversations/${sessionId.value}/chat`, {
      message: chatMessage.value.trim()
    })
    const data = res.data.data
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
</script>

<style scoped>
.page {
  max-width: 1440px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}
.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
  padding: 18px;
  border: 1px solid #dbe2ea;
  border-radius: 20px;
  background: linear-gradient(135deg, #f8fcff 0%, #ffffff 55%);
}
.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: .08em;
  text-transform: uppercase;
  font-weight: 700;
}
h1 {
  margin: 0;
  font-size: 32px;
  color: #0f172a;
}
.hero-copy {
  margin: 10px 0 0;
  max-width: 720px;
  color: #64748b;
  line-height: 1.7;
}
.hero-badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}
.hero-switch {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #f8fafc;
  color: #0f172a;
  padding: 8px 12px;
  cursor: pointer;
}
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(360px, 0.95fr);
  gap: 14px;
}
.lower-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 360px;
  gap: 14px;
  margin-top: 14px;
}
.side-stack {
  display: grid;
  gap: 14px;
  align-content: start;
}

@media (max-width: 1024px) {
  .page-hero,
  .hero-badges {
    flex-direction: column;
    align-items: flex-start;
  }
  .layout,
  .lower-layout {
    grid-template-columns: 1fr;
  }
}
</style>

