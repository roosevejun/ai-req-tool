<template>
  <aside class="sidebar">
    <ProjectTreeCard
      :loading="loading"
      :projects="projects"
      :selected-project-id="selectedProjectId"
      :selected-requirement-id="selectedRequirementId"
      :is-expanded="isExpanded"
      :requirements-of="requirementsOf"
      :versions-of="versionsOf"
      @reload-projects="$emit('reload-projects')"
      @toggle-project="$emit('toggle-project', $event)"
      @select-project="$emit('select-project', $event)"
      @select-requirement="(projectId, requirementId) => $emit('select-requirement', projectId, requirementId)"
    />
  </aside>
</template>

<script setup lang="ts">
import ProjectTreeCard from './ProjectTreeCard.vue'
import type { ProjectItem, RequirementItem, VersionItem } from './types'

defineProps<{
  loading: boolean
  projects: ProjectItem[]
  selectedProjectId: number | null
  selectedRequirementId: number | null
  isExpanded: (projectId: number) => boolean
  requirementsOf: (projectId: number) => RequirementItem[]
  versionsOf: (requirementId: number) => VersionItem[]
}>()

defineEmits<{
  (event: 'reload-projects'): void
  (event: 'toggle-project', projectId: number): void
  (event: 'select-project', projectId: number): void
  (event: 'select-requirement', projectId: number, requirementId: number): void
}>()
</script>

<style scoped>
.sidebar {
  min-height: 70vh;
}
</style>
