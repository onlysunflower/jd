import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body.data
  },
  (error) => {
    const message = error.response?.data?.message || (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '服务暂不可用，请确认后端服务已启动')
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data)
}

export const productApi = {
  list: (params) => request.get('/products', { params }),
  detail: (id) => request.get(`/products/${id}`),
  reviews: (id) => request.get(`/products/${id}/reviews`)
}

export const cartApi = {
  list: () => request.get('/cart/items'),
  add: (data) => request.post('/cart/items', data),
  update: (id, quantity) => request.put(`/cart/items/${id}`, null, { params: { quantity } }),
  remove: (id) => request.delete(`/cart/items/${id}`)
}

export const orderApi = {
  list: () => request.get('/orders'),
  create: (data) => request.post('/orders', data),
  pay: (id) => request.post(`/orders/${id}/pay`),
  cancel: (id) => request.post(`/orders/${id}/cancel`),
  confirm: (id) => request.post(`/orders/${id}/confirm`),
  items: (id) => request.get(`/orders/${id}/items`)
}

export const refundApi = {
  list: () => request.get('/refunds'),
  create: (data) => request.post('/refunds', data),
  logs: (id) => request.get(`/refunds/${id}/logs`),
  submitReturn: (id, data) => request.post(`/refunds/${id}/return`, data),
  intervention: (id) => request.post(`/refunds/${id}/intervention`)
}

export const reviewApi = {
  create: (data) => request.post('/reviews', data)
}

export const merchantApi = {
  products: () => request.get('/merchant/products'),
  createProduct: (data) => request.post('/merchant/products', data),
  updateProduct: (id, data) => request.put(`/merchant/products/${id}`, data),
  offShelf: (id) => request.post(`/merchant/products/${id}/off-shelf`),
  orders: () => request.get('/merchant/orders'),
  ship: (id, data) => request.post(`/merchant/orders/${id}/ship`, data),
  refunds: () => request.get('/merchant/refunds'),
  approveRefund: (id, data) => request.post(`/merchant/refunds/${id}/approve`, data),
  rejectRefund: (id, data) => request.post(`/merchant/refunds/${id}/reject`, data),
  confirmReturn: (id, data) => request.post(`/merchant/refunds/${id}/confirm-return`, data)
}

export const adminApi = {
  users: () => request.get('/admin/users'),
  merchants: () => request.get('/admin/merchants'),
  updateMerchantStatus: (id, status, reason) => request.post(`/admin/merchants/${id}/status`, null, { params: { status, reason } }),
  pendingProducts: () => request.get('/admin/products/pending'),
  approveProduct: (id) => request.post(`/admin/products/${id}/approve`),
  rejectProduct: (id, data) => request.post(`/admin/products/${id}/reject`, data),
  disputes: () => request.get('/admin/refunds/disputes'),
  arbitrate: (id, data) => request.post(`/admin/refunds/${id}/arbitrate`, data),
  logs: () => request.get('/admin/logs')
}
