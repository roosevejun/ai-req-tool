<template>
  <div class="page">
    <ProjectCreateFormPanel
      :loading="loading"
      :form="form"
      :user-options="userOptions"
      @create="createProject"
      @go-ai="goAi"
      @go-entry="goEntry"
    />

    <div class="feedback-stack">
      <FeedbackPanel title="下一步建议" tone="warning" :message="workspaceAdvice" />
      <FeedbackPanel title="处理提示" tone="danger" :message="error" />
      <FeedbackPanel title="最新进展" tone="success" :message="success" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import type { ApiResponse, ProjectFormState, UserOption } from '../components/projects/types'
import ProjectCreateFormPanel from '../components/project-create-form/ProjectCreateFormPanel.vue'

const router = useRouter()

const loading = ref(false)
const error = ref('')
const success = ref('')
const userOptions = ref<UserOption[]>([])
const form = reactive<ProjectFormState>({
  projectKey: '',
  projectName: '',
  description: '',
  projectBackground: '',
  similarProducts: '',
  targetCustomerGroups: '',
  commercialValue: '',
  coreProductValue: '',
  projectType: '',
  priority: 'P2',
  startDate: '',
  targetDate: '',
  tags: '',
  ownerUserId: '',
  visibility: 'PRIVATE'
})

const workspaceAdvice = computed(() => {
  if (!form.projectKey.trim() || !form.projectName.trim()) {
    return '先填写项目 Key 和项目名称，确保项目具备最基本的立项标识。'
  }
  if (!form.description.trim() && !form.projectBackground.trim()) {
    return '建议至少补充项目描述或项目背景，让创建后的项目更容易继续进入需求和知识链路。'
  }
  if (!form.targetCustomerGroups.trim() || !form.coreProductValue.trim()) {
    return '建议继续完善目标客户群体和核心产品价值，这样后续需求整理会更顺畅。'
  }
  return '当前已经具备传统创建条件，检查时间范围和负责人后就可以正式创建项目。'
})

function resetForm() {
  form.projectKey = ''
  form.projectName = ''
  form.description = ''
  form.projectBackground = ''
  form.similarProducts = ''
  form.targetCustomerGroups = ''
  form.commercialValue = ''
  form.coreProductValue = ''
  form.projectType = ''
  form.priority = 'P2'
  form.startDate = ''
  form.targetDate = ''
  form.tags = ''
  form.ownerUserId = ''
  form.visibility = 'PRIVATE'
}

async function loadUserOptions() {
  try {
    const res = await axios.get<ApiResponse<UserOption[]>>('/api/system/users')
    userOptions.value = (res.data.data || []).filter((item) => item.status !== 'DISABLED')
  } catch {
    userOptions.value = []
  }
}

async function createProject() {
  if (!form.projectKey.trim() || !form.projectName.trim()) {
    error.value = '请先填写项目 Key 和项目名称。'
    success.value = ''
    return
  }
  if (form.startDate && form.targetDate && form.targetDate < form.startDate) {
    error.value = '计划结束日期不能早于计划开始日期。'
    success.value = ''
    return
  }

  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await axios.post<ApiResponse<number>>('/api/projects', {
      ...form,
      ownerUserId: form.ownerUserId ? Number(form.ownerUserId) : null,
      startDate: form.startDate || null,
      targetDate: form.targetDate || null,
      projectType: form.projectType || null,
      tags: form.tags || null
    })
    const projectId = res.data.data
    resetForm()
    success.value = '项目创建成功，正在跳转到项目工作台。'
    await router.push(`/projects?projectId=${projectId}`)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建项目失败。'
  } finally {
    loading.value = false
  }
}

function goAi() {
  void router.push('/projects/create-ai')
}

function goEntry() {
  void router.push('/projects/create')
}

onMounted(async () => {
  await loadUserOptions()
})
</script>

<style scoped>
.page {
  max-width: 1320px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}
.feedback-stack {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}
</style>
