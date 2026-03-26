<template>
  <div class="container">
    <section class="panel">
      <div class="section-head">
        <div>
          <h2>需求管理</h2>
          <p class="muted">项目 ID：{{ projectId }}</p>
        </div>
      </div>

      <h3>创建需求</h3>
      <div class="form-grid">
        <input v-model.trim="form.title" class="input" placeholder="需求标题" />
        <select v-model="form.priority" class="input">
          <option value="P0">P0 - 紧急</option>
          <option value="P1">P1 - 高</option>
          <option value="P2">P2 - 中</option>
          <option value="P3">P3 - 低</option>
        </select>
        <select v-model="form.status" class="input">
          <option value="DRAFT">草稿</option>
          <option value="CLARIFYING">澄清中</option>
          <option value="READY_REVIEW">待评审</option>
          <option value="DONE">已完成</option>
        </select>
      </div>

      <textarea v-model="form.summary" class="input" placeholder="需求摘要" />

      <div class="row">
        <button class="primary" :disabled="loading || !form.title" @click="createRequirement">创建需求</button>
        <button class="ghost" :disabled="loading" @click="loadRequirements">刷新</button>
      </div>
    </section>

    <section class="panel">
      <div class="section-head">
        <h3>需求列表</h3>
      </div>
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>编号</th>
            <th>标题</th>
            <th>优先级</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in requirements" :key="r.id">
            <td>{{ r.id }}</td>
            <td>{{ r.requirementNo }}</td>
            <td>{{ r.title }}</td>
            <td>{{ priorityLabel(r.priority) }}</td>
            <td>{{ statusLabel(r.status) }}</td>
            <td class="ops">
              <button class="mini" @click="openWorkbench(r.id)">AI 工作台</button>
              <button class="mini" @click="openVersions(r.id)">版本页</button>
            </td>
          </tr>
          <tr v-if="requirements.length === 0">
            <td colspan="6" class="muted">暂无需求</td>
          </tr>
        </tbody>
      </table>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'

type ApiResponse<T> = { code: number; message: string; data: T; traceId: string }
type RequirementItem = {
  id: number
  requirementNo: string
  title: string
  summary: string
  priority: string
  status: string
}

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.projectId))
const loading = ref(false)
const error = ref('')
const success = ref('')
const requirements = ref<RequirementItem[]>([])
const form = reactive({
  title: '',
  summary: '',
  priority: 'P2',
  status: 'DRAFT'
})

function priorityLabel(value?: string) {
  if (value === 'P0') return 'P0 - 紧急'
  if (value === 'P1') return 'P1 - 高'
  if (value === 'P2') return 'P2 - 中'
  if (value === 'P3') return 'P3 - 低'
  return value || '-'
}

function statusLabel(value?: string) {
  if (value === 'DRAFT') return '草稿'
  if (value === 'CLARIFYING') return '澄清中'
  if (value === 'READY_REVIEW') return '待评审'
  if (value === 'DONE') return '已完成'
  return value || '-'
}

async function loadRequirements() {
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<RequirementItem[]>>(`/api/projects/${projectId.value}/requirements`)
    requirements.value = res.data.data || []
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载需求列表失败。'
  } finally {
    loading.value = false
  }
}

async function createRequirement() {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/projects/${projectId.value}/requirements`, form)
    success.value = '需求创建成功。'
    form.title = ''
    form.summary = ''
    form.priority = 'P2'
    form.status = 'DRAFT'
    await loadRequirements()
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建需求失败。'
  } finally {
    loading.value = false
  }
}

function openWorkbench(requirementId: number) {
  router.push(`/requirements/${requirementId}/workbench?projectId=${projectId.value}`)
}

function openVersions(requirementId: number) {
  router.push(`/requirements/${requirementId}/versions?projectId=${projectId.value}`)
}

onMounted(loadRequirements)
</script>

<style scoped>
.container { max-width: 1100px; margin: 24px auto; padding: 0 16px; font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }
.panel { background:#fff; border:1px solid #e5e7eb; border-radius:10px; padding:12px; margin-top:12px; }
.section-head { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:8px; }
.section-head h2, .section-head h3 { margin:0; }
.form-grid { display:grid; grid-template-columns: 2fr 1fr 1fr; gap:10px; margin-bottom:10px; }
.input { width:100%; box-sizing:border-box; border:1px solid #d1d5db; border-radius:8px; padding:8px; margin-top:8px; }
.row { display:flex; gap:10px; margin-top:10px; }
.primary,.ghost,.mini { border:1px solid #e5e7eb; border-radius:8px; padding:8px 12px; cursor:pointer; }
.primary { background:#2563eb; border-color:#2563eb; color:#fff; }
.table { width:100%; border-collapse:collapse; margin-top:8px; }
.table th,.table td { border:1px solid #e5e7eb; padding:8px; font-size:13px; }
.ops { display:flex; gap:8px; }
.muted { color:#6b7280; }
.error { color:#b91c1c; margin-top:10px; }
.success { color:#166534; margin-top:10px; }
</style>
