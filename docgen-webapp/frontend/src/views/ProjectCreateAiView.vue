<template>
  <div class="page">
    <header class="topbar">
      <div>
        <h1>AI 对话式创建项目</h1>
        <p class="sub">
          先让 AI 帮我们理解项目背景、客户、价值和参考资料，再把结果沉淀为正式项目。
        </p>
      </div>
    </header>

    <div class="layout">
      <section class="card">
        <h3>启动会话</h3>
        <div class="grid two">
          <input v-model.trim="startForm.projectName" class="input" placeholder="项目名称，例如：AI 需求管理平台" />
          <textarea
            v-model="startForm.description"
            class="input"
            placeholder="先描述你已经知道的信息，例如项目目标、业务场景、解决的问题。"
          />
        </div>

        <h3>补充资料</h3>
        <div class="materials">
          <div class="material-card">
            <div class="section-head">
              <strong>网站链接</strong>
              <button class="mini" @click="addUrlMaterial">添加</button>
            </div>
            <input v-model.trim="urlDraft.title" class="input" placeholder="资料标题，可选" />
            <input v-model.trim="urlDraft.sourceUri" class="input" placeholder="https://example.com" />
          </div>

          <div class="material-card">
            <div class="section-head">
              <strong>文本资料</strong>
              <button class="mini" @click="addTextMaterial">添加</button>
            </div>
            <input v-model.trim="textDraft.title" class="input" placeholder="资料标题，可选" />
            <textarea
              v-model="textDraft.rawContent"
              class="input"
              placeholder="可以粘贴会议纪要、客户原话、产品简介、行业材料等。"
            />
          </div>
        </div>

        <div v-if="pendingMaterials.length > 0" class="pending-list">
          <div class="section-head">
            <strong>待保存资料</strong>
            <button class="ghost mini" @click="clearPendingMaterials">清空</button>
          </div>
          <div v-for="(item, idx) in pendingMaterials" :key="idx" class="pending-item">
            <div>{{ item.materialType }} / {{ item.title || '未命名资料' }}</div>
            <div class="muted">{{ item.sourceUri || item.rawContent?.slice(0, 120) || '-' }}</div>
          </div>
        </div>

        <div v-if="savedMaterials.length > 0" class="pending-list">
          <div class="section-head">
            <strong>已保存资料</strong>
            <span class="muted">{{ savedMaterials.length }} 条</span>
          </div>
          <div v-for="(item, idx) in savedMaterials" :key="`saved-${idx}`" class="pending-item">
            <div>{{ item.materialType }} / {{ item.title || '未命名资料' }}</div>
            <div class="muted">{{ item.sourceUri || item.rawContent?.slice(0, 120) || '-' }}</div>
          </div>
        </div>

        <div class="row">
          <button class="primary" :disabled="loading || !canStartConversation" @click="startConversation">
            启动 AI 会话
          </button>
          <button class="ghost" :disabled="loading || !sessionId || pendingMaterials.length === 0" @click="saveMaterials">
            保存补充资料
          </button>
        </div>
      </section>

      <section class="card">
        <div class="section-head">
          <h3>AI 会话</h3>
          <span class="muted" v-if="sessionId">Session #{{ sessionId }} / {{ statusText }}</span>
        </div>

        <div v-if="messages.length === 0" class="empty-state">
          输入项目描述或补充资料后，点击“启动 AI 会话”。
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
          <h4>AI 当前关注的问题</h4>
          <div v-for="(question, idx) in followUpQuestions" :key="question + idx" class="question-item">
            {{ idx + 1 }}. {{ question }}
          </div>
        </div>

        <textarea
          v-model="chatMessage"
          class="input"
          :disabled="!sessionId || loading"
          placeholder="继续告诉 AI 更多背景、客户、价值、范围边界或纠正它的理解。"
        />

        <div class="row">
          <button class="primary" :disabled="loading || !sessionId || !chatMessage.trim()" @click="sendMessage">
            发送消息
          </button>
          <button class="ghost" :disabled="loading || !sessionId" @click="refreshConversation">
            刷新会话
          </button>
        </div>
      </section>
    </div>

    <section class="card result-card">
      <div class="section-head">
        <h3>结构化结果</h3>
        <span class="badge" :class="{ ready: readyToCreate }">
          {{ readyToCreate ? '已满足创建条件' : '信息还可继续完善' }}
        </span>
      </div>

      <div class="preview-grid">
        <div class="preview-item">
          <strong>项目背景</strong>
          <p>{{ structuredInfo.projectBackground || '-' }}</p>
        </div>
        <div class="preview-item">
          <strong>类似产品参考</strong>
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
          <strong>产品核心价值</strong>
          <p>{{ structuredInfo.coreProductValue || '-' }}</p>
        </div>
        <div class="preview-item wide">
          <strong>项目业务知识摘要</strong>
          <p>{{ structuredInfo.businessKnowledgeSummary || '-' }}</p>
        </div>
      </div>
    </section>

    <section class="card">
      <h3>创建正式项目</h3>
      <div class="grid create-grid">
        <input v-model.trim="createForm.projectKey" class="input" placeholder="项目 Key，例如 AI-INSPECT" />
        <input v-model.trim="createForm.projectName" class="input" placeholder="项目名称，可覆盖 AI 建议名称" />
        <select v-model="createForm.projectType" class="input">
          <option value="">项目类型，可选</option>
          <option value="PRODUCT">PRODUCT</option>
          <option value="PLATFORM">PLATFORM</option>
          <option value="OPS">OPS</option>
          <option value="INTEGRATION">INTEGRATION</option>
        </select>
        <select v-model="createForm.priority" class="input">
          <option value="">优先级，可选</option>
          <option value="P0">P0</option>
          <option value="P1">P1</option>
          <option value="P2">P2</option>
          <option value="P3">P3</option>
        </select>
        <select v-model="createForm.visibility" class="input">
          <option value="PRIVATE">PRIVATE</option>
          <option value="ORG">ORG</option>
        </select>
        <input v-model.trim="createForm.ownerUserId" class="input" placeholder="负责人用户 ID，可选" />
      </div>

      <div class="row">
        <button
          class="primary"
          :disabled="loading || !sessionId || !readyToCreate"
          @click="createProject"
        >
          从 AI 会话创建项目
        </button>
      </div>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

