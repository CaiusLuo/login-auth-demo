<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, login, type User } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref('')

async function submit() {
  error.value = ''
  try {
    await login(username.value, password.value)
    const me = await api<User>('/api/auth/me')
    await router.push(me.role === 'ADMIN' ? '/admin' : '/app')
  } catch (e) { error.value = (e as Error).message }
}
</script>

<template>
  <section class="card narrow">
    <h1>登录</h1>
    <form @submit.prevent="submit">
      <label>用户名<input v-model="username" autocomplete="username" required /></label>
      <label>密码<input v-model="password" type="password" autocomplete="current-password" required /></label>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit">登录</button>
    </form>
    <p>没有账号？<RouterLink to="/register">注册普通用户</RouterLink></p>
  </section>
</template>
