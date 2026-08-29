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
import { hasRole } from './store'

function requireUser(to) {
  if (hasRole('USER')) return true
  return { path: '/login', query: { redirect: to.fullPath } }
}

const routes = [
  { path: '/', component: Home },
  { path: '/products/:id', component: ProductDetail },
  { path: '/login', component: Login },
  { path: '/cart', component: Cart },
  { path: '/checkout', component: Checkout, beforeEnter: requireUser },
  { path: '/orders', component: UserOrders },
  { path: '/orders/:id', component: UserOrders, beforeEnter: requireUser },
  { path: '/refunds', component: Refunds },
  { path: '/merchant', component: MerchantDashboard },
  { path: '/admin', component: AdminDashboard }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
