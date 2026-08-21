<template>
  <el-container class="app-shell">
    <el-header class="topbar">
      <div class="brand" @click="$router.push('/')">
        <span class="brand-mark">JD</span>
        <span>核心业务复刻系统</span>
      </div>
      <el-menu mode="horizontal" router :ellipsis="false" class="nav">
        <el-menu-item index="/">商品</el-menu-item>
        <el-menu-item v-if="hasRole('USER')" index="/orders">我的订单</el-menu-item>
        <el-menu-item v-if="hasRole('USER')" index="/refunds">我的售后</el-menu-item>
        <el-menu-item v-if="hasRole('MERCHANT', 'SUPER_ADMIN')" index="/merchant">商家后台</el-menu-item>
        <el-menu-item v-if="hasRole('SERVICE_ADMIN', 'PRODUCT_ADMIN', 'SUPER_ADMIN')" index="/admin">管理员后台</el-menu-item>
      </el-menu>
      <div class="account">
        <template v-if="store.user">
          <el-tag>{{ roleLabel(store.user.role) }}</el-tag>
          <span>{{ store.user.nickname || store.user.username }}</span>
          <el-button text @click="logout">退出</el-button>
        </template>
        <el-button v-else type="primary" @click="$router.push('/login')">登录</el-button>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { store, hasRole } from './store'

const router = useRouter()

function roleLabel(role) {
  const labels = {
    USER: '用户',
    MERCHANT: '商家',
    SERVICE_ADMIN: '客服管理员',
    PRODUCT_ADMIN: '商品审核员',
    SUPER_ADMIN: '超级管理员'
  }
  return labels[role] || role
}

function logout() {
  store.logout()
  router.push('/login')
}
</script>
