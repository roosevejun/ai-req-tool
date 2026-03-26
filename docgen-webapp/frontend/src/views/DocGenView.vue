<template>
  <div class="page">
    <div class="layout">
      <aside class="sidebar">
        <ProjectRequirementTree
          :selected-project-id="selectedProjectId"
          :selected-requirement-id="selectedRequirementId"
          @select-project="onSelectProject"
          @select-requirement="onSelectRequirement"
          @error="showError"
        />
      </aside>

      <main class="content">
        <section class="card">
          <h3>瑜版挸澧犳稉濠佺瑓閺?/h3>
          <div class="meta-grid">
            <div><strong>妞ゅ湱娲伴敍?/strong>{{ selectedProjectId || '-' }}</div>
            <div><strong>闂団偓濮瑰偊绱?/strong>{{ selectedRequirementId || '-' }}</div>
          </div>
        </section>

        <section v-if="selectedProjectId" class="card">
          <h3>閸︺劑銆嶉惄?{{ selectedProjectId }} 娑撳鍨卞娲付濮?/h3>
          <div class="form-grid">
            <input v-model.trim="reqForm.title" class="input" placeholder="闂団偓濮瑰倹鐖ｆ０? />
            <select v-model="reqForm.priority" class="input">
              <option value="P0">P0</option>
              <option value="P1">P1</option>
              <option value="P2">P2</option>
              <option value="P3">P3</option>
            </select>
            <select v-model="reqForm.status" class="input">
              <option value="DRAFT">DRAFT</option>
              <option value="CLARIFYING">CLARIFYING</option>
              <option value="READY_REVIEW">READY_REVIEW</option>
              <option value="DONE">DONE</option>
            </select>
          </div>
          <textarea v-model="reqForm.summary" class="input" placeholder="闂団偓濮瑰倹鎲崇憰渚婄礄閸欘垶鈧绱? />
          <div class="row">
            <button class="primary" :disabled="loading || !reqForm.title" @click="createRequirement">閸掓稑缂撻棁鈧Ч?/button>
            <button class="ghost" :disabled="loading" @click="loadRequirements">閸掗攱鏌婇崚妤勩€?/button>
          </div>
        </section>

        <section v-if="selectedProjectId" class="card">
          <h3>闂団偓濮瑰倸鍨悰顭掔礄妞ゅ湱娲?{{ selectedProjectId }}閿?/h3>
          <table class="table">
            <thead>
            <tr>
              <th>ID</th>
              <th>缂傛牕褰?/th>
              <th>閺嶅洭顣?/th>
              <th>娴兼ê鍘涚痪?/th>
              <th>閻樿埖鈧?/th>
              <th>閹垮秳缍?/th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="r in requirements" :key="r.id">
              <td>{{ r.id }}</td>
              <td>{{ r.requirementNo }}</td>
              <td>{{ r.title }}</td>
              <td>{{ r.priority }}</td>
              <td>{{ r.status }}</td>
              <td class="ops">
                <button class="mini" @click="onSelectRequirement({ projectId: selectedProjectId!, requirementId: r.id })">闁鑵?/button>
                <button class="mini" @click="goWorkbench(r.id)">瀹搞儰缍旈崣?/button>
                <button class="mini" @click="goVersions(r.id)">閻楀牊婀?/button>
              </td>
            </tr>
            <tr v-if="requirements.length === 0">
              <td colspan="6" class="empty small">閺嗗倹妫ら棁鈧Ч?/td>
            </tr>
            </tbody>
          </table>
        </section>

        <section v-if="selectedRequirementId" class="card">
          <div class="row between">
            <h3>AI瀹搞儰缍旈崣甯礄闂団偓濮?{{ selectedRequirementId }}閿?/h3>
            <div class="row">
              <button class="ghost mini" @click="goWorkbench(selectedRequirementId)">閹垫挸绱戠€瑰本鏆ｅ銉ょ稊閸?/button>
              <button class="ghost mini" @click="goVersions(selectedRequirementId)">閹垫挸绱戦悧鍫熸拱妞?/button>
            </div>
          </div>
          <DocGenPage :api-base="`/api/requirements/${selectedRequirementId}/docgen`" :draft-key="`docgen-draft-req-${selectedRequirementId}`" />
        </section>

        <section v-else class="card empty-state">
          <h3>鐠囧嘲鍘涙禒搴′箯娓氀囥€嶉惄顔界埐闁瀚ㄦ稉鈧稉顏堟付濮?/h3>
          <p>闁瀚ㄩ崥搴㈡拱妞ゅ吀绱伴惄瀛樺复鐏炴洜銇氱拠銉╂付濮瑰倻娈?AI 鐎电鐦介崠鎭掆偓?/p>
        </section>
      </main>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import DocGenPage from '../components/DocGenPage.vue'
