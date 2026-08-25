import { createRouter, createWebHistory } from 'vue-router'
import Home from './views/Home.vue'
import ProductDetail from './views/ProductDetail.vue'
import Login from './views/Login.vue'
import UserOrders from './views/UserOrders.vue'
import Refunds from './views/Refunds.vue'
import MerchantDashboard from './views/MerchantDashboard.vue'
import AdminDashboard from './views/AdminDashboard.vue'

const routes = [
  { path: '/', component: Home },
  { path: '/products/:id', component: ProductDetail },
  { path: '/login', component: Login },
  { path: '/orders', component: UserOrders },
  { path: '/refunds', component: Refunds },
  { path: '/merchant', component: MerchantDashboard },
  { path: '/admin', component: AdminDashboard }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
