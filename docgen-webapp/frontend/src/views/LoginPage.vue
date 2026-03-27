<template>
  <div class="login-wrap">
    <div class="card">
      <h2>系统登录</h2>
      <p class="hint">登录后可使用需求整理、项目管理和系统管理能力。</p>

      <label class="label">用户名</label>
      <input v-model.trim="username" class="input" placeholder="请输入用户名" />

      <label class="label">密码</label>
      <input v-model="password" type="password" class="input" placeholder="请输入密码" @keyup.enter="onLogin" />

      <button class="primary" :disabled="loading || !username || !password" @click="onLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <p v-if="error" class="error">{{ error }}</p>
      <p class="tip">默认账号：admin / Admin@123</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import { setLoginUser, setToken } from '../auth'

type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
}

type LoginResponse = {
  token: string
  userId: number
  username: string
  displayName: string
  roleCodes: string[]
  permissionCodes: string[]
}

const route = useRoute()
const router = useRouter()
const username = ref('admin')
const password = ref('Admin@123')
const loading = ref(false)
const error = ref('')

async function onLogin() {
  error.value = ''
  loading.value = true
  try {
    const res = await axios.post<ApiResponse<LoginResponse>>('/api/system/auth/login', {
      username: username.value,
      password: password.value
    })
    if (res.data.code !== 0 || !res.data.data?.token) {
      throw new Error(res.data.message || '登录失败')
    }
    setToken(res.data.data.token)
    setLoginUser({
      userId: res.data.data.userId,
      username: res.data.data.username,
      displayName: res.data.data.displayName,
      roleCodes: res.data.data.roleCodes || [],
      permissionCodes: res.data.data.permissionCodes || []
    })
    const redirect = (route.query.redirect as string) || '/docgen'
    await router.push(redirect)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f7fafc, #eef2ff);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, "PingFang SC", "Hiragino Sans GB",
    "Microsoft YaHei", sans-serif;
}
.card {
  width: 360px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 18px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.08);
}
h2 {
  margin: 0 0 8px;
}
.hint {
  color: #6b7280;
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 13px;
}
.label {
  display: block;
  font-size: 13px;
  margin-bottom: 6px;
  color: #374151;
}
.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 12px;
}
.primary {
  width: 100%;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 10px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
}
.primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.error {
  color: #b91c1c;
  margin: 10px 0 0;
  font-size: 13px;
}
.tip {
  color: #6b7280;
  margin-top: 10px;
  font-size: 12px;
}
</style>
