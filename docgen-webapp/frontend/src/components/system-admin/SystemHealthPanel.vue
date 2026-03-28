<template>
  <WorkspaceSection
    eyebrow="治理视角"
    title="系统健康"
    description="把系统配置治理、模板标准和 AI 工作台入口放在一起，帮助管理员判断平台当前处于什么状态。"
    :tint="true"
  >
    <div class="metrics">
      <div class="metric-card">
        <span>用户</span>
        <strong>{{ usersCount }}</strong>
      </div>
      <div class="metric-card">
        <span>角色</span>
        <strong>{{ rolesCount }}</strong>
      </div>
      <div class="metric-card">
        <span>权限</span>
        <strong>{{ permissionsCount }}</strong>
      </div>
      <div class="metric-card">
        <span>待处理表单</span>
        <strong>{{ pendingFormsCount }}</strong>
      </div>
    </div>

    <div class="status-row">
      <StatusBadge :label="usersCount > 0 ? '用户体系已建立' : '缺少用户配置'" :variant="usersCount > 0 ? 'success' : 'warning'" small />
      <StatusBadge :label="rolesCount > 0 ? '角色体系已建立' : '缺少角色配置'" :variant="rolesCount > 0 ? 'success' : 'warning'" small />
      <StatusBadge :label="permissionsCount > 0 ? '权限体系已建立' : '缺少权限配置'" :variant="permissionsCount > 0 ? 'success' : 'warning'" small />
    </div>

    <div class="actions-guide">
      <p class="guide-title">治理建议</p>
      <ul>
        <li v-for="item in nextActions" :key="item">{{ item }}</li>
      </ul>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from '../projects/StatusBadge.vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'

const props = defineProps<{
  usersCount: number
  rolesCount: number
  permissionsCount: number
  pendingFormsCount: number
}>()

const nextActions = computed(() => {
  const actions: string[] = []
  if (props.usersCount === 0) actions.push('先创建一个可登录的系统用户，保证后续权限和项目归属有落点。')
  if (props.rolesCount === 0) actions.push('先建立角色体系，避免用户权限完全靠人工约定。')
  if (props.permissionsCount === 0) actions.push('补充权限项，让模板中心、知识库和系统治理能力可控。')
  if (props.pendingFormsCount > 0) actions.push(`当前有 ${props.pendingFormsCount} 个待处理表单，建议先完成保存或绑定，避免上下文丢失。`)
  if (!actions.length) actions.push('当前基础治理链路已经具备，可以继续进入模板中心或业务工作台推进标准化建设。')
  return actions.slice(0, 3)
})
</script>

<style scoped>
.metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  border: 1px solid #dbe2ea;
  border-radius: 14px;
  padding: 12px;
  background: #fff;
}

.metric-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.metric-card strong {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  color: #0f172a;
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.actions-guide {
  margin-top: 16px;
  border-top: 1px solid #e2e8f0;
  padding-top: 16px;
}

.guide-title {
  margin: 0 0 10px;
  color: #0f172a;
  font-weight: 700;
  font-size: 14px;
}

ul {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}
</style>
