<template>
  <div>
    <section class="card">
      <h2>1. 输入你的需求</h2>
      <p class="hint">AI会先分析并和你逐步确认，再细化生成PRD。</p>
      <details class="details">
        <summary>（可选）基于上一版 PRD 做修订</summary>
        <p class="hint">把上一版 PRD Markdown 粘贴到这里，AI 会以此为基线进行增量修订。</p>
        <textarea v-model="previousPrdMarkdown" class="textarea" placeholder="粘贴上一版PRD Markdown（可留空）" />
      </details>
      <textarea
        v-model="businessDescription"
        class="textarea"
        placeholder="例如：我要做一个XXX管理系统，支持登录、列表、导出..."
      />
      <div class="row">
        <button class="primary" :disabled="loading" @click="onCreateJob">
          {{ loading ? '处理中...' : '开始分析' }}
        </button>
        <button class="ghost" :disabled="loading" @click="onReset">重置</button>
      </div>
    </section>

    <section v-if="jobId" class="card">
      <h2>2. AI分析与确认对话</h2>
      <div class="meta">
        <span>jobId: <code>{{ jobId }}</code></span>
        <span>状态: <code>{{ status }}</code></span>
      </div>

      <div class="checklists">
        <div class="list">
          <h3>已确认项</h3>
          <ul>
            <li v-for="(it, idx) in confirmedItems" :key="idx">{{ it }}</li>
            <li v-if="confirmedItems.length === 0" class="muted">暂无</li>
          </ul>
        </div>
        <div class="list">
          <h3>未确认项</h3>
          <ul>
            <li v-for="(it, idx) in unconfirmedItems" :key="idx">{{ it }}</li>
            <li v-if="unconfirmedItems.length === 0" class="muted">无（已满足生成条件）</li>
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
        <p v-if="pendingQuestion" class="hint"><b>当前待确认：</b>{{ pendingQuestion }}</p>
        <div v-if="unconfirmedItems.length > 0" class="guide">
          <div class="guide-title">引导输入建议（可直接复制后补充）</div>
          <pre class="guide-pre">{{ buildGuidanceText(unconfirmedItems) }}</pre>
          <div class="row">
            <button class="ghost" :disabled="loading" @click="useGuidanceTemplate">
              一键填入输入框
            </button>
          </div>
        </div>

        <div v-if="unconfirmedItems.length > 0" class="form-box">
          <div class="guide-title">分项补充（推荐）</div>
          <div v-for="item in unconfirmedItems" :key="item" class="form-item">
            <label class="form-label">{{ item }}</label>
            <textarea
              v-model="structuredInputs[item]"
              class="textarea"
              :placeholder="getPlaceholderForItem(item)"
            />
          </div>
          <div class="row">
            <button class="primary" :disabled="loading || !canSendStructured" @click="onSendStructured">
              {{ loading ? '发送中...' : '一键合并并发送给AI' }}
            </button>
          </div>
        </div>
        <input v-model="chatInput" class="input" placeholder="输入你的补充说明或回答..." />
        <div class="row">
          <button class="primary" :disabled="loading || !chatInput.trim()" @click="onSendChat">
            {{ loading ? '发送中...' : '发送回答' }}
          </button>
          <button class="ghost" :disabled="loading || unconfirmedItems.length > 0" @click="onGeneratePrd">
            {{ loading ? '生成中...' : '生成PRD文档' }}
          </button>
        </div>
      </div>

      <div v-if="status === 'COMPLETED'">
        <h3>3. 生成结果（需求文档 PRD）</h3>
        <div class="diff-box" v-if="basePrdMarkdown">
          <b>修订摘要</b>
          <div>相对基线版：新增 {{ diffSummary.added }} 行，删除 {{ diffSummary.removed }} 行（按行去重近似统计）</div>
        </div>
        <div class="row">
          <button class="ghost" @click="onExportPrd">下载修订结果 .md</button>
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

type CreateJobResponse = {
  jobId: string
  status: string
  clarifyQuestions: ClarifyQuestion[] | null
  prdMarkdown: string | null
}

type ChatMessage = {
  role: string
  content: string
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
}

const businessDescription = ref('')
const previousPrdMarkdown = ref('')
const jobId = ref<string>('')
const status = ref<string>('')
const clarifyQuestions = ref<ClarifyQuestion[]>([])
const prdMarkdown = ref<string>('')
const basePrdMarkdown = ref<string>('')
const chatHistory = ref<ChatMessage[]>([])
const pendingQuestion = ref<string>('')
const confirmedItems = ref<string[]>([])
const unconfirmedItems = ref<string[]>([])
const chatInput = ref('')
const structuredInputs = reactive<Record<string, string>>({})
const loading = ref(false)
const error = ref<string>('')

