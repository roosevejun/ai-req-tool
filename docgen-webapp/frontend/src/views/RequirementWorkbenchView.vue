<template>
  <div class="page">
    <header class="topbar">
      <h1>需求工作台 #{{ requirementId }}</h1>
    </header>

    <div class="layout">
      <aside class="sidebar">
        <ProjectRequirementTree
          :selected-project-id="projectId"
          :selected-requirement-id="requirementId"
          @select-project="onSelectProject"
          @select-requirement="onSelectRequirement"
          @error="showError"
        />
      </aside>

      <main class="content">
        <section class="tabs card">
          <button class="tab active">AI工作台</button>
          <button class="tab" @click="goVersions">版本页</button>
        </section>
        <DocGenPage :api-base="apiBase" :draft-key="draftKey" />
      </main>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DocGenPage from '../components/DocGenPage.vue'
import ProjectRequirementTree from '../components/ProjectRequirementTree.vue'

const route = useRoute()
const router = useRouter()
const error = ref('')

const requirementId = computed(() => Number(route.params.requirementId))
const projectId = computed(() => {
  const v = Number(route.query.projectId || 0)
  return Number.isFinite(v) && v > 0 ? v : null
})

const apiBase = computed(() => `/api/requirements/${requirementId.value}/docgen`)
const draftKey = computed(() => `docgen-draft-req-${requirementId.value}`)

function showError(msg: string) {
  error.value = msg
}

function onSelectProject(pid: number) {
  router.push(`/projects?projectId=${pid}`)
}

function onSelectRequirement(payload: { projectId: number; requirementId: number }) {
  router.push(`/requirements/${payload.requirementId}/workbench?projectId=${payload.projectId}`)
}

function goVersions() {
  if (projectId.value) {
    router.push(`/requirements/${requirementId.value}/versions?projectId=${projectId.value}`)
    return
  }
  router.push(`/requirements/${requirementId.value}/versions`)
}
</script>

<style scoped>
.page {
  max-width: 1320px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.layout {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 14px;
}
.card {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  padding: 10px;
}
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.tab {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 12px;
  background: #f3f4f6;
  cursor: pointer;
}
.tab.active {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}
.ghost {
  border: 1px solid #d1d5db;
  background: #f3f4f6;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
}
.error {
  margin-top: 8px;
  color: #b91c1c;
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
