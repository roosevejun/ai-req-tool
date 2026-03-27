export function projectTypeLabel(value?: string) {
  if (value === 'PRODUCT') return '产品型'
  if (value === 'PLATFORM') return '平台型'
  if (value === 'OPS') return '运维型'
  if (value === 'INTEGRATION') return '集成型'
  return value || '-'
}

export function visibilityLabel(value?: string) {
  if (value === 'PRIVATE') return '私有'
  if (value === 'ORG') return '组织内'
  return value || '-'
}

export function projectStatusLabel(value?: string) {
  if (value === 'ACTIVE') return '进行中'
  if (value === 'ARCHIVED') return '已归档'
  if (value === 'PAUSED') return '已暂停'
  return value || '-'
}

export function projectRoleLabel(value?: string) {
  if (value === 'OWNER') return '负责人'
  if (value === 'PM') return '产品经理'
  if (value === 'DEV') return '开发'
  if (value === 'QA') return '测试'
  if (value === 'VIEWER') return '只读'
  return value || '-'
}

export function memberStatusLabel(value?: string) {
  if (value === 'ACTIVE') return '正常'
  if (value === 'DISABLED') return '已禁用'
  return value || '-'
}

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
