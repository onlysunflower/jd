import { createRouter, createWebHistory } from 'vue-router'
import Home from './views/Home.vue'
import ProductDetail from './views/ProductDetail.vue'
import Login from './views/Login.vue'
import UserOrders from './views/UserOrders.vue'
import Cart from './views/Cart.vue'
import Refunds from './views/Refunds.vue'
import MerchantDashboard from './views/MerchantDashboard.vue'
import AdminDashboard from './views/AdminDashboard.vue'
import Checkout from './views/Checkout.vue'
import OrderDetail from './views/OrderDetail.vue'
import MerchantApply from './views/MerchantApply.vue'
import MerchantFinance from './views/MerchantFinance.vue'
import MerchantReviews from './views/MerchantReviews.vue'
import { hasRole } from './store'

function requireUser(to) {
  if (hasRole('USER')) return true
  return { path: '/login', query: { redirect: to.fullPath } }
}

function requireRoles(...roles) {
  return (to) => hasRole(...roles) ? true : { path: '/login', query: { redirect: to.fullPath } }
}

const routes = [
  { path: '/', component: Home },
  { path: '/products/:id', component: ProductDetail },
  { path: '/login', component: Login },
  { path: '/cart', component: Cart },
  { path: '/checkout', component: Checkout, beforeEnter: requireUser },
  { path: '/orders', component: UserOrders },
  { path: '/orders/:id', component: OrderDetail, beforeEnter: requireUser },
  { path: '/refunds', component: Refunds },
  { path: '/merchant/apply', component: MerchantApply, beforeEnter: requireUser },
  { path: '/merchant', component: MerchantDashboard, beforeEnter: requireRoles('MERCHANT', 'SUPER_ADMIN') },
  { path: '/merchant/reviews', component: MerchantReviews, beforeEnter: requireRoles('MERCHANT') },
  { path: '/merchant/finance', component: MerchantFinance, beforeEnter: requireRoles('MERCHANT') },
  { path: '/admin', component: AdminDashboard, beforeEnter: requireRoles('SERVICE_ADMIN', 'PRODUCT_ADMIN', 'SUPER_ADMIN') }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
