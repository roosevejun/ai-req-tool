# Frontend Function Map

## Product Scope

The frontend is a Vue 3 + Vite business platform organized around five top-level centers:

- Project Management Center
- Requirements Management Center
- Template Management Center
- Enterprise / Industry Knowledge Base
- System Administration

The platform shell provides:

- a top navigation header
- breadcrumb context
- authenticated routing
- route-level workspace pages

## Route Map

### Authentication

- `/login`
  - Login page for authenticated access to the platform

### Project Management

- `/projects`
  - Project management center entry page
  - Business entry and traffic distribution for:
    - traditional project creation
    - AI project incubation
    - project management page

- `/projects/manage`
  - Project management page
  - Current layout:
    - left: project center overview + project list
    - right: selected project information and maintenance workspace

- `/projects/create`
  - Create-mode selection page

- `/projects/create/form`
  - Traditional project creation form

- `/projects/create-ai`
  - AI project incubation workspace
  - Current goal:
    - start from direct AI conversation
    - extract a project framework
    - preserve framework or formally create a project

- `/projects/:projectId/requirements`
  - Requirement list under a specific project

### Requirements Management

- `/docgen`
  - Requirements management center
  - Requirement creation, tree selection, AI requirement refinement, PRD workflow

- `/requirements/:requirementId/workbench`
  - Requirement workbench

- `/requirements/:requirementId/versions`
  - Requirement versions center

### Knowledge Management

- `/knowledge`
  - Enterprise / industry knowledge base workspace
  - Supports:
    - project or requirement scoped browsing
    - status filtering
    - detail modal
    - retry / reprocess
    - asset preview and download

### Template Management

- `/templates`
  - Template management center
  - Supports:
    - template list
    - version management
    - markdown editing
    - variables JSON editing
    - publish flow

### System Administration

- `/system`
  - System administration workspace
  - Supports:
    - users
    - roles
    - permissions
    - bindings and maintenance panels

## Current View Responsibilities

### App Shell

The shell currently provides:

- strong five-center navigation
- breadcrumb navigation
- authenticated workspace container

Current issues:

- visual hierarchy is still card-heavy
- header and context bar do not yet feel like a unified enterprise console
- some pages still carry inconsistent copy quality

### Project Management Center Entry

Current strengths:

- clear split between business entry and management page
- supports both traditional and AI creation paths

Current issues:

- page copy and presentation quality are uneven
- visual hierarchy is not yet strong enough for a formal center homepage
- the page does not fully express "what to do first" at a glance

### Project Management Page

Current strengths:

- left / right structure already exists
- selected project maintenance is clearer than before

Current issues:

- left list and right detail still feel like refactored workspace parts rather than one integrated center page
- object selection, status summary, and maintenance actions need clearer enterprise-page rhythm

### AI Project Incubation

Current strengths:

- first user message can directly start conversation
- the product intent is now closer to framework incubation rather than immediate creation

Current issues:

- conversation-first mental model still needs stronger visual emphasis
- framework maturity, uncertainty, and preservation decisions should feel more explicit

### Requirements Management

Current strengths:

- requirement tree, AI refinement, and DocGen workflow are connected

Current issues:

- the center page still reads as several stacked cards
- stages and next actions can be made more visually decisive

### Knowledge Base

Current strengths:

- status lifecycle exists
- filtering, detail, retry, preview, download are present

Current issues:

- the page can better express lifecycle, usage context, and recovery actions as one operations console

### Template Management

Current strengths:

- templates, versions, markdown, variables, and publish flow already exist

Current issues:

- the page still behaves more like an editor stack than a mature standards center
- hierarchy between template catalog, version governance, and editing needs refinement

### System Administration

Current strengths:

- users, roles, permissions, and bindings are already split by function

Current issues:

- system health, governance tasks, and management objects are not yet visually unified enough

## Shared Frontend Patterns Already Present

- route-level orchestration pages
- domain component folders
- workspace primitives:
  - `WorkspaceSection`
  - `StatusBadge`
  - `FeedbackPanel`
  - `EmptyWorkspaceState`

## UX / Design Gaps

The biggest upgrade opportunities are:

1. unify the enterprise center identity across all top-level pages
2. strengthen the top navigation and breadcrumb system
3. make center homepages feel deliberate, not like refactored feature pages
4. reduce stacked-card feeling in major workspaces
5. improve the information architecture rhythm:
   - center overview
   - object selection
   - current object status
   - action / maintenance area
6. make page-level copy, titles, and hints more consistent

## Recommended Next Design Upgrade Targets

Priority order:

1. global shell header + context bar
2. project management center entry page
3. project management page
4. requirements management center
5. template management center
6. knowledge base center
7. system administration center
