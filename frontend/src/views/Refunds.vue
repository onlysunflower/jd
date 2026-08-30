<template>
  <div class="page">
    <div class="toolbar">
      <div><h2>我的售后与评价</h2><p class="muted">跟进售后处理，也可以为已经完成的订单分享真实体验</p></div>
      <el-button :icon="Refresh" :loading="currentLoading" @click="refreshCurrent">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="service-tabs">
      <el-tab-pane name="refunds"><template #label><span class="tab-label"><el-icon><Service /></el-icon>售后记录</span></template></el-tab-pane>
      <el-tab-pane name="reviews"><template #label><span class="tab-label"><el-icon><ChatDotRound /></el-icon>商品评价<el-badge v-if="pendingReviewCount" :value="pendingReviewCount" /></span></template></el-tab-pane>
    </el-tabs>

    <template v-if="activeTab === 'refunds'">
      <div class="stat-grid">
        <div class="stat-card"><span>售后单</span><strong>{{ refunds.length }}</strong></div>
        <div class="stat-card"><span>等待商家</span><strong>{{ countBy(['MERCHANT_REVIEWING']) }}</strong></div>
        <div class="stat-card"><span>平台介入</span><strong>{{ countBy(['PLATFORM_INTERVENING']) }}</strong></div>
        <div class="stat-card"><span>已完成</span><strong>{{ countBy(['REFUND_SUCCESS', 'REFUND_FAILED']) }}</strong></div>
      </div>

      <div v-if="refundLoading" class="refund-list"><div v-for="item in 3" :key="item" class="skeleton-card"><el-skeleton animated :rows="3" /></div></div>
      <div v-else-if="refunds.length" class="refund-list">
        <article v-for="row in refunds" :key="row.id" class="refund-card">
          <header class="refund-card__head"><div class="order-reference"><span>售后单 <b>#{{ row.id }}</b></span><span>关联订单 #{{ row.orderId }}</span><span>申请于 {{ row.createdAt }}</span></div><el-tag :type="statusType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag></header>
          <div class="refund-card__body">
            <div><span class="refund-detail__label">售后类型 · 退款金额</span><div class="refund-detail__value"><strong>{{ typeText(row.type) }}</strong>　<span class="price small"><em>¥</em>{{ row.amount }}</span></div><div v-if="row.returnLogisticsNo" class="refund-detail__value muted return-logistics">退货物流：{{ row.returnLogisticsNo }}</div></div>
            <div><span class="refund-detail__label">申请原因</span><div class="refund-detail__value reason">{{ row.reason }}</div><div class="refund-process"><span v-for="step in processSteps(row.status)" :key="step.label" :class="step.state"><i />{{ step.label }}</span></div></div>
            <div class="refund-actions"><span class="refund-detail__label">最近更新 {{ row.updatedAt || row.createdAt }}</span><div class="compact-actions"><el-button v-if="row.status === 'WAIT_USER_RETURN'" type="primary" size="small" :icon="Van" @click="openReturn(row)">安排退货</el-button><el-button v-if="['MERCHANT_REJECTED','MERCHANT_REVIEWING'].includes(row.status)" type="danger" plain size="small" :icon="Warning" @click="intervention(row)">平台介入</el-button><el-button size="small" :icon="Tickets" @click="showLogs(row)">查看进度</el-button></div></div>
          </div>
        </article>
      </div>
      <div v-else class="empty-state"><el-empty description="暂无售后记录" :image-size="112" /><p class="empty-state__copy">提交售后申请后，处理进度会在这里持续更新</p></div>
    </template>

    <template v-else>
      <div class="review-toolbar">
        <div class="review-overview"><span>可评价商品 <b>{{ reviewTasks.length }}</b></span><span>待评价 <b class="danger-text">{{ pendingReviewCount }}</b></span><span>已评价 <b>{{ reviewedCount }}</b></span></div>
        <el-radio-group v-model="reviewFilter" size="small"><el-radio-button value="all">全部</el-radio-button><el-radio-button value="pending">待评价</el-radio-button><el-radio-button value="reviewed">已评价</el-radio-button></el-radio-group>
      </div>

      <div v-if="reviewLoading" class="evaluation-list"><div v-for="item in 3" :key="item" class="skeleton-card"><el-skeleton animated :rows="3" /></div></div>
      <div v-else-if="filteredReviewTasks.length" class="evaluation-list">
        <article v-for="task in filteredReviewTasks" :key="`${task.orderId}-${task.productId}`" class="evaluation-card">
          <button class="evaluation-product" title="查看该商品的全部评价" @click="showProductReviews(task)">
            <img :src="task.productImage || '/products/catalog-collection.png'" :alt="task.productName" />
            <span><strong>{{ task.productName }}</strong><small>订单 {{ task.orderNo }}</small><small>完成于 {{ formatTime(task.completedAt) }}</small></span>
          </button>
          <div class="evaluation-purchase"><span>购买数量 × {{ task.quantity }}</span><strong>¥{{ money(task.price) }}</strong></div>
          <div v-if="task.reviewed" class="evaluation-content"><el-rate :model-value="task.rating" disabled /><p>{{ task.content }}</p><p v-if="task.appendContent" class="append-copy"><b>追评：</b>{{ task.appendContent }}</p><small>评价于 {{ formatTime(task.reviewedAt) }}</small></div>
          <div v-else class="evaluation-content pending"><strong>这件商品还没有评价</strong><p>评价将公开展示在商品评价中，帮助其他用户做出选择。</p></div>
          <div class="evaluation-actions"><el-tag :type="task.reviewed ? 'success' : 'warning'" effect="plain">{{ task.reviewed ? '已评价' : '待评价' }}</el-tag><el-button size="small" @click="showProductReviews(task)">查看评价</el-button><el-button v-if="!task.reviewed" type="primary" size="small" :icon="EditPen" @click="openReviewForm(task)">去评价</el-button><el-button v-else-if="!task.appendContent" type="primary" plain size="small" :icon="EditPen" @click="openAppendForm(task)">写追评</el-button></div>
        </article>
      </div>
      <div v-else class="empty-state"><el-empty :description="reviewFilter === 'pending' ? '没有待评价商品' : '暂无符合条件的评价记录'" :image-size="112" /><p class="empty-state__copy">确认收货并完成订单后，对应商品会出现在这里</p></div>
    </template>

    <el-dialog v-model="logsVisible" title="售后进度" width="min(600px, 92vw)">
      <el-timeline><el-timeline-item v-for="log in logs" :key="log.id" :timestamp="log.createdAt"><strong>{{ roleText(log.operatorRole) }} - {{ actionText(log.action) }}</strong><p>{{ log.remark }}</p></el-timeline-item></el-timeline>
      <div v-if="!logs.length" class="empty-hint">暂无流转记录</div>
    </el-dialog>

    <el-dialog v-model="appendVisible" title="发表追评" width="min(520px, 92vw)">
      <el-input v-model="appendContent" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="补充一段使用后的真实感受" />
      <template #footer><el-button @click="appendVisible = false">取消</el-button><el-button type="primary" :loading="reviewSubmitting" @click="submitAppend">提交追评</el-button></template>
    </el-dialog>

    <el-dialog v-model="returnVisible" title="安排退货" width="min(560px, 92vw)">
      <el-alert title="仅已确认收货的退货退款需要寄回商品；未收货或物流中的订单由系统直接退款。" type="info" show-icon :closable="false" />
      <el-form label-position="top" class="return-form">
        <el-form-item label="寄件方式"><el-radio-group v-model="returnForm.method"><el-radio-button value="PICKUP">上门取件</el-radio-button><el-radio-button value="DROPOFF">自行到快递站寄件</el-radio-button></el-radio-group></el-form-item>
        <el-form-item label="物流公司"><el-select v-model="returnForm.company" style="width:100%" @change="generateReturnNo"><el-option label="京东快递" value="京东快递" /><el-option label="顺丰速运" value="顺丰速运" /><el-option label="中通快递" value="中通快递" /></el-select></el-form-item>
        <el-form-item label="模拟退货单号"><el-input v-model="returnForm.logisticsNo"><template #append><el-button @click="generateReturnNo">重新生成</el-button></template></el-input></el-form-item>
      </el-form>
      <template #footer><el-button @click="returnVisible = false">取消</el-button><el-button type="primary" @click="submitReturn">确认寄件</el-button></template>
    </el-dialog>

    <el-dialog v-model="reviewFormVisible" title="发表商品评价" width="min(560px, 92vw)" destroy-on-close>
      <div v-if="currentTask" class="review-form-product"><img :src="currentTask.productImage || '/products/catalog-collection.png'" :alt="currentTask.productName" /><div><strong>{{ currentTask.productName }}</strong><span>订单 {{ currentTask.orderNo }}</span></div></div>
      <el-form label-position="top">
        <el-form-item label="商品评分"><el-rate v-model="reviewForm.rating" show-text :texts="['很差', '较差', '一般', '满意', '非常满意']" /></el-form-item>
        <el-form-item label="评价内容"><el-input v-model="reviewForm.content" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="说说商品质量、使用感受或物流包装吧" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="reviewFormVisible = false">取消</el-button><el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button></template>
    </el-dialog>

    <ProductReviewsDialog v-model="reviewsVisible" :product="selectedProduct" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, EditPen, Refresh, Service, Tickets, Van, Warning } from '@element-plus/icons-vue'
