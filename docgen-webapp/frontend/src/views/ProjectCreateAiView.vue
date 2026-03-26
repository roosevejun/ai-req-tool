<template>
  <div class="page">
    <div class="layout">
      <section class="card">
        <h3>创建 AI 项目会话</h3>
        <div class="grid two">
          <input
            v-model.trim="startForm.projectName"
            class="input"
            placeholder="输入项目名称，例如 AI 巡检平台"
          />
          <textarea
            v-model="startForm.description"
            class="input"
            placeholder="描述项目背景、目标用户、核心流程和你目前已知的信息"
          />
        </div>

        <h3>补充资料</h3>
        <div class="materials">
          <div class="material-card">
            <div class="section-head">
              <strong>网站链接</strong>
              <button class="mini" type="button" @click="addUrlMaterial">添加</button>
            </div>
            <input v-model.trim="urlDraft.title" class="input" placeholder="链接标题，可选" />
            <input v-model.trim="urlDraft.sourceUri" class="input" placeholder="https://example.com" />
          </div>

          <div class="material-card">
            <div class="section-head">
              <strong>文本资料</strong>
              <button class="mini" type="button" @click="addTextMaterial">添加</button>
            </div>
            <input v-model.trim="textDraft.title" class="input" placeholder="资料标题，可选" />
            <textarea
              v-model="textDraft.rawContent"
              class="input"
              placeholder="粘贴文档内容、会议记录、业务说明或其他文字资料"
            />
          </div>

          <div class="material-card">
            <div class="section-head">
              <strong>文件资料</strong>
            </div>
            <input
              ref="fileInputRef"
              class="input"
              type="file"
              :disabled="loading || !sessionId"
              @change="handleFileSelect"
            />
            <input
              v-model.trim="fileDraft.title"
              class="input"
              :disabled="loading || !sessionId"
              placeholder="文件标题，可选"
            />
            <div class="row">
              <button
                class="ghost"
                type="button"
                :disabled="loading || !sessionId || !selectedFile"
                @click="uploadFileMaterial"
              >
                上传文件资料
              </button>
              <span class="muted">
                {{ selectedFile ? selectedFile.name : '请先启动会话后再上传文件' }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="pendingMaterials.length > 0" class="pending-list">
          <div class="section-head">
            <strong>待保存资料</strong>
            <button class="ghost mini" type="button" @click="clearPendingMaterials">清空</button>
          </div>
          <div v-for="(item, idx) in pendingMaterials" :key="idx" class="pending-item">
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
              <div
                v-for="doc in materialKnowledgeItems(item)"
                :key="doc.id"
                class="knowledge-status-block"
              >
                <div class="knowledge-status-item">
                  <span class="status-badge" :class="doc.status?.toLowerCase()">
                    {{ knowledgeStatusText(doc.status) }}
                  </span>
                  <span class="muted">
                    {{ doc.documentType }} / {{ doc.title || '知识文档' }}
                  </span>
                  <button class="ghost mini" type="button" @click="openKnowledgeDetail(doc.id)">
                    查看详情
                  </button>
                </div>
                <div v-if="doc.latestTaskError" class="knowledge-error">
                  错误：{{ doc.latestTaskError }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="row">
          <button class="primary" type="button" :disabled="loading || !canStartConversation" @click="startConversation">
            启动 AI 会话
          </button>
          <button
            class="ghost"
            type="button"
            :disabled="loading || !sessionId || pendingMaterials.length === 0"
            @click="saveMaterials"
          >
            保存待上传资料
          </button>
        </div>
      </section>

      <section class="card">
        <div class="section-head">
          <h3>AI 对话</h3>
          <span class="muted" v-if="sessionId">Session #{{ sessionId }} / {{ statusText }}</span>
        </div>

        <div v-if="messages.length === 0" class="empty-state">
          输入项目基本信息并启动 AI 会话后，这里会展示对话内容与追问。
        </div>

        <div v-else class="chat-list">
          <div
            v-for="message in messages"
            :key="message.id + '-' + message.seqNo"
            class="chat-item"
            :class="message.role"
          >
            <div class="chat-role">{{ roleLabel(message.role, message.messageType) }}</div>
            <div class="chat-content">{{ message.content }}</div>
          </div>
        </div>

        <div v-if="followUpQuestions.length > 0" class="questions">
          <h4>AI 当前追问的问题</h4>
          <div v-for="(question, idx) in followUpQuestions" :key="question + idx" class="question-item">
            {{ idx + 1 }}. {{ question }}
          </div>
        </div>

        <textarea
          v-model="chatMessage"
          class="input"
          :disabled="!sessionId || loading"
          placeholder="补充业务背景、回答 AI 追问，或修正 AI 当前理解"
        />

        <div class="row">
          <button class="primary" type="button" :disabled="loading || !sessionId || !chatMessage.trim()" @click="sendMessage">
            发送消息
          </button>
          <button class="ghost" type="button" :disabled="loading || !sessionId" @click="refreshConversation">
            刷新会话
          </button>
        </div>
      </section>
    </div>

    <section class="card result-card">
      <div class="section-head">
        <h3>结构化整理结果</h3>
        <span class="badge" :class="{ ready: readyToCreate }">
          {{ readyToCreate ? '可以创建项目' : '仍需补充信息' }}
        </span>
      </div>

      <div class="preview-grid">
        <div class="preview-item">
          <strong>项目背景</strong>
          <p>{{ structuredInfo.projectBackground || '-' }}</p>
        </div>
        <div class="preview-item">
          <strong>类似产品</strong>
          <p>{{ structuredInfo.similarProducts || '-' }}</p>
        </div>
        <div class="preview-item">
          <strong>目标客户群体</strong>
          <p>{{ structuredInfo.targetCustomerGroups || '-' }}</p>
        </div>
        <div class="preview-item">
          <strong>商业价值</strong>
          <p>{{ structuredInfo.commercialValue || '-' }}</p>
        </div>
        <div class="preview-item">
          <strong>核心产品价值</strong>
          <p>{{ structuredInfo.coreProductValue || '-' }}</p>
        </div>
        <div class="preview-item wide">
          <strong>业务知识摘要</strong>
          <p>{{ structuredInfo.businessKnowledgeSummary || '-' }}</p>
        </div>
      </div>
    </section>

    <section class="card">
      <h3>创建最终项目</h3>
      <div class="grid create-grid">
        <input v-model.trim="createForm.projectKey" class="input" placeholder="项目 Key，例如 AI-INSPECT" />
        <input v-model.trim="createForm.projectName" class="input" placeholder="项目名称，默认沿用 AI 整理结果" />
        <select v-model="createForm.projectType" class="input">
          <option value="">项目类型</option>
          <option value="PRODUCT">产品型</option>
          <option value="PLATFORM">平台型</option>
          <option value="OPS">运维型</option>
          <option value="INTEGRATION">集成型</option>
        </select>
        <select v-model="createForm.priority" class="input">
          <option value="">优先级</option>
          <option value="P0">P0</option>
          <option value="P1">P1</option>
          <option value="P2">P2</option>
          <option value="P3">P3</option>
        </select>
        <select v-model="createForm.visibility" class="input">
          <option value="PRIVATE">私有</option>
          <option value="ORG">组织内</option>
        </select>
        <input v-model.trim="createForm.ownerUserId" class="input" placeholder="负责人 ID，可选" />
      </div>

      <div class="row">
        <button class="primary" type="button" :disabled="loading || !sessionId || !readyToCreate" @click="createProject">
          根据 AI 会话创建项目
        </button>
      </div>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>

    <div v-if="knowledgeDetailVisible" class="modal-mask" @click.self="closeKnowledgeDetail">
      <div class="modal-card">
        <div class="section-head">
          <h3>知识文档详情</h3>
          <button class="ghost mini" type="button" @click="closeKnowledgeDetail">关闭</button>
        </div>

        <div v-if="knowledgeDetailLoading" class="empty-state">正在加载知识文档详情...</div>
        <div v-else-if="knowledgeDetail" class="detail-grid">
          <div class="detail-block">
            <strong>文档信息</strong>
            <p>ID: {{ knowledgeDetail.document.id }}</p>
            <p>类型: {{ knowledgeDetail.document.documentType }}</p>
            <p>状态: {{ knowledgeStatusText(knowledgeDetail.document.status) }}</p>
            <p>标题: {{ knowledgeDetail.document.title || '-' }}</p>
            <p>摘要: {{ knowledgeDetail.document.summary || '-' }}</p>
          </div>

          <div class="detail-block">
            <strong>资源列表</strong>
            <p v-if="knowledgeDetail.assets.length === 0">-</p>
            <p v-for="asset in knowledgeDetail.assets" :key="asset.id">
              {{ asset.assetRole }} / {{ asset.storageKey }}
            </p>
          </div>

          <div class="detail-block wide">
            <strong>任务历史</strong>
            <p v-if="knowledgeDetail.tasks.length === 0">-</p>
            <p v-for="task in knowledgeDetail.tasks" :key="task.id">
              {{ task.taskType }} / {{ knowledgeStatusText(task.status) }} / {{ task.lastError || '-' }}
            </p>
          </div>

          <div class="detail-block wide">
            <div class="section-head">
              <strong>Chunk 预览</strong>
              <button
                v-if="knowledgeDetail.chunks.length > 0"
                class="ghost mini"
                type="button"
                @click="chunkExpanded = !chunkExpanded"
              >
                {{ chunkExpanded ? '收起' : '展开' }}
              </button>
            </div>
            <p v-if="knowledgeDetail.chunks.length === 0">-</p>
            <p v-for="chunk in visibleChunks" :key="chunk.id">
              #{{ chunk.chunkNo }} {{ chunk.chunkText }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

type ApiResponse<T> = { code: number; message: string; data: T; traceId: string }

type SourceMaterial = {
  id?: number
  materialType: string
  title?: string
  sourceUri?: string
  rawContent?: string
  aiExtractedSummary?: string
}

type KnowledgeDocumentListItem = {
  id: number
  projectId?: number
  requirementId?: number
  sourceMaterialId?: number
  documentType: string
  sourceUri?: string
  title?: string
  status: string
  summary?: string
  tags?: string
  contentHash?: string
  versionNo?: number
  isLatest?: boolean
  retrievable?: boolean
  latestTaskStatus?: string
  latestTaskError?: string
}

type KnowledgeDocumentAsset = {
  id: number
  documentId: number
  assetRole: string
  storageBucket: string
  storageKey: string
  mimeType?: string
  sizeBytes?: number
  sha256?: string
}

type KnowledgeDocumentChunk = {
  id: number
  documentId: number
  chunkNo: number
  chunkText: string
  tokenCount?: number
  summary?: string
  embeddingStatus?: string
  vectorRef?: string
}

type KnowledgeDocumentTask = {
  id: number
  documentId: number
  taskType: string
  status: string
  attemptCount?: number
  lastError?: string
}

type KnowledgeDocumentEntity = {
  id: number
  documentType: string
  status: string
  title?: string
  summary?: string
}

type KnowledgeDocumentDetail = {
  document: KnowledgeDocumentEntity
  assets: KnowledgeDocumentAsset[]
  chunks: KnowledgeDocumentChunk[]
  tasks: KnowledgeDocumentTask[]
}

type ChatMessageItem = {
  id: number
  role: string
  messageType: string
  content: string
  seqNo: number
}

type StructuredInfo = {
  projectName: string
  description: string
  projectBackground: string
  similarProducts: string
  targetCustomerGroups: string
  commercialValue: string
  coreProductValue: string
  businessKnowledgeSummary: string
}

type ConversationTurnResult = {
  sessionId: number
  jobId: string
  assistantMessage: string
  followUpQuestions: string[]
  structuredInfo: StructuredInfo
  readyToCreate: boolean
}

type ConversationView = {
  sessionId: number
  jobId: string
  status: string
  assistantSummary: string
  businessKnowledgeSummary: string
  structuredInfo: StructuredInfo
  messages: ChatMessageItem[]
  materials: SourceMaterial[]
  readyToCreate: boolean
}

type UploadFileMaterialResponse = {
  sessionId: number
  materialCount: number
}

const router = useRouter()
const fileInputRef = ref<HTMLInputElement | null>(null)
const loading = ref(false)
const error = ref(')
const success = ref(')

const sessionId = ref<number | null>(null)
const jobId = ref(')
const status = ref('DRAFT')
const messages = ref<ChatMessageItem[]>([])
const followUpQuestions = ref<string[]>([])
const readyToCreate = ref(false)
const savedMaterials = ref<SourceMaterial[]>([])
const materialKnowledgeMap = ref<Record<number, KnowledgeDocumentListItem[]>>({})
const knowledgeDetailVisible = ref(false)
const knowledgeDetailLoading = ref(false)
const knowledgeDetail = ref<KnowledgeDocumentDetail | null>(null)
const chunkExpanded = ref(false)
const pendingMaterials = ref<SourceMaterial[]>([])
const selectedFile = ref<File | null>(null)
const chatMessage = ref(')

const structuredInfo = reactive<StructuredInfo>({
  projectName: ',
  description: ',
  projectBackground: ',
  similarProducts: ',
  targetCustomerGroups: ',
  commercialValue: ',
  coreProductValue: ',
  businessKnowledgeSummary: '
})

const startForm = reactive({
  projectName: ',
  description: '
})

const createForm = reactive({
  projectKey: ',
  projectName: ',
  projectType: ',
  priority: ',
  visibility: 'PRIVATE',
  ownerUserId: '
})

const urlDraft = reactive({
  title: ',
  sourceUri: '
})

const textDraft = reactive({
  title: ',
  rawContent: '
})

const fileDraft = reactive({
  title: '
})

const canStartConversation = computed(() => {
  return !!(startForm.projectName.trim() || startForm.description.trim() || pendingMaterials.value.length > 0)
})

const visibleChunks = computed(() => {
  if (!knowledgeDetail.value) {
    return []
  }
  return chunkExpanded.value ? knowledgeDetail.value.chunks : knowledgeDetail.value.chunks.slice(0, 3)
})

const statusText = computed(() => {
  if (status.value === 'READY_TO_CREATE') return '可以创建项目'
  if (status.value === 'COMPLETED') return '已完成'
  if (status.value === 'ACTIVE') return '会话进行中'
  return status.value || '草稿'
})

function roleLabel(role: string, messageType: string) {
  if (role === 'assistant' && messageType === 'question') return 'AI 追问'
  if (role === 'assistant') return 'AI'
  if (messageType === 'material') return '资料'
  return '用户'
}

function previewText(text?: string) {
  if (!text) return '-'
  return text.length <= 120 ? text : `${text.slice(0, 120)}...`
}

function knowledgeStatusText(statusValue?: string) {
  if (statusValue === 'READY' || statusValue === 'SUCCESS') return '处理完成'
  if (statusValue === 'FAILED') return '处理失败'
  if (statusValue === 'PROCESSING' || statusValue === 'RUNNING') return '处理中'
  if (statusValue === 'PENDING') return '待处理'
  return statusValue || '未知状态'
}

function materialKnowledgeItems(material: SourceMaterial) {
  if (!material.id) return []
  return materialKnowledgeMap.value[material.id] || []
}

async function refreshKnowledgeStatuses() {
  const targets = savedMaterials.value.filter((item) => typeof item.id === 'number')
  if (targets.length === 0) {
    materialKnowledgeMap.value = {}
    return
  }
  const entries = await Promise.all(
    targets.map(async (item) => {
      const res = await axios.get<ApiResponse<KnowledgeDocumentListItem[]>>(
        `/api/knowledge-documents/source-materials/${item.id}`
      )
      return [item.id as number, res.data.data || []] as const
    })
  )
  materialKnowledgeMap.value = Object.fromEntries(entries)
}

async function openKnowledgeDetail(documentId: number) {
  knowledgeDetailVisible.value = true
  knowledgeDetailLoading.value = true
  knowledgeDetail.value = null
  chunkExpanded.value = false
  try {
    const res = await axios.get<ApiResponse<KnowledgeDocumentDetail>>(`/api/knowledge-documents/${documentId}`)
    knowledgeDetail.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '鍔犺浇鐭ヨ瘑鏂囨。璇︽儏澶辫触銆?
  } finally {
    knowledgeDetailLoading.value = false
  }
}

function closeKnowledgeDetail() {
  knowledgeDetailVisible.value = false
  knowledgeDetail.value = null
  chunkExpanded.value = false
}

function addUrlMaterial() {
  if (!urlDraft.sourceUri.trim()) {
    error.value = '璇峰厛濉啓缃戠珯閾炬帴銆?
    return
  }
  pendingMaterials.value.push({
    materialType: 'URL',
    title: urlDraft.title.trim() || undefined,
    sourceUri: urlDraft.sourceUri.trim()
  })
  urlDraft.title = '
  urlDraft.sourceUri = '
  error.value = '
}

function addTextMaterial() {
  if (!textDraft.rawContent.trim()) {
    error.value = '璇峰厛濉啓鏂囨湰璧勬枡鍐呭銆?
    return
  }
  pendingMaterials.value.push({
    materialType: 'TEXT',
    title: textDraft.title.trim() || undefined,
    rawContent: textDraft.rawContent.trim()
  })
  textDraft.title = '
  textDraft.rawContent = '
  error.value = '
}

function clearPendingMaterials() {
  pendingMaterials.value = []
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] || null
}

function resetFileDraft() {
  selectedFile.value = null
  fileDraft.title = '
  if (fileInputRef.value) {
    fileInputRef.value.value = '
  }
}

function applyStructuredInfo(next: StructuredInfo) {
  structuredInfo.projectName = next.projectName || '
  structuredInfo.description = next.description || '
  structuredInfo.projectBackground = next.projectBackground || '
  structuredInfo.similarProducts = next.similarProducts || '
  structuredInfo.targetCustomerGroups = next.targetCustomerGroups || '
  structuredInfo.commercialValue = next.commercialValue || '
  structuredInfo.coreProductValue = next.coreProductValue || '
  structuredInfo.businessKnowledgeSummary = next.businessKnowledgeSummary || '
  if (!createForm.projectName && structuredInfo.projectName) {
    createForm.projectName = structuredInfo.projectName
  }
}

function appendAssistantTurn(result: ConversationTurnResult) {
  if (result.assistantMessage) {
    messages.value.push({
      id: Date.now(),
      role: 'assistant',
      messageType: 'chat',
      content: result.assistantMessage,
      seqNo: messages.value.length + 1
    })
  }
  if (result.followUpQuestions?.length) {
    result.followUpQuestions.forEach((question, idx) => {
      messages.value.push({
        id: Date.now() + idx + 1,
        role: 'assistant',
        messageType: 'question',
        content: question,
        seqNo: messages.value.length + idx + 1
      })
    })
  }
}

async function startConversation() {
  loading.value = true
  error.value = '
  success.value = '
  try {
    const res = await axios.post<ApiResponse<ConversationTurnResult>>('/api/projects/ai/conversations', {
      projectName: startForm.projectName || null,
      description: startForm.description || null,
      materials: pendingMaterials.value
    })
    const data = res.data.data
    sessionId.value = data.sessionId
    jobId.value = data.jobId
    status.value = data.readyToCreate ? 'READY_TO_CREATE' : 'ACTIVE'
    readyToCreate.value = data.readyToCreate
    messages.value = []
    if (startForm.projectName.trim() || startForm.description.trim()) {
      messages.value.push({
        id: Date.now() - 1,
        role: 'user',
        messageType: 'chat',
        content: [startForm.projectName.trim() && `椤圭洰鍚嶇О锛?{startForm.projectName.trim()}`, startForm.description.trim() && `椤圭洰鎻忚堪锛?{startForm.description.trim()}`]
          .filter(Boolean)
          .join('\n'),
        seqNo: 1
      })
    }
    appendAssistantTurn(data)
    applyStructuredInfo(data.structuredInfo)
    followUpQuestions.value = data.followUpQuestions || []
    savedMaterials.value = [...pendingMaterials.value]
    pendingMaterials.value = []
    if (!createForm.projectName) {
      createForm.projectName = startForm.projectName.trim()
    }
    success.value = 'AI 椤圭洰浼氳瘽宸插惎鍔ㄣ€?
    await refreshConversation()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '鍚姩 AI 浼氳瘽澶辫触銆?
  } finally {
    loading.value = false
  }
}

async function saveMaterials() {
  if (!sessionId.value || pendingMaterials.value.length === 0) return
  loading.value = true
  error.value = '
  success.value = '
  try {
    await axios.post(`/api/projects/ai/conversations/${sessionId.value}/materials`, {
      materials: pendingMaterials.value
    })
    savedMaterials.value = [...savedMaterials.value, ...pendingMaterials.value]
    pendingMaterials.value = []
    success.value = '璧勬枡宸蹭繚瀛樺埌褰撳墠浼氳瘽銆?
    await refreshConversation()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '淇濆瓨璧勬枡澶辫触銆?
  } finally {
    loading.value = false
  }
}

async function uploadFileMaterial() {
  if (!sessionId.value || !selectedFile.value) return
  loading.value = true
  error.value = '
  success.value = '
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    if (fileDraft.title.trim()) {
      formData.append('title', fileDraft.title.trim())
    }
    await axios.post<ApiResponse<UploadFileMaterialResponse>>(
      `/api/projects/ai/conversations/${sessionId.value}/materials/upload`,
      formData
    )
    resetFileDraft()
    success.value = '鏂囦欢璧勬枡宸蹭笂浼狅紝鍚庡彴宸插紑濮嬪鐞嗐€?
    await refreshConversation()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '涓婁紶鏂囦欢璧勬枡澶辫触銆?
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  if (!sessionId.value || !chatMessage.value.trim()) return
  loading.value = true
  error.value = '
  success.value = '
  const userText = chatMessage.value.trim()
  const optimisticMessage: ChatMessageItem = {
    id: Date.now(),
    role: 'user',
    messageType: 'chat',
    content: userText,
    seqNo: messages.value.length + 1
  }
  messages.value.push(optimisticMessage)
  try {
    const res = await axios.post<ApiResponse<ConversationTurnResult>>(`/api/projects/ai/conversations/${sessionId.value}/chat`, {
      message: userText
    })
    const data = res.data.data
    status.value = data.readyToCreate ? 'READY_TO_CREATE' : 'ACTIVE'
    readyToCreate.value = data.readyToCreate
    applyStructuredInfo(data.structuredInfo)
    followUpQuestions.value = data.followUpQuestions || []
    appendAssistantTurn(data)
    chatMessage.value = '
    success.value = data.readyToCreate ? '淇℃伅宸插熀鏈綈澶囷紝鍙互鍒涘缓椤圭洰銆? : 'AI 宸叉洿鏂版暣鐞嗙粨鏋溿€?
  } catch (e: any) {
    messages.value = messages.value.filter((item) => item.id !== optimisticMessage.id)
    error.value = e?.response?.data?.message || e?.message || '鍙戦€佹秷鎭け璐ャ€?
  } finally {
    loading.value = false
  }
}

async function refreshConversation() {
  if (!sessionId.value) return
  loading.value = true
  error.value = '
  try {
    const res = await axios.get<ApiResponse<ConversationView>>(`/api/projects/ai/conversations/${sessionId.value}`)
    const data = res.data.data
    jobId.value = data.jobId
    status.value = data.status || 'ACTIVE'
    readyToCreate.value = data.readyToCreate
    followUpQuestions.value = data.messages
      .filter((item) => item.role === 'assistant' && item.messageType === 'question')
      .slice(-3)
      .map((item) => item.content)
    messages.value = data.messages || []
    savedMaterials.value = data.materials || []
    applyStructuredInfo(data.structuredInfo)
    if (!createForm.projectName && data.structuredInfo.projectName) {
      createForm.projectName = data.structuredInfo.projectName
    }
    await refreshKnowledgeStatuses()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '鍒锋柊浼氳瘽澶辫触銆?
  } finally {
    loading.value = false
  }
}

async function createProject() {
  if (!sessionId.value) return
  if (!createForm.projectKey.trim()) {
    error.value = '璇峰厛濉啓椤圭洰 Key銆?
    success.value = '
    return
  }
  if (createForm.ownerUserId && (!Number.isInteger(Number(createForm.ownerUserId)) || Number(createForm.ownerUserId) <= 0)) {
    error.value = '璐熻矗浜虹敤鎴?ID 蹇呴』鏄鏁存暟銆?
    success.value = '
    return
  }
  loading.value = true
  error.value = '
  success.value = '
  try {
    const res = await axios.post<ApiResponse<{ sessionId: number; projectId: number }>>(
      `/api/projects/ai/conversations/${sessionId.value}/create-project`,
      {
        projectKey: createForm.projectKey,
        projectName: createForm.projectName || null,
        projectType: createForm.projectType || null,
        priority: createForm.priority || null,
        visibility: createForm.visibility || null,
        ownerUserId: createForm.ownerUserId ? Number(createForm.ownerUserId) : null
      }
    )
    const projectId = res.data.data.projectId
    status.value = 'COMPLETED'
    success.value = '椤圭洰鍒涘缓鎴愬姛锛屾鍦ㄨ烦杞埌椤圭洰椤点€?
    router.push(`/projects?projectId=${projectId}`)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '鍒涘缓椤圭洰澶辫触銆?
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 1280px;
  margin: 18px auto;
  padding: 0 14px 18px;
  color: #111827;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.muted {
  color: #6b7280;
}

.layout {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 14px;
}

.card {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 14px;
}

.grid {
  display: grid;
  gap: 10px;
}

.grid.two {
  grid-template-columns: 1fr 1.3fr;
}

.create-grid {
  grid-template-columns: repeat(3, minmax(160px, 1fr));
}

.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 9px 10px;
  background: #fff;
}

textarea.input {
  min-height: 92px;
  resize: vertical;
}

.materials {
  display: grid;
  gap: 12px;
  margin-top: 10px;
}

.material-card,
.pending-item,
.preview-item,
.question-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
  background: #fafcff;
}

.pending-list {
  margin-top: 12px;
}

.knowledge-status-list {
  display: grid;
  gap: 6px;
  margin-top: 8px;
}

.knowledge-status-block {
  display: grid;
  gap: 4px;
}

.knowledge-status-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.knowledge-error {
  color: #b91c1c;
  font-size: 12px;
  line-height: 1.5;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 30;
}

.modal-card {
  width: min(920px, 100%);
  max-height: 80vh;
  overflow: auto;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #dbe2ea;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}

.detail-grid {
  display: grid;
  gap: 12px;
}

.detail-block {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
  background: #fafcff;
}

.detail-block p {
  margin: 6px 0 0;
  white-space: pre-wrap;
}

.detail-block.wide {
  grid-column: 1 / -1;
}

.chat-list {
  display: grid;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
  margin-bottom: 12px;
}

.chat-item {
  border-radius: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe2ea;
}

.chat-item.user {
  background: #f6f9ff;
}

.chat-item.assistant {
  background: #fffef7;
}

.chat-role {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
  text-transform: uppercase;
}

.chat-content,
.preview-item p {
  white-space: pre-wrap;
  margin: 0;
}

.questions {
  margin: 10px 0 12px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 10px;
}

.preview-item.wide {
  grid-column: 1 / -1;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.primary,
.ghost,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.ghost,
.mini {
  background: #f3f4f6;
}

.mini {
  padding: 5px 9px;
  font-size: 12px;
}

.badge {
  padding: 4px 8px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 12px;
}

.badge.ready {
  background: #dcfce7;
  color: #166534;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 999px;
  background: #e5e7eb;
  color: #374151;
  font-size: 12px;
}

.status-badge.ready,
.status-badge.success {
  background: #dcfce7;
  color: #166534;
}

.status-badge.failed {
  background: #fee2e2;
  color: #b91c1c;
}

.status-badge.pending,
.status-badge.processing,
.status-badge.running {
  background: #dbeafe;
  color: #1d4ed8;
}

.error {
  color: #b91c1c;
}

.success {
  color: #166534;
}

.empty-state {
  color: #6b7280;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-card {
  margin-top: 0;
}

@media (max-width: 960px) {
  .layout,
  .grid.two,
  .create-grid,
  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>



