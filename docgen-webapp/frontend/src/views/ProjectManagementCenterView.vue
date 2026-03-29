<template>
  <div class="page">
    <section class="hero">
      <div class="hero-main">
        <p class="hero-eyebrow">项目创建入口</p>
        <h1 class="hero-title">请选择项目创建方式</h1>
        <p class="hero-description">
          当前页面只保留项目创建相关入口。信息已经明确时，直接进入传统创建；如果想法仍在梳理，建议先进入 AI
          项目孵化，让系统边沟通边提炼项目框架。
        </p>

        <div class="hero-notes">
          <div class="hero-note">
            <span class="hero-note-label">适合传统创建</span>
            <p>项目名称、背景、客户与价值信息已经比较完整，希望尽快形成正式项目档案。</p>
          </div>
          <div class="hero-note hero-note--ai">
            <span class="hero-note-label">适合 AI 孵化</span>
            <p>想法还在梳理，希望先通过持续对话提炼框架，再决定是否正式立项。</p>
          </div>
        </div>
      </div>

      <aside class="hero-side">
        <div class="overview-card">
          <p class="overview-eyebrow">当前概览</p>
          <h2 class="overview-title">项目准备度</h2>
          <div class="metric-list">
            <div class="metric-row">
              <span class="metric-label">项目总数</span>
              <strong class="metric-value">{{ metrics.projectCount }}</strong>
            </div>
            <div class="metric-row">
              <span class="metric-label">进行中</span>
              <strong class="metric-value">{{ metrics.activeCount }}</strong>
            </div>
            <div class="metric-row">
              <span class="metric-label">待补框架</span>
              <strong class="metric-value">{{ metrics.needsAiCount }}</strong>
            </div>
          </div>
          <p class="overview-tip">先选创建路径，再进入后续项目管理与协同维护。</p>
        </div>
      </aside>
    </section>

    <section class="entry-grid">
      <WorkspaceSection
        eyebrow="入口一"
        title="传统创建项目"
        description="适合项目信息已经明确，需要快速建立正式项目档案。"
        :tint="true"
      >
        <ul class="entry-list">
          <li>录入项目名称、背景、客户与价值信息。</li>
          <li>创建后直接进入项目管理页。</li>
          <li>适合已有清晰立项信息的场景。</li>
        </ul>

        <div class="entry-footer">
          <div class="entry-scene">
            <span class="entry-scene-label">办理指引</span>
            <p>信息已经明确时，优先进入传统创建。</p>
          </div>
          <button class="primary full" type="button" @click="goForm">进入传统创建</button>
        </div>
      </WorkspaceSection>

      <WorkspaceSection
        eyebrow="入口二"
        title="AI 项目孵化"
        description="适合边沟通边提炼项目框架，先形成框架再判断是否立项。"
      >
        <ul class="entry-list">
          <li>第一句话即可启动 AI 项目孵化。</li>
          <li>支持持续追问、补资料与保留框架。</li>
          <li>适合想法尚未完全收敛的场景。</li>
        </ul>

        <div class="entry-footer">
          <div class="entry-scene entry-scene--ai">
            <span class="entry-scene-label">AI 适用场景</span>
            <p>想法仍在梳理时，优先进入 AI 项目孵化。</p>
          </div>
          <button class="ai-button full" type="button" @click="goAi">进入 AI 项目孵化</button>
        </div>
      </WorkspaceSection>
    </section>

    <div v-if="error" class="feedback-stack">
      <FeedbackPanel title="处理提示" tone="danger" :message="error" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import WorkspaceSection from '../components/projects/WorkspaceSection.vue'
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
  needsAiCount: 0
})

function goForm() {
  void router.push('/projects/create/form')
}

function goAi() {
  void router.push('/projects/create-ai')
}

async function loadMetrics() {
  error.value = ''
  try {
    const res = await axios.get('/api/projects')
    const projects = (res.data?.data || []) as ProjectListItem[]
    metrics.projectCount = projects.length
    metrics.activeCount = projects.filter((item) => item.status === 'ACTIVE').length
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
  max-width: 1380px;
  box-sizing: border-box;
  margin: 0 auto;
  height: 100%;
  padding: 4px 18px 8px;
  display: grid;
  align-content: start;
  gap: 16px;
  color: #0f172a;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 320px;
  gap: 18px;
  align-items: stretch;
}

.hero-main {
  display: grid;
  gap: 10px;
  padding: 10px 4px 0;
}

.hero-eyebrow,
.overview-eyebrow,
.entry-scene-label {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  font-size: 42px;
  line-height: 1.06;
  color: #0f172a;
}

.hero-description {
  margin: 0;
  max-width: 880px;
  color: #475569;
  font-size: 17px;
  line-height: 1.7;
}

.hero-notes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 2px;
}

.hero-note,
.overview-card,
.entry-scene {
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 28px rgba(148, 163, 184, 0.08);
}

.hero-note {
  padding: 14px 16px;
}

.hero-note--ai {
  background: linear-gradient(180deg, #f6fffd 0%, #eefaf8 100%);
  border-color: #cfe8e3;
}

.hero-note-label {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef4ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.hero-note p {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.6;
}

.overview-card {
  height: 100%;
  padding: 18px 18px 16px;
  display: grid;
  align-content: start;
  gap: 12px;
}

.overview-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.15;
}

.metric-list {
  display: grid;
  gap: 8px;
}

.metric-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.metric-label {
  color: #64748b;
  font-size: 14px;
}

.metric-value {
  color: #0f172a;
  font-size: 26px;
  line-height: 1;
}

.overview-tip {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.entry-list {
  margin: 8px 0 0;
  padding-left: 20px;
  color: #475569;
  line-height: 1.7;
}

.entry-footer {
  margin-top: 14px;
  display: grid;
  gap: 12px;
}

.entry-scene {
  padding: 12px 14px;
  background: #f8fafc;
}

.entry-scene--ai {
  background: linear-gradient(180deg, #f6fffd 0%, #eefaf8 100%);
  border-color: #cfe8e3;
}

.entry-scene p {
  margin: 6px 0 0;
  color: #475569;
  line-height: 1.55;
}

.primary,
.ai-button {
  width: 100%;
  border-radius: 14px;
  border: 1px solid transparent;
  padding: 11px 14px;
  cursor: pointer;
  font-weight: 700;
  font-size: 15px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.primary:hover,
.ai-button:hover {
  transform: translateY(-1px);
}

.primary {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #fff;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.18);
}

.ai-button {
  background: #0f766e;
  border-color: #0f766e;
  color: #fff;
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.16);
}

.feedback-stack {
  display: grid;
}

@media (max-width: 1260px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .overview-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .hero-notes,
  .entry-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page {
    padding-inline: 14px;
  }

  .hero-title {
    font-size: 34px;
  }

  .hero-description {
    font-size: 16px;
  }

  .metric-value {
    font-size: 24px;
  }
}
</style>
