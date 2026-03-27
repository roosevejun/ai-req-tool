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
