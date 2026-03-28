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
  { label: '项目管理中心', to: '/projects', section: 'projects' },
  { label: '需求管理中心', to: '/docgen', section: 'requirements' },
  { label: '模板管理中心', to: '/templates', section: 'templates' },
  { label: '企业行业知识库', to: '/knowledge', section: 'knowledge' },
  { label: '系统管理', to: '/system', section: 'system' }
]

const loginUser = computed(() => getLoginUser())
const isLoginPage = computed(() => route.path === '/login')

const activeSection = computed(() => {
  const section = String(route.meta.section || '')
  if (section) return section
  if (route.path.startsWith('/projects')) return 'projects'
  if (route.path.startsWith('/requirements') || route.path.startsWith('/docgen')) return 'requirements'
  if (route.path.startsWith('/knowledge')) return 'knowledge'
  if (route.path.startsWith('/templates')) return 'templates'
  if (route.path.startsWith('/system')) return 'system'
  return 'projects'
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

  items.push({ label: '首页', to: '/projects' })

  if (activeSection.value === 'requirements') {
    items.push({ label: '需求管理中心', to: '/docgen' })
    if (route.path === '/docgen') {
      items[items.length - 1].to = undefined
      return items
    }
    if (requirementId.value) {
      items.push({ label: `需求 #${requirementId.value}` })
      if (route.path.endsWith('/workbench')) {
        items.push({ label: '需求工作台' })
      } else if (route.path.endsWith('/versions')) {
        items.push({ label: '需求版本中心' })
      }
    }
    return items
  }

  if (activeSection.value === 'knowledge') {
    items.push({ label: '企业行业知识库' })
    return items
  }

  if (activeSection.value === 'templates') {
    items.push({ label: '模板管理中心' })
    return items
  }

  if (activeSection.value === 'system') {
    items.push({ label: '系统管理' })
    return items
  }

  items.push({ label: '项目管理中心', to: '/projects' })

  if (route.path === '/projects') {
    items[items.length - 1].to = undefined
    return items
  }

  if (route.path === '/projects/manage') {
    items.push({ label: '项目管理页' })
    return items
  }

  if (route.path === '/projects/create') {
    items.push({ label: '选择创建方式' })
    return items
  }

  if (route.path === '/projects/create/form') {
    items.push({ label: '传统创建项目' })
    return items
  }

  if (route.path === '/projects/create-ai') {
    items.push({ label: 'AI 项目孵化' })
    return items
  }

  if (route.path.startsWith('/projects/') && route.path.endsWith('/requirements')) {
    items.push({ label: `项目 #${projectId.value ?? '-'}` })
    items.push({ label: '需求列表' })
    return items
  }

  return items
})

const backTarget = computed(() => {
  if (route.path === '/projects/manage') return '/projects'
  if (route.path === '/projects/create' || route.path === '/projects/create/form' || route.path === '/projects/create-ai') return '/projects'
  if (route.path.startsWith('/templates')) return '/system'
  if (route.path.startsWith('/requirements/')) return '/docgen'
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
