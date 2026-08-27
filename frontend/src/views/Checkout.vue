<template>
  <div class="page checkout-page">
    <el-button text :icon="ArrowLeft" @click="goBack">返回</el-button>
    <div class="toolbar checkout-heading"><div><h2>确认订单</h2><p class="muted">请确认商品与收货信息后提交订单</p></div></div>

    <div v-if="loading" class="checkout-card"><el-skeleton animated :rows="7" /></div>
    <div v-else-if="errorMessage" class="empty-state">
      <el-empty :description="errorMessage" :image-size="112" />
      <p class="empty-state__copy">请返回后重新选择可购买商品。</p>
      <el-button type="primary" @click="goBack">返回{{ isCartSource ? '购物车' : '商品列表' }}</el-button>
    </div>
    <template v-else-if="product">
      <section class="checkout-card">
        <h3>商品信息</h3>
        <div class="checkout-product">
          <img :src="productImage(product)" :alt="product.name" />
          <div class="checkout-product__name"><strong>{{ product.name }}</strong><span>单价 <b class="price small"><em>¥</em>{{ money(product.price) }}</b></span><span>库存 {{ product.stock }}</span></div>
          <div class="checkout-product__quantity"><span>数量</span><b>{{ quantity }}</b></div>
          <div class="checkout-product__subtotal"><span>小计</span><strong class="price"><em>¥</em>{{ totalAmount }}</strong></div>
        </div>
        <el-alert class="stock-notice" type="warning" show-icon :closable="false" title="创建订单不锁库存，支付时以实时库存为准" />
      </section>

      <section class="checkout-card">
        <h3>收货信息</h3>
        <el-form label-position="top" :disabled="submitting">
          <el-form-item label="收货人" :error="fieldErrors.receiver"><el-input v-model.trim="form.receiver" maxlength="64" placeholder="请输入收货人姓名" /></el-form-item>
          <el-form-item label="手机号" :error="fieldErrors.receiverPhone"><el-input v-model.trim="form.receiverPhone" maxlength="11" placeholder="请输入 11 位中国大陆手机号" /></el-form-item>
          <el-form-item label="收货地址" :error="fieldErrors.receiverAddress"><el-input v-model.trim="form.receiverAddress" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="请输入省、市、区及详细地址" /></el-form-item>
        </el-form>
      </section>

      <section class="checkout-submit"><div><span class="muted">应付金额</span><strong class="price"><em>¥</em>{{ totalAmount }}</strong></div><el-button type="primary" size="large" :loading="submitting" :disabled="submitting" @click="submit">提交订单</el-button></section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { orderApi, productApi } from '../api'
import { productImage } from '../productVisuals'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const quantity = ref(0)
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({ receiver: '', receiverPhone: '', receiverAddress: '' })
const fieldErrors = reactive({ receiver: '', receiverPhone: '', receiverAddress: '' })
const isCartSource = computed(() => route.query.source === 'cart')
const totalAmount = computed(() => (Number(product.value?.price || 0) * quantity.value).toFixed(2))

function money(value) { return Number(value || 0).toFixed(2) }
function goBack() { router.push(isCartSource.value ? '/cart' : '/') }

function validateProduct(data) {
  if (data.auditStatus !== 'APPROVED' || data.shelfStatus !== 'ON') return '该商品当前不可购买'
  if (!data.stock || quantity.value > Number(data.stock)) return '商品库存不足，请调整购买数量'
  return ''
}

function validateForm() {
  fieldErrors.receiver = form.receiver ? '' : '请输入收货人'
  fieldErrors.receiverPhone = /^1[3-9]\d{9}$/.test(form.receiverPhone) ? '' : '请输入正确的中国大陆手机号'
  fieldErrors.receiverAddress = form.receiverAddress ? '' : '请输入收货地址'
  return !fieldErrors.receiver && !fieldErrors.receiverPhone && !fieldErrors.receiverAddress
}

async function load() {
  const productId = Number(route.query.productId)
  const queryQuantity = Number(route.query.quantity)
  if (!Number.isInteger(productId) || productId < 1 || !Number.isInteger(queryQuantity) || queryQuantity < 1) {
    errorMessage.value = '订单参数无效'
    loading.value = false
    return
  }
  quantity.value = queryQuantity
  try {
    const data = await productApi.detail(productId)
    const unavailable = validateProduct(data)
    if (unavailable) errorMessage.value = unavailable
    else product.value = data
  } catch (error) {
    errorMessage.value = error.message || '商品不存在或暂时不可购买'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (submitting.value || !validateForm()) return
  submitting.value = true
  try {
    const order = await orderApi.create({ productId: product.value.id, quantity: quantity.value, ...form })
    ElMessage.success('订单创建成功，请及时支付')
    router.push(`/orders/${order.id}`)
  } catch (error) {
    ElMessage.error(error.message || '订单创建失败')
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.checkout-heading { margin-top: 8px; }
.checkout-card { margin-top: 16px; padding: 24px; border: 1px solid var(--line); border-radius: 12px; background: #fff; }
.checkout-card h3 { margin: 0 0 18px; font-size: 18px; }
.checkout-product { display: grid; grid-template-columns: 96px minmax(180px, 1fr) 100px 140px; gap: 20px; align-items: center; }
.checkout-product img { width: 96px; height: 96px; border-radius: 8px; object-fit: cover; background: #f3f4f6; }
.checkout-product__name, .checkout-product__quantity, .checkout-product__subtotal { display: grid; gap: 8px; }
.checkout-product__name span, .checkout-product__quantity span, .checkout-product__subtotal span { color: #64748b; font-size: 14px; }
.stock-notice { margin-top: 20px; }
.checkout-submit { display: flex; align-items: center; justify-content: flex-end; gap: 24px; margin-top: 20px; padding: 18px 24px; border: 1px solid var(--line); border-radius: 12px; background: #fff; }
.checkout-submit > div { display: flex; align-items: center; gap: 12px; }
@media (max-width: 720px) { .checkout-product { grid-template-columns: 72px 1fr; gap: 14px; } .checkout-product img { width: 72px; height: 72px; } .checkout-product__quantity, .checkout-product__subtotal { grid-column: 2; } .checkout-submit { padding: 16px; gap: 16px; } }
</style>
