<template>
  <section class="empty-state">
    <p class="eyebrow">{{ eyebrow }}</p>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    description: string
  }>(),
  {
    eyebrow: '工作区已就绪'
  }
)
</script>

<style scoped>
.empty-state {
  min-height: 320px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  padding: 18px;
}

.eyebrow {
  margin: 0;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

h3 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}

p {
  margin: 0;
  max-width: 620px;
  color: #64748b;
  line-height: 1.7;
}
</style>
