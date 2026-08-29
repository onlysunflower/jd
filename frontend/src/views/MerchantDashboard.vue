<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>商家后台</h2>
        <p class="muted">发布商品、处理订单发货、审核退款申请</p>
      </div>
      <el-space>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateProduct">发布商品</el-button>
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
          <el-table-column prop="rejectReason" label="驳回原因" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.auditStatus === 'REJECTED' ? (row.rejectReason || '—') : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="上下架" width="100">
            <template #default="{ row }">
              <el-tag :type="row.shelfStatus === 'ON' ? 'success' : 'info'">{{ row.shelfStatus === 'ON' ? '上架' : '下架' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button v-if="row.auditStatus === 'REJECTED' || row.auditStatus === 'PENDING'" link type="primary" :icon="Edit" @click="editProduct(row)">编辑</el-button>
              <el-button v-if="row.auditStatus === 'APPROVED' && row.shelfStatus === 'OFF'" link type="success" :icon="Top" @click="onShelf(row)">上架</el-button>
              <el-button link type="danger" :icon="Close" @click="offShelf(row)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单发货" name="orders">
        <el-radio-group v-model="orderFilter" style="margin-bottom: 14px">
          <el-radio-button label="全部" value="ALL" />
          <el-radio-button label="待发货" value="WAIT_SHIP" />
          <el-radio-button label="待收货" value="WAIT_RECEIVE" />
          <el-radio-button label="已完成" value="COMPLETED" />
        </el-radio-group>
        <el-table :data="filteredOrders" empty-text="暂无订单">
          <el-table-column prop="orderNo" label="订单号" min-width="175" />
          <el-table-column label="商品明细" min-width="280">
            <template #default="{ row }">
              <div v-for="item in row.items" :key="item.id" class="order-item-cell">
                <img :src="item.productImage || placeholder" alt="商品图" />
                <span class="order-item-name">{{ item.productName }}</span>
                <span class="muted">￥{{ item.price }} × {{ item.quantity }}</span>
              </div>
              <span v-if="!row.items || !row.items.length" class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100">
            <template #default="{ row }">￥{{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="orderType(row.status)">{{ orderText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="收货人及电话" min-width="180">
            <template #default="{ row }">
              <div>{{ row.receiver }}</div>
              <div class="muted">{{ row.receiverPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="receiverAddress" label="收货地址" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'WAIT_SHIP'" link type="primary" :icon="Van" @click="ship(row)">发货</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="售后处理" name="refunds">
        <el-table :data="refunds" empty-text="暂无售后">
          <el-table-column prop="id" label="售后单" width="80" />
          <el-table-column prop="orderId" label="订单" width="80" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ refundTypeText(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
          <el-table-column label="金额" width="100">
            <template #default="{ row }">￥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="申请时间" width="170">
            <template #default="{ row }">
              <span class="muted">{{ row.createdAt }}</span>
              <div v-if="row.status === 'WAIT_MERCHANT_RECEIVE' && row.returnLogisticsNo" class="muted">退货单号：{{ row.returnLogisticsNo }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="refundType(row.status)">{{ refundText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="{ row }">
              <div class="compact-actions">
                <span v-if="row.status === 'PLATFORM_INTERVENING' || row.status === 'REFUND_SUCCESS' || row.status === 'REFUND_FAILED'" class="platform-tag">{{ refundText(row.status) }}</span>
                <el-button v-else-if="row.status === 'MERCHANT_REVIEWING'" link type="primary" :icon="Check" @click="approveRefund(row)">同意</el-button>
                <el-button v-if="row.status === 'MERCHANT_REVIEWING'" link type="danger" :icon="Close" @click="rejectRefund(row)">拒绝</el-button>
                <el-button v-else-if="row.status === 'WAIT_MERCHANT_RECEIVE'" link type="primary" :icon="Money" @click="confirmReturn(row)">确认退款</el-button>
                <el-button link type="info" :icon="Tickets" @click="showRefundDetail(row)">详情</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="productVisible" :title="editingProductId ? '编辑商品' : '发布商品'" width="580px" @open="loadCategories">
      <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-width="86px">
        <el-form-item label="商品名称" prop="name"><el-input v-model="productForm.name" /></el-form-item>
        <el-form-item label="副标题" prop="subtitle"><el-input v-model="productForm.subtitle" /></el-form-item>
        <el-form-item label="图片地址" prop="mainImage"><el-input v-model="productForm.mainImage" placeholder="请输入以 http:// 或 https:// 开头的图片地址" /></el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" style="width: 100%" :placeholder="categories.length ? '请选择分类' : '暂无可用分类'">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price"><el-input-number v-model="productForm.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存" prop="stock"><el-input-number v-model="productForm.stock" :min="0" :precision="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productVisible = false">取消</el-button>
        <el-button type="primary" :loading="productSubmitting" @click="submitProduct">{{ editingProductId ? '重新提交审核' : '提交审核' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="订单发货" width="560px">
      <div v-if="currentOrder" class="ship-preview">
        <div class="ship-preview-title">订单信息核对</div>
        <div class="ship-preview-row">
          <span>收货人：</span><strong>{{ currentOrder.receiver || '—' }}</strong>
        </div>
        <div class="ship-preview-row">
          <span>收货电话：</span><strong>{{ currentOrder.receiverPhone || '—' }}</strong>
        </div>
        <div class="ship-preview-row">
          <span>收货地址：</span><strong class="ship-preview-addr">{{ currentOrder.receiverAddress || '—' }}</strong>
        </div>
        <div class="ship-preview-row">
          <span>商品明细：</span>
          <span v-if="!currentOrder.items || !currentOrder.items.length">—</span>
          <span v-else class="ship-preview-items">
            <span v-for="(item, i) in currentOrder.items" :key="item.id">
              {{ item.productName }} × {{ item.quantity }}<template v-if="i < currentOrder.items.length - 1">；</template>
            </span>
          </span>
        </div>
      </div>
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

    <el-dialog v-model="refundDetailVisible" title="售后详情" width="560px">
      <template v-if="currentRefund">
        <div class="refund-detail-grid">
          <div><span class="muted">售后单</span><b>#{{ currentRefund.id }}</b></div>
          <div><span class="muted">关联订单</span><b>#{{ currentRefund.orderId }}</b></div>
          <div><span class="muted">售后类型</span><b>{{ refundTypeText(currentRefund.type) }}</b></div>
          <div><span class="muted">退款金额</span><b>￥{{ currentRefund.amount }}</b></div>
          <div><span class="muted">售后状态</span><b>{{ refundText(currentRefund.status) }}</b></div>
          <div><span class="muted">申请时间</span><b>{{ currentRefund.createdAt }}</b></div>
        </div>
        <div class="refund-detail-section">
          <div class="muted">申请原因</div>
          <div>{{ currentRefund.reason || '—' }}</div>
        </div>
        <div class="refund-detail-section">
          <div class="muted">商家回复</div>
          <div>{{ currentRefund.merchantReply || '—' }}</div>
        </div>
        <div v-if="currentRefund.returnLogisticsNo" class="refund-detail-section">
          <div class="muted">退货物流单号</div>
          <div>{{ currentRefund.returnLogisticsNo }}</div>
        </div>
        <div class="refund-detail-section">
          <div class="muted">售后进度时间线</div>
          <el-timeline v-if="refundLogs.length">
            <el-timeline-item v-for="log in refundLogs" :key="log.id" :timestamp="log.createdAt">
              <div>{{ refundLogActionText(log.action) }}</div>
              <div v-if="log.remark" class="muted">{{ log.remark }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="muted">暂无进度记录</div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Edit, Money, Plus, Refresh, Tickets, Top, Van } from '@element-plus/icons-vue'
import { merchantApi, refundApi } from '../api'

const tab = ref('products')
const products = ref([])
const orders = ref([])
const refunds = ref([])
const orderFilter = ref('WAIT_SHIP')
const refundDetailVisible = ref(false)
const currentRefund = ref(null)
const refundLogs = ref([])
const categories = ref([])
const productVisible = ref(false)
const shipVisible = ref(false)
const currentOrder = ref(null)
const editingProductId = ref(null)
const productFormRef = ref(null)
const productSubmitting = ref(false)
const placeholder = 'https://dummyimage.com/120x90/f3f4f6/6b7280&text=JD'
const productForm = reactive({
  categoryId: 1,
  name: '',
  subtitle: '',
  mainImage: '',
  price: 99,
  stock: 100
})
const productRules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 60, message: '商品名称长度需在 2~60 之间', trigger: 'blur' }
  ],
  mainImage: [
    { required: true, message: '请输入图片地址', trigger: 'blur' },
    { pattern: /^https?:\/\/.+/, message: '图片地址需以 http:// 或 https:// 开头', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', message: '价格必须是数字', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '请输入库存', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存必须为非负整数', trigger: 'blur' }
  ]
}
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

const filteredOrders = computed(() => {
  if (orderFilter.value === 'ALL') return orders.value
  return orders.value.filter((item) => item.status === orderFilter.value)
})

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

function refundTypeText(type) {
  return type === 'REFUND_ONLY' ? '仅退款' : '退货退款'
}

function refundLogActionText(action) {
  return {
    CREATE: '用户提交售后申请',
    MERCHANT_APPROVE: '商家同意售后',
    MERCHANT_REJECT: '商家拒绝售后',
    USER_RETURN: '用户填写退货物流',
    MERCHANT_CONFIRM_RETURN: '商家确认收货并退款',
    USER_REQUEST_INTERVENTION: '用户申请平台介入',
    ADMIN_ARBITRATE: '平台仲裁'
  }[action] || action
}

async function load() {
  products.value = await merchantApi.products()
  orders.value = await merchantApi.orders()
  refunds.value = await merchantApi.refunds()
}

function openCreateProduct() {
  editingProductId.value = null
  productForm.categoryId = 1
  productForm.name = ''
  productForm.subtitle = ''
  productForm.mainImage = ''
  productForm.price = 99
  productForm.stock = 100
  productVisible.value = true
  clearProductValidate()
}

function editProduct(row) {
  editingProductId.value = row.id
  productForm.categoryId = row.categoryId
  productForm.name = row.name
  productForm.subtitle = row.subtitle || ''
  productForm.mainImage = row.mainImage || ''
  productForm.price = row.price
  productForm.stock = row.stock
  productVisible.value = true
  clearProductValidate()
}

function clearProductValidate() {
  if (productFormRef.value) {
    productFormRef.value.clearValidate()
  }
}

async function loadCategories() {
  try {
    categories.value = await merchantApi.categories()
    if (!categories.value.length) {
      ElMessage.warning('暂无可用分类，请联系管理员')
    }
  } catch (e) {
    categories.value = []
  }
}

async function submitProduct() {
  if (!categories.value.length) {
    ElMessage.warning('暂无可用分类，请联系管理员')
    return
  }
  try {
    await productFormRef.value.validate()
  } catch (e) {
    return
  }
  productSubmitting.value = true
  try {
    if (editingProductId.value) {
      await merchantApi.updateProduct(editingProductId.value, productForm)
      ElMessage.success('商品已重新提交审核')
    } else {
      await merchantApi.createProduct(productForm)
      ElMessage.success('商品已提交审核')
    }
    productVisible.value = false
    load()
  } finally {
    productSubmitting.value = false
  }
}

async function offShelf(row) {
  await merchantApi.offShelf(row.id)
  ElMessage.success('商品已下架')
  load()
}

async function onShelf(row) {
  await merchantApi.onShelf(row.id)
  ElMessage.success('商品已上架')
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
  try {
    await ElMessageBox.confirm('确认对该订单发货？发货后不可撤销', '确认发货', {
      type: 'warning',
      confirmButtonText: '确认发货',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  await merchantApi.ship(currentOrder.value.id, {
    logisticsCompany: shipForm.logisticsCompany,
    logisticsNo: shipForm.logisticsNo
  })
  ElMessage.success('已发货')
  shipVisible.value = false
  load()
}

async function approveRefund(row) {
  try {
    await ElMessageBox.confirm('确认同意该售后申请？同意后等待用户退货', '同意售后', {
      type: 'warning',
      confirmButtonText: '同意',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
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
  try {
    await ElMessageBox.confirm('确认收到退货并退款？确认后不可撤销', '确认退款', {
      type: 'warning',
      confirmButtonText: '确认退款',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  await merchantApi.confirmReturn(row.id, { remark: '商家确认收到退货，退款完成' })
  ElMessage.success('退款完成')
  load()
}

async function showRefundDetail(row) {
  currentRefund.value = row
  refundLogs.value = []
  refundDetailVisible.value = true
  try {
    refundLogs.value = await refundApi.logs(row.id)
  } catch (e) {
    refundLogs.value = []
  }
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

.order-item-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 5px 0;
}

.order-item-cell + .order-item-cell {
  border-top: 1px dashed var(--line);
}

.order-item-cell img {
  width: 44px;
  height: 36px;
  object-fit: cover;
  border: 1px solid var(--line);
  border-radius: 4px;
  flex-shrink: 0;
}

.order-item-name {
  flex: 1;
  margin-right: 8px;
}

.ship-preview {
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 16px;
  font-size: 13px;
}

.ship-preview-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.ship-preview-row {
  display: flex;
  gap: 8px;
  line-height: 1.7;
}

.ship-preview-row > span:first-child {
  color: var(--muted);
  flex-shrink: 0;
  width: 64px;
}

.ship-preview-addr {
  word-break: break-all;
}

.refund-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 20px;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 14px;
  margin-bottom: 14px;
}

.refund-detail-grid > div span {
  display: block;
  font-size: 12px;
  margin-bottom: 2px;
}

.refund-detail-section {
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 12px;
}

.refund-detail-section > .muted {
  margin-bottom: 4px;
}

.platform-tag {
  color: var(--el-color-info);
  font-size: 13px;
}
</style>
