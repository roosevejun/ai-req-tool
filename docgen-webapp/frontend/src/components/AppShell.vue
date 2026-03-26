<template>
  <div class="shell">
    <header class="shell-header">
      <div class="brand" @click="goDefault">
        <div class="brand-mark">AI</div>
        <div>
          <div class="brand-title">AI 需求工具</div>
          <div class="brand-sub">项目、需求与 AI 协同工作台</div>
        </div>
      </div>

      <nav class="main-nav">
        <button
          v-for="item in navItems"
          :key="item.to"
          class="nav-btn"
          :class="{ active: activeSection === item.section }"
          @click="router.push(item.to)"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="user-area" v-if="loginUser">
        <span class="user-name">{{ loginUser.displayName || loginUser.username }}</span>
        <button class="ghost" @click="handleLogout">退出</button>
      </div>
    </header>

    <div v-if="!isLoginPage" class="context-bar">
      <div class="breadcrumbs">
        <button
          v-for="(item, idx) in breadcrumbs"
          :key="`${item.label}-${idx}`"
          class="crumb"
          :class="{ current: idx === breadcrumbs.length - 1, clickable: !!item.to }"
          @click="item.to && router.push(item.to)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="context-actions">
        <button v-if="backTarget" class="ghost" @click="router.push(backTarget)">返回上层页面</button>
      </div>
    </div>

    <main class="shell-body">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLoginUser, logout } from '../auth'

type NavItem = {
  label: string
  to: string
  section: string
}

type Crumb = {
  label: string
  to?: string
}

const route = useRoute()
const router = useRouter()

const navItems: NavItem[] = [
  { label: 'AI 整理', to: '/docgen', section: 'docgen' },
  { label: '项目与需求', to: '/projects', section: 'projects' },
  { label: '系统管理', to: '/system', section: 'system' }
]

const loginUser = computed(() => getLoginUser())
const isLoginPage = computed(() => route.path === '/login')
const activeSection = computed(() => {
  const section = String(route.meta.section || '')
  if (section) return section
  if (route.path.startsWith('/projects') || route.path.startsWith('/requirements')) return 'projects'
  if (route.path.startsWith('/system')) return 'system'
  return 'docgen'
})

const projectId = computed(() => {
  const queryId = Number(route.query.projectId || 0)
  if (Number.isFinite(queryId) && queryId > 0) return queryId
  const paramId = Number(route.params.projectId || 0)
  return Number.isFinite(paramId) && paramId > 0 ? paramId : null
})

const requirementId = computed(() => {
  const value = Number(route.params.requirementId || 0)
  return Number.isFinite(value) && value > 0 ? value : null
})

const breadcrumbs = computed<Crumb[]>(() => {
  const items: Crumb[] = []
  if (route.path === '/login') return items

  items.push({ label: '首页', to: '/docgen' })

  if (activeSection.value === 'docgen') {
    items.push({ label: 'AI 整理' })
    return items
  }

  if (activeSection.value === 'system') {
    items.push({ label: '系统管理' })
    return items
  }

  items.push({ label: '项目与需求', to: '/projects' })

  if (route.path === '/projects') {
    items[items.length - 1].to = undefined
    return items
  }

  if (route.path === '/projects/create-ai') {
    items.push({ label: 'AI 创建项目' })
    return items
  }

  if (route.path.startsWith('/projects/') && route.path.endsWith('/requirements')) {
    items.push({ label: `项目 #${projectId.value ?? '-'}` })
    items.push({ label: '需求列表' })
    return items
  }

  if (requirementId.value) {
    if (projectId.value) {
      items.push({ label: `项目 #${projectId.value}`, to: `/projects?projectId=${projectId.value}` })
    }
    items.push({ label: `需求 #${requirementId.value}` })
    if (route.path.endsWith('/workbench')) {
      items.push({ label: 'AI 工作台' })
    } else if (route.path.endsWith('/versions')) {
      items.push({ label: '版本页面' })
    }
    return items
  }

  return items
})

const backTarget = computed(() => {
  if (route.path === '/projects/create-ai') return '/projects'
  if (route.path.startsWith('/requirements/') && projectId.value) {
    return `/projects?projectId=${projectId.value}`
  }
  if (route.path.startsWith('/projects/') && route.path.endsWith('/requirements') && projectId.value) {
    return `/projects?projectId=${projectId.value}`
  }
  return ''
})

function goDefault() {
  if (isLoginPage.value) return
  router.push('/projects')
}

function handleLogout() {
  logout()
  router.push('/login')
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.08), transparent 24%),
    linear-gradient(180deg, #f6f9fd 0%, #eef4fb 100%);
}
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
  gap: 10px;
  flex: 1;
  justify-content: center;
}
.nav-btn,
.ghost,
.crumb {
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
.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-name {
  color: #334155;
  font-size: 14px;
}
.context-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 18px 0;
}
.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.crumb {
  background: transparent;
  border-color: transparent;
  color: #64748b;
  padding: 0;
}
.crumb.clickable:hover {
  color: #2563eb;
}
.crumb.current {
  color: #0f172a;
  font-weight: 600;
}
.crumb + .crumb::before {
  content: '/';
  margin-right: 8px;
  color: #94a3b8;
}
.crumb.clickable {
  cursor: pointer;
}
.shell-body {
  padding-bottom: 24px;
}
@media (max-width: 980px) {
  .shell-header,
  .context-bar {
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
