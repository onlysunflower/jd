import { reactive } from 'vue'

const savedUser = localStorage.getItem('user')

export const store = reactive({
  user: savedUser ? JSON.parse(savedUser) : null,
  merchantId: localStorage.getItem('merchantId') || null,
  setLogin(payload) {
    this.user = payload.user
    this.merchantId = payload.merchantId
    localStorage.setItem('token', payload.token)
    localStorage.setItem('user', JSON.stringify(payload.user))
    if (payload.merchantId) {
      localStorage.setItem('merchantId', payload.merchantId)
    }
  },
  logout() {
    this.user = null
    this.merchantId = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('merchantId')
  }
})

export function hasRole(...roles) {
  return store.user && roles.includes(store.user.role)
}
