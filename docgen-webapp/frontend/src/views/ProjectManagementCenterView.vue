<template>
  <div class="page">
    <CenterHero
      eyebrow="Project Management Center"
      title="项目管理中心"
      summary="围绕项目创建、项目孵化与项目办理建立统一入口。先明确从哪里开始，再进入对应业务页面推进。"
    >
      <template #badges>
        <StatusBadge :label="`${metrics.projectCount} 个项目`" variant="info" />
        <StatusBadge :label="`${metrics.activeCount} 个进行中`" variant="success" />
        <StatusBadge :label="`${metrics.needsAiCount} 个待补框架`" variant="warning" />
      </template>
      <template #actions>
        <button class="ghost" type="button" @click="goManage">进入项目管理页</button>
        <button class="primary" type="button" @click="goCreate">快速创建项目</button>
      </template>
    </CenterHero>

    <section class="overview-strip">
      <article class="metric-card">
        <span>项目总数</span>
        <strong>{{ metrics.projectCount }}</strong>
      </article>
      <article class="metric-card">
        <span>进行中项目</span>
        <strong>{{ metrics.activeCount }}</strong>
      </article>
      <article class="metric-card">
        <span>待补框架项目</span>
        <strong>{{ metrics.needsAiCount }}</strong>
      </article>
      <article class="metric-card">
        <span>已归档项目</span>
        <strong>{{ metrics.archivedCount }}</strong>
      </article>
    </section>

    <section class="entry-layout">
      <div class="entry-main">
        <WorkspaceSection
          eyebrow="Business Entry 01"
          title="传统创建项目"
          description="适合项目信息已经比较明确的场景，直接录入项目名称、背景、客户群体与价值信息，快速建立项目档案。"
          :tint="true"
        >
          <ul class="entry-list">
            <li>直接建立正式项目并进入项目管理页。</li>
            <li>后续仍可进入 AI 协同继续补充和校准项目框架。</li>
            <li>适合已有明确立项结论的项目。</li>
          </ul>
          <button class="primary full" type="button" @click="goForm">进入传统创建</button>
        </WorkspaceSection>

        <WorkspaceSection
          eyebrow="Business Entry 02"
          title="AI 项目孵化"
          description="适合想法仍在梳理、需要边沟通边提炼项目框架的场景。先对话，再判断当前框架是否值得保留或正式立项。"
        >
          <ul class="entry-list">
            <li>第一句话即可启动 AI 孵化，不需要先建立正式项目。</li>
            <li>支持补资料、持续追问与保存当前项目框架。</li>
            <li>适合新想法探索与前期立项判断。</li>
          </ul>
          <button class="ai-button full" type="button" @click="goAi">进入 AI 项目孵化</button>
        </WorkspaceSection>
      </div>

      <aside class="entry-side">
        <WorkspaceSection
          eyebrow="Management Entry"
          title="进入项目管理"
          description="已有项目时，直接进入项目管理页查看项目列表、项目详情、AI 协同与需求入口。"
        >
          <button class="ghost full" type="button" @click="goManage">进入项目管理页</button>
        </WorkspaceSection>

        <WorkspaceSection
          eyebrow="Action Advice"
          title="办理建议"
          description="根据当前项目准备程度，选择最顺手的进入路径。"
          :tint="true"
        >
          <ul class="advice-list">
            <li>信息已经明确：优先走传统创建。</li>
            <li>还在梳理想法：优先走 AI 项目孵化。</li>
            <li>已有项目继续推进：直接进入项目管理页。</li>
          </ul>
        </WorkspaceSection>
      </aside>
    </section>

    <div class="feedback-stack">
      <FeedbackPanel
        title="使用说明"
        tone="warning"
        message="项目管理中心首页只负责业务分流与入口引导。进入具体业务后，再完成项目创建、项目办理与协同维护。"
      />
      <FeedbackPanel title="处理提示" tone="danger" :message="error" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import StatusBadge from '../components/projects/StatusBadge.vue'
import WorkspaceSection from '../components/projects/WorkspaceSection.vue'
import CenterHero from '../components/shell/CenterHero.vue'

type ProjectListItem = {
  id: number
  status?: string
  description?: string
}

const router = useRouter()
const error = ref('')
const metrics = reactive({
  projectCount: 0,
  activeCount: 0,
  needsAiCount: 0,
  archivedCount: 0
})

function goCreate() {
  void router.push('/projects/create')
}

function goForm() {
  void router.push('/projects/create/form')
}

function goAi() {
  void router.push('/projects/create-ai')
}

function goManage() {
  void router.push('/projects/manage')
}

async function loadMetrics() {
  error.value = ''
  try {
    const res = await axios.get('/api/projects')
    const projects = (res.data?.data || []) as ProjectListItem[]
    metrics.projectCount = projects.length
    metrics.activeCount = projects.filter((item) => item.status === 'ACTIVE').length
    metrics.archivedCount = projects.filter((item) => item.status === 'ARCHIVED').length
    metrics.needsAiCount = projects.filter((item) => !item.description || !item.description.trim()).length
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载项目管理中心概览失败。'
  }
}

onMounted(() => {
  void loadMetrics()
})
</script>

<style scoped>
.page {
  max-width: 1480px;
  margin: 18px auto;
  padding: 0 14px 18px;
  display: grid;
  gap: 16px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #0f172a;
}
.overview-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.metric-card {
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid #dbe2ea;
  background: #fff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.05);
}
.metric-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}
.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: #0f172a;
}
.entry-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 360px;
  gap: 16px;
}
.entry-main {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.entry-side,
.feedback-stack {
  display: grid;
  gap: 16px;
}
.entry-list,
.advice-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}
.primary,
.ghost,
.ai-button {
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  padding: 11px 16px;
  cursor: pointer;
  font-weight: 700;
}
.primary {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #fff;
}
.ghost {
  background: #fff;
  color: #0f172a;
}
.ai-button {
  background: #0f766e;
  border-color: #0f766e;
  color: #fff;
}
.full {
  width: 100%;
}
@media (max-width: 1180px) {
  .overview-strip,
  .entry-layout,
  .entry-main {
    grid-template-columns: 1fr;
  }
}
</style>
