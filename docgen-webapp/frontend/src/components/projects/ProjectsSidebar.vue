<template>
  <aside class="sidebar">
    <ProjectTreeCard
      :loading="loading"
      :projects="projects"
      :selected-project-id="selectedProjectId"
      @reload-projects="$emit('reload-projects')"
      @select-project="$emit('select-project', $event)"
    />
  </aside>
</template>

<script setup lang="ts">
import ProjectTreeCard from './ProjectTreeCard.vue'
import type { ProjectItem } from './types'

defineProps<{
  loading: boolean
  projects: ProjectItem[]
  selectedProjectId: number | null
}>()

defineEmits<{
  (event: 'reload-projects'): void
  (event: 'select-project', projectId: number): void
}>()
</script>

<style scoped>
.sidebar {
  min-height: 70vh;
  display: grid;
  gap: 14px;
  align-content: start;
}
</style>
