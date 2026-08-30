<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>管理员后台</h2>
        <p class="muted">统一处理商家资质、商品治理、售后仲裁与平台结算</p>
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
        <span>入驻 / 提现待办</span>
        <strong>{{ applications.length + withdrawals.length }}</strong>
      </div>
    </div>

    <div class="admin-layout">
      <aside class="admin-sidebar">
        <div class="sidebar-title">平台管理</div>
        <el-menu v-model:default-active="tab" class="admin-menu" @select="tab = $event">
          <el-menu-item index="applications">
            <el-icon><UserFilled /></el-icon><span>入驻审核</span>
            <el-badge :value="applications.length" :hidden="!applications.length" />
          </el-menu-item>
          <el-menu-item index="products">
            <el-icon><Goods /></el-icon>
            <span>商品治理</span>
            <el-badge :value="pendingProducts.length" :hidden="!pendingProducts.length" />
          </el-menu-item>
          <el-menu-item index="reviews">
            <el-icon><ChatDotRound /></el-icon><span>评价治理</span>
          </el-menu-item>
          <el-menu-item index="finance">
            <el-icon><Wallet /></el-icon><span>结算提现</span>
            <el-badge :value="withdrawals.length" :hidden="!withdrawals.length" type="warning" />
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
        <div v-if="tab === 'applications'" class="module-card">
          <div class="module-header"><div><h3>商家入驻审核</h3><p class="muted">核验营业执照和经营信息，审核通过时配置平台佣金扣点</p></div></div>
          <el-table :data="applications" empty-text="暂无待处理入驻申请">
            <el-table-column prop="companyName" label="企业名称" min-width="180" />
            <el-table-column prop="licenseNo" label="信用代码" min-width="180" />
            <el-table-column prop="contactName" label="联系人" width="100" />
            <el-table-column prop="contactPhone" label="联系电话" width="135" />
            <el-table-column label="营业执照" width="100"><template #default="{ row }"><el-link :href="row.licenseImage" target="_blank" type="primary">查看资料</el-link></template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="merchantType(row.status)">{{ merchantText(row.status) }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="180" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="Check" @click="reviewApplication(row, 'APPROVE')">通过</el-button><el-button link type="danger" :icon="Close" @click="reviewApplication(row, 'REJECT')">驳回</el-button></template></el-table-column>
          </el-table>
        </div>

        <div v-if="tab === 'products'" class="module-card">
          <div class="module-header">
            <div>
              <h3>商品审核与治理</h3>
              <p class="muted">审核待上架商品，并可对违规在售商品执行强制下架</p>
            </div>
          </div>
          <el-table :data="allProducts" empty-text="暂无商品">
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
            <el-table-column label="状态" width="150"><template #default="{ row }"><el-tag :type="row.auditStatus === 'REJECTED' ? 'danger' : row.auditStatus === 'PENDING' ? 'warning' : 'success'">{{ row.auditStatus === 'PENDING' ? '待审核' : row.auditStatus === 'REJECTED' ? '已驳回' : (row.shelfStatus === 'ON' ? '在售' : '已下架') }}</el-tag></template></el-table-column>
            <el-table-column prop="createdAt" label="提交时间" width="170" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.auditStatus === 'PENDING'" link type="primary" :icon="Check" @click="approveProduct(row)">通过</el-button>
                <el-button v-if="row.auditStatus === 'PENDING'" link type="danger" :icon="Close" @click="rejectProduct(row)">驳回</el-button>
                <el-button v-if="row.shelfStatus === 'ON'" link type="danger" :icon="Bottom" @click="forceOffShelf(row)">强制下架</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="tab === 'reviews'" class="module-card">
          <div class="module-header"><div><h3>评价治理</h3><p class="muted">监控用户评价与商家回复，屏蔽违规内容或恢复展示</p></div></div>
          <el-table :data="reviews" empty-text="暂无评价">
            <el-table-column prop="productId" label="商品" width="80" />
            <el-table-column label="评分" width="145"><template #default="{ row }"><el-rate :model-value="row.rating" disabled /></template></el-table-column>
            <el-table-column prop="content" label="评价内容" min-width="240" show-overflow-tooltip />
            <el-table-column prop="appendContent" label="追评" min-width="170" show-overflow-tooltip />
            <el-table-column prop="reply" label="商家回复" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'HIDDEN' ? 'danger' : 'success'">{{ row.status === 'HIDDEN' ? '已屏蔽' : '展示中' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="110" fixed="right"><template #default="{ row }"><el-button link :type="row.status === 'HIDDEN' ? 'primary' : 'danger'" @click="moderateReview(row)">{{ row.status === 'HIDDEN' ? '恢复' : '屏蔽' }}</el-button></template></el-table-column>
          </el-table>
        </div>

        <div v-if="tab === 'finance'" class="module-card">
          <div class="module-header"><div><h3>结算与提现</h3><p class="muted">核对平台佣金结算单，审核商家提现并生成打款流水</p></div></div>
          <el-tabs>
            <el-tab-pane label="提现待办">
              <el-table :data="withdrawals" empty-text="暂无待审核提现">
                <el-table-column prop="withdrawNo" label="提现单号" min-width="190" /><el-table-column prop="merchantId" label="商家" width="80" />
                <el-table-column label="金额" width="120"><template #default="{ row }">¥{{ row.amount }}</template></el-table-column><el-table-column prop="accountInfo" label="收款账户" min-width="220" /><el-table-column prop="createdAt" label="申请时间" width="180" />
                <el-table-column label="操作" width="170" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="reviewWithdrawal(row, 'APPROVE')">审核打款</el-button><el-button link type="danger" @click="reviewWithdrawal(row, 'REJECT')">驳回</el-button></template></el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="结算账单">
              <el-table :data="settlements" empty-text="暂无结算账单"><el-table-column prop="orderId" label="订单" width="80" /><el-table-column prop="merchantId" label="商家" width="80" /><el-table-column prop="grossAmount" label="实付金额" width="110" /><el-table-column prop="commissionAmount" label="平台佣金" width="110" /><el-table-column prop="settlementAmount" label="商家收入" width="110" /><el-table-column prop="status" label="状态" width="100" /><el-table-column prop="availableAt" label="解冻时间" min-width="180" /></el-table>
            </el-tab-pane>
          </el-tabs>
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
import { Bottom, ChatDotRound, Check, Close, Document, Goods, Refresh, Service, Shop, UserFilled, Wallet } from '@element-plus/icons-vue'
import { adminApi } from '../api'

