export function templateStatusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT':
      return '草稿'
    case 'PUBLISHED':
      return '已发布'
    case 'ARCHIVED':
      return '已归档'
    default:
      return status || '未知状态'
  }
}

export function templateStatusVariant(status?: string | null): 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'ai' {
  switch ((status || '').toUpperCase()) {
    case 'PUBLISHED':
      return 'success'
    case 'ARCHIVED':
      return 'warning'
    case 'DRAFT':
      return 'info'
    default:
      return 'neutral'
  }
}

export function scopeLevelLabel(scopeLevel?: string | null): string {
  switch ((scopeLevel || '').toUpperCase()) {
    case 'SYSTEM':
      return '系统级'
    case 'PROJECT':
      return '项目级'
    case 'ORG':
      return '组织级'
    default:
      return scopeLevel || '未设置'
  }
}

export function templateUsageHint(item: {
  scopeLevel?: string | null
  publishedVersionNo?: number | null
  latestVersionNo?: number | null
}) {
  if ((item.publishedVersionNo || 0) > 0) {
    return `当前默认使用发布版 v${item.publishedVersionNo}`
  }
  if ((item.latestVersionNo || 0) > 0) {
    return `当前只有草稿版 v${item.latestVersionNo}，建议确认后发布`
  }
  return '当前还没有模板版本，建议先创建第一个版本'
}
