<template>
  <div class="page cart-page">
    <div class="toolbar">
      <div>
        <h2>购物车</h2>
        <p class="muted">可自由勾选多件商品，同店商品合并下单，跨店商品自动拆单</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="loadError" class="cart-alert" type="error" show-icon :closable="false" title="购物车加载失败，请检查网络后重试。">
      <template #default><el-button link type="primary" @click="load">重新加载</el-button></template>
    </el-alert>

    <div v-if="loading" class="cart-list">
      <div v-for="item in 3" :key="item" class="cart-row skeleton-card"><el-skeleton animated :rows="3" /></div>
    </div>
    <div v-else-if="items.length" class="cart-list">
      <article v-for="row in items" :key="row.id" class="cart-row" :class="{ 'is-unavailable': !row.purchasable }">
        <div class="cart-select">
          <el-checkbox v-model="selectedIds" :value="row.id" :disabled="!row.purchasable || isBusy(row.id)">
            <span class="sr-only">选择 {{ row.productName || '该商品' }}</span>
          </el-checkbox>
        </div>
        <div class="cart-image-wrap">
          <img v-if="row.productImage" :src="row.productImage" :alt="row.productName" @error="hideBrokenImage" />
          <el-icon v-else><Goods /></el-icon>
        </div>
        <div class="cart-product">
          <strong>{{ row.productName || '商品已删除' }}</strong>
          <p class="stock-copy">规格：{{ row.specName || '默认规格' }}</p>
          <p v-if="row.purchasable" class="stock-copy">可售库存 {{ row.availableStock }}</p>
          <p v-else class="unavailable-copy">{{ row.unavailableReason || '商品当前不可结算' }}</p>
        </div>
        <div class="cart-price"><small>单价</small><span class="price small"><em>¥</em>{{ money(row.price) }}</span></div>
        <div class="cart-quantity">
          <small>数量</small>
          <el-input-number
            :model-value="row.quantity"
            :min="1"
            :max="Math.max(1, Number(row.availableStock || 1))"
            size="small"
            :disabled="isBusy(row.id) || !canUpdate(row)"
            @change="(value) => updateQuantity(row, value)"
          />
        </div>
        <div class="cart-subtotal"><small>小计</small><span class="price small"><em>¥</em>{{ subtotal(row) }}</span></div>
        <div class="cart-actions"><el-button link type="danger" :loading="removingId === row.id" :disabled="isBusy(row.id)" @click="remove(row)">删除</el-button></div>
      </article>
    </div>
    <div v-else-if="!loadError" class="empty-state">
      <el-empty description="购物车还是空的" :image-size="112" />
      <p class="empty-state__copy">先去挑选喜欢的商品吧。</p>
      <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
    </div>

    <section v-if="items.length" class="cart-summary">
      <div class="cart-summary__select">
        <el-checkbox :model-value="allSelected" :indeterminate="partlySelected" :disabled="!purchasableItems.length" @change="toggleAll">全选</el-checkbox>
        <span class="muted">共 {{ items.length }} 种商品</span>
      </div>
      <div class="cart-summary__actions"><span v-if="selectedItems.length" class="muted">已选 {{ selectedItems.length }} 种，共 {{ selectedQuantity }} 件，合计 <b class="price small"><em>¥</em>{{ selectedTotal }}</b></span><span v-else class="muted">请选择要结算的商品</span><el-button type="primary" :disabled="!selectedItems.length || selectedBusy" @click="checkout">去确认订单</el-button></div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Goods, Refresh } from '@element-plus/icons-vue'
import { cartApi } from '../api'

const router = useRouter()
const items = ref([])
const loading = ref(false)
const loadError = ref(false)
const selectedIds = ref([])
const updatingId = ref(null)
const removingId = ref(null)

const purchasableItems = computed(() => items.value.filter((item) => item.purchasable))
const selectedItems = computed(() => items.value.filter((item) => selectedIds.value.includes(item.id) && item.purchasable))
const selectedQuantity = computed(() => selectedItems.value.reduce((total, item) => total + Number(item.quantity || 0), 0))
const selectedTotal = computed(() => selectedItems.value.reduce((total, item) => total + Number(item.price || 0) * Number(item.quantity || 0), 0).toFixed(2))
const allSelected = computed(() => purchasableItems.value.length > 0 && selectedItems.value.length === purchasableItems.value.length)
const partlySelected = computed(() => selectedItems.value.length > 0 && !allSelected.value)
const selectedBusy = computed(() => selectedIds.value.some((id) => isBusy(id)))

