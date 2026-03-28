<template>
  <div class="page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">知识工作台</p>
        <h1>知识库</h1>
        <p class="hero-copy">统一查看项目知识文档、需求知识、处理状态、失败原因，以及资源预览和重跑入口。</p>
      </div>
      <div class="hero-badges">
        <StatusBadge :label="selectedProject ? `项目 #${selectedProject.id}` : '未选择项目'" :variant="selectedProject ? 'info' : 'warning'" />
        <StatusBadge :label="selectedRequirement ? `需求 #${selectedRequirement.id}` : '当前查看项目知识'" :variant="selectedRequirement ? 'ai' : 'neutral'" />
        <StatusBadge :label="`${documents.length} 份文档`" variant="success" />
      </div>
    </section>

    <div class="layout">
      <aside class="sidebar">
        <WorkspaceSection eyebrow="查看范围" title="选择知识范围" description="先选项目，再按需切换到某条需求的知识文档。" :tint="true">
          <div class="field">
            <label>项目</label>
            <select v-model="selectedProjectId" class="input">
              <option value="">请选择项目</option>
              <option v-for="project in projects" :key="project.id" :value="String(project.id)">{{ project.projectName }}</option>
            </select>
          </div>
          <div class="field">
            <label>需求</label>
            <select v-model="selectedRequirementId" class="input" :disabled="!selectedProjectId">
              <option value="">查看项目全部知识</option>
              <option v-for="item in requirements" :key="item.id" :value="String(item.id)">{{ item.title }}</option>
            </select>
          </div>
        </WorkspaceSection>

        <WorkspaceSection eyebrow="筛选条件" title="文档筛选" description="按状态和类型筛选当前范围内的知识文档。">
          <div class="quick-filters">
            <button v-for="item in quickFilters" :key="item.key" class="quick-filter" :class="{ 'quick-filter--active': activeQuickFilter === item.key }" @click="applyQuickFilter(item.key)">{{ item.label }}</button>
          </div>
          <div class="field">
            <label>状态</label>
            <select v-model="statusFilter" class="input">
              <option value="ALL">全部</option>
              <option value="READY">处理完成</option>
              <option value="PENDING">待处理</option>
              <option value="PROCESSING">处理中</option>
              <option value="FAILED">处理失败</option>
            </select>
          </div>
          <div class="field">
            <label>类型</label>
            <select v-model="typeFilter" class="input">
              <option value="ALL">全部</option>
              <option value="URL">网站链接</option>
              <option value="TEXT">文本资料</option>
              <option value="FILE">文件资料</option>
            </select>
          </div>
        </WorkspaceSection>

        <div class="feedback-stack">
          <FeedbackPanel title="下一步建议" :message="knowledgeAdvice" tone="warning" />
          <FeedbackPanel title="处理提示" :message="error" tone="danger" />
          <FeedbackPanel title="最新进展" :message="success" tone="success" />
        </div>
      </aside>

      <main class="content">
        <EmptyWorkspaceState v-if="!selectedProject" eyebrow="知识就绪" title="先选择一个项目" description="知识库会按项目或需求范围展示文档状态、摘要、资源和重跑能力。" />

        <template v-else>
          <KnowledgeLifecyclePanel
            :document-count="documents.length"
            :ready-count="readyCount"
            :pending-count="pendingCount"
            :failed-count="failedCount"
          />

          <WorkspaceSection eyebrow="知识概览" title="知识文档列表" :description="selectedRequirement ? `当前展示需求 #${selectedRequirement.id} 的知识文档。` : '当前展示项目级知识文档。'">
            <template #actions>
              <div class="metrics">
                <StatusBadge :label="`${readyCount} 完成`" variant="success" small />
                <StatusBadge :label="`${pendingCount} 待处理`" variant="warning" small />
                <StatusBadge :label="`${failedCount} 失败`" variant="danger" small />
              </div>
            </template>

            <div v-if="loading" class="placeholder">正在加载知识文档...</div>
            <div v-else-if="filteredDocuments.length === 0" class="placeholder">当前范围内还没有符合条件的知识文档。</div>
            <div v-else class="document-list">
              <article v-for="doc in filteredDocuments" :key="doc.id" class="document-card">
                <div class="document-head">
                  <div>
                    <p class="document-type">{{ documentTypeLabel(doc.documentType) }}</p>
                    <h3>{{ doc.title || `知识文档 #${doc.id}` }}</h3>
                    <p class="summary">{{ doc.summary || '当前暂无摘要，可打开详情查看原始资源和任务记录。' }}</p>
                  </div>
                  <div class="doc-status">
                    <StatusBadge :label="knowledgeStatusLabel(doc.status)" :variant="statusVariant(doc.status)" small />
                    <StatusBadge v-if="doc.latestTaskStatus" :label="`任务：${knowledgeStatusLabel(doc.latestTaskStatus)}`" :variant="statusVariant(doc.latestTaskStatus)" small />
                  </div>
                </div>

                <div class="document-meta">
                  <span v-if="doc.requirementId" class="meta-item">需求 #{{ doc.requirementId }}</span>
                  <span v-else-if="doc.projectId" class="meta-item">项目 #{{ doc.projectId }}</span>
                  <span v-if="doc.sourceUri" class="meta-item">{{ doc.sourceUri }}</span>
                  <span v-if="doc.sourceMaterialId" class="meta-item">资料 #{{ doc.sourceMaterialId }}</span>
                </div>

                <p v-if="doc.latestTaskError" class="error-text">{{ doc.latestTaskError }}</p>

                <div class="actions">
                  <button class="ghost" @click="openDetail(doc.id)">查看详情</button>
                  <button class="ghost" :disabled="reprocessingId === doc.id" @click="retryDocument(doc.id)">{{ reprocessingId === doc.id ? '处理中...' : '重新处理' }}</button>
                </div>
              </article>
            </div>
          </WorkspaceSection>
        </template>
      </main>
    </div>

    <KnowledgeLibraryDetailModal :visible="detailVisible" :loading="detailLoading" :reprocessing="detailReprocessing" :detail="detail" :chunk-expanded="chunkExpanded" :visible-chunks="visibleChunks" @close="closeDetail" @toggle-chunks="chunkExpanded = !chunkExpanded" @reprocess="reprocessDetail" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import EmptyWorkspaceState from '../components/projects/EmptyWorkspaceState.vue'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import KnowledgeLifecyclePanel from '../components/knowledge/KnowledgeLifecyclePanel.vue'
