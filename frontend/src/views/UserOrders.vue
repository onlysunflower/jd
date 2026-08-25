<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>我的订单</h2>
        <p class="muted">演示下单、支付、取消、确认收货和申请售后</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <span>全部订单</span>
        <strong>{{ orders.length }}</strong>
      </div>
      <div class="stat-card">
        <span>待处理</span>
        <strong>{{ countBy(['WAIT_PAY', 'WAIT_SHIP', 'WAIT_RECEIVE']) }}</strong>
      </div>
      <div class="stat-card">
        <span>退款中</span>
        <strong>{{ countBy(['REFUNDING']) }}</strong>
      </div>
      <div class="stat-card">
        <span>已完成</span>
        <strong>{{ countBy(['COMPLETED', 'REFUNDED']) }}</strong>
      </div>
    </div>

    <el-tabs v-model="activeStatus" class="order-tabs">
      <el-tab-pane v-for="tab in orderTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <div v-if="loading" class="order-list">
      <div v-for="item in 3" :key="item" class="skeleton-card"><el-skeleton animated :rows="3" /></div>
    </div>
    <div v-else-if="filteredOrders.length" class="order-list">
      <article v-for="row in filteredOrders" :key="row.id" class="order-card">
        <header class="order-card__head">
          <div class="order-reference"><span>订单号 <b>{{ row.orderNo }}</b></span><span>下单时间 {{ row.createdAt }}</span></div>
          <el-tag :type="statusType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
        </header>
        <div class="order-card__body">
          <div class="order-summary">
            <div class="order-product-placeholder"><el-icon><Goods /></el-icon></div>
            <div class="order-summary__text"><strong>订单商品</strong><span>订单商品信息将在订单详情中展示</span><div class="order-summary__address">收货地址：{{ row.receiverAddress }}</div></div>
          </div>
          <div class="order-finance"><small>订单实付</small><span class="price small"><em>¥</em>{{ row.totalAmount }}</span><div class="compact-actions">
            <el-button v-if="row.status === 'WAIT_PAY'" type="primary" size="small" :icon="CreditCard" @click="pay(row)">支付</el-button>
            <el-button v-if="['WAIT_PAY','WAIT_SHIP'].includes(row.status)" size="small" :type="row.status === 'WAIT_PAY' ? 'danger' : undefined" :icon="Close" @click="cancel(row)">取消</el-button>
            <el-button v-if="row.status === 'WAIT_RECEIVE'" type="primary" size="small" :icon="Check" @click="confirm(row)">确认收货</el-button>
            <el-button v-if="['WAIT_SHIP','WAIT_RECEIVE','COMPLETED'].includes(row.status)" size="small" :icon="RefreshLeft" @click="openRefund(row)">申请售后</el-button>
          </div></div>
        </div>
      </article>
    </div>
    <div v-else class="empty-state"><el-empty description="暂无相关订单" :image-size="112" /><p class="empty-state__copy">下单后的商品会在这里展示订单进度</p></div>

    <el-dialog v-model="refundVisible" title="申请退款 / 退货退款" width="520px">
      <el-alert type="info" show-icon :closable="false" title="申请后订单会进入退款中，商家可同意、拒绝或由平台介入处理。" />
      <el-form :model="refundForm" label-width="90px" style="margin-top: 18px">
        <el-form-item label="类型">
          <el-select v-model="refundForm.type" style="width: 100%">
            <el-option label="仅退款" value="REFUND_ONLY" />
            <el-option label="退货退款" value="RETURN_AND_REFUND" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="refundForm.reason" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close, CreditCard, Goods, Refresh, RefreshLeft } from '@element-plus/icons-vue'
import { orderApi, refundApi } from '../api'

const orders = ref([])
const loading = ref(false)
const activeStatus = ref('ALL')
const refundVisible = ref(false)
const currentOrder = ref(null)
const refundForm = reactive({ type: 'RETURN_AND_REFUND', reason: '商品与描述不符，申请售后处理' })

const labels = {
  WAIT_PAY: '待付款',
  WAIT_SHIP: '待发货',
  WAIT_RECEIVE: '待收货',
  COMPLETED: '已完成',
  CANCELED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款'
}

const orderTabs = [
  { label: '全部', value: 'ALL' }, { label: '待付款', value: 'WAIT_PAY' }, { label: '待发货', value: 'WAIT_SHIP' },
  { label: '待收货', value: 'WAIT_RECEIVE' }, { label: '已完成', value: 'COMPLETED' }, { label: '已取消', value: 'CANCELED' }
]
const filteredOrders = computed(() => activeStatus.value === 'ALL' ? orders.value : orders.value.filter((item) => item.status === activeStatus.value))

function statusText(status) {
  return labels[status] || status
}

function statusType(status) {
  const map = {
    WAIT_PAY: 'warning',
    WAIT_SHIP: 'primary',
    WAIT_RECEIVE: 'success',
    COMPLETED: 'success',
    CANCELED: 'info',
    REFUNDING: 'danger',
    REFUNDED: 'success'
  }
  return map[status] || 'info'
}

function countBy(statuses) {
  return orders.value.filter((item) => statuses.includes(item.status)).length
}

async function load() {
  loading.value = true
  try { orders.value = await orderApi.list() } finally { loading.value = false }
}

async function pay(row) {
  await orderApi.pay(row.id)
  ElMessage.success('模拟支付成功')
  load()
}

async function cancel(row) {
  await orderApi.cancel(row.id)
  ElMessage.success('订单已取消')
  load()
}

async function confirm(row) {
  await orderApi.confirm(row.id)
  ElMessage.success('已确认收货')
  load()
}

function openRefund(row) {
  currentOrder.value = row
  refundVisible.value = true
}

async function submitRefund() {
  await refundApi.create({ orderId: currentOrder.value.id, ...refundForm })
  ElMessage.success('售后申请已提交')
  refundVisible.value = false
  load()
}

onMounted(load)
</script>
