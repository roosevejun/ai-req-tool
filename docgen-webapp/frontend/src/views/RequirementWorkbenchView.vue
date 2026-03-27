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
        <RequirementPageHeader
          title="需求工作台"
          :requirement-id="requirementId"
          active-tab="workbench"
          @go-workbench="noop"
          @go-versions="goVersions"
        />

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
import RequirementPageHeader from '../components/requirements/RequirementPageHeader.vue'

const route = useRoute()
const router = useRouter()
const error = ref('')

const requirementId = computed(() => Number(route.params.requirementId))
const projectId = computed(() => {
  const value = Number(route.query.projectId || 0)
  return Number.isFinite(value) && value > 0 ? value : null
})

const apiBase = computed(() => `/api/requirements/${requirementId.value}/docgen`)
const draftKey = computed(() => `docgen-draft-req-${requirementId.value}`)

function noop() {}

function showError(message: string) {
  error.value = message
}

function onSelectProject(project: number) {
  void router.push(`/projects?projectId=${project}`)
}

function onSelectRequirement(payload: { projectId: number; requirementId: number }) {
  void router.push(`/requirements/${payload.requirementId}/workbench?projectId=${payload.projectId}`)
}

function goVersions() {
  if (projectId.value) {
    void router.push(`/requirements/${requirementId.value}/versions?projectId=${projectId.value}`)
    return
  }
  void router.push(`/requirements/${requirementId.value}/versions`)
}
</script>

<style scoped>
.page { max-width: 1320px; margin: 18px auto; padding: 0 14px 18px; font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }
.layout { display: grid; grid-template-columns: 350px 1fr; gap: 14px; }
.error { margin-top: 8px; color: #b91c1c; }
@media (max-width: 980px) {
  .layout { grid-template-columns: 1fr; }
}
</style>
