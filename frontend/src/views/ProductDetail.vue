<template>
  <div class="page detail-page">
    <el-button text :icon="ArrowLeft" @click="router.back()">返回商品列表</el-button>
    <div v-if="loading" class="detail-card"><el-skeleton animated :rows="8" /></div>
    <template v-else-if="product">
      <section class="detail-card">
        <div class="detail-image-panel"><img :src="productImage(product)" :alt="product.name" /></div>
        <div class="detail-info">
          <el-tag effect="plain" type="danger">{{ categoryName(product.categoryId) }}</el-tag>
          <h1>{{ product.name }}</h1>
          <p class="detail-subtitle">{{ product.subtitle || '精选优质商品，支持模拟下单与售后服务。' }}</p>
          <div class="detail-price"><small>商城价</small><strong><em>¥</em>{{ product.price }}</strong></div>
          <div class="detail-facts"><span>库存 <b>{{ product.stock }}</b></span><span>累计销量 <b>{{ product.sales || 0 }}</b></span><button class="detail-review-button" @click="reviewsVisible = true"><el-icon><ChatDotRound /></el-icon>查看商品评价</button><span>支持 <b>7 天售后</b></span></div>
          <div class="detail-buy-row">
            <el-input-number v-model="quantity" :min="1" :max="Math.max(1, product.stock)" :disabled="!product.stock" />
            <el-button type="primary" size="large" :icon="CreditCard" :disabled="!product.stock" @click="buy">立即下单</el-button>
            <el-button size="large" :icon="ShoppingCart" :disabled="!product.stock" @click="addCart">加入购物车</el-button>
          </div>
          <p class="detail-service">{{ product.stock ? '现货商品，模拟支付后由商家安排发货。' : '该商品暂时缺货，请浏览其他商品。' }}</p>
        </div>
      </section>
      <section class="detail-section">
        <h2>商品介绍</h2>
        <p>{{ product.subtitle || '本商品已通过平台审核，可用于演示浏览、加入购物车、下单、支付和售后等完整用户流程。' }}</p>
        <ul><li>平台审核后上架，库存和销量会随订单状态变化。</li><li>支持模拟支付、商家发货、用户确认收货及售后申请。</li><li>售后进度由用户、商家和平台管理员共同流转。</li></ul>
      </section>
      <ProductReviewsDialog v-model="reviewsVisible" :product="product" />
    </template>
    <div v-else class="empty-state"><el-empty description="商品暂时不可查看" /><el-button type="primary" @click="router.push('/')">返回首页</el-button></div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatDotRound, CreditCard, ShoppingCart } from '@element-plus/icons-vue'
import ProductReviewsDialog from '../components/ProductReviewsDialog.vue'
import { cartApi, productApi } from '../api'
import { hasRole } from '../store'
import { productImage } from '../productVisuals'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const quantity = ref(1)
const loading = ref(true)
const reviewsVisible = ref(false)
const categories = { 1: '手机数码', 2: '电脑办公', 3: '家用电器', 4: '生活百货', 5: '生鲜食品' }

function categoryName(id) { return categories[id] || '精选商品' }
function ensureUser() {
  if (hasRole('USER')) return true
  ElMessage.warning('请先使用普通用户账号登录')
  router.push('/login')
  return false
}
async function load() {
  loading.value = true
  try { product.value = await productApi.detail(route.params.id) }
  catch { ElMessage.error('商品加载失败，请确认后端服务和数据库已启动') }
  finally { loading.value = false }
}
async function addCart() {
  if (!ensureUser()) return
  await cartApi.add({ productId: product.value.id, quantity: quantity.value })
  ElMessage.success('已加入购物车')
}
function buy() {
  if (!ensureUser()) return
  router.push({ path: '/checkout', query: { productId: product.value.id, quantity: quantity.value, source: 'direct' } })
}
onMounted(load)
</script>
