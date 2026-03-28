<template>
  <div class="page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">模板中心</p>
        <h1>模板工作台</h1>
        <p class="hero-copy">统一管理需求模板、版本、正文和变量结构，为后续需求整理与文档生产提供稳定模板底座。</p>
      </div>
      <div class="hero-badges">
        <StatusBadge :label="`${templates.length} 个模板`" variant="info" />
        <StatusBadge :label="`${publishedCount} 个已发布`" variant="success" />
        <StatusBadge :label="selectedTemplate ? `当前：${selectedTemplate.templateCode}` : '请选择模板'" variant="ai" />
      </div>
    </section>

    <div class="layout">
      <main class="content">
        <WorkspaceSection
          eyebrow="模板目录"
          title="模板清单"
          description="先维护模板主记录，再围绕模板版本、正文和变量结构持续演进。"
          :tint="true"
        >
          <template #actions>
            <button class="primary" :disabled="loading" @click="createTemplate">新建模板</button>
          </template>

          <div v-if="templates.length > 0" class="template-grid">
            <button
              v-for="item in templates"
              :key="item.id"
              class="template-card"
              :class="{ 'template-card--active': selectedTemplateId === item.id }"
              @click="selectTemplate(item.id)"
            >
              <div class="template-card__head">
                <div>
                  <h3>{{ item.templateName }}</h3>
                  <p>{{ item.templateCode }}</p>
                </div>
                <StatusBadge :label="templateStatusLabel(item.status)" :variant="templateStatusVariant(item.status)" />
              </div>
              <p class="template-card__desc">{{ item.description || '暂未填写模板说明。' }}</p>
              <div class="template-card__meta">
                <span>{{ scopeLevelLabel(item.scopeLevel) }}</span>
                <span>最新版本 v{{ item.latestVersionNo || 0 }}</span>
              </div>
            </button>
          </div>

          <EmptyWorkspaceState
            v-else-if="!loading"
            eyebrow="模板中心"
            title="当前还没有模板"
            description="先创建一个基础模板，后面再补正文、变量和发布流程。"
          />
        </WorkspaceSection>
      </main>

      <aside class="sidebar">
        <WorkspaceSection
          eyebrow="模板详情"
          title="版本与编辑器"
          description="在这里查看版本列表、编辑正文与变量结构，并发布指定版本。"
        >
          <template v-if="detail && selectedTemplate">
            <div class="template-detail">
              <div class="template-detail__header">
                <div>
                  <h3>{{ selectedTemplate.templateName }}</h3>
                  <p>{{ selectedTemplate.templateCode }} · {{ scopeLevelLabel(selectedTemplate.scopeLevel) }}</p>
                </div>
                <StatusBadge :label="templateStatusLabel(selectedTemplate.status)" :variant="templateStatusVariant(selectedTemplate.status)" />
              </div>

              <p class="template-detail__copy">{{ selectedTemplate.description || '暂未填写模板说明。' }}</p>

              <div class="version-list">
                <div class="row between">
                  <strong>版本列表</strong>
                  <button class="ghost" :disabled="loading || !selectedTemplate" @click="createVersion">新增版本</button>
                </div>

                <button
                  v-for="version in detail.versions"
                  :key="version.id"
                  class="version-item"
                  :class="{ 'version-item--active': activeVersionId === version.id }"
                  @click="selectVersion(version.id)"
                >
                  <div class="version-item__head">
                    <strong>{{ version.versionLabel || `v${version.versionNo}` }}</strong>
                    <StatusBadge :label="templateStatusLabel(version.status)" :variant="templateStatusVariant(version.status)" />
                  </div>
                  <p>版本号 v{{ version.versionNo }}</p>
                  <p>{{ version.notes || '暂无版本说明。' }}</p>
                  <p v-if="version.isPublished" class="version-published">当前已发布版本</p>
                </button>
              </div>

              <div v-if="activeVersion" class="editor-card">
                <div class="row between">
                  <strong>版本编辑器</strong>
                  <div class="row">
                    <button class="ghost" :disabled="loading" @click="saveVersion">保存当前版本</button>
                    <button
                      v-if="!activeVersion.isPublished"
                      class="primary"
                      :disabled="loading"
                      @click="publishVersion(activeVersion.id)"
                    >
                      发布当前版本
                    </button>
                  </div>
                </div>

                <label class="field">
                  <span>版本标签</span>
                  <input v-model="editor.versionLabel" class="input" placeholder="例如 v2.0" />
                </label>

                <label class="field">
                  <span>版本说明</span>
                  <textarea v-model="editor.notes" class="textarea textarea--small" placeholder="记录本次模板版本变更说明。" />
                </label>

                <label class="field">
                  <span>模板正文 Markdown</span>
                  <textarea v-model="editor.contentMarkdown" class="textarea textarea--large" placeholder="维护模板正文结构。" />
                </label>

                <label class="field">
                  <span>变量结构 JSON</span>
                  <textarea
                    v-model="editor.variablesJson"
                    class="textarea textarea--medium"
                    placeholder='例如 [{"key":"projectBackground","label":"项目背景","required":true}]'
                  />
                </label>
              </div>
            </div>
          </template>

          <EmptyWorkspaceState
            v-else
            eyebrow="模板详情"
            title="请先选择模板"
            description="选择左侧模板后，这里会显示版本列表、编辑器和发布操作。"
          />
        </WorkspaceSection>

        <div class="feedback-stack">
          <FeedbackPanel title="错误提示" :message="error" tone="danger" />
          <FeedbackPanel title="最新进展" :message="success" tone="success" />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import EmptyWorkspaceState from '../components/projects/EmptyWorkspaceState.vue'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import StatusBadge from '../components/projects/StatusBadge.vue'
