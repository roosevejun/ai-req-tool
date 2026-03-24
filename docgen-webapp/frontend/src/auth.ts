export const TOKEN_KEY = 'docgen_auth_token'
export const USER_KEY = 'docgen_auth_user'

export type LoginUser = {
  userId: number
  username: string
  displayName: string
  roleCodes: string[]
  permissionCodes: string[]
}

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function setLoginUser(user: LoginUser) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function getLoginUser(): LoginUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as LoginUser
  } catch {
    return null
  }
}

export function clearLoginUser() {
  localStorage.removeItem(USER_KEY)
}

export function isLoggedIn(): boolean {
  return !!getToken()
}

export function logout() {
  clearToken()
  clearLoginUser()
}