function money(value) { return Number(value || 0).toFixed(2) }
function subtotal(row) { return (Number(row.price || 0) * Number(row.quantity || 0)).toFixed(2) }
function isBusy(id) { return updatingId.value === id || removingId.value === id }
function canUpdate(row) { return Number(row.availableStock || 0) > 0 }
function hideBrokenImage(event) { event.target.style.display = 'none' }
function toggleAll(checked) { selectedIds.value = checked ? purchasableItems.value.map((item) => item.id) : [] }

async function load() {
  loading.value = true
  loadError.value = false
  try {
    items.value = await cartApi.list()
    const availableIds = new Set(items.value.filter((item) => item.purchasable).map((item) => item.id))
    selectedIds.value = selectedIds.value.filter((id) => availableIds.has(id))
  } catch (error) {
    loadError.value = true
    ElMessage.error(error.message || '购物车加载失败')
  } finally {
    loading.value = false
  }
}

async function updateQuantity(row, value) {
  const quantity = Number(value)
  if (!Number.isInteger(quantity) || quantity < 1 || quantity === row.quantity) return
  updatingId.value = row.id
  try {
    const updated = await cartApi.update(row.id, quantity)
    const index = items.value.findIndex((item) => item.id === row.id)
    if (index !== -1) items.value[index] = updated
    if (!updated.purchasable) selectedIds.value = selectedIds.value.filter((id) => id !== row.id)
    ElMessage.success('数量已更新')
  } catch (error) {
    ElMessage.error(error.message || '数量更新失败')
    await load()
  } finally {
    updatingId.value = null
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除“${row.productName || '该商品'}”吗？`, '删除购物车商品', { type: 'warning' })
  } catch {
    return
  }
  removingId.value = row.id
  try {
    await cartApi.remove(row.id)
    items.value = items.value.filter((item) => item.id !== row.id)
    selectedIds.value = selectedIds.value.filter((id) => id !== row.id)
    ElMessage.success('商品已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  } finally {
    removingId.value = null
  }
}

function checkout() {
  if (!selectedItems.value.length) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  router.push({ path: '/checkout', query: { cartItemIds: selectedItems.value.map((item) => item.id).join(','), source: 'cart' } })
}

onMounted(load)
</script>

<style scoped>
.cart-alert { margin-bottom: 16px; }
.cart-list { display: grid; gap: 12px; }
.cart-row { display: grid; grid-template-columns: 38px 92px minmax(180px, 1fr) 110px 150px 110px 64px; gap: 16px; align-items: center; padding: 18px; border: 1px solid var(--line); border-radius: 12px; background: #fff; }
.cart-row.is-unavailable { background: #fafafa; }
.cart-select { display: flex; justify-content: center; }
.cart-image-wrap { display: flex; align-items: center; justify-content: center; width: 88px; height: 88px; overflow: hidden; border-radius: 8px; background: #f3f4f6; color: #9ca3af; font-size: 28px; }
.cart-image-wrap img { width: 100%; height: 100%; object-fit: cover; }
.cart-product strong { display: block; line-height: 1.5; }
.cart-product p { margin: 8px 0 0; font-size: 13px; }
.stock-copy { color: #64748b; }
.unavailable-copy { color: #dc2626; }
.cart-price, .cart-quantity, .cart-subtotal { display: grid; gap: 6px; }
.cart-row small { color: #94a3b8; }
.cart-actions { text-align: right; }
.cart-summary { position: sticky; bottom: 16px; display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: 20px; padding: 16px 20px; border: 1px solid var(--line); border-radius: 12px; background: rgba(255, 255, 255, .96); box-shadow: 0 8px 24px rgba(15, 23, 42, .08); }
.cart-summary > div { display: flex; align-items: center; gap: 12px; }
.cart-summary__select { flex-shrink: 0; }
.cart-summary__actions { margin-left: auto; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; }
@media (max-width: 900px) { .cart-row { grid-template-columns: 32px 72px 1fr 104px; gap: 12px; } .cart-image-wrap { width: 72px; height: 72px; } .cart-price, .cart-quantity, .cart-subtotal, .cart-actions { grid-column: 3 / -1; } .cart-actions { text-align: left; } .cart-summary { align-items: flex-start; flex-direction: column; } .cart-summary__actions { width: 100%; margin-left: 0; justify-content: space-between; } }
</style>
