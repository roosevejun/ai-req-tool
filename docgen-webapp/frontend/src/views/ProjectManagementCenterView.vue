<template>
  <div class="page">
    <section class="hero">
      <div class="hero-main">
        <p class="hero-eyebrow">项目创建入口</p>
        <h1 class="hero-title">请选择项目创建方式</h1>
        <p class="hero-description">
          当前页面只保留项目创建相关入口。信息已经比较清晰时，直接进入传统创建；如果想法仍在梳理，建议先进入 AI 项目孵化，让系统边沟通边提炼项目框架。
        </p>
      </div>

      <div class="scenario-strip">
        <article class="scenario-card">
          <span class="scenario-label">适合传统创建</span>
          <p>项目名称、背景、客户与价值信息已经较完整，适合尽快形成正式项目档案。</p>
        </article>
        <article class="scenario-card scenario-card--ai">
          <span class="scenario-label">适合 AI 孵化</span>
          <p>想法还在梳理，希望先通过持续对话提炼框架，再决定是否正式立项。</p>
        </article>
      </div>
    </section>

    <section class="entry-grid">
      <WorkspaceSection
        eyebrow="入口一"
        title="传统创建项目"
        description="适合项目范围、背景和关键价值已经明确，准备直接录入正式立项信息。"
        :tint="true"
      >
        <ul class="entry-list">
          <li>直接录入项目名称、背景、客户与价值信息。</li>
          <li>创建后进入项目管理页继续维护与协同。</li>
        </ul>

        <div class="entry-footer">
          <p class="entry-tip"><span>办理指引</span> 信息已经明确时，优先进入传统创建。</p>
          <button class="primary full" type="button" @click="goForm">进入传统创建</button>
        </div>
      </WorkspaceSection>

      <WorkspaceSection
        eyebrow="入口二"
        title="AI 项目孵化"
        description="适合边沟通边提炼项目框架，先形成框架，再判断是否正式立项。"
      >
        <ul class="entry-list">
          <li>第一句话即可启动 AI 项目孵化。</li>
          <li>支持持续追问、补充资料与保留当前框架。</li>
        </ul>

        <div class="entry-footer">
          <p class="entry-tip entry-tip--ai"><span>AI 适用场景</span> 想法仍在梳理时，优先进入 AI 项目孵化。</p>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import WorkspaceSection from '../components/projects/WorkspaceSection.vue'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'

const router = useRouter()
const error = ref('')

function goForm() {
  void router.push('/projects/create/form')
}

function goAi() {
  void router.push('/projects/create-ai')
}
</script>

<style scoped>
.page {
  max-width: 1360px;
  box-sizing: border-box;
  margin: 0 auto;
  min-height: 100%;
  padding: 8px 18px 16px;
  display: grid;
  align-content: start;
  gap: 18px;
  color: #0f172a;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.hero {
  display: grid;
  gap: 14px;
}

.hero-main {
  display: grid;
  gap: 10px;
  max-width: 980px;
  padding: 6px 4px 0;
}

.hero-eyebrow,
.scenario-label,
.entry-tip span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  font-size: 42px;
  line-height: 1.08;
  color: #0f172a;
}

.hero-description {
  margin: 0;
  max-width: 920px;
  color: #475569;
  font-size: 17px;
  line-height: 1.7;
}

.scenario-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.scenario-card {
  border: 1px solid #d7e2ef;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 28px rgba(148, 163, 184, 0.08);
  padding: 16px 18px;
  display: grid;
  gap: 10px;
}

.scenario-card--ai {
  background: linear-gradient(180deg, #f4fffd 0%, #eefaf8 100%);
  border-color: #cfe8e3;
}

.scenario-label {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 5px 10px;
  border-radius: 999px;
  background: #eff4ff;
  letter-spacing: normal;
  text-transform: none;
}

.scenario-card p {
  margin: 0;
  color: #334155;
  line-height: 1.7;
  font-size: 16px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.entry-list {
  margin: 8px 0 0;
  padding-left: 22px;
  color: #475569;
  line-height: 1.75;
  display: grid;
  gap: 2px;
}

.entry-footer {
  margin-top: 14px;
  display: grid;
  gap: 12px;
}

.entry-tip {
  margin: 0;
  padding: 14px 16px;
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  background: #f8fafc;
  color: #475569;
  line-height: 1.6;
}

.entry-tip--ai {
  background: linear-gradient(180deg, #f6fffd 0%, #eefaf8 100%);
  border-color: #cfe8e3;
}

.entry-tip span {
  display: block;
  margin-bottom: 6px;
  letter-spacing: normal;
  text-transform: none;
}

.primary,
.ai-button {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.ai-button {
  background: #0f766e;
  border-color: #0f766e;
  color: #fff;
}

.full {
  width: 100%;
}

.feedback-stack {
  display: grid;
  gap: 12px;
}

@media (max-width: 960px) {
  .scenario-strip,
  .entry-grid {
    grid-template-columns: 1fr;
  }

  .hero-title {
    font-size: 34px;
  }

  .page {
    padding-inline: 14px;
  }
}
</style>
