<template>
  <div class="page">
    <div class="layout">
      <ProjectsSidebar
        :loading="loading"
        :projects="projects"
        :selected-project-id="selectedProjectId"
        :project-count="projects.length"
        :requirement-count="selectedProject ? requirementsOf(selectedProject.id).length : 0"
        :pending-knowledge-count="pendingKnowledgeCount"
        @reload-projects="loadProjects"
        @select-project="selectProject"
      />

      <main class="content">
        <EmptyWorkspaceState
          v-if="!selectedProject"
          title="请选择一个项目开始协同"
          description="先在左侧选择项目，这里会切换到项目概览、AI 协同和资料知识工作区。"
        />

        <template v-else>
          <ProjectWorkspaceHeader
            :project="selectedProject"
            :active-tab="activeWorkspaceTab"
            :project-type-label="projectTypeLabel"
            :visibility-label="visibilityLabel"
            :project-status-label="projectStatusLabel"
            @change-tab="handleWorkspaceTabChange"
            @enter-ai="ensureAiWorkspace"
          />

          <section v-if="activeWorkspaceTab === 'overview'" class="workspace-stack">
            <ProjectOverviewPanel
              :loading="loading"
              :project="selectedProject"
              :project-type-label="projectTypeLabel"
              :visibility-label="visibilityLabel"
              :project-status-label="projectStatusLabel"
              @start-edit="startEditProject"
              @enter-ai="ensureAiWorkspace"
              @delete-project="deleteProject"
            />

            <ProjectMembersCard
              :loading="loading"
              :member-form="memberForm"
              :user-options="userOptions"
              :members="membersOf(selectedProject.id)"
              :project-role-label="projectRoleLabel"
              :member-status-label="memberStatusLabel"
              @add-member="addProjectMember"
              @remove-member="removeProjectMember"
            />
          </section>

          <section v-else-if="activeWorkspaceTab === 'ai'" class="workspace-stack">
            <ProjectAiWorkspacePanel
              :loading="loading"
              :project="selectedProject"
              :project-edit-form="projectEditForm"
              :ai-updated-fields="aiUpdatedFields"
              :project-conversation-loading="projectConversationLoading"
              :project-conversation="projectConversation"
              :project-conversation-input="projectConversationInput"
              :can-send-project-conversation="canSendProjectConversation"
              :project-knowledge-preview-visible="projectKnowledgePreviewVisible"
              :project-knowledge-preview-loading="projectKnowledgePreviewLoading"
              :project-knowledge-preview="projectKnowledgePreview"
              :project-knowledge-preview-query-text="projectKnowledgePreviewQueryText"
              :user-options="userOptions"
              @cancel-edit="cancelProjectEdit"
              @save-edit="saveProjectEdit"
              @delete-project="deleteProject"
              @refresh-project-ai="ensureProjectConversation"
              @send-project-ai="sendProjectConversation"
              @apply-project-ai="applyConversationStructuredInfo"
              @update-project-ai-input="projectConversationInput = $event"
              @load-project-knowledge-preview="loadProjectKnowledgePreview"
            />
          </section>

          <section v-else class="workspace-stack">
            <ProjectMaterialsPanel
              :project-conversation-loading="projectConversationLoading"
              :project-conversation="projectConversation"
              :project-url-draft="projectUrlDraft"
              :project-text-draft="projectTextDraft"
              :project-file-draft="projectFileDraft"
              :project-selected-file="projectSelectedFile"
              :project-pending-materials="projectPendingMaterials"
              :project-material-knowledge-map="projectMaterialKnowledgeMap"
              :project-material-filter="projectMaterialFilter"
              :filtered-project-materials="filteredProjectMaterials"
              :project-materials-collapsed="projectMaterialsCollapsed"
              :can-save-project-materials="canSaveProjectMaterials"
              :can-upload-project-file="canUploadProjectFile"
              :project-material-preview="projectMaterialPreview"
              :project-knowledge-status-text="projectKnowledgeStatusText"
              @add-project-url-material="addProjectUrlMaterial"
              @add-project-text-material="addProjectTextMaterial"
              @clear-project-pending-materials="clearProjectPendingMaterials"
              @save-project-materials="saveProjectMaterials"
              @select-project-file="handleProjectFileSelect"
              @upload-project-file="uploadProjectFileMaterial"
              @delete-project-material="deleteProjectMaterial"
              @open-project-knowledge-detail="openProjectKnowledgeDetail"
              @open-project-knowledge-library="goProjectKnowledgeLibrary"
              @retry-project-knowledge-document="retryProjectKnowledgeDocument"
              @update-project-material-filter="projectMaterialFilter = $event"
              @toggle-project-materials-collapse="projectMaterialsCollapsed = !projectMaterialsCollapsed"
            />
          </section>
        </template>
      </main>
    </div>

    <ProjectKnowledgeDetailModal
      :visible="projectKnowledgeDetailVisible"
      :loading="projectKnowledgeDetailLoading"
      :reprocessing="projectKnowledgeReprocessing"
      :detail="projectKnowledgeDetail"
      :chunk-expanded="projectKnowledgeChunkExpanded"
      :visible-chunks="visibleProjectKnowledgeChunks"
      @close="closeProjectKnowledgeDetail"
      @reprocess="reprocessProjectKnowledgeDetail"
      @toggle-chunks="projectKnowledgeChunkExpanded = !projectKnowledgeChunkExpanded"
    />

    <div class="feedback-stack">
      <FeedbackPanel title="工作台建议" :message="workspaceAdvice" tone="warning" />
      <FeedbackPanel v-if="error" title="处理提示" :message="error" tone="danger" />
      <FeedbackPanel v-if="success" title="最新进展" :message="success" tone="success" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import EmptyWorkspaceState from '../components/projects/EmptyWorkspaceState.vue'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import ProjectAiWorkspacePanel from '../components/projects/ProjectAiWorkspacePanel.vue'
