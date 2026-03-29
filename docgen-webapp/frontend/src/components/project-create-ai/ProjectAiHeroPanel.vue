<template>
  <section class="page-hero">
    <div class="hero-main">
      <div class="hero-head">
        <div class="hero-title">
          <p class="eyebrow">AI 项目孵化</p>
          <h1>先让 AI 提炼项目框架，再决定是否立项</h1>
        </div>

        <div class="hero-actions">
          <button class="hero-switch" type="button" @click="$emit('go-form-mode')">切换到传统创建</button>
        </div>
      </div>

      <div class="hero-toolbar">
        <div class="hero-summary">
          <span class="hero-summary__item">已形成 {{ confirmedCount }} 项</span>
          <span class="hero-summary__item">待确认 {{ pendingCount }} 项</span>
          <span class="hero-summary__item">待补充 {{ missingCount }} 项</span>
        </div>

        <div class="maturity-pill">
          <span class="maturity-label">当前成熟度</span>
          <strong>{{ readyToCreate ? '已接近可立项' : '仍在孵化中' }}</strong>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  confirmedCount: number
  pendingCount: number
  missingCount: number
  readyToCreate: boolean
  accuracyAdvice: string
}>()

defineEmits<{
  (event: 'go-form-mode'): void
}>()
</script>

<style scoped>
.page-hero {
  margin-bottom: 14px;
  padding: 10px 14px;
  border: 1px solid #dbe2ea;
  border-radius: 14px;
  background: linear-gradient(135deg, #f8fcff 0%, #ffffff 55%);
}

.hero-main {
  display: grid;
  gap: 10px;
}

.hero-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.hero-title {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.eyebrow {
  margin: 0;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
  color: #0f172a;
}

.hero-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex: 0 0 auto;
}

.hero-switch {
  border-radius: 9px;
  border: 1px solid #d1d5db;
  background: #f8fafc;
  color: #0f172a;
  padding: 7px 11px;
  cursor: pointer;
  white-space: nowrap;
}

.hero-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hero-summary__item {
  border-radius: 999px;
  border: 1px solid #dbe2ea;
  background: rgba(255, 255, 255, 0.92);
  color: #475569;
  padding: 5px 9px;
  font-size: 12px;
  line-height: 1.3;
}

.maturity-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 7px 11px;
  border: 1px solid #dbe2ea;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
}

.maturity-label {
  color: #64748b;
  font-size: 12px;
}

.maturity-pill strong {
  color: #0f172a;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .hero-head,
  .hero-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
