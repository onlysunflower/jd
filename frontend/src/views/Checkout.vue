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
    <template v-else-if="checkoutItems.length">
      <section class="checkout-card">
        <div class="checkout-section-head">
          <h3>商品信息</h3>
          <span class="muted">{{ checkoutItems.length }} 种商品，共 {{ totalQuantity }} 件</span>
        </div>
        <div class="checkout-products">
          <article v-for="item in checkoutItems" :key="item.key" class="checkout-product">
            <img :src="item.productImage" :alt="item.productName" />
            <div class="checkout-product__name"><strong>{{ item.productName }}</strong><span>规格 {{ item.specName || '默认规格' }}</span><span>单价 <b class="price small"><em>¥</em>{{ money(item.price) }}</b></span><span>可售库存 {{ item.availableStock }}</span></div>
            <div class="checkout-product__quantity"><span>数量</span><b>{{ item.quantity }}</b></div>
            <div class="checkout-product__subtotal"><span>小计</span><strong class="price"><em>¥</em>{{ itemSubtotal(item) }}</strong></div>
          </article>
        </div>
        <el-alert class="stock-notice" type="info" show-icon :closable="false" title="提交订单后会锁定所选 SKU 库存，30 分钟未支付将自动关闭并释放库存与优惠券" />
        <el-alert v-if="merchantCount > 1" class="stock-notice" type="warning" show-icon :closable="false" :title="`所选商品来自 ${merchantCount} 个商家，将自动生成 ${merchantCount} 个订单；跨店订单暂不使用优惠券`" />
      </section>

      <section class="checkout-card">
        <h3>优惠券</h3>
        <el-select v-model="selectedCouponId" clearable placeholder="不使用优惠券" style="width:100%" :disabled="merchantCount > 1">
          <el-option v-for="coupon in coupons" :key="coupon.userCouponId" :value="coupon.userCouponId" :disabled="!couponUsable(coupon)" :label="`${coupon.name}（满 ¥${money(coupon.minAmount)} 减 ¥${money(coupon.discountAmount)}）`" />
        </el-select>
      </section>

      <section class="checkout-card">
        <h3>收货信息</h3>
        <el-form label-position="top" :disabled="submitting">
          <el-form-item label="收货人" :error="fieldErrors.receiver"><el-input v-model.trim="form.receiver" maxlength="64" placeholder="请输入收货人姓名" /></el-form-item>
          <el-form-item label="手机号" :error="fieldErrors.receiverPhone"><el-input v-model.trim="form.receiverPhone" maxlength="11" placeholder="请输入 11 位中国大陆手机号" /></el-form-item>
          <el-form-item label="收货地址" :error="fieldErrors.receiverAddress"><el-input v-model.trim="form.receiverAddress" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="请输入省、市、区及详细地址" /></el-form-item>
        </el-form>
      </section>

      <section class="checkout-submit"><div class="amount-breakdown"><span class="muted">商品金额 ¥{{ originalAmount }}</span><span v-if="discountAmount > 0" class="discount-copy">优惠 -¥{{ money(discountAmount) }}</span><strong class="price"><em>¥</em>{{ payableAmount }}</strong></div><el-button type="primary" size="large" :loading="submitting" :disabled="submitting" @click="submit">提交订单</el-button></section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { cartApi, couponApi, orderApi, productApi } from '../api'
import { productImage } from '../productVisuals'

const route = useRoute()
const router = useRouter()
const checkoutItems = ref([])
const coupons = ref([])
const selectedCouponId = ref(null)
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({ receiver: '', receiverPhone: '', receiverAddress: '' })
const fieldErrors = reactive({ receiver: '', receiverPhone: '', receiverAddress: '' })
const isCartSource = computed(() => route.query.source === 'cart')
const totalQuantity = computed(() => checkoutItems.value.reduce((total, item) => total + Number(item.quantity || 0), 0))
const merchantCount = computed(() => new Set(checkoutItems.value.map((item) => item.merchantId).filter(Boolean)).size || 1)
const originalAmountValue = computed(() => checkoutItems.value.reduce((total, item) => total + Number(item.price || 0) * Number(item.quantity || 0), 0))
const originalAmount = computed(() => originalAmountValue.value.toFixed(2))
const selectedCoupon = computed(() => coupons.value.find((item) => item.userCouponId === selectedCouponId.value))
const discountAmount = computed(() => couponUsable(selectedCoupon.value) ? Math.min(Number(selectedCoupon.value.discountAmount || 0), originalAmountValue.value) : 0)
const payableAmount = computed(() => Math.max(0, originalAmountValue.value - discountAmount.value).toFixed(2))

function money(value) { return Number(value || 0).toFixed(2) }
function itemSubtotal(item) { return (Number(item.price || 0) * Number(item.quantity || 0)).toFixed(2) }
function goBack() { router.push(isCartSource.value ? '/cart' : '/') }
function couponUsable(coupon) { return merchantCount.value === 1 && !!coupon && coupon.status === 'AVAILABLE' && originalAmountValue.value >= Number(coupon.minAmount || 0) }

function validateForm() {
  fieldErrors.receiver = form.receiver ? '' : '请输入收货人'
  fieldErrors.receiverPhone = /^1[3-9]\d{9}$/.test(form.receiverPhone) ? '' : '请输入正确的中国大陆手机号'
  fieldErrors.receiverAddress = form.receiverAddress ? '' : '请输入收货地址'
  return !fieldErrors.receiver && !fieldErrors.receiverPhone && !fieldErrors.receiverAddress
}

