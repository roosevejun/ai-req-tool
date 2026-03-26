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
            <h2>需求版本</h2>
            <p class="muted">需求 #{{ requirementId }}</p>
          </div>
          <div class="tab-actions">
            <button class="tab" @click="goWorkbench">工作台</button>
            <button class="tab active">版本页</button>
          </div>
        </section>

        <section class="card">
          <div class="panel-head">
            <h3>版本列表</h3>
            <button class="ghost mini" :disabled="loading" @click="loadVersions">刷新</button>
          </div>
          <table class="table">
            <thead>
            <tr>
              <th>ID</th>
              <th>版本号</th>
              <th>生成时间</th>
              <th>当前版本</th>
              <th>任务</th>
              <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="v in versions" :key="v.id" :class="{ current: !!v.isCurrent }">
              <td>{{ v.id }}</td>
              <td>{{ v.versionNo }}</td>
              <td>{{ v.generatedAt || '-' }}</td>
              <td>{{ v.isCurrent ? '是' : '否' }}</td>
              <td>{{ v.sourceJobId || '-' }}</td>
              <td class="ops">
                <button class="mini" @click="selectVersion(v.id)">预览</button>
                <button class="mini" @click="downloadVersion(v.id)">下载</button>
              </td>
            </tr>
            <tr v-if="versions.length === 0"><td colspan="6" class="empty small">当前还没有版本</td></tr>
            </tbody>
          </table>
        </section>

        <section class="card" v-if="selected">
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

type ApiResponse<T> = { code: number; message: string; data: T; traceId: string }
type VersionItem = {
  id: number
  requirementId: number
  versionNo: string
  prdMarkdown: string
  changeSummary: string
  sourceJobId: string
  generatedBy: number
  generatedAt: string
  isCurrent: boolean
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const versions = ref<VersionItem[]>([])
const selected = ref<VersionItem | null>(null)

const requirementId = computed(() => Number(route.params.requirementId))
const projectId = computed(() => {
  const v = Number(route.query.projectId || 0)
  return Number.isFinite(v) && v > 0 ? v : null
})

function showError(msg: string) {
  error.value = msg
}

function onSelectProject(pid: number) {
  router.push(`/projects?projectId=${pid}`)
}

function onSelectRequirement(payload: { projectId: number; requirementId: number }) {
  router.push(`/requirements/${payload.requirementId}/versions?projectId=${payload.projectId}`)
}

function goWorkbench() {
  if (projectId.value) {
    router.push(`/requirements/${requirementId.value}/workbench?projectId=${projectId.value}`)
    return
  }
  router.push(`/requirements/${requirementId.value}/workbench`)
}

async function loadVersions() {
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<VersionItem[]>>(`/api/requirements/${requirementId.value}/versions`)
    versions.value = res.data.data || []
    const current = versions.value.find(v => v.isCurrent) || versions.value[0]
    if (current) {
      await selectVersion(current.id)
    } else {
      selected.value = null
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载版本列表失败'
  } finally {
    loading.value = false
  }
}

async function selectVersion(versionId: number) {
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<VersionItem>>(`/api/requirements/${requirementId.value}/versions/${versionId}`)
    selected.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载版本详情失败'
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
    const a = document.createElement('a')
    a.href = url
    a.download = `01-PRD-Agent-Requirement-v${versionId}.md`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '下载失败'
  }
}

onMounted(loadVersions)
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
  margin-bottom: 12px;
}
.tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 8px;
}
.table th,
.table td {
  border: 1px solid #e5e7eb;
  padding: 8px;
  font-size: 13px;
}
.ops {
  display: flex;
  gap: 8px;
}
.current {
  background: #eff6ff;
}
.preview {
  white-space: pre-wrap;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px;
  max-height: 520px;
  overflow: auto;
}
.muted { color: #6b7280; margin: 4px 0 0; }
.empty { color: #6b7280; }
.small { font-size: 12px; }
.ghost,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #f3f4f6;
  cursor: pointer;
}
.ghost { padding: 8px 12px; }
.mini { padding: 5px 9px; font-size: 12px; }
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
