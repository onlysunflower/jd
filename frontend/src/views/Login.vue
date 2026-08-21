<template>
  <div class="page login-page">
    <el-card class="login-card" shadow="never">
      <template #header>
        <strong>账号登录</strong>
      </template>
      <el-form :model="form" label-width="72px">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="user001 / merchant001 / super_admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="login">登录</el-button>
          <el-button @click="register">注册普通用户</el-button>
        </el-form-item>
      </el-form>
      <el-divider />
      <el-table :data="accounts" size="small">
        <el-table-column prop="role" label="角色" />
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="password" label="密码" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="useAccount(row)">填入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { store } from '../store'

const router = useRouter()
const form = reactive({ username: 'user001', password: '123456' })

const accounts = [
  { role: '普通用户', username: 'user001', password: '123456' },
  { role: '商家', username: 'merchant001', password: '123456' },
  { role: '客服管理员', username: 'service_admin', password: '123456' },
  { role: '商品审核员', username: 'product_admin', password: '123456' },
  { role: '超级管理员', username: 'super_admin', password: '123456' }
]

function useAccount(row) {
  form.username = row.username
  form.password = row.password
}

async function login() {
  const data = await authApi.login(form)
  store.setLogin(data)
  ElMessage.success('登录成功')
  if (data.user.role === 'MERCHANT') router.push('/merchant')
  else if (data.user.role.includes('ADMIN')) router.push('/admin')
  else router.push('/')
}

async function register() {
  const data = await authApi.register({
    username: form.username,
    password: form.password,
    nickname: form.username
  })
  ElMessage.success(`注册成功：${data.username}`)
}
</script>

<style scoped>
.login-page {
  display: grid;
  place-items: start center;
  padding-top: 48px;
}

.login-card {
  width: min(720px, 100%);
}
</style>
