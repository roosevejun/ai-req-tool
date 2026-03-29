<template>
  <header class="shell-header">
    <div class="brand" @click="$emit('go-default')">
      <div class="brand-mark">AP</div>
      <div class="brand-copy">
        <div class="brand-title">AI 项目与需求管理平台</div>
        <div class="brand-sub">五大中心统一承接项目、需求、模板、知识与系统治理</div>
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
        <span class="nav-label">{{ item.label }}</span>
        <span class="nav-caption">Business Center</span>
      </button>
    </nav>

    <div class="user-area" v-if="loginUser">
      <div class="user-card">
        <span class="user-caption">当前登录</span>
        <strong class="user-name">{{ loginUser.displayName || loginUser.username }}</strong>
      </div>
      <button class="ghost" @click="$emit('logout')">退出</button>
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
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 18px;
  padding: 16px 20px;
  border-bottom: 1px solid #dbe2ea;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(14px);
}
.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
}
.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1d4ed8, #0f172a);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.04em;
}
.brand-copy {
  display: grid;
  gap: 4px;
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
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}
.nav-btn,
.ghost {
  border: 1px solid #d4dce6;
  background: #fff;
  border-radius: 14px;
  cursor: pointer;
}
.nav-btn {
  min-width: 138px;
  padding: 10px 12px;
  display: grid;
  gap: 3px;
  text-align: left;
  transition: border-color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}
.nav-label {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}
.nav-caption {
  font-size: 11px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #64748b;
}
.nav-btn:hover,
.ghost:hover {
  border-color: #94a3b8;
  transform: translateY(-1px);
}
.nav-btn.active {
  border-color: #2563eb;
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.12);
}
.nav-btn.active .nav-label {
  color: #1d4ed8;
}
.nav-btn.active .nav-caption {
  color: #2563eb;
}
.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-card {
  display: grid;
  gap: 2px;
  padding: 9px 12px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #dbe2ea;
}
.user-caption {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.user-name {
  color: #0f172a;
  font-size: 14px;
}
.ghost {
  padding: 10px 14px;
}
@media (max-width: 1120px) {
  .shell-header {
    grid-template-columns: 1fr;
    align-items: stretch;
  }
  .main-nav {
    justify-content: flex-start;
  }
  .user-area {
    justify-content: space-between;
  }
}
</style>
