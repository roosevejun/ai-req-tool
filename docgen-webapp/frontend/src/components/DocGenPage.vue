<template>
  <div>
    <section class="card">
      <h2>1. 输入业务背景</h2>
      <p class="hint">先描述业务场景、目标用户、核心流程和你当前的想法，AI 会先分析并逐步追问，再生成 PRD。</p>
      <details class="details">
        <summary>如果你已经有上一版 PRD，可以贴到这里作为修订基线</summary>
        <p class="hint">AI 会在已有 PRD 的基础上做增量整理，便于后续查看版本变化。</p>
        <textarea
          v-model="previousPrdMarkdown"
          class="textarea"
          placeholder="粘贴上一版 PRD Markdown，可选"
        />
      </details>
      <textarea
        v-model="businessDescription"
        class="textarea"
        placeholder="例如：我们要做一个面向企业客户的 AI 需求管理工具，支持项目创建、需求澄清、PRD 生成与版本管理。"
      />
      <div class="row">
        <button class="primary" :disabled="loading" @click="onCreateJob">
          {{ loading ? '启动中...' : '启动 AI 整理' }}
        </button>
        <button class="ghost" :disabled="loading" @click="onReset">重置</button>
      </div>
    </section>

    <section v-if="jobId" class="card">
      <h2>2. AI 分析与确认对话</h2>
      <div class="meta">
        <span>任务 ID: <code>{{ jobId }}</code></span>
        <span>状态: <code>{{ statusLabel(status) }}</code></span>
      </div>

      <div class="checklists">
        <div class="list">
          <h3>已确认信息</h3>
          <ul>
            <li v-for="(it, idx) in confirmedItems" :key="`confirmed-${idx}`">{{ it }}</li>
            <li v-if="confirmedItems.length === 0" class="muted">暂时还没有确认项</li>
          </ul>
        </div>
        <div class="list">
          <h3>待补充信息</h3>
          <ul>
            <li v-for="(it, idx) in unconfirmedItems" :key="`pending-${idx}`">{{ it }}</li>
            <li v-if="unconfirmedItems.length === 0" class="muted">当前没有待补充项，可以直接生成 PRD。</li>
          </ul>
        </div>
      </div>

      <div class="chat-box">
        <div v-for="(m, idx) in chatHistory" :key="idx" class="msg" :class="m.role">
          <div class="bubble">
            <div class="role">{{ m.role === 'assistant' ? 'AI' : '你' }}</div>
            <div>{{ m.content }}</div>
          </div>
        </div>
      </div>

      <div v-if="status !== 'COMPLETED'">
        <p v-if="pendingQuestion" class="hint"><b>当前追问：</b>{{ pendingQuestion }}</p>

        <div v-if="unconfirmedItems.length > 0" class="guide">
          <div class="guide-title">结构化补充建议</div>
          <pre class="guide-pre">{{ buildGuidanceText(unconfirmedItems) }}</pre>
          <div class="row">
            <button class="ghost" :disabled="loading" @click="useGuidanceTemplate">填入到对话输入框</button>
          </div>
        </div>

        <div v-if="unconfirmedItems.length > 0" class="form-box">
          <div class="guide-title">逐项回答待补充问题</div>
          <div class="qa-progress">第 {{ currentQuestionIndex + 1 }} / {{ unconfirmedItems.length }} 项</div>
          <div v-if="currentQuestionItem" class="form-item">
            <label class="form-label">{{ currentQuestionItem }}</label>
            <textarea
              v-model="structuredInputs[currentQuestionItem]"
              class="textarea"
              :placeholder="getPlaceholderForItem(currentQuestionItem)"
            />
          </div>
          <div class="row">
            <button class="ghost" :disabled="loading || currentQuestionIndex <= 0" @click="goPrevQuestion">上一项</button>
            <button
              class="ghost"
              :disabled="loading || currentQuestionIndex >= unconfirmedItems.length - 1"
              @click="goNextQuestion"
            >
              下一项
            </button>
            <button class="primary" :disabled="loading || !canSendCurrent" @click="onSendCurrent">
              {{ loading ? '提交中...' : '提交当前回答' }}
            </button>
            <button class="ghost" :disabled="loading || !canSendStructured" @click="onSendStructured">
              {{ loading ? '提交中...' : '一次性提交全部回答' }}
            </button>
          </div>
        </div>

        <input v-model="chatInput" class="input" placeholder="直接补充说明、修正 AI 理解，或回答当前追问" />
        <div class="row">
          <button class="primary" :disabled="loading || !chatInput.trim()" @click="onSendChat">
            {{ loading ? '发送中...' : '发送给 AI' }}
          </button>
          <button class="ghost" :disabled="loading || unconfirmedItems.length > 0" @click="onGeneratePrd">
            {{ loading ? '生成中...' : '生成 PRD' }}
          </button>
        </div>
      </div>

      <div v-if="status === 'COMPLETED'">
        <h3>3. 生成结果</h3>
        <div class="diff-box" v-if="basePrdMarkdown">
          <b>与基线版本对比</b>
          <div>新增 {{ diffSummary.added }} 行，删除 {{ diffSummary.removed }} 行。</div>
        </div>
        <div class="row">
          <button class="ghost" @click="onExportPrd">导出 Markdown</button>
        </div>
        <pre class="pre">{{ prdMarkdown }}</pre>
      </div>
    </section>

    <div v-if="error" class="error">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import axios from 'axios'