const canSendStructured = computed(() => {
  if (unconfirmedItems.value.length === 0) return false
  return unconfirmedItems.value.every(item => (structuredInputs[item] || '').trim().length > 0)
})

const diffSummary = computed(() => {
  const base = (basePrdMarkdown.value || '').split('\n')
  const revised = (prdMarkdown.value || '').split('\n')
  const baseSet = new Set(base)
  const revisedSet = new Set(revised)
  let added = 0
  let removed = 0
  for (const line of revisedSet) {
    if (line.trim() && !baseSet.has(line)) added++
  }
  for (const line of baseSet) {
    if (line.trim() && !revisedSet.has(line)) removed++
  }
  return { added, removed }
})

const DRAFT_KEY = 'docgen-draft-v1'

async function onCreateJob() {
  error.value = ''
  loading.value = true
  try {
    const res = await axios.post<ApiResponse<CreateJobResponse>>('/api/docgen/jobs', {
      businessDescription: businessDescription.value,
      previousPrdMarkdown: previousPrdMarkdown.value
    })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '请求失败')
    }
    const data = res.data.data
    jobId.value = data.jobId
    status.value = data.status
    clarifyQuestions.value = (data.clarifyQuestions ?? []) as ClarifyQuestion[]
    if (clarifyQuestions.value.length > 0) {
      const first = clarifyQuestions.value[0]
      const msg = `${first.question}${first.whyNeeded ? `（原因：${first.whyNeeded}）` : ''}`
      chatHistory.value = [{ role: 'assistant', content: msg }]
      pendingQuestion.value = first.question
      confirmedItems.value = []
      unconfirmedItems.value = ['用户与角色', '核心业务流程', '输入输出与边界', '验收标准口径']
    }
    prdMarkdown.value = data.prdMarkdown ?? ''
    basePrdMarkdown.value = previousPrdMarkdown.value
  } catch (e: any) {
    // 如果后端已返回 body，尽量展示出来便于排查
    const resp = e?.response
    if (resp?.data) {
      error.value = `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}`
    } else {
      error.value = e?.message || '未知错误'
    }
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
    const res = await axios.post<ApiResponse<ChatResponse>>(`/api/docgen/jobs/${jobId.value}/chat`, { message })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '提交失败')
    }
    const data = res.data.data
    status.value = data.status
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
    if (resp?.data) {
      error.value = `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}`
    } else {
      error.value = e?.message || '未知错误'
    }
  } finally {
    loading.value = false
  }
}

async function onGeneratePrd() {
  error.value = ''
  loading.value = true
  try {
    const res = await axios.post<ApiResponse<ChatResponse>>(`/api/docgen/jobs/${jobId.value}/generate`)
    if (res.data.code !== 0) throw new Error(res.data.message || '生成失败')
    const data = res.data.data
    status.value = data.status
    chatHistory.value = data.chatHistory ?? []
    confirmedItems.value = data.confirmedItems ?? []
    unconfirmedItems.value = data.unconfirmedItems ?? []
    initStructuredInputs(unconfirmedItems.value)
    prdMarkdown.value = data.prdMarkdown ?? ''
    basePrdMarkdown.value = data.basePrdMarkdown ?? basePrdMarkdown.value
  } catch (e: any) {
    const resp = e?.response
    if (resp?.data) {
      error.value = `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}`
    } else {
      error.value = e?.message || '未知错误'
    }
  } finally {
    loading.value = false
  }
}

function onReset() {
  businessDescription.value = ''
  previousPrdMarkdown.value = ''
  jobId.value = ''
  status.value = ''
  clarifyQuestions.value = []
  chatHistory.value = []
  pendingQuestion.value = ''
  confirmedItems.value = []
  unconfirmedItems.value = []
  Object.keys(structuredInputs).forEach(k => delete structuredInputs[k])
  chatInput.value = ''
  prdMarkdown.value = ''
  basePrdMarkdown.value = ''
  error.value = ''
  localStorage.removeItem(DRAFT_KEY)
}

function initStructuredInputs(items: string[]) {
  // 保留已有输入，不在列表中的项清理掉
  const set = new Set(items)
  Object.keys(structuredInputs).forEach(k => {
    if (!set.has(k)) delete structuredInputs[k]
  })
  for (const it of items) {
    if (structuredInputs[it] === undefined) structuredInputs[it] = ''
  }
}