import ProductReviewsDialog from '../components/ProductReviewsDialog.vue'
import { refundApi, reviewApi } from '../api'

const route = useRoute()
const activeTab = ref(route.query.tab === 'reviews' ? 'reviews' : 'refunds')
const refunds = ref([])
const refundLoading = ref(false)
const logs = ref([])
const logsVisible = ref(false)
const reviewTasks = ref([])
const reviewLoading = ref(false)
const reviewFilter = ref('all')
const reviewFormVisible = ref(false)
const appendVisible = ref(false)
const appendContent = ref('')
const returnVisible = ref(false)
const currentReturn = ref(null)
const reviewSubmitting = ref(false)
const currentTask = ref(null)
const reviewsVisible = ref(false)
const selectedProduct = ref(null)
const reviewForm = reactive({ rating: 5, content: '' })
const returnForm = reactive({ method: 'PICKUP', company: '京东快递', logisticsNo: '' })

const pendingReviewCount = computed(() => reviewTasks.value.filter((item) => !item.reviewed).length)
const reviewedCount = computed(() => reviewTasks.value.filter((item) => item.reviewed).length)
const filteredReviewTasks = computed(() => reviewTasks.value.filter((item) => reviewFilter.value === 'all' || (reviewFilter.value === 'reviewed') === item.reviewed))
const currentLoading = computed(() => activeTab.value === 'refunds' ? refundLoading.value : reviewLoading.value)

