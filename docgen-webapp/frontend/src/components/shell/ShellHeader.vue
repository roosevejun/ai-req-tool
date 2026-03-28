<template>
  <header class="shell-header">
    <div class="brand" @click="$emit('go-default')">
      <div class="brand-mark">AI</div>
      <div>
        <div class="brand-title">AI 项目与需求管理平台</div>
        <div class="brand-sub">项目管理、需求生产、模板标准、行业知识与系统治理统一工作台</div>
      </div>
    </div>

    <nav class="main-nav">
      <button
        v-for="item in navItems"
        :key="item.to"
        class="nav-btn"
        :class="{ active: activeSection === item.section }"
        @click="$emit('navigate', item.to)"
      >
        <span class="nav-eyebrow">管理中心</span>
        {{ item.label }}
      </button>
    </nav>

    <div class="user-area" v-if="loginUser">
      <span class="user-name">{{ loginUser.displayName || loginUser.username }}</span>
      <button class="ghost" @click="$emit('logout')">退出登录</button>
    </div>
  </header>
</template>

<script setup lang="ts">
import type { LoginUser } from '../../auth'
import type { NavItem } from './types'

defineProps<{
  navItems: NavItem[]
  activeSection: string
  loginUser: LoginUser | null
}>()

defineEmits<{
  (event: 'go-default'): void
  (event: 'navigate', to: string): void
  (event: 'logout'): void
}>()
</script>

<style scoped>
.shell-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-bottom: 1px solid #dbe2ea;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1d4ed8, #0f766e);
  color: #fff;
  font-weight: 700;
}
.brand-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}
.brand-sub {
  font-size: 12px;
  color: #64748b;
}
.main-nav {
  display: flex;
  gap: 12px;
  flex: 1;
  justify-content: center;
}
.nav-btn,
.ghost {
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: 10px;
  padding: 8px 12px;
  cursor: pointer;
}
.nav-btn.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.nav-btn {
  min-width: 136px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}
.nav-eyebrow {
  font-size: 11px;
  letter-spacing: .06em;
  text-transform: uppercase;
  color: #64748b;
}
.nav-btn.active .nav-eyebrow {
  color: rgba(255, 255, 255, 0.82);
}
.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-name {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}
.ghost:hover,
.nav-btn:hover {
  border-color: #94a3b8;
}
@media (max-width: 980px) {
  .shell-header {
    flex-direction: column;
    align-items: stretch;
  }
  .main-nav {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
  .user-area {
    justify-content: space-between;
  }
}
</style>
