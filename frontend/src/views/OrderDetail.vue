<template>
  <div class="page order-detail-page">
    <el-button text :icon="ArrowLeft" @click="router.push('/orders')">返回我的订单</el-button>
    <div v-if="loading" class="detail-card"><el-skeleton animated :rows="10" /></div>
    <div v-else-if="errorMessage" class="empty-state">
      <el-empty :description="errorMessage" :image-size="112" />
      <el-button type="primary" @click="router.push('/orders')">返回我的订单</el-button>
    </div>
    <template v-else-if="order">
      <section class="detail-card">
        <div class="order-detail-head">
          <div>
            <span class="muted">订单号</span>
            <h2>{{ order.orderNo }}</h2>
          </div>
          <el-tag :type="statusType(order.status)" size="large">{{ statusText(order.status) }}</el-tag>
        </div>
        <div class="order-detail-grid">
          <span>下单时间 <b>{{ order.createdAt }}</b></span>
          <span v-if="order.paidAt">支付时间 <b>{{ order.paidAt }}</b></span>
          <span v-if="order.shippedAt">发货时间 <b>{{ order.shippedAt }}</b></span>
          <span v-if="order.completedAt">完成时间 <b>{{ order.completedAt }}</b></span>
        </div>
      </section>

      <section class="detail-card">
        <h3>商品信息</h3>
        <div v-if="itemsLoading" class="skeleton-card"><el-skeleton animated :rows="4" /></div>
        <div v-else class="order-items">
          <article v-for="item in items" :key="item.id" class="order-item">
            <img :src="item.productImage || '/products/catalog-collection.png'" :alt="item.productName" />
            <div>
              <strong>{{ item.productName }}</strong>
              <span>成交单价 ¥{{ money(item.price) }}</span>
            </div>
            <span>x {{ item.quantity }}</span>
            <strong class="price small"><em>¥</em>{{ subtotal(item) }}</strong>
          </article>
        </div>
        <div class="detail-total">订单总金额 <strong class="price"><em>¥</em>{{ money(order.totalAmount) }}</strong></div>
      </section>

      <section class="detail-card">
        <h3>收货与物流</h3>
        <div class="order-detail-grid">
          <span>收货人 <b>{{ order.receiver }}</b></span>
          <span>手机号 <b>{{ order.receiverPhone }}</b></span>
          <span class="wide">收货地址 <b>{{ order.receiverAddress }}</b></span>
          <span v-if="order.logisticsNo" class="wide">
            物流信息 <b>{{ order.logisticsCompany || '物流公司待补充' }} · {{ order.logisticsNo }}</b>
          </span>
          <span v-else class="wide">物流信息 <b>待商家发货</b></span>
        </div>
      </section>

      <section class="detail-actions">
        <el-button
          v-if="order.status === 'WAIT_PAY'"
          type="primary"
          :loading="processing === 'pay'"
          :disabled="!!processing"
          @click="operate('pay')"
        >
          支付
        </el-button>
        <el-button
          v-if="order.status === 'WAIT_PAY'"
          type="danger"
          plain
          :loading="processing === 'cancel'"
          :disabled="!!processing"
          @click="operate('cancel')"
        >
          取消订单
        </el-button>
        <el-button
          v-if="order.status === 'WAIT_RECEIVE'"
          type="primary"
          :loading="processing === 'confirm'"
          :disabled="!!processing"
          @click="operate('confirm')"
        >
          确认收货
        </el-button>
        <el-button
          v-if="canApplyRefund(order.status)"
          type="warning"
          plain
          :disabled="!!processing || refundSubmitting"
          @click="openRefundDialog"
        >
          申请售后
        </el-button>
      </section>
    </template>

    <el-dialog v-model="refundVisible" title="申请售后" width="560px">
      <template v-if="order">
        <div class="refund-hint">
          当前订单状态为 <b>{{ statusText(order.status) }}</b>，可申请：
          <b>{{ refundTypeLabels.join('、') }}</b>
        </div>
        <el-form :model="refundForm" label-width="90px">
          <el-form-item label="售后类型">
            <el-select v-model="refundForm.type" style="width: 100%">
              <el-option
                v-for="type in refundTypeOptions"
                :key="type"
                :label="refundTypeText(type)"
                :value="type"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="申请原因">
            <el-input
              v-model="refundForm.reason"
              type="textarea"
              :rows="4"
              maxlength="255"
              show-word-limit
              placeholder="请填写售后原因"
            />
          </el-form-item>
          <el-form-item label="凭证图片">
            <el-input
              v-model="refundForm.evidenceImages"
              placeholder="可选，多个图片地址可用逗号分隔"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { orderApi, refundApi } from '../api'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const items = ref([])
const loading = ref(true)
const itemsLoading = ref(false)
const errorMessage = ref('')
const processing = ref('')
const refundVisible = ref(false)
const refundSubmitting = ref(false)
const refundForm = reactive({
  type: 'REFUND_ONLY',
  reason: '',
  evidenceImages: ''
})

