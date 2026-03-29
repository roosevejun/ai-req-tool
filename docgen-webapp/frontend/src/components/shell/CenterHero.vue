<template>
  <section class="center-hero">
    <div class="copy">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p class="summary">{{ summary }}</p>
    </div>

    <div v-if="$slots.badges || $slots.actions" class="aside">
      <div v-if="$slots.badges" class="badges">
        <slot name="badges" />
      </div>
      <div v-if="$slots.actions" class="actions">
        <slot name="actions" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  summary: string
}>()
</script>

<style scoped>
.center-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 26px 28px;
  border-radius: 24px;
  border: 1px solid #d9e2ef;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 250, 255, 0.94));
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}
.copy {
  max-width: 860px;
}
.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-weight: 700;
  color: #1d4ed8;
}
h1 {
  margin: 0;
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.08;
  color: #0f172a;
}
.summary {
  margin: 12px 0 0;
  color: #475569;
  line-height: 1.75;
  font-size: 15px;
}
.aside {
  display: grid;
  gap: 12px;
  justify-items: end;
  min-width: 280px;
}
.badges,
.actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}
@media (max-width: 980px) {
  .center-hero {
    flex-direction: column;
  }
  .aside {
    min-width: 0;
    justify-items: start;
  }
  .badges,
  .actions {
    justify-content: flex-start;
  }
}
</style>