const props = withDefaults(defineProps<{ apiBase?: string; draftKey?: string }>(), {
  apiBase: '/api/docgen',
  draftKey: 'docgen-draft-v1'
})

type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

type ClarifyQuestion = {
  id: string
  question: string
  whyNeeded: string
  required: boolean
}

type ChatMessage = {
  role: string
  content: string
}

type CreateJobResponse = {
  jobId: string
  status: string
  clarifyQuestions: ClarifyQuestion[] | null
  prdMarkdown: string | null
  readyToGenerate: boolean
  pendingQuestion: string
  confirmedItems: string[]
  unconfirmedItems: string[]
  chatHistory: ChatMessage[]
  basePrdMarkdown: string | null
  currentVersion: number
}

type ChatResponse = {
  jobId: string
  status: string
  assistantMessage: string
  pendingQuestion: string
  confirmedItems: string[]
  unconfirmedItems: string[]
  chatHistory: ChatMessage[]
  prdMarkdown: string | null
  basePrdMarkdown: string | null
  currentVersion: number
}

const businessDescription = ref('')
const previousPrdMarkdown = ref('')
const jobId = ref('')
const status = ref('')
const currentVersion = ref(0)
const clarifyQuestions = ref<ClarifyQuestion[]>([])
const prdMarkdown = ref('')
const basePrdMarkdown = ref('')
const chatHistory = ref<ChatMessage[]>([])
const pendingQuestion = ref('')
const confirmedItems = ref<string[]>([])
const unconfirmedItems = ref<string[]>([])
const currentQuestionIndex = ref(0)
const chatInput = ref('')
const structuredInputs = reactive<Record<string, string>>({})
const loading = ref(false)
const error = ref('')

const currentQuestionItem = computed(() => {
  if (unconfirmedItems.value.length === 0) return ''
  const idx = Math.min(Math.max(currentQuestionIndex.value, 0), unconfirmedItems.value.length - 1)
  return unconfirmedItems.value[idx] || ''
})

const canSendCurrent = computed(() => {
  const item = currentQuestionItem.value
  if (!item) return false
  return (structuredInputs[item] || '').trim().length > 0
})

const canSendStructured = computed(() => {
  if (unconfirmedItems.value.length === 0) return false
  return unconfirmedItems.value.every((item) => (structuredInputs[item] || '').trim().length > 0)
})

const diffSummary = computed(() => {
  const base = (basePrdMarkdown.value || '').split('\n')
  const revised = (prdMarkdown.value || '').split('\n')
  const baseSet = new Set(base)
  const revisedSet = new Set(revised)
  let added = 0
  let removed = 0
  for (const line of revisedSet) {
    if (line.trim() && !baseSet.has(line)) added += 1
  }
  for (const line of baseSet) {
    if (line.trim() && !revisedSet.has(line)) removed += 1
  }
  return { added, removed }
})

const DRAFT_KEY = props.draftKey

function statusLabel(value?: string) {
  if (value === 'CREATED') return '已创建'
  if (value === 'ANALYZING') return '分析中'
  if (value === 'CLARIFYING') return '澄清中'
  if (value === 'READY_TO_GENERATE') return '可生成'
  if (value === 'COMPLETED') return '已完成'
  return value || '-'
}