import ProjectKnowledgeDetailModal from '../components/projects/ProjectKnowledgeDetailModal.vue'
import ProjectMaterialsPanel from '../components/projects/ProjectMaterialsPanel.vue'
import ProjectMembersCard from '../components/projects/ProjectMembersCard.vue'
import ProjectOverviewPanel from '../components/projects/ProjectOverviewPanel.vue'
import ProjectsSidebar from '../components/projects/ProjectsSidebar.vue'
import ProjectWorkspaceHeader from '../components/projects/ProjectWorkspaceHeader.vue'
import { createProjectEditFormState, fillProjectEditForm } from '../components/projects/projectWorkspaceForm'
import { useProjectConversation } from '../components/projects/useProjectConversation'
import { useProjectKnowledgeDetail } from '../components/projects/useProjectKnowledgeDetail'
import { useProjectMaterials } from '../components/projects/useProjectMaterials'
import { useProjectWorkspaceAdvice } from '../components/projects/useProjectWorkspaceAdvice'
import { useProjectWorkspace } from '../components/projects/useProjectWorkspace'
import {
  memberStatusLabel,
  projectRoleLabel,
  projectStatusLabel,
  projectTypeLabel,
  visibilityLabel
} from '../components/projects/labels'
import type { MemberFormState, ProjectEditFormState } from '../components/projects/types'

const router = useRouter()
const route = useRoute()

const error = ref('')
const success = ref('')

const projectEditForm = reactive<ProjectEditFormState>(createProjectEditFormState())
const memberForm = reactive<MemberFormState>({
  selectedUserId: '',
  userId: '',
  projectRole: 'DEV'
})
const routeProjectId = computed(() => {
  const value = Number(route.query.projectId || 0)
  return Number.isFinite(value) && value > 0 ? value : null
})
let runResetProjectConversation = () => {}
let runResetProjectMaterials = () => {}
let runCloseProjectKnowledgeDetail = () => {}

const {
  loading,
  projects,
  userOptions,
  selectedProjectId,
  activeWorkspaceTab,
  selectedProject,
  resetProjectEditForm,
  requirementsOf,
  membersOf,
  loadProjects,
  loadUserOptions,
  selectProject,
  deleteProject,
  saveProjectEdit,
  addProjectMember,
  removeProjectMember
} = useProjectWorkspace({
  routeProjectId,
  error,
  success,
  projectEditForm,
  memberForm,
  resetProjectConversation: () => runResetProjectConversation(),
  resetProjectMaterials: () => runResetProjectMaterials(),
  closeProjectKnowledgeDetail: () => runCloseProjectKnowledgeDetail()
})

const {
  projectConversationLoading,
  projectConversationInput,
  projectConversation,
  aiUpdatedFields,
  projectMaterialKnowledgeMap,
  projectKnowledgePreviewVisible,
  projectKnowledgePreviewLoading,
  projectKnowledgePreview,
  canSendProjectConversation,
  projectKnowledgePreviewQueryText,
  refreshProjectMaterialKnowledgeStatuses,
  ensureProjectConversation,
  loadProjectKnowledgePreview,
  sendProjectConversation,
  applyConversationStructuredInfo,
  clearAiUpdatedFields,
  resetProjectConversation
} = useProjectConversation({
  selectedProject,
  projectEditForm,
  error,
  success
})

