export type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

export type SourceMaterial = {
  id?: number
  materialType: string
  title?: string
  sourceUri?: string
  rawContent?: string
  aiExtractedSummary?: string
}

export type RetrievalItem = {
  chunkId: number
  documentId: number
  chunkNo: number
  chunkText: string
  score?: number
}

export type KnowledgePreview = {
  query: string
  items: RetrievalItem[]
  contextText: string
}

export type KnowledgeDocumentListItem = {
  id: number
  projectId?: number
  requirementId?: number
  sourceMaterialId?: number
  documentType: string
  sourceUri?: string
  title?: string
  status: string
  summary?: string
  latestTaskStatus?: string
  latestTaskError?: string
}

export type KnowledgeDocumentAsset = {
  id: number
  assetRole: string
  storageKey: string
  mimeType?: string
  sizeBytes?: number
}

export type KnowledgeDocumentChunk = {
  id: number
  chunkNo: number
  chunkText: string
}

export type KnowledgeDocumentTask = {
  id: number
  taskType: string
  status: string
  lastError?: string
}

export type KnowledgeDocumentEntity = {
  id: number
  projectId?: number
  requirementId?: number
  sourceMaterialId?: number
  documentType: string
  status: string
  title?: string
  summary?: string
}

export type KnowledgeDocumentDetail = {
  document: KnowledgeDocumentEntity
  assets: KnowledgeDocumentAsset[]
  chunks: KnowledgeDocumentChunk[]
  tasks: KnowledgeDocumentTask[]
}

export type ChatMessageItem = {
  id: number
  role: string
  messageType: string
  content: string
  seqNo: number
}

export type StructuredInfo = {
  projectName: string
  description: string
  projectBackground: string
  similarProducts: string
  targetCustomerGroups: string
  commercialValue: string
  coreProductValue: string
  businessKnowledgeSummary: string
}

export type ConversationTurnResult = {
  sessionId: number
  jobId: string
  assistantMessage: string
  followUpQuestions: string[]
  structuredInfo: StructuredInfo
  readyToCreate: boolean
}

export type ConversationView = {
  sessionId: number
  jobId: string
  status: string
  structuredInfo: StructuredInfo
  messages: ChatMessageItem[]
  materials: SourceMaterial[]
  readyToCreate: boolean
}

export type UploadFileMaterialResponse = {
  sessionId: number
  materialCount: number
}

export type StartFormState = {
  projectName: string
  description: string
}

export type CreateFormState = {
  projectKey: string
  projectName: string
  projectType: string
  priority: string
  visibility: string
  ownerUserId: string
}

export type UrlDraftState = {
  title: string
  sourceUri: string
}

export type TextDraftState = {
  title: string
  rawContent: string
}

export type FileDraftState = {
  title: string
}
