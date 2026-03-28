<template>
  <WorkspaceSection
    eyebrow="主工作区"
    title="AI 对话与项目孵化"
    description="围绕你的项目想法持续对话。AI 会边追问、边整理、边校准理解，目标是提炼出一个值得保留的项目框架。"
    tint
  >
    <template #actions>
      <StatusBadge v-if="sessionId" :label="`会话 #${sessionId}`" variant="neutral" small />
      <StatusBadge v-if="sessionId" :label="computedStatusText" variant="ai" small />
      <StatusBadge v-else label="第一句话将自动启动 AI 孵化" variant="warning" small />
    </template>

    <div v-if="messages.length === 0" class="empty-state">
      直接说出你的项目想法、问题、目标用户或业务背景。你的第一句话会自动启动 AI 项目孵化会话。
    </div>
    <div v-else class="chat-list">
      <div v-for="message in messages" :key="`${message.id}-${message.seqNo}`" class="chat-item" :class="message.role">
        <div class="chat-role">{{ roleLabel(message.role, message.messageType) }}</div>
        <div class="chat-content">{{ message.content }}</div>
      </div>
    </div>

    <div v-if="followUpQuestions.length > 0" class="questions">
      <h4>AI 当前需要确认的问题</h4>
      <div v-for="(question, idx) in followUpQuestions" :key="`${question}-${idx}`" class="question-item">
        {{ idx + 1 }}. {{ question }}
      </div>
    </div>

    <textarea
      v-model="chatMessage"
      class="input"
      :disabled="loading"
      placeholder="直接告诉 AI：你现在想做什么项目、想解决什么问题、面向谁、为什么要做。"
    />
    <div class="row">
      <button class="primary" type="button" :disabled="loading || !chatMessage.trim()" @click="$emit('send-message')">
        发送并继续提炼
      </button>
      <button class="ghost" type="button" :disabled="loading || !sessionId" @click="$emit('refresh-conversation')">刷新会话</button>
      <button class="ghost" type="button" :disabled="loading || !sessionId" @click="$emit('load-knowledge-preview')">查看检索上下文</button>
    </div>

    <div v-if="knowledgePreviewVisible" class="knowledge-preview">
      <div class="section-head">
        <h4>本次检索上下文</h4>
        <span class="muted" v-if="knowledgePreviewQueryText">检索词：{{ knowledgePreviewQueryText }}</span>
      </div>
      <div v-if="knowledgePreviewLoading" class="empty-state">正在加载检索上下文...</div>
      <div v-else-if="knowledgePreview">
        <div class="preview-item wide">
          <strong>命中的片段</strong>
          <p v-if="knowledgePreview.items.length === 0">当前没有命中可用知识片段。</p>
          <p v-for="item in knowledgePreview.items" :key="`${item.documentId}-${item.chunkId}`">
            文档 #{{ item.documentId }} / Chunk #{{ item.chunkNo }} / 分数 {{ typeof item.score === 'number' ? item.score.toFixed(3) : '-' }}<br />
            {{ item.chunkText }}
          </p>
        </div>
        <div class="preview-item wide">
          <strong>拼接后的上下文</strong>
          <p>{{ knowledgePreview.contextText || '-' }}</p>
        </div>
      </div>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from '../projects/StatusBadge.vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'
import { roleLabel, statusText } from './helpers'
import type { ChatMessageItem, KnowledgePreview } from './types'

const chatMessage = defineModel<string>('chatMessage', { required: true })

const props = defineProps<{
  loading: boolean
  sessionId: number | null
  status: string
  messages: ChatMessageItem[]
  followUpQuestions: string[]
  knowledgePreviewVisible: boolean
  knowledgePreviewLoading: boolean
  knowledgePreview: KnowledgePreview | null
  knowledgePreviewQueryText: string
}>()

defineEmits<{
  (event: 'send-message'): void
  (event: 'refresh-conversation'): void
  (event: 'load-knowledge-preview'): void
}>()

const computedStatusText = computed(() => statusText(props.status))
</script>

<style scoped>
.input { width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 10px; padding: 10px 12px; background: #fff; }
textarea.input { min-height: 110px; resize: vertical; }
.knowledge-preview { margin-top: 12px; }
.section-head, .row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.section-head { justify-content: space-between; margin-bottom: 8px; }
.muted { color: #6b7280; }
.chat-list { display: grid; gap: 10px; max-height: 420px; overflow: auto; margin-bottom: 12px; }
.chat-item { border-radius: 12px; padding: 10px 12px; border: 1px solid #dbe2ea; }
.chat-item.user { background: #f6f9ff; }
.chat-item.assistant { background: #f8fffb; }
.chat-role { font-size: 12px; color: #6b7280; margin-bottom: 6px; text-transform: uppercase; }
.chat-content, .preview-item p { white-space: pre-wrap; margin: 0; }
.preview-item, .question-item { border: 1px solid #e5e7eb; border-radius: 12px; padding: 10px; background: #fafcff; }
.wide { margin-top: 10px; }
.questions { display: grid; gap: 8px; }
.primary, .ghost { border-radius: 10px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; border-color: #2563eb; color: #fff; }
.ghost { background: #f3f4f6; }
.empty-state { color: #6b7280; min-height: 120px; display: flex; align-items: center; justify-content: center; border: 1px dashed #dbe2ea; border-radius: 12px; background: #fff; }
</style>
