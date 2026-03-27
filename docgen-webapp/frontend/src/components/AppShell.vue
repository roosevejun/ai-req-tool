<template>
  <div class="shell">
    <ShellHeader
      :nav-items="navItems"
      :active-section="activeSection"
      :login-user="loginUser"
      @go-default="goDefault"
      @navigate="navigate"
      @logout="handleLogout"
    />

    <ShellContextBar
      v-if="!isLoginPage"
      :breadcrumbs="breadcrumbs"
      :back-target="backTarget"
      @navigate="navigate"
    />

    <main class="shell-body">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLoginUser, logout } from '../auth'
import ShellContextBar from './shell/ShellContextBar.vue'
import ShellHeader from './shell/ShellHeader.vue'
import type { Crumb, NavItem } from './shell/types'

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
      items.push({ label: '版本页' })
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

function navigate(to: string) {
  void router.push(to)
}

function goDefault() {
  if (isLoginPage.value) return
  navigate('/projects')
}

function handleLogout() {
  logout()
  navigate('/login')
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.08), transparent 24%),
    linear-gradient(180deg, #f6f9fd 0%, #eef4fb 100%);
}
.shell-body {
  padding: 12px 0 28px;
}
</style>
