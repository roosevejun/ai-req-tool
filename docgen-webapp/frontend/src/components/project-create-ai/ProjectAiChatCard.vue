<template>
  <WorkspaceSection
    eyebrow="主工作区"
    title="AI 沟通输入与沟通历史"
    description="先在输入区说出你的项目想法，再根据沟通历史和 AI 反馈持续补齐项目框架。"
    tint
  >
    <template #actions>
      <StatusBadge v-if="sessionId" :label="`会话 #${sessionId}`" variant="neutral" small />
      <StatusBadge v-if="sessionId" :label="computedStatusText" variant="ai" small />
      <StatusBadge v-else label="第一句话将自动启动 AI 孵化" variant="warning" small />
    </template>

    <div class="workspace-grid">
      <section class="composer-panel">
        <div class="panel-head">
          <div>
            <p class="panel-eyebrow">沟通输入</p>
            <h4>告诉 AI 你现在的项目想法</h4>
          </div>
          <span class="panel-hint">第一句话会自动启动孵化</span>
        </div>

        <textarea
          v-model="chatMessage"
          class="input"
          :disabled="loading"
          placeholder="直接告诉 AI：你现在想做什么项目、想解决什么问题、面向谁、为什么要做。"
        />

        <div class="row actions-row">
          <button class="primary" type="button" :disabled="loading || !chatMessage.trim()" @click="$emit('send-message')">
            发送并继续提炼
          </button>
          <button class="ghost" type="button" :disabled="loading || !sessionId" @click="$emit('refresh-conversation')">刷新会话</button>
          <button class="ghost" type="button" :disabled="loading || !sessionId" @click="$emit('load-knowledge-preview')">查看检索上下文</button>
        </div>

        <div v-if="followUpQuestions.length > 0" class="followup-panel">
          <div class="panel-head compact">
            <div>
              <p class="panel-eyebrow">AI 反馈</p>
              <h4>当前需要确认的问题</h4>
            </div>
          </div>
          <div class="followup-list">
            <div v-for="(question, idx) in followUpQuestions" :key="`${question}-${idx}`" class="question-item">
              {{ idx + 1 }}. {{ question }}
            </div>
          </div>
        </div>
      </section>

      <section class="history-panel">
        <div class="panel-head">
          <div>
            <p class="panel-eyebrow">沟通历史</p>
            <h4>用户输入与 AI 反馈历史</h4>
          </div>
          <span class="panel-hint">{{ messages.length }} 条记录</span>
        </div>

        <div v-if="messages.length === 0" class="empty-state">
          直接说出你的项目想法、问题、目标用户或业务背景。你的第一句话会自动启动 AI 项目孵化会话。
        </div>
        <div v-else class="chat-list">
          <div v-for="message in messages" :key="`${message.id}-${message.seqNo}`" class="chat-item" :class="message.role">
            <div class="chat-meta">
              <span class="chat-role">{{ roleLabel(message.role, message.messageType) }}</span>
              <span class="chat-seq">第 {{ message.seqNo }} 轮</span>
            </div>
            <div class="chat-content">{{ message.content }}</div>
          </div>
        </div>
      </section>
    </div>

    <div v-if="knowledgePreviewVisible" class="knowledge-preview">
      <div class="section-head">
        <h4>本次检索上下文</h4>
        <span class="muted" v-if="knowledgePreviewQueryText">检索词：{{ knowledgePreviewQueryText }}</span>
      </div>
      <div v-if="knowledgePreviewLoading" class="empty-state preview-empty">正在加载检索上下文...</div>
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
.workspace-grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(0, 1.15fr);
  gap: 14px;
  align-items: start;
}

.composer-panel,
.history-panel,
.followup-panel {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  background: #fff;
  padding: 14px;
}

.composer-panel {
  display: grid;
  grid-template-rows: auto auto auto;
  gap: 12px;
}

.history-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 560px;
}

.panel-head,
.section-head,
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.panel-head.compact {
  margin-bottom: 4px;
}

.panel-eyebrow {
  margin: 0 0 4px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0f766e;
}

h4 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.panel-hint,
.muted,
.chat-seq {
  color: #64748b;
  font-size: 12px;
}

.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fff;
}

textarea.input {
  min-height: 180px;
  resize: vertical;
}

.actions-row {
  justify-content: flex-start;
}

.followup-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  min-height: 140px;
}

.followup-list {
  display: grid;
  gap: 8px;
  align-content: start;
  max-height: 100%;
  overflow: auto;
}

.chat-list {
  display: grid;
  gap: 10px;
  align-content: start;
  min-height: 0;
  max-height: 100%;
  overflow: auto;
}

.chat-item {
  border-radius: 12px;
  padding: 12px;
  border: 1px solid #dbe2ea;
  background: #fff;
}

.chat-item.user {
  background: #f6f9ff;
}

.chat-item.assistant {
  background: #f8fffb;
}

.chat-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.chat-role {
  font-size: 12px;
  color: #0f172a;
  text-transform: uppercase;
  font-weight: 700;
}

.chat-content,
.preview-item p {
  white-space: pre-wrap;
  margin: 0;
  color: #334155;
  line-height: 1.7;
}

.knowledge-preview {
  margin-top: 12px;
}

.preview-item,
.question-item {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 10px;
  background: #fafcff;
}

.wide {
  margin-top: 10px;
}

.primary,
.ghost {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.ghost {
  background: #f3f4f6;
}

.empty-state {
  color: #6b7280;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dbe2ea;
  border-radius: 12px;
  background: #fff;
  text-align: center;
  padding: 0 12px;
}

.preview-empty {
  min-height: 120px;
}

@media (max-width: 1024px) {
  .workspace-grid {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .composer-panel,
  .history-panel {
    min-height: auto;
  }

  .history-panel {
    min-height: 520px;
  }
}
</style>
