<template>
  <div class="page">
    <div class="toolbar"><div><h2>我的订单</h2><p class="muted">查看订单进度并完成支付、取消或确认收货</p></div><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button></div>
    <div class="stat-grid"><div class="stat-card"><span>全部订单</span><strong>{{ orders.length }}</strong></div><div class="stat-card"><span>待处理</span><strong>{{ countBy(['WAIT_PAY', 'WAIT_SHIP', 'WAIT_RECEIVE']) }}</strong></div><div class="stat-card"><span>退款中</span><strong>{{ countBy(['REFUNDING']) }}</strong></div><div class="stat-card"><span>已完成</span><strong>{{ countBy(['COMPLETED', 'REFUNDED']) }}</strong></div></div>
    <el-tabs v-model="activeStatus" class="order-tabs"><el-tab-pane v-for="tab in orderTabs" :key="tab.value" :label="tab.label" :name="tab.value" /></el-tabs>
    <div v-if="loading" class="order-list"><div v-for="item in 3" :key="item" class="skeleton-card"><el-skeleton animated :rows="4" /></div></div>
    <div v-else-if="filteredOrders.length" class="order-list">
      <article v-for="row in filteredOrders" :key="row.id" class="order-card">
        <header class="order-card__head"><div class="order-reference"><span>订单号 <b>{{ row.orderNo }}</b></span><span>下单时间 {{ row.createdAt }}</span></div><el-tag :type="statusType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag></header>
        <div class="order-card__body"><div class="order-summary"><div class="order-product-placeholder"><el-icon><Goods /></el-icon></div><div class="order-summary__text"><strong>订单金额 ¥{{ money(row.totalAmount) }}</strong><span class="order-summary__address">收货地址：{{ row.receiverAddress }}</span><span v-if="row.logisticsNo">物流：{{ row.logisticsCompany || '物流公司待补充' }} · {{ row.logisticsNo }}</span><span v-else>物流：待商家发货</span></div></div><div class="order-finance"><span class="price small"><em>¥</em>{{ money(row.totalAmount) }}</span><div class="compact-actions"><el-button size="small" @click="router.push(`/orders/${row.id}`)">查看详情</el-button><el-button v-if="row.status === 'WAIT_PAY'" type="primary" size="small" :icon="CreditCard" :loading="isProcessing(row.id, 'pay')" :disabled="isProcessing(row.id)" @click="pay(row)">支付</el-button><el-button v-if="row.status === 'WAIT_PAY'" type="danger" plain size="small" :icon="Close" :loading="isProcessing(row.id, 'cancel')" :disabled="isProcessing(row.id)" @click="cancel(row)">取消</el-button><el-button v-if="row.status === 'WAIT_RECEIVE'" type="primary" size="small" :icon="Check" :loading="isProcessing(row.id, 'confirm')" :disabled="isProcessing(row.id)" @click="confirm(row)">确认收货</el-button></div></div></div>
      </article>
    </div>
    <div v-else class="empty-state"><el-empty description="暂无相关订单" :image-size="112" /><p class="empty-state__copy">下单后的商品会在这里展示订单进度</p><el-button type="primary" @click="router.push('/')">去逛逛</el-button></div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, CreditCard, Goods, Refresh } from '@element-plus/icons-vue'
import { orderApi } from '../api'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const activeStatus = ref('ALL')
const processingId = ref(null)
const processingAction = ref(null)
const labels = { WAIT_PAY: '待付款', WAIT_SHIP: '待发货', WAIT_RECEIVE: '待收货', COMPLETED: '已完成', CANCELED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款' }
const orderTabs = [{ label: '全部', value: 'ALL' }, { label: '待付款', value: 'WAIT_PAY' }, { label: '待发货', value: 'WAIT_SHIP' }, { label: '待收货', value: 'WAIT_RECEIVE' }, { label: '已完成', value: 'COMPLETED' }, { label: '已取消', value: 'CANCELED' }, { label: '退款中', value: 'REFUNDING' }, { label: '已退款', value: 'REFUNDED' }]
const filteredOrders = computed(() => activeStatus.value === 'ALL' ? orders.value : orders.value.filter((item) => item.status === activeStatus.value))
function money(value) { return Number(value || 0).toFixed(2) }
function statusText(status) { return labels[status] || status }
function statusType(status) { return ({ WAIT_PAY: 'warning', WAIT_SHIP: 'primary', WAIT_RECEIVE: 'success', COMPLETED: 'success', CANCELED: 'info', REFUNDING: 'danger', REFUNDED: 'success' })[status] || 'info' }
function countBy(statuses) { return orders.value.filter((item) => statuses.includes(item.status)).length }
function isProcessing(id, action) { return processingId.value === id && (!action || processingAction.value === action) }
async function load() { loading.value = true; try { orders.value = await orderApi.list() } catch (error) { ElMessage.error(error.message || '订单加载失败') } finally { loading.value = false } }
async function operate(row, action, title, message, request) { try { await ElMessageBox.confirm(message, title, { type: 'warning' }) } catch { return } processingId.value = row.id; processingAction.value = action; try { await request(row.id); ElMessage.success(action === 'pay' ? '模拟支付成功' : action === 'cancel' ? '订单已取消' : '已确认收货'); await load() } catch (error) { ElMessage.error(error.message || '订单操作失败'); await load() } finally { processingId.value = null; processingAction.value = null } }
function pay(row) { return operate(row, 'pay', '确认支付', `确认支付订单 ${row.orderNo} 吗？`, orderApi.pay) }
function cancel(row) { return operate(row, 'cancel', '取消订单', `确认取消订单 ${row.orderNo} 吗？`, orderApi.cancel) }
function confirm(row) { return operate(row, 'confirm', '确认收货', `确认已收到订单 ${row.orderNo} 的商品吗？`, orderApi.confirm) }
onMounted(load)
</script>
