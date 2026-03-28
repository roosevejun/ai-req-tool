<template>
  <section class="panel">
    <header class="panel-header">
      <div>
        <p class="eyebrow">传统模式</p>
        <h2>表单创建项目</h2>
        <p class="summary">适合信息已经明确的项目，先用表单完成立项，后续再进入项目工作台继续协同。</p>
      </div>
      <div class="header-actions">
        <button class="ghost" type="button" @click="$emit('go-entry')">切换创建方式</button>
        <button class="ghost" type="button" @click="$emit('go-ai')">改用 AI 创建</button>
      </div>
    </header>

    <div class="form-grid">
      <label class="field">
        <span>项目 Key</span>
        <input v-model.trim="form.projectKey" class="input" placeholder="例如 AI-REQ" />
      </label>
      <label class="field">
        <span>项目名称</span>
        <input v-model.trim="form.projectName" class="input" placeholder="请输入项目名称" />
      </label>
      <label class="field">
        <span>项目类型</span>
        <select v-model="form.projectType" class="input">
          <option v-for="option in projectTypeOptions" :key="option.value || 'empty'" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
      <label class="field">
        <span>优先级</span>
        <select v-model="form.priority" class="input">
          <option v-for="option in priorityOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
      <label class="field">
        <span>计划开始日期</span>
        <input v-model="form.startDate" type="date" class="input" />
      </label>
      <label class="field">
        <span>计划结束日期</span>
        <input v-model="form.targetDate" type="date" class="input" />
      </label>
      <label class="field">
        <span>负责人</span>
        <select v-model="form.ownerUserId" class="input">
          <option value="">默认当前登录用户</option>
          <option v-for="user in userOptions" :key="user.id" :value="String(user.id)">
            {{ user.displayName || user.username }} ({{ user.username }})
          </option>
        </select>
      </label>
      <label class="field">
        <span>可见性</span>
        <select v-model="form.visibility" class="input">
          <option v-for="option in visibilityOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
      <label class="field field-full">
        <span>标签</span>
        <input v-model.trim="form.tags" class="input" placeholder="逗号分隔，例如 AI, 文档平台, 需求管理" />
      </label>
      <label class="field field-full">
        <span>项目描述</span>
        <textarea v-model.trim="form.description" class="input area" placeholder="简要描述项目目标、范围和当前阶段。" />
      </label>
      <label class="field field-full">
        <span>项目背景</span>
        <textarea v-model.trim="form.projectBackground" class="input area" placeholder="说明业务背景、立项目标和当前问题。" />
      </label>
      <label class="field field-full">
        <span>类似产品</span>
        <textarea v-model.trim="form.similarProducts" class="input area" placeholder="填写参考产品、竞品或内部参考方案。" />
      </label>
      <label class="field field-full">
        <span>目标客户群体</span>
        <textarea v-model.trim="form.targetCustomerGroups" class="input area" placeholder="描述目标用户、客户类型或核心使用角色。" />
      </label>
      <label class="field">
        <span>商业价值</span>
        <textarea v-model.trim="form.commercialValue" class="input area" placeholder="说明预期收益、价值和业务意义。" />
      </label>
      <label class="field">
        <span>核心产品价值</span>
        <textarea v-model.trim="form.coreProductValue" class="input area" placeholder="说明产品最核心的交付价值。" />
      </label>
    </div>

    <footer class="panel-footer">
      <p class="tip">传统模式更适合快速录入。项目创建完成后，仍可以在项目工作台进入 AI 协同继续优化。</p>
      <button class="primary" type="button" :disabled="loading" @click="$emit('create')">创建项目</button>
    </footer>
  </section>
</template>

<script setup lang="ts">
import type { ProjectFormState, UserOption } from '../projects/types'
import { priorityOptions, projectTypeOptions, visibilityOptions } from './helpers'

defineProps<{
  loading: boolean
  form: ProjectFormState
  userOptions: UserOption[]
}>()

defineEmits<{
  (event: 'create'): void
  (event: 'go-ai'): void
  (event: 'go-entry'): void
}>()
</script>

<style scoped>
.panel {
  border: 1px solid #dbe2ea;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  padding: 22px;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}
.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}
h2 {
  margin: 0;
  font-size: 30px;
  color: #0f172a;
}
.summary {
  margin: 10px 0 0;
  max-width: 760px;
  color: #475569;
  line-height: 1.7;
}
.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.field {
  display: grid;
  gap: 6px;
}
.field span {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}
.field-full {
  grid-column: 1 / -1;
}
.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fff;
}
.area {
  min-height: 96px;
  resize: vertical;
}
.panel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 18px;
}
.tip {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}
.primary,
.ghost {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 10px 14px;
  cursor: pointer;
}
.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.ghost {
  background: #f8fafc;
  color: #0f172a;
}
@media (max-width: 960px) {
  .panel-header,
  .panel-footer {
    flex-direction: column;
    align-items: flex-start;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
