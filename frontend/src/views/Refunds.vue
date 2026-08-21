<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>我的售后</h2>
        <p class="muted">商家拒绝或超时未处理时，可以申请平台介入</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="refunds" class="panel">
      <el-table-column prop="id" label="售后单" width="90" />
      <el-table-column prop="orderId" label="订单ID" width="90" />
      <el-table-column prop="type" label="类型" width="150" />
      <el-table-column prop="reason" label="原因" min-width="200" />
      <el-table-column prop="amount" label="金额" width="100" />
      <el-table-column label="状态" width="170">
        <template #default="{ row }">
          <el-tag>{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button v-if="row.status === 'WAIT_USER_RETURN'" link type="primary" @click="submitReturn(row)">填写退货物流</el-button>
          <el-button v-if="['MERCHANT_REJECTED','MERCHANT_REVIEWING'].includes(row.status)" link type="danger" @click="intervention(row)">平台介入</el-button>
          <el-button link @click="showLogs(row)">进度</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="logsVisible" title="售后进度" width="560px">
      <el-timeline>
        <el-timeline-item v-for="log in logs" :key="log.id" :timestamp="log.createdAt">
          <strong>{{ log.operatorRole }} - {{ log.action }}</strong>
          <p>{{ log.remark }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { refundApi } from '../api'

const refunds = ref([])
const logs = ref([])
const logsVisible = ref(false)

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
