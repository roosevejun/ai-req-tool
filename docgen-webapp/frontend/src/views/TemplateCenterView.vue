<template>
  <div class="page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">模板中心</p>
        <h1>文档标准中心</h1>
        <p class="hero-copy">
          在这里统一管理需求模板、版本、正文和变量结构，让后续需求整理和文档生成都建立在稳定标准上。
        </p>
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
          title="标准模板清单"
          description="模板先定义文档标准，再驱动需求整理和文档生成。建议优先维护模板说明、发布版本和变量结构。"
          :tint="true"
        >
          <template #actions>
            <button class="primary" :disabled="loading" @click="createTemplate">新建模板</button>
          </template>

          <div class="catalog-summary">
            <article class="summary-card">
              <span>模板总数</span>
              <strong>{{ templates.length }}</strong>
            </article>
            <article class="summary-card">
              <span>已发布</span>
              <strong>{{ publishedCount }}</strong>
            </article>
            <article class="summary-card">
              <span>草稿模板</span>
              <strong>{{ draftCount }}</strong>
            </article>
          </div>

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
              <p class="template-card__desc">
                {{ item.description || '当前还没有模板说明，建议补充模板适用场景和输出目标。' }}
              </p>
              <div class="template-card__meta">
                <span>{{ scopeLevelLabel(item.scopeLevel) }}</span>
                <span>{{ templateUsageHint(item) }}</span>
              </div>
            </button>
          </div>

          <EmptyWorkspaceState
            v-else-if="!loading"
            eyebrow="模板中心"
            title="还没有模板"
            description="先创建一个基础模板，再逐步补充正文、变量结构和发布版本。"
          />
        </WorkspaceSection>
      </main>

      <aside class="sidebar">
        <WorkspaceSection
          eyebrow="标准详情"
          title="版本与编辑器"
          description="在这里查看版本、维护正文和变量结构，并决定哪个版本可以成为默认标准。"
        >
          <template v-if="detail && selectedTemplate">
            <div class="template-detail">
              <div class="template-detail__header">
                <div>
                  <h3>{{ selectedTemplate.templateName }}</h3>
                  <p>{{ selectedTemplate.templateCode }} · {{ scopeLevelLabel(selectedTemplate.scopeLevel) }}</p>
                </div>
                <StatusBadge
                  :label="templateStatusLabel(selectedTemplate.status)"
                  :variant="templateStatusVariant(selectedTemplate.status)"
                />
              </div>

              <p class="template-detail__copy">
                {{ selectedTemplate.description || '当前还没有模板说明，建议先写明该模板面向的文档类型和使用场景。' }}
              </p>

              <div class="governance-card">
                <p class="governance-title">标准建议</p>
                <ul>
                  <li>{{ templateUsageHint(selectedTemplate) }}</li>
                  <li v-if="!detail.publishedVersion">当前还没有发布版，建议确认一版后发布，避免需求整理使用不稳定草稿。</li>
                  <li v-else>当前默认发布版是 {{ detail.publishedVersion.versionLabel || `v${detail.publishedVersion.versionNo}` }}。</li>
                </ul>
              </div>

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
                    <StatusBadge :label="templateStatusLabel(version.status)" :variant="templateStatusVariant(version.status)" small />
                  </div>
                  <p>版本号 v{{ version.versionNo }}</p>
                  <p>{{ version.notes || '当前还没有版本说明，建议记录本次模板变更重点。' }}</p>
                  <p v-if="version.isPublished" class="version-published">当前默认发布版本</p>
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
                  <input v-model="editor.versionLabel" class="input" placeholder="例如 v2.0 / PRD-基础版" />
                </label>

                <label class="field">
                  <span>版本说明</span>
                  <textarea v-model="editor.notes" class="textarea textarea--small" placeholder="记录本次模板变更点、适用范围和注意事项。" />
                </label>

                <label class="field">
                  <span>模板正文 Markdown</span>
                  <textarea v-model="editor.contentMarkdown" class="textarea textarea--large" placeholder="维护文档结构、章节标题和默认说明文案。" />
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
            description="选择左侧模板后，这里会显示版本、标准建议和模板编辑器。"
          />
        </WorkspaceSection>

        <div class="feedback-stack">
          <FeedbackPanel title="下一步建议" :message="templateAdvice" tone="warning" />
          <FeedbackPanel title="处理提示" :message="error" tone="danger" />
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
import { scopeLevelLabel, templateStatusLabel, templateStatusVariant, templateUsageHint } from '../components/template-center/helpers'
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
const draftCount = computed(() => templates.value.filter((item) => (item.status || '').toUpperCase() === 'DRAFT').length)
const templateAdvice = computed(() => {
  if (templates.value.length === 0) return '先创建一个基础模板，再补正文、变量结构和发布版本，后续需求整理才有稳定标准。'
  if (!selectedTemplate.value) return '先选择一个模板，再查看版本和编辑器。'
  if (!detail.value?.publishedVersion) return '当前模板还没有发布版，建议先确认一版并发布，避免需求整理使用不稳定草稿。'
  if (!activeVersion.value) return '先选择一个版本，再继续维护模板正文和变量结构。'
  return '当前模板标准链路已经建立，可以继续优化变量结构和版本说明，让需求整理更稳定。'
})

