<template>
  <section class="section" :class="{ 'section--tint': tint }">
    <div class="section-head">
      <div class="section-copy">
        <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
        <h3 class="title">{{ title }}</h3>
        <p v-if="description" class="description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    description?: string
    tint?: boolean
  }>(),
  {
    eyebrow: '',
    description: '',
    tint: false
  }
)
</script>

<style scoped>
.section {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  padding: 18px;
}

.section--tint {
  background: linear-gradient(180deg, #f8fcff 0%, #ffffff 36%);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.title {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.description {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.body {
  margin-top: 18px;
}

@media (max-width: 860px) {
  .section-head {
    flex-direction: column;
  }

  .actions {
    justify-content: flex-start;
  }
}
</style>