const tab = ref('products')
const pendingProducts = ref([])
const allProducts = ref([])
const disputes = ref([])
const merchants = ref([])
const applications = ref([])
const reviews = ref([])
const settlements = ref([])
const withdrawals = ref([])
const logs = ref([])
const placeholder = 'https://dummyimage.com/120x90/f3f4f6/6b7280&text=JD'

async function load() {
  const tasks = []
  tasks.push(adminApi.pendingProducts().then((data) => (pendingProducts.value = data)).catch(() => {}))
  tasks.push(adminApi.products().then((data) => (allProducts.value = data)).catch(() => {}))
  tasks.push(adminApi.disputes().then((data) => (disputes.value = data)).catch(() => {}))
  tasks.push(adminApi.merchants().then((data) => (merchants.value = data)).catch(() => {}))
  tasks.push(adminApi.merchantApplications().then((data) => (applications.value = data)).catch(() => {}))
  tasks.push(adminApi.reviews().then((data) => (reviews.value = data)).catch(() => {}))
  tasks.push(adminApi.settlements().then((data) => (settlements.value = data)).catch(() => {}))
  tasks.push(adminApi.withdrawals().then((data) => (withdrawals.value = data)).catch(() => {}))
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

async function forceOffShelf(row) {
  const { value } = await ElMessageBox.prompt('请输入违规下架原因', '强制下架商品', { inputPattern: /\S+/, inputErrorMessage: '请填写处理原因' })
  await adminApi.forceOffShelf(row.id, { remark: value })
  ElMessage.success(`商品已强制下架：${value}`)
  load()
}

async function reviewApplication(row, decision) {
  let remark = '资质审核通过'
  let commissionRate = 5
  if (decision === 'APPROVE') {
    const result = await ElMessageBox.prompt('请输入平台佣金扣点（0-100）', '通过入驻审核', { inputValue: '5', inputPattern: /^(100(?:\.0+)?|\d{1,2}(?:\.\d{1,2})?)$/, inputErrorMessage: '请输入 0-100 的数字' })
    commissionRate = Number(result.value)
  } else {
    const result = await ElMessageBox.prompt('请输入驳回原因', '驳回入驻申请', { inputPattern: /\S+/, inputErrorMessage: '请填写驳回原因' })
    remark = result.value
  }
  await adminApi.reviewMerchantApplication(row.id, { decision, remark, commissionRate })
  ElMessage.success('入驻申请已处理')
  load()
}

async function moderateReview(row) {
  const status = row.status === 'HIDDEN' ? 'VISIBLE' : 'HIDDEN'
  const { value } = await ElMessageBox.prompt('请输入内容治理说明', status === 'HIDDEN' ? '屏蔽评价' : '恢复评价', { inputValue: status === 'HIDDEN' ? '评价包含违规内容' : '复核后符合展示规则' })
  await adminApi.moderateReview(row.id, { status, reason: value })
  ElMessage.success('评价状态已更新')
  load()
}

async function reviewWithdrawal(row, decision) {
  const { value } = await ElMessageBox.prompt('请输入审核或打款说明', decision === 'APPROVE' ? '审核打款' : '驳回提现', { inputPattern: /\S+/, inputErrorMessage: '请填写处理说明' })
  await adminApi.reviewWithdrawal(row.id, { decision, remark: value })
  ElMessage.success(decision === 'APPROVE' ? '模拟打款完成' : '提现已驳回，金额退回商家余额')
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
