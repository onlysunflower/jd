<template>
  <div class="page login-page">
    <div class="login-layout">
      <aside class="login-brand-panel">
        <div>
          <span class="brand-mark">JD</span>
          <h1>发现好物，轻松选购</h1>
          <p>登录京东精选商城，继续查看订单、售后进度和为你精选的品质商品。</p>
        </div>
        <div class="login-trust"><span><strong>品质保障</strong>严选优质商品</span><span><strong>安心服务</strong>售后进度可追踪</span></div>
      </aside>
      <section class="login-form-panel">
        <div class="login-heading"><h2>欢迎回来</h2><p>使用你的商城账号继续</p></div>
        <el-form :model="form" label-position="top">
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="login" />
        </el-form-item>
        <el-form-item>
            <el-button class="login-primary" type="primary" :loading="loading" @click="login">登录</el-button>
        </el-form-item>
      </el-form>
        <div class="login-register"><el-button link type="primary" @click="register">还没有账号？注册普通用户</el-button></div>
        <div class="demo-accounts">
          <div class="demo-accounts__head"><span>演示账号</span><small>点击即可填入密码 123456</small></div>
          <div class="demo-account-list"><button v-for="account in accounts" :key="account.username" class="demo-account" @click="useAccount(account)">{{ account.role }} · {{ account.username }}</button></div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { store } from '../store'

const router = useRouter()
const form = reactive({ username: 'user001', password: '123456' })
const loading = ref(false)

const accounts = [
  { role: '普通用户', username: 'user001', password: '123456' },
  { role: '普通用户二号', username: 'user002', password: '123456' },
  { role: '商家', username: 'merchant001', password: '123456' },
  { role: '商家二号', username: 'merchant002', password: '123456' },
  { role: '客服管理员', username: 'service_admin', password: '123456' },
  { role: '商品审核员', username: 'product_admin', password: '123456' },
  { role: '超级管理员', username: 'super_admin', password: '123456' }
]

function useAccount(row) {
  form.username = row.username
  form.password = row.password
}

async function login() {
  loading.value = true
  try {
    const data = await authApi.login(form)
    store.setLogin(data)
    ElMessage.success('登录成功')
    if (data.user.role === 'MERCHANT') router.push('/merchant')
    else if (data.user.role.includes('ADMIN')) router.push('/admin')
    else router.push('/')
  } catch {
    ElMessage.error('登录失败：请确认 MySQL 已启动、已导入数据库脚本，并启动后端服务')
  } finally { loading.value = false }
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