function countBy(statuses) { return refunds.value.filter((item) => statuses.includes(item.status)).length }
function typeText(type) { return type === 'REFUND_ONLY' ? '仅退款' : '退货退款' }
function money(value) { return Number(value || 0).toFixed(2) }
function formatTime(value) { return value ? String(value).replace('T', ' ') : '' }

function statusText(status) {
  const labels = { MERCHANT_REVIEWING: '商家审核中', MERCHANT_APPROVED: '商家已同意', MERCHANT_REJECTED: '商家已拒绝', WAIT_USER_RETURN: '等待用户退货', WAIT_MERCHANT_RECEIVE: '等待商家收货', PLATFORM_INTERVENING: '平台介入中', REFUND_SUCCESS: '退款成功', REFUND_FAILED: '退款失败', CLOSED: '售后关闭' }
  return labels[status] || status
}

function statusType(status) {
  const map = { MERCHANT_REVIEWING: 'warning', WAIT_USER_RETURN: 'primary', WAIT_MERCHANT_RECEIVE: 'primary', MERCHANT_REJECTED: 'danger', PLATFORM_INTERVENING: 'danger', REFUND_SUCCESS: 'success', REFUND_FAILED: 'info', CLOSED: 'info' }
  return map[status] || 'info'
}

function roleText(role) {
  const labels = { USER: '用户', MERCHANT: '商家', SERVICE_ADMIN: '客服管理员', PRODUCT_ADMIN: '商品审核员', SUPER_ADMIN: '超级管理员' }
  return labels[role] || role
}

