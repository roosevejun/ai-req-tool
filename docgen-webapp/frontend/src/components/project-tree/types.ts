export type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

export type ProjectTreeProjectItem = {
  id: number
  projectKey: string
  projectName: string
}

export type ProjectTreeRequirementItem = {
  id: number
  title: string
}

export type ProjectTreeVersionItem = {
  id: number
  versionNo: string
  isCurrent: boolean
}