type ApiResponse<T> = { code: number; message: string; data: T; traceId: string }
type SourceMaterial = {
  materialType: string
  title?: string
  sourceUri?: string
  rawContent?: string
  aiExtractedSummary?: string
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

const router = useRouter()
const loading = ref(false)
const error = ref('')
const success = ref('')

const sessionId = ref<number | null>(null)
const jobId = ref('')
const status = ref('DRAFT')
const messages = ref<ChatMessageItem[]>([])
const followUpQuestions = ref<string[]>([])
const readyToCreate = ref(false)
const savedMaterials = ref<SourceMaterial[]>([])
const chatMessage = ref('')
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

const startForm = reactive({
  projectName: '',
  description: ''
})

const createForm = reactive({
  projectKey: '',
  projectName: '',
  projectType: '',
  priority: '',
  visibility: 'PRIVATE',
  ownerUserId: ''
})

const urlDraft = reactive({
  title: '',
  sourceUri: ''
})

const textDraft = reactive({
  title: '',
  rawContent: ''
})

const pendingMaterials = ref<SourceMaterial[]>([])
const canStartConversation = computed(() => {
  return !!(startForm.projectName.trim() || startForm.description.trim() || pendingMaterials.value.length > 0)
})
const statusText = computed(() => {
  if (status.value === 'READY_TO_CREATE') return '待创建项目'
  if (status.value === 'COMPLETED') return '已创建项目'
  if (status.value === 'ACTIVE') return '会话进行中'
  return status.value || '未开始'
})

function roleLabel(role: string, messageType: string) {
  if (role === 'assistant' && messageType === 'question') return 'AI 追问'
  if (role === 'assistant') return 'AI'
  if (messageType === 'material') return '资料'
  return '你'
}

function addUrlMaterial() {
  if (!urlDraft.sourceUri.trim()) {
    error.value = '请先输入网站链接。'
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
    error.value = '请先输入文本资料内容。'
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
  error.value = ''
  success.value = ''
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
        content: [
          startForm.projectName.trim() && `项目名称：${startForm.projectName.trim()}`,
          startForm.description.trim() && `项目简介：${startForm.description.trim()}`
        ]
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
    success.value = '项目 AI 会话已启动。'
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
    savedMaterials.value = [...savedMaterials.value, ...pendingMaterials.value]
    pendingMaterials.value = []
    success.value = '资料已保存到当前会话。'
    await refreshConversation()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '保存资料失败。'
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  if (!sessionId.value || !chatMessage.value.trim()) return
  loading.value = true
  error.value = ''
  success.value = ''
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
    chatMessage.value = ''
    success.value = data.readyToCreate ? '当前信息已经可以创建项目。' : 'AI 已更新对项目的理解。'
  } catch (e: any) {
    messages.value = messages.value.filter((item) => item.id !== optimisticMessage.id)
    error.value = e?.response?.data?.message || e?.message || '发送消息失败。'
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
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '刷新会话失败。'
  } finally {
    loading.value = false
  }
}

async function createProject() {
  if (!sessionId.value) return
  if (!createForm.projectKey.trim()) {
    error.value = '请先填写项目 Key。'
    success.value = ''
    return
  }
  if (createForm.ownerUserId && (!Number.isInteger(Number(createForm.ownerUserId)) || Number(createForm.ownerUserId) <= 0)) {
    error.value = '负责人用户 ID 必须是正整数。'
    success.value = ''
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
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
    success.value = '项目创建成功，正在跳转到项目页。'
    router.push(`/projects?projectId=${projectId}`)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建项目失败。'
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
.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.sub,
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
.row,
.ops {
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