import StatusBadge from '../components/projects/StatusBadge.vue'
import WorkspaceSection from '../components/projects/WorkspaceSection.vue'
import KnowledgeLibraryDetailModal from '../components/knowledge/KnowledgeLibraryDetailModal.vue'
import { visibleChunks as toVisibleChunks } from '../components/project-create-ai/helpers'
import type { ApiResponse, KnowledgeDocumentChunk, KnowledgeDocumentDetail, KnowledgeDocumentListItem } from '../components/project-create-ai/types'
import type { ProjectItem, RequirementItem } from '../components/projects/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const success = ref('')
const projects = ref<ProjectItem[]>([])
const requirements = ref<RequirementItem[]>([])
const documents = ref<KnowledgeDocumentListItem[]>([])
const selectedProjectId = ref('')
const selectedRequirementId = ref('')
const statusFilter = ref<'ALL' | 'READY' | 'PENDING' | 'PROCESSING' | 'FAILED'>('ALL')
const typeFilter = ref<'ALL' | 'URL' | 'TEXT' | 'FILE'>('ALL')
const activeQuickFilter = ref<'ALL' | 'FAILED' | 'PENDING' | 'RECENT'>('ALL')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailReprocessing = ref(false)
const detail = ref<KnowledgeDocumentDetail | null>(null)
const chunkExpanded = ref(false)
const reprocessingId = ref<number | null>(null)

const selectedProject = computed(() => projects.value.find((item) => String(item.id) === selectedProjectId.value) || null)
const selectedRequirement = computed(() => requirements.value.find((item) => String(item.id) === selectedRequirementId.value) || null)
const quickFilters = [{ key: 'ALL' as const, label: '全部文档' }, { key: 'FAILED' as const, label: '失败项' }, { key: 'PENDING' as const, label: '待处理项' }, { key: 'RECENT' as const, label: '最近新增' }]
const filteredDocuments = computed(() => {
  const items = documents.value.filter((doc) => {
    const status = doc.status || doc.latestTaskStatus || ''
    if (statusFilter.value !== 'ALL' && normalizeStatus(status) !== statusFilter.value) return false
    if (typeFilter.value !== 'ALL' && normalizeType(doc.documentType) !== typeFilter.value) return false
    return true
  })
  return activeQuickFilter.value === 'RECENT' ? [...items].sort((a, b) => b.id - a.id).slice(0, 10) : items
})
const readyCount = computed(() => documents.value.filter((doc) => normalizeStatus(doc.status || doc.latestTaskStatus || '') === 'READY').length)
const pendingCount = computed(() => documents.value.filter((doc) => ['PENDING', 'PROCESSING'].includes(normalizeStatus(doc.status || doc.latestTaskStatus || ''))).length)
const failedCount = computed(() => documents.value.filter((doc) => normalizeStatus(doc.status || doc.latestTaskStatus || '') === 'FAILED').length)
const knowledgeAdvice = computed(() => {
  if (!selectedProject.value) return '先选择一个项目，再查看该项目或某条需求范围内的知识文档。'
  if (failedCount.value > 0) return `当前有 ${failedCount.value} 份知识文档处理失败，建议优先查看失败原因并重新处理。`
  if (pendingCount.value > 0) return `当前有 ${pendingCount.value} 份知识文档仍在处理中，可稍后刷新状态确认是否已可检索。`
  if (documents.value.length === 0) return '当前范围还没有知识文档，建议回到项目页或 AI 创建页补充资料。'
  return '当前知识链路运行正常，可以继续查看详情、资源预览和检索上下文。'
})
const visibleChunks = computed<KnowledgeDocumentChunk[]>(() => toVisibleChunks(detail.value, chunkExpanded.value))

