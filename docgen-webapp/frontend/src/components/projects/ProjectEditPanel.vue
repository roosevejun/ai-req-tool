<template>
  <section class="panel">
    <div class="panel-head">
      <div>
        <p class="eyebrow">项目编辑</p>
        <h4>项目信息表单</h4>
      </div>
      <button class="danger mini" :disabled="loading" @click="$emit('delete-project')">删除项目</button>
    </div>

    <p class="summary">这里是最终落表的数据区。建议先通过右侧对话补全信息，再把 AI 结果回填到这里确认并保存。</p>

    <div class="meta-grid compact-grid">
      <div><strong>ID:</strong> {{ project.id }}</div>
      <div><strong>项目 Key:</strong> {{ project.projectKey }}</div>
    </div>

    <div class="section-card">
      <div class="section-head">
        <div>
          <h5>核心项目信息</h5>
          <p>先确认项目名称、描述和类型，这些是 AI 回填的主要落点。</p>
        </div>
        <span class="section-badge">优先确认</span>
      </div>

      <div class="form-grid">
        <label class="field" :class="{ highlighted: isAiUpdated('projectName') }">
          <span class="field-label">
            项目名称
            <span v-if="isAiUpdated('projectName')" class="ai-tag">AI 刚更新</span>
          </span>
          <input v-model.trim="projectEditForm.projectName" class="input" placeholder="项目名称" />
        </label>

        <label class="field">
          <span class="field-label">项目类型</span>
          <select v-model="projectEditForm.projectType" class="input">
            <option value="">请选择项目类型</option>
            <option value="PRODUCT">产品型</option>
            <option value="PLATFORM">平台型</option>
            <option value="OPS">运营型</option>
            <option value="INTEGRATION">集成型</option>
          </select>
        </label>
      </div>

      <label class="field" :class="{ highlighted: isAiUpdated('description') }">
        <span class="field-label">
          项目描述
          <span v-if="isAiUpdated('description')" class="ai-tag">AI 刚更新</span>
        </span>
        <textarea v-model="projectEditForm.description" class="input textarea" placeholder="项目描述" />
      </label>

      <label class="field" :class="{ highlighted: isAiUpdated('projectBackground') }">
        <span class="field-label">
          项目背景
          <span v-if="isAiUpdated('projectBackground')" class="ai-tag">AI 刚更新</span>
        </span>
        <textarea v-model="projectEditForm.projectBackground" class="input textarea" placeholder="项目背景" />
      </label>
    </div>

    <div class="section-card">
      <div class="section-head">
        <div>
          <h5>客户与价值判断</h5>
          <p>这一组字段用于承接 AI 沟通整理后的业务理解。</p>
        </div>
      </div>

      <div class="field-stack">
        <label class="field" :class="{ highlighted: isAiUpdated('targetCustomerGroups') }">
          <span class="field-label">
            目标客户群体
            <span v-if="isAiUpdated('targetCustomerGroups')" class="ai-tag">AI 刚更新</span>
          </span>
          <textarea
            v-model="projectEditForm.targetCustomerGroups"
            class="input textarea"
            placeholder="目标客户群体"
          />
        </label>

        <label class="field" :class="{ highlighted: isAiUpdated('commercialValue') }">
          <span class="field-label">
            商业价值
            <span v-if="isAiUpdated('commercialValue')" class="ai-tag">AI 刚更新</span>
          </span>
          <textarea v-model="projectEditForm.commercialValue" class="input textarea" placeholder="商业价值" />
        </label>

        <label class="field" :class="{ highlighted: isAiUpdated('coreProductValue') }">
          <span class="field-label">
            核心产品价值
            <span v-if="isAiUpdated('coreProductValue')" class="ai-tag">AI 刚更新</span>
          </span>
          <textarea v-model="projectEditForm.coreProductValue" class="input textarea" placeholder="核心产品价值" />
        </label>

        <label class="field" :class="{ highlighted: isAiUpdated('similarProducts') }">
          <span class="field-label">
            类似产品或竞品信息
            <span v-if="isAiUpdated('similarProducts')" class="ai-tag">AI 刚更新</span>
          </span>
          <textarea
            v-model="projectEditForm.similarProducts"
            class="input textarea"
            placeholder="类似产品或竞品信息"
          />
        </label>
      </div>
    </div>

    <div class="section-card">
      <div class="section-head">
        <div>
          <h5>管理属性</h5>
          <p>这里保留负责人、时间、优先级等管理字段，便于在确认业务信息后统一保存。</p>
        </div>
      </div>

      <div class="form-grid form-grid-wide">
        <label class="field">
          <span class="field-label">优先级</span>
          <select v-model="projectEditForm.priority" class="input">
            <option value="P0">P0</option>
            <option value="P1">P1</option>
            <option value="P2">P2</option>
            <option value="P3">P3</option>
          </select>
        </label>

        <label class="field">
          <span class="field-label">可见范围</span>
          <select v-model="projectEditForm.visibility" class="input">
            <option value="PRIVATE">私有</option>
            <option value="ORG">组织内</option>
          </select>
        </label>

        <label class="field">
          <span class="field-label">项目状态</span>
          <select v-model="projectEditForm.status" class="input">
            <option value="ACTIVE">进行中</option>
            <option value="ARCHIVED">已归档</option>
            <option value="PAUSED">已暂停</option>
          </select>
        </label>

        <label class="field">
          <span class="field-label">负责人</span>
          <select v-model="projectEditForm.ownerUserId" class="input">
            <option value="">请选择负责人</option>
            <option v-for="user in userOptions" :key="user.id" :value="String(user.id)">
              {{ user.displayName || user.username }} ({{ user.username }})
            </option>
          </select>
        </label>

        <label class="field">
          <span class="field-label">开始日期</span>
          <input v-model="projectEditForm.startDate" type="date" class="input" />
        </label>

        <label class="field">
          <span class="field-label">目标日期</span>
          <input v-model="projectEditForm.targetDate" type="date" class="input" />
        </label>

        <label class="field field-full">
          <span class="field-label">标签</span>
          <input v-model.trim="projectEditForm.tags" class="input" placeholder="标签，多个用逗号分隔" />
        </label>
      </div>
    </div>

    <div class="row">
      <button class="primary mini" :disabled="loading || !projectEditForm.projectName" @click="$emit('save-edit')">保存修改</button>
      <button class="ghost mini" :disabled="loading" @click="$emit('cancel-edit')">取消编辑</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ProjectEditFormState, ProjectItem, UserOption } from './types'

