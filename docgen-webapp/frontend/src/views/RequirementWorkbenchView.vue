<template>
  <div class="page">
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
          <div class="title-block">
            <h2>需求工作台</h2>
            <p class="muted">需求 #{{ requirementId }}</p>
          </div>
          <div class="tab-actions">
            <button class="tab active">工作台</button>
            <button class="tab" @click="goVersions">版本页</button>
          </div>
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
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.title-block h2 {
  margin: 0;
}
.tab-actions {
  display: flex;
  gap: 8px;
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
.muted {
  margin: 4px 0 0;
  color: #6b7280;
}
.error {
  margin-top: 8px;
  color: #b91c1c;
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .tabs {
    flex-direction: column;
    align-items: stretch;
  }
  .tab-actions {
    flex-wrap: wrap;
  }
}
</style>
