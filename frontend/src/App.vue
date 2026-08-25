<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, logout, type User } from './api'

const router = useRouter()
const route = useRoute()
const me = ref<User>()
watch(() => route.path, async () => {
  if (route.path === '/login' || route.path === '/register') { me.value = undefined; return }
  try { me.value = await api<User>('/api/auth/me') } catch { me.value = undefined }
}, { immediate: true })
async function signOut() {
  try { await logout() } finally { await router.push('/login') }
}
</script>

<template>
  <header>
    <strong>登录授权 Demo</strong>
    <nav>
      <RouterLink v-if="me" to="/app">普通界面</RouterLink>
      <RouterLink v-if="me?.role === 'ADMIN'" to="/admin">管理员界面</RouterLink>
      <button v-if="me" class="link" @click="signOut">退出</button>
    </nav>
  </header>
  <main><RouterView /></main>
</template>
