<template>
  <div>
    <DocGenIntroCard
      v-model:business-description="businessDescription"
      v-model:previous-prd-markdown="previousPrdMarkdown"
      :loading="loading"
      @create-job="onCreateJob"
      @reset="onReset"
    />

    <DocGenSessionCard
      v-if="jobId"
      v-model:chat-input="chatInput"
      :job-id="jobId"
      :status="status"
      :current-version="currentVersion"
      :confirmed-items="confirmedItems"
      :unconfirmed-items="unconfirmedItems"
      :chat-history="chatHistory"
      :pending-question="pendingQuestion"
      :current-question-index="currentQuestionIndex"
      :current-question-item="currentQuestionItem"
      :structured-inputs="structuredInputs"
      :can-send-current="canSendCurrent"
      :can-send-structured="canSendStructured"
      :loading="loading"
      :knowledge-preview-loading="knowledgePreviewLoading"
      :knowledge-preview-visible="knowledgePreviewVisible"
      :knowledge-preview="knowledgePreview"
      :base-prd-markdown="basePrdMarkdown"
      :prd-markdown="prdMarkdown"
      :diff-summary="diffSummary"
      @use-guidance-template="useGuidanceTemplate"
      @prev-question="goPrevQuestion"
      @next-question="goNextQuestion"
      @send-current="onSendCurrent"
      @send-structured="onSendStructured"
      @send-chat="onSendChat"
      @generate-prd="onGeneratePrd"
      @load-knowledge-preview="onLoadKnowledgePreview"
      @export-prd="onExportPrd"
    />

    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import axios from 'axios'
import DocGenIntroCard from './docgen/DocGenIntroCard.vue'
import DocGenSessionCard from './docgen/DocGenSessionCard.vue'
import { buildGuidanceText } from './docgen/helpers'
import type {
  ApiResponse,
  ChatResponse,
  ChatMessage,
  ClarifyQuestion,
  CreateJobResponse,
  KnowledgePreviewResponse
} from './docgen/types'

const props = withDefaults(defineProps<{ apiBase?: string; draftKey?: string }>(), {
  apiBase: '/api/docgen',
  draftKey: 'docgen-draft-v1'
})

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
const knowledgePreviewLoading = ref(false)
const knowledgePreviewVisible = ref(false)
const knowledgePreview = ref<KnowledgePreviewResponse | null>(null)

const currentQuestionItem = computed(() => {
  if (unconfirmedItems.value.length === 0) return ''
  const index = Math.min(Math.max(currentQuestionIndex.value, 0), unconfirmedItems.value.length - 1)
  return unconfirmedItems.value[index] || ''
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

const draftKey = props.draftKey

async function onLoadKnowledgePreview() {
  error.value = ''
  knowledgePreviewLoading.value = true
  knowledgePreviewVisible.value = true
  try {
    const query = chatInput.value.trim()
    const res = await axios.get<ApiResponse<KnowledgePreviewResponse>>(`${props.apiBase}/knowledge-preview`, {
      params: query ? { query } : undefined
    })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '加载检索上下文失败。')
    }
    knowledgePreview.value = res.data.data
  } catch (e: any) {
    const resp = e?.response
    error.value = resp?.data
      ? `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}`
      : e?.message || '加载检索上下文失败。'
  } finally {
    knowledgePreviewLoading.value = false
  }
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
    knowledgePreview.value = null
    knowledgePreviewVisible.value = false
  } catch (e: any) {
    const resp = e?.response
    error.value = resp?.data
      ? `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}`
      : e?.message || '启动整理失败。'
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
    knowledgePreview.value = null
    knowledgePreviewVisible.value = false
  } catch (e: any) {
    const resp = e?.response
    error.value = resp?.data
      ? `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}`
      : e?.message || '发送对话失败。'
  } finally {
    loading.value = false
  }
}

async function onGeneratePrd() {
  error.value = ''
  loading.value = true
  try {
    const res = await axios.post<ApiResponse<ChatResponse>>(`${props.apiBase}/jobs/${jobId.value}/generate`)
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '生成 PRD 失败。')
    }
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
    error.value = resp?.data
      ? `HTTP ${resp.status}: ${typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data)}`
      : e?.message || '生成 PRD 失败。'
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
  Object.keys(structuredInputs).forEach((key) => delete structuredInputs[key])
  chatInput.value = ''
  prdMarkdown.value = ''
  basePrdMarkdown.value = ''
  error.value = ''
  knowledgePreview.value = null
  knowledgePreviewVisible.value = false
  localStorage.removeItem(draftKey)
}

function initStructuredInputs(items: string[]) {
  const set = new Set(items)
  Object.keys(structuredInputs).forEach((key) => {
    if (!set.has(key)) delete structuredInputs[key]
  })
  for (const item of items) {
    if (structuredInputs[item] === undefined) structuredInputs[item] = ''
  }
  if (items.length === 0) {
    currentQuestionIndex.value = 0
    return
  }
  if (currentQuestionIndex.value > items.length - 1) currentQuestionIndex.value = items.length - 1
  if (currentQuestionIndex.value < 0) currentQuestionIndex.value = 0
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

async function onSendStructured() {
  const lines: string[] = []
  for (const item of unconfirmedItems.value) {
    const value = (structuredInputs[item] || '').trim()
    if (!value) {
      error.value = `请先补充：${item}`
      return
    }
    lines.push(`${item}\n${value}`)
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
    const link = document.createElement('a')
    link.href = url
    const version = currentVersion.value > 0 ? currentVersion.value : 1
    link.download = `01-PRD-Agent需求文档-v${version}.md`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (e: any) {
    error.value = e?.message || '导出失败。'
  }
}

function loadDraft() {
  try {
    const raw = localStorage.getItem(draftKey)
    if (!raw) return
    const draft = JSON.parse(raw)
    businessDescription.value = draft.businessDescription || ''
    previousPrdMarkdown.value = draft.previousPrdMarkdown || ''
    chatInput.value = draft.chatInput || ''
    for (const [key, value] of Object.entries(draft.structuredInputs || {})) {
      structuredInputs[key] = String(value || '')
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
    localStorage.setItem(draftKey, JSON.stringify(data))
  },
  { deep: true }
)

loadDraft()
</script>

<style scoped>
.error {
  color: #b91c1c;
}
</style>
