export type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

export type RequirementItem = {
  id: number
  requirementNo: string
  title: string
  summary: string
  priority: string
  status: string
}

export type RequirementVersionItem = {
  id: number
  requirementId: number
  versionNo: string
  prdMarkdown: string
  changeSummary: string
  sourceJobId: string
  generatedBy: number
  generatedAt: string
  isCurrent: boolean
}
