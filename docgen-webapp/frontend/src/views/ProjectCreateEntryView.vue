<template>
  <div class="page">
    <CenterHero
      eyebrow="Project Creation Center"
      title="选择项目创建方式"
      summary="先明确你是要直接录入项目信息，还是通过 AI 对话逐步提炼项目框架。两条路径最终都会回到同一套项目管理体系。"
    >
      <template #actions>
        <button class="ghost" type="button" @click="goProjects">返回项目管理中心</button>
      </template>
    </CenterHero>

    <ProjectCreateModeSelector @open-form="goForm" @open-ai="goAi" />

    <div class="feedback-stack">
      <FeedbackPanel
        title="使用建议"
        tone="warning"
        message="项目信息已经明确时优先走传统模式；如果项目想法仍在梳理、需要补资料和持续追问，优先走 AI 项目孵化。"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import ProjectCreateModeSelector from '../components/project-create-entry/ProjectCreateModeSelector.vue'
import CenterHero from '../components/shell/CenterHero.vue'

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
  display: grid;
  gap: 16px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}
.feedback-stack {
  display: grid;
  gap: 12px;
}
.ghost {
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #0f172a;
  padding: 11px 16px;
  cursor: pointer;
  font-weight: 700;
}
</style>
