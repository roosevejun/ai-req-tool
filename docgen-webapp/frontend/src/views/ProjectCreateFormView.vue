<template>
  <div class="page">
    <section class="workspace-shell">
      <header class="workspace-intro">
        <div class="workspace-copy">
          <p class="hero-eyebrow">传统创建项目</p>
          <h1 class="hero-title">请录入项目立项信息</h1>
          <p class="hero-description">
            当前页面用于快速完成正式立项。适合项目信息已经清晰，希望直接建立项目档案并进入后续项目管理与协同维护的场景。
          </p>
        </div>

        <div class="workspace-meta">
          <div class="mode-summary">
            <span class="mode-summary__label">当前办理方式</span>
            <strong class="mode-summary__value">表单创建项目</strong>
            <span class="mode-summary__text">信息已经明确时，优先使用传统模式完成立项录入。</span>
          </div>

          <button class="ghost-action" type="button" @click="goAi">改用 AI 项目孵化</button>
        </div>
      </header>

      <div class="workspace-main">
        <ProjectCreateFormPanel
          :loading="loading"
          :form="form"
          :user-options="userOptions"
          @create="createProject"
        />
      </div>

      <aside class="workspace-side">
        <div class="side-card side-card--integrated">
          <div class="side-section">
            <p class="side-eyebrow">办理提示</p>
            <h2 class="side-title">立项录入要点</h2>
            <ul class="side-list">
              <li>优先补齐项目 Key、项目名称和项目背景。</li>
              <li>目标客户与核心价值越明确，后续需求整理越顺畅。</li>
              <li>计划日期与负责人建议在立项时一并确认。</li>
            </ul>
          </div>

          <div class="side-divider" />

          <div class="side-section">
            <p class="side-eyebrow">切换建议</p>
            <h2 class="side-title">什么时候改用 AI 孵化</h2>
            <p class="side-copy">
              如果你发现项目背景、客户价值或项目框架仍在梳理，建议切换到 AI 项目孵化继续提炼，再决定是否正式立项。
            </p>
          </div>

          <button class="side-action" type="button" @click="goAi">进入 AI 项目孵化</button>
        </div>

        <FeedbackPanel title="下一步建议" tone="warning" :message="workspaceAdvice" />
        <FeedbackPanel title="处理提示" tone="danger" :message="error" />
        <FeedbackPanel title="最新进展" tone="success" :message="success" />
      </aside>
    </section>
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
    return '先填写项目 Key 和项目名称，确保项目具备最基础的立项标识。'
  }
  if (!form.description.trim() && !form.projectBackground.trim()) {
    return '建议至少补充项目描述或项目背景，让项目创建后更容易继续进入需求和知识链路。'
  }
  if (!form.targetCustomerGroups.trim() || !form.coreProductValue.trim()) {
    return '建议继续完善目标客户群体和核心产品价值，这样后续需求整理会更顺畅。'
  }
  return '当前已经具备传统创建条件，检查时间范围和负责人后即可正式创建项目。'
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

onMounted(async () => {
  await loadUserOptions()
})
</script>

<style scoped>
.page {
  max-width: 1380px;
  margin: 0 auto;
  padding: 8px 16px 24px;
  color: #0f172a;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.workspace-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
  border: 1px solid #dbe2ea;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 251, 255, 0.96) 100%);
  box-shadow: 0 18px 42px rgba(148, 163, 184, 0.08);
  padding: 20px;
}

.workspace-intro {
  grid-column: 1 / -1;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5edf5;
}

.workspace-copy {
  display: grid;
  gap: 8px;
  min-width: 0;
  max-width: 920px;
}

.hero-eyebrow,
.side-eyebrow {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  font-size: 40px;
  line-height: 1.08;
}

.hero-description {
  margin: 0;
  color: #475569;
  font-size: 17px;
  line-height: 1.68;
}

.workspace-meta {
  display: grid;
  gap: 10px;
  width: min(360px, 100%);
}

.mode-summary {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid #e5edf5;
  background: rgba(255, 255, 255, 0.82);
}

.mode-summary__label {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.mode-summary__value {
  color: #0f172a;
  font-size: 18px;
  line-height: 1.3;
}

.mode-summary__text {
  color: #64748b;
  line-height: 1.55;
}

.ghost-action {
  border: 1px solid #d8e1eb;
  background: #fff;
  color: #0f172a;
  border-radius: 12px;
  padding: 11px 14px;
  cursor: pointer;
  font-weight: 700;
}

.workspace-main {
  min-width: 0;
}

.workspace-side {
  display: grid;
  gap: 12px;
  position: sticky;
  top: 12px;
  padding-left: 4px;
}

.side-card {
  border: 1px solid #d9e3ef;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  padding: 14px 16px;
  box-shadow: 0 10px 24px rgba(148, 163, 184, 0.06);
}

.side-card--integrated {
  display: grid;
  gap: 14px;
}

.side-divider {
  height: 1px;
  background: #e5edf5;
}

.side-title {
  margin: 8px 0 0;
  font-size: 20px;
  line-height: 1.2;
}

.side-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.7;
}

.side-copy {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.65;
}

.side-action {
  width: 100%;
  border-radius: 12px;
  border: 1px solid #0f766e;
  background: #0f766e;
  color: #fff;
  padding: 11px 14px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 1180px) {
  .workspace-shell {
    grid-template-columns: 1fr;
  }

  .workspace-intro {
    flex-direction: column;
  }

  .workspace-meta {
    width: 100%;
    max-width: none;
  }

  .workspace-side {
    position: static;
    padding-left: 0;
  }
}

@media (max-width: 768px) {
  .page {
    padding-inline: 12px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-description {
    font-size: 16px;
  }
}
</style>
