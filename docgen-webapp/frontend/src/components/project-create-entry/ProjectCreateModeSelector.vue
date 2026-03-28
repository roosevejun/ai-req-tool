<template>
  <section class="selector">
    <article class="mode-card">
      <p class="eyebrow">传统模式</p>
      <h3>表单创建项目</h3>
      <p class="description">
        适合项目信息已经比较明确的场景，直接录入项目背景、目标客户和核心价值，快速完成创建。
      </p>
      <ul class="feature-list">
        <li>一次性填写项目核心信息</li>
        <li>创建后直接进入项目工作台</li>
        <li>后续仍可进入 AI 协同继续优化</li>
      </ul>
      <button class="primary" type="button" @click="$emit('open-form')">进入传统创建</button>
    </article>

    <article class="mode-card ai-card">
      <p class="eyebrow">AI 模式</p>
      <h3>对话创建项目</h3>
      <p class="description">
        适合想法还不完整、需要边聊边沉淀的场景，通过资料、对话和结构化结果逐步形成正式项目。
      </p>
      <ul class="feature-list">
        <li>支持网址、文本和文件资料</li>
        <li>AI 会持续追问并输出结构化结果</li>
        <li>创建时自动保留知识和会话上下文</li>
      </ul>
      <button class="primary ai-primary" type="button" @click="$emit('open-ai')">进入 AI 创建</button>
    </article>
  </section>
</template>

<script setup lang="ts">
defineEmits<{
  (event: 'open-form'): void
  (event: 'open-ai'): void
}>()
</script>

<style scoped>
.selector {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.mode-card {
  padding: 22px;
  border-radius: 20px;
  border: 1px solid #dbe2ea;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}
.ai-card {
  background: linear-gradient(180deg, #f4fbff 0%, #ffffff 100%);
  border-color: #bfdbfe;
}
.eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}
h3 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}
.description {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.7;
}
.feature-list {
  margin: 18px 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.8;
}
.primary {
  border-radius: 10px;
  border: 1px solid #2563eb;
  background: #2563eb;
  color: #fff;
  padding: 10px 14px;
  cursor: pointer;
}
.ai-primary {
  background: #0f766e;
  border-color: #0f766e;
}
@media (max-width: 960px) {
  .selector {
    grid-template-columns: 1fr;
  }
}
</style>