function actionText(action) {
  const labels = { CREATE: '提交申请', MERCHANT_APPROVE: '商家同意', MERCHANT_REJECT: '商家拒绝', USER_RETURN: '用户退货', USER_REQUEST_INTERVENTION: '申请介入', ADMIN_ARBITRATE: '平台仲裁', MERCHANT_CONFIRM_RETURN: '确认退款' }
  return labels[action] || action
}

async function loadRefunds() { refundLoading.value = true; try { refunds.value = await refundApi.list() } finally { refundLoading.value = false } }
async function loadReviewTasks() { reviewLoading.value = true; try { reviewTasks.value = await reviewApi.tasks() } finally { reviewLoading.value = false } }
function refreshCurrent() { return activeTab.value === 'refunds' ? loadRefunds() : loadReviewTasks() }

function processSteps(status) {
  const steps = ['申请售后', '商家审核', '退货处理', '平台处理', '完成']
  const current = status === 'MERCHANT_REVIEWING' ? 1 : status === 'MERCHANT_APPROVED' || status === 'WAIT_USER_RETURN' || status === 'WAIT_MERCHANT_RECEIVE' ? 2 : status === 'PLATFORM_INTERVENING' || status === 'MERCHANT_REJECTED' ? 3 : ['REFUND_SUCCESS', 'REFUND_FAILED', 'CLOSED'].includes(status) ? 4 : 0
  return steps.map((label, index) => ({ label, state: index < current ? 'is-done' : index === current ? 'is-current' : '' }))
}

function generateReturnNo() { const prefix = returnForm.company === '顺丰速运' ? 'SF' : returnForm.company === '中通快递' ? 'ZT' : 'JDR'; returnForm.logisticsNo = `${prefix}${Date.now()}${Math.floor(Math.random() * 90 + 10)}` }
function openReturn(row) { currentReturn.value = row; returnForm.method = 'PICKUP'; returnForm.company = '京东快递'; generateReturnNo(); returnVisible.value = true }
async function submitReturn() { if (!returnForm.logisticsNo.trim()) { ElMessage.warning('请生成物流单号'); return }; await refundApi.submitReturn(currentReturn.value.id, { returnLogisticsNo: returnForm.logisticsNo.trim() }); ElMessage.success(returnForm.method === 'PICKUP' ? '上门取件预约成功，运单已生成' : '寄件信息已提交商家'); returnVisible.value = false; await loadRefunds() }

async function intervention(row) { await refundApi.intervention(row.id); ElMessage.success('已申请平台介入'); await loadRefunds() }
async function showLogs(row) { logs.value = await refundApi.logs(row.id); logsVisible.value = true }

function showProductReviews(task) { selectedProduct.value = { id: task.productId, name: task.productName }; reviewsVisible.value = true }
function openReviewForm(task) { currentTask.value = task; reviewForm.rating = 5; reviewForm.content = ''; reviewFormVisible.value = true }
function openAppendForm(task) { currentTask.value = task; appendContent.value = ''; appendVisible.value = true }

async function submitReview() {
  if (!reviewForm.content.trim()) { ElMessage.warning('请填写评价内容'); return }
  reviewSubmitting.value = true
  try {
    await reviewApi.create({ orderId: currentTask.value.orderId, productId: currentTask.value.productId, rating: reviewForm.rating, content: reviewForm.content.trim() })
    ElMessage.success('评价发表成功，感谢你的分享')
    reviewFormVisible.value = false
    await loadReviewTasks()
  } finally { reviewSubmitting.value = false }
}

