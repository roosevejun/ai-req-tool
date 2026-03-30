import type { ProjectEditFormState, ProjectItem } from './types'

export function createProjectEditFormState(): ProjectEditFormState {
  return {
    projectName: '',
    description: '',
    projectBackground: '',
    similarProducts: '',
    targetCustomerGroups: '',
    commercialValue: '',
    coreProductValue: '',
    projectType: '',
    priority: 'P2',
    startDate: '',
    targetDate: '',
    tags: '',
    ownerUserId: '',
    visibility: 'PRIVATE',
    status: 'ACTIVE'
  }
}

export function assignProjectEditForm(target: ProjectEditFormState, source: ProjectEditFormState) {
  Object.assign(target, source)
}

export function fillProjectEditForm(target: ProjectEditFormState, project: ProjectItem) {
  assignProjectEditForm(target, {
    projectName: project.projectName || '',
    description: project.description || '',
    projectBackground: project.projectBackground || '',
    similarProducts: project.similarProducts || '',
    targetCustomerGroups: project.targetCustomerGroups || '',
    commercialValue: project.commercialValue || '',
    coreProductValue: project.coreProductValue || '',
    projectType: project.projectType || '',
    priority: project.priority || 'P2',
    startDate: project.startDate || '',
    targetDate: project.targetDate || '',
    tags: project.tags || '',
    ownerUserId: project.ownerUserId ? String(project.ownerUserId) : '',
    visibility: project.visibility || 'PRIVATE',
    status: project.status || 'ACTIVE'
  })
}