async function onCreateJob() {
  error.value = ''
  loading.value = true
  try {
    const res = await axios.post<ApiResponse<CreateJobResponse>>(`${props.apiBase}/jobs`, {
      businessDescription: businessDescription.value,
      previousPrdMarkdown: previousPrdMarkdown.value
    })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '启动整理失败。')
    }
    const data = res.data.data
    jobId.value = data.jobId
    status.value = data.status
    currentVersion.value = data.currentVersion ?? 0
    clarifyQuestions.value = data.clarifyQuestions ?? []
    chatHistory.value = data.chatHistory ?? []
    pendingQuestion.value = data.pendingQuestion ?? ''
    confirmedItems.value = data.confirmedItems ?? []
    unconfirmedItems.value = data.unconfirmedItems ?? []
    initStructuredInputs(unconfirmedItems.value)
    prdMarkdown.value = data.prdMarkdown ?? ''
    basePrdMarkdown.value = data.basePrdMarkdown ?? previousPrdMarkdown.value
  } catch (e: any) {
    const resp = e?.response
    error.value = resp?.data ? `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}` : e?.message || '启动整理失败。'
  } finally {
    loading.value = false
  }
}

async function onSendChat() {
  error.value = ''
  loading.value = true
  try {
    const message = chatInput.value.trim()
    if (!message) return
    const res = await axios.post<ApiResponse<ChatResponse>>(`${props.apiBase}/jobs/${jobId.value}/chat`, { message })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '发送对话失败。')
    }
    const data = res.data.data
    status.value = data.status
    currentVersion.value = data.currentVersion ?? currentVersion.value
    chatHistory.value = data.chatHistory ?? []
    pendingQuestion.value = data.pendingQuestion ?? ''
    confirmedItems.value = data.confirmedItems ?? []
    unconfirmedItems.value = data.unconfirmedItems ?? []
    initStructuredInputs(unconfirmedItems.value)
    prdMarkdown.value = data.prdMarkdown ?? ''
    basePrdMarkdown.value = data.basePrdMarkdown ?? basePrdMarkdown.value
    chatInput.value = ''
  } catch (e: any) {
    const resp = e?.response
    error.value = resp?.data ? `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}` : e?.message || '发送对话失败。'
  } finally {
    loading.value = false
  }
}

async function onGeneratePrd() {
  error.value = ''
  loading.value = true
  try {
    const res = await axios.post<ApiResponse<ChatResponse>>(`${props.apiBase}/jobs/${jobId.value}/generate`)
    if (res.data.code !== 0) throw new Error(res.data.message || '生成 PRD 失败。')
    const data = res.data.data
    status.value = data.status
    currentVersion.value = data.currentVersion ?? currentVersion.value
    chatHistory.value = data.chatHistory ?? []
    confirmedItems.value = data.confirmedItems ?? []
    unconfirmedItems.value = data.unconfirmedItems ?? []
    initStructuredInputs(unconfirmedItems.value)
    prdMarkdown.value = data.prdMarkdown ?? ''
    basePrdMarkdown.value = data.basePrdMarkdown ?? basePrdMarkdown.value
  } catch (e: any) {
    const resp = e?.response
    error.value = resp?.data ? `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}` : e?.message || '生成 PRD 失败。'
  } finally {
    loading.value = false
  }
}

function onReset() {
  businessDescription.value = ''
  previousPrdMarkdown.value = ''
  jobId.value = ''
  status.value = ''
  currentVersion.value = 0
  clarifyQuestions.value = []
  chatHistory.value = []
  pendingQuestion.value = ''
  confirmedItems.value = []
  unconfirmedItems.value = []
  Object.keys(structuredInputs).forEach((k) => delete structuredInputs[k])
  chatInput.value = ''
  prdMarkdown.value = ''
  basePrdMarkdown.value = ''
  error.value = ''
  localStorage.removeItem(DRAFT_KEY)
}

function initStructuredInputs(items: string[]) {
  const set = new Set(items)
  Object.keys(structuredInputs).forEach((k) => {
    if (!set.has(k)) delete structuredInputs[k]
  })
  for (const it of items) {
    if (structuredInputs[it] === undefined) structuredInputs[it] = ''
  }
  if (items.length === 0) {
    currentQuestionIndex.value = 0
    return
  }
  if (currentQuestionIndex.value > items.length - 1) currentQuestionIndex.value = items.length - 1
  if (currentQuestionIndex.value < 0) currentQuestionIndex.value = 0
}

function buildGuidanceText(items: string[]): string {
  return items.map((item, index) => `${index + 1}. ${item}\n- 请补充你的业务理解、目标、限制条件和期望结果`).join('\n\n')
}

