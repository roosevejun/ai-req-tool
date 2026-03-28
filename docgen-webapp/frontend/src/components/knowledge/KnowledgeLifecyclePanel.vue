<template>
  <WorkspaceSection
    eyebrow="资料运营"
    title="知识生命周期"
    description="把上传、处理、失败和可检索状态放到一张卡里，帮助你判断当前资料是否真正进入了知识链路。"
    :tint="true"
  >
    <div class="lifecycle-grid">
      <article class="lifecycle-card">
        <StatusBadge :label="`${documentCount} 份文档`" variant="info" />
        <h4>已收录</h4>
        <p>当前范围内已经进入知识库索引的文档总数。</p>
      </article>
      <article class="lifecycle-card">
        <StatusBadge :label="`${readyCount} 份可检索`" variant="success" />
        <h4>已可用</h4>
        <p>这些资料已经处理完成，可以被 AI 检索和引用。</p>
      </article>
      <article class="lifecycle-card">
        <StatusBadge :label="`${pendingCount} 份处理中`" variant="warning" />
        <h4>处理中</h4>
        <p>这些资料已经进入队列，但还需要继续解析或生成索引。</p>
      </article>
      <article class="lifecycle-card">
        <StatusBadge :label="`${failedCount} 份失败`" variant="danger" />
        <h4>待修复</h4>
        <p>这些资料没有成功进入知识链路，建议优先查看失败原因并重试。</p>
      </article>
    </div>

    <div class="lifecycle-footer">
      <p class="next-title">当前建议</p>
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
  documentCount: number
  readyCount: number
  pendingCount: number
  failedCount: number
}>()

const nextActions = computed(() => {
  const actions: string[] = []
  if (props.documentCount === 0) {
    actions.push('当前范围还没有知识文档，建议先回到项目或 AI 创建页补充网址、文本或文件资料。')
  }
  if (props.failedCount > 0) {
    actions.push(`优先处理 ${props.failedCount} 份失败资料，避免知识检索缺口持续扩大。`)
  }
  if (props.pendingCount > 0) {
    actions.push(`继续关注 ${props.pendingCount} 份处理中资料，确认它们最终进入可检索状态。`)
  }
  if (props.documentCount > 0 && props.readyCount === 0 && props.failedCount === 0) {
    actions.push('当前资料已经进入队列，但还没有可检索结果，建议稍后刷新状态或进入详情查看处理进度。')
  }
  if (!actions.length) {
    actions.push('当前知识链路运行正常，可以继续从详情页查看资源、任务历史和检索上下文。')
  }
  return actions.slice(0, 3)
})
</script>

<style scoped>
.lifecycle-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.lifecycle-card {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.lifecycle-card h4 {
  margin: 12px 0 8px;
  color: #0f172a;
  font-size: 18px;
}

.lifecycle-card p {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.lifecycle-footer {
  margin-top: 16px;
  border-top: 1px solid #e2e8f0;
  padding-top: 16px;
}

.next-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

ul {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

@media (max-width: 980px) {
  .lifecycle-grid {
    grid-template-columns: 1fr;
  }
}
</style>
