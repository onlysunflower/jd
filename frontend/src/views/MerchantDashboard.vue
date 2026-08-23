<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>商家后台</h2>
        <p class="muted">发布商品、处理订单发货、审核退款申请</p>
      </div>
      <el-space>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="productVisible = true">发布商品</el-button>
      </el-space>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <span>商品数量</span>
        <strong>{{ products.length }}</strong>
      </div>
      <div class="stat-card">
        <span>待审核商品</span>
        <strong>{{ countProducts('PENDING') }}</strong>
      </div>
      <div class="stat-card">
        <span>待发货订单</span>
        <strong>{{ countOrders('WAIT_SHIP') }}</strong>
      </div>
      <div class="stat-card">
        <span>待处理售后</span>
        <strong>{{ countRefunds('MERCHANT_REVIEWING') }}</strong>
      </div>
    </div>

    <el-tabs v-model="tab" class="panel">
      <el-tab-pane label="商品管理" name="products">
        <el-table :data="products" empty-text="暂无商品">
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
          <el-table-column prop="sales" label="销量" width="90" />
          <el-table-column label="审核" width="120">
            <template #default="{ row }">
              <el-tag :type="auditType(row.auditStatus)">{{ auditText(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="上下架" width="100">
            <template #default="{ row }">
              <el-tag :type="row.shelfStatus === 'ON' ? 'success' : 'info'">{{ row.shelfStatus === 'ON' ? '上架' : '下架' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="danger" :icon="Close" @click="offShelf(row)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单发货" name="orders">
        <el-table :data="orders" empty-text="暂无订单">
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column label="金额" width="110">
            <template #default="{ row }">￥{{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="orderType(row.status)">{{ orderText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="receiverAddress" label="收货地址" min-width="240" show-overflow-tooltip />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button v-if="row.status === 'WAIT_SHIP'" link type="primary" :icon="Van" @click="ship(row)">发货</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="售后处理" name="refunds">
        <el-table :data="refunds" empty-text="暂无售后">
          <el-table-column prop="id" label="售后单" width="90" />
          <el-table-column prop="orderId" label="订单" width="90" />
          <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="170">
            <template #default="{ row }">
              <el-tag :type="refundType(row.status)">{{ refundText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <div class="compact-actions">
                <el-button v-if="row.status === 'MERCHANT_REVIEWING'" link type="primary" :icon="Check" @click="approveRefund(row)">同意</el-button>
                <el-button v-if="row.status === 'MERCHANT_REVIEWING'" link type="danger" :icon="Close" @click="rejectRefund(row)">拒绝</el-button>
                <el-button v-if="row.status === 'WAIT_MERCHANT_RECEIVE'" link type="primary" :icon="Money" @click="confirmReturn(row)">确认退款</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="productVisible" title="发布商品" width="580px">
      <el-form :model="productForm" label-width="86px">
        <el-form-item label="商品名称"><el-input v-model="productForm.name" /></el-form-item>
        <el-form-item label="副标题"><el-input v-model="productForm.subtitle" /></el-form-item>
        <el-form-item label="图片地址"><el-input v-model="productForm.mainImage" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="productForm.categoryId" style="width: 100%">
            <el-option label="手机数码" :value="1" />
            <el-option label="电脑办公" :value="2" />
            <el-option label="家用电器" :value="3" />
            <el-option label="生活百货" :value="4" />
            <el-option label="生鲜食品" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="productForm.price" :min="0" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="productForm.stock" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productVisible = false">取消</el-button>
        <el-button type="primary" @click="createProduct">提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="订单发货" width="500px">
      <el-alert
        title="课程项目中使用模拟物流单号。真实业务里单号通常由京东物流、快递鸟、顺丰等物流系统创建运单后返回。"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 18px"
      />
      <el-form :model="shipForm" label-width="90px">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.logisticsCompany" style="width: 100%" @change="generateLogisticsNo">
            <el-option label="京东快递" value="京东快递" />
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.logisticsNo">
            <template #append>
              <el-button @click="generateLogisticsNo">重新生成</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Money, Plus, Refresh, Van } from '@element-plus/icons-vue'
import { merchantApi } from '../api'

const tab = ref('products')
const products = ref([])
const orders = ref([])
const refunds = ref([])
const productVisible = ref(false)
const shipVisible = ref(false)
const currentOrder = ref(null)
const placeholder = 'https://dummyimage.com/120x90/f3f4f6/6b7280&text=JD'
const productForm = reactive({
  categoryId: 1,
  name: '',
  subtitle: '',
  mainImage: '',
  price: 99,
  stock: 100
})
const shipForm = reactive({
  logisticsCompany: '京东快递',
  logisticsNo: ''
})

function countProducts(status) {
  return products.value.filter((item) => item.auditStatus === status).length
}

function countOrders(status) {
  return orders.value.filter((item) => item.status === status).length
}

function countRefunds(status) {
  return refunds.value.filter((item) => item.status === status).length
}

function auditText(status) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[status] || status
}

function auditType(status) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info'
}

function orderText(status) {
  return {
    WAIT_PAY: '待付款',
    WAIT_SHIP: '待发货',
    WAIT_RECEIVE: '待收货',
    COMPLETED: '已完成',
    CANCELED: '已取消',
    REFUNDING: '退款中',
    REFUNDED: '已退款'
  }[status] || status
}

function orderType(status) {
  return { WAIT_SHIP: 'primary', WAIT_RECEIVE: 'success', COMPLETED: 'success', REFUNDING: 'danger', CANCELED: 'info' }[status] || 'warning'
}

function refundText(status) {
  return {
    MERCHANT_REVIEWING: '商家审核中',
    WAIT_USER_RETURN: '等待用户退货',
    WAIT_MERCHANT_RECEIVE: '等待商家收货',
    MERCHANT_REJECTED: '商家已拒绝',
    PLATFORM_INTERVENING: '平台介入中',
    REFUND_SUCCESS: '退款成功',
    REFUND_FAILED: '退款失败'
  }[status] || status
}

function refundType(status) {
  return { MERCHANT_REVIEWING: 'warning', WAIT_MERCHANT_RECEIVE: 'primary', PLATFORM_INTERVENING: 'danger', REFUND_SUCCESS: 'success', REFUND_FAILED: 'info' }[status] || 'info'
}

async function load() {
  products.value = await merchantApi.products()
  orders.value = await merchantApi.orders()
  refunds.value = await merchantApi.refunds()
}

async function createProduct() {
  await merchantApi.createProduct(productForm)
  ElMessage.success('商品已提交审核')
  productVisible.value = false
  load()
}

async function offShelf(row) {
  await merchantApi.offShelf(row.id)
  ElMessage.success('商品已下架')
  load()
}

function ship(row) {
  currentOrder.value = row
  shipForm.logisticsCompany = '京东快递'
  generateLogisticsNo()
  shipVisible.value = true
}

function generateLogisticsNo() {
  const prefixes = {
    京东快递: 'JDV',
    顺丰速运: 'SF',
    中通快递: 'ZT',
    圆通速递: 'YT'
  }
  const prefix = prefixes[shipForm.logisticsCompany] || 'EXP'
  shipForm.logisticsNo = `${prefix}${Date.now().toString().slice(-10)}${Math.floor(Math.random() * 90 + 10)}`
}

async function submitShip() {
  await merchantApi.ship(currentOrder.value.id, {
    logisticsCompany: shipForm.logisticsCompany,
    logisticsNo: shipForm.logisticsNo
  })
  ElMessage.success('已发货')
  shipVisible.value = false
  load()
}

async function approveRefund(row) {
  await merchantApi.approveRefund(row.id, { remark: '商家同意售后，请寄回商品' })
  ElMessage.success('已同意售后')
  load()
}

async function rejectRefund(row) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝售后')
  await merchantApi.rejectRefund(row.id, { remark: value })
  ElMessage.success('已拒绝售后')
  load()
}

async function confirmReturn(row) {
  await merchantApi.confirmReturn(row.id, { remark: '商家确认收到退货，退款完成' })
  ElMessage.success('退款完成')
  load()
}

watch(tab, load)
onMounted(load)
</script>

<style scoped>
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
</style>
