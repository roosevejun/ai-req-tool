import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from './auth'
import LoginPage from './views/LoginPage.vue'
import DocGenView from './views/DocGenView.vue'
import KnowledgeLibraryView from './views/KnowledgeLibraryView.vue'
import SystemAdminView from './views/SystemAdminView.vue'
import TemplateCenterView from './views/TemplateCenterView.vue'
import ProjectsView from './views/ProjectsView.vue'
import ProjectCreateEntryView from './views/ProjectCreateEntryView.vue'
import ProjectCreateFormView from './views/ProjectCreateFormView.vue'
import ProjectCreateAiView from './views/ProjectCreateAiView.vue'
import RequirementsView from './views/RequirementsView.vue'
import RequirementWorkbenchView from './views/RequirementWorkbenchView.vue'
import RequirementVersionsView from './views/RequirementVersionsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginPage, meta: { public: true } },
    { path: '/', redirect: '/docgen' },
    { path: '/docgen', component: DocGenView, meta: { section: 'docgen' } },
    { path: '/knowledge', component: KnowledgeLibraryView, meta: { section: 'knowledge' } },
    { path: '/system', component: SystemAdminView, meta: { section: 'system' } },
    { path: '/templates', component: TemplateCenterView, meta: { section: 'system' } },
    { path: '/projects', component: ProjectsView, meta: { section: 'projects' } },
    { path: '/projects/create', component: ProjectCreateEntryView, meta: { section: 'projects' } },
    { path: '/projects/create/form', component: ProjectCreateFormView, meta: { section: 'projects' } },
    { path: '/projects/create-ai', component: ProjectCreateAiView, meta: { section: 'projects' } },
    { path: '/projects/:projectId/requirements', component: RequirementsView, meta: { section: 'projects' } },
    { path: '/requirements/:requirementId/workbench', component: RequirementWorkbenchView, meta: { section: 'projects' } },
    { path: '/requirements/:requirementId/versions', component: RequirementVersionsView, meta: { section: 'projects' } }
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
