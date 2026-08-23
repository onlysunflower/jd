<template>
  <div class="page">
    <section class="hero">
      <div class="hero-main">
        <el-tag effect="dark" type="danger">课程项目演示商城</el-tag>
        <h1>把订单、售后、商家审核这条主流程跑完整</h1>
        <p>这里展示审核通过并已上架的商品。你可以使用普通用户账号下单支付，再切换商家和管理员账号测试发货、退款、平台介入和仲裁。</p>
        <el-space wrap>
          <el-button type="primary" :icon="ShoppingCart" @click="scrollToProducts">开始选购</el-button>
          <el-button plain :icon="Refresh" @click="load">刷新商品</el-button>
        </el-space>
      </div>
      <div class="hero-side">
        <div class="metric-card">
          <span class="muted">已上架商品</span>
          <strong class="metric-value">{{ products.length }}</strong>
        </div>
        <div class="metric-card">
          <span class="muted">总库存</span>
          <strong class="metric-value">{{ totalStock }}</strong>
        </div>
        <div class="metric-card">
          <span class="muted">累计销量</span>
          <strong class="metric-value">{{ totalSales }}</strong>
        </div>
      </div>
    </section>

    <div id="products" class="section-title">
      <div>
        <h2>商品商城</h2>
        <p class="muted">按分类筛选商品，库存不足或未审核商品不会展示</p>
      </div>
    </div>

    <div class="filter-bar">
      <el-segmented v-model="categoryId" :options="categories" @change="load" />
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索手机、电脑、家电、配件"
        style="width: min(340px, 100%)"
        :prefix-icon="Search"
        @keyup.enter="load"
        @clear="load"
      />
    </div>

    <div v-if="products.length" class="grid">
      <div v-for="product in products" :key="product.id" class="product-card">
        <img :src="product.mainImage || placeholder" alt="商品图" />
        <div class="product-body">
          <el-tag size="small" :type="stockType(product.stock)">{{ categoryName(product.categoryId) }}</el-tag>
          <h3>{{ product.name }}</h3>
          <p class="product-subtitle muted">{{ product.subtitle }}</p>
          <div class="toolbar">
            <span class="price">￥{{ product.price }}</span>
            <span class="muted">已售 {{ product.sales || 0 }}</span>
          </div>
          <div class="toolbar">
            <span class="muted">库存 {{ product.stock }}</span>
            <el-input-number v-model="quantities[product.id]" :min="1" :max="product.stock" size="small" />
          </div>
          <el-space fill wrap style="width: 100%">
            <el-button type="primary" :icon="CreditCard" @click="buy(product)">立即下单</el-button>
            <el-button :icon="ShoppingCart" @click="addCart(product)">加入购物车</el-button>
          </el-space>
        </div>
      </div>
    </div>
    <div v-else class="empty-hint">没有找到符合条件的商品</div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CreditCard, Refresh, Search, ShoppingCart } from '@element-plus/icons-vue'
import { cartApi, orderApi, productApi } from '../api'
import { hasRole } from '../store'

const router = useRouter()
const keyword = ref('')
const categoryId = ref('')
const products = ref([])
const quantities = reactive({})
const placeholder = 'https://dummyimage.com/600x450/f3f4f6/6b7280&text=JD+Product'

const categories = [
  { label: '全部', value: '' },
  { label: '手机数码', value: 1 },
  { label: '电脑办公', value: 2 },
  { label: '家用电器', value: 3 },
  { label: '生活百货', value: 4 },
  { label: '生鲜食品', value: 5 }
]

const totalStock = computed(() => products.value.reduce((sum, item) => sum + Number(item.stock || 0), 0))
const totalSales = computed(() => products.value.reduce((sum, item) => sum + Number(item.sales || 0), 0))

async function load() {
  products.value = await productApi.list({
    keyword: keyword.value,
    categoryId: categoryId.value || undefined
  })
  products.value.forEach((item) => {
    if (!quantities[item.id]) quantities[item.id] = 1
  })
}

function categoryName(id) {
  return categories.find((item) => item.value === id)?.label || '精选'
}

function stockType(stock) {
  if (stock <= 10) return 'danger'
  if (stock <= 30) return 'warning'
  return 'success'
}

function scrollToProducts() {
  document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' })
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