import WorkspaceSection from '../components/projects/WorkspaceSection.vue'
import { scopeLevelLabel, templateStatusLabel, templateStatusVariant } from '../components/template-center/helpers'
import type { ApiResponse, TemplateDetail, TemplateEditorForm, TemplateItem, TemplateVersionItem } from '../components/template-center/types'

const loading = ref(false)
const error = ref('')
const success = ref('')
const templates = ref<TemplateItem[]>([])
const selectedTemplateId = ref<number | null>(null)
const activeVersionId = ref<number | null>(null)
const detail = ref<TemplateDetail | null>(null)
const editor = reactive<TemplateEditorForm>({
  versionLabel: '',
  contentMarkdown: '',
  variablesJson: '',
  notes: '',
  status: 'DRAFT'
})

const selectedTemplate = computed(() => templates.value.find((item) => item.id === selectedTemplateId.value) || null)
const activeVersion = computed<TemplateVersionItem | null>(() => detail.value?.versions.find((item) => item.id === activeVersionId.value) || null)
const publishedCount = computed(() => templates.value.filter((item) => (item.status || '').toUpperCase() === 'PUBLISHED').length)

function clearTips() { error.value = ''; success.value = '' }
function showError(e: any, fallback: string) { error.value = e?.response?.data?.message || e?.message || fallback }

function syncEditor(version: TemplateVersionItem | null) {
  editor.versionLabel = version?.versionLabel || ''
  editor.contentMarkdown = version?.contentMarkdown || ''
  editor.variablesJson = version?.variablesJson || ''
  editor.notes = version?.notes || ''
  editor.status = version?.status || 'DRAFT'
}

async function loadTemplates() {
  loading.value = true
  clearTips()
  try {
    const res = await axios.get<ApiResponse<TemplateItem[]>>('/api/system/templates')
    templates.value = res.data.data || []
    if (!selectedTemplateId.value && templates.value.length > 0) {
      selectedTemplateId.value = templates.value[0].id
    }
    if (selectedTemplateId.value) {
      await loadDetail(selectedTemplateId.value)
    } else {
      detail.value = null
    }
  } catch (e: any) {
    showError(e, '加载模板列表失败。')
  } finally {
    loading.value = false
  }
}

