<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type User } from '../api'

const users = ref<User[]>([])
const search = ref('')
const error = ref('')

async function load() {
  error.value = ''
  try { users.value = await api<User[]>(`/api/admin/users?search=${encodeURIComponent(search.value)}`) }
  catch (e) { error.value = (e as Error).message }
}
async function setEnabled(user: User) {
  try {
    await api<User>(`/api/admin/users/${user.id}/enabled`, {
      method: 'PATCH', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ enabled: !user.enabled }),
    })
    await load()
  } catch (e) { error.value = (e as Error).message }
}
onMounted(load)
</script>

<template>
  <section class="card">
    <h1>管理员界面 · 用户管理</h1>
    <p><RouterLink to="/app">切换到普通用户界面</RouterLink></p>
    <form class="search" @submit.prevent="load"><input v-model="search" placeholder="搜索用户名" /><button>搜索</button></form>
    <p v-if="error" class="error">{{ error === 'Access denied' ? 'Forbidden：当前账号没有管理员权限。' : error }}</p>
    <table><thead><tr><th>用户名</th><th>角色</th><th>状态</th><th>操作</th></tr></thead>
      <tbody><tr v-for="user in users" :key="user.id"><td>{{ user.username }}</td><td>{{ user.role }}</td><td>{{ user.enabled ? '启用' : '停用' }}</td><td><button @click="setEnabled(user)">{{ user.enabled ? '停用' : '启用' }}</button></td></tr></tbody>
    </table>
  </section>
</template>
