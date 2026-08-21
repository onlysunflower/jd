<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>商家后台</h2>
        <p class="muted">发布商品、处理订单发货、审核退款申请</p>
      </div>
      <el-button type="primary" @click="productVisible = true">发布商品</el-button>
    </div>

    <el-tabs v-model="tab" class="panel">
      <el-tab-pane label="商品管理" name="products">
        <el-table :data="products">
          <el-table-column prop="name" label="商品" min-width="180" />
          <el-table-column prop="price" label="价格" width="100" />
          <el-table-column prop="stock" label="库存" width="90" />
          <el-table-column prop="auditStatus" label="审核" width="120" />
          <el-table-column prop="shelfStatus" label="上下架" width="100" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="danger" @click="offShelf(row)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单发货" name="orders">
        <el-table :data="orders">
          <el-table-column prop="orderNo" label="订单号" min-width="170" />
          <el-table-column prop="totalAmount" label="金额" width="100" />
          <el-table-column prop="status" label="状态" width="130" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button v-if="row.status === 'WAIT_SHIP'" link type="primary" @click="ship(row)">发货</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="售后处理" name="refunds">
        <el-table :data="refunds">
          <el-table-column prop="id" label="售后单" width="90" />
          <el-table-column prop="orderId" label="订单" width="90" />
          <el-table-column prop="reason" label="原因" min-width="180" />
          <el-table-column prop="status" label="状态" width="180" />
          <el-table-column label="操作" width="280">
            <template #default="{ row }">
              <el-button v-if="row.status === 'MERCHANT_REVIEWING'" link type="primary" @click="approveRefund(row)">同意</el-button>
              <el-button v-if="row.status === 'MERCHANT_REVIEWING'" link type="danger" @click="rejectRefund(row)">拒绝</el-button>
              <el-button v-if="row.status === 'WAIT_MERCHANT_RECEIVE'" link type="primary" @click="confirmReturn(row)">确认收货并退款</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="productVisible" title="发布商品" width="560px">
      <el-form :model="productForm" label-width="86px">
        <el-form-item label="商品名称"><el-input v-model="productForm.name" /></el-form-item>
        <el-form-item label="副标题"><el-input v-model="productForm.subtitle" /></el-form-item>
        <el-form-item label="图片地址"><el-input v-model="productForm.mainImage" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="productForm.price" :min="0" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="productForm.stock" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productVisible = false">取消</el-button>
        <el-button type="primary" @click="createProduct">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantApi } from '../api'

const tab = ref('products')
const products = ref([])
const orders = ref([])
const refunds = ref([])
const productVisible = ref(false)
const productForm = reactive({
  categoryId: 1,
  name: '',
  subtitle: '',
  mainImage: '',
  price: 99,
  stock: 100
})

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

async function ship(row) {
  const { value } = await ElMessageBox.prompt('请输入物流单号', '订单发货')
  await merchantApi.ship(row.id, { logisticsCompany: '京东快递', logisticsNo: value })
  ElMessage.success('已发货')
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
