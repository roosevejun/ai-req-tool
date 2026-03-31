import type { ProjectItem } from './types'

type CompletenessField = {
  label: string
  read: (project: ProjectItem) => string | number | undefined
}

const COMPLETENESS_FIELDS: CompletenessField[] = [
  { label: '项目描述', read: (project) => project.description },
  { label: '项目背景', read: (project) => project.projectBackground },
  { label: '目标客户群体', read: (project) => project.targetCustomerGroups },
  { label: '商业价值', read: (project) => project.commercialValue },
  { label: '核心产品价值', read: (project) => project.coreProductValue },
  { label: '类似产品', read: (project) => project.similarProducts },
  { label: '项目类型', read: (project) => project.projectType },
  { label: '优先级', read: (project) => project.priority }
]

export type ProjectCompleteness = {
  score: number
  completedCount: number
  totalCount: number
  missingFieldLabels: string[]
}

export function getProjectCompleteness(project: ProjectItem | null | undefined): ProjectCompleteness {
  if (!project) {
    return {
      score: 0,
      completedCount: 0,
      totalCount: COMPLETENESS_FIELDS.length,
      missingFieldLabels: COMPLETENESS_FIELDS.map((field) => field.label)
    }
  }

  const completedCount = COMPLETENESS_FIELDS.filter((field) => {
    const value = field.read(project)
    return typeof value === 'number' ? value > 0 : !!value?.toString().trim()
  }).length

  return {
    score: Math.round((completedCount / COMPLETENESS_FIELDS.length) * 100),
    completedCount,
    totalCount: COMPLETENESS_FIELDS.length,
    missingFieldLabels: COMPLETENESS_FIELDS.filter((field) => {
      const value = field.read(project)
      return !(typeof value === 'number' ? value > 0 : !!value?.toString().trim())
    }).map((field) => field.label)
  }
}

export function getProjectHealthTone(score: number): 'danger' | 'warning' | 'success' {
  if (score < 45) return 'danger'
  if (score < 75) return 'warning'
  return 'success'
}