const labels = {
  WAIT_PAY: '待付款',
  WAIT_SHIP: '待发货',
  WAIT_RECEIVE: '待收货',
  COMPLETED: '已完成',
  CANCELED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款'
}

const refundTypeOptions = computed(() => {
  switch (order.value?.status) {
    case 'WAIT_SHIP':
      return ['REFUND_ONLY']
    case 'WAIT_RECEIVE':
      return ['REFUND_ONLY', 'RETURN_AND_REFUND']
    case 'COMPLETED':
      return ['RETURN_AND_REFUND']
    default:
      return []
  }
})

const refundTypeLabels = computed(() => refundTypeOptions.value.map((type) => refundTypeText(type)))

function money(value) {
  return Number(value || 0).toFixed(2)
}

function subtotal(item) {
  return (Number(item.price || 0) * Number(item.quantity || 0)).toFixed(2)
}

function statusText(status) {
  return labels[status] || status
}

function statusType(status) {
  return ({
    WAIT_PAY: 'warning',
    WAIT_SHIP: 'primary',
    WAIT_RECEIVE: 'success',
    COMPLETED: 'success',
    CANCELED: 'info',
    REFUNDING: 'danger',
    REFUNDED: 'success'
  })[status] || 'info'
}

function refundTypeText(type) {
  return type === 'REFUND_ONLY' ? '仅退款' : '退货退款'
}

function canApplyRefund(status) {
  return ['WAIT_SHIP', 'WAIT_RECEIVE', 'COMPLETED'].includes(status)
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    order.value = await orderApi.detail(route.params.id)
    itemsLoading.value = true
    try {
      items.value = await orderApi.items(route.params.id)
    } catch (error) {
      errorMessage.value = error.message || '订单商品明细加载失败'
    } finally {
      itemsLoading.value = false
    }
  } catch (error) {
    errorMessage.value = error.message || '订单不存在或无权查看'
  } finally {
    loading.value = false
  }
}

async function operate(action) {
  const config = {
    pay: ['确认支付', '确认支付该订单吗？', orderApi.pay, '模拟支付成功'],
    cancel: ['取消订单', '确认取消该订单吗？', orderApi.cancel, '订单已取消'],
    confirm: ['确认收货', '确认已收到商品吗？', orderApi.confirm, '已确认收货']
  }[action]
  try {
    await ElMessageBox.confirm(config[1], config[0], { type: 'warning' })
  } catch {
    return
  }

  processing.value = action
  try {
    await config[2](order.value.id)
    ElMessage.success(config[3])
    await load()
  } catch (error) {
    ElMessage.error(error.message || '订单操作失败')
    await load()
  } finally {
    processing.value = ''
  }
}

function openRefundDialog() {
  if (!refundTypeOptions.value.length) {
    return
  }
  refundForm.type = refundTypeOptions.value[0]
  refundForm.reason = ''
  refundForm.evidenceImages = ''
  refundVisible.value = true
}

async function submitRefund() {
  if (!refundTypeOptions.value.includes(refundForm.type)) {
    ElMessage.error('当前订单状态不支持该售后类型')
    return
  }
  if (!refundForm.reason.trim()) {
    ElMessage.error('请填写售后原因')
    return
  }

  refundSubmitting.value = true
  try {
    await refundApi.create({
      orderId: order.value.id,
      type: refundForm.type,
      reason: refundForm.reason.trim(),
      evidenceImages: refundForm.evidenceImages.trim()
    })
    ElMessage.success('售后申请已提交')
    refundVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message || '售后申请提交失败')
  } finally {
    refundSubmitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-detail-head { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.order-detail-head h2 { margin: 6px 0 0; font-size: 20px; }
.order-detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 24px; margin-top: 20px; color: #64748b; font-size: 14px; }
.order-detail-grid b { margin-left: 8px; color: #1f2937; font-weight: 500; }
.wide { grid-column: 1 / -1; }
.order-items { display: grid; gap: 12px; }
.order-item { display: grid; grid-template-columns: 76px minmax(160px, 1fr) 70px 120px; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.order-item img { width: 76px; height: 76px; object-fit: cover; border-radius: 8px; background: #f3f4f6; }
.order-item div { display: grid; gap: 8px; }
.order-item div span { color: #64748b; font-size: 14px; }
.detail-total { display: flex; justify-content: flex-end; align-items: center; gap: 12px; margin-top: 20px; }
.detail-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; flex-wrap: wrap; }
.refund-hint { margin-bottom: 16px; padding: 12px 14px; border-radius: 8px; background: #fff7ed; color: #9a3412; line-height: 1.6; }
@media (max-width: 640px) {
  .order-detail-grid { grid-template-columns: 1fr; }
  .order-item { grid-template-columns: 64px 1fr; }
  .order-item img { width: 64px; height: 64px; }
  .order-item > span, .order-item > strong { grid-column: 2; }
  .detail-actions { justify-content: stretch; }
  .detail-actions .el-button { flex: 1; }
}
</style>
