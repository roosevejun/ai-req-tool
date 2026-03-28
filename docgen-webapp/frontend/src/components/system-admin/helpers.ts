export function statusLabel(value?: string) {
  if (value === 'ENABLED') return '启用'
  if (value === 'DISABLED') return '禁用'
  return value || '-'
}
