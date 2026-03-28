<template>
  <div class="page">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">项目管理中心</p>
        <h1>项目管理业务入口</h1>
        <p class="summary">
          面向项目创建、项目孵化与项目办理的统一入口。先选择业务入口，再进入对应业务页面处理项目事项。
        </p>
      </div>

      <div class="hero-actions">
        <button class="secondary" type="button" @click="goManage">进入项目管理页</button>
        <button class="primary" type="button" @click="goCreate">快速创建项目</button>
      </div>
    </section>

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
        <article class="entry-card entry-card--primary">
          <p class="entry-eyebrow">业务入口一</p>
          <h2>传统创建项目</h2>
          <p>
            适合项目信息已经明确的场景，直接录入项目名称、背景、客户群体、商业价值等核心信息，快速建立项目档案。
          </p>
          <ul>
            <li>直接创建项目并进入项目管理页</li>
            <li>后续仍可进入 AI 协同优化项目框架</li>
            <li>适合已有明确立项结论的项目</li>
          </ul>
          <button class="primary" type="button" @click="goForm">进入传统创建</button>
        </article>

        <article class="entry-card entry-card--ai">
          <p class="entry-eyebrow">业务入口二</p>
          <h2>AI 项目孵化</h2>
          <p>
            适合想法还不完整、需要一边沟通一边校准的场景。先和 AI 沟通想法，提炼当前项目框架，再决定是否正式立项。
          </p>
          <ul>
            <li>第一句话即可启动 AI 孵化</li>
            <li>支持补充资料、校准理解、保留当前框架</li>
            <li>适合新想法探索和前期立项评估</li>
          </ul>
          <button class="ai-button" type="button" @click="goAi">进入 AI 项目孵化</button>
        </article>
      </div>

      <aside class="entry-side">
        <section class="panel">
          <h3>项目办理入口</h3>
          <p>已有项目时，直接进入项目管理页查看项目列表、项目详情、AI 协同和需求事项。</p>
          <button class="secondary full" type="button" @click="goManage">进入项目管理页</button>
        </section>

        <section class="panel">
          <h3>办理建议</h3>
          <ul class="tips">
            <li>信息已经明确：优先走传统创建。</li>
            <li>还在梳理想法：优先走 AI 项目孵化。</li>
            <li>已有项目要继续维护：直接进入项目管理页。</li>
          </ul>
        </section>
      </aside>
    </section>

    <div class="feedback-stack">
      <FeedbackPanel
        title="使用说明"
        tone="warning"
        message="项目管理中心首页只负责业务分流与入口引导。进入具体业务后，再在对应页面完成项目创建、项目办理和协同维护。"
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
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #111827;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) auto;
  gap: 16px;
  align-items: start;
  padding: 20px 22px;
  background: #ffffff;
  border: 1px solid #d4dde8;
  border-top: 4px solid #1d4ed8;
  border-radius: 10px;
}

.eyebrow,
.entry-eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1d4ed8;
  font-weight: 700;
}

h1 {
  margin: 0;
  font-size: 34px;
  color: #0f172a;
}

.summary {
  margin: 10px 0 0;
  max-width: 760px;
  color: #475569;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.metric-card,
.entry-card,
.panel {
  background: #ffffff;
  border: 1px solid #d4dde8;
  border-radius: 10px;
}

.metric-card {
  padding: 14px 16px;
}

.metric-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.metric-card strong {
  display: block;
  margin-top: 6px;
  font-size: 26px;
  color: #0f172a;
}

.entry-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) 340px;
  gap: 16px;
  margin-top: 14px;
}

.entry-main {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.entry-card {
  padding: 20px;
}

.entry-card--primary {
  border-top: 4px solid #1d4ed8;
}

.entry-card--ai {
  border-top: 4px solid #0f766e;
}

h2,
h3 {
  margin: 0;
  color: #0f172a;
}

.entry-card p,
.panel p {
  color: #475569;
  line-height: 1.7;
}

.entry-card ul,
.tips {
  margin: 16px 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.8;
}

.entry-side {
  display: grid;
  gap: 16px;
}

.panel {
  padding: 18px;
}

.primary,
.secondary,
.ai-button {
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  padding: 10px 16px;
  cursor: pointer;
  font-weight: 600;
}

.primary {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
}

.secondary {
  background: #ffffff;
  color: #0f172a;
}

.ai-button {
  background: #0f766e;
  border-color: #0f766e;
  color: #ffffff;
}

.full {
  width: 100%;
}

.feedback-stack {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

@media (max-width: 1180px) {
  .hero,
  .entry-layout {
    grid-template-columns: 1fr;
  }

  .overview-strip,
  .entry-main {
    grid-template-columns: 1fr 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .overview-strip,
  .entry-main {
    grid-template-columns: 1fr;
  }
}
</style>
