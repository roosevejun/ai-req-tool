# Frontend UI Upgrade Plan

## Source

This plan is based on:

- `docs/frontend-function-map.md`
- `ui-ux-pro-max` design-system guidance for an enterprise multi-center platform

## Recommended Visual Direction

### Primary Design System

- Minimalism & Swiss Style
- Trust & Authority

### Why This Fits

The current product is not a marketing site. It is a multi-center enterprise platform that manages:

- projects
- requirements
- templates
- knowledge
- system governance

This requires:

- clear hierarchy
- strong navigation
- structured content rhythm
- enterprise trust signals
- fewer decorative cards and fewer isolated page styles

## Platform-Level Recommendations

### 1. Global Shell

The shell should feel like one enterprise platform, not a collection of refactored pages.

Required upgrades:

- stronger top navigation identity
- lighter but clearer breadcrumb bar
- consistent header language across all centers
- formal business-platform copy

### 2. Center Hero Pattern

All top-level centers should share one hero pattern:

- eyebrow for center category
- center title
- one-sentence responsibility summary
- right-side badges for current status
- optional actions for the most important next step

### 3. Center Page Types

The platform should use two major page models:

#### Center Entry Page

Used when the page is a business gateway.

Examples:

- project management center

Traits:

- business entry cards
- overview metrics
- action recommendations

#### Business Handling Page

Used when the page is for selecting an object and operating on it.

Examples:

- project management page
- knowledge workspace
- system administration

Traits:

- left object / scope selection
- right status, detail, and maintenance area
- stronger task rhythm

## Page-Specific Upgrade Guidance

### Project Management Center

Target:

- formal business entry center
- clear split between:
  - traditional creation
  - AI incubation
  - project management entry

### Project Management Page

Target:

- left list / right handling page
- stronger distinction between project selection and project maintenance

### Requirements Management Center

Target:

- clearer stage-driven workspace
- stronger visibility for:
  - selected project
  - selected requirement
  - current stage
  - next action

### Template Management Center

Target:

- standards center, not just an editor
- stronger structure:
  - template catalog
  - published / draft summary
  - version governance
  - version editor

### Knowledge Center

Target:

- operations console for knowledge lifecycle
- stronger expression of:
  - document scope
  - lifecycle
  - failure handling
  - reprocess actions

### System Administration

Target:

- governance console
- unify:
  - health summary
  - management tabs
  - pending actions

## Interaction Guidance

- use fewer decorative gradients
- use stronger business-state badges
- prefer structured sections over stacked mixed cards
- prefer one main action per page or section
- keep destructive actions secondary in visual weight

## Implementation Priority

1. global shell header and context bar
2. center hero pattern
3. project management center
4. requirements management center
5. template management center
6. knowledge center
7. system administration