async function loadDetail(templateId: number) {
  try {
    const res = await axios.get<ApiResponse<TemplateDetail>>(`/api/system/templates/${templateId}`)
    detail.value = res.data.data
    if (detail.value?.versions?.length) {
      const preferred = detail.value.versions.find((item) => item.isPublished) || detail.value.versions[0]
      activeVersionId.value = preferred.id
      syncEditor(preferred)
    } else {
      activeVersionId.value = null
      syncEditor(null)
    }
  } catch (e: any) {
    showError(e, '加载模板详情失败。')
  }
}

async function selectTemplate(templateId: number) {
  selectedTemplateId.value = templateId
  await loadDetail(templateId)
}

function selectVersion(versionId: number) {
  activeVersionId.value = versionId
  syncEditor(activeVersion.value)
}

async function createTemplate() {
  const templateCode = window.prompt('请输入模板编码，例如 PRD_BASE')
  if (!templateCode) return
  const templateName = window.prompt('请输入模板名称', '基础 PRD 模板')
  if (!templateName) return
  loading.value = true
  clearTips()
  try {
    const res = await axios.post<ApiResponse<number>>('/api/system/templates', {
      templateCode,
      templateName,
      templateCategory: 'PRD',
      scopeLevel: 'SYSTEM',
      status: 'DRAFT',
      initialVersionLabel: 'v1',
      contentMarkdown: '# PRD 模板\n\n## 1. 项目背景\n\n## 2. 用户与角色\n\n## 3. 核心流程\n',
      variablesJson: '[\n  {\"key\":\"projectBackground\",\"label\":\"项目背景\",\"required\":true}\n]',
      notes: '基础版模板'
    })
    success.value = '模板创建成功。'
    selectedTemplateId.value = res.data.data
    await loadTemplates()
  } catch (e: any) {
    showError(e, '创建模板失败。')
    loading.value = false
  }
}

async function createVersion() {
  if (!selectedTemplate.value) return
  const baseVersion = activeVersion.value
  const versionLabel = window.prompt('请输入版本标签', `v${(selectedTemplate.value.latestVersionNo || 0) + 1}`)
  if (!versionLabel) return
  loading.value = true
  clearTips()
  try {
    await axios.post(`/api/system/templates/${selectedTemplate.value.id}/versions`, {
      versionLabel,
      contentMarkdown: baseVersion?.contentMarkdown || '# PRD 模板\n',
      variablesJson: baseVersion?.variablesJson || '[]',
      notes: '从模板中心新增版本',
      status: 'DRAFT'
    })
    success.value = '模板版本创建成功。'
    await loadDetail(selectedTemplate.value.id)
    await loadTemplates()
  } catch (e: any) {
    showError(e, '创建模板版本失败。')
    loading.value = false
  }
}

