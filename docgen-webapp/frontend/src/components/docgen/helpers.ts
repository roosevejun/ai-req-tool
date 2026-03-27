export function statusLabel(value?: string) {
  if (value === 'CREATED') return '已创建'
  if (value === 'ANALYZING') return '分析中'
  if (value === 'CLARIFYING') return '澄清中'
  if (value === 'READY_TO_GENERATE' || value === 'READY') return '可生成'
  if (value === 'COMPLETED') return '已完成'
  return value || '-'
}

export function buildGuidanceText(items: string[]): string {
  return items
    .map((item, index) => `${index + 1}. ${item}\n- 请补充你的业务理解、目标、限制条件和期望结果`)
    .join('\n\n')
}

export function getPlaceholderForItem(item: string) {
  return `请补充：${item}`
}
