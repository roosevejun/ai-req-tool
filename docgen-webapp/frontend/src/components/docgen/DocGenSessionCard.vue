<template>
  <WorkspaceSection
    eyebrow="需求整理"
    title="AI 需求整理会话"
    description="围绕需求澄清、结构化补充和 PRD 生成，持续与 AI 协同完成整理。"
    tint
  >
    <template #actions>
      <StatusBadge :label="`任务 ${jobId}`" variant="neutral" small />
      <StatusBadge :label="statusLabel(status)" variant="ai" small />
      <StatusBadge :label="`版本 v${currentVersion || 0}`" variant="info" small />
      <StatusBadge
        v-if="templateSelection?.templateId"
        :label="`模板 ${templateSelection.templateVersionLabel || '已选择'}`"
        variant="success"
        small
      />
    </template>

    <DocGenProgressPanel
      :status="status"
      :confirmed-count="confirmedItems.length"
      :pending-count="unconfirmedItems.length"
      :has-template="!!templateSelection?.templateId"
    />

    <div class="snapshot-tip" v-if="templateSelection?.templateId">
      <p class="snapshot-title">当前模板快照正在生效</p>
      <p class="snapshot-copy">
        当前会话已经绑定模板
        <strong>{{ templateSelection.templateVersionLabel || '已选择版本' }}</strong>
        ，后续澄清、生成和导出都会继续基于这份模板快照执行。
      </p>
    </div>

    <div class="checklists">
      <div class="list">
        <h3>已确认信息</h3>
        <ul>
          <li v-for="(it, idx) in confirmedItems" :key="`confirmed-${idx}`">{{ it }}</li>
          <li v-if="confirmedItems.length === 0" class="muted">当前还没有已确认内容。</li>
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
      <div v-for="(message, idx) in chatHistory" :key="idx" class="msg" :class="message.role">
        <div class="bubble">
          <div class="role">{{ message.role === 'assistant' ? 'AI' : '用户' }}</div>
          <div>{{ message.content }}</div>
        </div>
      </div>
    </div>

    <div v-if="status !== 'COMPLETED'">
      <p v-if="pendingQuestion" class="hint"><b>当前问题：</b>{{ pendingQuestion }}</p>

      <div v-if="unconfirmedItems.length > 0" class="guide">
        <div class="guide-title">建议补充模板</div>
        <pre class="guide-pre">{{ buildGuidanceText(unconfirmedItems) }}</pre>
        <div class="row">
          <button class="ghost" :disabled="loading" @click="$emit('use-guidance-template')">填入引导模板</button>
        </div>
      </div>

      <div v-if="unconfirmedItems.length > 0" class="form-box">
        <div class="guide-title">结构化补充</div>
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
          <button class="ghost" :disabled="loading || currentQuestionIndex <= 0" @click="$emit('prev-question')">上一项</button>
          <button class="ghost" :disabled="loading || currentQuestionIndex >= unconfirmedItems.length - 1" @click="$emit('next-question')">下一项</button>
          <button class="primary" :disabled="loading || !canSendCurrent" @click="$emit('send-current')">
            {{ loading ? '发送中...' : '提交当前项' }}
          </button>
          <button class="ghost" :disabled="loading || !canSendStructured" @click="$emit('send-structured')">
            {{ loading ? '发送中...' : '提交全部补充' }}
          </button>
        </div>
      </div>

      <input v-model="chatInput" class="input" placeholder="继续补充信息，或让 AI 帮你澄清当前需求" />
      <div class="row">
        <button class="primary" :disabled="loading || !chatInput.trim()" @click="$emit('send-chat')">
          {{ loading ? '发送中...' : '发送给 AI' }}
        </button>
        <button class="ghost" :disabled="loading || unconfirmedItems.length > 0" @click="$emit('generate-prd')">
          {{ loading ? '生成中...' : '生成 PRD' }}
        </button>
        <button class="ghost" :disabled="loading" @click="$emit('load-knowledge-preview')">查看检索上下文</button>
      </div>

      <div v-if="knowledgePreviewVisible" class="knowledge-preview">
        <div class="guide-title">检索上下文预览</div>
        <div v-if="knowledgePreviewLoading" class="muted">正在加载检索上下文...</div>
        <template v-else-if="knowledgePreview">
          <div class="diff-box">
            <b>检索词</b>
            <div>{{ knowledgePreview.query || '-' }}</div>
          </div>

          <div class="checklists">
            <div class="list">
              <h3>需求知识命中</h3>
              <ul>
                <li v-for="item in knowledgePreview.requirementKnowledge.items" :key="`rq-${item.chunkId}`">
                  文档 #{{ item.documentId }} / Chunk #{{ item.chunkNo }} / 分数 {{ typeof item.score === 'number' ? item.score.toFixed(3) : '-' }}
                </li>
                <li v-if="knowledgePreview.requirementKnowledge.items.length === 0" class="muted">暂无命中。</li>
              </ul>
            </div>
            <div class="list">
              <h3>项目知识命中</h3>
              <ul>
                <li v-for="item in knowledgePreview.projectKnowledge.items" :key="`pj-${item.chunkId}`">
                  文档 #{{ item.documentId }} / Chunk #{{ item.chunkNo }} / 分数 {{ typeof item.score === 'number' ? item.score.toFixed(3) : '-' }}
                </li>
                <li v-if="knowledgePreview.projectKnowledge.items.length === 0" class="muted">暂无命中。</li>
              </ul>
            </div>
          </div>

          <pre class="guide-pre">{{ knowledgePreview.mergedContext || '-' }}</pre>
        </template>
      </div>
    </div>

    <div v-if="status === 'COMPLETED'">
      <h3>PRD 结果</h3>
      <div class="diff-box" v-if="basePrdMarkdown">
        <b>与基线版本对比</b>
        <div>新增 {{ diffSummary.added }} 处，删除 {{ diffSummary.removed }} 处</div>
      </div>
      <div class="row">
        <button class="ghost" @click="$emit('export-prd')">导出 Markdown</button>
        <button class="ghost" :disabled="loading" @click="$emit('load-knowledge-preview')">查看检索上下文</button>
      </div>
      <pre class="pre">{{ prdMarkdown }}</pre>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import DocGenProgressPanel from './DocGenProgressPanel.vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'
