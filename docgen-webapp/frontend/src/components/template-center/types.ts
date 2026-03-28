export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface TemplateItem {
  id: number
  templateCode: string
  templateName: string
  templateCategory: string
  description?: string | null
  scopeLevel: string
  status: string
  latestVersionNo?: number | null
  publishedVersionNo?: number | null
  createdAt?: string
  updatedAt?: string
}

export interface TemplateVersionItem {
  id: number
  templateId: number
  versionNo: number
  versionLabel?: string | null
  contentMarkdown?: string | null
  variablesJson?: string | null
  notes?: string | null
  status: string
  isPublished: boolean
  createdAt?: string
  updatedAt?: string
}

export interface TemplateDetail {
  template: TemplateItem
  versions: TemplateVersionItem[]
  publishedVersion?: TemplateVersionItem | null
}

export interface TemplateEditorForm {
  versionLabel: string
  contentMarkdown: string
  variablesJson: string
  notes: string
  status: string
}