function clearTips() {
  error.value = ''
  success.value = ''
}

function showError(e: any, fallback: string) {
  error.value = e?.response?.data?.message || e?.message || fallback
}

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
      variablesJson: '[\n  {"key":"projectBackground","label":"项目背景","required":true}\n]',
      notes: '基础模板版本'
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
  if (!selectedTemplate.value || !activeVersion.value) return false
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
  if (!window.confirm('确认发布这个模板版本吗？发布后会覆盖当前默认发布版。')) return
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
.layout { display: grid; grid-template-columns: minmax(0, 1fr) 440px; gap: 18px; align-items: start; }
.content, .sidebar { display: grid; gap: 18px; min-width: 0; }
.catalog-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.summary-card { border: 1px solid #dbe2ea; border-radius: 14px; padding: 14px; background: #fff; }
.summary-card span { display: block; font-size: 12px; color: #64748b; }
.summary-card strong { display: block; margin-top: 8px; color: #0f172a; font-size: 24px; }
.template-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; }
.template-card { width: 100%; text-align: left; border: 1px solid #dbe2ea; border-radius: 18px; padding: 16px; background: #fff; cursor: pointer; transition: border-color .2s ease, transform .2s ease, box-shadow .2s ease; }
.template-card:hover, .template-card--active { border-color: #3b82f6; box-shadow: 0 10px 24px rgba(59, 130, 246, 0.12); transform: translateY(-1px); }
.template-card__head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.template-card__head h3 { margin: 0; font-size: 18px; color: #0f172a; }
.template-card__head p { margin: 4px 0 0; color: #64748b; font-size: 13px; }
.template-card__desc { margin: 12px 0; color: #475569; line-height: 1.6; min-height: 48px; }
.template-card__meta { display: grid; gap: 6px; color: #64748b; font-size: 12px; }
.template-detail { display: grid; gap: 16px; }
.template-detail__header { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.template-detail__header h3 { margin: 0; font-size: 22px; color: #0f172a; }
.template-detail__header p { margin: 6px 0 0; color: #64748b; }
.template-detail__copy { margin: 0; color: #475569; line-height: 1.7; }
.governance-card { border: 1px solid #dbe2ea; border-radius: 14px; padding: 14px; background: linear-gradient(180deg, #f8fcff 0%, #ffffff 100%); }
.governance-title { margin: 0 0 10px; color: #0f172a; font-weight: 700; }
.governance-card ul { margin: 0; padding-left: 18px; color: #475569; line-height: 1.8; }
.version-list { display: grid; gap: 12px; }
.version-item { text-align: left; border: 1px solid #dbe2ea; border-radius: 14px; padding: 12px; background: #fff; cursor: pointer; }
.version-item--active { border-color: #2563eb; background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%); }
.version-item__head { display: flex; justify-content: space-between; gap: 10px; align-items: center; margin-bottom: 8px; }
.version-item p { margin: 0 0 6px; color: #475569; line-height: 1.6; }
.version-published { color: #166534; font-weight: 700; }
.editor-card { display: grid; gap: 14px; border: 1px solid #dbe2ea; border-radius: 16px; padding: 16px; background: #fff; }
.field { display: grid; gap: 8px; }
.field span { font-size: 13px; font-weight: 700; color: #334155; }
.input, .textarea { width: 100%; box-sizing: border-box; padding: 10px 12px; border-radius: 10px; border: 1px solid #cbd5e1; background: #fff; }
.textarea { resize: vertical; line-height: 1.6; }
.textarea--small { min-height: 88px; }
.textarea--medium { min-height: 180px; font-family: "Consolas", "SFMono-Regular", monospace; }
.textarea--large { min-height: 260px; }
.row { display: flex; gap: 10px; flex-wrap: wrap; }
.between { justify-content: space-between; align-items: center; }
.feedback-stack { display: grid; gap: 12px; }
.primary, .ghost { border-radius: 10px; border: 1px solid #d1d5db; padding: 10px 14px; cursor: pointer; }
.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.ghost { background: #f8fafc; }
@media (max-width: 1180px) {
  .page-hero, .hero-badges, .template-detail__header, .between { flex-direction: column; align-items: flex-start; }
  .layout, .catalog-summary { grid-template-columns: 1fr; }
}
</style>
