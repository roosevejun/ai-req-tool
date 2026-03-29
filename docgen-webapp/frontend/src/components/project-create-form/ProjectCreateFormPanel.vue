<template>
  <section class="panel">
    <div class="section-grid">
      <section class="form-section form-section--primary form-section--wide">
        <div class="section-head">
          <p class="section-eyebrow">基础标识</p>
          <h3>项目立项信息</h3>
        </div>

        <div class="field-grid field-grid--identity">
          <label class="field field-span-2">
            <span>项目 Key</span>
            <input v-model.trim="form.projectKey" class="input" placeholder="例如 AI-REQ" />
          </label>
          <label class="field field-span-2">
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
        </div>
      </section>

      <section class="form-section form-section--wide">
        <div class="section-head">
          <p class="section-eyebrow">业务背景</p>
          <h3>项目定位与背景</h3>
        </div>

        <div class="field-grid">
          <label class="field field-full">
            <span>项目描述</span>
            <textarea
              v-model.trim="form.description"
              class="input area"
              placeholder="简要说明项目目标、范围和当前阶段。"
            />
          </label>
          <label class="field field-full">
            <span>项目背景</span>
            <textarea
              v-model.trim="form.projectBackground"
              class="input area"
              placeholder="说明业务背景、立项目标和当前待解决的问题。"
            />
          </label>
          <label class="field field-full">
            <span>类似产品</span>
            <textarea
              v-model.trim="form.similarProducts"
              class="input area"
              placeholder="填写参考产品、竞品或内部参考方案。"
            />
          </label>
        </div>
      </section>

      <section class="form-section form-section--wide">
        <div class="section-head">
          <p class="section-eyebrow">价值判断</p>
          <h3>客户与价值信息</h3>
        </div>

        <div class="field-grid">
          <label class="field field-full">
            <span>目标客户群体</span>
            <textarea
              v-model.trim="form.targetCustomerGroups"
              class="input area"
              placeholder="描述目标用户、客户类型或核心使用角色。"
            />
          </label>
          <label class="field">
            <span>商业价值</span>
            <textarea
              v-model.trim="form.commercialValue"
              class="input area"
              placeholder="说明预期收益、价值和业务意义。"
            />
          </label>
          <label class="field">
            <span>核心产品价值</span>
            <textarea
              v-model.trim="form.coreProductValue"
              class="input area"
              placeholder="说明产品最核心的交付价值。"
            />
          </label>
        </div>
      </section>
    </div>

    <footer class="panel-footer">
      <p class="tip">如果当前信息还不完整，可随时切换到 AI 项目孵化，让系统通过持续对话帮助你继续提炼项目框架。</p>
      <button class="primary" type="button" :disabled="loading" @click="$emit('create')">
        {{ loading ? '正在创建项目...' : '创建项目' }}
      </button>
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
}>()
</script>

<style scoped>
.panel {
  padding: 0;
}

.section-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
}

.form-section {
  border: 1px solid #e3ebf3;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  padding: 18px;
}

.form-section--primary {
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  border-color: #d8e6fb;
}

.section-head {
  display: grid;
  gap: 2px;
  margin-bottom: 16px;
}

.section-eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field-grid--identity {
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.field-span-2 {
  grid-column: span 2;
}

.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d9e4;
  border-radius: 12px;
  padding: 11px 12px;
  background: #fff;
  transition: border-color 160ms ease, box-shadow 160ms ease;
}

.input:focus {
  outline: none;
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.area {
  min-height: 104px;
  resize: vertical;
}

.panel-footer {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #e8eef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.tip {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
  max-width: 780px;
}

.primary {
  border-radius: 12px;
  border: 1px solid #2563eb;
  padding: 11px 16px;
  cursor: pointer;
  font-weight: 700;
  background: #2563eb;
  color: #fff;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.14);
}

@media (max-width: 1080px) {
  .field-grid,
  .field-grid--identity {
    grid-template-columns: 1fr;
  }

  .field-span-2 {
    grid-column: auto;
  }
}

@media (max-width: 768px) {
  .panel-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
