export type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

export type ProjectItem = {
  id: number
  projectKey: string
  projectName: string
  description?: string
  projectBackground?: string
  similarProducts?: string
  targetCustomerGroups?: string
  commercialValue?: string
  coreProductValue?: string
  projectType?: string
  priority?: string
  startDate?: string
  targetDate?: string
  tags?: string
  visibility: string
  status: string
  ownerUserId?: number
}

export type RequirementItem = {
  id: number
  requirementNo: string
  title: string
  summary: string
  priority: string
  status: string
}

export type VersionItem = {
  id: number
  versionNo: string
  isCurrent: boolean
}

export type UserOption = {
  id: number
  username: string
  displayName?: string
  status?: string
}

export type ProjectMemberItem = {
  id: number
  projectId: number
  userId: number
  username?: string
  displayName?: string
  projectRole: string
  status: string
}

export type ProjectProductGuideAnswer = {
  question: string
  answer: string
}

export type ProjectProductGuideResult = {
  assistantMessage: string
  followUpQuestions: string[]
  projectBackground: string
  similarProducts: string
  targetCustomerGroups: string
  commercialValue: string
  coreProductValue: string
}

export type ProjectFormState = {
  projectKey: string
  projectName: string
  description: string
  projectBackground: string
  similarProducts: string
  targetCustomerGroups: string
  commercialValue: string
  coreProductValue: string
  projectType: string
  priority: string
  startDate: string
  targetDate: string
  tags: string
  ownerUserId: string
  visibility: string
}

export type ProjectEditFormState = {
  projectName: string
  description: string
  projectBackground: string
  similarProducts: string
  targetCustomerGroups: string
  commercialValue: string
  coreProductValue: string
  projectType: string
  priority: string
  startDate: string
  targetDate: string
  tags: string
  ownerUserId: string
  visibility: string
  status: string
}

export type RequirementFormState = {
  title: string
  summary: string
  priority: string
  status: string
}

export type MemberFormState = {
  selectedUserId: string
  userId: string
  projectRole: string
}

export type ProjectAiSuggestions = {
  projectBackground: string
  similarProducts: string
  targetCustomerGroups: string
  commercialValue: string
  coreProductValue: string
}

export type ProjectConversationStructuredInfo = {
  projectName: string
  description: string
  projectBackground: string
  similarProducts: string
  targetCustomerGroups: string
  commercialValue: string
  coreProductValue: string
  businessKnowledgeSummary: string
}

export type ProjectConversationMessage = {
  id: number
  role: string
  messageType: string
  content: string
  seqNo: number
}

export type ProjectConversationView = {
  sessionId: number
  jobId: string
  status: string
  assistantSummary: string
  businessKnowledgeSummary: string
  structuredInfo: ProjectConversationStructuredInfo
  messages: ProjectConversationMessage[]
  materials: ProjectSourceMaterial[]
  readyToCreate: boolean
}

export type ProjectSourceMaterial = {
  id?: number
  materialType: string
  title?: string
  sourceUri?: string
  rawContent?: string
  aiExtractedSummary?: string
}

export type ProjectKnowledgeDocumentListItem = {
  id: number
  documentType: string
  title?: string
  status?: string
  latestTaskStatus?: string
  latestTaskError?: string
}

export type ProjectKnowledgeDocumentAsset = {
  id: number
  assetRole: string
  storageKey: string
  mimeType?: string
  sizeBytes?: number
}

export type ProjectKnowledgeDocumentChunk = {
  id: number
  chunkNo: number
  chunkText: string
}

export type ProjectKnowledgeDocumentTask = {
  id: number
  taskType: string
  status: string
  lastError?: string
}

export type ProjectKnowledgeDocumentEntity = {
  id: number
  documentType: string
  status: string
  title?: string
  summary?: string
}

export type ProjectKnowledgeDocumentDetail = {
  document: ProjectKnowledgeDocumentEntity
  assets: ProjectKnowledgeDocumentAsset[]
  chunks: ProjectKnowledgeDocumentChunk[]
  tasks: ProjectKnowledgeDocumentTask[]
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

export type ProjectRetrievalItem = {
  chunkId: number
  documentId: number
  chunkNo: number
  chunkText: string
  score?: number
}

export type ProjectKnowledgePreview = {
  query: string
  items: ProjectRetrievalItem[]
  contextText: string
}
