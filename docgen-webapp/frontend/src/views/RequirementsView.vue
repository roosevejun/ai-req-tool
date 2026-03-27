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

    <RequirementsTableCard
      :requirements="requirements"
      @open-workbench="openWorkbench"
      @open-versions="openVersions"
    />

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import RequirementsTableCard from '../components/requirements/RequirementsTableCard.vue'
import type { ApiResponse, RequirementItem } from '../components/requirements/types'

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
  void router.push(`/requirements/${requirementId}/workbench?projectId=${projectId.value}`)
}

function openVersions(requirementId: number) {
  void router.push(`/requirements/${requirementId}/versions?projectId=${projectId.value}`)
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
.primary,.ghost { border:1px solid #e5e7eb; border-radius:8px; padding:8px 12px; cursor:pointer; }
.primary { background:#2563eb; border-color:#2563eb; color:#fff; }
.ghost { background:#f3f4f6; }
.muted { color:#6b7280; }
.error { color:#b91c1c; margin-top:10px; }
.success { color:#166534; margin-top:10px; }
@media (max-width: 980px) { .form-grid { grid-template-columns: 1fr; } }
</style>
