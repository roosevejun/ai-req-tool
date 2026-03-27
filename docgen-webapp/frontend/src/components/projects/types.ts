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
