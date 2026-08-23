<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>管理员后台</h2>
        <p class="muted">审核商品、处理平台介入售后、查看商家与操作日志</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <span>待审核商品</span>
        <strong>{{ pendingProducts.length }}</strong>
      </div>
      <div class="stat-card">
        <span>售后仲裁</span>
        <strong>{{ disputes.length }}</strong>
      </div>
      <div class="stat-card">
        <span>商家数量</span>
        <strong>{{ merchants.length }}</strong>
      </div>
      <div class="stat-card">
        <span>操作日志</span>
        <strong>{{ logs.length }}</strong>
      </div>
    </div>

    <div class="admin-layout">
      <aside class="admin-sidebar">
        <div class="sidebar-title">平台管理</div>
        <el-menu v-model:default-active="tab" class="admin-menu" @select="tab = $event">
          <el-menu-item index="products">
            <el-icon><Goods /></el-icon>
            <span>商品审核</span>
            <el-badge :value="pendingProducts.length" :hidden="!pendingProducts.length" />
          </el-menu-item>
          <el-menu-item index="refunds">
            <el-icon><Service /></el-icon>
            <span>售后仲裁</span>
            <el-badge :value="disputes.length" :hidden="!disputes.length" type="danger" />
          </el-menu-item>
          <el-menu-item index="merchants">
            <el-icon><Shop /></el-icon>
            <span>商家管理</span>
          </el-menu-item>
          <el-menu-item index="logs">
            <el-icon><Document /></el-icon>
            <span>操作日志</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <section class="admin-content">
        <div v-if="tab === 'products'" class="module-card">
          <div class="module-header">
            <div>
              <h3>商品审核</h3>
              <p class="muted">商家新发布或修改后的商品会进入待审核列表</p>
            </div>
          </div>
          <el-table :data="pendingProducts" empty-text="暂无待审核商品">
            <el-table-column label="商品" min-width="260">
              <template #default="{ row }">
                <div class="product-cell">
                  <img :src="row.mainImage || placeholder" alt="商品图" />
                  <div>
                    <strong>{{ row.name }}</strong>
                    <p class="muted">{{ row.subtitle }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="价格" width="110">
              <template #default="{ row }">￥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="90" />
            <el-table-column prop="createdAt" label="提交时间" width="170" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Check" @click="approveProduct(row)">通过</el-button>
                <el-button link type="danger" :icon="Close" @click="rejectProduct(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="tab === 'refunds'" class="module-card">
          <div class="module-header">
            <div>
              <h3>售后仲裁</h3>
              <p class="muted">用户申请平台介入后，客服管理员可进行最终裁决</p>
            </div>
          </div>
          <el-alert title="管理员裁决会直接改变售后和订单状态，操作时建议填写清楚处理依据。" type="warning" show-icon :closable="false" style="margin-bottom: 14px" />
          <el-table :data="disputes" empty-text="暂无平台介入售后">
            <el-table-column prop="id" label="售后单" width="90" />
            <el-table-column prop="orderId" label="订单" width="90" />
            <el-table-column prop="reason" label="用户原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="merchantReply" label="商家回复" min-width="200" show-overflow-tooltip />
            <el-table-column label="金额" width="110">
              <template #default="{ row }">￥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Check" @click="arbitrate(row, 'APPROVE')">同意退款</el-button>
                <el-button link type="danger" :icon="Close" @click="arbitrate(row, 'REJECT')">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="tab === 'merchants'" class="module-card">
          <div class="module-header">
            <div>
              <h3>商家管理</h3>
              <p class="muted">管理商家审核、冻结与驳回状态</p>
            </div>
          </div>
          <el-table :data="merchants" empty-text="暂无商家">
            <el-table-column prop="companyName" label="商家公司" min-width="180" />
            <el-table-column prop="contactName" label="联系人" width="120" />
            <el-table-column prop="contactPhone" label="联系电话" width="140" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="merchantType(row.status)">{{ merchantText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="rejectReason" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="setMerchant(row, 'APPROVED')">通过</el-button>
                <el-button link type="warning" @click="setMerchant(row, 'FROZEN')">冻结</el-button>
                <el-button link type="danger" @click="setMerchant(row, 'REJECTED')">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="tab === 'logs'" class="module-card">
          <div class="module-header">
            <div>
              <h3>操作日志</h3>
              <p class="muted">记录商品审核、订单发货、售后仲裁等关键后台行为</p>
            </div>
          </div>
          <el-table :data="logs" empty-text="暂无日志">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column label="角色" width="130">
              <template #default="{ row }">{{ roleText(row.operatorRole) }}</template>
            </el-table-column>
            <el-table-column prop="module" label="模块" width="100" />
            <el-table-column prop="action" label="动作" width="130" />
            <el-table-column prop="detail" label="详情" min-width="280" show-overflow-tooltip />
          </el-table>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Document, Goods, Refresh, Service, Shop } from '@element-plus/icons-vue'
import { adminApi } from '../api'

const tab = ref('products')
const pendingProducts = ref([])
const disputes = ref([])
const merchants = ref([])
const logs = ref([])
const placeholder = 'https://dummyimage.com/120x90/f3f4f6/6b7280&text=JD'

async function load() {
  const tasks = []
  tasks.push(adminApi.pendingProducts().then((data) => (pendingProducts.value = data)).catch(() => {}))
  tasks.push(adminApi.disputes().then((data) => (disputes.value = data)).catch(() => {}))
  tasks.push(adminApi.merchants().then((data) => (merchants.value = data)).catch(() => {}))
  tasks.push(adminApi.logs().then((data) => (logs.value = data)).catch(() => {}))
  await Promise.all(tasks)
}

function merchantText(status) {
  return { APPROVED: '正常', FROZEN: '冻结', REJECTED: '驳回', PENDING: '待审核' }[status] || status
}

function merchantType(status) {
  return { APPROVED: 'success', FROZEN: 'warning', REJECTED: 'danger', PENDING: 'info' }[status] || 'info'
}

function roleText(role) {
  return {
    USER: '用户',
    MERCHANT: '商家',
    SERVICE_ADMIN: '客服管理员',
    PRODUCT_ADMIN: '商品审核员',
    SUPER_ADMIN: '超级管理员',
    SYSTEM: '系统'
  }[role] || role
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

onMounted(load)
</script>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
}

.admin-sidebar {
  position: sticky;
  top: 84px;
  align-self: start;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.sidebar-title {
  padding: 16px 18px;
  font-weight: 700;
  border-bottom: 1px solid var(--line);
}

.admin-menu {
  border-right: none;
}

.admin-menu :deep(.el-menu-item) {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
}

.admin-menu :deep(.el-badge) {
  margin-left: auto;
}

.admin-content {
  min-width: 0;
}

.module-card {
  padding: 18px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.module-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.module-header h3 {
  margin: 0;
  font-size: 20px;
}

.module-header p {
  margin: 5px 0 0;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-cell img {
  width: 72px;
  height: 54px;
  object-fit: cover;
  border: 1px solid var(--line);
  border-radius: 6px;
}

.product-cell p {
  max-width: 420px;
  margin: 5px 0 0;
}

@media (max-width: 900px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: static;
  }
}
</style>
