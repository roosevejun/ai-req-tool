<template>
  <div class="login-page">
    <section class="login-shell">
      <section class="login-card">
        <div class="card-brand">
          <div class="brand-mark">AP</div>
          <div class="brand-meta">
            <p class="eyebrow">Platform Access</p>
            <p class="platform-name">AI 项目与需求管理平台</p>
          </div>
        </div>

        <header class="card-header">
          <h1>登录平台</h1>
          <p class="lead">进入统一平台，继续项目管理、需求生产、模板治理、知识沉淀与系统管理。</p>
        </header>

        <div class="trust-strip">
          <div class="trust-chip">
            <strong>统一平台入口</strong>
            <span>当前页面仅用于身份确认与进入系统。</span>
          </div>
          <div class="trust-chip">
            <strong>可信与可追踪</strong>
            <span>项目、需求、知识与模板都将在平台内持续沉淀。</span>
          </div>
        </div>

        <div class="form-group">
          <label class="label" for="username">用户名</label>
          <input
            id="username"
            v-model.trim="username"
            class="input"
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </div>

        <div class="form-group">
          <label class="label" for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            class="input"
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter="onLogin"
          />
        </div>

        <button class="primary" :disabled="loading || !username || !password" @click="onLogin">
          {{ loading ? '登录中...' : '登录并进入平台' }}
        </button>

        <p v-if="error" class="error">{{ error }}</p>

        <div class="account-tip">
          <span class="tip-label">默认演示账号</span>
          <code>admin / Admin@123</code>
        </div>
      </section>
    </section>
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
    const redirect = (route.query.redirect as string) || '/projects'
    await router.push(redirect)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 20px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at center top, rgba(30, 64, 175, 0.05), transparent 36%),
    linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
  font-family: "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

.login-shell {
  width: 100%;
  max-width: 500px;
}

.login-card {
  display: grid;
  gap: 20px;
  padding: 30px;
  border: 1px solid #d6dfeb;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.card-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: #1d4ed8;
  color: #ffffff;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.brand-meta {
  display: grid;
  gap: 4px;
}

.eyebrow {
  margin: 0;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.platform-name {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.card-header {
  display: grid;
  gap: 10px;
}

.card-header h1 {
  margin: 0;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.15;
  letter-spacing: -0.03em;
}

.lead {
  margin: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
}

.trust-strip {
  display: grid;
  gap: 10px;
}

.trust-chip {
  padding: 12px 14px;
  border: 1px solid #dbe3ee;
  border-radius: 14px;
  background: #f8fbff;
}

.trust-chip strong {
  display: block;
  margin-bottom: 4px;
  color: #0f172a;
  font-size: 13px;
}

.trust-chip span {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.form-group {
  display: grid;
  gap: 8px;
}

.label {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 13px 14px;
  border: 1px solid #cfd8e3;
  border-radius: 14px;
  background: #ffffff;
  color: #0f172a;
  font-size: 15px;
  transition: border-color 180ms ease, box-shadow 180ms ease;
}

.input:focus {
  outline: none;
  border-color: #1d4ed8;
  box-shadow: 0 0 0 4px rgba(29, 78, 216, 0.1);
}

.primary {
  width: 100%;
  padding: 14px 18px;
  border: 1px solid #1d4ed8;
  border-radius: 14px;
  background: #1d4ed8;
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 180ms ease, transform 180ms ease, opacity 180ms ease;
}

.primary:hover:not(:disabled) {
  background: #1e40af;
  transform: translateY(-1px);
}

.primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid #fecaca;
  border-radius: 14px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 13px;
}

.account-tip {
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.tip-label {
  color: #64748b;
  font-size: 13px;
}

.account-tip code {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 640px) {
  .login-page {
    padding: 12px;
  }

  .login-card {
    padding: 24px 18px;
  }

  .card-header h1 {
    font-size: 28px;
  }

  .account-tip {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
