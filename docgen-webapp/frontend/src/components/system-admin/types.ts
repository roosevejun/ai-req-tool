export type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

export type UserItem = {
  id: number
  username: string
  displayName: string
  status: string
  roleCodes: string[]
}

export type RoleItem = {
  id: number
  roleCode: string
  roleName: string
  status: string
}

export type PermItem = {
  id: number
  permCode: string
  permName: string
  status: string
}

export type NewUserForm = {
  username: string
  password: string
  displayName: string
  status: string
  roleIds: number[]
}

export type NewRoleForm = {
  roleCode: string
  roleName: string
  status: string
}

export type NewPermForm = {
  permCode: string
  permName: string
  status: string
}

export type EditUserForm = {
  displayName: string
  status: string
}

export type EditRoleForm = {
  roleName: string
  status: string
}

export type EditPermForm = {
  permName: string
  status: string
}
