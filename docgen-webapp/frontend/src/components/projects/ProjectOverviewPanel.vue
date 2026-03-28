<template>
  <WorkspaceSection
    eyebrow="Project Overview"
    title="????"
    description="?????????????????????????????? AI ???"
  >
    <template #actions>
      <button class="ghost mini" :disabled="loading" @click="$emit('enter-ai')">?? AI ??</button>
      <button class="ghost mini" :disabled="loading" @click="$emit('start-edit')">????</button>
      <button class="danger mini" :disabled="loading" @click="$emit('delete-project')">????</button>
    </template>

    <div class="meta-grid">
      <div><strong>ID:</strong> {{ project.id }}</div>
      <div><strong>?? Key:</strong> {{ project.projectKey }}</div>
      <div><strong>????:</strong> {{ project.projectName }}</div>
      <div><strong>????:</strong> {{ projectTypeLabel(project.projectType) }}</div>
      <div><strong>???:</strong> {{ project.priority || '-' }}</div>
      <div><strong>???:</strong> {{ visibilityLabel(project.visibility) }}</div>
      <div><strong>????:</strong> {{ project.startDate || '-' }}</div>
      <div><strong>????:</strong> {{ project.targetDate || '-' }}</div>
      <div><strong>??:</strong> <StatusBadge :label="projectStatusLabel(project.status)" variant="info" small /></div>
      <div><strong>??:</strong> {{ project.tags || '-' }}</div>
    </div>

    <div class="description-card">
      <p class="section-label">????</p>
      <p class="summary">{{ project.description || '????????????' }}</p>
    </div>

    <div class="detail-grid">
      <article class="detail-section">
        <h4>????</h4>
        <p class="summary">{{ project.projectBackground || '????????????' }}</p>
      </article>
      <article class="detail-section">
        <h4>????</h4>
        <p class="summary">{{ project.similarProducts || '????????????' }}</p>
      </article>
      <article class="detail-section">
        <h4>??????</h4>
        <p class="summary">{{ project.targetCustomerGroups || '??????????????' }}</p>
      </article>
      <article class="detail-section">
        <h4>????</h4>
        <p class="summary">{{ project.commercialValue || '????????????' }}</p>
      </article>
      <article class="detail-section detail-section--wide">
        <h4>??????</h4>
        <p class="summary">{{ project.coreProductValue || '??????????????' }}</p>
      </article>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import WorkspaceSection from './WorkspaceSection.vue'
import type { ProjectItem } from './types'

defineProps<{
  loading: boolean
  project: ProjectItem
  projectTypeLabel: (value?: string) => string
  visibilityLabel: (value?: string) => string
  projectStatusLabel: (value?: string) => string
}>()

defineEmits<{
  (event: 'start-edit'): void
  (event: 'enter-ai'): void
  (event: 'delete-project'): void
}>()
</script>

<style scoped>
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  font-size: 14px;
  color: #334155;
}

.description-card,
.detail-section {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.description-card {
  margin-top: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.section-label {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.detail-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-section h4 {
  margin: 0 0 10px;
  color: #0f172a;
}

.detail-section--wide {
  grid-column: 1 / -1;
}

.summary {
  margin: 0;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
}

.ghost,
.danger {
  border-radius: 999px;
  padding: 8px 14px;
  border: 1px solid #cbd5e1;
  background: #fff;
  cursor: pointer;
}

.danger {
  border-color: #fecaca;
  color: #b91c1c;
  background: #fff5f5;
}

.mini {
  font-size: 13px;
}

@media (max-width: 860px) {
  .meta-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-section--wide {
    grid-column: auto;
  }
}
</style>
