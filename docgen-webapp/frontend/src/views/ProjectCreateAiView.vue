<template>
  <div class="page">
    <div class="layout">
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
        @open-knowledge-detail="openKnowledgeDetail"
      />

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
    </div>

    <ProjectAiResultCard :ready-to-create="readyToCreate" :structured-info="structuredInfo" />

    <ProjectAiCreateCard
      :loading="loading"
      :session-id="sessionId"
      :ready-to-create="readyToCreate"
      :create-form="createForm"
      @create-project="createProject"
    />

    <KnowledgeDetailModal
      :visible="knowledgeDetailVisible"
      :loading="knowledgeDetailLoading"
      :detail="knowledgeDetail"
      :chunk-expanded="chunkExpanded"
      :visible-chunks="visibleChunks"
      @close="closeKnowledgeDetail"
      @toggle-chunks="chunkExpanded = !chunkExpanded"
    />

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
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

function closeKnowledgeDetail() {
  knowledgeDetailVisible.value = false
  knowledgeDetail.value = null
  chunkExpanded.value = false
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
    const res = await axios.post<ApiResponse<ConversationTurnResult>>('/api/projects/ai/conversations', {
      projectName: startForm.projectName || null,
      description: startForm.description || null,
      materials: pendingMaterials.value
    })
    sessionId.value = res.data.data.sessionId
    status.value = res.data.data.readyToCreate ? 'READY_TO_CREATE' : 'ACTIVE'
    readyToCreate.value = res.data.data.readyToCreate
    followUpQuestions.value = res.data.data.followUpQuestions || []
    applyStructuredInfo(res.data.data.structuredInfo)
    success.value = 'AI 项目会话已启动。'
    pendingMaterials.value = []
    await refreshConversation()
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
    await axios.post(`/api/projects/ai/conversations/${sessionId.value}/materials`, { materials: pendingMaterials.value })
    pendingMaterials.value = []
    success.value = '资料已保存到当前会话。'
    await refreshConversation()
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
    await axios.post<ApiResponse<UploadFileMaterialResponse>>(`/api/projects/ai/conversations/${sessionId.value}/materials/upload`, formData)
    resetFileDraft()
    success.value = '文件资料已上传，后台已开始处理。'
    await refreshConversation()
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
    status.value = res.data.data.readyToCreate ? 'READY_TO_CREATE' : 'ACTIVE'
    readyToCreate.value = res.data.data.readyToCreate
    followUpQuestions.value = res.data.data.followUpQuestions || []
    applyStructuredInfo(res.data.data.structuredInfo)
    chatMessage.value = ''
    success.value = readyToCreate.value ? '信息已基本齐备，可以创建项目。' : 'AI 已更新整理结果。'
    await refreshConversation()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '发送消息失败。'
  } finally {
    loading.value = false
  }
}

async function createProject() {
  if (!sessionId.value) return
  if (!createForm.projectKey.trim()) {
    error.value = '请先填写项目 Key。'
    return
  }
  if (createForm.ownerUserId && (!Number.isInteger(Number(createForm.ownerUserId)) || Number(createForm.ownerUserId) <= 0)) {
    error.value = '负责人用户 ID 必须是正整数。'
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await axios.post<ApiResponse<{ projectId: number }>>(`/api/projects/ai/conversations/${sessionId.value}/create-project`, {
      projectKey: createForm.projectKey,
      projectName: createForm.projectName || null,
      projectType: createForm.projectType || null,
      priority: createForm.priority || null,
      visibility: createForm.visibility || null,
      ownerUserId: createForm.ownerUserId ? Number(createForm.ownerUserId) : null
    })
    success.value = '项目创建成功，正在跳转到项目页。'
    await router.push(`/projects?projectId=${res.data.data.projectId}`)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建项目失败。'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { max-width: 1280px; margin: 18px auto; padding: 0 14px 18px; color: #111827; font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }
.layout { display: grid; grid-template-columns: 420px 1fr; gap: 14px; }
.error { color: #b91c1c; }
.success { color: #166534; }
@media (max-width: 960px) { .layout { grid-template-columns: 1fr; } }
</style>