import ProjectRequirementTree from '../components/ProjectRequirementTree.vue'

type ApiResponse<T> = { code: number; message: string; data: T; traceId: string }
type RequirementItem = {
  id: number
  requirementNo: string
  title: string
  summary: string
  priority: string
  status: string
}

const router = useRouter()
const loading = ref(false)
const error = ref('')
const success = ref('')

const selectedProjectId = ref<number | null>(null)
const selectedRequirementId = ref<number | null>(null)
const requirements = ref<RequirementItem[]>([])

const reqForm = reactive({
  title: '',
  summary: '',
  priority: 'P2',
  status: 'DRAFT'
})

function showError(msg: string) {
  error.value = msg
}
`r`n
function goWorkbench(requirementId: number) {
  if (!selectedProjectId.value) return
  router.push(`/requirements/${requirementId}/workbench?projectId=${selectedProjectId.value}`)
}

function goVersions(requirementId: number) {
  if (!selectedProjectId.value) return
  router.push(`/requirements/${requirementId}/versions?projectId=${selectedProjectId.value}`)
}

async function loadRequirements() {
  if (!selectedProjectId.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get<ApiResponse<RequirementItem[]>>(`/api/projects/${selectedProjectId.value}/requirements`)
    requirements.value = res.data.data || []
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Failed to load requirements'
  } finally {
    loading.value = false
  }
}

async function createRequirement() {
  if (!selectedProjectId.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await axios.post<ApiResponse<number>>(`/api/projects/${selectedProjectId.value}/requirements`, reqForm)
    success.value = '闂団偓濮瑰倸鍨卞鐑樺灇閸?
    reqForm.title = ''
    reqForm.summary = ''
    reqForm.priority = 'P2'
    reqForm.status = 'DRAFT'
    await loadRequirements()
    if (res.data?.data) {
      selectedRequirementId.value = res.data.data
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Failed to create requirement'
  } finally {
    loading.value = false
  }
}

async function onSelectProject(projectId: number) {
  selectedProjectId.value = projectId
  selectedRequirementId.value = null
  await loadRequirements()
}

async function onSelectRequirement(payload: { projectId: number; requirementId: number }) {
  selectedProjectId.value = payload.projectId
  selectedRequirementId.value = payload.requirementId
  if (requirements.value.length === 0 || !requirements.value.some(r => r.id === payload.requirementId)) {
    await loadRequirements()
  }
}

watch(selectedProjectId, async (pid) => {
  if (pid) {
    await loadRequirements()
  } else {
    requirements.value = []
  }
})
</script>

<style scoped>
.page {
  max-width: 1320px;
  margin: 18px auto;
  padding: 0 14px 18px;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}`r`nh1 {
  margin: 0;
  font-size: 24px;
}`r`n.layout {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 14px;
}
.card {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 10px;
  font-size: 14px;
}
.form-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 10px;
}
.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
  margin-top: 8px;
  background: #fff;
}
textarea.input {
  min-height: 72px;
  resize: vertical;
}
.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 8px;
}
.table th,
.table td {
  border: 1px solid #e5e7eb;
  padding: 8px;
  font-size: 13px;
}
.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.between {
  justify-content: space-between;
  align-items: center;
}
.primary,
.ghost,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
}
.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.ghost,
.mini {
  background: #f3f4f6;
}
.mini {
  padding: 5px 9px;
  font-size: 12px;
}
.error {
  margin-top: 8px;
  color: #b91c1c;
}
.success {
  margin-top: 8px;
  color: #166534;
}
.empty {
  color: #6b7280;
}
.small {
  font-size: 12px;
}
.empty-state {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .meta-grid {
    grid-template-columns: 1fr;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
