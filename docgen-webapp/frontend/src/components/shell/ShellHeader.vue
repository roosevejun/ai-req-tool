<template>
  <aside class="shell-header">
    <div class="brand" @click="$emit('go-default')">
      <div class="brand-mark">AP</div>
      <div class="brand-copy">
        <div class="brand-title">AI 项目与需求管理平台</div>
        <div class="brand-sub">五大中心统一承接项目、需求、模板、知识与系统治理</div>
      </div>
    </div>

    <div class="nav-heading">
      <span class="nav-heading-eyebrow">系统导航</span>
      <p class="nav-heading-text">先选择业务中心，再进入当前中心的子栏目办理业务。</p>
    </div>

    <nav class="main-nav">
      <div
        v-for="item in navItems"
        :key="item.to"
        class="nav-group"
        :class="{ 'nav-group--active': activeSection === item.section }"
      >
        <button
          class="nav-btn"
          :class="{ active: activeSection === item.section }"
          @click="$emit('navigate', item.to)"
        >
          <span class="nav-accent" />
          <span class="nav-copy">
            <span class="nav-label">{{ item.label }}</span>
            <span class="nav-caption">业务中心</span>
          </span>
        </button>

        <div v-if="activeSection === item.section && item.children?.length" class="sub-nav">
          <button
            v-for="child in item.children"
            :key="child.to"
            class="sub-btn"
            :class="{ active: currentPath === child.to }"
            @click="$emit('navigate', child.to)"
          >
            {{ child.label }}
          </button>
        </div>
      </div>
    </nav>

    <div class="user-area" v-if="loginUser">
      <div class="user-card">
        <span class="user-caption">当前登录</span>
        <strong class="user-name">{{ loginUser.displayName || loginUser.username }}</strong>
      </div>
      <button class="ghost" @click="$emit('logout')">退出</button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import type { LoginUser } from '../../auth'
import type { NavItem } from './types'

defineProps<{
  navItems: NavItem[]
  activeSection: string
  currentPath: string
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
  height: 100dvh;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 16px;
  border-right: 1px solid #dbe2ea;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(14px);
}

.brand {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5edf5;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1d4ed8, #0f172a);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.04em;
  flex: 0 0 auto;
}

.brand-copy {
  display: grid;
  gap: 2px;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
  color: #0f172a;
}

.brand-sub {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.nav-heading {
  display: grid;
  gap: 4px;
  padding: 4px 8px 0;
}

.nav-heading-eyebrow,
.nav-caption,
.user-caption {
  color: #64748b;
  font-size: 11px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.nav-heading-text {
  margin: 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.45;
}

.main-nav {
  display: grid;
  gap: 2px;
  align-content: start;
}

.nav-group {
  display: grid;
  gap: 4px;
  padding: 6px 0;
  border-bottom: 1px solid #eef2f7;
}

.nav-btn,
.ghost,
.sub-btn {
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
}

.nav-btn {
  width: 100%;
  border-radius: 10px;
  padding: 10px 10px 10px 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  transition: border-color 180ms ease, background-color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.nav-accent {
  width: 4px;
  align-self: stretch;
  border-radius: 999px;
  background: transparent;
  flex: 0 0 auto;
}

.nav-copy {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.nav-label {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.nav-btn:hover,
.ghost:hover,
.sub-btn:hover {
  border-color: #d8e1eb;
  background: #f8fafc;
  transform: translateY(-1px);
}

.nav-btn.active {
  border-color: #bfdbfe;
  background: linear-gradient(180deg, #eff6ff 0%, #f8fbff 100%);
  box-shadow: 0 8px 14px rgba(37, 99, 235, 0.08);
}

.nav-btn.active .nav-accent {
  background: #2563eb;
}

.nav-btn.active .nav-label,
.nav-btn.active .nav-caption {
  color: #1d4ed8;
}

.sub-nav {
  display: grid;
  gap: 4px;
  padding-left: 18px;
  margin: 0 0 0 8px;
  border-left: 1px solid #dbe2ea;
}

.sub-btn {
  border-radius: 8px;
  padding: 7px 10px;
  text-align: left;
  font-size: 13px;
  color: #475569;
}

.sub-btn.active {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
}

.user-area {
  margin-top: auto;
  display: grid;
  gap: 8px;
  padding-top: 8px;
}

.user-card {
  display: grid;
  gap: 2px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #dbe2ea;
}

.user-name {
  color: #0f172a;
  font-size: 14px;
}

.ghost {
  border-radius: 12px;
  padding: 10px 14px;
  border-color: #d8e1eb;
  background: #fff;
}

@media (max-width: 1120px) {
  .shell-header {
    position: static;
    height: auto;
    border-right: 0;
    border-bottom: 1px solid #dbe2ea;
  }

  .main-nav {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }

  .sub-nav {
    padding-left: 0;
    margin-left: 0;
    border-left: 0;
  }
}
</style>
