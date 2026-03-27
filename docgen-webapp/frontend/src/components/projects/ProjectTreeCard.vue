<template>
  <section class="card tree">
    <div class="tree-head">
      <h3>项目树</h3>
      <button class="ghost mini" :disabled="loading" @click="$emit('reload-projects')">刷新</button>
    </div>

    <div v-if="projects.length === 0" class="empty">暂无项目</div>

    <ul v-else class="tree-list">
      <li v-for="p in projects" :key="p.id" class="tree-project">
        <div class="tree-row">
          <button class="toggle" @click="$emit('toggle-project', p.id)">{{ isExpanded(p.id) ? '-' : '+' }}</button>
          <button
            class="tree-label"
            :class="{ active: selectedProjectId === p.id }"
            :title="p.projectName"
            @click="$emit('select-project', p.id)"
          >
            {{ p.projectKey }} / {{ p.projectName }}
          </button>
        </div>

        <ul v-if="isExpanded(p.id)" class="tree-children">
          <li v-if="requirementsOf(p.id).length === 0" class="empty small">暂无需求</li>
          <li v-for="(r, idx) in requirementsOf(p.id)" :key="r.id">
            <div class="tree-row req-row">
              <button
                class="tree-label req"
                :class="{ active: selectedRequirementId === r.id }"
                @click="$emit('select-requirement', p.id, r.id)"
              >
                需求 {{ idx + 1 }}：{{ r.title }}
              </button>
            </div>

            <ul v-if="selectedRequirementId === r.id && versionsOf(r.id).length > 0" class="tree-children ver">
              <li v-for="v in versionsOf(r.id)" :key="v.id" class="ver-item">
                {{ v.versionNo }} <span v-if="v.isCurrent" class="current">当前版本</span>
              </li>
            </ul>
          </li>
        </ul>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
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
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 12px; margin-bottom: 12px; }
.tree { background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); }
.tree-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.tree-list, .tree-children { list-style: none; margin: 10px 0 0; padding: 0; }
.tree-children { padding-left: 18px; border-left: 1px dashed #c7d2e0; }
.tree-row { display: flex; align-items: center; gap: 8px; margin: 6px 0; }
.toggle { width: 22px; height: 22px; border: 1px solid #d1d5db; border-radius: 6px; background: #f8fafc; cursor: pointer; }
.tree-label { flex: 1; text-align: left; border: 1px solid transparent; background: transparent; padding: 4px 6px; border-radius: 6px; cursor: pointer; color: #1f2937; }
.tree-label:hover { background: #eef4ff; }
.tree-label.active { background: #2563eb; color: #fff; }
.tree-label.req { font-size: 13px; }
.ver-item { color: #4b5563; font-size: 12px; margin: 5px 0; }
.current { color: #2563eb; font-weight: 600; }
.empty { color: #6b7280; }
.small { font-size: 12px; }
.ghost, .mini { border-radius: 8px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; background: #f3f4f6; }
.mini { padding: 5px 9px; font-size: 12px; }
</style>
