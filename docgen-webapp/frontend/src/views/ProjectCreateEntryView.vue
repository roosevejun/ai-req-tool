<template>
  <div class="page">
    <section class="hero">
      <div>
        <p class="eyebrow">项目创建中心</p>
        <h1>选择项目创建方式</h1>
        <p class="summary">先明确你是想直接录入项目信息，还是通过 AI 对话和资料沉淀来逐步创建项目。</p>
      </div>
      <div class="hero-actions">
        <button class="ghost" type="button" @click="goProjects">返回项目工作台</button>
      </div>
    </section>

    <ProjectCreateModeSelector @open-form="goForm" @open-ai="goAi" />

    <div class="feedback-stack">
      <FeedbackPanel
        title="使用建议"
        tone="warning"
        message="信息已经明确时优先走传统模式；如果项目想法还在梳理中、需要补资料和持续追问，优先走 AI 模式。"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import ProjectCreateModeSelector from '../components/project-create-entry/ProjectCreateModeSelector.vue'

const router = useRouter()

function goProjects() {
  void router.push('/projects')
}

function goForm() {
  void router.push('/projects/create/form')
}

function goAi() {
  void router.push('/projects/create-ai')
}
</script>

<style scoped>
.page {
  max-width: 1320px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}
.hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
  padding: 20px 22px;
  border-radius: 22px;
  border: 1px solid #dbe2ea;
  background: linear-gradient(135deg, #f7fbff 0%, #ffffff 60%);
}
.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}
h1 {
  margin: 0;
  font-size: 34px;
  color: #0f172a;
}
.summary {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.7;
  max-width: 720px;
}
.hero-actions {
  display: flex;
  gap: 10px;
}
.ghost {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #f8fafc;
  color: #0f172a;
  padding: 10px 14px;
  cursor: pointer;
}
.feedback-stack {
  margin-top: 14px;
}
@media (max-width: 960px) {
  .hero {
    flex-direction: column;
  }
}
</style>
