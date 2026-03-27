<template>
  <div class="page">
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

        <RequirementListCard
          :selected-project-id="selectedProjectId"
          :requirements="requirements"
          @select-requirement="onSelectRequirement"
          @open-workbench="goWorkbench"
          @open-versions="goVersions"
        />

        <section v-if="selectedRequirementId" class="card">
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

        <section v-else class="card empty-state">
          <h3>请选择一个需求开始 AI 整理</h3>
          <p>先在左侧树中选择项目和需求，这里会显示对应的 AI 整理面板。</p>
        </section>
      </main>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import DocGenPage from '../components/DocGenPage.vue'
import ProjectRequirementTree from '../components/ProjectRequirementTree.vue'
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

const reqForm = reactive({
  title: '',
  summary: '',
  priority: 'P2',
  status: 'DRAFT'
})

function showError(message: string) {
  error.value = message
}

function goWorkbench(requirementId: number) {
  if (!selectedProjectId.value) return
  void router.push(`/requirements/${requirementId}/workbench?projectId=${selectedProjectId.value}`)
}

function goVersions(requirementId: number) {
  if (!selectedProjectId.value) return
  void router.push(`/requirements/${requirementId}/versions?projectId=${selectedProjectId.value}`)
}

async function loadRequirements() {
  if (!selectedProjectId.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<RequirementItem[]>>(`/api/projects/${selectedProjectId.value}/requirements`)
    requirements.value = res.data.data || []
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载需求列表失败。'
  } finally {
    loading.value = false
  }
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
    if (res.data?.data) {
      selectedRequirementId.value = res.data.data
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '创建需求失败。'
  } finally {
    loading.value = false
  }
}

async function onSelectProject(projectId: number) {
  selectedProjectId.value = projectId
  selectedRequirementId.value = null
  await loadRequirements()
}

async function onSelectRequirement(projectId: number, requirementId: number) {
  selectedProjectId.value = projectId
  selectedRequirementId.value = requirementId
  if (requirements.value.length === 0 || !requirements.value.some((item) => item.id === requirementId)) {
    await loadRequirements()
  }
}

watch(selectedProjectId, async (projectId) => {
  if (projectId) {
    await loadRequirements()
  } else {
    requirements.value = []
  }
})
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
  padding: 12px;
  margin-bottom: 12px;
}
.row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.between {
  justify-content: space-between;
  align-items: center;
}
.ghost,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
  background: #f3f4f6;
}
.mini {
  padding: 5px 9px;
  font-size: 12px;
}
.empty-state h3,
.empty-state p {
  margin: 0;
}
.empty-state p {
  margin-top: 8px;
  color: #6b7280;
}
.error {
  margin-top: 8px;
  color: #b91c1c;
}
.success {
  margin-top: 8px;
  color: #166534;
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .between {
    align-items: flex-start;
  }
}
</style>
