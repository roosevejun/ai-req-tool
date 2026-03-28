# Component Guidelines

> How components are built in this project.

## Overview

This project uses Vue 3 SFCs with route views as orchestration layers and domain components as focused UI sections.

## Scenario: Building Or Refactoring A Page

### 1. Scope / Trigger
- Trigger: adding a new page, splitting an oversized page, or introducing a reusable panel/card
- Use this pattern when a view starts mixing routing, API calls, labels, and multiple visual sections

### 2. Signatures
- Route view: `src/views/*.vue`
- Domain sections: `src/components/<domain>/*.vue`
- Shared domain types: `src/components/<domain>/types.ts`
- Shared domain formatting: `src/components/<domain>/helpers.ts` or `labels.ts`

### 3. Contracts
- Parent view owns:
  - route params and navigation
  - API loading and mutation calls
  - top-level `ref` / `reactive` state
  - cross-card coordination
- Child components own:
  - section template and local presentational structure
  - section-level emits for user actions
  - lightweight computed display logic
- Shared helpers own:
  - enum-to-label mapping
  - formatting utilities
  - presentation-safe constants
- Shared workspace primitives own:
  - section shells such as `WorkspaceSection.vue`
  - state badges such as `StatusBadge.vue`
  - repeatable feedback and empty states such as `FeedbackPanel.vue` and `EmptyWorkspaceState.vue`

### 4. Validation & Error Matrix

| Situation | Required handling |
|----------|-------------------|
| View file grows beyond one business concern | Split by domain section before adding more logic |
| Same labels used in multiple components | Move to `helpers.ts` or `labels.ts` in that domain |
| Child needs routing side effect | Emit intent upward instead of importing router everywhere |
| Child needs parent API data | Pass typed props from the view |
| Build breaks after refactor | Fix structure first, then rerun WSL build before finishing |

### 5. Good / Base / Bad Cases

#### Good
- `ProjectsView.vue` orchestrates selected project state and delegates rendering to `projects/*`
- `ProjectCreateAiView.vue` keeps API flow while cards handle setup/chat/result/create sections
- `SystemAdminView.vue` delegates user/role/permission sections to domain cards

#### Base
- A small page can stay in one file if it has one visual block and limited local state

#### Bad
- A route view contains hundreds of lines of repeated template blocks, enum labels, and inline handlers for unrelated sections

### 6. Tests Required
- Build assertion:
  - run WSL build command after structural changes
- Manual assertion points:
  - route still renders
  - child emits still trigger parent actions
  - labels and status mappings still display correctly
  - no duplicated top navigation is reintroduced

### 7. Wrong vs Correct

#### Wrong

```vue
<template>
  <div>
    <!-- one huge view owns sidebar, forms, lists, details, labels, and modals -->
  </div>
</template>
```

#### Correct

```vue
<template>
  <div class="projects-view">
    <ProjectsSidebar />
    <ProjectDetailsCard />
  </div>
</template>
```

## Component Structure

Prefer this order inside `.vue` files:
1. `<template>`
2. `<script setup lang="ts">`
3. `<style scoped>`

Keep the script section focused:
- props and emits
- local computed values
- small section-specific handlers

Move large label maps, shared types, and formatting helpers into sibling `.ts` files.

## Props Conventions

Use typed props and events with `script setup`.

Conventions:
- pass explicit props instead of broad state objects when practical
- emit user intent upward, for example `select`, `delete`, `refresh`, `submit`
- keep prop contracts domain-specific and colocated with domain `types.ts`

When multiple sibling components share the same data shape, define the type once in the domain folder.

## Styling Patterns

The project uses scoped styles inside SFCs.

Rules:
- keep styles close to the component that owns them
- use a stable root class per component
- do not duplicate large layout styles across pages when a child card can own them
- preserve the existing visual language instead of inventing a new layout system per page
- when multiple workspace pages need the same shell or state expression, extract a reusable primitive first

### Workspace Primitives

For workspace-style pages, prefer these shared primitives before creating a page-local equivalent:
- `WorkspaceSection.vue` for titled section shells with eyebrow, description, and actions
- `StatusBadge.vue` for status, tags, and small metrics
- `FeedbackPanel.vue` for success / error / info feedback blocks
- `EmptyWorkspaceState.vue` for no-selection and first-use placeholders

## Accessibility

Minimum requirements:
- buttons must use real `<button>` elements for actions
- form controls should keep visible labels or clear placeholders
- destructive actions should keep confirmation
- modals and expandable panels should expose obvious close or collapse controls

## Common Mistakes

### Common Mistake: Rebuilding global navigation inside a page

**Symptom**: top bars, breadcrumbs, or back buttons appear twice.

**Fix**: keep global navigation in `AppShell` and only render page content inside views.

### Common Mistake: Leaving enum labels inside multiple templates

**Symptom**: `PRIVATE`, `ACTIVE`, `P1`, or role codes leak into the UI in several places.

**Fix**: move label mapping into a domain `helpers.ts` or `labels.ts`.

### Common Mistake: Keeping refactor-only state inside child and parent at the same time

**Symptom**: selection, dialogs, or submit state drift after splitting components.

**Fix**: keep cross-section state in the parent view and let children emit actions.

