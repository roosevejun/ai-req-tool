<!-- TRELLIS:START -->
# Trellis Instructions

These instructions are for AI assistants working in this project.

Use the `/trellis:start` command when starting a new session to:
- Initialize your developer identity
- Understand current project context
- Read relevant guidelines

Use `@/.trellis/` to learn:
- Development workflow (`workflow.md`)
- Project structure guidelines (`spec/`)
- Developer workspace (`workspace/`)

Keep this managed block so 'trellis update' can refresh the instructions.

<!-- TRELLIS:END -->

## Project Skills

### Imperial Collaboration

- `imperial-dispatch`: Use when the user wants to work in "emperor -> Three Departments and Six Ministries" mode. The assistant must normalize the edict, plan as `Zhongshu`, review as `Menxia`, dispatch as `Shangshu`, execute the work, and report back as a memorial.

### Design Skills

- `ui-ux-pro-max`: Use when the task needs stronger UI/UX direction, design-system generation, style selection, color/typography guidance, stack-specific frontend recommendations, or UI review. File path: `G:/Agent/.codex/skills/ui-ux-pro-max/SKILL.md`

## Project Agents

### Claude Subagents

- `imperial-dispatch`: Stored at `.claude/agents/imperial-dispatch.md`. Use when you want the imperial workflow as a reusable agent persona instead of re-explaining the protocol in each task.

## Release Shortcut

- When the user says `今天先这样吧，我准备睡觉了`, treat it as an end-of-day release command instead of a casual closing message.
- Default execution order:
  1. Check current workspace state and confirm what will be included.
  2. Commit current code changes.
  3. Push the current branch to GitHub.
  4. Build the deployment image.
  5. Push the image to Harbor.
- If any required release parameters are missing, ask only for the missing items, then continue:
  - target git branch or default push rule
  - commit message convention
  - image name and tag rule
  - Docker build command or build context
  - Harbor registry/project target
- Do not treat the phrase as a release command if the user clearly uses it as ordinary conversation and explicitly says not to deploy, commit, or push.
