<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, type User } from '../api'

interface AppData { user: User; resource: string; content: string }
const router = useRouter()
const data = ref<AppData>()
const error = ref('')
onMounted(async () => {
  try { data.value = await api<AppData>('/api/app') }
  catch (e) { error.value = (e as Error).message; await router.push('/login') }
})
</script>

<template>
  <section class="card">
    <h1>普通用户界面</h1>
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="data">
      <p>你好，{{ data.user.username }}（{{ data.user.role }}）</p>
      <h2>{{ data.resource }}</h2><p>{{ data.content }}</p>
      <RouterLink v-if="data.user.role === 'ADMIN'" to="/admin">切换到管理员界面</RouterLink>
    </template>
  </section>
</template>
