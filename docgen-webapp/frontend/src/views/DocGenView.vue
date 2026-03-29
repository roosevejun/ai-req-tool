<template>
  <div class="page">
    <CenterHero
      eyebrow="Requirements Management Center"
      title="AI 需求整理"
      summary="围绕项目与需求上下文持续整理、澄清并沉淀 PRD，让需求列表、AI 工作台与版本链路保持同一条业务主线。"
    >
      <template #badges>
        <StatusBadge :label="selectedProjectId ? `项目 #${selectedProjectId}` : '未选中项目'" :variant="selectedProjectId ? 'info' : 'warning'" />
        <StatusBadge :label="selectedRequirementId ? `需求 #${selectedRequirementId}` : '未选中需求'" :variant="selectedRequirementId ? 'success' : 'warning'" />
        <StatusBadge :label="`${requirements.length} 条需求`" variant="ai" />
      </template>
    </CenterHero>

    <div class="layout">
      <aside class="sidebar">
        <ProjectRequirementTree
          :selected-project-id="selectedProjectId"
          :selected-requirement-id="selectedRequirementId"
          @select-project="onSelectProject"
          @select-requirement="onSelectRequirement"
          @error="showError"
        />
      </aside>

      <main class="content">
        <RequirementComposerCard
          :loading="loading"
          :selected-project-id="selectedProjectId"
          :selected-requirement-id="selectedRequirementId"
          :req-form="reqForm"
          @create-requirement="createRequirement"
          @refresh-requirements="loadRequirements"
        />

        <DocGenStagePanel
          :selected-project-id="selectedProjectId"
          :selected-requirement-id="selectedRequirementId"
          :requirements-count="requirements.length"
        />

        <RequirementListCard
          :selected-project-id="selectedProjectId"
          :requirements="requirements"
          @select-requirement="onSelectRequirement"
          @open-workbench="goWorkbench"
          @open-versions="goVersions"
        />

        <section v-if="selectedRequirementId" class="card workspace-card">
          <div class="row between">
            <h3>需求 {{ selectedRequirementId }} 的 AI 整理</h3>
            <div class="row">
              <button class="ghost mini" @click="goWorkbench(selectedRequirementId)">打开工作台</button>
              <button class="ghost mini" @click="goVersions(selectedRequirementId)">查看版本页</button>
            </div>
          </div>
          <DocGenPage
            :api-base="`/api/requirements/${selectedRequirementId}/docgen`"
            :draft-key="`docgen-draft-req-${selectedRequirementId}`"
          />
        </section>

        <EmptyWorkspaceState
          v-else
          title="请选择一个需求开始 AI 整理"
          description="先在左侧树中选择项目和需求，这里会显示对应的 AI 整理面板。"
        />
      </main>
    </div>

    <div class="feedback-stack">
      <FeedbackPanel title="下一步建议" :message="docgenAdvice" tone="warning" />
      <FeedbackPanel title="处理提示" :message="error" tone="danger" />
      <FeedbackPanel title="最新进展" :message="success" tone="success" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import EmptyWorkspaceState from '../components/projects/EmptyWorkspaceState.vue'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import StatusBadge from '../components/projects/StatusBadge.vue'
import CenterHero from '../components/shell/CenterHero.vue'
import DocGenPage from '../components/DocGenPage.vue'
import ProjectRequirementTree from '../components/ProjectRequirementTree.vue'
import DocGenStagePanel from '../components/docgen-view/DocGenStagePanel.vue'
import RequirementComposerCard from '../components/docgen-view/RequirementComposerCard.vue'
import RequirementListCard from '../components/docgen-view/RequirementListCard.vue'
import type { ApiResponse, RequirementItem } from '../components/docgen-view/types'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const success = ref('')
const selectedProjectId = ref<number | null>(null)
const selectedRequirementId = ref<number | null>(null)
const requirements = ref<RequirementItem[]>([])
const reqForm = reactive({ title: '', summary: '', priority: 'P2', status: 'DRAFT' })
const docgenAdvice = computed(() => {
  if (!selectedProjectId.value) return '先在左侧树中选择项目，需求生产流程会基于当前项目范围展开。'
  if (!selectedRequirementId.value) {
    return requirements.value.length > 0
      ? '当前项目已有需求，建议选择一条需求并进入下方 AI 整理面板。'
      : '当前项目还没有需求，建议先创建一条需求，再进入 AI 整理。'
  }
  return '当前已经进入需求整理主流程，建议优先完成澄清和结构化补充，再生成 PRD。'
})

function showError(message: string) { error.value = message }
function goWorkbench(requirementId: number) { if (!selectedProjectId.value) return; void router.push(`/requirements/${requirementId}/workbench?projectId=${selectedProjectId.value}`) }
function goVersions(requirementId: number) { if (!selectedProjectId.value) return; void router.push(`/requirements/${requirementId}/versions?projectId=${selectedProjectId.value}`) }

async function loadRequirements() {
  if (!selectedProjectId.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<RequirementItem[]>>(`/api/projects/${selectedProjectId.value}/requirements`)
    requirements.value = res.data.data || []
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载需求列表失败。'
  } finally { loading.value = false }
}

async function createRequirement() {
  if (!selectedProjectId.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await axios.post<ApiResponse<number>>(`/api/projects/${selectedProjectId.value}/requirements`, reqForm)
    success.value = '需求创建成功。'
    reqForm.title = ''
    reqForm.summary = ''
    reqForm.priority = 'P2'
    reqForm.status = 'DRAFT'
    await loadRequirements()
    if (res.data?.data) selectedRequirementId.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建需求失败。'
  } finally { loading.value = false }
}

async function onSelectProject(projectId: number) { selectedProjectId.value = projectId; selectedRequirementId.value = null; await loadRequirements() }
async function onSelectRequirement(projectId: number, requirementId: number) {
  selectedProjectId.value = projectId
  selectedRequirementId.value = requirementId
  if (requirements.value.length === 0 || !requirements.value.some((item) => item.id === requirementId)) await loadRequirements()
}

watch(selectedProjectId, async (projectId) => {
  if (projectId) await loadRequirements()
  else requirements.value = []
})
</script>

<style scoped>
.page { max-width: 1440px; margin: 18px auto; padding: 0 14px 18px; font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }
.layout { display: grid; grid-template-columns: 350px 1fr; gap: 14px; }
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 16px; padding: 14px; margin-bottom: 14px; }
.workspace-card { background: linear-gradient(180deg, #f8fcff 0%, #ffffff 45%); }
.row { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.between { justify-content: space-between; align-items: center; }
.ghost, .mini { border-radius: 8px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; background: #f3f4f6; }
.mini { padding: 5px 9px; font-size: 12px; }
.feedback-stack { display: grid; gap: 10px; margin-top: 12px; }
@media (max-width: 980px) { .layout { grid-template-columns: 1fr; } .between { align-items: flex-start; } }
</style>
