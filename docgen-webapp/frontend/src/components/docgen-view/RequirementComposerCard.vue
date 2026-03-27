<template>
  <section class="card">
    <h3>当前上下文</h3>
    <div class="meta-grid">
      <div><strong>项目 ID：</strong> {{ selectedProjectId || '-' }}</div>
      <div><strong>需求 ID：</strong> {{ selectedRequirementId || '-' }}</div>
    </div>
  </section>

  <section v-if="selectedProjectId" class="card">
    <h3>为项目 {{ selectedProjectId }} 创建需求</h3>
    <div class="form-grid">
      <input v-model.trim="reqForm.title" class="input" placeholder="需求标题" />
      <select v-model="reqForm.priority" class="input">
        <option value="P0">P0 - 紧急</option>
        <option value="P1">P1 - 高</option>
        <option value="P2">P2 - 中</option>
        <option value="P3">P3 - 低</option>
      </select>
      <select v-model="reqForm.status" class="input">
        <option value="DRAFT">草稿</option>
        <option value="CLARIFYING">澄清中</option>
        <option value="READY_REVIEW">待评审</option>
        <option value="DONE">已完成</option>
      </select>
    </div>
    <textarea v-model="reqForm.summary" class="input" placeholder="需求摘要" />
    <div class="row">
      <button class="primary" :disabled="loading || !reqForm.title" @click="$emit('create-requirement')">创建需求</button>
      <button class="ghost" :disabled="loading" @click="$emit('refresh-requirements')">刷新列表</button>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  loading: boolean
  selectedProjectId: number | null
  selectedRequirementId: number | null
  reqForm: {
    title: string
    summary: string
    priority: string
    status: string
  }
}>()

defineEmits<{
  (event: 'create-requirement'): void
  (event: 'refresh-requirements'): void
}>()
</script>

<style scoped>
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 12px; margin-bottom: 12px; }
.meta-grid { display: grid; grid-template-columns: repeat(2, minmax(180px, 1fr)); gap: 10px; font-size: 14px; }
.form-grid { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 10px; }
.input { width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 8px; padding: 8px 10px; margin-top: 8px; background: #fff; }
textarea.input { min-height: 72px; resize: vertical; }
.row { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.primary, .ghost { border-radius: 8px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.ghost { background: #f3f4f6; }
@media (max-width: 980px) { .form-grid, .meta-grid { grid-template-columns: 1fr; } }
</style>
