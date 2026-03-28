<template>
  <WorkspaceSection
    eyebrow="DocGen Kickoff"
    title="??????"
    description="???????????????????????AI ????????????? PRD?"
  >
    <details class="details">
      <summary>????????? PRD?????????????</summary>
      <p class="hint">AI ???? PRD ?????????????????????</p>
      <textarea
        v-model="previousPrdMarkdown"
        class="textarea"
        placeholder="????? PRD Markdown???"
      />
    </details>

    <textarea
      v-model="businessDescription"
      class="textarea"
      placeholder="???????????????? AI ???????????????????PRD ????????"
    />

    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('create-job')">
        {{ loading ? '???...' : '?? AI ??' }}
      </button>
      <button class="ghost" :disabled="loading" @click="$emit('reset')">??</button>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import WorkspaceSection from '../projects/WorkspaceSection.vue'
const businessDescription = defineModel<string>('businessDescription', { required: true })
const previousPrdMarkdown = defineModel<string>('previousPrdMarkdown', { required: true })

defineProps<{
  loading: boolean
}>()

defineEmits<{
  (event: 'create-job'): void
  (event: 'reset'): void
}>()
</script>

<style scoped>
.hint { color: #6b7280; margin-top: 8px; margin-bottom: 10px; }
.details { margin-bottom: 12px; }
.textarea { width: 100%; min-height: 140px; resize: vertical; padding: 10px; box-sizing: border-box; border-radius: 10px; border: 1px solid #e5e7eb; font-size: 14px; line-height: 1.4; }
.row { display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap; }
button { border-radius: 10px; padding: 10px 14px; border: 1px solid transparent; cursor: pointer; font-size: 14px; }
.primary { background: #2563eb; color: #fff; }
.ghost { background: #f3f4f6; color: #111827; border-color: #e5e7eb; }
</style>