function cartItemIds() {
  const values = String(route.query.cartItemIds || '').split(',').filter(Boolean).map(Number)
  return [...new Set(values)].filter((id) => Number.isInteger(id) && id > 0)
}

async function loadCartItems() {
  const ids = cartItemIds()
  if (!ids.length) throw new Error('未选择要结算的购物车商品')
  const rows = await cartApi.list()
  const rowMap = new Map(rows.map((row) => [row.id, row]))
  const selected = ids.map((id) => rowMap.get(id)).filter(Boolean)
  if (selected.length !== ids.length) throw new Error('部分购物车商品已被删除，请重新选择')
  const unavailable = selected.find((row) => !row.purchasable)
  if (unavailable) throw new Error(`${unavailable.productName || '部分商品'}当前不可结算`)
  checkoutItems.value = selected.map((row) => ({
    key: `cart-${row.id}`,
    cartItemId: row.id,
    merchantId: row.merchantId,
    productName: row.productName,
    productImage: row.productImage || '/products/catalog-collection.png',
    specName: row.specName,
    price: row.price,
    quantity: row.quantity,
    availableStock: row.availableStock
  }))
}

async function loadDirectItem() {
  const productId = Number(route.query.productId)
  const quantity = Number(route.query.quantity)
  if (!Number.isInteger(productId) || productId < 1 || !Number.isInteger(quantity) || quantity < 1) throw new Error('订单参数无效')
  const data = await productApi.detail(productId)
  const skuId = Number(route.query.skuId)
  const sku = data.skus?.find((item) => item.id === skuId) || data.skus?.[0] || null
  const availableStock = sku ? Math.max(0, Number(sku.stock || 0) - Number(sku.lockedStock || 0)) : Number(data.stock || 0)
  if (data.auditStatus !== 'APPROVED' || data.shelfStatus !== 'ON') throw new Error('该商品当前不可购买')
  if (!availableStock || quantity > availableStock) throw new Error('所选规格库存不足，请调整购买数量')
  checkoutItems.value = [{
    key: `product-${data.id}-${sku?.id || 'default'}`,
    productId: data.id,
    skuId: sku?.id,
    merchantId: data.merchantId,
    productName: data.name,
    productImage: productImage(data),
    specName: sku?.specName || '默认规格',
    price: sku?.price ?? data.price,
    quantity,
    availableStock
  }]
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const couponPromise = couponApi.mine()
    if (isCartSource.value) await loadCartItems()
    else await loadDirectItem()
    coupons.value = await couponPromise
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
    if (isCartSource.value) {
      const orders = await orderApi.createFromCart({ cartItemIds: checkoutItems.value.map((item) => item.cartItemId), couponId: selectedCouponId.value, ...form })
      ElMessage.success(orders.length > 1 ? `下单成功，已按商家生成 ${orders.length} 个订单` : '订单创建成功，请及时支付')
      router.push(orders.length === 1 ? `/orders/${orders[0].id}` : '/orders')
    } else {
      const item = checkoutItems.value[0]
      const order = await orderApi.create({ productId: item.productId, skuId: item.skuId, couponId: selectedCouponId.value, quantity: item.quantity, ...form })
      ElMessage.success('订单创建成功，请及时支付')
      router.push(`/orders/${order.id}`)
    }
  } catch (error) {
    ElMessage.error(error.message || '订单创建失败')
    await load()
  } finally {
    submitting.value = false
  }
}

watch(merchantCount, (count) => { if (count > 1) selectedCouponId.value = null })
onMounted(load)
</script>

<style scoped>
.checkout-heading { margin-top: 8px; }
.checkout-card { margin-top: 16px; padding: 24px; border: 1px solid var(--line); border-radius: 12px; background: #fff; }
.checkout-card h3 { margin: 0 0 18px; font-size: 18px; }
.checkout-section-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.checkout-section-head h3 { margin-bottom: 0; }
.checkout-products { display: grid; margin-top: 18px; }
.checkout-product { display: grid; grid-template-columns: 96px minmax(180px, 1fr) 100px 140px; gap: 20px; align-items: center; padding: 16px 0; border-top: 1px solid var(--line); }
.checkout-product:first-child { border-top: 0; padding-top: 0; }
.checkout-product img { width: 96px; height: 96px; border-radius: 8px; object-fit: cover; background: #f3f4f6; }
.checkout-product__name, .checkout-product__quantity, .checkout-product__subtotal { display: grid; gap: 8px; }
.checkout-product__name span, .checkout-product__quantity span, .checkout-product__subtotal span { color: #64748b; font-size: 14px; }
.stock-notice { margin-top: 20px; }
.checkout-submit { display: flex; align-items: center; justify-content: flex-end; gap: 24px; margin-top: 20px; padding: 18px 24px; border: 1px solid var(--line); border-radius: 12px; background: #fff; }
.checkout-submit > div { display: flex; align-items: center; gap: 12px; }
.amount-breakdown { flex-wrap: wrap; justify-content: flex-end; }
.discount-copy { color: #16a34a; font-size: 14px; }
@media (max-width: 720px) { .checkout-product { grid-template-columns: 72px 1fr; gap: 14px; } .checkout-product img { width: 72px; height: 72px; } .checkout-product__quantity, .checkout-product__subtotal { grid-column: 2; } .checkout-submit { padding: 16px; gap: 16px; } }
</style>
