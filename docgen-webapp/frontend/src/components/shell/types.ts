export type NavChildItem = {
  label: string
  to: string
}

export type NavItem = {
  label: string
  to: string
  section: string
  children?: NavChildItem[]
}

export type Crumb = {
  label: string
  to?: string
}