function normalizeStatus(value?: string) { if (!value) return ''; if (value === 'SUCCESS') return 'READY'; if (value === 'RUNNING') return 'PROCESSING'; return value }
function normalizeType(value?: string) { if (!value) return ''; const upper = value.toUpperCase(); if (['URL', 'TEXT', 'FILE'].includes(upper)) return upper; if (upper.includes('URL')) return 'URL'; if (upper.includes('TEXT')) return 'TEXT'; return 'FILE' }
function documentTypeLabel(value?: string) { const normalized = normalizeType(value); if (normalized === 'URL') return '网站链接'; if (normalized === 'TEXT') return '文本资料'; if (normalized === 'FILE') return '文件资料'; return value || '未知类型' }
function knowledgeStatusLabel(value?: string) { const normalized = normalizeStatus(value); if (normalized === 'READY') return '处理完成'; if (normalized === 'FAILED') return '处理失败'; if (normalized === 'PROCESSING') return '处理中'; if (normalized === 'PENDING') return '待处理'; return value || '未知' }
function statusVariant(value?: string) { const normalized = normalizeStatus(value); if (normalized === 'READY') return 'success'; if (normalized === 'FAILED') return 'danger'; if (normalized === 'PROCESSING') return 'ai'; if (normalized === 'PENDING') return 'warning'; return 'neutral' }
function applyQuickFilter(filter: 'ALL' | 'FAILED' | 'PENDING' | 'RECENT') { activeQuickFilter.value = filter; if (filter === 'ALL') { statusFilter.value = 'ALL'; return } if (filter === 'FAILED') { statusFilter.value = 'FAILED'; return } if (filter === 'PENDING') { statusFilter.value = 'PENDING'; return } statusFilter.value = 'ALL' }
function syncQuickFilterFromStatus() { if (statusFilter.value === 'FAILED') { activeQuickFilter.value = 'FAILED'; return } if (statusFilter.value === 'PENDING' || statusFilter.value === 'PROCESSING') { activeQuickFilter.value = 'PENDING'; return } if (activeQuickFilter.value !== 'RECENT') activeQuickFilter.value = 'ALL' }

