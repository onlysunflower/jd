<template>
  <div class="page marketplace-page">
    <section class="mall-hero">
      <div class="mall-hero__content">
        <span class="eyebrow">JD SELECT · 精选好物</span>
        <h1>甄选品质好物，让每一次购物更简单</h1>
        <p>为你呈现已上架的优质商品。清晰的价格、库存与销量信息，让选购决策更轻松。</p>
      </div>
      <div class="mall-hero__metrics">
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

    <div class="commerce-search">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索你想要的商品"
        :prefix-icon="Search"
        @keyup.enter="load"
        @clear="load"
      />
      <el-button type="primary" :icon="Search" :loading="loading" @click="load">搜索</el-button>
    </div>

    <div class="category-rail" aria-label="商品分类">
      <button v-for="category in categories" :key="category.value" class="category-pill" :class="{ 'is-active': categoryId === category.value }" @click="selectCategory(category.value)">
        {{ category.label }}
      </button>
    </div>

    <div id="products" class="product-section__heading">
      <div><h2>精选商品</h2><span class="results-count">{{ resultDescription }}</span></div>
      <el-button text :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-if="loading" class="grid skeleton-grid">
      <div v-for="item in 8" :key="item" class="skeleton-card"><el-skeleton animated><template #template><el-skeleton-item variant="image" style="width:100%;height:164px" /><el-skeleton-item variant="h3" style="width:72%;margin-top:14px" /><el-skeleton-item variant="text" style="width:100%;margin-top:10px" /><el-skeleton-item variant="text" style="width:42%;margin-top:18px" /></template></el-skeleton></div>
    </div>
    <div v-else-if="products.length" class="grid">
      <div v-for="product in products" :key="product.id" class="product-card">
        <button class="product-image-wrap product-link" @click="openDetail(product)"><img class="product-image" :src="productImage(product)" :alt="product.name" @error="handleImageError" /></button>
        <div class="product-body">
          <div class="product-meta"><el-tag size="small" effect="plain" :type="stockType(product.stock)">{{ categoryName(product.categoryId) }}</el-tag><span class="stock-label" :class="stockClass(product.stock)">{{ stockText(product.stock) }}</span></div>
          <h3><button class="product-title-link" @click="openDetail(product)">{{ product.name }}</button></h3>
          <p class="product-subtitle">{{ product.subtitle || '品质好物，值得入手' }}</p>
          <div class="product-price-row"><span class="price"><em>¥</em>{{ product.price }}</span><span class="sales-label">已售 {{ product.sales || 0 }}</span></div>
          <div class="quantity-row">
            <span>库存 {{ product.stock }}</span>
            <el-input-number v-model="quantities[product.id]" :min="1" :max="Math.max(1, product.stock)" size="small" :disabled="!product.stock" />
          </div>
          <div class="product-actions"><el-button type="primary" :icon="CreditCard" :disabled="!product.stock" @click="buy(product)">立即下单</el-button><el-button :icon="ShoppingCart" :disabled="!product.stock" @click="addCart(product)">加入购物车</el-button></div>
        </div>
      </div>
    </div>
    <div v-else class="empty-state"><el-empty :description="loadError ? '商品加载失败' : '暂未找到匹配商品'" :image-size="112" /><p class="empty-state__copy">{{ loadError ? '请启动 MySQL、导入数据库脚本并运行后端服务后重试。' : '试试调整关键词或浏览其他商品分类。' }}</p><div style="text-align:center"><el-button type="primary" plain @click="loadError ? load() : clearFilters()">{{ loadError ? '重新加载' : '查看全部商品' }}</el-button></div></div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CreditCard, Refresh, Search, ShoppingCart } from '@element-plus/icons-vue'
import { cartApi, productApi } from '../api'
import { hasRole } from '../store'
import { productImage } from '../productVisuals'

const router = useRouter()
const keyword = ref('')
const categoryId = ref('')
const products = ref([])
const loading = ref(false)
const loadError = ref(false)
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
const resultDescription = computed(() => keyword.value ? `“${keyword.value}” 的搜索结果 · ${products.value.length} 件商品` : `共 ${products.value.length} 件在售商品`)

async function load() {
  loading.value = true
  loadError.value = false
  try {
    products.value = await productApi.list({ keyword: keyword.value, categoryId: categoryId.value || undefined })
    products.value.forEach((item) => { if (!quantities[item.id]) quantities[item.id] = 1 })
  } catch {
    loadError.value = true
    ElMessage.error('商品加载失败，请确认后端服务和数据库已启动')
  } finally { loading.value = false }
}

function selectCategory(value) { categoryId.value = value; load() }
function clearFilters() { keyword.value = ''; categoryId.value = ''; load() }
function handleImageError(event) { event.target.src = placeholder }
function openDetail(product) { router.push(`/products/${product.id}`) }

function categoryName(id) {
  return categories.find((item) => item.value === id)?.label || '精选'
}

function stockType(stock) {
  if (stock <= 10) return 'danger'
  if (stock <= 30) return 'warning'
  return 'success'
}

function stockText(stock) { return stock <= 0 ? '已售罄' : stock <= 10 ? '库存紧张' : '库存充足' }
function stockClass(stock) { return stock <= 0 ? 'stock-out' : stock <= 10 ? 'stock-low' : 'stock-ok' }

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

function buy(product) {
  if (!ensureUser()) return
  router.push({ path: '/checkout', query: { productId: product.id, quantity: quantities[product.id], source: 'direct' } })
}

onMounted(load)
</script>