async function saveVersion() {
  if (!selectedTemplate.value || !activeVersion.value) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/templates/${selectedTemplate.value.id}/versions/${activeVersion.value.id}`, {
      versionLabel: editor.versionLabel,
      contentMarkdown: editor.contentMarkdown,
      variablesJson: editor.variablesJson,
      notes: editor.notes,
      status: editor.status
    })
    success.value = '模板版本保存成功。'
    await loadDetail(selectedTemplate.value.id)
    await loadTemplates()
    return true
  } catch (e: any) {
    showError(e, '保存模板版本失败。')
    loading.value = false
    return false
  }
}

async function publishVersion(versionId: number) {
  if (!selectedTemplate.value) return
  if (!window.confirm('确认发布这个模板版本吗？发布后会覆盖当前已发布版本。')) return
  loading.value = true
  clearTips()
  try {
    const saved = await saveVersion()
    if (!saved) return
    await axios.post(`/api/system/templates/${selectedTemplate.value.id}/versions/${versionId}/publish`)
    success.value = '模板版本发布成功。'
    await loadDetail(selectedTemplate.value.id)
    await loadTemplates()
  } catch (e: any) {
    showError(e, '发布模板版本失败。')
    loading.value = false
  }
}

onMounted(loadTemplates)
</script>

<style scoped>
.page { display: grid; gap: 18px; }
.page-hero { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; padding: 28px; border-radius: 24px; background: linear-gradient(135deg, #eff6ff, #f8fafc); border: 1px solid #dbeafe; }
.eyebrow { margin: 0 0 8px; color: #0f766e; font-size: 12px; letter-spacing: 0.12em; text-transform: uppercase; font-weight: 700; }
.page-hero h1 { margin: 0; font-size: 32px; color: #0f172a; }
.hero-copy { margin: 10px 0 0; max-width: 720px; color: #475569; line-height: 1.7; }
.hero-badges { display: flex; flex-wrap: wrap; gap: 10px; justify-content: flex-end; }
.layout { display: grid; grid-template-columns: minmax(0, 1fr) 420px; gap: 18px; align-items: start; }
.content, .sidebar { display: grid; gap: 18px; }
.template-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; }
.template-card { width: 100%; text-align: left; border: 1px solid #dbe2ea; border-radius: 18px; padding: 16px; background: #fff; cursor: pointer; transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease; }
.template-card:hover, .template-card--active { border-color: #3b82f6; box-shadow: 0 10px 24px rgba(59, 130, 246, 0.12); transform: translateY(-1px); }
.template-card__head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.template-card__head h3 { margin: 0; font-size: 18px; color: #0f172a; }
.template-card__head p { margin: 4px 0 0; color: #64748b; font-size: 13px; }
.template-card__desc { margin: 12px 0; color: #475569; line-height: 1.6; min-height: 48px; }
.template-card__meta { display: flex; justify-content: space-between; gap: 10px; color: #64748b; font-size: 13px; }
.template-detail { display: grid; gap: 16px; }
.template-detail__header { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.template-detail__header h3 { margin: 0; font-size: 22px; color: #0f172a; }
.template-detail__header p { margin: 6px 0 0; color: #64748b; }
.template-detail__copy { margin: 0; color: #475569; line-height: 1.6; }
.version-list { display: grid; gap: 12px; }
.version-item { width: 100%; text-align: left; padding: 14px; border-radius: 16px; border: 1px solid #dbe2ea; background: #f8fafc; display: grid; gap: 8px; cursor: pointer; }
.version-item--active { border-color: #3b82f6; background: #eff6ff; }
.version-item__head { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.version-item p { margin: 0; color: #475569; line-height: 1.5; }
.version-published { color: #047857 !important; font-weight: 600; }
.editor-card { display: grid; gap: 12px; padding: 14px; border-radius: 16px; border: 1px solid #dbe2ea; background: #fff; }
.field { display: grid; gap: 6px; }
.field span { color: #334155; font-size: 13px; font-weight: 600; }
.input, .textarea { width: 100%; padding: 10px; box-sizing: border-box; border-radius: 10px; border: 1px solid #d1d5db; font: inherit; background: #fff; }
.textarea { resize: vertical; }
.textarea--small { min-height: 96px; }
.textarea--medium { min-height: 160px; font-family: Consolas, Monaco, monospace; }
.textarea--large { min-height: 280px; font-family: Consolas, Monaco, monospace; }
.feedback-stack { display: grid; gap: 12px; }
.row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.between { justify-content: space-between; }
.primary, .ghost { border-radius: 10px; border: 1px solid #d1d5db; padding: 10px 14px; cursor: pointer; font: inherit; }
.primary { background: #2563eb; border-color: #2563eb; color: #fff; }
.ghost { background: #f8fafc; }
@media (max-width: 1200px) {
  .layout { grid-template-columns: 1fr; }
}
</style>
