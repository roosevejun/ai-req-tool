import type {
  ChatMessageItem,
  KnowledgeDocumentDetail,
  KnowledgeDocumentListItem,
  SourceMaterial
} from './types'

export function previewText(text?: string) {
  if (!text) {
    return '-'
  }
  return text.length <= 120 ? text : `${text.slice(0, 120)}...`
}

export function roleLabel(role: string, messageType: string) {
  if (role === 'assistant' && messageType === 'question') return 'AI 追问'
  if (role === 'assistant') return 'AI'
  if (messageType === 'material') return '资料'
  return '用户'
}

export function knowledgeStatusText(value?: string) {
  if (value === 'READY' || value === 'SUCCESS') return '处理完成'
  if (value === 'FAILED') return '处理失败'
  if (value === 'PROCESSING' || value === 'RUNNING') return '处理中'
  if (value === 'PENDING') return '待处理'
  return value || '未知状态'
}

export function statusText(value: string) {
  if (value === 'READY_TO_CREATE') return '可以创建项目'
  if (value === 'COMPLETED') return '已完成'
  if (value === 'ACTIVE') return '会话进行中'
  return value || '草稿'
}

export function materialKnowledgeItems(
  material: SourceMaterial,
  materialKnowledgeMap: Record<number, KnowledgeDocumentListItem[]>
) {
  return material.id ? materialKnowledgeMap[material.id] || [] : []
}

export function latestQuestions(messages: ChatMessageItem[]) {
  return messages
    .filter((item) => item.role === 'assistant' && item.messageType === 'question')
    .slice(-3)
    .map((item) => item.content)
}

export function visibleChunks(detail: KnowledgeDocumentDetail | null, expanded: boolean) {
  if (!detail) {
    return []
  }
  return expanded ? detail.chunks : detail.chunks.slice(0, 3)
}
