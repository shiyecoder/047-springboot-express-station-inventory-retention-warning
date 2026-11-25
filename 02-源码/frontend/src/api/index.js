import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一 axios 实例：JWT 注入 + 业务码校验 + 401 回登录
const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body ? body.data : null
  },
  (err) => {
    const status = err.response?.status
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (location.hash.indexOf('/login') < 0) location.hash = '#/login'
    } else if (document.visibilityState === 'visible') {
      ElMessage.error(err.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(err)
  }
)

export default http

// ---------------- 认证 ----------------
export const login = (data) => http.post('/auth/login', data)
export const logout = () => http.post('/auth/logout')
export const me = () => http.get('/auth/me')
export const updateProfile = (data) => http.put('/auth/profile', data)
export const changePassword = (data) => http.put('/auth/password', data)

// ---------------- 看板 / 选项 ----------------
export const dashboard = (period) => http.get('/dashboard', { params: { period } })
export const options = () => http.get('/options')
export const intakeDesk = () => http.get('/intake/desk')

// ---------------- 快件 ----------------
export const parcelPage = (params) => http.get('/parcel/page', { params })
export const parcelDetail = (id) => http.get('/parcel/' + id)
export const saveParcel = (data) => http.post('/parcel', data)
export const transition = (id, data) => http.post('/parcel/' + id + '/transition', data)
export const evaluate = (id) => http.post('/parcel/' + id + '/evaluate')
export const exportParcels = (params) => http.get('/parcel/export', { params })

// ---------------- 批次 / 货架 ----------------
export const batchPage = (params) => http.get('/batch/page', { params })
export const batchDetail = (id) => http.get('/batch/' + id)
export const saveBatch = (data) => http.post('/batch', data)
export const shelfPage = (params) => http.get('/shelf/page', { params })
export const shelfMap = () => http.get('/shelf/map')
export const stationList = () => http.get('/station/list')
export const courierPage = (params) => http.get('/courier/page', { params })
export const staffPage = (params) => http.get('/staff/page', { params })

// ---------------- 通知 / 取件 ----------------
export const notifyPage = (params) => http.get('/notify/page', { params })
export const notifyParcel = (id, data) => http.post('/notify/' + id, data)
export const pickupQuery = (key) => http.get('/pickup/query', { params: { key } })
export const confirmPickup = (data) => http.post('/pickup/confirm', data)
export const pickupPage = (params) => http.get('/pickup/page', { params })

// ---------------- 滞留 / 退件 / 异常 ----------------
export const retentionPage = (params) => http.get('/retention/page', { params })
export const generateRetention = () => http.post('/retention/generate')
export const retentionNotify = (data) => http.post('/retention/notify', data)
export const handleRetention = (id, data) => http.put('/retention/' + id + '/handle', data)
export const returnPage = (params) => http.get('/return/page', { params })
export const createReturn = (data) => http.post('/return', data)
export const approveReturn = (id, data) => http.put('/return/' + id + '/approve', data)
export const handoverReturn = (data) => http.post('/return/handover', data)
export const exceptionPage = (params) => http.get('/exception/page', { params })
export const createException = (data) => http.post('/exception', data)
export const auditException = (id, data) => http.put('/exception/' + id + '/audit', data)

// ---------------- 盘点 / 移库 / 评价 ----------------
export const stocktakePage = (params) => http.get('/stocktake/page', { params })
export const stocktakeDetail = (id) => http.get('/stocktake/' + id)
export const createStocktake = (data) => http.post('/stocktake', data)
export const checkStocktakeItem = (id, data) => http.put('/stocktake/item/' + id + '/check', data)
export const finishStocktake = (id) => http.put('/stocktake/' + id + '/finish')
export const movePage = (params) => http.get('/move/page', { params })
export const moveParcels = (data) => http.post('/move', data)
export const reviewPage = (params) => http.get('/review/page', { params })
export const replyReview = (id, data) => http.put('/review/' + id + '/reply', data)

// ---------------- 创新点 / 系统 ----------------
export const scorePage = (params) => http.get('/score/page', { params })
export const adjustScore = (id, data) => http.put('/score/' + id + '/adjust', data)
export const rulePage = (params) => http.get('/rule/page', { params })
export const saveRule = (data) => http.post('/rule', data)
export const messagePage = (params) => http.get('/system/message/page', { params })
export const unread = () => http.get('/system/message/unread')
export const readMessage = (id) => http.put('/system/message/' + id + '/read')
export const readAll = () => http.put('/system/message/read-all')
export const roleList = () => http.get('/system/role/list')
export const menuList = () => http.get('/system/menu/list')