function buildGuidanceText(items: string[]): string {
  const lines: string[] = []
  for (const item of items) {
    if (item.includes('用户与角色')) {
      lines.push('【用户与角色】')
      lines.push('- 角色1：')
      lines.push('  - 职责：')
      lines.push('  - 关键操作：')
      lines.push('- 角色2：')
      lines.push('  - 职责：')
      lines.push('  - 关键操作：')
      lines.push('')
    } else if (item.includes('核心业务流程')) {
      lines.push('【核心业务流程】')
      lines.push('- 步骤1：输入来源是...')
      lines.push('- 步骤2：系统处理...')
      lines.push('- 步骤3：输出结果...')
      lines.push('')
    } else if (item.includes('输入输出与边界')) {
      lines.push('【输入输出与边界】')
      lines.push('- 输入：字段A/字段B（必填/可选）')
      lines.push('- 输出：页面展示/文件导出/API返回')
      lines.push('- 边界：不处理...')
      lines.push('')
    } else if (item.includes('验收标准')) {
      lines.push('【验收标准】')
      lines.push('- 功能验收：')
      lines.push('- 权限/安全验收：')
      lines.push('- 性能验收（如p95）：')
      lines.push('')
    } else {
      lines.push(`【${item}】`)
      lines.push('- 请补充具体内容...')
      lines.push('')
    }
  }
  return lines.join('\n').trim()
}

function useGuidanceTemplate() {
  chatInput.value = buildGuidanceText(unconfirmedItems.value)
}

function getPlaceholderForItem(item: string): string {
  if (item.includes('用户与角色')) {
    return '例如：管理员-配置模板与审批；产品经理-提交需求与修订；评审人-验收'
  }
  if (item.includes('核心业务流程')) {
    return '例如：步骤1录入需求 -> 步骤2AI分析提问 -> 步骤3用户补充 -> 步骤4生成PRD'
  }
  if (item.includes('输入输出与边界')) {
    return '例如：输入=业务描述/上一版PRD；输出=修订版PRD；边界=不实现具体业务代码'
  }
  if (item.includes('验收标准')) {
    return '例如：功能完整性、未确认项清零、生成耗时p95<120s、权限校验通过'
  }
  return '请补充该项的可执行信息'
}

async function onSendStructured() {
  const lines: string[] = []
  for (const item of unconfirmedItems.value) {
    const v = (structuredInputs[item] || '').trim()
    if (!v) {
      error.value = `请先填写：${item}`
      return
    }
    lines.push(`【${item}】`)
    lines.push(v)
    lines.push('')
  }
  chatInput.value = lines.join('\n').trim()
  await onSendChat()
}

async function onExportPrd() {
  if (!jobId.value) return
  try {
    const resp = await axios.get(`/api/docgen/jobs/${jobId.value}/export`, { responseType: 'blob' })
    const blob = new Blob([resp.data], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `PRD-${jobId.value}-revised.md`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e: any) {
    error.value = e?.message || '导出失败'
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
      structuredInputs: structuredInputs
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

.textarea {
  width: 100%;
  min-height: 140px;
  resize: vertical;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  font-size: 14px;
  line-height: 1.4;
}

.row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
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

.qblock {
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
}

.qhead {
  display: flex;
  gap: 10px;
  align-items: baseline;
}

.qid {
  min-width: 42px;
  font-weight: 700;
  color: #111827;
}

.qtitle {
  font-weight: 600;
}

.req {
  margin-left: 8px;
  font-weight: 500;
  font-size: 12px;
  color: #dc2626;
}

.qwhy {
  margin-top: 6px;
  color: #6b7280;
}

.label {
  font-weight: 600;
  color: #4b5563;
}

.input {
  width: 100%;
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  font-size: 14px;
}

.pre {
  white-space: pre-wrap;
  background: #0b1020;
  color: #e5e7eb;
  padding: 14px;
  border-radius: 10px;
  overflow: auto;
  max-height: 540px;
}

.diff-box {
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  border-radius: 8px;
  padding: 8px 10px;
  margin-bottom: 8px;
  color: #164e63;
}

.error {
  background: #fee2e2;
  border: 1px solid #fecaca;
  color: #991b1b;
  padding: 10px 12px;
  border-radius: 10px;
}

.chat-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  max-height: 360px;
  overflow: auto;
  margin-bottom: 12px;
}

.guide {
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 10px;
}

.form-box {
  background: #f8fafc;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 10px;
}

.form-item {
  margin-bottom: 10px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.guide-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.guide-pre {
  white-space: pre-wrap;
  background: #fffbeb;
  border: 1px dashed #f59e0b;
  border-radius: 8px;
  padding: 8px;
  max-height: 220px;
  overflow: auto;
  margin-bottom: 8px;
}

.checklists {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.list {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px 12px;
}

.list h3 {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.list ul {
  margin: 0;
  padding-left: 18px;
}

.muted {
  color: #6b7280;
}

.msg {
  display: flex;
  margin-bottom: 8px;
}

.msg.user {
  justify-content: flex-end;
}

.msg.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 80%;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 8px 10px;
}

.msg.user .bubble {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.role {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.details {
  margin-bottom: 10px;
}
</style>