const props = defineProps<{
  loading: boolean
  project: ProjectItem
  projectEditForm: ProjectEditFormState
  userOptions: UserOption[]
  aiUpdatedFields: Array<keyof ProjectEditFormState>
}>()

defineEmits<{
  (event: 'cancel-edit'): void
  (event: 'save-edit'): void
  (event: 'delete-project'): void
}>()

function isAiUpdated(field: keyof ProjectEditFormState) {
  return props.aiUpdatedFields.includes(field)
}
</script>

<style scoped>
.panel {
  border: 1px solid #dbe5ef;
  border-radius: 16px;
  background: #fff;
  padding: 16px;
  display: grid;
  gap: 14px;
}

.panel-head,
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.row {
  flex-wrap: wrap;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

h4 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.summary,
.compact-grid {
  color: #475569;
  line-height: 1.65;
}

.section-card {
  border: 1px solid #dbe5ef;
  border-radius: 16px;
  padding: 14px;
  display: grid;
  gap: 12px;
  background: #fcfdff;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.section-head h5 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
}

.section-head p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.6;
  font-size: 13px;
}

.section-badge,
.ai-tag {
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.section-badge {
  background: #e0f2fe;
  color: #075985;
  padding: 6px 10px;
}

.meta-grid,
.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-grid-wide {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.field-stack {
  display: grid;
  gap: 10px;
}

.field {
  display: grid;
  gap: 8px;
}

.field-full {
  grid-column: 1 / -1;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.highlighted {
  border-radius: 14px;
  padding: 12px;
  background: #effdf5;
  border: 1px solid #bbf7d0;
}

.ai-tag {
  background: #dcfce7;
  color: #166534;
  padding: 4px 8px;
}

.input {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 10px 12px;
  font: inherit;
  color: #0f172a;
  background: #fff;
  box-sizing: border-box;
}

.textarea {
  min-height: 108px;
  resize: vertical;
}

.primary,
.ghost,
.danger {
  border-radius: 999px;
  padding: 8px 14px;
  border: 1px solid transparent;
  cursor: pointer;
}

.primary {
  background: #1d4ed8;
  color: #fff;
}

.ghost {
  background: #fff;
  border-color: #cbd5e1;
  color: #334155;
}

.danger {
  background: #fff5f5;
  border-color: #fecaca;
  color: #b91c1c;
}

.mini {
  font-size: 13px;
}

@media (max-width: 860px) {
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .section-head {
    flex-direction: column;
  }

  .meta-grid,
  .form-grid,
  .form-grid-wide {
    grid-template-columns: 1fr;
  }
}
</style>
