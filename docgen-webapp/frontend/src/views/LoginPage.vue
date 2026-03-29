<template>
  <div class="login-page">
    <section class="login-shell">
      <aside class="brand-panel">
        <div class="brand-top">
          <div class="brand-mark">AP</div>
          <div class="brand-copy">
            <p class="eyebrow">Enterprise Platform</p>
            <h1>AI 项目与需求管理平台</h1>
            <p class="lead">
              面向项目管理、需求生产、模板治理与企业知识沉淀的一体化业务平台。
            </p>
          </div>
        </div>

        <div class="trust-band">
          <div class="trust-item">
            <strong>五大中心</strong>
            <span>项目、需求、模板、知识与系统治理统一承接</span>
          </div>
          <div class="trust-item">
            <strong>全链路可追踪</strong>
            <span>从项目孵化到文档生产，状态、版本、知识都可持续沉淀</span>
          </div>
          <div class="trust-item">
            <strong>AI 协同可复用</strong>
            <span>统一会话、统一知识、统一模板，形成持续演进的业务资产</span>
          </div>
        </div>

        <div class="platform-metrics">
          <article class="metric-card">
            <span class="metric-label">业务定位</span>
            <strong>项目与需求生产平台</strong>
          </article>
          <article class="metric-card">
            <span class="metric-label">设计原则</span>
            <strong>清晰、可信、可治理</strong>
          </article>
        </div>
      </aside>

      <section class="login-panel">
        <div class="login-card">
          <div class="card-header">
            <p class="eyebrow">Secure Sign In</p>
            <h2>登录平台</h2>
            <p class="hint">
              登录后可进入项目管理中心、需求管理中心、模板管理中心、企业行业知识库和系统管理中心。
            </p>
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
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.08), transparent 28%),
    linear-gradient(180deg, #f5f7fb 0%, #eef3f8 100%);
  font-family:
    "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

.login-shell {
  min-height: calc(100vh - 64px);
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 460px);
  gap: 28px;
  max-width: 1360px;
  margin: 0 auto;
}

.brand-panel,
.login-card {
  border: 1px solid #dbe3ee;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
}

.brand-panel {
  display: grid;
  align-content: space-between;
  gap: 28px;
  padding: 44px;
  border-radius: 32px;
}

.brand-top {
  display: grid;
  gap: 24px;
}

.brand-mark {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: linear-gradient(135deg, #1d4ed8, #0f172a);
  color: #ffffff;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.brand-copy {
  display: grid;
  gap: 14px;
  max-width: 620px;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #2563eb;
}

.brand-copy h1,
.card-header h2 {
  margin: 0;
  color: #0f172a;
}

.brand-copy h1 {
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.lead,
.hint {
  margin: 0;
  color: #475569;
  line-height: 1.75;
}

.lead {
  max-width: 560px;
  font-size: 16px;
}

.trust-band {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.trust-item,
.metric-card {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid #dbe3ee;
  background: #f8fbff;
}

.trust-item strong,
.metric-card strong {
  display: block;
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 16px;
}

.trust-item span,
.metric-label {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.platform-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.metric-label {
  display: block;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.login-panel {
  display: grid;
  align-items: center;
}

.login-card {
  padding: 34px;
  border-radius: 28px;
}

.card-header {
  display: grid;
  gap: 10px;
  margin-bottom: 28px;
}

.card-header h2 {
  font-size: 30px;
  letter-spacing: -0.02em;
}

.form-group {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
}

.label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid #cfd8e3;
  background: #ffffff;
  color: #0f172a;
  font-size: 15px;
  transition: border-color 180ms ease, box-shadow 180ms ease;
}

.input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.primary {
  width: 100%;
  padding: 15px 18px;
  border: 1px solid #1d4ed8;
  border-radius: 16px;
  background: linear-gradient(180deg, #2563eb 0%, #1d4ed8 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, opacity 180ms ease;
}

.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.22);
}

.primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error {
  margin: 14px 0 0;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 13px;
}

.account-tip {
  margin-top: 18px;
  padding-top: 18px;
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

@media (max-width: 1120px) {
  .login-page {
    padding: 20px;
  }

  .login-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    padding: 28px;
  }

  .trust-band,
  .platform-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 14px;
  }

  .brand-panel,
  .login-card {
    padding: 22px;
    border-radius: 24px;
  }

  .brand-copy h1 {
    font-size: 30px;
  }

  .card-header h2 {
    font-size: 26px;
  }

  .account-tip {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
