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

    <div class="table-card">
      <el-table :data="orders" empty-text="暂无订单">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">
            <span class="price small">￥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiverAddress" label="收货地址" min-width="240" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <div class="compact-actions">
              <el-button v-if="row.status === 'WAIT_PAY'" link type="primary" :icon="CreditCard" @click="pay(row)">支付</el-button>
              <el-button v-if="['WAIT_PAY','WAIT_SHIP'].includes(row.status)" link type="danger" :icon="Close" @click="cancel(row)">取消</el-button>
              <el-button v-if="row.status === 'WAIT_RECEIVE'" link type="primary" :icon="Check" @click="confirm(row)">确认收货</el-button>
              <el-button v-if="['WAIT_SHIP','WAIT_RECEIVE','COMPLETED'].includes(row.status)" link :icon="RefreshLeft" @click="openRefund(row)">申请售后</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close, CreditCard, Refresh, RefreshLeft } from '@element-plus/icons-vue'
import { orderApi, refundApi } from '../api'

const orders = ref([])
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
  orders.value = await orderApi.list()
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

<style scoped>
.small {
  font-size: 15px;
}
</style>