function useGuidanceTemplate() {
  chatInput.value = buildGuidanceText(unconfirmedItems.value)
}

function goPrevQuestion() {
  if (currentQuestionIndex.value > 0) currentQuestionIndex.value -= 1
}

function goNextQuestion() {
  if (currentQuestionIndex.value < unconfirmedItems.value.length - 1) currentQuestionIndex.value += 1
}

function getPlaceholderForItem(item: string): string {
  return `请补充：${item}`
}

async function onSendStructured() {
  const lines: string[] = []
  for (const item of unconfirmedItems.value) {
    const v = (structuredInputs[item] || '').trim()
    if (!v) {
      error.value = `请先补充：${item}`
      return
    }
    lines.push(`${item}\n${v}`)
  }
  chatInput.value = lines.join('\n\n')
  await onSendChat()
}

async function onSendCurrent() {
  const item = currentQuestionItem.value
  if (!item) return
  const value = (structuredInputs[item] || '').trim()
  if (!value) {
    error.value = `请先补充：${item}`
    return
  }
  chatInput.value = `${item}\n${value}`
  await onSendChat()
}

async function onExportPrd() {
  if (!jobId.value) return
  try {
    const resp = await axios.get(`${props.apiBase}/jobs/${jobId.value}/export`, { responseType: 'blob' })
    const blob = new Blob([resp.data], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const version = currentVersion.value > 0 ? currentVersion.value : 1
    a.download = `01-PRD-Agent需求文档-v${version}.md`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e: any) {
    error.value = e?.message || '导出失败。'
  }
}

function loadDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const d = JSON.parse(raw)
    businessDescription.value = d.businessDescription || ''
    previousPrdMarkdown.value = d.previousPrdMarkdown || ''
    chatInput.value = d.chatInput || ''
    for (const [k, v] of Object.entries(d.structuredInputs || {})) {
      structuredInputs[k] = String(v || '')
    }
  } catch {
    // ignore broken draft
  }
}

watch(
  [businessDescription, previousPrdMarkdown, chatInput, unconfirmedItems],
  () => {
    const data = {
      businessDescription: businessDescription.value,
      previousPrdMarkdown: previousPrdMarkdown.value,
      chatInput: chatInput.value,
      structuredInputs
    }
    localStorage.setItem(DRAFT_KEY, JSON.stringify(data))
  },
  { deep: true }
)

loadDraft()
</script>

<style scoped>
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 14px;
}

.hint {
  color: #6b7280;
  margin-top: -6px;
  margin-bottom: 10px;
}

.details {
  margin-bottom: 12px;
}

.textarea {
  width: 100%;
  min-height: 140px;
  resize: vertical;
  padding: 10px;
  box-sizing: border-box;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  font-size: 14px;
  line-height: 1.4;
}

.row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

button {
  border-radius: 8px;
  padding: 10px 14px;
  border: 1px solid transparent;
  cursor: pointer;
  font-size: 14px;
}

.primary {
  background: #2563eb;
  color: #fff;
}

.ghost {
  background: #f3f4f6;
  color: #111827;
  border-color: #e5e7eb;
}

.meta {
  display: flex;
  gap: 14px;
  color: #374151;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.checklists {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.list {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
}

.list h3 {
  margin: 0 0 8px;
}

.chat-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 12px 0;
}

.msg {
  display: flex;
}

.msg.assistant {
  justify-content: flex-start;
}

.msg.user {
  justify-content: flex-end;
}

.bubble {
  max-width: 80%;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 10px 12px;
}

.msg.user .bubble {
  background: #e0ecff;
  border-color: #bfd6ff;
}

.role {
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 4px;
  color: #475569;
}

.guide,
.form-box,
.diff-box {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
  margin: 12px 0;
  background: #fafcff;
}

.guide-title,
.form-label {
  font-weight: 700;
  color: #334155;
}

.guide-pre,
.pre {
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px;
  margin-top: 10px;
}

.pre {
  max-height: 600px;
  overflow: auto;
}

.qa-progress,
.muted {
  color: #6b7280;
}

.input {
  width: 100%;
  box-sizing: border-box;
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
}

.error {
  margin-top: 12px;
  color: #b91c1c;
}

@media (max-width: 900px) {
  .checklists {
    grid-template-columns: 1fr;
  }

  .bubble {
    max-width: 100%;
  }
}
</style>
