export function priorityLabel(value?: string) {
  if (value === 'P0') return 'P0 - 紧急'
  if (value === 'P1') return 'P1 - 高'
  if (value === 'P2') return 'P2 - 中'
  if (value === 'P3') return 'P3 - 低'
  return value || '-'
}

export function requirementStatusLabel(value?: string) {
  if (value === 'DRAFT') return '草稿'
  if (value === 'CLARIFYING') return '澄清中'
  if (value === 'READY_REVIEW') return '待评审'
  if (value === 'DONE') return '已完成'
  return value || '-'
}