const {
  projectPendingMaterials,
  projectSelectedFile,
  projectMaterialFilter,
  projectMaterialsCollapsed,
  projectUrlDraft,
  projectTextDraft,
  projectFileDraft,
  canSaveProjectMaterials,
  canUploadProjectFile,
  filteredProjectMaterials,
  projectMaterialPreview,
  projectKnowledgeStatusText,
  handleProjectFileSelect,
  addProjectUrlMaterial,
  addProjectTextMaterial,
  clearProjectPendingMaterials,
  saveProjectMaterials,
  uploadProjectFileMaterial,
  deleteProjectMaterial,
  resetProjectMaterials
} = useProjectMaterials({
  projectConversation,
  projectConversationLoading,
  projectMaterialKnowledgeMap,
  error,
  success,
  ensureProjectConversation
})

const {
  projectKnowledgeDetailVisible,
  projectKnowledgeDetailLoading,
  projectKnowledgeReprocessing,
  projectKnowledgeDetail,
  projectKnowledgeChunkExpanded,
  visibleProjectKnowledgeChunks,
  openProjectKnowledgeDetail,
  reprocessProjectKnowledgeDetail,
  closeProjectKnowledgeDetail
} = useProjectKnowledgeDetail(error, success, refreshProjectMaterialKnowledgeStatuses)
runResetProjectConversation = resetProjectConversation
runResetProjectMaterials = resetProjectMaterials
runCloseProjectKnowledgeDetail = closeProjectKnowledgeDetail

const {
  pendingKnowledgeCount,
  workspaceAdvice
} = useProjectWorkspaceAdvice({
  selectedProject,
  activeWorkspaceTab,
  projectConversation,
  projectMaterialKnowledgeMap
})

async function handleWorkspaceTabChange(tab: 'overview' | 'ai' | 'materials') {
  activeWorkspaceTab.value = tab
  if ((tab === 'ai' || tab === 'materials') && !projectConversation.value) {
    await ensureProjectConversation()
  }
}

async function ensureAiWorkspace() {
  if (!selectedProject.value) {
    return
  }
  fillProjectEditForm(projectEditForm, selectedProject.value)
  clearAiUpdatedFields()
  activeWorkspaceTab.value = 'ai'
  error.value = ''
  success.value = ''
  await ensureProjectConversation()
}

async function startEditProject() {
  await ensureAiWorkspace()
}

function cancelProjectEdit() {
  activeWorkspaceTab.value = 'overview'
  clearAiUpdatedFields()
  if (selectedProject.value) {
    fillProjectEditForm(projectEditForm, selectedProject.value)
    return
  }
  resetProjectEditForm()
}

function goProjectKnowledgeLibrary(documentId: number) {
  void router.push({
    path: '/knowledge',
    query: {
      projectId: selectedProjectId.value ? String(selectedProjectId.value) : undefined,
      documentId: String(documentId)
    }
  })
}

async function retryProjectKnowledgeDocument(documentId: number) {
  projectConversationLoading.value = true
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/knowledge-documents/${documentId}/reprocess`)
    await refreshProjectMaterialKnowledgeStatuses()
    if (projectKnowledgeDetail.value?.document.id === documentId) {
      await openProjectKnowledgeDetail(documentId)
    }
    success.value = '知识文档已重新处理。'
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '重新处理知识文档失败。'
  } finally {
    projectConversationLoading.value = false
  }
}

watch(
  () => route.query.projectId,
  async (value) => {
    const projectId = Number(value || 0)
    if (projectId > 0 && projects.value.some((project) => project.id === projectId) && selectedProjectId.value !== projectId) {
      await selectProject(projectId)
    }
  }
)

onMounted(async () => {
  await Promise.all([loadProjects(), loadUserOptions()])
})
</script>

<style scoped>
.page {
  max-width: 1560px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #111827;
  background: #f4f7fb;
}

.layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.content {
  min-width: 0;
  display: grid;
  gap: 14px;
}

.workspace-stack {
  display: grid;
  gap: 14px;
}

.feedback-stack {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

@media (max-width: 1280px) {
  .layout {
    grid-template-columns: 320px minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
