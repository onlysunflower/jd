<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>管理员后台</h2>
        <p class="muted">审核商品、处理平台介入售后、查看用户与操作日志</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-tabs v-model="tab" class="panel">
      <el-tab-pane label="商品审核" name="products">
        <el-table :data="pendingProducts">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="商品" min-width="180" />
          <el-table-column prop="price" label="价格" width="100" />
          <el-table-column prop="stock" label="库存" width="90" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button link type="primary" @click="approveProduct(row)">通过</el-button>
              <el-button link type="danger" @click="rejectProduct(row)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="售后仲裁" name="refunds">
        <el-table :data="disputes">
          <el-table-column prop="id" label="售后单" width="90" />
          <el-table-column prop="orderId" label="订单" width="90" />
          <el-table-column prop="reason" label="用户原因" min-width="180" />
          <el-table-column prop="merchantReply" label="商家回复" min-width="180" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button link type="primary" @click="arbitrate(row, 'APPROVE')">同意退款</el-button>
              <el-button link type="danger" @click="arbitrate(row, 'REJECT')">驳回退款</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="商家管理" name="merchants">
        <el-table :data="merchants">
          <el-table-column prop="companyName" label="商家公司" min-width="180" />
          <el-table-column prop="contactName" label="联系人" width="120" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button link type="primary" @click="setMerchant(row, 'APPROVED')">通过</el-button>
              <el-button link type="warning" @click="setMerchant(row, 'FROZEN')">冻结</el-button>
              <el-button link type="danger" @click="setMerchant(row, 'REJECTED')">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <el-table :data="logs">
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="operatorRole" label="角色" width="130" />
          <el-table-column prop="module" label="模块" width="100" />
          <el-table-column prop="action" label="动作" width="130" />
          <el-table-column prop="detail" label="详情" min-width="260" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api'

const tab = ref('products')
const pendingProducts = ref([])
const disputes = ref([])
const merchants = ref([])
const logs = ref([])

async function load() {
  const tasks = []
  tasks.push(adminApi.pendingProducts().then((data) => (pendingProducts.value = data)).catch(() => {}))
  tasks.push(adminApi.disputes().then((data) => (disputes.value = data)).catch(() => {}))
  tasks.push(adminApi.merchants().then((data) => (merchants.value = data)).catch(() => {}))
  tasks.push(adminApi.logs().then((data) => (logs.value = data)).catch(() => {}))
  await Promise.all(tasks)
}

async function approveProduct(row) {
  await adminApi.approveProduct(row.id)
  ElMessage.success('商品审核通过')
  load()
}

async function rejectProduct(row) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回商品')
  await adminApi.rejectProduct(row.id, { remark: value })
  ElMessage.success('商品已驳回')
  load()
}

async function arbitrate(row, decision) {
  const { value } = await ElMessageBox.prompt('请输入仲裁说明', '售后仲裁')
  await adminApi.arbitrate(row.id, { decision, remark: value })
  ElMessage.success('仲裁完成')
  load()
}

async function setMerchant(row, status) {
  await adminApi.updateMerchantStatus(row.id, status, '管理员后台操作')
  ElMessage.success('商家状态已更新')
  load()
}

watch(tab, load)
onMounted(load)
</script>
