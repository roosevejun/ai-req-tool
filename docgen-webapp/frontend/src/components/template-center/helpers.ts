export function templateStatusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT': return '草稿'
    case 'PUBLISHED': return '已发布'
    case 'ARCHIVED': return '已归档'
    default: return status || '未知状态'
  }
}

export function templateStatusVariant(status?: string | null): 'default' | 'info' | 'success' | 'warning' | 'danger' | 'ai' {
  switch ((status || '').toUpperCase()) {
    case 'PUBLISHED': return 'success'
    case 'ARCHIVED': return 'warning'
    case 'DRAFT': return 'info'
    default: return 'default'
  }
}

export function scopeLevelLabel(scopeLevel?: string | null): string {
  switch ((scopeLevel || '').toUpperCase()) {
    case 'SYSTEM': return '系统级'
    case 'PROJECT': return '项目级'
    case 'ORG': return '组织级'
    default: return scopeLevel || '未设置'
  }
}
