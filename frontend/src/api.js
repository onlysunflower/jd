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
  detail: (id) => request.get(`/orders/${id}`),
  create: (data) => request.post('/orders', data),
  createFromCart: (data) => request.post('/orders/from-cart', data),
  pay: (id) => request.post(`/orders/${id}/pay`),
  cancel: (id) => request.post(`/orders/${id}/cancel`),
  confirm: (id) => request.post(`/orders/${id}/confirm`),
  items: (id) => request.get(`/orders/${id}/items`)
}

export const couponApi = {
  mine: () => request.get('/coupons/mine')
}

export const merchantApplicationApi = {
  mine: () => request.get('/merchant-application/mine'),
  submit: (data) => request.post('/merchant-application', data)
}

export const refundApi = {
  list: () => request.get('/refunds'),
  create: (data) => request.post('/refunds', data),
  logs: (id) => request.get(`/refunds/${id}/logs`),
  submitReturn: (id, data) => request.post(`/refunds/${id}/return`, data),
  intervention: (id) => request.post(`/refunds/${id}/intervention`)
}

export const reviewApi = {
  create: (data) => request.post('/reviews', data),
  tasks: () => request.get('/reviews/tasks'),
  append: (id, data) => request.post(`/reviews/${id}/append`, data)
}

export const merchantApi = {
  categories: () => request.get('/merchant/categories'),
  products: () => request.get('/merchant/products'),
  createProduct: (data) => request.post('/merchant/products', data),
  updateProduct: (id, data) => request.put(`/merchant/products/${id}`, data),
  offShelf: (id) => request.post(`/merchant/products/${id}/off-shelf`),
  onShelf: (id) => request.post(`/merchant/products/${id}/on-shelf`),
  orders: () => request.get('/merchant/orders'),
  ship: (id, data) => request.post(`/merchant/orders/${id}/ship`, data),
  refunds: () => request.get('/merchant/refunds'),
  approveRefund: (id, data) => request.post(`/merchant/refunds/${id}/approve`, data),
  rejectRefund: (id, data) => request.post(`/merchant/refunds/${id}/reject`, data),
  confirmReturn: (id, data) => request.post(`/merchant/refunds/${id}/confirm-return`, data),
  reviews: () => request.get('/merchant/reviews'),
  replyReview: (id, data) => request.post(`/merchant/reviews/${id}/reply`, data),
  balance: () => request.get('/merchant/finance/balance'),
  settlements: () => request.get('/merchant/finance/settlements'),
  withdrawals: () => request.get('/merchant/finance/withdrawals'),
  requestWithdrawal: (data) => request.post('/merchant/finance/withdrawals', data)
}

export const adminApi = {
  users: () => request.get('/admin/users'),
  merchants: () => request.get('/admin/merchants'),
  updateMerchantStatus: (id, status, reason) => request.post(`/admin/merchants/${id}/status`, null, { params: { status, reason } }),
  pendingProducts: () => request.get('/admin/products/pending'),
  products: () => request.get('/admin/products'),
  approveProduct: (id) => request.post(`/admin/products/${id}/approve`),
  rejectProduct: (id, data) => request.post(`/admin/products/${id}/reject`, data),
  forceOffShelf: (id, data) => request.post(`/admin/products/${id}/force-off-shelf`, data),
  merchantApplications: () => request.get('/admin/merchant-applications'),
  reviewMerchantApplication: (id, data) => request.post(`/admin/merchant-applications/${id}/review`, data),
  disputes: () => request.get('/admin/refunds/disputes'),
  arbitrate: (id, data) => request.post(`/admin/refunds/${id}/arbitrate`, data),
  reviews: () => request.get('/admin/reviews'),
  moderateReview: (id, data) => request.post(`/admin/reviews/${id}/moderate`, data),
  settlements: () => request.get('/admin/finance/settlements'),
  withdrawals: () => request.get('/admin/finance/withdrawals'),
  reviewWithdrawal: (id, data) => request.post(`/admin/finance/withdrawals/${id}/review`, data),
  logs: () => request.get('/admin/logs')
}