async function loadProjects() { const res = await axios.get<ApiResponse<ProjectItem[]>>('/api/projects'); projects.value = res.data.data || [] }
async function loadRequirements(projectId: string) { if (!projectId) { requirements.value = []; return } const res = await axios.get<ApiResponse<RequirementItem[]>>(`/api/projects/${projectId}/requirements`); requirements.value = res.data.data || [] }
async function loadDocuments() {
  if (!selectedProjectId.value) { documents.value = []; return }
  loading.value = true
  error.value = ''
  try {
    const path = selectedRequirementId.value ? `/api/knowledge-documents/requirements/${selectedRequirementId.value}` : `/api/knowledge-documents/projects/${selectedProjectId.value}`
    const res = await axios.get<ApiResponse<KnowledgeDocumentListItem[]>>(path)
    documents.value = res.data.data || []
  } catch (e: any) { error.value = e?.response?.data?.message || e?.message || '加载知识文档失败。' }
  finally { loading.value = false }
}
async function openDetail(documentId: number) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  await router.replace({ path: '/knowledge', query: { ...route.query, projectId: selectedProjectId.value || undefined, requirementId: selectedRequirementId.value || undefined, documentId: String(documentId) } })
  try { const res = await axios.get<ApiResponse<KnowledgeDocumentDetail>>(`/api/knowledge-documents/${documentId}`); detail.value = res.data.data }
  catch (e: any) { error.value = e?.response?.data?.message || e?.message || '加载知识文档详情失败。' }
  finally { detailLoading.value = false }
}
async function retryDocument(documentId: number) {
  reprocessingId.value = documentId
  error.value = ''
  success.value = ''
  try {
    await axios.post(`/api/knowledge-documents/${documentId}/reprocess`)
    await loadDocuments()
    if (detail.value?.document.id === documentId) await openDetail(documentId)
    success.value = '知识文档已重新处理。'
  } catch (e: any) { error.value = e?.response?.data?.message || e?.message || '重新处理知识文档失败。' }
  finally { reprocessingId.value = null }
}
async function reprocessDetail() {
  if (!detail.value) return
  detailReprocessing.value = true
  error.value = ''
  success.value = ''
  try {
    const documentId = detail.value.document.id
    const res = await axios.post<ApiResponse<KnowledgeDocumentDetail>>(`/api/knowledge-documents/${documentId}/reprocess`)
    detail.value = res.data.data
    await loadDocuments()
    success.value = '知识文档已重新处理。'
  } catch (e: any) { error.value = e?.response?.data?.message || e?.message || '重新处理知识文档失败。' }
  finally { detailReprocessing.value = false }
}
function closeDetail() { detailVisible.value = false; detail.value = null; chunkExpanded.value = false; detailReprocessing.value = false; const nextQuery = { ...route.query }; delete nextQuery.documentId; void router.replace({ path: '/knowledge', query: nextQuery }) }
async function applyRouteSelection() {
  const queryProjectId = String(route.query.projectId || '')
  const queryRequirementId = String(route.query.requirementId || '')
  const queryDocumentId = Number(route.query.documentId || 0)
  if (queryProjectId && selectedProjectId.value !== queryProjectId) { selectedProjectId.value = queryProjectId; await loadRequirements(queryProjectId) }
  if (queryRequirementId) selectedRequirementId.value = queryRequirementId
  if (queryDocumentId > 0 && (!detailVisible.value || detail.value?.document.id !== queryDocumentId)) await openDetail(queryDocumentId)
}
watch(selectedProjectId, async (projectId) => { selectedRequirementId.value = ''; await loadRequirements(projectId); await loadDocuments() })
watch(selectedRequirementId, async () => { await loadDocuments() })
watch(statusFilter, () => { syncQuickFilterFromStatus() })
onMounted(async () => { await loadProjects(); await applyRouteSelection() })
</script>

<style scoped>
.page { max-width: 1480px; margin: 18px auto; padding: 0 14px 18px; font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }
.page-hero { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 14px; padding: 18px; border: 1px solid #dbe2ea; border-radius: 20px; background: linear-gradient(135deg, #f8fcff 0%, #ffffff 55%); }
.eyebrow { margin: 0 0 6px; color: #0f766e; font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase; font-weight: 700; }
h1 { margin: 0; font-size: 32px; color: #0f172a; }
.hero-copy { margin: 10px 0 0; max-width: 720px; color: #64748b; line-height: 1.7; }
.hero-badges { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; }
.layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 14px; }
.sidebar, .feedback-stack { display: grid; gap: 14px; align-content: start; }
.field { display: grid; gap: 8px; margin-bottom: 12px; }
.quick-filters { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.quick-filter { border-radius: 999px; border: 1px solid #cbd5e1; background: #fff; padding: 7px 12px; cursor: pointer; color: #334155; }
.quick-filter--active { background: #0f172a; border-color: #0f172a; color: #fff; }
.field label { font-size: 13px; font-weight: 700; color: #334155; }
.input { width: 100%; box-sizing: border-box; padding: 10px 12px; border-radius: 10px; border: 1px solid #cbd5e1; background: #fff; }
.metrics { display: flex; flex-wrap: wrap; gap: 8px; }
.placeholder { color: #64748b; }
.document-list { display: grid; gap: 14px; }
.document-card { border: 1px solid #dbe2ea; border-radius: 18px; padding: 16px; background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); }
.document-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.document-type { margin: 0 0 6px; font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase; color: #0f766e; font-weight: 700; }
.document-card h3 { margin: 0; font-size: 20px; color: #0f172a; }
.summary { margin: 10px 0 0; color: #475569; line-height: 1.6; }
.doc-status { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; }
.document-meta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px; }
.meta-item { font-size: 12px; color: #64748b; }
.error-text { margin: 10px 0 0; color: #b91c1c; line-height: 1.6; }
.actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 14px; }
.ghost { border-radius: 10px; border: 1px solid #d1d5db; background: #fff; padding: 8px 12px; cursor: pointer; }
@media (max-width: 1080px) { .page-hero, .hero-badges { flex-direction: column; align-items: flex-start; } .layout { grid-template-columns: 1fr; } .document-head { flex-direction: column; } .doc-status { justify-content: flex-start; } }
</style>