import StatusBadge from '../projects/StatusBadge.vue'
import { buildGuidanceText, getPlaceholderForItem, statusLabel } from './helpers'
import type { ChatMessage, KnowledgePreviewResponse } from './types'

const chatInput = defineModel<string>('chatInput', { required: true })

defineProps<{
  jobId: string
  status: string
  currentVersion: number
  confirmedItems: string[]
  unconfirmedItems: string[]
  chatHistory: ChatMessage[]
  pendingQuestion: string
  currentQuestionIndex: number
  currentQuestionItem: string
  structuredInputs: Record<string, string>
  canSendCurrent: boolean
  canSendStructured: boolean
  loading: boolean
  knowledgePreviewLoading: boolean
  knowledgePreviewVisible: boolean
  knowledgePreview: KnowledgePreviewResponse | null
  basePrdMarkdown: string
  prdMarkdown: string
  diffSummary: { added: number; removed: number }
  templateSelection?: { templateId?: number | null; templateVersionId?: number | null; templateVersionLabel?: string | null } | null
}>()

defineEmits<{
  (event: 'use-guidance-template'): void
  (event: 'prev-question'): void
  (event: 'next-question'): void
  (event: 'send-current'): void
  (event: 'send-structured'): void
  (event: 'send-chat'): void
  (event: 'generate-prd'): void
  (event: 'load-knowledge-preview'): void
  (event: 'export-prd'): void
}>()
</script>

<style scoped>
.hint, .muted { color: #6b7280; }
.snapshot-tip { border: 1px solid #bbf7d0; border-radius: 14px; padding: 12px; background: linear-gradient(180deg, #f0fdf4 0%, #ffffff 100%); margin-bottom: 12px; }
.snapshot-title { margin: 0 0 6px; color: #166534; font-weight: 700; }
.snapshot-copy { margin: 0; color: #475569; line-height: 1.7; }
.checklists { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-bottom: 12px; }
.list { border: 1px solid #e5e7eb; border-radius: 12px; padding: 12px; background: #fafafa; }
.list h3 { margin-top: 0; margin-bottom: 8px; font-size: 15px; }
.list ul { margin: 0; padding-left: 18px; }
.chat-box { display: grid; gap: 10px; margin-bottom: 12px; max-height: 420px; overflow: auto; }
.msg { display: flex; }
.msg.assistant { justify-content: flex-start; }
.msg.user { justify-content: flex-end; }
.bubble { max-width: 86%; padding: 10px 12px; border-radius: 12px; background: #f3f4f6; white-space: pre-wrap; }
.msg.assistant .bubble { background: #eff6ff; }
.role { font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.guide, .form-box, .diff-box, .knowledge-preview { border: 1px dashed #cbd5e1; border-radius: 12px; padding: 12px; margin-bottom: 12px; background: #fcfcfd; }
.guide-title { font-weight: 600; margin-bottom: 8px; }
.guide-pre, .pre { white-space: pre-wrap; background: #0f172a; color: #e2e8f0; padding: 12px; border-radius: 10px; overflow: auto; }
.qa-progress { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.form-label { display: block; font-weight: 600; margin-bottom: 6px; }
.textarea { width: 100%; min-height: 140px; resize: vertical; padding: 10px; box-sizing: border-box; border-radius: 10px; border: 1px solid #e5e7eb; font-size: 14px; line-height: 1.4; }
.input { width: 100%; padding: 10px; box-sizing: border-box; border-radius: 10px; border: 1px solid #e5e7eb; }
.row { display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap; }
button { border-radius: 10px; padding: 10px 14px; border: 1px solid transparent; cursor: pointer; font-size: 14px; }
.primary { background: #2563eb; color: #fff; }
.ghost { background: #f3f4f6; color: #111827; border-color: #e5e7eb; }
@media (max-width: 900px) { .checklists { grid-template-columns: 1fr; } }
</style>
