<template>
  <li class="tree-project">
    <div class="tree-row">
      <button class="toggle" @click="$emit('toggle-project', project.id)">{{ expanded ? '-' : '+' }}</button>
      <button
        class="tree-label"
        :class="{ active: selectedProjectId === project.id }"
        :title="project.projectName"
        @click="$emit('select-project', project.id)"
      >
        {{ project.projectKey }} / {{ project.projectName }}
      </button>
    </div>

    <ul v-if="expanded" class="tree-children">
      <li v-if="requirements.length === 0" class="empty small">暂无需求</li>
      <li v-for="(requirement, idx) in requirements" :key="requirement.id">
        <div class="tree-row req-row">
          <button
            class="tree-label req"
            :class="{ active: selectedRequirementId === requirement.id }"
            @click="$emit('select-requirement', project.id, requirement.id)"
          >
            需求 {{ idx + 1 }}：{{ requirement.title }}
          </button>
        </div>

        <ul v-if="selectedRequirementId === requirement.id && versionsOf(requirement.id).length > 0" class="tree-children ver">
          <li v-for="version in versionsOf(requirement.id)" :key="version.id" class="ver-item">
            {{ version.versionNo }} <span v-if="version.isCurrent" class="current">当前版本</span>
          </li>
        </ul>
      </li>
    </ul>
  </li>
</template>

<script setup lang="ts">
import type { ProjectTreeProjectItem, ProjectTreeRequirementItem, ProjectTreeVersionItem } from './types'

defineProps<{
  project: ProjectTreeProjectItem
  expanded: boolean
  requirements: ProjectTreeRequirementItem[]
  selectedProjectId: number | null
  selectedRequirementId: number | null
  versionsOf: (requirementId: number) => ProjectTreeVersionItem[]
}>()

defineEmits<{
  (event: 'toggle-project', projectId: number): void
  (event: 'select-project', projectId: number): void
  (event: 'select-requirement', projectId: number, requirementId: number): void
}>()
</script>

<style scoped>
.tree-children {
  list-style: none;
  margin: 10px 0 0;
  padding: 0 0 0 18px;
  border-left: 1px dashed #c7d2e0;
}
.tree-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0;
}
.toggle {
  width: 22px;
  height: 22px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #f8fafc;
  cursor: pointer;
}
.tree-label {
  flex: 1;
  text-align: left;
  border: 1px solid transparent;
  background: transparent;
  padding: 4px 6px;
  border-radius: 6px;
  cursor: pointer;
  color: #1f2937;
}
.tree-label:hover {
  background: #eef4ff;
}
.tree-label.active {
  background: #2563eb;
  color: #fff;
}
.tree-label.req {
  font-size: 13px;
}
.ver-item {
  color: #4b5563;
  font-size: 12px;
  margin: 5px 0;
}
.current {
  color: #2563eb;
  font-weight: 600;
}
.empty {
  color: #6b7280;
}
.small {
  font-size: 12px;
}
</style>
