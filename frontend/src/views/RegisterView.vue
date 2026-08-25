<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, type User } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  busy.value = true
  try {
    await api<User>('/api/auth/register', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value }),
    })
    await router.push('/login')
  } catch (e) { error.value = (e as Error).message } finally { busy.value = false }
}
</script>

<template>
  <section class="card narrow">
    <h1>注册</h1>
    <p>用户名将经过本地规则和内容审核；注册账号固定为普通用户。</p>
    <form @submit.prevent="submit">
      <label>用户名<input v-model="username" minlength="3" maxlength="32" required /></label>
      <label>密码<input v-model="password" type="password" minlength="8" maxlength="72" required /></label>
      <p v-if="error" class="error">{{ error }}</p>
      <button :disabled="busy" type="submit">{{ busy ? '审核中…' : '注册' }}</button>
    </form>
    <RouterLink to="/login">返回登录</RouterLink>
  </section>
</template>
