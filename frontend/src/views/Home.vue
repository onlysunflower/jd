<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>商品商城</h2>
        <p class="muted">只展示审核通过并已上架的商品</p>
      </div>
      <el-input v-model="keyword" clearable placeholder="搜索商品" style="width: 260px" @keyup.enter="load" />
    </div>

    <div class="grid">
      <div v-for="product in products" :key="product.id" class="product-card">
        <img :src="product.mainImage || placeholder" alt="商品图" />
        <div class="product-body">
          <h3>{{ product.name }}</h3>
          <p class="muted">{{ product.subtitle }}</p>
          <div class="toolbar">
            <span class="price">￥{{ product.price }}</span>
            <span class="muted">库存 {{ product.stock }}</span>
          </div>
          <el-space>
            <el-input-number v-model="quantities[product.id]" :min="1" :max="product.stock" size="small" />
            <el-button type="primary" @click="buy(product)">立即下单</el-button>
            <el-button @click="addCart(product)">加购物车</el-button>
          </el-space>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartApi, orderApi, productApi } from '../api'
import { hasRole } from '../store'

const router = useRouter()
const keyword = ref('')
const products = ref([])
const quantities = reactive({})
const placeholder = 'https://dummyimage.com/600x450/f3f4f6/6b7280&text=JD+Product'

async function load() {
  products.value = await productApi.list({ keyword: keyword.value })
  products.value.forEach((item) => {
    if (!quantities[item.id]) quantities[item.id] = 1
  })
}

function ensureUser() {
  if (!hasRole('USER')) {
    ElMessage.warning('请使用普通用户账号操作')
    router.push('/login')
    return false
  }
  return true
}

async function addCart(product) {
  if (!ensureUser()) return
  await cartApi.add({ productId: product.id, quantity: quantities[product.id] })
  ElMessage.success('已加入购物车')
}

async function buy(product) {
  if (!ensureUser()) return
  const order = await orderApi.create({
    productId: product.id,
    quantity: quantities[product.id],
    receiver: '演示用户',
    receiverPhone: '13800000000',
    receiverAddress: '北京市朝阳区京东课程项目演示地址'
  })
  ElMessage.success(`订单已创建：${order.orderNo}`)
  router.push('/orders')
}

onMounted(load)
</script>
