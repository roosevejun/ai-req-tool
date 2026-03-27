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
          title="需求版本页"
          :requirement-id="requirementId"
          active-tab="versions"
          @go-workbench="goWorkbench"
          @go-versions="noop"
        />

        <RequirementVersionsCard
          :loading="loading"
          :versions="versions"
          @refresh="loadVersions"
          @select-version="selectVersion"
          @download-version="downloadVersion"
        />

        <section v-if="selected" class="card">
          <div class="panel-head">
            <h3>版本 {{ selected.versionNo }}</h3>
            <button class="ghost mini" @click="downloadVersion(selected.id)">下载</button>
          </div>
          <p class="muted">变更摘要：{{ selected.changeSummary || '-' }}</p>
          <pre class="preview">{{ selected.prdMarkdown }}</pre>
        </section>
      </main>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import ProjectRequirementTree from '../components/ProjectRequirementTree.vue'
import RequirementPageHeader from '../components/requirements/RequirementPageHeader.vue'
import RequirementVersionsCard from '../components/requirements/RequirementVersionsCard.vue'
import type { ApiResponse, RequirementVersionItem } from '../components/requirements/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const versions = ref<RequirementVersionItem[]>([])
const selected = ref<RequirementVersionItem | null>(null)

const requirementId = computed(() => Number(route.params.requirementId))
const projectId = computed(() => {
  const value = Number(route.query.projectId || 0)
  return Number.isFinite(value) && value > 0 ? value : null
})

function noop() {}

function showError(message: string) {
  error.value = message
}

function onSelectProject(project: number) {
  void router.push(`/projects?projectId=${project}`)
}

function onSelectRequirement(payload: { projectId: number; requirementId: number }) {
  void router.push(`/requirements/${payload.requirementId}/versions?projectId=${payload.projectId}`)
}

function goWorkbench() {
  if (projectId.value) {
    void router.push(`/requirements/${requirementId.value}/workbench?projectId=${projectId.value}`)
    return
  }
  void router.push(`/requirements/${requirementId.value}/workbench`)
}

async function loadVersions() {
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<RequirementVersionItem[]>>(`/api/requirements/${requirementId.value}/versions`)
    versions.value = res.data.data || []
    const currentVersion = versions.value.find((item) => item.isCurrent) || versions.value[0]
    if (currentVersion) {
      await selectVersion(currentVersion.id)
    } else {
      selected.value = null
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载版本列表失败。'
  } finally {
    loading.value = false
  }
}

async function selectVersion(versionId: number) {
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<RequirementVersionItem>>(`/api/requirements/${requirementId.value}/versions/${versionId}`)
    selected.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载版本详情失败。'
  }
}

async function downloadVersion(versionId: number) {
  error.value = ''
  try {
    const resp = await axios.get(`/api/requirements/${requirementId.value}/versions/${versionId}/export`, {
      responseType: 'blob'
    })
    const blob = new Blob([resp.data], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `01-PRD-Agent-Requirement-v${versionId}.md`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '下载版本失败。'
  }
}

onMounted(loadVersions)
</script>

<style scoped>
.page { max-width: 1320px; margin: 18px auto; padding: 0 14px 18px; font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }
.layout { display: grid; grid-template-columns: 350px 1fr; gap: 14px; }
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 10px; margin-bottom: 12px; }
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.preview { white-space: pre-wrap; background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px; max-height: 520px; overflow: auto; }
.muted { color: #6b7280; margin: 4px 0 0; }
.ghost, .mini { border-radius: 8px; border: 1px solid #d1d5db; background: #f3f4f6; cursor: pointer; }
.ghost { padding: 8px 12px; }
.mini { padding: 5px 9px; font-size: 12px; }
.error { margin-top: 8px; color: #b91c1c; }
@media (max-width: 980px) {
  .layout { grid-template-columns: 1fr; }
}
</style>
