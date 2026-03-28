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

export function templateReasonText(hasTemplate: boolean, hasVersion: boolean) {
  if (!hasTemplate) {
    return '当前会优先使用系统默认模板，适合先快速启动需求整理。'
  }
  if (!hasVersion) {
    return '当前已选择模板，但未指定版本，系统会优先采用该模板的已发布版本。'
  }
  return '当前已明确模板和版本，后续澄清、生成和导出都会基于这份模板快照执行。'
}