async function submitAppend() {
  if (!appendContent.value.trim()) { ElMessage.warning('请填写追评内容'); return }
  reviewSubmitting.value = true
  try { await reviewApi.append(currentTask.value.reviewId, { content: appendContent.value.trim() }); ElMessage.success('追评发表成功'); appendVisible.value = false; await loadReviewTasks() }
  finally { reviewSubmitting.value = false }
}

onMounted(() => Promise.all([loadRefunds(), loadReviewTasks()]))
</script>

<style scoped>
.service-tabs { margin-bottom: 18px; padding: 0 18px; border: 1px solid var(--line); border-radius: 8px; background: #fff; }
.tab-label { display: inline-flex; align-items: center; gap: 7px; }
.tab-label :deep(.el-badge__content) { position: static; transform: none; margin-left: 2px; }
.return-logistics { margin-top: 8px; }
.return-form { margin-top: 18px; }
.append-copy { padding-top: 6px; border-top: 1px dashed var(--line); }
.review-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 18px; margin-bottom: 16px; padding: 16px 18px; border: 1px solid var(--line); border-radius: 8px; background: #fff; }
.review-overview { display: flex; flex-wrap: wrap; gap: 12px 28px; color: #6b7280; font-size: 14px; }
.review-overview b { margin-left: 5px; color: #111827; font-size: 18px; }
.review-overview .danger-text { color: #e2231a; }
.evaluation-list { display: grid; gap: 12px; }
.evaluation-card { display: grid; grid-template-columns: minmax(260px, 1.2fr) 140px minmax(220px, 1fr) auto; align-items: center; gap: 20px; padding: 18px; border: 1px solid var(--line); border-radius: 8px; background: #fff; transition: border-color .2s, box-shadow .2s; }
.evaluation-card:hover { border-color: #f2b8b5; box-shadow: 0 6px 18px rgba(31, 41, 55, .06); }
.evaluation-product { display: grid; grid-template-columns: 74px minmax(0, 1fr); align-items: center; gap: 14px; padding: 0; border: 0; background: transparent; text-align: left; cursor: pointer; }
.evaluation-product img { width: 74px; height: 74px; border: 1px solid #eef0f2; border-radius: 7px; object-fit: cover; background: #f7f8fa; }
.evaluation-product > span { display: grid; gap: 6px; min-width: 0; }
.evaluation-product strong { overflow: hidden; color: #1f2937; text-overflow: ellipsis; white-space: nowrap; }
.evaluation-product:hover strong { color: #e2231a; }
.evaluation-product small, .evaluation-content small { color: #9ca3af; }
.evaluation-purchase { display: grid; gap: 7px; color: #6b7280; font-size: 13px; }
.evaluation-purchase strong { color: #e2231a; font-size: 18px; }
.evaluation-content { min-width: 0; }
.evaluation-content :deep(.el-rate) { height: 20px; }
.evaluation-content p { display: -webkit-box; overflow: hidden; margin: 6px 0; color: #4b5563; font-size: 13px; line-height: 1.55; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.evaluation-content.pending strong { color: #374151; font-size: 14px; }
.evaluation-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; min-width: 190px; }
.review-form-product { display: grid; grid-template-columns: 64px 1fr; align-items: center; gap: 14px; margin-bottom: 20px; padding: 12px; border-radius: 7px; background: #f7f8fa; }
.review-form-product img { width: 64px; height: 64px; border-radius: 6px; object-fit: cover; }
.review-form-product > div { display: grid; gap: 7px; }
.review-form-product span { color: #6b7280; font-size: 13px; }
@media (max-width: 1100px) { .evaluation-card { grid-template-columns: minmax(260px, 1fr) 120px minmax(200px, 1fr); }.evaluation-actions { grid-column: 1 / -1; justify-content: flex-end; } }
@media (max-width: 720px) { .review-toolbar { align-items: flex-start; flex-direction: column; }.evaluation-card { grid-template-columns: 1fr; gap: 14px; }.evaluation-actions { grid-column: auto; justify-content: flex-start; }.evaluation-purchase { display: flex; justify-content: space-between; }.evaluation-content { padding: 12px 0; border-top: 1px solid #eef0f2; border-bottom: 1px solid #eef0f2; } }
</style>
