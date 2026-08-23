<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>我的售后</h2>
        <p class="muted">商家拒绝或超时未处理时，可以申请平台介入</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <span>售后单</span>
        <strong>{{ refunds.length }}</strong>
      </div>
      <div class="stat-card">
        <span>等待商家</span>
        <strong>{{ countBy(['MERCHANT_REVIEWING']) }}</strong>
      </div>
      <div class="stat-card">
        <span>平台介入</span>
        <strong>{{ countBy(['PLATFORM_INTERVENING']) }}</strong>
      </div>
      <div class="stat-card">
        <span>已完成</span>
        <strong>{{ countBy(['REFUND_SUCCESS', 'REFUND_FAILED']) }}</strong>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="refunds" empty-text="暂无售后单">
        <el-table-column prop="id" label="售后单" width="90" />
        <el-table-column prop="orderId" label="订单ID" width="90" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">￥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="170">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="compact-actions">
              <el-button v-if="row.status === 'WAIT_USER_RETURN'" link type="primary" :icon="Van" @click="submitReturn(row)">填写退货物流</el-button>
              <el-button v-if="['MERCHANT_REJECTED','MERCHANT_REVIEWING'].includes(row.status)" link type="danger" :icon="Warning" @click="intervention(row)">平台介入</el-button>
              <el-button link :icon="Tickets" @click="showLogs(row)">进度</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="logsVisible" title="售后进度" width="600px">
      <el-timeline>
        <el-timeline-item v-for="log in logs" :key="log.id" :timestamp="log.createdAt">
          <strong>{{ roleText(log.operatorRole) }} - {{ actionText(log.action) }}</strong>
          <p>{{ log.remark }}</p>
        </el-timeline-item>
      </el-timeline>
      <div v-if="!logs.length" class="empty-hint">暂无流转记录</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Tickets, Van, Warning } from '@element-plus/icons-vue'
import { refundApi } from '../api'

const refunds = ref([])
const logs = ref([])
const logsVisible = ref(false)

function countBy(statuses) {
  return refunds.value.filter((item) => statuses.includes(item.status)).length
}

function typeText(type) {
  return type === 'REFUND_ONLY' ? '仅退款' : '退货退款'
}

function statusText(status) {
  const labels = {
    MERCHANT_REVIEWING: '商家审核中',
    MERCHANT_APPROVED: '商家已同意',
    MERCHANT_REJECTED: '商家已拒绝',
    WAIT_USER_RETURN: '等待用户退货',
    WAIT_MERCHANT_RECEIVE: '等待商家收货',
    PLATFORM_INTERVENING: '平台介入中',
    REFUND_SUCCESS: '退款成功',
    REFUND_FAILED: '退款失败',
    CLOSED: '售后关闭'
  }
  return labels[status] || status
}

function statusType(status) {
  const map = {
    MERCHANT_REVIEWING: 'warning',
    WAIT_USER_RETURN: 'primary',
    WAIT_MERCHANT_RECEIVE: 'primary',
    MERCHANT_REJECTED: 'danger',
    PLATFORM_INTERVENING: 'danger',
    REFUND_SUCCESS: 'success',
    REFUND_FAILED: 'info',
    CLOSED: 'info'
  }
  return map[status] || 'info'
}

function roleText(role) {
  const labels = {
    USER: '用户',
    MERCHANT: '商家',
    SERVICE_ADMIN: '客服管理员',
    PRODUCT_ADMIN: '商品审核员',
    SUPER_ADMIN: '超级管理员'
  }
  return labels[role] || role
}

function actionText(action) {
  const labels = {
    CREATE: '提交申请',
    MERCHANT_APPROVE: '商家同意',
    MERCHANT_REJECT: '商家拒绝',
    USER_RETURN: '用户退货',
    USER_REQUEST_INTERVENTION: '申请介入',
    ADMIN_ARBITRATE: '平台仲裁',
    MERCHANT_CONFIRM_RETURN: '确认退款'
  }
  return labels[action] || action
}

async function load() {
  refunds.value = await refundApi.list()
}

async function submitReturn(row) {
  const { value } = await ElMessageBox.prompt('请输入退货物流单号', '填写退货物流')
  await refundApi.submitReturn(row.id, { returnLogisticsNo: value })
  ElMessage.success('已提交退货物流')
  load()
}

async function intervention(row) {
  await refundApi.intervention(row.id)
  ElMessage.success('已申请平台介入')
  load()
}

async function showLogs(row) {
  logs.value = await refundApi.logs(row.id)
  logsVisible.value = true
}

onMounted(load)
</script>
