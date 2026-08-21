<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>我的订单</h2>
        <p class="muted">演示下单、支付、取消、确认收货和申请售后</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="orders" class="panel">
      <el-table-column prop="orderNo" label="订单号" min-width="170" />
      <el-table-column prop="totalAmount" label="金额" width="100" />
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag>{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="receiverAddress" label="收货地址" min-width="220" />
      <el-table-column label="操作" width="360">
        <template #default="{ row }">
          <el-button v-if="row.status === 'WAIT_PAY'" link type="primary" @click="pay(row)">支付</el-button>
          <el-button v-if="['WAIT_PAY','WAIT_SHIP'].includes(row.status)" link type="danger" @click="cancel(row)">取消</el-button>
          <el-button v-if="row.status === 'WAIT_RECEIVE'" link type="primary" @click="confirm(row)">确认收货</el-button>
          <el-button v-if="['WAIT_SHIP','WAIT_RECEIVE','COMPLETED'].includes(row.status)" link @click="openRefund(row)">申请售后</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="refundVisible" title="申请退款 / 退货退款" width="520px">
      <el-form :model="refundForm" label-width="90px">
        <el-form-item label="类型">
          <el-select v-model="refundForm.type">
            <el-option label="仅退款" value="REFUND_ONLY" />
            <el-option label="退货退款" value="RETURN_AND_REFUND" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="refundForm.reason" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRefund">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
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
