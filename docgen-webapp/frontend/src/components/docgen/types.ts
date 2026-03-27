export type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

export type ClarifyQuestion = {
  id: string
  question: string
  whyNeeded: string
  required: boolean
}

export type ChatMessage = {
  role: string
  content: string
}

export type CreateJobResponse = {
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

export type ChatResponse = {
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

export type RetrievalItem = {
  chunkId: number
  documentId: number
  chunkNo: number
  chunkText: string
  summary?: string
  score?: number
}

export type RetrievalContext = {
  query: string
  items: RetrievalItem[]
  contextText: string
}

export type KnowledgePreviewResponse = {
  query: string
  requirementKnowledge: RetrievalContext
  projectKnowledge: RetrievalContext
  mergedContext: string
}
