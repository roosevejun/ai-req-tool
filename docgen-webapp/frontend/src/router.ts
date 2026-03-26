import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from './auth'
import LoginPage from './views/LoginPage.vue'
import DocGenView from './views/DocGenView.vue'
import SystemAdminView from './views/SystemAdminView.vue'
import ProjectsView from './views/ProjectsView.vue'
import ProjectCreateAiView from './views/ProjectCreateAiView.vue'
import RequirementsView from './views/RequirementsView.vue'
import RequirementWorkbenchView from './views/RequirementWorkbenchView.vue'
import RequirementVersionsView from './views/RequirementVersionsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginPage, meta: { public: true } },
    { path: '/', redirect: '/docgen' },
    { path: '/docgen', component: DocGenView },
    { path: '/system', component: SystemAdminView },
    { path: '/projects', component: ProjectsView },
    { path: '/projects/create-ai', component: ProjectCreateAiView },
    { path: '/projects/:projectId/requirements', component: RequirementsView },
    { path: '/requirements/:requirementId/workbench', component: RequirementWorkbenchView },
    { path: '/requirements/:requirementId/versions', component: RequirementVersionsView }
  ]
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
